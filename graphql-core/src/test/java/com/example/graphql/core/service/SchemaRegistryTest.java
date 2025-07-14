package com.example.graphql.core.service;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class SchemaRegistryTest {
    @Test
    void testSetAndGetSchemaFile() {
        SchemaRegistry registry = new SchemaRegistry();
        File file = new File("test.graphql");
        registry.setSchemaFile("testspec", file);
        assertEquals(file, registry.getSchemaFile("testspec"));
    }

    @Test
    void testHasSchema() {
        SchemaRegistry registry = new SchemaRegistry();
        assertFalse(registry.hasSchema("testspec"));
        File file = new File("test.graphql");
        registry.setSchemaFile("testspec", file);
        assertFalse(registry.hasSchema("testspec")); // file does not exist
        try {
            file.createNewFile();
            assertTrue(registry.hasSchema("testspec"));
        } catch (Exception e) {
            // ignore
        } finally {
            file.delete();
        }
    }
} 