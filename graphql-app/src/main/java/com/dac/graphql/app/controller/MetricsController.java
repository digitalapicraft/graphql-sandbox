
package com.dac.graphql.app.controller;

import com.dac.graphql.core.controller.SpecUploadController;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final SpecUploadController delegate;
    private final Counter uploadAttempts;
    private final Counter uploadSuccesses;
    private final Counter uploadFailures;

    @Autowired
    public MetricsController(SpecUploadController delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.uploadAttempts = Counter.builder("schema.upload.attempts").register(meterRegistry);
        this.uploadSuccesses = Counter.builder("schema.upload.successes").register(meterRegistry);
        this.uploadFailures = Counter.builder("schema.upload.failures").register(meterRegistry);
    }

    @PostMapping("/upload-graphql-spec/{specName}")
    public ResponseEntity<String> uploadGraphqlSpecWithMetrics(@PathVariable String specName, @RequestParam("file") MultipartFile file) {
        uploadAttempts.increment();
        try {
            ResponseEntity<String> response = delegate.uploadGraphqlSpec(specName, file);
            if (response.getStatusCode().is2xxSuccessful()) {
                uploadSuccesses.increment();
            } else {
                uploadFailures.increment();
            }
            return response;
        } catch (IOException e) {
            uploadFailures.increment();
            return ResponseEntity.status(500).body("Schema upload failed: " + e.getMessage());
        }
    }
} 