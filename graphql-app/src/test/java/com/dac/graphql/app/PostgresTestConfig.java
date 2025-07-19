package com.dac.graphql.app;

import com.dac.graphql.core.adapter.DatabaseAdapter;
import graphql.language.Type;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@TestConfiguration
@Profile("postgres")
public class PostgresTestConfig {
    @Bean
    public DatabaseAdapter databaseAdapter() {
        return new DatabaseAdapter() {
            @Override
            public Connection getConnection() throws SQLException { return null; }
            @Override
            public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException { return Collections.emptyList(); }
            @Override
            public Map<String, Object> executeQuerySingle(String sql, Object... params) throws SQLException { return null; }
            @Override
            public int executeUpdate(String sql, Object... params) throws SQLException { return 0; }
            @Override
            public void createTable(String tableName, String columns) throws SQLException {}
            @Override
            public String mapGraphQLTypeToSql(Type<?> graphQLType) { return "text"; }
            @Override
            public String getDatabaseType() { return "postgres"; }
        };
    }
} 