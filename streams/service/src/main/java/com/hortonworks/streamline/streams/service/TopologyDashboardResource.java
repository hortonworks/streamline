package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Stopwatch;
import com.hortonworks.registries.common.transaction.TransactionIsolation;
import com.hortonworks.registries.storage.TransactionManager;
import com.hortonworks.registries.storage.transaction.ManagedTransaction;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.ParallelStreamUtil;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.security.Roles;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static com.hortonworks.streamline.streams.catalog.Topology.NAMESPACE;
import static com.hortonworks.streamline.streams.security.Permission.READ;
import static com.hortonworks.streamline.streams.service.TopologySortType.LAST_UPDATED;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class TopologyDashboardResource {
    private static final Logger LOG = LoggerFactory.getLogger(TopologyDashboardResource.class);

    private static final String DEFAULT_SORT_TYPE = LAST_UPDATED.name();
    private static final Boolean DEFAULT_SORT_ORDER_ASCENDING = false;

    private static final int FORK_JOIN_POOL_PARALLELISM = 10;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(FORK_JOIN_POOL_PARALLELISM);

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final EnvironmentService environmentService;
    private final TopologyActionsService actionsService;
    private final TopologyMetricsService metricsService;
    private final ManagedTransaction managedTransaction;

    public TopologyDashboardResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService,
                                     EnvironmentService environmentService, TopologyActionsService actionsService,
                                     TopologyMetricsService metricsService, TransactionManager transactionManager) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.environmentService = environmentService;
        this.actionsService = actionsService;
        this.metricsService = metricsService;
        this.managedTransaction = new ManagedTransaction(transactionManager, TransactionIsolation.JDBC_DEFAULT);
    }

    @GET
    @Path("/topologies/dashboard")
    @Timed
    public Response listTopologies(@javax.ws.rs.QueryParam("sort") String sortType,
                                  @javax.ws.rs.QueryParam("ascending") Boolean ascending,
                                  @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN,
                                  @Context SecurityContext securityContext) {
        Collection<Topology> topologies = catalogService.listTopologies();
        boolean topologyUser = SecurityUtil.hasRole(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER);
        if (topologyUser) {
            LOG.debug("Returning all topologies since user has role: {}", Roles.ROLE_TOPOLOGY_USER);
        } else {
            topologies = SecurityUtil.filter(authorizer, securityContext, NAMESPACE, topologies, READ);
        }

        Response response;
        if (topologies != null) {
            if (sortType == null) {
                sortType = DEFAULT_SORT_TYPE;
            }
            if (ascending == null) {
                ascending = DEFAULT_SORT_ORDER_ASCENDING;
            }

            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            List<CatalogResourceUtil.TopologyDashboardResponse> detailedTopologies = enrichTopologies(topologies, asUser,
                    sortType, ascending, latencyTopN);

            response = WSUtils.respondEntities(detailedTopologies, OK);
        } else {
            response = WSUtils.respondEntities(Collections.emptyList(), OK);
        }

        return response;
    }

    @GET
    @Path("/topologies/{topologyId}/dashboard")
    @Timed
    public Response getTopologyById(@PathParam("topologyId") Long topologyId,
                                    @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN,
                                    @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        String asUser = WSUtils.getUserFromSecurityContext(securityContext);

        Topology result = catalogService.getTopology(topologyId);
        if (result != null) {
            CatalogResourceUtil.TopologyDashboardResponse topologyDetailed =
                    CatalogResourceUtil.enrichTopology(result, asUser, latencyTopN,
                            environmentService, actionsService, metricsService, catalogService);
            return WSUtils.respondEntity(topologyDetailed, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    @GET
    @Path("/topologies/{topologyId}/versions/{versionId}/dashboard")
    @Timed
    public Response getTopologyByIdAndVersion(@PathParam("topologyId") Long topologyId,
                                              @PathParam("versionId") Long versionId,
                                              @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN,
                                              @Context SecurityContext securityContext) {
        SecurityUtil.checkRoleOrPermissions(authorizer, securityContext, Roles.ROLE_TOPOLOGY_USER,
                NAMESPACE, topologyId, READ);
        String asUser = WSUtils.getUserFromSecurityContext(securityContext);

        Topology result = catalogService.getTopology(topologyId, versionId);
        if (result != null) {
            CatalogResourceUtil.TopologyDashboardResponse topologyDetailed =
                    CatalogResourceUtil.enrichTopology(result, asUser, latencyTopN,
                            environmentService, actionsService, metricsService, catalogService);
            return WSUtils.respondEntity(topologyDetailed, OK);
        }

        throw EntityNotFoundException.byId(topologyId.toString());
    }

    private List<CatalogResourceUtil.TopologyDashboardResponse> enrichTopologies(Collection<Topology> topologies,
                                                                                 String asUser, String sortType,
                                                                                 Boolean ascending,
                                                                                 Integer latencyTopN) {
        LOG.debug("[START] enrichTopologies");
        Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            List<CatalogResourceUtil.TopologyDashboardResponse> responses = ParallelStreamUtil.execute(() ->
                    topologies.parallelStream()
                            .map(Unchecked.function(t ->
                                    managedTransaction.executeFunction(() ->
                                            CatalogResourceUtil.enrichTopology(t, asUser, latencyTopN,
                                                    environmentService, actionsService, metricsService, catalogService))))
                            .sorted((c1, c2) -> {
                                int compared;

                                switch (TopologySortType.valueOf(sortType.toUpperCase())) {
                                    case NAME:
                                        compared = c1.getTopology().getName().compareTo(c2.getTopology().getName());
                                        break;
                                    case STATUS:
                                        compared = c1.getRunning().compareTo(c2.getRunning());
                                        break;
                                    case LAST_UPDATED:
                                        compared = c1.getTopology().getVersionTimestamp().compareTo(c2.getTopology().getVersionTimestamp());
                                        break;
                                    default:
                                        throw new IllegalStateException("Not supported SortType: " + sortType);
                                }

                                return ascending ? compared : (compared * -1);
                            })
                            .collect(toList()), forkJoinPool);

            LOG.debug("[END] enrichTopologies - elapsed: {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return responses;
        } finally {
            stopwatch.stop();
        }
    }

}
