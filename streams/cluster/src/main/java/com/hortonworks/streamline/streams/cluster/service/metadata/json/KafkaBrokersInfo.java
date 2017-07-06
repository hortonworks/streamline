package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hortonworks.streamline.streams.catalog.ComponentProcess;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Wrapper used to show proper JSON formatting
 * <pre>
 * {@code
 * { "brokers" : [ { "host" : "H1", "port" : 23 },
 *                 { "host" : "H2", "port" : 23 },
 *                 { "host" : "H3", "port" : 23 }
 *               ]
 * }
 *
 * { "brokers" : [ { "id" : "1" }, { "id" : "2" }, { "id" : "3" } ] } }
 * </pre>
 */
@JsonPropertyOrder({"brokers", "security", "protocolToHostsWithPort"})
public class KafkaBrokersInfo<T> {
    private final List<T> brokers;
    private final Security security;
    private final Map<KafkaBrokerListeners.Protocol, List<String>> protocolToHostsWithPort;

    public KafkaBrokersInfo(List<T> brokers, Security security,
            Map<KafkaBrokerListeners.Protocol, List<String>> protocolToHostsWithPort) {
        this.brokers = brokers;
        this.security = security;
        this.protocolToHostsWithPort = protocolToHostsWithPort;
    }

    public static KafkaBrokersInfo<HostPort> hostPort(Collection<ComponentProcess> componentProcesses,
                                                      Security security, KafkaBrokerListeners kafkaBrokerListeners)
            throws IOException {

        List<HostPort> hostsPorts = componentProcesses.stream()
                .map(cp -> new HostPort(cp.getHost(), cp.getPort()))
                .collect(toList());
        return new KafkaBrokersInfo<>(hostsPorts, security, kafkaBrokerListeners.getProtocolToHostsWithPort());
    }

    public static KafkaBrokersInfo<KafkaBrokersInfo.BrokerId> brokerIds(
            List<String> brokerIds, Security security, KafkaBrokerListeners kafkaBrokerListeners) throws IOException {

        List<KafkaBrokersInfo.BrokerId> brokerIdsType = Collections.emptyList();
        if (brokerIds != null) {
            brokerIdsType = new ArrayList<>(brokerIds.size());
            for (String brokerId : brokerIds) {
                brokerIdsType.add(new KafkaBrokersInfo.BrokerId(brokerId));
            }
        }
        return new KafkaBrokersInfo<>(brokerIdsType, security, kafkaBrokerListeners.getProtocolToHostsWithPort());
    }

    public static KafkaBrokersInfo<String> fromZk(
            List<String> brokerInfo, Security security, KafkaBrokerListeners kafkaBrokerListeners) throws IOException {
        return brokerInfo == null
                ? new KafkaBrokersInfo<>(Collections.<String>emptyList(), security, kafkaBrokerListeners.getProtocolToHostsWithPort())
                : new KafkaBrokersInfo<>(brokerInfo, security, kafkaBrokerListeners.getProtocolToHostsWithPort());
    }

    public List<T> getBrokers() {
        return brokers;
    }

    public Security getSecurity() {
        return security;
    }

    @JsonProperty("protocol")
    public Map<KafkaBrokerListeners.Protocol, List<String>> getProtocolToHostsWithPort() {
        return Collections.unmodifiableMap(protocolToHostsWithPort);
    }

    @Override
    public String toString() {
        return "KafkaBrokersInfo{" +
                "brokers=" + brokers +
                ", security=" + security +
                ", protocolToHostsWithPort=" + protocolToHostsWithPort +
                '}';
    }

    public static class BrokerId {
        final String id;

        public BrokerId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
