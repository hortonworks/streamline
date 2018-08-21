package com.hortonworks.streamline.storage.tool.sql.initenv.postgres;

import com.hortonworks.streamline.storage.tool.sql.initenv.UserCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgreSqlUserCreator implements UserCreator {
    private static final String QUERY_USER_TEMPLATE = "SELECT count(*) AS cnt FROM pg_user WHERE usename = '%s'";
    private static final String CREATE_USER_TEMPLATE = "CREATE USER %s WITH PASSWORD '%s'";

    private Connection connection;

    public PostgreSqlUserCreator(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean exists(String userName) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(String.format(QUERY_USER_TEMPLATE, userName));
             ResultSet rs = pstmt.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("It must return the result.");
            }

            return rs.getInt("cnt") > 0;
        }
    }

    @Override
    public void create(String userName, String password) throws SQLException {
        String query = String.format(CREATE_USER_TEMPLATE, userName, password);
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.executeUpdate();
        }
    }
}
