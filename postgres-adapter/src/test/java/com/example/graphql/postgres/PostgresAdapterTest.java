package com.example.graphql.postgres;

import com.example.graphql.core.adapter.ConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PostgresAdapterTest {
    private PostgresAdapter adapter;

    @BeforeEach
    void setUp() {
        // Provide a dummy connection provider for test
        adapter = new PostgresAdapter(() -> null);
    }

    @Test
    void testGetDatabaseType() {
        assertEquals("postgres", adapter.getDatabaseType());
    }

    @Test
    void testMapGraphQLTypeToSql() {
        assertEquals("text", adapter.mapGraphQLTypeToSql("String"));
        assertEquals("text", adapter.mapGraphQLTypeToSql("Int"));
    }

    @Test
    void testExecuteQueryReturnsEmptyList() throws SQLException {
        List<Map<String, Object>> result = adapter.executeQuery("SELECT 1");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExecuteQuerySingleReturnsNull() throws SQLException {
        assertNull(adapter.executeQuerySingle("SELECT 1"));
    }

    @Test
    void testExecuteUpdateReturnsZero() throws SQLException {
        assertEquals(0, adapter.executeUpdate("UPDATE something SET x=1"));
    }

    @Test
    void testCreateTableNoOp() throws SQLException {
        // Should not throw
        adapter.createTable("table", "id int");
    }
} 