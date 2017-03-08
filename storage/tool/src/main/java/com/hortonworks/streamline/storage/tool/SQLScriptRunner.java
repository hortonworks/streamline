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
package com.hortonworks.streamline.storage.tool;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class SQLScriptRunner {
    private static final String OPTION_SCRIPT_PATH = "file";
    private static final String OPTION_CONFIG_FILE_PATH = "config";
    private static final String OPTION_MYSQL_JAR_URL_PATH = "mysql-jar-url";
    public static final String PLACEHOLDER_REPLACE_DBTYPE = "<dbtype>";

    private final StorageProviderConfiguration storageProperties;

    public SQLScriptRunner(StorageProviderConfiguration storageProperties) {
        this.storageProperties = storageProperties;
    }

    public void initializeDriver() throws ClassNotFoundException {
        Class.forName(storageProperties.getDriverClass());
    }

    public void runScriptWithReplaceDBType(String path) throws Exception {
        String newPath = path.replace(PLACEHOLDER_REPLACE_DBTYPE, storageProperties.getDbType());
        runScript(newPath);
    }

    public boolean checkConnection() {
        try (Connection connection = connect()) {
            return true;
        } catch (SQLException e) {
            System.out.println("Connection failure: " + e.getMessage());
            return false;
        }
    }

    private Connection connect() throws SQLException {
        String user = storageProperties.getUser();
        String password = storageProperties.getPassword();
        String url = storageProperties.getUrl();

        Connection connection;
        if ((user == null && password == null) || (user.equals("") && password.equals(""))) {
            connection = DriverManager.getConnection(url);
        } else {
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    public void runScript(String path) throws Exception {
        String delimiter = storageProperties.getDelimiter();

        final File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("File not found for given path " + path);
        }

        String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);

        try (Connection connection = connect()) {
            connection.setAutoCommit(true);

            String[] queries = content.split(delimiter);
            for (String query : queries) {
                query = query.trim();
                if (!query.isEmpty()) {
                    System.out.println(String.format("######## SQL Query:  %s ", query));
                    connection.createStatement().execute(query);
                    System.out.println("######## Query executed");
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        options.addOption(option(1, "c", OPTION_CONFIG_FILE_PATH, "Config file path"));
        options.addOption(option(Option.UNLIMITED_VALUES, "f", OPTION_SCRIPT_PATH, "Script path to execute"));
        options.addOption(option(Option.UNLIMITED_VALUES, "m", OPTION_MYSQL_JAR_URL_PATH, "Mysql client jar url to download"));
        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);

        if (!commandLine.hasOption(OPTION_CONFIG_FILE_PATH) ||
            !commandLine.hasOption(OPTION_SCRIPT_PATH) ||
            commandLine.getOptionValues(OPTION_SCRIPT_PATH).length <= 0) {
            usage(options);
            System.exit(1);
        }

        String confFilePath = commandLine.getOptionValue(OPTION_CONFIG_FILE_PATH);
        String[] scripts = commandLine.getOptionValues(OPTION_SCRIPT_PATH);
        String mysqlJarUrl = commandLine.getOptionValue(OPTION_MYSQL_JAR_URL_PATH);

        try {
            Map<String, Object> conf = Utils.readStreamlineConfig(confFilePath);

            StorageProviderConfigurationReader confReader = new StorageProviderConfigurationReader();
            StorageProviderConfiguration storageProperties = confReader.readStorageConfig(conf);

            String bootstrapDirPath = System.getProperty("bootstrap.dir");

            MySqlDriverHelper.downloadMySQLJarIfNeeded(storageProperties, bootstrapDirPath, mysqlJarUrl);

            SQLScriptRunner sqlScriptRunner = new SQLScriptRunner(storageProperties);
            try {
                sqlScriptRunner.initializeDriver();
            } catch (ClassNotFoundException e) {
                System.err.println("Driver class is not found in classpath. Please ensure that driver is in classpath.");
                System.exit(1);
            }

            for (String script : scripts) {
                sqlScriptRunner.runScriptWithReplaceDBType(script);
            }
        } catch (IOException e) {
            System.err.println("Error occurred while reading config file: " + confFilePath);
            System.exit(1);
        }
    }

    private static Option option(int argCount, String shortName, String longName, String description){
        return option(argCount, shortName, longName, longName, description);
    }

    private static Option option(int argCount, String shortName, String longName, String argName, String description){
        return OptionBuilder.hasArgs(argCount)
            .withArgName(argName)
            .withLongOpt(longName)
            .withDescription(description)
            .create(shortName);
    }

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SQLScriptRunner [options]", options);
    }

}
