package com.dac.graphql.postgres;

import com.dac.graphql.core.adapter.DatabaseAdapter;
import com.dac.graphql.core.adapter.ConnectionProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import graphql.language.Type;

@Component
@Profile("postgres")
public class PostgresAdapter implements DatabaseAdapter {
    private ConnectionProvider connectionProvider;
    private boolean closeConnections = true;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    // Default constructor for Spring
    public PostgresAdapter() {
        this.connectionProvider = () -> DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        this.closeConnections = true;
    }

    @Autowired
    public PostgresAdapter(DataSource dataSource) {
        this.connectionProvider = dataSource::getConnection;
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
        Connection conn = getConnection();
        try {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        results.add(row);
                    }
                    return results;
                }
            }
        } finally {
            if (closeConnections && conn != null) conn.close();
        }
    }

    @Override
    public Map<String, Object> executeQuerySingle(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = executeQuery(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        try {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                return stmt.executeUpdate();
            }
        } finally {
            if (closeConnections && conn != null) conn.close();
        }
    }

    @Override
    public void createTable(String tableName, String columns) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ");";
        Connection conn = getConnection();
        try {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } finally {
            if (closeConnections && conn != null) conn.close();
        }
    }

    @Override
    public String mapGraphQLTypeToSql(Type<?> graphQLType) {
        String baseType = getBaseTypeName(graphQLType);
        switch (baseType) {
            case "Int": return "INTEGER";
            case "Float": return "DOUBLE PRECISION";
            case "Boolean": return "BOOLEAN";
            case "ID": return "TEXT PRIMARY KEY";
            case "String":
            default: return "TEXT";
        }
    }

    @Override
    public String getDatabaseType() {
        return "postgres";
    }
} 