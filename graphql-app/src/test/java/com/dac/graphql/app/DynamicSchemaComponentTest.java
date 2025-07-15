package com.dac.graphql.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sqlite")
class DynamicSchemaComponentTest {

    @Autowired
    private MockMvc mockMvc;

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
} 