package com.hortonworks.streamline.storage.tool;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.Map;

public class TablesInitializer {
    private static final String OPTION_SCRIPT_ROOT_PATH = "script-root";
    private static final String OPTION_CONFIG_FILE_PATH = "config";
    private static final String OPTION_MYSQL_JAR_URL_PATH = "mysql-jar-url";


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
                        .longOpt(SchemaMigrationOption.CREATE.toString())
                        .desc("Run sql migrations from scatch")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(SchemaMigrationOption.DROP.toString())
                        .desc("Drop all the tables in the target database")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(SchemaMigrationOption.CHECK_CONNECTION.toString())
                        .desc("Check the connection for configured data source")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(SchemaMigrationOption.MIGRATE.toString())
                        .desc("Execute schema migration from last check point")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(SchemaMigrationOption.INFO.toString())
                        .desc("Show the status of the schema migration compared to the target database")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(SchemaMigrationOption.VALIDATE.toString())
                        .desc("Validate the target database changes with the migration scripts")
                        .build()
        );

        options.addOption(
                Option.builder()
                        .hasArg(false)
                        .longOpt(SchemaMigrationOption.REPAIR.toString())
                        .desc("Repairs the DATABASE_CHANGE_LOG by removing failed migrations and correcting checksum of existing migration script")
                        .build()
        );

        CommandLineParser parser = new BasicParser();
        CommandLine commandLine = parser.parse(options, args);

        if (!commandLine.hasOption(OPTION_CONFIG_FILE_PATH) || !commandLine.hasOption(OPTION_SCRIPT_ROOT_PATH)) {
            usage(options);
            System.exit(1);
        }

        boolean isSchemaMigrationOptionSpecified = false;
        SchemaMigrationOption schemaMigrationOptionSpecified = null;
        for (SchemaMigrationOption schemaMigrationOption : SchemaMigrationOption.values()) {
            if (commandLine.hasOption(schemaMigrationOption.toString())) {
                if (isSchemaMigrationOptionSpecified) {
                    System.out.println("Only one operation can be execute at once, please select one of 'create', ',migrate', 'validate', 'info', 'drop', 'repair', 'check-connection'.");
                    System.exit(1);
                }
                isSchemaMigrationOptionSpecified = true;
                schemaMigrationOptionSpecified = schemaMigrationOption;
            }
        }

        if (!isSchemaMigrationOptionSpecified) {
            System.out.println("One of the option 'create', ',migrate', 'validate', 'info', 'drop', 'repair', 'check-connection' must be specified to execute.");
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

        SchemaMigrationHelper schemaMigrationHelper = new SchemaMigrationHelper(FlywayFactory.get(storageProperties, scriptRootPath));
        try {
            schemaMigrationHelper.execute(schemaMigrationOptionSpecified);
            System.out.println(String.format("\"%s\" option successful", schemaMigrationOptionSpecified.toString()));
        } catch (Exception e) {
            System.err.println(String.format("\"%s\" option failed : %s", schemaMigrationOptionSpecified.toString(), e.getMessage()));
            System.exit(1);
        }

    }

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("StreamlineTableInitializer [options]", options);
    }

}
