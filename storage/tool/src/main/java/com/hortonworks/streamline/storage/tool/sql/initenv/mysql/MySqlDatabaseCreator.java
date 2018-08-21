package com.hortonworks.streamline.storage.tool.sql.initenv.mysql;

import com.hortonworks.streamline.storage.tool.sql.initenv.DatabaseCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlDatabaseCreator implements DatabaseCreator {

    private static final String QUERY_DATABASE_TEMPLATE = "SELECT count(*) AS cnt FROM information_schema.schemata WHERE schema_name = '%s'";
    private static final String CREATE_DATABASE_TEMPLATE = "CREATE DATABASE %s";
    private static final String[] GRANT_PRIVILEGES_TEMPLATE = {
        "GRANT ALL PRIVILEGES ON %s.* TO '%s'@'localhost'",
        "GRANT ALL PRIVILEGES ON %s.* TO '%s'@'%%'"
    };

    private Connection connection;

    public MySqlDatabaseCreator(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean exists(String databaseName) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(String.format(QUERY_DATABASE_TEMPLATE, databaseName));
             ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("It must return the result.");
            }

            return rs.getInt("cnt") > 0;
        }
    }

    @Override
    public void create(String databaseName) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(String.format(CREATE_DATABASE_TEMPLATE, databaseName))) {
            pstmt.executeUpdate();
        }
    }

    @Override
    public void grantPrivileges(String databaseName, String userName) throws SQLException {
        for (String queryTemplate : GRANT_PRIVILEGES_TEMPLATE) {
            String query = String.format(queryTemplate, databaseName, userName);
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.executeUpdate();
            }
        }
    }
}
