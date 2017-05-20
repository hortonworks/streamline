package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public class ServicePrincipal implements Principal {
    private static final String PATTERN = "[/@]";
    private static final Logger LOG = LoggerFactory.getLogger(ServicePrincipal.class);

    private String service;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String host;
    private String realm;

    public ServicePrincipal(String service, String host, String realm) {
        this.service = service;
        this.host = host;
        this.realm = realm;
        LOG.debug("Created {}", this);
    }

    /**
     * principalPattern in Ambari is of the form "nimbus/_HOST@EXAMPLE.COM"
     */
    public static ServicePrincipal forHost(String principalPattern, String host) {
        final String[] split = principalPattern.split(PATTERN);
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid service principal [" + principalPattern
                    + "] for host [" + host + "]. If user principal use " + UserPrincipal.class.getName());
        }

        return new ServicePrincipal(split[0], host, split[2]);
    }

    public static ServicePrincipal fromPrincipal(String principal) {
        final String[] split = principal.split(PATTERN);
        if (split.length != 3) {
            throw new IllegalArgumentException("Invalid service principal [" + principal + "]. " +
                    "If user principal use " + UserPrincipal.class.getName());
        }

        return new ServicePrincipal(split[0], split[1], split[2]);
    }

    public String getService() {
        return service;
    }

    public String getHost() {
        return host;
    }

    public String getRealm() {
        return realm;
    }

    @JsonProperty("name")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getName() {
        return host == null ? null : service + "/" + host + "@" + realm;
    }

    @Override
    public String toString() {
        return "ServicePrincipal{" +
                "service='" + service + '\'' +
                ", host='" + host + '\'' +
                " " + super.toString();
    }
}
