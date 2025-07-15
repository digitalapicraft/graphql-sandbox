# How to Generate OpenAPI Specs for Admin APIs

This project uses [springdoc-openapi](https://springdoc.org/) to automatically generate OpenAPI (Swagger) documentation for all REST endpoints, including admin APIs.

## 1. Add the Dependency

Make sure your `graphql-app/pom.xml` includes:

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.5.0</version>
</dependency>
```

## 2. Start the Application

Start your Spring Boot app (with the appropriate profile, e.g. sqlite):

```bash
./mvnw spring-boot:run -pl graphql-app -Dspring-boot.run.profiles=sqlite
```

## 3. Access the OpenAPI Spec

Once the app is running, you can access the OpenAPI JSON spec at:

```
http://localhost:8080/v3/api-docs
```

To download it:

```bash
curl http://localhost:8080/v3/api-docs -o openapi-admin.json
```

## 4. View the Swagger UI

You can also view and interact with the API documentation in your browser:

```
http://localhost:8080/swagger-ui.html
```

## 5. Notes
- The OpenAPI spec will include all REST endpoints, including:
  - `POST /api/upload-graphql-spec/{specName}`
  - `GET /api/graphql-specs`
  - `GET /api/graphql-specs/{specName}`
- You can use the generated `openapi-admin.json` for API client generation, documentation, or integration with other tools. 