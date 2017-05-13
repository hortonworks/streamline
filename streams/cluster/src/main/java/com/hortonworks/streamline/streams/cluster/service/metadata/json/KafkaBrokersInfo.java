package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hortonworks.streamline.streams.cluster.service.metadata.common.HostPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
@JsonPropertyOrder({"brokers", "security"})
public class KafkaBrokersInfo<T> {
    private final List<T> brokers;
    private final Security security;

    public KafkaBrokersInfo(List<T> brokers) {
        this(brokers, null);
    }

    public KafkaBrokersInfo(List<T> brokers, Security security) {
        this.brokers = brokers;
        this.security = security;
    }

    public static KafkaBrokersInfo<HostPort> hostPort(List<String> hosts, Integer port, SecurityContext securityContext) {
        final Security security = new Security(securityContext, new Authorizer(false));
        List<HostPort> hostsPorts = Collections.emptyList();
        if (hosts != null) {
            hostsPorts = new ArrayList<>(hosts.size());
            for (String host : hosts) {
                hostsPorts.add(new HostPort(host, port));
            }
        }
        return new KafkaBrokersInfo<>(hostsPorts, security);
    }

    public static KafkaBrokersInfo<KafkaBrokersInfo.BrokerId> brokerIds(List<String> brokerIds, SecurityContext securityContext) {
        final Security security = new Security(securityContext, new Authorizer(false));
        List<KafkaBrokersInfo.BrokerId> brokerIdsType = Collections.emptyList();
        if (brokerIds != null) {
            brokerIdsType = new ArrayList<>(brokerIds.size());
            for (String brokerId : brokerIds) {
                brokerIdsType.add(new KafkaBrokersInfo.BrokerId(brokerId));
            }
        }
        return new KafkaBrokersInfo<>(brokerIdsType, security);
    }

    public static KafkaBrokersInfo<String> fromZk(List<String> brokerInfo, SecurityContext securityContext) {
        final Security security = new Security(securityContext, new Authorizer(false));
        return brokerInfo == null
                ? new KafkaBrokersInfo<>(Collections.<String>emptyList(), security)
                : new KafkaBrokersInfo<>(brokerInfo, security);
    }

    public List<T> getBrokers() {
        return brokers;
    }

    public Security getSecurity() {
        return security;
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
