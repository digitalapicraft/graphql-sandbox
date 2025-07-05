package com.example.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DynamicSchemaComponentTest {
    @LocalServerPort
    int port;
    @Autowired
    TestRestTemplate restTemplate;
    @Autowired
    MockMvc mockMvc;

    @Test
    void dynamicSchemaAndDataFetchers_workForCat() throws Exception {
        // 1. Upload Cat schema using MockMvc
        String catSchema = """
            type Cat {\n  id: ID!\n  name: String!\n  age: Int!\n}\n\ntype Query {\n  cats: [Cat]\n  cat(id: ID!): Cat\n}\n\ntype Mutation {\n  addCat(id: ID!, name: String!, age: Int!): Cat\n  updateCat(id: ID!, name: String, age: Int): Cat\n  deleteCat(id: ID!): Cat\n}\n""";
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "cat-schema.graphql",
            "text/plain",
            catSchema.getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/upload-graphql-spec")
                .file(file))
                .andExpect(status().isOk());

        // 2. Add a Cat using GraphQL mutation
        String addCatMutation = "mutation { addCat(id: \"1\", name: \"Whiskers\", age: 3) { id name age } }";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> addReq = new HttpEntity<>(Map.of("query", addCatMutation), headers);
        ResponseEntity<String> addResp = restTemplate.postForEntity("http://localhost:" + port + "/graphql", addReq, String.class);
        assertEquals(HttpStatus.OK, addResp.getStatusCode());
        assertTrue(addResp.getBody().contains("Whiskers"));

        // 3. Query all Cats
        String catsQuery = "{ cats { id name age } }";
        HttpEntity<Map<String, String>> queryReq = new HttpEntity<>(Map.of("query", catsQuery), headers);
        ResponseEntity<String> queryResp = restTemplate.postForEntity("http://localhost:" + port + "/graphql", queryReq, String.class);
        assertEquals(HttpStatus.OK, queryResp.getStatusCode());
        assertTrue(queryResp.getBody().contains("Whiskers"));
    }
} 