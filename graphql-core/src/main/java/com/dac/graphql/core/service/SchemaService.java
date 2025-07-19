package com.dac.graphql.core.service;

import graphql.language.Document;
import graphql.language.ObjectTypeDefinition;
import graphql.parser.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import com.dac.graphql.core.adapter.DatabaseAdapter;
import java.sql.SQLException;

@Service
public class SchemaService {
    
    @Autowired
    private DatabaseAdapter databaseAdapter;

    public void processSchemaFile(File schemaFile) throws IOException, SQLException {
        String schema = Files.readString(schemaFile.toPath());
        Parser parser = new Parser();
        Document document = parser.parseDocument(schema);
        List<ObjectTypeDefinition> types = document.getDefinitionsOfType(ObjectTypeDefinition.class);
        for (ObjectTypeDefinition type : types) {
            if (type.getName().equals("Query") || type.getName().equals("Mutation")) continue;
            String columns = type.getFieldDefinitions().stream()
                    .map(field -> field.getName() + " " + databaseAdapter.mapGraphQLTypeToSql(field.getType()))
                    .collect(Collectors.joining(", "));
            databaseAdapter.createTable(type.getName(), columns);
        }
    }



    // --- Schema Parsing Helpers for Dynamic GraphQL ---
    public List<ObjectTypeDefinition> getObjectTypes(File schemaFile) throws IOException {
        String schema = Files.readString(schemaFile.toPath());
        Parser parser = new Parser();
        Document document = parser.parseDocument(schema);
        return document.getDefinitionsOfType(ObjectTypeDefinition.class).stream()
                .filter(type -> !type.getName().equals("Query") && !type.getName().equals("Mutation"))
                .collect(Collectors.toList());
    }

    public ObjectTypeDefinition getQueryType(File schemaFile) throws IOException {
        String schema = Files.readString(schemaFile.toPath());
        Parser parser = new Parser();
        Document document = parser.parseDocument(schema);
        return document.getDefinitionsOfType(ObjectTypeDefinition.class).stream()
                .filter(type -> type.getName().equals("Query"))
                .findFirst().orElse(null);
    }

    public ObjectTypeDefinition getMutationType(File schemaFile) throws IOException {
        String schema = Files.readString(schemaFile.toPath());
        Parser parser = new Parser();
        Document document = parser.parseDocument(schema);
        return document.getDefinitionsOfType(ObjectTypeDefinition.class).stream()
                .filter(type -> type.getName().equals("Mutation"))
                .findFirst().orElse(null);
    }

    // Setter for test injection
    void setDatabaseAdapter(com.dac.graphql.core.adapter.DatabaseAdapter adapter) {
        this.databaseAdapter = adapter;
    }
} 