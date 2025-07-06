package com.example.graphql.core.adapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database adapter interface for database-agnostic operations.
 * Implementations should handle specific database operations for different database types.
 */
public interface DatabaseAdapter {
    
    /**
     * Get a database connection.
     * 
     * @return database connection
     * @throws SQLException if connection fails
     */
    Connection getConnection() throws SQLException;
    
    /**
     * Execute a query and return results as a list of maps.
     * 
     * @param sql the SQL query to execute
     * @param params parameters for the prepared statement
     * @return list of result rows as maps
     * @throws SQLException if query execution fails
     */
    List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException;
    
    /**
     * Execute a query and return a single result row.
     * 
     * @param sql the SQL query to execute
     * @param params parameters for the prepared statement
     * @return single result row as map, or null if no results
     * @throws SQLException if query execution fails
     */
    Map<String, Object> executeQuerySingle(String sql, Object... params) throws SQLException;
    
    /**
     * Execute an update statement (INSERT, UPDATE, DELETE).
     * 
     * @param sql the SQL update statement
     * @param params parameters for the prepared statement
     * @return number of affected rows
     * @throws SQLException if update execution fails
     */
    int executeUpdate(String sql, Object... params) throws SQLException;
    
    /**
     * Create a table based on GraphQL type definition.
     * 
     * @param tableName name of the table to create
     * @param columns column definitions
     * @throws SQLException if table creation fails
     */
    void createTable(String tableName, String columns) throws SQLException;
    
    /**
     * Map GraphQL type to database-specific SQL type.
     * 
     * @param graphQLType the GraphQL type string
     * @return database-specific SQL type string
     */
    String mapGraphQLTypeToSql(String graphQLType);
    
    /**
     * Get the database type identifier.
     * 
     * @return database type (e.g., "sqlite", "postgres")
     */
    String getDatabaseType();
} 