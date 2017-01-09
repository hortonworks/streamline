package org.apache.streamline.registries.dashboard.service;

import org.apache.commons.io.IOUtils;
import org.apache.streamline.common.QueryParam;
import org.apache.streamline.common.util.FileStorage;
import org.apache.streamline.registries.dashboard.entites.Dashboard;
import org.apache.streamline.registries.dashboard.entites.Datasource;
import org.apache.streamline.registries.dashboard.entites.Widget;
import org.apache.streamline.registries.dashboard.entites.WidgetDatasourceMapping;
import org.apache.streamline.storage.Storable;
import org.apache.streamline.storage.StorableKey;
import org.apache.streamline.storage.StorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DashboardCatalogService {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardCatalogService.class);
    private static final String DASHBOARD_NAMESPACE = new Dashboard().getNameSpace();
    private static final String DATASOURCE_NAMESPACE = new Datasource().getNameSpace();
    private static final String WIDGET_NAMESPACE = new Widget().getNameSpace();
    private static final String WIDGET_DATASOURCE_MAPPING_NAMESPACE = new WidgetDatasourceMapping().getNameSpace();
    private final StorageManager dao;
    private final FileStorage fileStorage;

    public DashboardCatalogService(StorageManager storageManager, FileStorage fileStorage) {
        dao = storageManager;
        dao.registerStorables(getStorableClasses());
        this.fileStorage = fileStorage;
    }

    public static Collection<Class<? extends Storable>> getStorableClasses() {
        InputStream resourceAsStream = DashboardCatalogService.class.getClassLoader().getResourceAsStream("dashboardstorables.props");
        HashSet<Class<? extends Storable>> classes = new HashSet<>();
        try {
            List<String> classNames = IOUtils.readLines(resourceAsStream);
            for (String className : classNames) {
                classes.add((Class<? extends Storable>) Class.forName(className));
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return classes;
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
            dao.<WidgetDatasourceMapping>remove(new WidgetDatasourceMapping(widget.getId(), datasourceId).getStorableKey());
        });
    }

    public void addWidgetDatasourceMapping(Widget widget, Set<Long> datasourceIds) {
        datasourceIds.forEach(datasourceId -> {
            ensureDatasourceExists(widget.getDashboardId(), datasourceId);
            dao.<WidgetDatasourceMapping>add(new WidgetDatasourceMapping(widget.getId(), datasourceId));
        });
    }

    public Set<Long> getWidgetDatasourceMapping(Widget widget) {
        List<QueryParam> queryParams = Collections.singletonList(
                new QueryParam(WidgetDatasourceMapping.WIDGET_ID, widget.getId().toString()));
        Collection<WidgetDatasourceMapping> mappings = dao.find(WIDGET_DATASOURCE_MAPPING_NAMESPACE, queryParams);
        if (mappings != null) {
            return mappings.stream().map(WidgetDatasourceMapping::getWidgetId).collect(Collectors.toSet());
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
