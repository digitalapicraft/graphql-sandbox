package com.example.graphql.service;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class SchemaRegistryTest {
    @Test
    void testSetAndGetSchemaFile() {
        SchemaRegistry registry = new SchemaRegistry();
        File file = new File("test.graphql");
        registry.setSchemaFile(file);
        assertEquals(file, registry.getSchemaFile());
    }

    @Test
    void testHasSchema() {
        SchemaRegistry registry = new SchemaRegistry();
        assertFalse(registry.hasSchema());
        File file = new File("test.graphql");
        registry.setSchemaFile(file);
        assertFalse(registry.hasSchema()); // file does not exist
        try {
            file.createNewFile();
            assertTrue(registry.hasSchema());
        } catch (Exception e) {
            // ignore
        } finally {
            file.delete();
        }
    }
} 