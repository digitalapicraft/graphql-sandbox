#!/bin/bash
# run.sh - GraphQL Application Runner
# Usage: ./run.sh [profile] [port]
# Examples:
#   ./run.sh              # Run with SQLite profile on port 8080
#   ./run.sh postgres     # Run with PostgreSQL profile on port 8080
#   ./run.sh sqlite 8081  # Run with SQLite profile on port 8081

# Default values
PROFILE=${1:-sqlite}
PORT=${2:-8080}

echo "🚀 Starting GraphQL application..."
echo "   Profile: $PROFILE"
echo "   Port: $PORT"
echo "   URL: http://localhost:$PORT"
echo ""

# Set the profile and run the application
SPRING_PROFILES_ACTIVE=$PROFILE ./mvnw spring-boot:run -pl graphql-app -Dspring-boot.run.jvmArguments="-Dserver.port=$PORT" 