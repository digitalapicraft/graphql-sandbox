# GraphQL Sandbox

**Project managed by Digital API Corp**

This project is an open-source Spring Boot server that dynamically generates a GraphQL API and an SQLite database from an uploaded GraphQL schema. It allows you to upload a `.graphql` spec, automatically creates the corresponding database tables, and exposes a `/graphql` endpoint for queries and mutations.

## High-Level Flow

```mermaid
sequenceDiagram
    participant User
    participant REST_API as REST API (/api/upload-graphql-spec)
    participant SchemaService
    participant SchemaRegistry
    participant SQLiteDB as SQLite DB
    participant GraphQL_API as GraphQL API (/graphql)

    User->>REST_API: Upload GraphQL schema
    REST_API->>SchemaService: processSchemaFile(schemaFile)
    SchemaService->>SQLiteDB: Create tables from schema
    REST_API->>SchemaRegistry: setSchemaFile(schemaFile)
    REST_API-->>User: Success response

    User->>GraphQL_API: Query/Mutation request
    GraphQL_API->>SchemaRegistry: getSchemaFile()
    GraphQL_API->>SQLiteDB: Execute SQL (via resolvers)
    GraphQL_API-->>User: Query/Mutation result
```

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
   git clone https://github.com/digitalapicraft/graphql-sandbox.git
   cd graphql-sandbox
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

## How to Contribute

We welcome contributions from the community! To help us review and merge your changes efficiently, please follow these guidelines:

### 1. Fork and Branch
- Fork the repository and create a new branch for your feature or bugfix.

### 2. Create a Pull Request (PR)
- Push your branch to your fork and open a [Pull Request](https://github.com/digitalapicraft/graphql-sandbox/pulls) against the `main` branch.
- Clearly describe your changes and reference any related issues or discussions.

### 3. Add Discussion Notes
- If your PR introduces a significant change, please start a [Discussion](https://github.com/digitalapicraft/graphql-sandbox/discussions) and link it in your PR description.
- For questions, ideas, or proposals, use the [Discussions](https://github.com/digitalapicraft/graphql-sandbox/discussions) tab.

### 4. General Guidelines
- Ensure your code passes all tests (`./mvnw test`) and follows the project's style.
- Add or update documentation as needed.
- Keep PRs focused and minimal—one feature or fix per PR is preferred.
- Be respectful and constructive in all communications.

## Contributing
Contributions are welcome! Please open issues or pull requests for improvements or bug fixes.

_This project is managed and maintained by **Digital API Corp**._

## License
This project is licensed under the MIT License. See [LICENSE](LICENSE) for details. 