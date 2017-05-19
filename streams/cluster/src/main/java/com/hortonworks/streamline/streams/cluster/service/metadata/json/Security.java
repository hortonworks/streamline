package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hortonworks.streamline.streams.security.SecurityUtil;

import java.util.Map;

import javax.ws.rs.core.SecurityContext;

@JsonPropertyOrder({"authentication", "authorizer", "principals", "keytabs"})
public class Security {

    private Authentication authentication;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Authorizer authorizer;

    @JsonIgnore     // see method getPrincipalsMap()
    private Principals principals;

    @JsonIgnore     // see method getKeytabsMap()
    private Keytabs keytabs;

    @JsonIgnore
    private final SecurityContext securityContext;

    public Security(SecurityContext securityContext, Authentication authentication,
                    Authorizer authorizer, Principals principals, Keytabs keytabs) {
        this.securityContext = securityContext;
        this.authentication = authentication;
        this.authorizer = authorizer;
        this.principals = principals;
        this.keytabs = keytabs;
    }

    /**
     * Sets authorizer info iff {@code authentication.isEnabled()}, otherwise it is set to null
     */
    public Security(SecurityContext securityContext, Authorizer authorizer,
                    Principals principals, Keytabs keytabs) {
        this.securityContext = securityContext;
        this.authentication = new Authentication(securityContext);
        this.principals = principals;
        this.keytabs = keytabs;
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

    @JsonIgnore
    public Principals getPrincipals() {
        return principals;
    }

    @JsonIgnore
    public Keytabs getKeytabs() {
        return keytabs;
    }

    @JsonIgnore
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @JsonProperty("principals")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getPrincipalsMap() {
        return getMapIfSecureIfNotEmpty(principals.toMap());
    }

    @JsonProperty("keytabs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getKeytabsMap() {
        return getMapIfSecureIfNotEmpty(keytabs.toMap());
    }

    /**
     * @return the map specified as argument only if Kerberos authentication is enabled and map !=null && !map.isEmpty()
     */
    private Map<String, String> getMapIfSecureIfNotEmpty(Map<String, String> map) {
        return SecurityUtil.isKerberosAuthenticated(securityContext) &&
                map != null &&
                !map.isEmpty() ? map : null;
    }

    @Override
    public String toString() {
        return "Security{" +
                "authentication=" + authentication +
                ", authorizer=" + authorizer +
                ", principals=" + principals +
                ", keytabs=" + keytabs +
                ", securityContext=" + securityContext +
                '}';
    }
}
