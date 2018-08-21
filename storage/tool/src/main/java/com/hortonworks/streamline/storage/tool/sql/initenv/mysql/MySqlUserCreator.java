package com.hortonworks.streamline.storage.tool.sql.initenv.mysql;

import com.hortonworks.streamline.storage.tool.sql.initenv.UserCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySqlUserCreator implements UserCreator {
    private static final String QUERY_USER_TEMPLATE = "SELECT count(*) AS cnt FROM mysql.user WHERE user = '%s'";
    private static final String[] CREATE_USER_TEMPLATE = {
            "create user '%s'@'localhost' IDENTIFIED BY '%s'",
            "create user '%s'@'%%' IDENTIFIED BY '%s'"
    };

    private Connection connection;

    public MySqlUserCreator(Connection connection) {
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
        for (String queryTemplate : CREATE_USER_TEMPLATE) {
            String query = String.format(queryTemplate, userName, password);
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.executeUpdate();
            }
        }
    }
}
