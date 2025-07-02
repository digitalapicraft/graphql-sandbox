package com.example.graphql.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

class SchemaServiceTest {
    private static final String DB_URL = "jdbc:sqlite:test-database.db";

    @AfterEach
    void cleanup() throws Exception {
        Files.deleteIfExists(new File("test-database.db").toPath());
    }

    @Test
    void testProcessSchemaFileCreatesTable() throws Exception {
        String schema = "type Book { id: ID! title: String! author: String! }";
        File file = new File("test-schema.graphql");
        try (FileWriter fw = new FileWriter(file)) { fw.write(schema); }
        SchemaService service = new SchemaService();
        service.processSchemaFile(file);
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Book'")) {
            assertTrue(rs.next());
            assertEquals("Book", rs.getString(1));
        }
        file.delete();
    }
} 