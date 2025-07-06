# Multi-Module Refactor: GraphQL Sandbox

## Multi-Module Structure

- **Parent POM**  
  - Now at the root, with `<packaging>pom</packaging>` and modules:  
    - `graphql-core`
    - `sqlite-adapter`
    - `postgres-adapter`
    - `graphql-app`

- **graphql-core**  
  - Contains all core logic, controllers, services, and the new `DatabaseAdapter` interface.
  - All code is now under `com.example.graphql.core`.
  - Tests for core logic are in `graphql-core/src/test/java/com/example/graphql/core`.
  - **Packaged as a regular JAR** (not a Spring Boot fat JAR) to avoid dependency issues.

- **graphql-app**  
  - Contains the executable Spring Boot application.
  - Houses the main application class `GraphqlServerApplication`.
  - **Packaged as a Spring Boot fat JAR** with all dependencies.
  - Contains integration tests that require the full Spring Boot context.

- **sqlite-adapter**  
  - Contains `SqliteAdapter` implementing `DatabaseAdapter`.
  - Only SQLite-specific dependencies.
  - Uses `@Profile("sqlite")` for conditional bean creation.

- **postgres-adapter**  
  - Contains `PostgresAdapter` implementing `DatabaseAdapter`.
  - Only PostgreSQL-specific dependencies.
  - Uses `@Profile("postgres")` for conditional bean creation.

---

## Why Separate graphql-core and graphql-app?

### The Problem with Single Module
Initially, we had all code in one module that was packaged as a Spring Boot fat JAR. This caused issues:
- **Dependency Access**: Adapter modules couldn't access classes from the core module because they were inside a Spring Boot fat JAR structure (`BOOT-INF/classes/`)
- **Circular Dependencies**: If we made core depend on adapters, it would create circular dependencies
- **Classpath Issues**: Maven couldn't resolve dependencies properly when core was a fat JAR

### The Solution: Module Separation
We separated the concerns into two modules:

#### graphql-core (Library Module)
- **Purpose**: Contains all business logic, interfaces, and core functionality
- **Packaging**: Regular JAR (not fat JAR)
- **Dependencies**: Only Spring Boot starters, no adapter dependencies
- **Benefits**: 
  - Clean separation of concerns
  - Can be used as a library by other projects
  - No Spring Boot application context overhead
  - Easy to test individual components

#### graphql-app (Application Module)
- **Purpose**: Executable Spring Boot application
- **Packaging**: Spring Boot fat JAR with all dependencies
- **Dependencies**: All modules (core + adapters)
- **Benefits**:
  - Single executable JAR with all dependencies
  - Proper Spring Boot application lifecycle
  - Integration tests with full application context
  - Easy deployment and distribution

### Dependency Flow
```
graphql-app (executable)
├── graphql-core (library)
├── sqlite-adapter (library)
└── postgres-adapter (library)
```

This structure ensures:
- **No circular dependencies**
- **Clean separation of concerns**
- **Proper dependency resolution**
- **Easy testing and deployment**

## Key Refactor Points

- **DatabaseAdapter interface**  
  - Defines all DB operations needed by the core.
  - Core code (controllers, services) now depend on this interface, not on any specific DB.

- **Adapter Implementations**  
  - `SqliteAdapter` and `PostgresAdapter` implement all methods for their respective DBs.
  - Registered as Spring beans, activated by profile.

- **Configuration**  
  - `app.database.type` property in `application.properties` (default: `sqlite`).
  - `spring.profiles.active` is set from this property, so switching DBs is as simple as changing the property.

- **No Cyclic Dependencies**  
  - Core does not depend on adapters.
  - Adapters depend on core.
  - No circular references.

- **Tests**  
  - Core tests remain in `graphql-core`.
  - Adapter-specific tests can be added in their respective modules (none present yet, but structure supports it).

- **Spring Boot Main Class**  
  - Now in `graphql-core` as `com.example.graphql.core.GraphqlServerApplication`.

---

## How to Switch Databases

- In `graphql-core/src/main/resources/application.properties`, set:
  ```
  app.database.type=sqlite   # or postgres
  ```
- Or override with `SPRING_PROFILES_ACTIVE=postgres` as an environment variable.

---

## What's Left/Recommended

- **Move any SQLite-specific logic from core to the adapter** (if any remains).
- **Add adapter-specific tests** in `sqlite-adapter` and `postgres-adapter` if you want to test those classes directly.
- **Verify**: Run `./mvnw compile` and `./mvnw test` to ensure everything builds and tests pass.
- **Documentation**: Update your README to explain the new structure and how to switch DBs.

---

## Summary Table

| Module            | Depends On      | Contains                        | DB-specific? | Tests?         | Packaging     |
|-------------------|-----------------|----------------------------------|--------------|---------------|---------------|
| graphql-core      | —               | Core logic, interfaces, services | No           | Yes           | Regular JAR   |
| sqlite-adapter    | graphql-core    | SqliteAdapter                    | Yes          | (Addable)     | Regular JAR   |
| postgres-adapter  | graphql-core    | PostgresAdapter                  | Yes          | (Addable)     | Regular JAR   |
| graphql-app       | All modules     | Executable application           | No           | Yes           | Fat JAR       |

---

**You are now ready to build and test the new structure!**

Would you like to proceed with a build/test, or do you want to review any specific part of the code or structure in more detail? 