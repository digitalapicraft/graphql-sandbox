package com.example.graphql.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.graphql.core.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.graphql.core.service.SchemaRegistry;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class SpecUploadController {

    @Value("${graphql.schema.upload-dir:uploaded-schemas}")
    private String uploadDir;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaRegistry schemaRegistry;

    @PostMapping("/upload-graphql-spec/{specName}")
    public ResponseEntity<String> uploadGraphqlSpec(@PathVariable String specName, @RequestParam("file") MultipartFile file) throws IOException {
        String basePath = System.getProperty("user.dir");
        File dir = new File(basePath, uploadDir);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, specName + ".graphql");
        file.transferTo(dest);
        // Process schema and generate DB
        try {
            schemaService.processSchemaFile(dest);
            schemaRegistry.setSchemaFile(specName, dest);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Schema upload failed: " + e.getMessage());
        }
        return ResponseEntity.ok("Schema uploaded and database generated successfully: " + dest.getAbsolutePath());
    }

    @GetMapping("/graphql-specs")
    public ResponseEntity<?> listRegisteredSpecs() {
        return ResponseEntity.ok(schemaRegistry.getAllSchemas().keySet());
    }

    @GetMapping("/graphql-specs/{specName}")
    public ResponseEntity<?> getSpecFile(@PathVariable String specName) throws IOException {
        if (!schemaRegistry.hasSchema(specName)) {
            return ResponseEntity.status(404).body("Spec not found: " + specName);
        }
        File file = schemaRegistry.getSchemaFile(specName);
        String content = java.nio.file.Files.readString(file.toPath());
        return ResponseEntity.ok().header("Content-Type", "text/plain").body(content);
    }
} 