package com.example.graphql.core.controller;

import com.example.graphql.core.service.SchemaRegistry;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GraphQLControllerTest {
    @Mock
    private SchemaRegistry schemaRegistry;
    @InjectMocks
    private GraphQLController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void returns503WhenNoSchema() {
        when(schemaRegistry.hasSchema()).thenReturn(false);
        Map<String, Object> req = new HashMap<>();
        req.put("query", "{ books { id } }");
        ResponseEntity<?> resp = controller.execute(req);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.getStatusCode());
    }

    @Test
    void returnsErrorOnException() {
        when(schemaRegistry.hasSchema()).thenReturn(true);
        when(schemaRegistry.getSchemaFile()).thenReturn(new File("/notfound.graphql"));
        Map<String, Object> req = new HashMap<>();
        req.put("query", "{ books { id } }");
        ResponseEntity<?> resp = controller.execute(req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
    }

} 