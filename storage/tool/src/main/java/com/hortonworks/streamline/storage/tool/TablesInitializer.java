package com.hortonworks.streamline.storage.tool;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.BooleanUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class TablesInitializer {
    private static final String OPTION_SCRIPT_ROOT_PATH = "script-root";
    private static final String OPTION_CONFIG_FILE_PATH = "config";
    private static final String OPTION_MYSQL_JAR_URL_PATH = "mysql-jar-url";
    private static final String OPTION_EXECUTE_CREATE_TABLE = "create";
    private static final String OPTION_EXECUTE_DROP_TABLE = "drop";
    private static final String OPTION_EXECUTE_CHECK_CONNECTION = "check-connection";

    private static final String CREATE_SCRIPT_FILE_NAME = "create_tables.sql";
    private static final String DROP_SCRIPT_FILE_NAME = "drop_tables.sql";

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        options.addOption(
                Option.builder("s")
                        .numberOfArgs(1)
                        .longOpt(OPTION_SCRIPT_ROOT_PATH)
                        .desc("Root directory of script path")
                        .build()
        );

        options.addOption(
                Option.builder("c")
                        .numberOfArgs(1)
                        .longOpt(OPTION_CONFIG_FILE_PATH)
                        .desc("Config file path")
                        .build()
        );

        options.addOption(
                Option.builder("m")
                        .numberOfArgs(1)
                        .longOpt(OPTION_MYSQL_JAR_URL_PATH)
                        .desc("Mysql client jar url to download")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(OPTION_EXECUTE_CREATE_TABLE)
                        .desc("Execute 'create table' script")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(OPTION_EXECUTE_DROP_TABLE)
                        .desc("Execute 'drop table' script")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(OPTION_EXECUTE_CHECK_CONNECTION)
                        .desc("Check the connection for configured data source")
                        .build()
        );

        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);

        if (!commandLine.hasOption(OPTION_CONFIG_FILE_PATH) || !commandLine.hasOption(OPTION_SCRIPT_ROOT_PATH)) {
            usage(options);
            System.exit(1);
        }

        // either create or drop should be specified, not both
        boolean executeCreate = commandLine.hasOption(OPTION_EXECUTE_CREATE_TABLE);
        boolean executeDrop = commandLine.hasOption(OPTION_EXECUTE_DROP_TABLE);
        boolean checkConnection = commandLine.hasOption(OPTION_EXECUTE_CHECK_CONNECTION);

        boolean moreThanOneOperationIsSpecified = executeCreate == executeDrop ? executeCreate : checkConnection;
        boolean noOperationSpecified = !(executeCreate || executeDrop || checkConnection);

        if (moreThanOneOperationIsSpecified) {
            System.out.println("Only one operation can be execute at once, please select 'create' or 'drop', or 'check-connection'.");
            System.exit(1);
        } else if (noOperationSpecified) {
            System.out.println("One of 'create', 'drop', 'check-connection' operation should be specified to execute.");
            System.exit(1);
        }

        String confFilePath = commandLine.getOptionValue(OPTION_CONFIG_FILE_PATH);
        String scriptRootPath = commandLine.getOptionValue(OPTION_SCRIPT_ROOT_PATH);
        String mysqlJarUrl = commandLine.getOptionValue(OPTION_MYSQL_JAR_URL_PATH);

        StorageProviderConfiguration storageProperties;
        try {
            Map<String, Object> conf = Utils.readStreamlineConfig(confFilePath);

            StorageProviderConfigurationReader confReader = new StorageProviderConfigurationReader();
            storageProperties = confReader.readStorageConfig(conf);
        } catch (IOException e) {
            System.err.println("Error occurred while reading config file: " + confFilePath);
            System.exit(1);
            throw new IllegalStateException("Shouldn't reach here");
        }

        String bootstrapDirPath = null;
        try {
            bootstrapDirPath = System.getProperty("bootstrap.dir");
            MySqlDriverHelper.downloadMySQLJarIfNeeded(storageProperties, bootstrapDirPath, mysqlJarUrl);
        } catch (Exception e) {
            System.err.println("Error occurred while downloading MySQL jar. bootstrap dir: " + bootstrapDirPath);
            System.exit(1);
            throw new IllegalStateException("Shouldn't reach here");
        }

        try {
            SQLScriptRunner sqlScriptRunner = new SQLScriptRunner(storageProperties);

            try {
                sqlScriptRunner.initializeDriver();
            } catch (ClassNotFoundException e) {
                System.err.println("Driver class is not found in classpath. Please ensure that driver is in classpath.");
                System.exit(1);
            }

            if (checkConnection) {
                if (!sqlScriptRunner.checkConnection()) {
                    System.exit(1);
                }
            } else if (executeDrop) {
                doExecuteDrop(sqlScriptRunner, storageProperties, scriptRootPath);
            } else {
                // executeCreate
                doExecuteCreate(sqlScriptRunner, storageProperties, scriptRootPath);
            }
        } catch (IOException e) {
            System.err.println("Error occurred while reading script file. Script root path: " + scriptRootPath);
            System.exit(1);
        }
    }

    private static void doExecuteCreate(SQLScriptRunner sqlScriptRunner, StorageProviderConfiguration storageProperties,
                                        String scriptRootPath) throws Exception {
        String scriptPath = scriptRootPath + File.separator + storageProperties.getDbType() +
                File.separator + CREATE_SCRIPT_FILE_NAME;

        doExecute(sqlScriptRunner, scriptPath);
    }

    private static void doExecuteDrop(SQLScriptRunner sqlScriptRunner, StorageProviderConfiguration storageProperties,
                                      String scriptRootPath) throws Exception {
        System.out.println("The operation will drop any existing tables.");
        System.out.print("Are you sure you want to proceed. (y/n)?");
        Scanner scan = new Scanner(System.in);
        String line = scan.nextLine();
        System.out.println();

        Boolean proceed = BooleanUtils.toBooleanObject(line);
        if (!BooleanUtils.toBoolean(proceed)) {
            System.exit(0);
        }

        String scriptPath = scriptRootPath + File.separator + storageProperties.getDbType() +
                File.separator + DROP_SCRIPT_FILE_NAME;

        doExecute(sqlScriptRunner, scriptPath);
    }

    private static void doExecute(SQLScriptRunner sqlScriptRunner, String scriptPath) throws Exception {
        sqlScriptRunner.runScript(scriptPath);
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
        formatter.printHelp("StreamlineTableInitializer [options]", options);
    }

}
