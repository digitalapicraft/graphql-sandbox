package com.example.graphql.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import com.example.graphql.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.graphql.service.SchemaRegistry;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class SpecUploadController {

    @Value("${graphql.schema.upload-dir:src/main/resources/schema}")
    private String uploadDir;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaRegistry schemaRegistry;

    @PostMapping("/upload-graphql-spec")
    public ResponseEntity<String> uploadGraphqlSpec(@RequestParam("file") MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, file.getOriginalFilename());
        file.transferTo(dest);
        // Process schema and generate DB
        try {
            schemaService.processSchemaFile(dest);
            schemaRegistry.setSchemaFile(dest);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Schema upload failed: " + e.getMessage());
        }
        return ResponseEntity.ok("Schema uploaded and database generated successfully: " + dest.getAbsolutePath());
    }
} 