package com.example.graphql.controller;

import com.example.graphql.service.SchemaRegistry;
import com.example.graphql.service.SchemaService;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
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
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.language.ListType;

@RestController
@RequestMapping("/graphql")
public class GraphQLController {
    @Autowired
    private SchemaRegistry schemaRegistry;

    @Autowired
    private SchemaService schemaService;

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
                RuntimeWiring wiring = buildDynamicWiring(schemaFile);
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

    // --- Dynamic Wiring and Generic Data Fetchers ---
    private RuntimeWiring buildDynamicWiring(File schemaFile) throws Exception {
        RuntimeWiring.Builder wiringBuilder = RuntimeWiring.newRuntimeWiring();
        // Wire Query fields
        ObjectTypeDefinition queryType = schemaService.getQueryType(schemaFile);
        if (queryType != null) {
            TypeRuntimeWiring.Builder queryWiring = TypeRuntimeWiring.newTypeWiring("Query");
            for (FieldDefinition field : queryType.getFieldDefinitions()) {
                queryWiring.dataFetcher(field.getName(), genericQueryFetcher(field));
            }
            wiringBuilder.type(queryWiring);
        }
        // Wire Mutation fields
        ObjectTypeDefinition mutationType = schemaService.getMutationType(schemaFile);
        if (mutationType != null) {
            TypeRuntimeWiring.Builder mutationWiring = TypeRuntimeWiring.newTypeWiring("Mutation");
            for (FieldDefinition field : mutationType.getFieldDefinitions()) {
                mutationWiring.dataFetcher(field.getName(), genericMutationFetcher(field));
            }
            wiringBuilder.type(mutationWiring);
        }
        return wiringBuilder.build();
    }

    private DataFetcher<Object> genericQueryFetcher(FieldDefinition field) {
        return env -> {
            // Extract the base type name (e.g., User) from the field's type using AST
            String tableName = getBaseTypeName(field.getType());
            // Debug: print the SQL and table name
            System.out.println("[GraphQL] Querying table: " + tableName + ", field: " + field.getName() + ", args: " + env.getArguments());
            if (env.getArguments().isEmpty()) {
                // Fetch all
                String sql = "SELECT * FROM " + tableName;
                System.out.println("[GraphQL] SQL: " + sql);
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    ArrayList<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        results.add(row);
                    }
                    return results;
                } catch (SQLException e) {
                    System.err.println("[GraphQL] SQL Error: " + e.getMessage());
                    throw new RuntimeException("SQL error querying table '" + tableName + "': " + e.getMessage());
                }
            } else {
                // Fetch by ID (assume first argument is ID)
                String idArg = env.getArguments().keySet().iterator().next();
                Object idVal = env.getArgument(idArg);
                String sql = "SELECT * FROM " + tableName + " WHERE " + idArg + " = ?";
                System.out.println("[GraphQL] SQL: " + sql + ", idVal: " + idVal);
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, idVal);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            ResultSetMetaData meta = rs.getMetaData();
                            for (int i = 1; i <= meta.getColumnCount(); i++) {
                                row.put(meta.getColumnName(i), rs.getObject(i));
                            }
                            return row;
                        }
                    }
                    return null;
                } catch (SQLException e) {
                    System.err.println("[GraphQL] SQL Error: " + e.getMessage());
                    throw new RuntimeException("SQL error querying table '" + tableName + "': " + e.getMessage());
                }
            }
        };
    }

    // Helper to extract the base type name from a GraphQL Type
    private String getBaseTypeName(Type<?> type) {
        if (type instanceof TypeName) {
            return ((TypeName) type).getName();
        } else if (type instanceof ListType) {
            return getBaseTypeName(((ListType) type).getType());
        } else if (type instanceof graphql.language.NonNullType) {
            return getBaseTypeName(((graphql.language.NonNullType) type).getType());
        }
        return type.toString(); // fallback
    }

    private DataFetcher<Object> genericMutationFetcher(FieldDefinition field) {
        return env -> {
            String fieldName = field.getName();
            // For simplicity, assume mutation is addX, updateX, or deleteX
            // and arguments match table columns
            String op = fieldName.toLowerCase();
            String typeName = op.replaceAll("^(add|update|delete)", "");
            if (typeName.isEmpty() && field.getType() != null) {
                typeName = field.getType().toString().replace("!", "");
            }
            String tableName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
            Map<String, Object> args = env.getArguments();
            if (op.startsWith("add")) {
                // Insert
                String columns = String.join(", ", args.keySet());
                String placeholders = String.join(", ", args.keySet().stream().map(k -> "?").toArray(String[]::new));
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                     PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")")) {
                    int idx = 1;
                    for (String key : args.keySet()) {
                        stmt.setObject(idx++, args.get(key));
                    }
                    stmt.executeUpdate();
                }
                return args;
            } else if (op.startsWith("update")) {
                // Update by ID (assume first arg is ID)
                String idArg = args.keySet().iterator().next();
                Object idVal = args.get(idArg);
                String setClause = String.join(", ", args.keySet().stream().filter(k -> !k.equals(idArg)).map(k -> k + " = ?").toArray(String[]::new));
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                     PreparedStatement stmt = conn.prepareStatement("UPDATE " + tableName + " SET " + setClause + " WHERE " + idArg + " = ?")) {
                    int idx = 1;
                    for (String key : args.keySet()) {
                        if (!key.equals(idArg)) stmt.setObject(idx++, args.get(key));
                    }
                    stmt.setObject(idx, idVal);
                    stmt.executeUpdate();
                }
                return args;
            } else if (op.startsWith("delete")) {
                // Delete by ID (assume first arg is ID)
                String idArg = args.keySet().iterator().next();
                Object idVal = args.get(idArg);
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + tableName + " WHERE " + idArg + " = ?")) {
                    stmt.setObject(1, idVal);
                    stmt.executeUpdate();
                }
                return Map.of(idArg, idVal);
            }
            return null;
        };
    }
} 