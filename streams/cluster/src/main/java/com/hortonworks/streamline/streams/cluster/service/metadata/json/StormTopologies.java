package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

import javax.ws.rs.core.SecurityContext;

/** Wrapper used to show proper JSON formatting
 * {@code
 *  {
 *   "topologies" : [ "A", "B", "C" ]
 *  }
 * }
 * */
@JsonPropertyOrder({"topologies", "security"})
public class StormTopologies {
    private final List<String> topologies;
    private final Security security;

    public StormTopologies(List<String> topologies, Security security) {
        this.topologies = topologies;
        this.security = security;
    }

    public StormTopologies newInstance(List<String> topologies, SecurityContext securityContext) {
        return new StormTopologies(topologies, new Security(securityContext, new Authorizer(false)));
    }

    @JsonGetter("topologies")
    public List<String> list() {
        return topologies;
    }

    public Security getSecurity() {
        return security;
    }

    @Override
    public String toString() {
        return "StormTopologies{" +
                "topologies=" + topologies +
                ", security=" + security +
                '}';
    }
}
