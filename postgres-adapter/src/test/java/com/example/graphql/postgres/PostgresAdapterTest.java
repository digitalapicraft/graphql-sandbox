package com.example.graphql.postgres;

import com.example.graphql.core.adapter.ConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import org.h2.jdbcx.JdbcDataSource;

class PostgresAdapterTest {
    private PostgresAdapter adapter;
    private Connection sharedConnection;

    @BeforeEach
    void setUp() throws SQLException {
        // Use H2 in PostgreSQL compatibility mode for testing
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        sharedConnection = ds.getConnection();
        adapter = new PostgresAdapter(() -> sharedConnection);
    }

    @Test
    void testGetDatabaseType() {
        assertEquals("postgres", adapter.getDatabaseType());
    }

    @Test
    void testMapGraphQLTypeToSql() {
        assertEquals("TEXT", adapter.mapGraphQLTypeToSql("String"));
        assertEquals("INTEGER", adapter.mapGraphQLTypeToSql("Int"));
        assertEquals("DOUBLE PRECISION", adapter.mapGraphQLTypeToSql("Float"));
        assertEquals("BOOLEAN", adapter.mapGraphQLTypeToSql("Boolean"));
        assertEquals("SERIAL PRIMARY KEY", adapter.mapGraphQLTypeToSql("ID"));
    }

    @Test
    void testExecuteQueryReturnsEmptyList() throws SQLException {
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        List<Map<String, Object>> result = adapter.executeQuery("SELECT * FROM TestTable");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExecuteQuerySingleReturnsNull() throws SQLException {
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        assertNull(adapter.executeQuerySingle("SELECT * FROM TestTable WHERE id = ?", 1));
    }

    @Test
    void testExecuteUpdateReturnsZero() throws SQLException {
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        assertEquals(1, adapter.executeUpdate("INSERT INTO TestTable (id, name) VALUES (?, ?)", 1, "Alice"));
        assertEquals(1, adapter.executeUpdate("UPDATE TestTable SET name = ? WHERE id = ?", "Bob", 1));
        assertEquals(1, adapter.executeUpdate("DELETE FROM TestTable WHERE id = ?", 1));
    }

    @Test
    void testCreateTableNoOp() throws SQLException {
        // Should not throw
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
    }
} 