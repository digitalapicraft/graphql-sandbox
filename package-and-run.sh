#!/bin/bash
set -e

if [[ "$1" == "-h" || "$1" == "--help" ]]; then
  echo "\nUsage: $0 [profile] [extra-args...]\n"
  echo "  profile        Spring profile to use (default: sqlite, or postgres)"
  echo "  extra-args     Any additional arguments to pass to the Spring Boot app"
  echo "\nExamples:"
  echo "  $0             # Build and run with sqlite profile (default)"
  echo "  $0 postgres    # Build and run with postgres profile"
  echo "  $0 sqlite --server.port=8081   # Custom port with sqlite"
  exit 0
fi

PROFILE=${1:-sqlite}
shift || true

# Build the jar
./mvnw clean package -pl graphql-app -am

JAR=graphql-app/target/graphql-app-0.0.1-SNAPSHOT.jar

if [ ! -f "$JAR" ]; then
  echo "JAR not found: $JAR"
  exit 1
fi

echo "Running: java -jar $JAR --spring.profiles.active=$PROFILE $@ (via package-and-run.sh)"
exec java -jar "$JAR" --spring.profiles.active="$PROFILE" "$@" 