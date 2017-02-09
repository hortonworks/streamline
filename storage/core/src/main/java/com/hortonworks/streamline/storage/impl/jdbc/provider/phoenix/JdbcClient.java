/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.storage.impl.jdbc.provider.phoenix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Basic PhoenixClient
 */
public class JdbcClient {
    private static final Logger log = LoggerFactory.getLogger(JdbcClient.class.getName());
    private static final String DEFAULT_URL = "jdbc:phoenix:localhost:2181";

    private final String url;

    public JdbcClient() {
        this(DEFAULT_URL);
    }

    public JdbcClient(String url) {
        this.url = url;
    }

    public void runScript(String path) throws Exception {
        final InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(path);
        if(resourceAsStream == null) {
            log.warn("Given resource with path [{}] could not be found", path);
            throw new RuntimeException("Resource not found for given path "+path);
        }

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(resourceAsStream))
        ) {
            Connection connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    connection.createStatement().execute(line);
                    log.info(String.format("######## SQL Query:  %s ", line));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String url = "jdbc:phoenix:localhost:2181";
        String createPath = "phoenix/create_tables.sql";
        String dropPath = "phoenix/drop_tables.sql";
        JdbcClient jdbcClient = new JdbcClient(url);
        jdbcClient.runScript(dropPath);
        jdbcClient.runScript(createPath);
    }
}
