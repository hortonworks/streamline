package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.ReflectionHelper;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.util.StorageUtils;
import com.hortonworks.streamline.streams.actions.topology.service.TopologyActionsService;
import com.hortonworks.streamline.streams.cluster.catalog.Cluster;
import com.hortonworks.streamline.streams.cluster.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.resource.ClusterResourceUtil;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
import com.hortonworks.streamline.streams.metrics.topology.service.TopologyMetricsService;
import com.hortonworks.streamline.streams.security.SecurityUtil;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.hortonworks.streamline.streams.security.Permission.READ;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class SearchCatalogResource {
    private static final Logger LOG = LoggerFactory.getLogger(SearchCatalogResource.class);

    private final StreamlineAuthorizer authorizer;
    private final StreamCatalogService catalogService;
    private final EnvironmentService environmentService;
    private final TopologyActionsService actionsService;
    private final TopologyMetricsService metricsService;

    public SearchCatalogResource(StreamlineAuthorizer authorizer,
                                 StreamCatalogService catalogService,
                                 EnvironmentService environmentService,
                                 TopologyActionsService actionsService,
                                 TopologyMetricsService metricsService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.environmentService = environmentService;
        this.actionsService = actionsService;
        this.metricsService = metricsService;
    }

    // used internally to execute the different list commands in a seamless way
    private Supplier<Collection<Storable>> listCommand(String namespace) {
        switch (namespace) {
            case Topology.NAMESPACE:
                return () -> {
                    Collection<Storable> res = new ArrayList<>();
                    res.addAll(catalogService.listTopologies());
                    return res;
                };
            case Namespace.NAMESPACE:
                return () -> {
                    Collection<Storable> res = new ArrayList<>();
                    res.addAll(environmentService.listNamespaces());
                    return res;
                };
            case Cluster.NAMESPACE:
                return () -> {
                    Collection<Storable> res = new ArrayList<>();
                    res.addAll(environmentService.listClusters());
                    return res;
                };
            default:
                throw new UnsupportedOperationException("Not implemented for " + namespace);
        }
    }

    // used internally to enrich the storables in a seamless way
    private Function<Collection<Storable>, Collection<?>> enrichCommand(String namespace,
                                                                        String asUser,
                                                                        Integer latencyTopN) {
        switch (namespace) {
            case Topology.NAMESPACE:
                return (Collection<Storable> storables) -> storables
                        .parallelStream()
                        .map(s -> CatalogResourceUtil.enrichTopology((Topology) s, asUser, latencyTopN,
                                environmentService, actionsService, metricsService, catalogService))
                        .collect(Collectors.toList());
            case Namespace.NAMESPACE:
                return (Collection<Storable> storables) -> storables
                        .parallelStream()
                        .map(s -> CatalogResourceUtil.enrichNamespace((Namespace) s, environmentService))
                        .collect(Collectors.toList());
            case Cluster.NAMESPACE:
                return (Collection<Storable> storables) -> storables
                        .parallelStream()
                        .map(s -> ClusterResourceUtil.enrichCluster((Cluster) s, environmentService))
                        .collect(Collectors.toList());
            default:
                throw new UnsupportedOperationException("Not implemented for " + namespace);
        }
    }

    @GET
    @Path("/search")
    @Timed
    public Response searchEntities(@javax.ws.rs.QueryParam("sort") String sortType,
                                   @javax.ws.rs.QueryParam("desc") Boolean desc,
                                   @javax.ws.rs.QueryParam("namespace") String namespace,
                                   @javax.ws.rs.QueryParam("queryString") String queryString,
                                   @javax.ws.rs.QueryParam("detail") Boolean detail,
                                   @javax.ws.rs.QueryParam("latencyTopN") Integer latencyTopN,
                                   @Context SecurityContext securityContext) {
        Collection<Storable> storables = SecurityUtil.filter(authorizer, securityContext, namespace,
                listCommand(namespace).get(), READ);
        Collection<Storable> searchResult = new ArrayList<>();
        if (!storables.isEmpty()) {
            String sortFieldName = getSortFieldName(sortType);
            searchResult.addAll(storables.stream()
                    .filter(s -> StringUtils.isEmpty(queryString)
                            || matches(s, Pattern.compile(queryString, Pattern.CASE_INSENSITIVE)))
                    .sorted((s1, s2) -> compare(s1, s2, sortFieldName, desc))
                    .collect(Collectors.toList()));
        }
        if (detail != null && detail) {
            String asUser = WSUtils.getUserFromSecurityContext(securityContext);
            return WSUtils.respondEntities(enrichCommand(namespace, asUser, latencyTopN).apply(searchResult), OK);
        } else {
            return WSUtils.respondEntities(searchResult, OK);
        }
    }

    private String getSortFieldName(String sortType) {
        return sortType == null ? SortType.NAME.getFieldName() : SortType.valueOf(sortType.toUpperCase()).getFieldName();
    }

    private boolean matches(Storable storable, Pattern pattern) {
        try {
            return StorageUtils
                    .getSearchableFieldValues(storable)
                    .stream()
                    .anyMatch(fv -> pattern.matcher(fv.getValue()).find());
        } catch (Exception ex) {
            LOG.error("Error trying to get searchable field values from storable " + storable, ex);
        }
        return false;
    }

    private <T extends Comparable<T>> int compare(Storable s1, Storable s2, String fieldName, Boolean desc) {
        try {
            Comparable<T> field1 = ReflectionHelper.<T>invokeGetter(fieldName, s1);
            T field2 = ReflectionHelper.invokeGetter(fieldName, s2);
            int res;
            if (field1 instanceof String && field2 instanceof String) {
                res = ((String) field1).compareToIgnoreCase((String) field2);
            } else {
                res = field1.compareTo(field2);
            }
            return (desc != null && desc) ? -res : res;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ex) {
            LOG.error("Got exception trying to get value for " + fieldName, ex);
            throw new RuntimeException(ex);
        }
    }

}
