package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

public class UserPrincipal implements Principal {
    private static final String PATTERN = "[/@]";
    private static final Logger LOG = LoggerFactory.getLogger(UserPrincipal.class);

    private String user;
    private String realm;

    public UserPrincipal(String user, String realm) {
        this.user = user;
        this.realm = realm;
        LOG.debug("Created {}", this);
    }

    public static UserPrincipal fromPrincipal(String principal) {
        final String[] split = principal.split(PATTERN);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid user principal [" + principal + "]. " +
                    "If service principal use " + ServicePrincipal.class.getName());
        }

        return new UserPrincipal(split[0], split[1]);
    }

    public String getUser() {
        return user;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public String getName() {
        return user + "@" + realm;
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "user='" + user + '\'' +
                ", realm='" + realm + '\'' +
                '}';
    }
}
