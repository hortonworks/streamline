package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hortonworks.streamline.streams.security.SecurityUtil;

import javax.ws.rs.core.SecurityContext;


@JsonPropertyOrder({"enabled", "scheme"})
public class Authentication {
    private boolean enabled;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String scheme;

    public Authentication(boolean enabled, String scheme) {
        this.enabled = enabled;
        this.scheme = scheme;
    }

    public Authentication(SecurityContext securityContext) {
        if (SecurityUtil.isKerberosAuthenticated(securityContext)) {
            enabled = true;
            scheme = securityContext.getAuthenticationScheme();
        } else {
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getScheme() {
        return scheme;
    }

    @Override
    public String toString() {
        return "Authentication{" +
                "enabled=" + enabled +
                ", scheme='" + scheme + '\'' +
                '}';
    }
}
