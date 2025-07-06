package com.example.graphql.sqlite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SqliteAdapterTest {
    private SqliteAdapter adapter;
    private Connection sharedConnection;

    @BeforeEach
    void setUp() throws SQLException {
        sharedConnection = DriverManager.getConnection("jdbc:sqlite::memory:");
        adapter = new SqliteAdapter(() -> sharedConnection);
    }

    @Test
    void testGetDatabaseType() {
        assertEquals("sqlite", adapter.getDatabaseType());
    }

    @Test
    void testMapGraphQLTypeToSql() {
        assertEquals("INTEGER", adapter.mapGraphQLTypeToSql("Int"));
        assertEquals("REAL", adapter.mapGraphQLTypeToSql("Float"));
        assertEquals("BOOLEAN", adapter.mapGraphQLTypeToSql("Boolean"));
        assertEquals("TEXT PRIMARY KEY", adapter.mapGraphQLTypeToSql("ID"));
        assertEquals("TEXT", adapter.mapGraphQLTypeToSql("String"));
        assertEquals("TEXT", adapter.mapGraphQLTypeToSql("CustomType"));
    }

    @Test
    void testCreateTableAndQuery() throws SQLException {
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        int updated = adapter.executeUpdate("INSERT INTO TestTable (id, name) VALUES (?, ?)", 1, "Alice");
        assertEquals(1, updated);
        List<Map<String, Object>> results = adapter.executeQuery("SELECT * FROM TestTable WHERE id = ?", 1);
        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).get("name"));
        Map<String, Object> single = adapter.executeQuerySingle("SELECT * FROM TestTable WHERE id = ?", 1);
        assertNotNull(single);
        assertEquals("Alice", single.get("name"));
    }
} 