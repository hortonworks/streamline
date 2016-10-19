package org.apache.streamline.storage.tool;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

public class SQLScriptRunner {
    private static final String OPTION_JDBC_URL = "url";
    private static final String OPTION_JDBC_DRIVER_CLASS = "driver";
    private static final String OPTION_SCRIPT_PATH = "file";
    private static final String OPTION_QUERY_DELIMITER = "delimiter";

    private final String url;

    public SQLScriptRunner(String url) {
        this.url = url;
    }

    public void runScript(String path, String delimiter) throws Exception {
        final File file = new File(path);
        if (!file.exists()) {
            System.err.println("Given path " + path + " could not be found");
            throw new RuntimeException("File not found for given path " + path);
        }

        String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())), StandardCharsets.UTF_8);

        Connection connection = DriverManager.getConnection(url);
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

        options.addOption(option(1, "u", OPTION_JDBC_URL, "JDBC Connect URL"));
        options.addOption(option(1, "c", OPTION_JDBC_DRIVER_CLASS, "JDBC Driver class"));
        options.addOption(option(Option.UNLIMITED_VALUES, "f", OPTION_SCRIPT_PATH, "Script path to execute"));
        options.addOption(option(1, "d", OPTION_QUERY_DELIMITER, "Query delimiter"));

        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);

        if (!commandLine.hasOption(OPTION_JDBC_URL) ||
            !commandLine.hasOption(OPTION_JDBC_DRIVER_CLASS) ||
            !commandLine.hasOption(OPTION_QUERY_DELIMITER) ||
            !commandLine.hasOption(OPTION_SCRIPT_PATH) ||
            commandLine.getOptionValues(OPTION_SCRIPT_PATH).length <= 0) {
            usage(options);
            System.exit(1);
        }

        String url = commandLine.getOptionValue(OPTION_JDBC_URL);
        String driver = commandLine.getOptionValue(OPTION_JDBC_DRIVER_CLASS);
        String[] scripts = commandLine.getOptionValues(OPTION_SCRIPT_PATH);
        String delimiter = commandLine.getOptionValue(OPTION_QUERY_DELIMITER);

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            System.err.println("Driver class is not found in classpath. Please ensure that driver is in classpath.");
            System.exit(2);
        }

        SQLScriptRunner SQLScriptRunner = new SQLScriptRunner(url);

        for (String script : scripts) {
            SQLScriptRunner.runScript(script, delimiter);
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
