package com.hortonworks.streamline.storage.tool;

import java.io.IOException;
import java.util.Map;

public class StreamlinePropertiesReader {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("USAGE: [config file path] [property key]");
            System.exit(1);
        }

        String configFilePath = args[0];
        String propertyKey = args[1];
        try {
            Map<String, Object> conf = Utils.readStreamlineConfig(configFilePath);
            if (!conf.containsKey(propertyKey)) {
                System.err.println("The key " + propertyKey + " is not defined to the config file.");
                System.exit(3);
            }

            System.out.println(conf.get(propertyKey));
        } catch (IOException e) {
            System.err.println("Error occurred while reading config file: " + configFilePath);
            System.exit(2);
        }
    }
}
