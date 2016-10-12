package com.hortonworks.iotas.streams.cluster.discovery.ambari;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.hortonworks.iotas.streams.cluster.discovery.ServiceNodeDiscoverer;
import com.hortonworks.iotas.streams.exception.ConfigException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Services and nodes discover using Ambari.
 */
public class AmbariServiceNodeDiscoverer implements ServiceNodeDiscoverer {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariServiceNodeDiscoverer.class);

  public static final String CONFIGURATIONS_URL = "/configurations";
  public static final String SERVICES_URL = "/services";
  public static final String SERVICE_URL = "/services/%s";
  public static final String COMPONENT_URL = "/services/%s/components/%s";

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

    Map<String, ?> responseMap = client.target(targetUrl).request(MediaType.TEXT_PLAIN_TYPE).get(Map.class);
    List<Map<String, ?>> services = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_ITEMS);

    if (services.size() > 0) {
      for (Map<String, ?> service : services) {
        Map<String, ?> componentInfo = (Map<String, ?>) service.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_SERVICE_INFO);
        String serviceName = (String) componentInfo.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_SERVICE_NAME);
        serviceNames.add(serviceName);
      }
    }

    return serviceNames;
  }

  @Override public List<String> getComponents(String serviceName) {
    List<String> componentNames = new ArrayList<>();

    String targetUrl = String.format(apiRootUrl + SERVICE_URL, serviceName);

    LOG.debug("components URI: {}", targetUrl);

    Map<String, ?> responseMap = client.target(targetUrl).request(MediaType.TEXT_PLAIN_TYPE).get(Map.class);
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

    Map<String, ?> responseMap = client.target(targetUrl).request(MediaType.TEXT_PLAIN_TYPE).get(Map.class);
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

  @Override public Map<String, Map<String, Object>> getConfigurations(String serviceName) {
    // this will throw IllegalArgumentException if service is not supported yet.
    ServiceConfigurations serviceConfigurations;
    try {
      serviceConfigurations = ServiceConfigurations.valueOf(serviceName.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("service " + serviceName + " is not supported.");
    }

    String[] confNames = serviceConfigurations.getConfNames();
    List<String> confNameList = Lists.newArrayList(confNames);

    Map<String, Map<String, Object>> configurations = new HashMap<>();

    String targetUrl = apiRootUrl + CONFIGURATIONS_URL;

    LOG.debug("configurations URI: {}", targetUrl);

    Map<String, ?> responseMap = client.target(targetUrl).request(MediaType.TEXT_PLAIN_TYPE).get(Map.class);
    List<Map<String, ?>> items = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_ITEMS);

    if (items.size() > 0) {
      Map<String, ServiceConfigurationItem> confToItem = extractLatestConfigurationItems(
          confNameList, items);

      for (ServiceConfigurationItem confItem : confToItem.values()) {
        configurations.put(confItem.getType(), getProperties(confItem));
      }
    }

    return configurations;
  }

  @Override
  public String getActualFileName(String configType) {
    return ConfigFilePattern.getActualFileName(configType);
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

  private Map<String, Object> getProperties(ServiceConfigurationItem confItem) {
    String targetUrl = confItem.getHref();

    LOG.debug("configuration item URI: {}", targetUrl);

    Map<String, ?> responseMap = client.target(targetUrl).request(MediaType.TEXT_PLAIN_TYPE).get(Map.class);
    List<Map<String, ?>> items = (List<Map<String, ?>>) responseMap.get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_ITEMS);

    if (items.size() > 0) {
      return (Map<String, Object>) items.get(0).get(AmbariRestAPIConstants.AMBARI_JSON_SCHEMA_COMMON_PROPERTIES);
    }

    return Collections.emptyMap();
  }

  private void setupClient() {
    HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
        .credentials(username, password).build();
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(feature);
    clientConfig.register(JsonToMapProvider.class);

    client = ClientBuilder.newClient(clientConfig);
  }

  @Provider
  public static class JsonToMapProvider implements MessageBodyReader<Map> {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
        MediaType mediaType) {
      return (MediaType.APPLICATION_JSON_TYPE.equals(mediaType) ||
          MediaType.TEXT_PLAIN_TYPE.equals(mediaType));
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