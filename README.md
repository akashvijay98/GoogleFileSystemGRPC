# Google File System (GFS)

A distributed file system built with gRPC for communication between clients, master server, and chunk servers.


## Components

- **Client**: REST API for file upload/download, communicates with master and chunk servers via gRPC
- **Master Server**: Manages file metadata, chunk locations, and server health via heartbeats
- **Chunk Servers**: Store actual file chunks, send heartbeats to master

## Features

- File upload with chunk distribution across servers
- File download from multiple chunk servers
- Server health monitoring via heartbeat
- Automatic dead server detection

## Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL (or use Docker Compose)

## Quick Start

### Option 1: Using Docker Compose (Recommended)
from project root, type:
```bash
 mvn clean package -DskipTests
then 
docker compose up -d
```

### Option 2: Local Development

**1. Start PostgreSQL:**
```bash
docker run -d \
  --name postgres-gfs \
  -e POSTGRES_DB=gfs \
  -e POSTGRES_USER=gfs \
  -e POSTGRES_PASSWORD=gfs \
  -p 5432:5432 \
  postgres:15
```

**2. Build the project:**
```bash
mvn clean package -DskipTests
```

**3. Run Master Server:**
```bash
java -jar master-server/target/master-server-1.0-SNAPSHOT.jar
```
Master runs on port 8081 (gRPC)

**4. Run Chunk Servers:**
```bash
# Chunk Server 1
java -Dserver.port=8083 -Dgrpc.server.port=8082 -DSERVER_NAME=chunk-server-1 -jar chunk-server/target/chunk-server-1.0-SNAPSHOT.jar

# Chunk Server 2
java -Dserver.port=8084 -Dgrpc.server.port=8083 -DSERVER_NAME=chunk-server-2 -jar chunk-server/target/chunk-server-1.0-SNAPSHOT.jar

# Chunk Server 3
java -Dserver.port=8085 -Dgrpc.server.port=8084 -DSERVER_NAME=chunk-server-3 -jar chunk-server/target/chunk-server-1.0-SNAPSHOT.jar
```

**5. Run Client:**
```bash
java -jar client/target/client-1.0-SNAPSHOT.jar
```
Client REST API runs on port 8080

## API Endpoints

### Upload File
```bash
curl -X POST http://localhost:8080/upload \
  -F "file=@/path/to/yourfile.txt"
```

### Download File
```bash
curl -J -o downloaded.txt "http://localhost:8080/download?fileName=yourfile.txt"
```

### List Files
```bash
curl http://localhost:8080/files
```

### Get File Chunks
```bash
curl "http://localhost:8080/chunks?fileName=yourfile.txt"
```

## Testing with PostgreSQL

### Connect to PostgreSQL:
```bash
docker exec -it postgres-gfs psql -U gfs -d gfs
```

### View Tables:
```sql
-- List all tables
\dt

-- View file metadata
SELECT * FROM file_metadata;

-- View chunk records  
SELECT * FROM file_chunks;
```

### Manual Queries:

```sql
-- List all files
SELECT id, file_name, created_at FROM file_metadata;

-- Find chunks for a file
SELECT fc.chunk_id, fc.server_address, fc.sequence_number 
FROM file_chunks fc
JOIN file_metadata fm ON fm.id = fc.file_metadata_id
WHERE fm.file_name = 'test.txt';

-- Check server nodes (if using chunk_server_nodes table)
SELECT server_address, status, last_heartbeat_time 
FROM chunk_server_nodes;
```

## Health Monitoring

### View Master Server Health:
```bash
curl http://localhost:8081/actuator/health
```

### View Chunk Server Health:
```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

### Check Prometheus Metrics:
```bash
curl http://localhost:8081/actuator/prometheus
```

## Environment Variables

### Master Server
| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8081 | REST API port |
| GRPC_SERVER_PORT | 9090 | gRPC port |
| SPRING_DATASOURCE_URL | jdbc:postgresql://localhost:5432/gfs | DB URL |
| SPRING_DATASOURCE_USERNAME | gfs | DB username |
| SPRING_DATASOURCE_PASSWORD | gfs | DB password |

### Chunk Server
| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8080 | REST API port |
| GRPC_SERVER_PORT | 8082 | gRPC port |
| MASTER_SERVER_ADDRESS | master-server:8081 | Master gRPC address |
| SERVER_NAME | chunk-server | Server identifier |

### Client
| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8080 | REST API port |
| MASTER_SERVER_ADDRESS | localhost:8081 | Master gRPC address |

## Troubleshooting

### Connection Refused Errors
- Ensure PostgreSQL is running
- Check if ports are available
- Verify Docker network configuration

### Chunk Server Not Registering
- Check heartbeat interval (5 seconds)
- Verify master server is accessible
- Check logs for connection errors

### File Upload Fails
- Ensure at least one chunk server is running
- Check chunk server health
- Verify file permissions

## Load Testing (Max Throughput)

This repo includes a small load test harness: `load_test.py`. It targets the Client REST API (`:8080`) and reports:
- `ok_rps` (successful requests/sec)
- `mbps_out` / `mbps_in` (MiB/sec)
- `p50` / `p95` / `p99` latency

Note: `/upload` always returns HTTP 200 even when the upload fails; the load test treats any non-success response body as a failure.

### Upload throughput
```bash
python3 load_test.py upload --base-url http://localhost:8080 --duration 30 --concurrency 50 --file-size-mb 1 --unique-names
```

### Download throughput
This seeds the file(s) first, then repeatedly downloads:
```bash
python3 load_test.py download --base-url http://localhost:8080 --duration 30 --concurrency 100 --file-size-mb 8 --file-name loadtest.bin --download-pool 3
```

### Find max stable throughput (concurrency sweep)
Stops when `error_rate` exceeds 1% or `p95` exceeds 2000ms (tunable):
```bash
python3 load_test.py sweep upload --base-url http://localhost:8080 --duration 20 --file-size-mb 1 --unique-names --concurrency-list 1,2,4,8,16,32,64,128
python3 load_test.py sweep download --base-url http://localhost:8080 --duration 20 --file-size-mb 8 --download-pool 3 --concurrency-list 1,2,4,8,16,32,64,128,256
```

### Notes
- For more realistic results, run the load generator on a different machine/container than the GFS services.
- If `ok_rps` keeps rising but `p95` explodes, you are past the system’s comfortable throughput; use the highest `ok_rps` that still meets your latency/error SLO.
- Prefer `--unique-names` for upload sweeps to avoid testing overwrite behavior.

### Prometheus metrics for the load generator
`load_test.py` can expose Prometheus metrics (requests/latency/bytes) while a test is running:
```bash
python3 load_test.py upload --base-url http://localhost:8080 --duration 60 --concurrency 50 --file-size-mb 1 --unique-names --prom-port 8000
```
In Docker Compose, `prometheus.yml` also scrapes the `loadtest` service at `/metrics`, so Prometheus shows load-generator + server-side metrics together.
