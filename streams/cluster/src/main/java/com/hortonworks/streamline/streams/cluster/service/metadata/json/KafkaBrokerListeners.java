package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hortonworks.streamline.streams.cluster.catalog.Component;
import com.hortonworks.streamline.streams.cluster.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.catalog.ServiceConfiguration;
import org.apache.parquet.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class KafkaBrokerListeners {
    public static final String KAFKA_BROKER_PROP_INTER_BROKER_SECURITY_PROTOCOL = "security.inter.broker.protocol";
    public static final String KAFKA_BROKER_PROP_LISTENERS = "listeners";

    protected static final Logger LOG = LoggerFactory.getLogger(KafkaBrokerListeners.class);

    // list of hosts associated with a particular security protocol
    private final Map<Protocol, List<String>> protocolToHostsWithPort;

    private KafkaBrokerListeners(Map<Protocol, List<String>> protocolToHostsWithPort) {
        this.protocolToHostsWithPort = protocolToHostsWithPort;
    }

    public static KafkaBrokerListeners newInstance(ServiceConfiguration config, Component component,
                                                   Collection<ComponentProcess> brokers) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(component);

        Map<Protocol, List<String>> protocolToHostsWithPort = new HashMap<>();

        brokers.forEach(broker -> {
            String protocol = broker.getProtocol();
            LOG.debug("Protocol [{}] for broker [{}]", protocol, broker);
            List<String> hostAndPorts = protocolToHostsWithPort.computeIfAbsent(Protocol.find(protocol), k -> new ArrayList<>());
            hostAndPorts.add(broker.getHost() + ":" + broker.getPort());
        });

        return new KafkaBrokerListeners(protocolToHostsWithPort);
    }

    public static class ListenersPropParsed {
        private List<ListenersPropEntry> parsedProps;

        public ListenersPropParsed(List<ListenersPropEntry> parsedProps) {
            this.parsedProps = parsedProps;
        }

        public ListenersPropParsed(ServiceConfiguration config) {
            try {
                Map<String, String> confMap = config.getConfigurationMap();
                parseProps(confMap);
            } catch (IOException e) {
                throw new RuntimeException("Fail to read configuration map while parsing properties.");
            }
        }

        public ListenersPropParsed(Map<String, String> configMap) {
            parseProps(configMap);
        }

        private void parseProps(Map<String, String> configMap) {
            final String brokerSecurityProtocol = configMap.get(KAFKA_BROKER_PROP_INTER_BROKER_SECURITY_PROTOCOL);
            final String listenersStr = configMap.get(KAFKA_BROKER_PROP_LISTENERS);
            LOG.debug("Parsing Kafka properties [{}={}, {}={}]", KAFKA_BROKER_PROP_LISTENERS, listenersStr,
                    KAFKA_BROKER_PROP_INTER_BROKER_SECURITY_PROTOCOL, brokerSecurityProtocol);

            if (listenersStr != null) {
                // listenersStr property has the format PLAINTEXT://localhost:6677,SASL_PLAINTEXT://localhost:6688
                final String[] listeners = listenersStr.split(",");       // splits on ,

                parsedProps = Arrays.stream(listeners)
                        .map((listener) -> listener.split(":"))     // splits on : --> index 0 is protocol, 1 is host, 2 is port
                        .map((split) -> {
                            // Handle Ambari bug that in the scenario handled bellow sets listeners=PLAINTEXT
                            // when it set it to listeners=PLAINTEXTSASL
                            Protocol protocol = listeners.length == 1 && Protocol.SASL_PLAINTEXT.hasAlias(brokerSecurityProtocol)
                                    ? Protocol.SASL_PLAINTEXT
                                    : Protocol.find(split[0]);

                            String host = split[1].replaceAll("/", "");
                            int port = Integer.parseInt(split[2]);

                            final ListenersPropEntry e = new ListenersPropEntry(host, port, protocol);
                            LOG.debug("Added {}", e);
                            return e;
                        }).collect(toList());
            }
        }

        public List<ListenersPropEntry> getParsedProps() {
            return parsedProps;
        }
    }

    public static class ListenersPropEntry {
        private String host;
        private int port;
        private Protocol protocol;

        public ListenersPropEntry(String host, int port, Protocol protocol) {
            this.host = host;
            this.port = port;
            this.protocol = protocol;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public Protocol getProtocol() {
            return protocol;
        }

        @Override
        public String toString() {
            return "ListenersPropEntry{" +
                    "host='" + host + '\'' +
                    ", port=" + port +
                    ", protocol=" + protocol +
                    '}';
        }
    }

    @JsonProperty("protocol")
    public Map<Protocol, List<String>> getProtocolToHostsWithPort() {
        return Collections.unmodifiableMap(protocolToHostsWithPort);
    }

    public enum Protocol {
        PLAINTEXT("PLAINTEXT"),
        SASL_PLAINTEXT("SASL_PLAINTEXT", "PLAINTEXTSASL"),
        SASL_SSL("SASL_SSL"),
        SSL("SSL");

        private List<String> aliases;

        Protocol(String... aliases) {
            this.aliases = Arrays.asList(aliases);
        }

        public static Protocol find(final String alias) {
            Preconditions.checkNotNull(alias, "alias can not be null");
            return Arrays.stream(Protocol.values()).filter((p) -> p.aliases.contains(alias)).findFirst().get();
        }

        public boolean hasAlias(final String alias) {
            return alias != null && aliases.contains(alias);
        }
    }
}

