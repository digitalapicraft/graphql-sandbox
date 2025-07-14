package com.example.graphql.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.InitializingBean;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SchemaRegistry implements InitializingBean {
    @Value("${graphql.schema.upload-dir:uploaded-schemas}")
    private String uploadDir;

    // In-memory map: specName -> schema file
    private final Map<String, File> schemaMap = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() {
        File dir = new File(System.getProperty("user.dir"), uploadDir);
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".graphql"));
        if (files != null) {
            for (File file : files) {
                String specName = file.getName().replaceFirst("\\.graphql$", "");
                schemaMap.put(specName, file);
            }
        }
    }

    public void setSchemaFile(String specName, File file) {
        schemaMap.put(specName, file);
    }

    public File getSchemaFile(String specName) {
        return schemaMap.get(specName);
    }

    public boolean hasSchema(String specName) {
        File file = schemaMap.get(specName);
        return file != null && file.exists();
    }

    public Map<String, File> getAllSchemas() {
        return schemaMap;
    }
} 