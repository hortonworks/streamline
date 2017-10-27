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

import com.hortonworks.streamline.registries.dashboard.entites.WidgetDatasourceMap;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.registries.dashboard.entites.Dashboard;
import com.hortonworks.streamline.registries.dashboard.entites.Datasource;
import com.hortonworks.streamline.registries.dashboard.entites.Widget;
import com.hortonworks.registries.storage.StorableKey;
import com.hortonworks.registries.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DashboardCatalogService {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardCatalogService.class);
    private static final String DASHBOARD_NAMESPACE = new Dashboard().getNameSpace();
    private static final String DATASOURCE_NAMESPACE = new Datasource().getNameSpace();
    private static final String WIDGET_NAMESPACE = new Widget().getNameSpace();
    private static final String WIDGET_DATASOURCE_MAPPING_NAMESPACE = new WidgetDatasourceMap().getNameSpace();
    private final StorageManager dao;
    private final FileStorage fileStorage;

    public DashboardCatalogService(StorageManager storageManager, FileStorage fileStorage) {
        dao = storageManager;
        this.fileStorage = fileStorage;
    }

    /*
     * Dashboard apis
     */

    public Collection<Dashboard> listDashboards() {
        return dao.list(DASHBOARD_NAMESPACE);
    }

    public Collection<Dashboard> listDashboards(List<QueryParam> queryParams) {
        return dao.find(DASHBOARD_NAMESPACE, queryParams);
    }

    public Dashboard getDashboard(Long dashboardId) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(dashboardId);
        return dao.get(new StorableKey(DASHBOARD_NAMESPACE, dashboard.getPrimaryKey()));
    }

    public Dashboard addDashboard(Dashboard dashboard) {
        if (dashboard.getId() == null) {
            dashboard.setId(this.dao.nextId(DASHBOARD_NAMESPACE));
        }
        dashboard.setTimestamp(System.currentTimeMillis());
        dao.add(dashboard);
        return dashboard;
    }

    public Dashboard addOrUpdateDashboard(Long dashboardId, Dashboard dashboard) {
        dashboard.setId(dashboardId);
        dashboard.setTimestamp(System.currentTimeMillis());
        dao.addOrUpdate(dashboard);
        return dashboard;
    }

    public Dashboard removeDashboard(Long dashboardId) {
        Dashboard dashboard = new Dashboard();
        dashboard.setId(dashboardId);
        return dao.remove(new StorableKey(DASHBOARD_NAMESPACE, dashboard.getPrimaryKey()));
    }

    /*
     * Widget apis
     */

    public Collection<Widget> listWidgets() {
        return dao.list(WIDGET_NAMESPACE);
    }

    public Collection<Widget> listWidgets(List<QueryParam> queryParams) {
        return dao.find(WIDGET_NAMESPACE, queryParams);
    }

    public Widget getWidget(Long dashboardId, Long widgetId) {
        Widget widget = new Widget();
        widget.setDashboardId(dashboardId);
        widget.setId(widgetId);
        ensureDashboardExists(dashboardId);
        return dao.get(new StorableKey(WIDGET_NAMESPACE, widget.getPrimaryKey()));
    }

    public Widget addWidget(Long dashboardId, Widget widget) {
        ensureDashboardExists(dashboardId);
        if (widget.getId() == null) {
            widget.setId(this.dao.nextId(WIDGET_NAMESPACE));
        }
        widget.setDashboardId(dashboardId);
        widget.setTimestamp(System.currentTimeMillis());
        dao.add(widget);
        return widget;
    }

    public void removeWidgetDatasourceMapping(Widget widget, Set<Long> datasourceIds) {
        datasourceIds.forEach(datasourceId -> {
            dao.<WidgetDatasourceMap>remove(new WidgetDatasourceMap(widget.getId(), datasourceId).getStorableKey());
        });
    }

    public void addWidgetDatasourceMapping(Widget widget, Set<Long> datasourceIds) {
        datasourceIds.forEach(datasourceId -> {
            ensureDatasourceExists(widget.getDashboardId(), datasourceId);
            dao.<WidgetDatasourceMap>add(new WidgetDatasourceMap(widget.getId(), datasourceId));
        });
    }

    public Set<Long> getWidgetDatasourceMapping(Widget widget) {
        List<QueryParam> queryParams = Collections.singletonList(
                new QueryParam(WidgetDatasourceMap.WIDGET_ID, widget.getId().toString()));
        Collection<WidgetDatasourceMap> mappings = dao.find(WIDGET_DATASOURCE_MAPPING_NAMESPACE, queryParams);
        if (mappings != null) {
            return mappings.stream().map(WidgetDatasourceMap::getWidgetId).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public Widget addOrUpdateWidget(Long dashboardId, Long widgetId, Widget widget) {
        ensureDashboardExists(dashboardId);
        widget.setId(widgetId);
        widget.setDashboardId(dashboardId);
        widget.setTimestamp(System.currentTimeMillis());
        dao.addOrUpdate(widget);
        return widget;
    }

    public Widget removeWidget(Long dashboardId, Long widgetId) {
        ensureDashboardExists(dashboardId);
        Widget widget = new Widget();
        widget.setDashboardId(dashboardId);
        widget.setId(widgetId);
        return dao.remove(new StorableKey(WIDGET_NAMESPACE, widget.getPrimaryKey()));
    }

    /*
     * Datasource apis
     */

    public Collection<Datasource> listDatasources() {
        return dao.list(DATASOURCE_NAMESPACE);
    }

    public Collection<Datasource> listDatasources(List<QueryParam> queryParams) {
        return dao.find(DATASOURCE_NAMESPACE, queryParams);
    }

    public Datasource getDatasource(Long dashboardId, Long datasourceId) {
        Datasource datasource = new Datasource();
        datasource.setDashboardId(dashboardId);
        datasource.setId(datasourceId);
        ensureDashboardExists(dashboardId);
        return dao.get(new StorableKey(DATASOURCE_NAMESPACE, datasource.getPrimaryKey()));
    }

    public Datasource addDatasource(Long dashboardId, Datasource datasource) {
        ensureDashboardExists(dashboardId);
        if (datasource.getId() == null) {
            datasource.setId(this.dao.nextId(DATASOURCE_NAMESPACE));
        }
        datasource.setDashboardId(dashboardId);
        datasource.setTimestamp(System.currentTimeMillis());
        dao.add(datasource);
        return datasource;
    }

    public Datasource addOrUpdateDatasource(Long dashboardId, Long datasourceId, Datasource datasource) {
        ensureDashboardExists(dashboardId);
        datasource.setId(datasourceId);
        datasource.setDashboardId(dashboardId);
        datasource.setTimestamp(System.currentTimeMillis());
        dao.addOrUpdate(datasource);
        return datasource;
    }

    public Datasource removeDatasource(Long dashboardId, Long datasourceId) {
        ensureDashboardExists(dashboardId);
        Datasource datasource = new Datasource();
        datasource.setDashboardId(dashboardId);
        datasource.setId(datasourceId);
        return dao.remove(new StorableKey(DATASOURCE_NAMESPACE, datasource.getPrimaryKey()));
    }

    private void ensureDashboardExists(Long dashboardId) {
        if (getDashboard(dashboardId) == null) {
            throw new IllegalArgumentException("Dashboard with id " + dashboardId + " does not exist");
        }
    }

    private void ensureDatasourceExists(Long dashboardId, Long datasourceId) {
        if (getDatasource(dashboardId, datasourceId) == null) {
            throw new IllegalArgumentException("Datasource with id " + datasourceId + " does not exist");
        }
    }
}
