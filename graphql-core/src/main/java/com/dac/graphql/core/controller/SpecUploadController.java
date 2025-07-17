package com.dac.graphql.core.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.dac.graphql.core.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import com.dac.graphql.core.service.SchemaRegistry;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Spec Upload", description = "Endpoints for uploading and managing GraphQL schema specifications.")
@RestController
@RequestMapping("/api")
public class SpecUploadController {

    @Value("${graphql.schema.upload-dir:uploaded-schemas}")
    private String uploadDir;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaRegistry schemaRegistry;

    @Operation(
        summary = "Upload a GraphQL schema specification file",
        description = "Uploads a GraphQL schema file and generates the corresponding database schema.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Schema uploaded and database generated successfully."),
            @ApiResponse(responseCode = "500", description = "Schema upload failed.")
        }
    )
    @PostMapping(value = "/upload-graphql-spec/{specName}", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadGraphqlSpec(
            @Parameter(description = "The name to register the uploaded schema under.", required = true)
            @PathVariable String specName,
            @Parameter(description = "The GraphQL schema file to upload.", required = true, schema = @Schema(type = "string", format = "binary"))
            @RequestParam("file") MultipartFile file) throws IOException {
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