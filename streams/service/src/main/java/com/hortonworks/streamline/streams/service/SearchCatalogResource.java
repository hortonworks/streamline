package com.hortonworks.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import com.hortonworks.streamline.common.util.ReflectionHelper;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.storage.Storable;
import com.hortonworks.streamline.storage.util.StorageUtils;
import com.hortonworks.streamline.streams.catalog.Cluster;
import com.hortonworks.streamline.streams.catalog.Namespace;
import com.hortonworks.streamline.streams.catalog.Topology;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.cluster.service.EnvironmentService;
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
import java.util.ArrayList;
import java.util.Collection;
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

    public SearchCatalogResource(StreamlineAuthorizer authorizer, StreamCatalogService catalogService, EnvironmentService environmentService) {
        this.authorizer = authorizer;
        this.catalogService = catalogService;
        this.environmentService = environmentService;
    }

    // used internally to execute the different list commands in a seamless way
    private interface ListCommand {
        Collection<Storable> execute();
    }

    private ListCommand getListCommand(String namespace) {
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

    @GET
    @Path("/search")
    @Timed
    public Response searchEntities(@javax.ws.rs.QueryParam("sort") String sortType,
                                   @javax.ws.rs.QueryParam("desc") Boolean desc,
                                   @javax.ws.rs.QueryParam("namespace") String namespace,
                                   @javax.ws.rs.QueryParam("queryString") String queryString,
                                   @Context SecurityContext securityContext) {
        Collection<Storable> storables = SecurityUtil.filter(authorizer, securityContext, namespace,
                getListCommand(namespace).execute(), READ);
        Collection<Storable> res = new ArrayList<>();
        if (!storables.isEmpty()) {
            String sortFieldName = getSortFieldName(sortType);
            res.addAll(storables.stream()
                    .filter(s -> StringUtils.isEmpty(queryString) || matches(s, Pattern.compile(queryString)))
                    .sorted((s1, s2) -> compare(s1, s2, sortFieldName, desc))
                    .collect(Collectors.toList()));
        }
        return WSUtils.respondEntities(res, OK);
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
            int res = ReflectionHelper.<T>invokeGetter(fieldName, s1)
                    .compareTo(ReflectionHelper.invokeGetter(fieldName, s2));
            return (desc != null && desc) ? -res : res;
        } catch (Exception ex) {
            LOG.error("Got exception trying to get value for " + fieldName, ex);
            throw new RuntimeException(ex);
        }
    }

}
