"""
Load test harness for the GFS "client" REST API.

Why this exists:
- The /upload endpoint always returns HTTP 200, even on failures, so we treat non-success
  response bodies as failures for throughput measurements.

Examples:
  python3 load_test.py upload --base-url http://localhost:8080 --duration 30 --concurrency 50 --file-size-mb 1
  python3 load_test.py download --base-url http://localhost:8080 --duration 30 --concurrency 100 --file-name test.bin
  python3 load_test.py sweep upload --base-url http://localhost:8080 --duration 20 --file-size-mb 1 --concurrency-list 1,2,4,8,16,32,64
"""

from __future__ import annotations

import argparse
import json
import os
import random
import statistics
import threading
import time
import uuid
from dataclasses import dataclass
from typing import Callable, Dict, List, Optional, Sequence, Tuple

import requests

try:
    from prometheus_client import Counter, Gauge, Histogram, start_http_server  # type: ignore
except Exception:  # pragma: no cover
    Counter = None  # type: ignore[assignment]
    Gauge = None  # type: ignore[assignment]
    Histogram = None  # type: ignore[assignment]
    start_http_server = None  # type: ignore[assignment]


_PROM_ENABLED = False
_REQS: "Counter" = None  # type: ignore[assignment]
_LAT_S: "Histogram" = None  # type: ignore[assignment]
_BYTES_OUT: "Counter" = None  # type: ignore[assignment]
_BYTES_IN: "Counter" = None  # type: ignore[assignment]
_INFLIGHT: "Gauge" = None  # type: ignore[assignment]
_TARGET_CONCURRENCY: "Gauge" = None  # type: ignore[assignment]
_TARGET_FILE_SIZE_MB: "Gauge" = None  # type: ignore[assignment]


def _enable_prometheus(addr: str, port: int) -> None:
    global _PROM_ENABLED, _REQS, _LAT_S, _BYTES_OUT, _BYTES_IN, _INFLIGHT, _TARGET_CONCURRENCY, _TARGET_FILE_SIZE_MB
    if port <= 0:
        return
    if start_http_server is None:
        print("Prometheus disabled: prometheus_client is not installed. Install: pip install prometheus-client")
        return

    # Keep metric names stable; use labels for mode/status.
    _REQS = Counter(
        "gfs_loadtest_requests_total",
        "Total HTTP requests sent by load_test.py",
        labelnames=("mode", "status"),
    )
    _LAT_S = Histogram(
        "gfs_loadtest_request_latency_seconds",
        "End-to-end request latency (seconds)",
        labelnames=("mode",),
        buckets=(0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2, 5, 10, 20, 30),
    )
    _BYTES_OUT = Counter(
        "gfs_loadtest_bytes_out_total",
        "Total request payload bytes sent",
        labelnames=("mode",),
    )
    _BYTES_IN = Counter(
        "gfs_loadtest_bytes_in_total",
        "Total response/body bytes received",
        labelnames=("mode",),
    )
    _INFLIGHT = Gauge(
        "gfs_loadtest_inflight_requests",
        "In-flight requests at the load generator",
        labelnames=("mode",),
    )
    _TARGET_CONCURRENCY = Gauge(
        "gfs_loadtest_target_concurrency",
        "Configured load-test concurrency",
        labelnames=("mode",),
    )
    _TARGET_FILE_SIZE_MB = Gauge(
        "gfs_loadtest_target_file_size_mb",
        "Configured file size in MiB",
        labelnames=("mode",),
    )

    start_http_server(port=port, addr=addr)
    _PROM_ENABLED = True
    print(f"Prometheus metrics: http://{addr}:{port}/metrics")


def _now() -> float:
    return time.perf_counter()


def _percentile(sorted_values: Sequence[float], p: float) -> Optional[float]:
    if not sorted_values:
        return None
    if p <= 0:
        return float(sorted_values[0])
    if p >= 100:
        return float(sorted_values[-1])
    k = (len(sorted_values) - 1) * (p / 100.0)
    f = int(k)
    c = min(f + 1, len(sorted_values) - 1)
    if f == c:
        return float(sorted_values[f])
    return float(sorted_values[f] + (sorted_values[c] - sorted_values[f]) * (k - f))


def _format_float(v: Optional[float], digits: int = 2) -> str:
    if v is None:
        return "-"
    return f"{v:.{digits}f}"


def _parse_concurrency_list(value: str) -> List[int]:
    out: List[int] = []
    for part in value.split(","):
        part = part.strip()
        if not part:
            continue
        out.append(int(part))
    if not out or any(v <= 0 for v in out):
        raise argparse.ArgumentTypeError("concurrency list must contain positive integers")
    return out


def _make_payload_bytes(size_mb: float) -> bytes:
    size_bytes = int(size_mb * 1024 * 1024)
    if size_bytes <= 0:
        raise ValueError("file-size-mb must be > 0")
    # Use deterministic-ish data (not all-zero) to avoid extreme compression/dedup in some stacks.
    seed = os.urandom(16)
    buf = bytearray(size_bytes)
    for i in range(size_bytes):
        buf[i] = seed[i % len(seed)] ^ (i & 0xFF)
    return bytes(buf)


@dataclass
class RunResult:
    mode: str
    concurrency: int
    duration_s: float
    total: int
    ok: int
    errors: int
    bytes_out: int
    bytes_in: int
    lat_ms: List[float]

    def summary(self) -> Dict[str, object]:
        lat_sorted = sorted(self.lat_ms)
        p50 = _percentile(lat_sorted, 50)
        p95 = _percentile(lat_sorted, 95)
        p99 = _percentile(lat_sorted, 99)
        rps = self.total / self.duration_s if self.duration_s > 0 else 0.0
        ok_rps = self.ok / self.duration_s if self.duration_s > 0 else 0.0
        mbps_out = (self.bytes_out / (1024 * 1024)) / self.duration_s if self.duration_s > 0 else 0.0
        mbps_in = (self.bytes_in / (1024 * 1024)) / self.duration_s if self.duration_s > 0 else 0.0
        err_rate = (self.errors / self.total) if self.total else 0.0
        return {
            "mode": self.mode,
            "concurrency": self.concurrency,
            "duration_s": round(self.duration_s, 3),
            "total": self.total,
            "ok": self.ok,
            "errors": self.errors,
            "error_rate": err_rate,
            "rps": rps,
            "ok_rps": ok_rps,
            "mbps_out": mbps_out,
            "mbps_in": mbps_in,
            "p50_ms": p50,
            "p95_ms": p95,
            "p99_ms": p99,
        }


class Stats:
    def __init__(self, mode: str, concurrency: int, file_size_mb: float) -> None:
        self._lock = threading.Lock()
        self._mode = mode
        self.total = 0
        self.ok = 0
        self.errors = 0
        self.bytes_out = 0
        self.bytes_in = 0
        self.lat_ms: List[float] = []
        if _PROM_ENABLED:
            _TARGET_CONCURRENCY.labels(mode).set(concurrency)
            _TARGET_FILE_SIZE_MB.labels(mode).set(file_size_mb)

    def record(self, ok: bool, lat_ms: float, bytes_out: int, bytes_in: int) -> None:
        with self._lock:
            self.total += 1
            self.ok += 1 if ok else 0
            self.errors += 0 if ok else 1
            self.bytes_out += bytes_out
            self.bytes_in += bytes_in
            self.lat_ms.append(lat_ms)
        if _PROM_ENABLED:
            _REQS.labels(self._mode, "ok" if ok else "error").inc()
            _LAT_S.labels(self._mode).observe(lat_ms / 1000.0)
            if bytes_out:
                _BYTES_OUT.labels(self._mode).inc(bytes_out)
            if bytes_in:
                _BYTES_IN.labels(self._mode).inc(bytes_in)


def _upload_once(
    session: requests.Session,
    base_url: str,
    file_name: str,
    payload: bytes,
    timeout_s: float,
) -> Tuple[bool, int, int]:
    # /upload always returns 200; treat body content as success signal.
    # Controller returns: "File uploaded successfully: <name>"
    url = base_url.rstrip("/") + "/upload"
    files = {"file": (file_name, payload, "application/octet-stream")}
    resp = session.post(url, files=files, timeout=timeout_s)
    body = resp.text or ""
    ok = resp.status_code == 200 and body.startswith("File uploaded successfully:")
    bytes_out = len(payload)
    bytes_in = len(resp.content or b"")
    return ok, bytes_out, bytes_in


def _download_once(
    session: requests.Session,
    base_url: str,
    file_name: str,
    timeout_s: float,
) -> Tuple[bool, int, int]:
    url = base_url.rstrip("/") + "/download"
    resp = session.get(url, params={"fileName": file_name}, stream=True, timeout=timeout_s)
    ok = resp.status_code == 200
    bytes_in = 0
    if ok:
        for chunk in resp.iter_content(chunk_size=256 * 1024):
            if chunk:
                bytes_in += len(chunk)
    else:
        _ = resp.content  # drain for connection reuse
    return ok, 0, bytes_in


def _worker_loop(stop_at: float, fn: Callable[[], Tuple[bool, int, int]], stats: Stats) -> None:
    session = requests.Session()
    while _now() < stop_at:
        start = _now()
        ok = False
        bytes_out = 0
        bytes_in = 0
        if _PROM_ENABLED:
            _INFLIGHT.labels(stats._mode).inc()
        try:
            ok, bytes_out, bytes_in = fn()
        except Exception:
            ok = False
        finally:
            if _PROM_ENABLED:
                _INFLIGHT.labels(stats._mode).dec()
        end = _now()
        stats.record(ok=ok, lat_ms=(end - start) * 1000.0, bytes_out=bytes_out, bytes_in=bytes_in)


def run_load(
    mode: str,
    base_url: str,
    concurrency: int,
    duration_s: float,
    timeout_s: float,
    file_size_mb: float,
    file_name: str,
    unique_names: bool,
    download_pool: int,
) -> RunResult:
    if concurrency <= 0:
        raise ValueError("concurrency must be > 0")
    if duration_s <= 0:
        raise ValueError("duration must be > 0")

    stats = Stats(mode=mode, concurrency=concurrency, file_size_mb=file_size_mb)
    start = _now()
    stop_at = start + duration_s

    if mode == "upload":
        payload = _make_payload_bytes(file_size_mb)
        shared_name = file_name

        def make_upload_fn() -> Callable[[], Tuple[bool, int, int]]:
            session = requests.Session()
            counter = 0

            def _fn() -> Tuple[bool, int, int]:
                nonlocal counter
                counter += 1
                if unique_names:
                    name = f"{os.path.splitext(shared_name)[0]}-{uuid.uuid4().hex}{os.path.splitext(shared_name)[1]}"
                else:
                    name = shared_name
                return _upload_once(session, base_url=base_url, file_name=name, payload=payload, timeout_s=timeout_s)

            return _fn

        threads: List[threading.Thread] = []
        for _ in range(concurrency):
            fn = make_upload_fn()
            t = threading.Thread(target=_worker_loop, args=(stop_at, fn, stats), daemon=True)
            t.start()
            threads.append(t)
        for t in threads:
            t.join()

    elif mode == "download":
        # Ensure at least one file exists; optionally create a pool to spread hot-spotting.
        if download_pool <= 0:
            download_pool = 1
        payload = _make_payload_bytes(file_size_mb)
        seed_session = requests.Session()
        available_names: List[str] = []
        for i in range(download_pool):
            name = file_name if download_pool == 1 else f"{os.path.splitext(file_name)[0]}-seed{i}{os.path.splitext(file_name)[1]}"
            ok, _, _ = _upload_once(seed_session, base_url=base_url, file_name=name, payload=payload, timeout_s=timeout_s)
            if ok:
                available_names.append(name)
        if not available_names:
            raise RuntimeError("failed to seed file(s) for download test; check client logs and master/chunk health")

        def make_download_fn() -> Callable[[], Tuple[bool, int, int]]:
            session = requests.Session()

            def _fn() -> Tuple[bool, int, int]:
                name = random.choice(available_names)
                return _download_once(session, base_url=base_url, file_name=name, timeout_s=timeout_s)

            return _fn

        threads = []
        for _ in range(concurrency):
            fn = make_download_fn()
            t = threading.Thread(target=_worker_loop, args=(stop_at, fn, stats), daemon=True)
            t.start()
            threads.append(t)
        for t in threads:
            t.join()
    else:
        raise ValueError("mode must be 'upload' or 'download'")

    end = _now()
    return RunResult(
        mode=mode,
        concurrency=concurrency,
        duration_s=end - start,
        total=stats.total,
        ok=stats.ok,
        errors=stats.errors,
        bytes_out=stats.bytes_out,
        bytes_in=stats.bytes_in,
        lat_ms=stats.lat_ms,
    )


def _print_row(result: RunResult) -> None:
    s = result.summary()
    print(
        " ".join(
            [
                f"mode={s['mode']}",
                f"c={s['concurrency']}",
                f"ok_rps={_format_float(float(s['ok_rps']), 1)}",
                f"err={_format_float(float(s['error_rate'])*100.0, 2)}%",
                f"p50={_format_float(s['p50_ms'], 1)}ms",
                f"p95={_format_float(s['p95_ms'], 1)}ms",
                f"mbps_out={_format_float(float(s['mbps_out']), 2)}",
                f"mbps_in={_format_float(float(s['mbps_in']), 2)}",
            ]
        )
    )


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="GFS REST load test (upload/download).")
    sub = parser.add_subparsers(dest="cmd", required=True)

    def add_common(p: argparse.ArgumentParser) -> None:
        p.add_argument("--base-url", default="http://localhost:8080", help="Client base URL (default: http://localhost:8080)")
        p.add_argument("--duration", type=float, default=30.0, help="Test duration in seconds (default: 30)")
        p.add_argument("--concurrency", type=int, default=50, help="Number of concurrent workers (default: 50)")
        p.add_argument("--timeout", type=float, default=10.0, help="Request timeout seconds (default: 10)")
        p.add_argument("--file-size-mb", type=float, default=1.0, help="Upload/download file size in MiB (default: 1)")
        p.add_argument("--json-out", default="", help="Write JSON summary to this path (optional)")
        p.add_argument("--prom-addr", default="0.0.0.0", help="Prometheus metrics bind addr (default: 0.0.0.0)")
        p.add_argument("--prom-port", type=int, default=0, help="Expose /metrics on this port (default: 0=off)")

    p_upload = sub.add_parser("upload", help="Upload throughput test against /upload.")
    add_common(p_upload)
    p_upload.add_argument("--file-name", default="loadtest.bin", help="Uploaded file name (default: loadtest.bin)")
    p_upload.add_argument("--unique-names", action="store_true", help="Use unique file names to avoid overwrites.")

    p_download = sub.add_parser("download", help="Download throughput test against /download.")
    add_common(p_download)
    p_download.add_argument("--file-name", default="loadtest.bin", help="File name to seed and download (default: loadtest.bin)")
    p_download.add_argument("--download-pool", type=int, default=1, help="Seed N distinct files to spread reads (default: 1)")

    p_sweep = sub.add_parser("sweep", help="Run a concurrency sweep and report the max stable throughput.")
    p_sweep_sub = p_sweep.add_subparsers(dest="mode", required=True)
    p_sweep_upload = p_sweep_sub.add_parser("upload", help="Sweep upload.")
    add_common(p_sweep_upload)
    p_sweep_upload.add_argument("--file-name", default="loadtest.bin", help="Uploaded file name (default: loadtest.bin)")
    p_sweep_upload.add_argument("--unique-names", action="store_true", help="Use unique file names to avoid overwrites.")
    p_sweep_upload.add_argument("--concurrency-list", type=_parse_concurrency_list, default=_parse_concurrency_list("1,2,4,8,16,32,64"), help="Comma-separated list.")
    p_sweep_upload.add_argument("--max-error-rate", type=float, default=0.01, help="Stop when error_rate exceeds this (default: 0.01)")
    p_sweep_upload.add_argument("--max-p95-ms", type=float, default=2000.0, help="Stop when p95 exceeds this (default: 2000ms)")

    p_sweep_download = p_sweep_sub.add_parser("download", help="Sweep download.")
    add_common(p_sweep_download)
    p_sweep_download.add_argument("--file-name", default="loadtest.bin", help="File name to seed and download (default: loadtest.bin)")
    p_sweep_download.add_argument("--download-pool", type=int, default=1, help="Seed N distinct files (default: 1)")
    p_sweep_download.add_argument("--concurrency-list", type=_parse_concurrency_list, default=_parse_concurrency_list("1,2,4,8,16,32,64,128"), help="Comma-separated list.")
    p_sweep_download.add_argument("--max-error-rate", type=float, default=0.01, help="Stop when error_rate exceeds this (default: 0.01)")
    p_sweep_download.add_argument("--max-p95-ms", type=float, default=2000.0, help="Stop when p95 exceeds this (default: 2000ms)")

    args = parser.parse_args(argv)

    if args.cmd in ("upload", "download"):
        _enable_prometheus(addr=args.prom_addr, port=args.prom_port)
        result = run_load(
            mode=args.cmd,
            base_url=args.base_url,
            concurrency=args.concurrency,
            duration_s=args.duration,
            timeout_s=args.timeout,
            file_size_mb=args.file_size_mb,
            file_name=getattr(args, "file_name"),
            unique_names=getattr(args, "unique_names", False),
            download_pool=getattr(args, "download_pool", 1),
        )
        _print_row(result)
        out = result.summary()
        if args.json_out:
            with open(args.json_out, "w", encoding="utf-8") as f:
                json.dump(out, f, indent=2, sort_keys=True)
                f.write("\n")
        return 0

    # sweep
    _enable_prometheus(addr=args.prom_addr, port=args.prom_port)
    results: List[Dict[str, object]] = []
    best: Optional[Dict[str, object]] = None
    conc_list: List[int] = list(getattr(args, "concurrency_list"))
    for c in conc_list:
        result = run_load(
            mode=args.mode,
            base_url=args.base_url,
            concurrency=c,
            duration_s=args.duration,
            timeout_s=args.timeout,
            file_size_mb=args.file_size_mb,
            file_name=getattr(args, "file_name"),
            unique_names=getattr(args, "unique_names", False),
            download_pool=getattr(args, "download_pool", 1),
        )
        _print_row(result)
        s = result.summary()
        results.append(s)
        best = s if (best is None or float(s["ok_rps"]) > float(best["ok_rps"])) else best
        if float(s["error_rate"]) > float(args.max_error_rate):
            print(f"Stopping: error_rate {float(s['error_rate']):.4f} > {args.max_error_rate:.4f}")
            break
        p95 = s.get("p95_ms")
        if p95 is not None and float(p95) > float(args.max_p95_ms):
            print(f"Stopping: p95 {float(p95):.1f}ms > {args.max_p95_ms:.1f}ms")
            break

    payload = {"best": best, "results": results}
    if args.json_out:
        with open(args.json_out, "w", encoding="utf-8") as f:
            json.dump(payload, f, indent=2, sort_keys=True)
            f.write("\n")
    else:
        print("Best:")
        print(json.dumps(best, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
