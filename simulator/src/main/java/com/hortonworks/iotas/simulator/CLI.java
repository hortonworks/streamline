/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.iotas.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.hortonworks.iotas.model.IotasMessage;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.Properties;
import java.util.Scanner;


public class CLI {

    private static final String OPTION_BROKER_HOSTS = "broker-hosts";
    private static final String OPTION_INTERACTIVE = "interactive";
    private static final String OPTION_TOPIC = "topic";
    private static final String OPTION_DELAY = "delay";
    private static final String OPTION_TIMESTAMP = "timestamp";
    private static final String OPTION_DATA_FILE_PATH = "dataFilePath";

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(option(1, "b", OPTION_BROKER_HOSTS, "hosts string", "Kafka broker host string (required)"));
        options.addOption(option(0, "i", OPTION_INTERACTIVE, "Run in interactive mode."));
        options.addOption(option(1, "t", OPTION_TOPIC, "Kafka topic to publish to."));
        options.addOption(option(1, "d", OPTION_DELAY, "When processing a data file, the delay, in milliseconds, between messages."));
        options.addOption(option(0, "T", OPTION_TIMESTAMP, "When processing a data file, override timestamp with the current time."));
        options.addOption(option(1, "f", OPTION_DATA_FILE_PATH, "Data File Path from which data will be read and published to kafka topic."));

        CommandLineParser parser = new BasicParser();
        CommandLine cmd = parser.parse(options, args);

        if(args.length == 0 || !cmd.hasOption(OPTION_BROKER_HOSTS)){
            usage(options);
            System.exit(1);
        }

        Properties props = new Properties();
        props.put("metadata.broker.list", cmd.getOptionValue(OPTION_BROKER_HOSTS));
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        ProducerConfig config = new ProducerConfig(props);
        Producer<String, String> producer = new Producer<String, String>(config);
        System.out.println("Connected to kafka for producing.");

        if(cmd.hasOption(OPTION_INTERACTIVE)) {
            interactiveLoop(producer, cmd);
        } else if(cmd.hasOption(OPTION_DATA_FILE_PATH)) {
            File file = new File(cmd.getOptionValue(OPTION_DATA_FILE_PATH));
            if(!(file.exists() && file.canRead() && file.isFile())){
                System.out.println("Error: Unable to read file: " + file.getAbsolutePath());
                System.exit(2);
            }

            processDataFile(file, producer, cmd);
        } else {
            usage(options);
        }
    }

    private static Option option(int argCount, String shortName, String longName, String description){
        return option(argCount, shortName, longName, longName, description);
    }

    private static Option option(int argCount, String shortName, String longName, String argName, String description){
        Option option = OptionBuilder.hasArgs(argCount)
                .withArgName(argName)
                .withLongOpt(longName)
                .withDescription(description)
                .create(shortName);
        return option;
    }

    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("CLI -b <brokerhosts> [options]", options);
    }

    private static void processDataFile(File file, Producer producer, CommandLine cmd) throws InterruptedException, IOException {
        String topic = cmd.getOptionValue(OPTION_TOPIC);
        System.out.println("Publishing to topic: " + topic);
        try(BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(file)))) {
            ObjectMapper mapper = new ObjectMapper();
            String line = null;
            while((line = reader.readLine()) != null){
                if(line.trim().length() == 0) continue;

                IotasMessage message = mapper.readValue(line, IotasMessage.class);
                if(cmd.hasOption(OPTION_TIMESTAMP)){
                    message.setTimestamp(System.currentTimeMillis());
                }
                writeToKafka(producer, message, topic);
                if(cmd.hasOption(OPTION_DELAY)){
                    Long delay = Long.parseLong(cmd.getOptionValue(OPTION_DELAY));
                    System.out.println("Delay " + delay + " ms.");
                    Thread.sleep(delay);
                }
            }
        }
    }

    private static void interactiveLoop(Producer producer, CommandLine cmd){
        String lastTopic = "";

        System.out.println("Entering interactive mode. Press CTRL + C to exit.");
        if(!cmd.hasOption(OPTION_TOPIC)){
            System.out.println("No kafka topic specified. Will prompt for topic.");
        } else {
            lastTopic = cmd.getOptionValue(OPTION_TOPIC);
        }
        Scanner scanner = new Scanner(System.in);
        String lastMake = "";
        String lastType = "";
        String lastModel = "";
        String lastData = "";

        String temp;
        while(true){
            System.out.print(String.format("Device Make [%s]: ", lastMake));
            temp = scanner.nextLine();
            if(!temp.equals("")){
                lastMake = temp;
            }

            System.out.print(String.format("Device Type [%s]: ", lastType));
            temp = scanner.nextLine();
            if(!temp.equals("")) {
                lastType = temp;
            }

            System.out.print(String.format("Device Model [%s]: ", lastModel));
            temp = scanner.nextLine();
            if(!temp.equals("")) {
                lastModel = temp;
            }

            System.out.print(String.format("Data [%s]: ", lastData));
            temp = scanner.nextLine();
            if(!temp.equals("")){
                lastData = temp;
            }

            if(!cmd.hasOption(OPTION_TOPIC)){
                System.out.print(String.format("Topic [%s]: ", lastTopic));
                temp = scanner.nextLine();
                if(!temp.equals("")){
                    lastTopic = temp;
                }
            }

            IotasMessage message = new IotasMessage();
            message.setMake(lastMake);
            message.setType(lastType);
            message.setModel(lastModel);
            message.setData(lastData.getBytes(Charsets.UTF_8));

            writeToKafka(producer, message, lastTopic);
        }
    }

    private static void writeToKafka(Producer<String, String> producer, IotasMessage message, String topic){
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(message);
            producer.send(new KeyedMessage<String, String>(topic, json));
            System.out.println("Sent: " + json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

}
