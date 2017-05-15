package com.hortonworks.streamline.streams.cluster.service.metadata.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"invoked", "msg"})
public class Authorizer {
    public static final String AUTHRZ_MSG =
            "Authorization not enforced. Every authenticated user has access to all metadata info";

    private boolean invoked;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String msg;

    public Authorizer(boolean invoked, String msg) {
        this.invoked = invoked;
        this.msg = msg;
    }

    public Authorizer(boolean invoked) {
        this(invoked, invoked ? "" : AUTHRZ_MSG);
    }

    /**
     * @return true if the operation invokes (goes through) the authorizer code. False otherwise
     */
    public boolean isInvoked() {
        return invoked;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "Authorizer{" +
                "invoked=" + invoked +
                ", msg='" + msg + '\'' +
                '}';
    }
}
