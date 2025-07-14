package com.example.graphql.core.service;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void testGetAllSchemas() {
        SchemaRegistry registry = new SchemaRegistry();
        File file = new File("test.graphql");
        registry.setSchemaFile("testspec", file);
        assertTrue(registry.getAllSchemas().containsKey("testspec"));
        assertEquals(file, registry.getAllSchemas().get("testspec"));
    }

    @Test
    void testAfterPropertiesSetInitializesSchemas() throws Exception {
        Path tempDir = Files.createTempDirectory("test-uploads");
        File tempSchema = new File(tempDir.toFile(), "testspec.graphql");
        tempSchema.createNewFile();
        SchemaRegistry registry = new SchemaRegistry();
        java.lang.reflect.Field uploadDirField = SchemaRegistry.class.getDeclaredField("uploadDir");
        uploadDirField.setAccessible(true);
        // Set uploadDir as a path relative to the current working directory
        Path relativePath = Path.of(System.getProperty("user.dir")).relativize(tempDir.toAbsolutePath());
        uploadDirField.set(registry, relativePath.toString());
        registry.afterPropertiesSet();
        assertTrue(registry.getAllSchemas().containsKey("testspec"));
        tempSchema.delete();
        tempDir.toFile().delete();
    }
} 