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
package com.hortonworks.streamline.streams.catalog.configuration;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "configuration")
public class HadoopXml {
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "property")
  private List<HadoopXmlProperty> properties = new ArrayList<>();

  public List<HadoopXmlProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<HadoopXmlProperty> properties) {
    this.properties = properties;
  }

  public void addProperty(HadoopXmlProperty property) {
    properties.add(property);
  }
}
