# LLM_README: Context for Prompt Engineering

## Project Overview
This project is a Spring Boot server that dynamically generates GraphQL APIs and databases (SQLite/Postgres) from uploaded GraphQL schemas. It supports **multiple schemas**, each identified by a unique name (specName), and exposes dynamic endpoints for each schema.

## Key Features
- **Multi-schema support:** Upload and manage multiple GraphQL schemas, each with its own endpoint and database.
- **Dynamic endpoints:**
  - Upload: `POST /api/upload-graphql-spec/{specName}`
  - Query: `POST /graphql/{specName}`
- **In-memory mapping:** Fast lookup of schemas by name, loaded from disk on startup and updated on upload.
- **Integration tested:** Uses Spring Boot and MockMvc for end-to-end HTTP and database testing.

## Prompt Engineering Guidelines
- **Be explicit about the specName** when describing upload, query, or mutation actions.
- **Reference endpoints** as `/api/upload-graphql-spec/{specName}` for uploads and `/graphql/{specName}` for queries/mutations.
- **Describe test scenarios** in terms of uploading, querying, and error handling for multiple schemas.
- **Mention in-memory and file-system mapping** if discussing performance or architecture.
- **For integration tests:**
  - Use `@SpringBootTest` and `@AutoConfigureMockMvc`.
  - Simulate HTTP requests for upload and query endpoints.
  - Use a test profile (e.g., SQLite) and clean up after tests.

## Example Prompts
- "Add an integration test that uploads two schemas and queries both endpoints."
- "Refactor the upload controller to reject duplicate specNames."
- "How does the system handle a query to a non-existent specName?"
- "Add a new endpoint to list all available specNames."

## Dynamic Architecture
- **Schema and database selection is dynamic**: The specName in the URL determines which schema and database are used for each request.
- **All mappings are kept in-memory for performance**, and are synchronized with the file system on startup and upload.

---
Use this file as context for all LLM-based prompt engineering and code generation for this project. 