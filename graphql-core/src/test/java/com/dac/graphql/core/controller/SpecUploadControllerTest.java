package com.dac.graphql.core.controller;

import com.dac.graphql.core.service.SchemaRegistry;
import com.dac.graphql.core.service.SchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

class SpecUploadControllerTest {
    private MockMvc mockMvc;

    @Mock
    private SchemaService schemaService;
    @Mock
    private SchemaRegistry schemaRegistry;
    @InjectMocks
    private SpecUploadController controller;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        controller = new SpecUploadController();
        // Manually inject mocks
        java.lang.reflect.Field schemaServiceField = SpecUploadController.class.getDeclaredField("schemaService");
        schemaServiceField.setAccessible(true);
        schemaServiceField.set(controller, schemaService);
        java.lang.reflect.Field schemaRegistryField = SpecUploadController.class.getDeclaredField("schemaRegistry");
        schemaRegistryField.setAccessible(true);
        schemaRegistryField.set(controller, schemaRegistry);
        // Manually set uploadDir
        java.lang.reflect.Field uploadDirField = SpecUploadController.class.getDeclaredField("uploadDir");
        uploadDirField.setAccessible(true);
        uploadDirField.set(controller, "test-uploads");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void uploadGraphqlSpec_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "schema.graphql", MediaType.TEXT_PLAIN_VALUE, "type Query { hello: String }".getBytes());
        mockMvc.perform(multipart("/api/upload-graphql-spec/testspec").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void uploadGraphqlSpec_failure() throws Exception {
        doThrow(new RuntimeException("fail")).when(schemaService).processSchemaFile(any(File.class));
        MockMultipartFile file = new MockMultipartFile("file", "schema.graphql", MediaType.TEXT_PLAIN_VALUE, "type Query { hello: String }".getBytes());
        mockMvc.perform(multipart("/api/upload-graphql-spec/testspec").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Schema upload failed")));
    }
} 