package com.hortonworks.streamline.common.exception.service.exception.request;

import com.hortonworks.streamline.common.exception.service.exception.WebServiceException;

import javax.ws.rs.core.Response;

/**
 * Topology with same name already running in the cluster
 */
public class TopologyAlreadyExistsOnCluster extends WebServiceException {
    private static final String MSG = "Topology with name '%s' is still running in the cluster";

    public TopologyAlreadyExistsOnCluster(String topologyName) {
        super(Response.Status.CONFLICT, String.format(MSG, topologyName));
    }
}
