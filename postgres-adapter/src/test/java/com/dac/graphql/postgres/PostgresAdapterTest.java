package com.dac.graphql.postgres;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import javax.sql.DataSource;
import org.mockito.Mockito;

class PostgresAdapterTest {
    private PostgresAdapter adapter;

    @BeforeEach
    void setUp() throws SQLException {
        // Use a fresh H2 in PostgreSQL compatibility mode for each test
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        Connection connection = ds.getConnection();
        adapter = new PostgresAdapter(() -> connection);
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

    @Test
    void testExecuteQueryReturnsData() throws SQLException {
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        adapter.executeUpdate("INSERT INTO TestTable (id, name) VALUES (?, ?)", 1, "Alice");
        List<Map<String, Object>> result = adapter.executeQuery("SELECT * FROM TestTable WHERE id = ?", 1);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("ID"));
        assertEquals("Alice", result.get(0).get("NAME"));
    }

    @Test
    void testExecuteQuerySingleReturnsData() throws SQLException {
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        adapter.executeUpdate("INSERT INTO TestTable (id, name) VALUES (?, ?)", 2, "Bob");
        Map<String, Object> row = adapter.executeQuerySingle("SELECT * FROM TestTable WHERE id = ?", 2);
        assertNotNull(row);
        assertEquals(2, row.get("ID"));
        assertEquals("Bob", row.get("NAME"));
    }

    @Test
    void testExecuteUpdateReturnsZeroWhenNoRowsAffected() throws SQLException {
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        int updated = adapter.executeUpdate("UPDATE TestTable SET name = ? WHERE id = ?", "Charlie", 999);
        assertEquals(0, updated);
    }

    @Test
    void testDefaultConstructorClosesConnection() throws Exception {
        Connection mockConn = Mockito.mock(Connection.class);
        Statement mockStmt = Mockito.mock(Statement.class);
        Mockito.when(mockConn.createStatement()).thenReturn(mockStmt);
        Mockito.when(mockStmt.execute(Mockito.anyString())).thenReturn(true);

        PostgresAdapter adapter = new PostgresAdapter(() -> mockConn);
        java.lang.reflect.Field field = PostgresAdapter.class.getDeclaredField("closeConnections");
        field.setAccessible(true);
        field.set(adapter, true);

        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY");
        Mockito.verify(mockConn, Mockito.atLeastOnce()).close();
    }

    @Test
    void testDataSourceConstructorClosesConnection() throws Exception {
        DataSource mockDs = Mockito.mock(DataSource.class);
        Connection mockConn = Mockito.mock(Connection.class);
        Statement mockStmt = Mockito.mock(Statement.class);
        Mockito.when(mockConn.createStatement()).thenReturn(mockStmt);
        Mockito.when(mockStmt.execute(Mockito.anyString())).thenReturn(true);
        Mockito.when(mockDs.getConnection()).thenReturn(mockConn);
        PostgresAdapter adapter = new PostgresAdapter(mockDs);
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY");
        Mockito.verify(mockConn, Mockito.atLeastOnce()).close();
    }

    @Test
    void testDefaultConstructorBasicOperation() throws Exception {
        PostgresAdapter adapter = new PostgresAdapter();
        java.lang.reflect.Field urlField = PostgresAdapter.class.getDeclaredField("dbUrl");
        urlField.setAccessible(true);
        urlField.set(adapter, "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        java.lang.reflect.Field userField = PostgresAdapter.class.getDeclaredField("dbUser");
        userField.setAccessible(true);
        userField.set(adapter, "sa");
        java.lang.reflect.Field passField = PostgresAdapter.class.getDeclaredField("dbPassword");
        passField.setAccessible(true);
        passField.set(adapter, "");
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        int inserted = adapter.executeUpdate("INSERT INTO TestTable (id, name) VALUES (?, ?)", 1, "Test");
        assertEquals(1, inserted);
    }
} 