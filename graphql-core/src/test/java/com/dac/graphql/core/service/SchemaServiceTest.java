package com.dac.graphql.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;
import com.dac.graphql.core.adapter.DatabaseAdapter;

class SchemaServiceTest {
    private SchemaService schemaService;
    private boolean createTableCalled = false;

    @BeforeEach
    void setUp() {
        schemaService = new SchemaService();
        schemaService.setDatabaseAdapter(new DatabaseAdapter() {
            @Override public java.sql.Connection getConnection() { return null; }
            @Override public java.util.List<java.util.Map<String, Object>> executeQuery(String sql, Object... params) { return java.util.Collections.emptyList(); }
            @Override public java.util.Map<String, Object> executeQuerySingle(String sql, Object... params) { return null; }
            @Override public int executeUpdate(String sql, Object... params) { return 0; }
            @Override public void createTable(String tableName, String columns) { 
                createTableCalled = true;
                assertEquals("Book", tableName);
                assertTrue(columns.contains("id"));
                assertTrue(columns.contains("title"));
                assertTrue(columns.contains("author"));
            }
            @Override public String mapGraphQLTypeToSql(String graphQLType) { return "TEXT"; }
            @Override public String getDatabaseType() { return "test"; }
        });
    }

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(new File("test-schema.graphql").toPath());
    }

    @Test
    void testProcessSchemaFileCreatesTable() throws Exception {
        String schema = "type Book { id: ID! title: String! author: String! }";
        File file = new File("test-schema.graphql");
        try (FileWriter fw = new FileWriter(file)) { fw.write(schema); }
        
        schemaService.processSchemaFile(file);
        
        assertTrue(createTableCalled, "createTable method should have been called");
    }

    @Test
    void testGetObjectTypes() throws Exception {
        String schema = "type Book { id: ID! title: String! author: String! }\ntype Query { books: [Book] }";
        File file = new File("test-schema.graphql");
        try (FileWriter fw = new FileWriter(file)) { fw.write(schema); }
        assertEquals(1, schemaService.getObjectTypes(file).size());
    }

    @Test
    void testGetQueryType() throws Exception {
        String schema = "type Book { id: ID! title: String! author: String! }\ntype Query { books: [Book] }";
        File file = new File("test-schema.graphql");
        try (FileWriter fw = new FileWriter(file)) { fw.write(schema); }
        assertNotNull(schemaService.getQueryType(file));
    }

    @Test
    void testGetMutationType() throws Exception {
        String schema = "type Book { id: ID! title: String! author: String! }\ntype Mutation { addBook(id: ID!, title: String!, author: String!): Book }";
        File file = new File("test-schema.graphql");
        try (FileWriter fw = new FileWriter(file)) { fw.write(schema); }
        assertNotNull(schemaService.getMutationType(file));
    }
} 