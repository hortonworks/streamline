package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.ws.rs.core.SecurityContext;

@JsonPropertyOrder({"authentication", "authorizer"})
public class Security {
    private Authentication authentication;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Authorizer authorizer;

    public Security(Authentication authentication, Authorizer authorizer) {
        this.authentication = authentication;
        this.authorizer = authorizer;
    }

    public Security(SecurityContext securityContext, Authorizer authorizer) {
        this.authentication = new Authentication(securityContext);
        if (authentication.isEnabled()) {
            this.authorizer = authorizer;
        }
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public Authorizer getAuthorizer() {
        return authorizer;
    }

    @Override
    public String toString() {
        return "Security{" +
                "authentication=" + authentication +
                ", authorizer=" + authorizer +
                '}';
    }
}
