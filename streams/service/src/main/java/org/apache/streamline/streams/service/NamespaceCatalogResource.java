package org.apache.streamline.streams.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.lang.BooleanUtils;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.storage.exception.AlreadyExistsException;
import org.apache.streamline.streams.catalog.Namespace;
import org.apache.streamline.streams.catalog.NamespaceServiceClusterMapping;
import org.apache.streamline.streams.catalog.service.StreamCatalogService;
import org.apache.streamline.common.exception.service.exception.request.BadRequestException;
import org.apache.streamline.common.exception.service.exception.request.EntityNotFoundException;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class NamespaceCatalogResource {
  private final StreamCatalogService catalogService;

  public NamespaceCatalogResource(StreamCatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GET
  @Path("/namespaces")
  @Timed
  public Response listNamespaces(@Context UriInfo uriInfo) {
    List<QueryParam> queryParams = new ArrayList<>();
    MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
    Collection<Namespace> namespaces;
    Boolean detail = false;

    if (params.isEmpty()) {
      namespaces = catalogService.listNamespaces();
    } else {
      MultivaluedMap<String, String> copiedParams = new MultivaluedHashMap<>();
      copiedParams.putAll(params);
      List<String> detailOption = copiedParams.remove("detail");
      if (detailOption != null && !detailOption.isEmpty()) {
        detail = BooleanUtils.toBooleanObject(detailOption.get(0));
      }

      queryParams = WSUtils.buildQueryParameters(copiedParams);
      namespaces = catalogService.listNamespaces(queryParams);
    }
    if (namespaces != null) {
      return buildNamespacesGetResponse(namespaces, detail);
    }

    throw EntityNotFoundException.byFilter(queryParams.toString());
  }

  @GET
  @Path("/namespaces/{id}")
  @Timed
  public Response getNamespaceById(@PathParam("id") Long namespaceId,
                                   @javax.ws.rs.QueryParam("detail") Boolean detail) {
    Namespace result = catalogService.getNamespace(namespaceId);
    if (result != null) {
      return buildNamespaceGetResponse(result, detail);
    }

    throw EntityNotFoundException.byId(namespaceId.toString());
  }

  @GET
  @Path("/namespaces/name/{namespaceName}")
  @Timed
  public Response getNamespaceByName(@PathParam("namespaceName") String namespaceName,
                                     @javax.ws.rs.QueryParam("detail") Boolean detail) {
    Namespace result = catalogService.getNamespaceByName(namespaceName);
    if (result != null) {
      return buildNamespaceGetResponse(result, detail);
    }

    throw EntityNotFoundException.byName(namespaceName);
  }

  @Timed
  @POST
  @Path("/namespaces")
  public Response addNamespace(Namespace namespace) {
    try {
      String namespaceName = namespace.getName();
      Namespace result = catalogService.getNamespaceByName(namespaceName);
      if (result != null) {
        throw new AlreadyExistsException("Namespace entity already exists with name " + namespaceName);
      }

      Namespace created = catalogService.addNamespace(namespace);
      return WSUtils.respondEntity(created, CREATED);
    } catch (ProcessingException ex) {
      throw BadRequestException.of();
    }
  }

  @DELETE
  @Path("/namespaces/{id}")
  @Timed
  public Response removeNamespace(@PathParam("id") Long namespaceId) {
    Namespace removed = catalogService.removeNamespace(namespaceId);
    if (removed != null) {
      return WSUtils.respondEntity(removed, OK);
    }

    throw EntityNotFoundException.byId(namespaceId.toString());
  }

  @PUT
  @Path("/namespaces/{id}")
  @Timed
  public Response addOrUpdateNamespace(@PathParam("id") Long namespaceId,
      Namespace namespace) {
    try {
      Namespace newNamespace = catalogService.addOrUpdateNamespace(namespaceId, namespace);
      return WSUtils.respondEntity(newNamespace, OK);
    } catch (ProcessingException ex) {
      throw BadRequestException.of();
    }
  }

  @GET
  @Path("/namespaces/{id}/mapping")
  @Timed
  public Response listServiceToClusterMappingInNamespace(@PathParam("id") Long namespaceId) {
    Namespace namespace = catalogService.getNamespace(namespaceId);
    if (namespace == null) {
      throw EntityNotFoundException.byId(namespaceId.toString());
    }

    Collection<NamespaceServiceClusterMapping> existingMappings = catalogService.listServiceClusterMapping(namespaceId);
    if (existingMappings != null) {
      return WSUtils.respondEntities(existingMappings, OK);
    }

    return WSUtils.respondEntities(Collections.emptyList(), OK);
  }

  @GET
  @Path("/namespaces/{id}/mapping/{serviceName}")
  @Timed
  public Response findServicesToClusterMappingInNamespace(@PathParam("id") Long namespaceId,
                                                          @PathParam("serviceName") String serviceName) {
    Namespace namespace = catalogService.getNamespace(namespaceId);
    if (namespace == null) {
      throw EntityNotFoundException.byId("Namespace: " + namespaceId.toString());
    }

    Collection<NamespaceServiceClusterMapping> mappings = catalogService.listServiceClusterMapping(namespaceId, serviceName);
    if (mappings != null) {
      return WSUtils.respondEntities(mappings, OK);
    } else {
      return WSUtils.respondEntities(Collections.emptyList(), OK);
    }
  }

  @POST
  @Path("/namespaces/{id}/mapping/bulk")
  @Timed
  public Response setServicesToClusterInNamespace(@PathParam("id") Long namespaceId,
                                                  List<NamespaceServiceClusterMapping> mappings) {
    Namespace namespace = catalogService.getNamespace(namespaceId);
    if (namespace == null) {
      throw EntityNotFoundException.byId(namespaceId.toString());
    }

    // remove any existing mapping for (namespace, service name) pairs
    Collection<NamespaceServiceClusterMapping> existingMappings = catalogService.listServiceClusterMapping(namespaceId);
    if (existingMappings != null) {
      existingMappings.forEach(m -> catalogService.removeServiceClusterMapping(m.getNamespaceId(), m.getServiceName(),
              m.getClusterId()));
    }

    List<NamespaceServiceClusterMapping> newMappings = mappings.stream()
            .map(catalogService::addOrUpdateServiceClusterMapping).collect(toList());

    return WSUtils.respondEntities(newMappings, CREATED);
  }

  @POST
  @Path("/namespaces/{id}/mapping")
  @Timed
  public Response mapServiceToClusterInNamespace(@PathParam("id") Long namespaceId,
      NamespaceServiceClusterMapping mapping) {
    Namespace namespace = catalogService.getNamespace(namespaceId);
    if (namespace == null) {
      throw EntityNotFoundException.byId(namespaceId.toString());
    }

    NamespaceServiceClusterMapping newMapping = catalogService.addOrUpdateServiceClusterMapping(mapping);
    return WSUtils.respondEntity(newMapping, CREATED);
  }

  @DELETE
  @Path("/namespaces/{id}/mapping/{serviceName}/cluster/{clusterId}")
  @Timed
  public Response unmapServiceToClusterInNamespace(@PathParam("id") Long namespaceId,
      @PathParam("serviceName") String serviceName, @PathParam("clusterId") Long clusterId) {
    NamespaceServiceClusterMapping mapping = catalogService.removeServiceClusterMapping(namespaceId, serviceName, clusterId);
    if (mapping != null) {
      return WSUtils.respondEntity(mapping, OK);
    }

    throw EntityNotFoundException.byId("Namespace: " + namespaceId + " / Service name: " + serviceName +
            " / cluster id: " + clusterId);
  }

  @DELETE
  @Path("/namespaces/{id}/mapping")
  @Timed
  public Response unmapAllServicesToClusterInNamespace(@PathParam("id") Long namespaceId) {
    List<NamespaceServiceClusterMapping> mappings = catalogService.listServiceClusterMapping(namespaceId).stream()
            .map((x) -> catalogService.removeServiceClusterMapping(x.getNamespaceId(), x.getServiceName(),
                    x.getClusterId()))
            .collect(toList());
    return WSUtils.respondEntities(mappings, OK);
  }

  private Response buildNamespacesGetResponse(Collection<Namespace> namespaces, Boolean detail) {
    if (BooleanUtils.isTrue(detail)) {
      List<NamespaceWithMapping> namespacesWithMapping = namespaces.stream()
              .map(this::buildNamespaceWithMapping)
              .collect(toList());

      return WSUtils.respondEntities(namespacesWithMapping, OK);
    } else {
      return WSUtils.respondEntities(namespaces, OK);
    }
  }

  private Response buildNamespaceGetResponse(Namespace namespace, Boolean detail) {
    if (BooleanUtils.isTrue(detail)) {
      NamespaceWithMapping namespaceWithMapping = buildNamespaceWithMapping(namespace);
      return WSUtils.respondEntity(namespaceWithMapping, OK);
    } else {
      return WSUtils.respondEntity(namespace, OK);
    }
  }

  private NamespaceWithMapping buildNamespaceWithMapping(Namespace namespace) {
    NamespaceWithMapping nm = new NamespaceWithMapping(namespace);
    nm.setServiceClusterMappings(catalogService.listServiceClusterMapping(namespace.getId()));
    return nm;
  }

  private static class NamespaceWithMapping {
    private Namespace namespace;
    private Collection<NamespaceServiceClusterMapping> mappings = new ArrayList<>();

    public NamespaceWithMapping(Namespace namespace) {
      this.namespace = namespace;
    }

    public Namespace getNamespace() {
      return namespace;
    }

    public Collection<NamespaceServiceClusterMapping> getMappings() {
      return mappings;
    }

    public void setServiceClusterMappings(Collection<NamespaceServiceClusterMapping> mappings) {
      this.mappings = mappings;
    }

    public void addServiceClusterMapping(NamespaceServiceClusterMapping mapping) {
      mappings.add(mapping);
    }
  }
}
