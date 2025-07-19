package com.dac.graphql.sqlite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import graphql.language.TypeName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        assertEquals("INTEGER", adapter.mapGraphQLTypeToSql(TypeName.newTypeName("Int").build()));
        assertEquals("REAL", adapter.mapGraphQLTypeToSql(TypeName.newTypeName("Float").build()));
        assertEquals("BOOLEAN", adapter.mapGraphQLTypeToSql(TypeName.newTypeName("Boolean").build()));
        assertEquals("TEXT PRIMARY KEY", adapter.mapGraphQLTypeToSql(TypeName.newTypeName("ID").build()));
        assertEquals("TEXT", adapter.mapGraphQLTypeToSql(TypeName.newTypeName("String").build()));
        assertEquals("TEXT", adapter.mapGraphQLTypeToSql(TypeName.newTypeName("CustomType").build()));
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

    @Test
    void testDefaultConstructorClosesConnection() throws Exception {
        Connection mockConn = Mockito.mock(Connection.class);
        Statement mockStmt = Mockito.mock(Statement.class);
        Mockito.when(mockConn.createStatement()).thenReturn(mockStmt);
        Mockito.when(mockStmt.execute(Mockito.anyString())).thenReturn(true);
        SqliteAdapter adapter = new SqliteAdapter(() -> mockConn);
        java.lang.reflect.Field field = SqliteAdapter.class.getDeclaredField("closeConnections");
        field.setAccessible(true);
        field.set(adapter, true);
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY");
        Mockito.verify(mockConn, Mockito.atLeastOnce()).close();
    }

    @Test
    void testDefaultConstructorBasicOperation() throws Exception {
        SqliteAdapter adapter = new SqliteAdapter();
        java.lang.reflect.Field urlField = SqliteAdapter.class.getDeclaredField("dbUrl");
        urlField.setAccessible(true);
        String dbFile = "test-sqlite-adapter.db";
        urlField.set(adapter, "jdbc:sqlite:" + dbFile);
        adapter.createTable("TestTable", "id INTEGER PRIMARY KEY, name TEXT");
        int inserted = adapter.executeUpdate("INSERT INTO TestTable (id, name) VALUES (?, ?)", 1, "Test");
        assertEquals(1, inserted);
        // Clean up
        new java.io.File(dbFile).delete();
    }
} 