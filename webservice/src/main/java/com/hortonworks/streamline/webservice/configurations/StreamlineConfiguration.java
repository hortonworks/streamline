/**
 * Copyright 2017 Hortonworks.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package com.hortonworks.streamline.webservice.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hortonworks.registries.common.ServletFilterConfiguration;

import com.hortonworks.streamline.webservice.util.StreamlineConfigurationSerializer;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;

@JsonSerialize(using = StreamlineConfigurationSerializer.class)

public class StreamlineConfiguration extends Configuration {

    @NotEmpty
    private List<ModuleConfiguration> modules;

    @NotEmpty
    private String catalogRootUrl;

    @NotNull
    private FileStorageConfiguration fileStorageConfiguration;

    @NotNull
    private StorageProviderConfiguration storageProviderConfiguration;

    @NotNull
    private DashboardConfiguration dashboardConfiguration;

    private AuthorizerConfiguration authorizerConfiguration;


    @JsonProperty
    private boolean enableCors;

    @JsonProperty
    private List<String> corsUrlPatterns;

    @JsonProperty
    private String trustStorePath;

    @JsonProperty
    private String trustStorePassword;

    private List<ServletFilterConfiguration> servletFilters;

    private LoginConfiguration loginConfiguration;

    private String httpProxyUrl;
    private String httpProxyUsername;
    private String httpProxyPassword;

    @JsonProperty
    public StorageProviderConfiguration getStorageProviderConfiguration() {
        return storageProviderConfiguration;
    }

    @JsonProperty
    public void setStorageProviderConfiguration(StorageProviderConfiguration storageProviderConfiguration) {
        this.storageProviderConfiguration = storageProviderConfiguration;
    }

   public String getCatalogRootUrl() {
        return catalogRootUrl;
    }

    public void setCatalogRootUrl(String catalogRootUrl) {
        this.catalogRootUrl = catalogRootUrl;
    }

    public FileStorageConfiguration getFileStorageConfiguration() {
        return this.fileStorageConfiguration;
    }

    public void setFileStorageConfiguration(FileStorageConfiguration configuration) {
        this.fileStorageConfiguration = configuration;
    }

    public List<ModuleConfiguration> getModules() {
        return modules;
    }

    public void setModules(List<ModuleConfiguration> modules) {
        this.modules = modules;
    }

    public boolean isEnableCors() {
        return enableCors;
    }

    public void setEnableCors(boolean enableCors) {
        this.enableCors = enableCors;
    }

    public List<String> getCorsUrlPatterns() {
        return corsUrlPatterns;
    }

    public void setCorsUrlPatterns(List<String> corsUrlPatterns) {
        this.corsUrlPatterns = corsUrlPatterns;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public DashboardConfiguration getDashboardConfiguration() {
        return dashboardConfiguration;
    }

    public void setDashboardConfiguration(DashboardConfiguration dashboardConfiguration) {
        this.dashboardConfiguration = dashboardConfiguration;
    }


    public AuthorizerConfiguration getAuthorizerConfiguration() {
        return authorizerConfiguration;
    }

    public void setAuthorizerConfiguration(AuthorizerConfiguration authorizerConfiguration) {
        this.authorizerConfiguration = authorizerConfiguration;
    }

    public List<ServletFilterConfiguration> getServletFilters() {
        return servletFilters;
    }

    public void setServletFilters(List<ServletFilterConfiguration> servletFilters) {
        this.servletFilters = servletFilters;
    }

    public LoginConfiguration getLoginConfiguration() {
        return loginConfiguration;
    }

    public void setLoginConfiguration(LoginConfiguration loginConfiguration) {
        this.loginConfiguration = loginConfiguration;
    }

    public String getHttpProxyUrl() {
        return httpProxyUrl;
    }

    public void setHttpProxyUrl(String httpProxyUrl) {
        this.httpProxyUrl = httpProxyUrl;
    }

    public String getHttpProxyUsername() {
        return httpProxyUsername;
    }

    public void setHttpProxyUsername(String httpProxyUsername) {
        this.httpProxyUsername = httpProxyUsername;
    }

    public String getHttpProxyPassword() {
        return httpProxyPassword;
    }

    public void setHttpProxyPassword(String httpProxyPassword) {
        this.httpProxyPassword = httpProxyPassword;
    }



}
