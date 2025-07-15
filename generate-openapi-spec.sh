#!/bin/bash

# Script to start the server, generate OpenAPI spec, and shut down the server
# Usage: ./generate-openapi-spec.sh [host] [port]
# Defaults: host=localhost, port=8080

HOST=${1:-localhost}
PORT=${2:-8080}
URL="http://$HOST:$PORT/v3/api-docs"
OUTPUT="openapi-admin.json"

# Start the Spring Boot server in the background
./mvnw spring-boot:run -pl graphql-app -Dspring-boot.run.profiles=sqlite &
SERVER_PID=$!
echo "Started Spring Boot server (PID $SERVER_PID)"

# Wait for the app to be up (max 60s)
echo "Checking if Spring Boot app is running at $URL ..."
for i in {1..60}; do
  if curl --output /dev/null --silent --head --fail "$URL"; then
    echo "App is up!"
    break
  fi
  echo "Waiting for app... ($i)"
  sleep 1
done

# Download the OpenAPI spec
if curl --fail "$URL" -o "$OUTPUT"; then
  echo "OpenAPI spec downloaded to $OUTPUT"
else
  echo "Failed to download OpenAPI spec from $URL"
  kill $SERVER_PID
  exit 1
fi

# Format the JSON file if jq is available
if command -v jq >/dev/null 2>&1; then
  jq . "$OUTPUT" > "${OUTPUT}.tmp" && mv "${OUTPUT}.tmp" "$OUTPUT"
  echo "OpenAPI spec formatted with jq."
else
  echo "Warning: jq not found. JSON will not be pretty-printed."
fi

# Gracefully shut down the server
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "Spring Boot server stopped." 