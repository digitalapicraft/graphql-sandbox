package com.example.graphql.controller;

import com.example.graphql.service.SchemaRegistry;
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

    @Test
    void returnsBooksQueryResult() throws Exception {
        // Prepare schema file
        File schemaFile = File.createTempFile("book-schema", ".graphql");
        schemaFile.deleteOnExit();
        try (FileWriter fw = new FileWriter(schemaFile)) {
            fw.write("""
                type Book {\n  id: ID!\n  title: String!\n  author: String!\n}\n\ntype Query {\n  books: [Book]\n  book(id: ID!): Book\n}\n\ntype Mutation {\n  addBook(id: ID!, title: String!, author: String!): Book\n}\n""");
        }
        // Prepare SQLite DB
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS Book");
            stmt.executeUpdate("CREATE TABLE Book (id TEXT PRIMARY KEY, title TEXT, author TEXT)");
            stmt.executeUpdate("INSERT INTO Book (id, title, author) VALUES ('1', 'Book One', 'Author A')");
        }
        when(schemaRegistry.hasSchema()).thenReturn(true);
        when(schemaRegistry.getSchemaFile()).thenReturn(schemaFile);
        Map<String, Object> req = new HashMap<>();
        req.put("query", "{ books { id title author } }");
        ResponseEntity<?> resp = controller.execute(req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("data"));
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertTrue(data.containsKey("books"));
        var books = (java.util.List<?>) data.get("books");
        assertFalse(books.isEmpty());
        Map<String, Object> book = (Map<String, Object>) books.get(0);
        assertEquals("1", book.get("id"));
        assertEquals("Book One", book.get("title"));
        assertEquals("Author A", book.get("author"));
    }

    @Test
    void addBookMutationWorks() throws Exception {
        // Prepare schema file
        File schemaFile = File.createTempFile("book-schema", ".graphql");
        schemaFile.deleteOnExit();
        try (FileWriter fw = new FileWriter(schemaFile)) {
            fw.write("""
                type Book {\n  id: ID!\n  title: String!\n  author: String!\n}\n\ntype Query {\n  books: [Book]\n  book(id: ID!): Book\n}\n\ntype Mutation {\n  addBook(id: ID!, title: String!, author: String!): Book\n}\n""");
        }
        // Prepare SQLite DB
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS Book");
            stmt.executeUpdate("CREATE TABLE Book (id TEXT PRIMARY KEY, title TEXT, author TEXT)");
        }
        when(schemaRegistry.hasSchema()).thenReturn(true);
        when(schemaRegistry.getSchemaFile()).thenReturn(schemaFile);
        Map<String, Object> req = new HashMap<>();
        req.put("query", "mutation { addBook(id: \"2\", title: \"Book Two\", author: \"Author B\") { id title author } }");
        ResponseEntity<?> resp = controller.execute(req);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("data"));
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertTrue(data.containsKey("addBook"));
        Map<String, Object> book = (Map<String, Object>) data.get("addBook");
        assertEquals("2", book.get("id"));
        assertEquals("Book Two", book.get("title"));
        assertEquals("Author B", book.get("author"));
        // Verify book is in DB
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:database.db");
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Book WHERE id = ?")) {
            stmt.setString(1, "2");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("Book Two", rs.getString("title"));
                assertEquals("Author B", rs.getString("author"));
            }
        }
    }
} 