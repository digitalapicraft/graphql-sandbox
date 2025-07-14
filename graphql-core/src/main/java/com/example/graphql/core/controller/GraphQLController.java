package com.example.graphql.core.controller;

import com.example.graphql.core.service.SchemaRegistry;
import com.example.graphql.core.service.SchemaService;
import com.example.graphql.core.adapter.DatabaseAdapter;
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

    @Autowired
    private DatabaseAdapter databaseAdapter;

    // Cache GraphQL instances per specName
    private final Map<String, GraphQL> graphQLMap = new HashMap<>();
    private final Map<String, File> lastSchemaFileMap = new HashMap<>();

    @PostMapping("/{specName}")
    public ResponseEntity<?> execute(@PathVariable String specName, @RequestBody Map<String, Object> request) {
        if (!schemaRegistry.hasSchema(specName)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("GraphQL schema not found for spec: " + specName);
        }
        File schemaFile = schemaRegistry.getSchemaFile(specName);
        try {
            GraphQL graphQL = graphQLMap.get(specName);
            File lastSchemaFile = lastSchemaFileMap.get(specName);
            if (graphQL == null || !schemaFile.equals(lastSchemaFile)) {
                // (Re)build GraphQL instance if schema changed
                SchemaParser parser = new SchemaParser();
                TypeDefinitionRegistry typeRegistry = parser.parse(new FileReader(schemaFile));
                RuntimeWiring wiring = buildDynamicWiring(schemaFile);
                SchemaGenerator generator = new SchemaGenerator();
                graphQL = GraphQL.newGraphQL(generator.makeExecutableSchema(typeRegistry, wiring)).build();
                graphQLMap.put(specName, graphQL);
                lastSchemaFileMap.put(specName, schemaFile);
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
            try {
                if (env.getArguments().isEmpty()) {
                    // Fetch all
                    String sql = "SELECT * FROM " + tableName;
                    System.out.println("[GraphQL] SQL: " + sql);
                    return databaseAdapter.executeQuery(sql);
                } else {
                    // Fetch by ID (assume first argument is ID)
                    String idArg = env.getArguments().keySet().iterator().next();
                    Object idVal = env.getArgument(idArg);
                    String sql = "SELECT * FROM " + tableName + " WHERE " + idArg + " = ?";
                    System.out.println("[GraphQL] SQL: " + sql + ", idVal: " + idVal);
                    return databaseAdapter.executeQuerySingle(sql, idVal);
                }
            } catch (Exception e) {
                System.err.println("[GraphQL] SQL Error: " + e.getMessage());
                throw new RuntimeException("SQL error querying table '" + tableName + "': " + e.getMessage());
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
            try {
                if (op.startsWith("add")) {
                    // Insert
                    String columns = String.join(", ", args.keySet());
                    String placeholders = String.join(", ", args.keySet().stream().map(k -> "?").toArray(String[]::new));
                    String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
                    databaseAdapter.executeUpdate(sql, args.values().toArray());
                    return args;
                } else if (op.startsWith("update")) {
                    // Update by ID (assume first arg is ID)
                    String idArg = args.keySet().iterator().next();
                    Object idVal = args.get(idArg);
                    String setClause = String.join(", ", args.keySet().stream().filter(k -> !k.equals(idArg)).map(k -> k + " = ?").toArray(String[]::new));
                    String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + idArg + " = ?";
                    Object[] params = new Object[args.size()];
                    int idx = 0;
                    for (String key : args.keySet()) {
                        if (!key.equals(idArg)) params[idx++] = args.get(key);
                    }
                    params[idx] = idVal;
                    databaseAdapter.executeUpdate(sql, params);
                    return args;
                } else if (op.startsWith("delete")) {
                    // Delete by ID (assume first arg is ID)
                    String idArg = args.keySet().iterator().next();
                    Object idVal = args.get(idArg);
                    String sql = "DELETE FROM " + tableName + " WHERE " + idArg + " = ?";
                    databaseAdapter.executeUpdate(sql, idVal);
                    return Map.of(idArg, idVal);
                }
            } catch (Exception e) {
                System.err.println("[GraphQL] SQL Error: " + e.getMessage());
                throw new RuntimeException("SQL error in mutation '" + fieldName + "': " + e.getMessage());
            }
            return null;
        };
    }
} 