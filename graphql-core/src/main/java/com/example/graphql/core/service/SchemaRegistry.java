package com.example.graphql.core.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SchemaRegistry {
    private final AtomicReference<File> currentSchema = new AtomicReference<>();

    public void setSchemaFile(File file) {
        currentSchema.set(file);
    }

    public File getSchemaFile() {
        return currentSchema.get();
    }

    public boolean hasSchema() {
        return currentSchema.get() != null && currentSchema.get().exists();
    }
} 