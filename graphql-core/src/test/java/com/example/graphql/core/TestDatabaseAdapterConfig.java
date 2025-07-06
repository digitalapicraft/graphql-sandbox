package com.example.graphql.core;

import com.example.graphql.core.adapter.DatabaseAdapter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class TestDatabaseAdapterConfig {
    @Bean
    public DatabaseAdapter testDatabaseAdapter() {
        return new DatabaseAdapter() {
            @Override
            public Connection getConnection() throws SQLException { return null; }
            @Override
            public List<Map<String, Object>> executeQuery(String sql, Object... params) { return Collections.emptyList(); }
            @Override
            public Map<String, Object> executeQuerySingle(String sql, Object... params) { return null; }
            @Override
            public int executeUpdate(String sql, Object... params) { return 0; }
            @Override
            public void createTable(String tableName, String columns) { }
            @Override
            public String mapGraphQLTypeToSql(String graphQLType) { return "TEXT"; }
            @Override
            public String getDatabaseType() { return "test"; }
        };
    }
} 