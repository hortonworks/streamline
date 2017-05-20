package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

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
