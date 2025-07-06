package com.example.graphql.postgres;

import com.example.graphql.core.adapter.DatabaseAdapter;
import com.example.graphql.core.adapter.ConnectionProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Profile("postgres")
public class PostgresAdapter implements DatabaseAdapter {
    private ConnectionProvider connectionProvider;
    private boolean closeConnections = true;

    // Default constructor for Spring
    public PostgresAdapter() {
        this.connectionProvider = () -> null;
        this.closeConnections = true;
    }

    // Constructor for testability
    public PostgresAdapter(ConnectionProvider provider) {
        this.connectionProvider = provider;
        this.closeConnections = false;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> executeQuerySingle(String sql, Object... params) throws SQLException {
        return null;
    }

    @Override
    public int executeUpdate(String sql, Object... params) throws SQLException {
        return 0;
    }

    @Override
    public void createTable(String tableName, String columns) throws SQLException {
        // no-op
    }

    @Override
    public String mapGraphQLTypeToSql(String graphQLType) {
        return "text";
    }

    @Override
    public String getDatabaseType() {
        return "postgres";
    }
} 