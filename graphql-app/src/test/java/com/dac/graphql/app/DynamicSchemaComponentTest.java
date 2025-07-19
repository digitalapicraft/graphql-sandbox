package com.dac.graphql.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.AfterEach;
import java.nio.file.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
class DynamicSchemaComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void deleteDatabaseFile() throws Exception {
        Files.deleteIfExists(Paths.get("database.db"));
    }

    @Test
    void dynamicSchemaAndDataFetchers_workForCat() throws Exception {
        // First, upload a GraphQL schema with Cat type
        String schemaContent = """
            type Cat {
              id: ID!
              name: String!
              age: Int!
            }
            
            type Query {
              cats: [Cat!]!
            }
            """;

        MockMultipartFile schemaFile = new MockMultipartFile(
            "file", 
            "cat-schema.graphql", 
            "text/plain", 
            schemaContent.getBytes()
        );

        // Upload the schema
        mockMvc.perform(multipart("/api/upload-graphql-spec/cat")
                .file(schemaFile))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Schema uploaded and database generated successfully")));

        // Now test that the dynamic schema works for the Cat type
        String query = """
            {
              cats {
                id
                name
                age
              }
            }
            """;

        mockMvc.perform(post("/graphql/cat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"" + query.replace("\n", "\\n") + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.cats").isArray());
    }

    @Test
    void primaryKeyConstraint_preventsDuplicateIds() throws Exception {
        // Upload schema with Bike type
        String schemaContent = """
            type Bike {
              id: ID!
              model: String!
            }
            type Mutation {
              addBike(id: ID!, model: String!): Bike
            }
            type Query {
              bikes: [Bike!]!
            }
            """;

        MockMultipartFile schemaFile = new MockMultipartFile(
            "file",
            "bike-schema.graphql",
            "text/plain",
            schemaContent.getBytes()
        );

        mockMvc.perform(multipart("/api/upload-graphql-spec/bike")
                .file(schemaFile))
                .andExpect(status().isOk());

        // Insert a bike with id "1"
        String mutation = "mutation { addBike(id: \"1\", model: \"A\") { id model } }";
        String json = "{\"query\":\"" + mutation.replace("\"", "\\\"") + "\"}";
        mockMvc.perform(post("/graphql/bike")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addBike.id").value("1"));

        // Attempt to insert another bike with the same id "1"
        mockMvc.perform(post("/graphql/bike")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").exists());
    }
} 