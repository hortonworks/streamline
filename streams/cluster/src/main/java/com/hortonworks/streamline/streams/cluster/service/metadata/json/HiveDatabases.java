package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.SecurityContext;

/**
 * Wrapper used to show proper JSON formatting
 */
@JsonPropertyOrder({"databases", "security"})
public class HiveDatabases {
    private final List<String> databases;
    private final Security security;


    public HiveDatabases(List<String> databases, Security security) {
        this.databases = databases;
        this.security = security;
    }

    public static HiveDatabases newInstance(List<String> databases, SecurityContext securityContext) {
        final Security security = new Security(securityContext, new Authorizer(false));
        return databases == null ? new HiveDatabases(Collections.emptyList(), security) : new HiveDatabases(databases, security);
    }

    @JsonProperty("databases")
    public List<String> list() {
        return databases;
    }

    public Security getSecurity() {
        return security;
    }

    @Override
    public String toString() {
        return "{" +
                "databases=" + databases +
                '}';
    }
}
