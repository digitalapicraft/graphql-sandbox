package com.example.graphql.controller;

import com.example.graphql.service.SchemaRegistry;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/graphql")
public class GraphQLController {
    @Autowired
    private SchemaRegistry schemaRegistry;

    private GraphQL graphQL;
    private File lastSchemaFile;

    @PostMapping
    public ResponseEntity<?> execute(@RequestBody Map<String, Object> request) {
        if (!schemaRegistry.hasSchema()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("GraphQL schema not uploaded. Please upload a schema first.");
        }
        File schemaFile = schemaRegistry.getSchemaFile();
        try {
            if (graphQL == null || !schemaFile.equals(lastSchemaFile)) {
                // (Re)build GraphQL instance if schema changed
                SchemaParser parser = new SchemaParser();
                TypeDefinitionRegistry typeRegistry = parser.parse(new FileReader(schemaFile));
                RuntimeWiring wiring = RuntimeWiring.newRuntimeWiring()
                    .type(TypeRuntimeWiring.newTypeWiring("Query")
                        .dataFetcher("books", booksFetcher())
                        .dataFetcher("book", bookFetcher())
                    )
                    .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                        .dataFetcher("addBook", addBookFetcher())
                    )
                    .build();
                SchemaGenerator generator = new SchemaGenerator();
                graphQL = GraphQL.newGraphQL(generator.makeExecutableSchema(typeRegistry, wiring)).build();
                lastSchemaFile = schemaFile;
            }
            String query = (String) request.get("query");
            Map<String, Object> variables = (Map<String, Object>) request.getOrDefault("variables", null);
            ExecutionInput input = ExecutionInput.newExecutionInput()
                    .query(query)
                    .variables(variables != null ? variables : Map.of())
                    .build();
            ExecutionResult result = graphQL.execute(input);
            return ResponseEntity.ok(result.toSpecification());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // --- Example DataFetchers for Book ---
    private DataFetcher<Object> booksFetcher() {
        return env -> {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM Book")) {
                ArrayList<Map<String, Object>> books = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> book = new HashMap<>();
                    book.put("id", rs.getString("id"));
                    book.put("title", rs.getString("title"));
                    book.put("author", rs.getString("author"));
                    books.add(book);
                }
                return books;
            }
        };
    }

    private DataFetcher<Object> bookFetcher() {
        return env -> {
            String id = env.getArgument("id");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Book WHERE id = ?")) {
                stmt.setString(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> book = new HashMap<>();
                        book.put("id", rs.getString("id"));
                        book.put("title", rs.getString("title"));
                        book.put("author", rs.getString("author"));
                        return book;
                    }
                }
                return null;
            }
        };
    }

    private DataFetcher<Object> addBookFetcher() {
        return env -> {
            String id = env.getArgument("id");
            String title = env.getArgument("title");
            String author = env.getArgument("author");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO Book (id, title, author) VALUES (?, ?, ?)")) {
                stmt.setString(1, id);
                stmt.setString(2, title);
                stmt.setString(3, author);
                stmt.executeUpdate();
            }
            // Return the created book
            Map<String, Object> book = new HashMap<>();
            book.put("id", id);
            book.put("title", title);
            book.put("author", author);
            return book;
        };
    }
} 