# Spring Boot Dynamic GraphQL Server

**Project managed by Digital API Corp**

This project is an open-source Spring Boot server that dynamically generates a GraphQL API and an SQLite database from an uploaded GraphQL schema. It allows you to upload a `.graphql` spec, automatically creates the corresponding database tables, and exposes a `/graphql` endpoint for queries and mutations.

## Features
- Upload a GraphQL schema and auto-generate SQLite tables
- Dynamic GraphQL endpoint based on uploaded schema
- Query and mutate data via GraphQL
- REST API for schema upload
- Code coverage and CI via GitHub Actions

## Getting Started

### Prerequisites
- Java 17+
- Maven

### Setup
1. Clone the repository:
   ```bash
   git clone <your-repo-url>
   cd graphql-server
   ```
2. Build the project:
   ```bash
   ./mvnw clean install
   ```
3. Run the server:
   ```bash
   ./mvnw spring-boot:run
   ```

### Usage
1. **Upload a GraphQL schema:**
   ```bash
   curl -F "file=@/path/to/your/schema.graphql" http://localhost:8080/api/upload-graphql-spec
   ```
2. **Query the GraphQL endpoint:**
   Send POST requests to `http://localhost:8080/graphql` with a JSON body:
   ```json
   { "query": "{ books { id title author } }" }
   ```

## API Endpoints
- `POST /api/upload-graphql-spec` — Upload a GraphQL schema file
- `POST /graphql` — Execute GraphQL queries and mutations

## Testing
Run all tests and generate a code coverage report:
```bash
./mvnw test
```
The coverage report will be available in `target/site/jacoco/index.html`.

## Contributing
Contributions are welcome! Please open issues or pull requests for improvements or bug fixes.

_This project is managed and maintained by **Digital API Corp**._

## License
This project is licensed under the MIT License. See [LICENSE](LICENSE) for details. 