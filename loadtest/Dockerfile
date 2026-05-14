FROM python:3.11-slim

WORKDIR /app
COPY loadtest/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY load_test.py /app/load_test.py

# Default: 50 concurrent uploads + Prometheus metrics on :8000.
CMD ["python", "/app/load_test.py", "upload", "--base-url", "http://client:8080", "--duration", "30", "--concurrency", "50", "--file-size-mb", "1", "--unique-names", "--prom-port", "8000"]
