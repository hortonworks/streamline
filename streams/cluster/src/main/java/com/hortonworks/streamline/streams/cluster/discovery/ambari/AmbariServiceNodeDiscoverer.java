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
package com.hortonworks.streamline.streams.cluster.discovery.ambari;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hortonworks.streamline.common.JsonClientUtil;
import com.hortonworks.streamline.common.exception.WrappedWebApplicationException;
import com.hortonworks.streamline.streams.cluster.discovery.ServiceNodeDiscoverer;
import com.hortonworks.streamline.streams.exception.ConfigException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Services and nodes discover using Ambari.
 */
public class AmbariServiceNodeDiscoverer implements ServiceNodeDiscoverer {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariServiceNodeDiscoverer.class);

  public static final String CONFIGURATIONS_URL = "/configurations";
  public static final String SERVICES_URL = "/services";
  public static final String SERVICE_URL = "/services/%s";
  public static final String COMPONENT_URL = "/services/%s/components/%s";
  public static final String AMBARI_VIEWS_STORM_MONITORING_URL = "/views/Storm_Monitoring";
  public static final MediaType AMBARI_REST_API_MEDIA_TYPE = MediaType.TEXT_PLAIN_TYPE;

  private Client client;
  private final String apiRootUrl;

  private final String username;
  private final String password;

  public AmbariServiceNodeDiscoverer(String apiRootUrl, String username, String password) {
    this.apiRootUrl = apiRootUrl;
    this.username = username;
    this.password = password;
  }

  @Override
  public void init(Map<String, String> conf) throws ConfigException {
    setupClient();
  }

  @Override public List<String> getServices() {
    List<String> serviceNames = new ArrayList<>();

    String targetUrl = apiRootUrl + SERVICES_URL;

    LOG.debug("services URI: {}", targetUrl);

    try {
      Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);
      List<Map<String, ?>> services = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_ITEMS);

      if (services.size() > 0) {
        for (Map<String, ?> service : services) {
          Map<String, ?> componentInfo = (Map<String, ?>) service.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_SERVICE_INFO);
          String serviceName = (String) componentInfo.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_SERVICE_NAME);
          serviceNames.add(serviceName);
        }
      }

      return serviceNames;
    } catch (WebApplicationException e) {
      throw WrappedWebApplicationException.of(e);
    }
  }

  @Override public List<String> getComponents(String serviceName) {
    List<String> componentNames = new ArrayList<>();

    String targetUrl = String.format(apiRootUrl + SERVICE_URL, serviceName);

    LOG.debug("components URI: {}", targetUrl);

    Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);
    List<Map<String, ?>> components = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMPONENTS);

    if (components.size() > 0) {
      for (Map<String, ?> component : components) {
        Map<String, ?> componentInfo = (Map<String, ?>) component.get(AmbariRestAPIConstants.ABARI_JSON_SCHEMA_SERVICE_COMPONENT_INFO);
        String componentName = (String) componentInfo.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMPONENT_NAME);
        componentNames.add(componentName);
      }
    }

    return componentNames;
  }

  @Override public List<String> getComponentNodes(String serviceName, String componentName) {
    List<String> componentNodes = new ArrayList<>();

    String targetUrl = String.format(apiRootUrl + COMPONENT_URL, serviceName, componentName);

    LOG.debug("host components URI: {}", targetUrl);

    Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);
    List<Map<String, ?>> hostComponents = (List<Map<String, ?>>) responseMap.get(
            AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_HOST_COMPONENTS);

    if (hostComponents.size() > 0) {
      for (Map<String, ?> hostComponent : hostComponents) {
        Map<String, ?> hostRoles = (Map<String, ?>) hostComponent.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_HOST_ROLES);
        String hostName = (String) hostRoles.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_HOST_NAME);
        componentNodes.add(hostName);
      }
    }

    return componentNodes;
  }

  @Override public Map<String, Map<String, String>> getConfigurations(String serviceName) {
    // this will throw IllegalArgumentException if service is not supported yet.
    ServiceConfigurations serviceConfigurations;
    try {
      serviceConfigurations = ServiceConfigurations.valueOf(serviceName.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("service " + serviceName + " is not supported.");
    }

    List<String> confNameList = createAmbariConfNameList(serviceConfigurations);

    Map<String, Map<String, String>> configurations = new HashMap<>();

    String targetUrl = apiRootUrl + CONFIGURATIONS_URL;

    LOG.debug("configurations URI: {}", targetUrl);

    try {
      Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);
      List<Map<String, ?>> items = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_ITEMS);

      if (items.size() > 0) {
        Map<String, ServiceConfigurationItem> confToItem = extractLatestConfigurationItems(
                confNameList, items);

        for (ServiceConfigurationItem confItem : confToItem.values()) {
          // convert Ambari type to the actual Service's configuration file
          String type = getOriginConfigTypeName(confItem.getType());
          configurations.put(type, getProperties(confItem));
        }
      }

      return configurations;
    } catch (WebApplicationException e) {
      throw WrappedWebApplicationException.of(e);
    }
  }

  private List<String> createAmbariConfNameList(ServiceConfigurations serviceConfigurations) {
    String[] confNames = serviceConfigurations.getConfNames();
    return Arrays.stream(confNames).map(confName -> {
      AmbariConfigTypeRollbackPattern pattern = AmbariConfigTypeRollbackPattern.lookupByOriginConfType(confName);
      if (pattern != null) {
        return pattern.ambariConfType();
      }
      return confName;
    }).collect(toList());
  }

  private String getOriginConfigTypeName(String confItemType) {
    AmbariConfigTypeRollbackPattern pattern = AmbariConfigTypeRollbackPattern.lookupByAmbariConfType(confItemType);
    if (pattern != null) {
      return pattern.originConfType();
    }
    return confItemType;
  }


  @Override
  public String getOriginFileName(String configType) {
    return ConfigFilePattern.getOriginFileName(configType);
  }

  public String getStormViewUrl() {
    String targetUrl = findRootUrlForView(apiRootUrl) + AMBARI_VIEWS_STORM_MONITORING_URL;

    LOG.debug("storm view URI: {}", targetUrl);

    Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);
    List<Map<String, ?>> items = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_VERSIONS);

    if (items.size() == 0) {
      return null;
    }

    String versionUrl = (String) items.get(0).get("href");

    responseMap = JsonClientUtil.getEntity(client.target(versionUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);
    items = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_INSTANCES);

    if (items.size() == 0) {
      return null;
    }

    String instancesUrl = (String) items.get(0).get("href");

    responseMap = JsonClientUtil.getEntity(client.target(instancesUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);

    Map<String, ?> responseMap2 = (Map<String, ?>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_VIEW_INSTANCE_INFO);
    String contextPath = (String) responseMap2.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_CONTEXT_PATH);

    // page doesn't open without hash path from Ambari 2.4, not sure for other version
    return findSiteUrl(apiRootUrl) + "/#/main" + contextPath;
  }

  private String findRootUrlForView(String apiRootUrl) {
    int clustersIdx = apiRootUrl.indexOf("/clusters/");
    if (clustersIdx == -1) {
      throw new IllegalArgumentException("Ambari API Root URL should contains 'clusters'");
    }
    return apiRootUrl.substring(0, clustersIdx);
  }

  private String findSiteUrl(String apiRootUrl) {
    try {
      URL url = new URL(apiRootUrl);
      return String.format("%s://%s:%s", url.getProtocol(), url.getHost(),
              url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, ServiceConfigurationItem> extractLatestConfigurationItems(List<String> confNameList,
      List<Map<String, ?>> items) {
    Map<String, ServiceConfigurationItem> confToItem = new HashMap<>();
    for (Map<String, ?> item : items) {
      String type = (String) item.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_TYPE);
      if (!confNameList.contains(type)) {
        continue;
      }

      Integer version = ((Number) item.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_VERSION)).intValue();
      String tag = (String) item.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_TAG);
      String href = (String) item.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_HREF);

      ServiceConfigurationItem latestItem = confToItem.get(type);
      if (latestItem == null || latestItem.getVersion() < version) {
        confToItem.put(type, new ServiceConfigurationItem(type, version, tag, href));
      }
    }
    return confToItem;
  }

  private Map<String, String> getProperties(ServiceConfigurationItem confItem) {
    String targetUrl = confItem.getHref();

    LOG.debug("configuration item URI: {}", targetUrl);

    try {
      Map<String, ?> responseMap = JsonClientUtil.getEntity(client.target(targetUrl), AMBARI_REST_API_MEDIA_TYPE, Map.class);
      List<Map<String, ?>> items = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_ITEMS);

      if (items.size() > 0) {
        return (Map<String, String>) items.get(0).get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_PROPERTIES);
      }

      return Collections.emptyMap();
    } catch (WebApplicationException e) {
      throw WrappedWebApplicationException.of(e);
    }
  }

  private void setupClient() {
    HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
        .credentials(username, password).build();
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(feature);
    clientConfig.register(JsonToMapProvider.class);

    client = ClientBuilder.newClient(clientConfig);
  }

  public void validateApiUrl() {
    // just calling getServices() to retrieve service list from Ambari REST API
    // it also parses the response, so if API is not valid, any exceptions should be thrown
    getServices();
  }

  @Provider
  public static class JsonToMapProvider implements MessageBodyReader<Map> {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
      return (MediaType.APPLICATION_JSON_TYPE.equals(mediaType) ||
          AMBARI_REST_API_MEDIA_TYPE.equals(mediaType));
    }

    @Override
    public Map<String, Object> readFrom(Class<Map> aClass, Type type,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
        throws IOException, WebApplicationException {
      return objectMapper.readValue(inputStream, aClass);
    }

  }
}