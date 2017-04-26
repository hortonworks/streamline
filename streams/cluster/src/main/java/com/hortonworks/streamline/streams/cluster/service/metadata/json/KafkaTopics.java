package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

import javax.ws.rs.core.SecurityContext;

/**
 * Wrapper used to show proper JSON formatting
 */
@JsonPropertyOrder({"topics", "security"})
public class KafkaTopics {
    private final List<String> topics;
    private final Security security;

    public KafkaTopics(List<String> topics, Security security) {
        this.topics = topics;
        this.security = security;
    }

    public KafkaTopics newInstance(List<String> topics, SecurityContext securityContext) {
        return new KafkaTopics(topics, new Security(securityContext, new Authorizer(false)));
    }

    @JsonProperty("topics")
    public List<String> list() {
        return topics;
    }

    public Security getSecurity() {
        return security;
    }

    @Override
    public String toString() {
        return "KafkaTopics{" +
                "topics=" + topics +
                ", security=" + security +
                '}';
    }
}
