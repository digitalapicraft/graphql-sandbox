#!/bin/bash

set -e

PROM_DIR="$(dirname "$0")"
CONFIG_FILE="$PROM_DIR/prometheus.yml"

# Create prometheus.yml if it doesn't exist
if [ ! -f "$CONFIG_FILE" ]; then
  cat > "$CONFIG_FILE" <<EOF
scrape_configs:
  - job_name: 'graphql-sandbox'
    static_configs:
      - targets: ['host.docker.internal:8080']
    metrics_path: /actuator/prometheus
EOF
  echo "Created default prometheus.yml in $PROM_DIR"
fi

echo "Starting Prometheus Docker container..."
docker run --rm -d \
  --name prometheus-graphql-sandbox \
  -p 9090:9090 \
  -v "$CONFIG_FILE":/etc/prometheus/prometheus.yml \
  prom/prometheus

echo "Prometheus is running at http://localhost:9090" 