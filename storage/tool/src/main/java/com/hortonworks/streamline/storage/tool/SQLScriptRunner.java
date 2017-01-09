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
import java.util.Map;

public class SQLScriptRunner {
    private static final String OPTION_SCRIPT_PATH = "file";
    private static final String OPTION_CONFIG_FILE_PATH = "config";
    private static final String OPTION_MYSQL_JAR_URL_PATH = "mysql-jar-url";
    public static final String PLACEHOLDER_REPLACE_DBTYPE = "<dbtype>";
    public static final String MYSQL_JAR_FILE_PATTERN = "mysql-connector-java-.*?-bin.jar";


    private final String url;
    private final String user;
    private final String password;

    public SQLScriptRunner(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void runScript(String path, String delimiter) throws Exception {
        final File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("File not found for given path " + path);
        }

        String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);

        Connection connection;
        if ((user == null && password == null) || (user.equals("") && password.equals(""))) {
           connection =  DriverManager.getConnection(url);
        } else {
            connection = DriverManager.getConnection(url, user, password);
        }
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

            /* Due to license issues we will not be able to ship mysql driver.
               If the dbtype is mysql we will prompt user to download the jar and place
               it under bootstrap/lib and libs folder. This runs only one-time and for
               next time onwards we will check if the mysql jar exists in the path.
             */
            File bootstrapDir = new File (System.getProperty("streamline.bootstrap.dir") + File.separator + "lib/");
            File streamlineDir = new File (System.getProperty("streamline.bootstrap.dir") + File.separator +  "../libs/");
    
            if (storageProperties.getDbType().equals("mysql")
                    && (!Utils.fileExists(streamlineDir, MYSQL_JAR_FILE_PATTERN)
                    || !Utils.fileExists(bootstrapDir, MYSQL_JAR_FILE_PATTERN))) {
                if (mysqlJarUrl == null || mysqlJarUrl == "")
                    throw new IllegalArgumentException("Missing mysql client jar url. " +
                            "Please pass mysql client jar url using -m option.");
                try {
                    String mysqlJarFileName = Utils.downloadMysqlJarAndCopyToLibDir(mysqlJarUrl, MYSQL_JAR_FILE_PATTERN);
                    if (mysqlJarFileName != null) {
                        File mysqlJarFile = new File(bootstrapDir+ File.separator + mysqlJarFileName);
                        Utils.loadJarIntoClasspath(mysqlJarFile);
                    }
                } catch(Exception e) {
                    System.exit(1);
                }
            }

            try {
                Class.forName(storageProperties.getDriverClass());
            } catch (ClassNotFoundException e) {
                System.err.println("Driver class is not found in classpath. Please ensure that driver is in classpath.");
                System.exit(1);
            }

            SQLScriptRunner SQLScriptRunner = new SQLScriptRunner(storageProperties.getUrl(), storageProperties.getUser(),
                    storageProperties.getPassword());

            for (String script : scripts) {
                script = script.replace(PLACEHOLDER_REPLACE_DBTYPE, storageProperties.getDbType());
                SQLScriptRunner.runScript(script, storageProperties.getDelimiter());
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
