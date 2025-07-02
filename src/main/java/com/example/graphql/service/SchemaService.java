package com.example.graphql.service;

import graphql.language.Document;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.parser.Parser;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class SchemaService {
    private static final String DB_URL = "jdbc:sqlite:database.db";

    public void processSchemaFile(File schemaFile) throws IOException, SQLException {
        String schema = Files.readString(schemaFile.toPath());
        Parser parser = new Parser();
        Document document = parser.parseDocument(schema);
        List<ObjectTypeDefinition> types = document.getDefinitionsOfType(ObjectTypeDefinition.class);
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            for (ObjectTypeDefinition type : types) {
                if (type.getName().equals("Query") || type.getName().equals("Mutation")) continue;
                String createTableSql = buildCreateTableSql(type);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableSql);
                }
            }
        }
    }

    private String buildCreateTableSql(ObjectTypeDefinition type) {
        String tableName = type.getName();
        String columns = type.getFieldDefinitions().stream()
                .map(field -> field.getName() + " " + mapGraphQLTypeToSql(field.getType().toString()))
                .collect(Collectors.joining(", "));
        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ");";
    }

    private String mapGraphQLTypeToSql(String graphQLType) {
        switch (graphQLType.replace("!", "")) {
            case "Int": return "INTEGER";
            case "Float": return "REAL";
            case "Boolean": return "BOOLEAN";
            case "ID": return "TEXT PRIMARY KEY";
            case "String":
            default: return "TEXT";
        }
    }
} 