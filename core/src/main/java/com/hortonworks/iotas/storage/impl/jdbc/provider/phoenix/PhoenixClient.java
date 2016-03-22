package com.hortonworks.iotas.storage.impl.jdbc.provider.phoenix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Basic PhoenixClient
 */
public class PhoenixClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhoenixClient.class.getName());
    private static final String DEFAULT_URL = "jdbc:phoenix:localhost:2181";

    private final String url;

    public PhoenixClient() {
        this(DEFAULT_URL);
    }

    public PhoenixClient(String url) {
        this.url = url;
    }

    public void runScript(String path) throws Exception {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(path)));
        ) {
            Connection connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);

            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    connection.createStatement().execute(line);
                    LOGGER.info(String.format("######## SQL Query:  %s ", line));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String url = "jdbc:phoenix:localhost:2181";
        String createPath = "phoenix/create_tables.sql";
        String dropPath = "phoenix/drop_tables.sql";
        PhoenixClient phoenixClient = new PhoenixClient(url);
        phoenixClient.runScript(dropPath);
        phoenixClient.runScript(createPath);
    }
}
