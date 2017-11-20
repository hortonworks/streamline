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


package com.hortonworks.streamline.webservice;

import com.google.common.cache.CacheBuilder;
import com.hortonworks.registries.auth.Login;
import com.hortonworks.registries.common.ServletFilterConfiguration;
import com.hortonworks.registries.cache.Cache;
import com.hortonworks.registries.storage.NOOPTransactionManager;
import com.hortonworks.registries.storage.TransactionManager;
import com.hortonworks.registries.storage.TransactionManagerAware;
import com.hortonworks.streamline.common.Constants;
import com.hortonworks.streamline.common.ModuleRegistration;
import com.hortonworks.registries.common.util.FileStorage;
import com.hortonworks.streamline.common.util.ReflectionHelper;
import com.hortonworks.registries.storage.CacheBackedStorageManager;
import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.StorableKey;
import com.hortonworks.registries.storage.StorageManager;
import com.hortonworks.registries.storage.StorageManagerAware;
import com.hortonworks.registries.storage.cache.impl.GuavaCache;
import com.hortonworks.registries.storage.cache.writer.StorageWriteThrough;
import com.hortonworks.registries.storage.cache.writer.StorageWriter;
import com.hortonworks.streamline.common.exception.ConfigException;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.authentication.StreamlineKerberosRequestFilter;
import com.hortonworks.streamline.streams.security.impl.DefaultStreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.service.SecurityCatalogService;
import com.hortonworks.streamline.streams.service.GenericExceptionMapper;
import com.hortonworks.streamline.webservice.configurations.AuthorizerConfiguration;
import com.hortonworks.streamline.webservice.configurations.LoginConfiguration;
import com.hortonworks.streamline.webservice.configurations.StorageProviderConfiguration;
import com.hortonworks.streamline.webservice.configurations.StreamlineConfiguration;
import com.hortonworks.streamline.webservice.configurations.ModuleConfiguration;
import com.hortonworks.streamline.webservice.listeners.TransactionEventListener;
import com.hortonworks.streamline.webservice.resources.StreamlineConfigurationResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.ws.rs.container.ContainerRequestFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.registries.storage.util.StorageUtils.getStorableEntities;

public class StreamlineApplication extends Application<StreamlineConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(StreamlineApplication.class);

    public static void main(String[] args) throws Exception {
        new StreamlineApplication().run(args);
    }

    @Override
    public String getName() {
        return "Streamline Web Service";
    }

    @Override
    public void initialize(Bootstrap<StreamlineConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets", "/", "index.html", "static"));
        super.initialize(bootstrap);
    }

    @Override
    public void run(StreamlineConfiguration configuration, Environment environment) throws Exception {
        AbstractServerFactory sf = (AbstractServerFactory) configuration.getServerFactory();
        // disable all default exception mappers
        sf.setRegisterDefaultExceptionMappers(false);

        environment.jersey().register(GenericExceptionMapper.class);

        registerResources(configuration, environment, getSubjectFromLoginImpl(configuration));

        if (configuration.isEnableCors()) {
            List<String> urlPatterns = configuration.getCorsUrlPatterns();
            if (urlPatterns != null && !urlPatterns.isEmpty()) {
                enableCORS(environment, urlPatterns);
            }
        }

        setupCustomTrustStore(configuration);

        addServletFilters(configuration, environment);

    }

    @SuppressWarnings("unchecked")
    private void addServletFilters(StreamlineConfiguration configuration, Environment environment) {
        List<ServletFilterConfiguration> servletFilterConfigurations = configuration.getServletFilters();
        if (servletFilterConfigurations != null && !servletFilterConfigurations.isEmpty()) {
            for (ServletFilterConfiguration servletFilterConfiguration: servletFilterConfigurations) {
                try {
                    FilterRegistration.Dynamic dynamic = environment.servlets()
                            .addFilter(servletFilterConfiguration.getClassName(),
                                (Class<? extends Filter>) Class.forName(servletFilterConfiguration.getClassName()));
                    dynamic.setInitParameters(servletFilterConfiguration.getParams());
                    dynamic.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
                    LOG.info("Added servlet filter with configuration {}", servletFilterConfiguration);
                } catch (Exception e) {
                    LOG.error("Error occurred while adding servlet filter {}", servletFilterConfiguration);
                    throw new RuntimeException(e);
                }
            }
        } else {
            LOG.info("No servlet filters configured");
        }
    }

    private Subject getSubjectFromLoginImpl (StreamlineConfiguration streamlineConfiguration) {
        LoginConfiguration loginConfiguration = streamlineConfiguration.getLoginConfiguration();
        if (loginConfiguration == null) {
            return null;
        }
        try {
            Login login = (Login) Class.forName(loginConfiguration.getClassName()).newInstance();
            login.configure(loginConfiguration.getParams() != null ? loginConfiguration.getParams() : new HashMap<String, Object>(), "StreamlineServer");
            try {
                return login.login().getSubject();
            } catch (LoginException e) {
                LOG.error("Unable to login using login configuration {}", loginConfiguration);
                throw new RuntimeException(e);
            }
        } catch (InstantiationException|IllegalAccessException|ClassNotFoundException e) {
            LOG.error("Unable to instantiate loginImpl using login configuration {}", loginConfiguration);
            throw new RuntimeException(e);
        }
    }

    private void setupCustomTrustStore(StreamlineConfiguration configuration) {
        if (StringUtils.isNotEmpty(configuration.getTrustStorePath())) {
            System.setProperty("javax.net.ssl.trustStore", configuration.getTrustStorePath());
            if (StringUtils.isNotEmpty(configuration.getTrustStorePassword())) {
                System.setProperty("javax.net.ssl.trustStorePassword", configuration.getTrustStorePassword());
            }
        }
    }

    private void enableCORS(Environment environment, List<String> urlPatterns) {
        // Enable CORS headers
        final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

        // Configure CORS parameters
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Authorization,Content-Type,Accept,Origin");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");

        // Add URL mapping
        String[] urls = urlPatterns.toArray(new String[urlPatterns.size()]);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, urls);
    }

    private StorageManager getCacheBackedDao(StreamlineConfiguration configuration) {
        StorageProviderConfiguration storageProviderConfiguration = configuration.getStorageProviderConfiguration();
        final StorageManager dao = getStorageManager(storageProviderConfiguration);
        final CacheBuilder cacheBuilder = getGuavaCacheBuilder();
        final Cache<StorableKey, Storable> cache = getCache(dao, cacheBuilder);
        final StorageWriter storageWriter = getStorageWriter(dao);

        return doGetCacheBackedDao(cache, storageWriter);
    }

    private StorageManager getStorageManager(StorageProviderConfiguration storageProviderConfiguration) {
        final String providerClass = storageProviderConfiguration.getProviderClass();
        StorageManager storageManager = null;
        try {
            storageManager = (StorageManager) Class.forName(providerClass).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        storageManager.init(storageProviderConfiguration.getProperties());

        return storageManager;
    }

    private StorageWriter getStorageWriter(StorageManager dao) {
        return new StorageWriteThrough(dao);
    }

    private StorageManager doGetCacheBackedDao(Cache<StorableKey, Storable> cache, StorageWriter writer) {
        return new CacheBackedStorageManager(cache, writer);
    }

    private Cache<StorableKey, Storable> getCache(StorageManager dao, CacheBuilder guavaCacheBuilder) {
        return new GuavaCache(dao, guavaCacheBuilder);
    }

    private CacheBuilder getGuavaCacheBuilder() {
        final long maxSize = 1000;
        return CacheBuilder.newBuilder().maximumSize(maxSize);
    }

    private FileStorage getJarStorage (StreamlineConfiguration configuration, StorageManager storageManager) {
        FileStorage fileStorage = null;
        try {
            fileStorage = ReflectionHelper.newInstance(configuration.getFileStorageConfiguration().getClassName());
            fileStorage.init(configuration.getFileStorageConfiguration().getProperties());
            if (fileStorage instanceof StorageManagerAware) {
                ((StorageManagerAware) fileStorage).setStorageManager(storageManager);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
        return fileStorage;
    }

    private void registerResources(StreamlineConfiguration configuration, Environment environment, Subject subject) throws ConfigException,
            ClassNotFoundException, IllegalAccessException, InstantiationException {
        StorageManager storageManager = getCacheBackedDao(configuration);
        StorageManager effectiveStorageManager = storageManager instanceof CacheBackedStorageManager ?
                ((CacheBackedStorageManager) storageManager).getStorageManager() : storageManager;
        TransactionManager transactionManager;
        if (effectiveStorageManager instanceof TransactionManager)
            transactionManager = (TransactionManager) effectiveStorageManager;
        else
            transactionManager = new NOOPTransactionManager();
        environment.jersey().register(new TransactionEventListener(transactionManager));
        Collection<Class<? extends Storable>> streamlineEntities = getStorableEntities();
        storageManager.registerStorables(streamlineEntities);
        LOG.info("Registered streamline entities {}", streamlineEntities);
        FileStorage fileStorage = this.getJarStorage(configuration, storageManager);
        int appPort = ((HttpConnectorFactory) ((DefaultServerFactory) configuration.getServerFactory()).getApplicationConnectors().get(0)).getPort();
        String catalogRootUrl = configuration.getCatalogRootUrl().replaceFirst("8080", appPort +"");
        List<ModuleConfiguration> modules = configuration.getModules();
        List<Object> resourcesToRegister = new ArrayList<>();

        // add StreamlineConfigResource
        resourcesToRegister.add(new StreamlineConfigurationResource(configuration));


        // authorizer
        StreamlineAuthorizer authorizer;
        AuthorizerConfiguration authorizerConf = configuration.getAuthorizerConfiguration();
        SecurityCatalogService securityCatalogService = new SecurityCatalogService(storageManager);
        if (authorizerConf != null) {
            authorizer = ((Class<StreamlineAuthorizer>) Class.forName(authorizerConf.getClassName())).newInstance();
            Map<String, Object> authorizerConfig = new HashMap<>();
            authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_CATALOG_SERVICE, securityCatalogService);
            authorizerConfig.put(DefaultStreamlineAuthorizer.CONF_ADMIN_PRINCIPALS, authorizerConf.getAdminPrincipals());
            authorizer.init(authorizerConfig);
            String filterClazzName = authorizerConf.getContainerRequestFilter();
            ContainerRequestFilter filter;
            if (StringUtils.isEmpty(filterClazzName)) {
                filter = new StreamlineKerberosRequestFilter(); // default
            } else {
                filter = ((Class<ContainerRequestFilter>) Class.forName(filterClazzName)).newInstance();
            }
            LOG.info("Registering ContainerRequestFilter: {}", filter.getClass().getCanonicalName());
            environment.jersey().register(filter);
        } else {
            LOG.info("Authorizer config not set, setting noop authorizer");
            String noopAuthorizerClassName = "com.hortonworks.streamline.streams.security.impl.NoopAuthorizer";
            authorizer = ((Class<StreamlineAuthorizer>) Class.forName(noopAuthorizerClassName)).newInstance();
        }
        //
        for (ModuleConfiguration moduleConfiguration: modules) {
            String moduleName = moduleConfiguration.getName();
            String moduleClassName = moduleConfiguration.getClassName();
            LOG.info("Registering module [{}] with class [{}]", moduleName, moduleClassName);
            ModuleRegistration moduleRegistration = (ModuleRegistration) Class.forName(moduleClassName).newInstance();
            if (moduleConfiguration.getConfig() == null) {
                moduleConfiguration.setConfig(new HashMap<String, Object>());
            }
            if (moduleName.equals(Constants.CONFIG_STREAMS_MODULE)) {
                moduleConfiguration.getConfig().put(Constants.CONFIG_CATALOG_ROOT_URL, catalogRootUrl);
            }
            Map<String, Object> initConfig = new HashMap<>(moduleConfiguration.getConfig());
            initConfig.put(Constants.CONFIG_AUTHORIZER, authorizer);
            initConfig.put(Constants.CONFIG_SECURITY_CATALOG_SERVICE, securityCatalogService);
            initConfig.put(Constants.CONFIG_SUBJECT, subject);
            moduleRegistration.init(initConfig, fileStorage);
            if (moduleRegistration instanceof StorageManagerAware) {
                LOG.info("Module [{}] is StorageManagerAware and setting StorageManager.", moduleName);
                StorageManagerAware storageManagerAware = (StorageManagerAware) moduleRegistration;
                storageManagerAware.setStorageManager(storageManager);
            }
            if (moduleRegistration instanceof TransactionManagerAware) {
                LOG.info("Module [{}] is TransactionManagerAware and setting TransactionManager.", moduleName);
                TransactionManagerAware transactionManagerAware = (TransactionManagerAware) moduleRegistration;
                transactionManagerAware.setTransactionManager(transactionManager);
            }
            resourcesToRegister.addAll(moduleRegistration.getResources());

        }

        LOG.info("Registering resources to Jersey environment: [{}]", resourcesToRegister);
        for(Object resource : resourcesToRegister) {
            environment.jersey().register(resource);
        }
        environment.jersey().register(MultiPartFeature.class);
    }
}
