/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at

  *   http://www.apache.org/licenses/LICENSE-2.0

  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 **/
package com.hortonworks.streamline.registries.dashboard.service;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.common.util.WSUtils;
import com.hortonworks.streamline.registries.dashboard.dto.WidgetDto;
import com.hortonworks.streamline.registries.dashboard.entites.Dashboard;
import com.hortonworks.streamline.registries.dashboard.entites.Datasource;
import com.hortonworks.streamline.registries.dashboard.entites.Widget;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/dashboards")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardCatalogResource {
    private final DashboardCatalogService dashboardCatalogService;

    public DashboardCatalogResource(DashboardCatalogService dashboardCatalogService) {
        this.dashboardCatalogService = dashboardCatalogService;
    }

    @GET
    @Timed
    public Response listDashboards(@Context UriInfo uriInfo) {
        Collection<Dashboard> dashboards = null;
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        List<QueryParam> queryParams = WSUtils.buildQueryParameters(params);
        if (params == null || params.isEmpty()) {
            dashboards = dashboardCatalogService.listDashboards();
        } else {
            dashboards = dashboardCatalogService.listDashboards(queryParams);
        }
        if (dashboards != null) {
            return WSUtils.respondEntities(dashboards, OK);
        }
        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/{id}")
    @Timed
    public Response getDashboard(@PathParam("id") Long dashboardId) {
        Dashboard dashboard = dashboardCatalogService.getDashboard(dashboardId);
        if (dashboard != null) {
            return WSUtils.respondEntity(dashboard, OK);
        }
        throw EntityNotFoundException.byId(dashboardId.toString());
    }

    @Timed
    @POST
    public Response addDashboard(Dashboard dashboard) {
        Dashboard createdDashboard = dashboardCatalogService.addDashboard(dashboard);
        return WSUtils.respondEntity(createdDashboard, CREATED);
    }

    @PUT
    @Path("/{id}")
    @Timed
    public Response addOrUpdateDashboard(@PathParam("id") Long dashboardId, Dashboard dashboard) {
        Dashboard newDashboard = dashboardCatalogService.addOrUpdateDashboard(dashboardId, dashboard);
        return WSUtils.respondEntity(newDashboard, CREATED);
    }

    @DELETE
    @Path("/{id}")
    @Timed
    public Response deleteDashboard(@PathParam("id") Long dashboardId) {
        Dashboard dashboard = dashboardCatalogService.removeDashboard(dashboardId);
        if (dashboard != null) {
            return WSUtils.respondEntity(dashboard, OK);
        }
        throw EntityNotFoundException.byId(dashboardId.toString());
    }

    @GET
    @Path("/{dashboardId}/widgets")
    @Timed
    public Response listWidgets(@PathParam("dashboardId") Long dashboardId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildDashboardIdAwareQueryParams(dashboardId, uriInfo);
        Collection<Widget> widgets = dashboardCatalogService.listWidgets(queryParams);
        if (widgets != null) {
            List<WidgetDto> dtos = new ArrayList<>();
            widgets.forEach(widget -> {
                WidgetDto dto = WidgetDto.fromWidget(widget);
                dto.setDatasourceIds(dashboardCatalogService.getWidgetDatasourceMapping(widget));
                dtos.add(dto);
            });
            return WSUtils.respondEntities(dtos, OK);
        }
        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/{dashboardId}/widgets/{widgetId}")
    @Timed
    public Response getWidget(@PathParam("dashboardId") Long dashboardId,
                              @PathParam("widgetId") Long widgetId) {
        Widget widget = dashboardCatalogService.getWidget(dashboardId, widgetId);
        if (widget != null) {
            WidgetDto dto = WidgetDto.fromWidget(widget);
            dto.setDatasourceIds(dashboardCatalogService.getWidgetDatasourceMapping(widget));
            return WSUtils.respondEntity(dto, OK);
        }
        throw EntityNotFoundException.byId(getCompositeId(dashboardId, widgetId));
    }

    @POST
    @Path("/{dashboardId}/widgets")
    @Timed
    public Response addWidget(@PathParam("dashboardId") Long dashboardId, WidgetDto dto) {
        Widget createdWidget = dashboardCatalogService.addWidget(dashboardId, Widget.fromDto(dto));
        WidgetDto resultDto = WidgetDto.fromWidget(createdWidget);
        if (dto.getDatasourceIds() != null) {
            dashboardCatalogService.addWidgetDatasourceMapping(createdWidget, dto.getDatasourceIds());
            resultDto.setDatasourceIds(dto.getDatasourceIds());
        }
        return WSUtils.respondEntity(resultDto, CREATED);
    }

    @PUT
    @Path("/{dashboardId}/widgets/{widgetId}")
    @Timed
    public Response addOrUpdateWidget(@PathParam("dashboardId") Long dashboardId, @PathParam("widgetId") Long widgetId,
                                      WidgetDto dto) {
        Widget widget = Widget.fromDto(dto);
        Widget updatedWidget = dashboardCatalogService.addOrUpdateWidget(dashboardId, widgetId, widget);
        WidgetDto resultDto = WidgetDto.fromWidget(updatedWidget);
        if (dto.getDatasourceIds() != null) {
            Set<Long> existing = dashboardCatalogService.getWidgetDatasourceMapping(widget);
            Set<Long> newSet = dto.getDatasourceIds();
            Sets.SetView<Long> mappingsToRemove = Sets.difference(ImmutableSet.copyOf(existing), ImmutableSet.copyOf(newSet));
            Sets.SetView<Long> mappingsToAdd = Sets.difference(ImmutableSet.copyOf(newSet), ImmutableSet.copyOf(existing));
            dashboardCatalogService.removeWidgetDatasourceMapping(widget, mappingsToRemove);
            dashboardCatalogService.addWidgetDatasourceMapping(widget, mappingsToAdd);
            resultDto.setDatasourceIds(dto.getDatasourceIds());
        }
        return WSUtils.respondEntity(resultDto, CREATED);
    }

    @DELETE
    @Path("/{dashboardId}/widgets/{widgetId}")
    @Timed
    public Response deleteWidget(@PathParam("dashboardId") Long dashboardId,
                                 @PathParam("widgetId") Long widgetId) {
        Widget widget = dashboardCatalogService.removeWidget(dashboardId, widgetId);
        if (widget != null) {
            WidgetDto dto = WidgetDto.fromWidget(widget);
            Set<Long> datasourceIds = dashboardCatalogService.getWidgetDatasourceMapping(widget);
            dashboardCatalogService.removeWidgetDatasourceMapping(widget, datasourceIds);
            dto.setDatasourceIds(datasourceIds);
            return WSUtils.respondEntity(dto, OK);
        }
        throw EntityNotFoundException.byId(getCompositeId(dashboardId, widgetId));
    }

    @GET
    @Path("/{dashboardId}/datasources")
    @Timed
    public Response listDatasources(@PathParam("dashboardId") Long dashboardId, @Context UriInfo uriInfo) {
        List<QueryParam> queryParams = buildDashboardIdAwareQueryParams(dashboardId, uriInfo);
        Collection<Datasource> datasources = dashboardCatalogService.listDatasources(queryParams);
        if (datasources != null) {
            return WSUtils.respondEntities(datasources, OK);
        }
        throw EntityNotFoundException.byFilter(queryParams.toString());
    }

    @GET
    @Path("/{dashboardId}/datasources/{datasourceId}")
    @Timed
    public Response getDatasource(@PathParam("dashboardId") Long dashboardId,
                                  @PathParam("datasourceId") Long datasourceId) {
        Datasource datasource = dashboardCatalogService.getDatasource(dashboardId, datasourceId);
        if (datasource != null) {
            return WSUtils.respondEntity(datasource, OK);
        }
        throw EntityNotFoundException.byId(getCompositeId(dashboardId, datasourceId));
    }

    @POST
    @Path("/{dashboardId}/datasources")
    @Timed
    public Response addDatasource(@PathParam("dashboardId") Long dashboardId, Datasource datasource) {
        Datasource createdDatasource = dashboardCatalogService.addDatasource(dashboardId, datasource);
        return WSUtils.respondEntity(createdDatasource, CREATED);
    }

    @PUT
    @Path("/{dashboardId}/datasources/{datasourceId}")
    @Timed
    public Response addOrUpdateDatasource(@PathParam("dashboardId") Long dashboardId,
                                          @PathParam("datasourceId") Long datasourceId,
                                          Datasource datasource) {
        Datasource updatedDatasource = dashboardCatalogService.addOrUpdateDatasource(
                dashboardId, datasourceId, datasource);
        return WSUtils.respondEntity(updatedDatasource, CREATED);
    }

    @DELETE
    @Path("/{dashboardId}/datasources/{datasourceId}")
    @Timed
    public Response deleteDatasource(@PathParam("dashboardId") Long dashboardId,
                                     @PathParam("datasourceId") Long datasourceId) {
        Datasource datasource = dashboardCatalogService.removeDatasource(dashboardId, datasourceId);
        if (datasource != null) {
            return WSUtils.respondEntity(datasource, OK);
        }
        throw EntityNotFoundException.byId(getCompositeId(dashboardId, datasourceId));
    }

    public static List<QueryParam> buildDashboardIdAwareQueryParams(Long dashboardId, UriInfo uriInfo) {
        List<QueryParam> queryParams = new ArrayList<>();
        queryParams.add(new QueryParam(Dashboard.DASHBOARD_ID, dashboardId.toString()));
        if (uriInfo != null) {
            MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
            if (!params.isEmpty()) {
                queryParams.addAll(WSUtils.buildQueryParameters(params));
            }
        }
        return queryParams;
    }

    private String getCompositeId(Long dashboardId, Long id) {
        return String.format("dashboard id <%d>, id <%d>", dashboardId, id);
    }
}
