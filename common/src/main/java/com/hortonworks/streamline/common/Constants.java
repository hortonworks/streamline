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


package com.hortonworks.streamline.common;


public class Constants {

    private Constants() {
    }
    public static final String CATALOG_ROOT_URL = "catalog.root.url";
    public static final String LOCAL_FILES_PATH = "local.files.path";
    public static final String CONFIG_CATALOG_ROOT_URL = "catalogRootUrl";
    public static final String CONFIG_AUTHORIZER = "authorizer";
    public static final String CONFIG_SECURITY_CATALOG_SERVICE = "securityCatalogService";
    public static final String CONFIG_MODULES = "modules";
    public static final String CONFIG_SCHEMA_REGISTRY_URL = "schemaRegistryUrl";
    public static final String CONFIG_STREAMS_MODULE = "streams";
    public static final String CONFIG_SUBJECT = "subject";

    public static final String JAAS_STREAMLINE_APP_CONFIG_ENTRY_NAME = "StreamlineServer";
}
