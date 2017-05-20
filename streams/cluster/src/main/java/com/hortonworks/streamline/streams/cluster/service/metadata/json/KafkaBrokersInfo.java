package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hortonworks.streamline.streams.catalog.Component;
import com.hortonworks.streamline.streams.catalog.ServiceConfiguration;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.SecurityContext;

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

    public static KafkaBrokersInfo<HostPort> hostPort(List<String> hosts, Integer port,
            Security security, KafkaBrokerListeners listeners) {

        List<HostPort> hostsPorts = Collections.emptyList();
        if (hosts != null) {
            hostsPorts = new ArrayList<>(hosts.size());
            for (String host : hosts) {
                hostsPorts.add(new HostPort(host, port));
            }
        }
        return new KafkaBrokersInfo<>(hostsPorts, security, listeners.getProtocolToHostsWithPort());
    }

    public static KafkaBrokersInfo<HostPort> hostPort(List<String> hosts, Integer port,
          SecurityContext securityContext, ServiceConfiguration config, Component component) {

        return  hostPort(hosts, port,
                new Security(securityContext, new Authorizer(false)),
                KafkaBrokerListeners.newInstance(config, component));
    }

    public static KafkaBrokersInfo<KafkaBrokersInfo.BrokerId> brokerIds(List<String> brokerIds, Security security,
                                                                        KafkaBrokerListeners listeners) {
        List<KafkaBrokersInfo.BrokerId> brokerIdsType = Collections.emptyList();
        if (brokerIds != null) {
            brokerIdsType = new ArrayList<>(brokerIds.size());
            for (String brokerId : brokerIds) {
                brokerIdsType.add(new KafkaBrokersInfo.BrokerId(brokerId));
            }
        }
        return new KafkaBrokersInfo<>(brokerIdsType, security, listeners.getProtocolToHostsWithPort());
    }

    public static KafkaBrokersInfo<KafkaBrokersInfo.BrokerId> brokerIds(List<String> brokerIds,
            SecurityContext securityContext, ServiceConfiguration config, Component component) {

        return brokerIds(brokerIds,
                new Security(securityContext, new Authorizer(false)),
                KafkaBrokerListeners.newInstance(config, component));
    }

    public static KafkaBrokersInfo<String> fromZk(List<String> brokerInfo,SecurityContext securityContext,
            ServiceConfiguration config, Component component) {

        final Security security = new Security(securityContext, new Authorizer(false));
        final KafkaBrokerListeners listeners = KafkaBrokerListeners.newInstance(config, component);

        return brokerInfo == null
                ? new KafkaBrokersInfo<>(Collections.<String>emptyList(), security, listeners.getProtocolToHostsWithPort())
                : new KafkaBrokersInfo<>(brokerInfo, security, listeners.getProtocolToHostsWithPort());
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
