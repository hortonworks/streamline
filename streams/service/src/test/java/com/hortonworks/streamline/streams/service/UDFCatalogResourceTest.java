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
package com.hortonworks.streamline.streams.service;

import com.hortonworks.streamline.common.util.FileStorage;
import com.hortonworks.streamline.common.util.LocalFileSystemStorage;
import com.hortonworks.streamline.streams.catalog.UDF;
import com.hortonworks.streamline.streams.catalog.service.StreamCatalogService;
import com.hortonworks.streamline.streams.security.StreamlineAuthorizer;
import com.hortonworks.streamline.streams.security.impl.NoopAuthorizer;
import mockit.Injectable;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.hortonworks.streamline.streams.layout.component.rule.expression.Udf.Type.FUNCTION;


/**
 * This unit test is very incomplete, but it tests basic functionality of {@link UDFCatalogResource#processUdf},
 * which is the kernel functionality of Custom UDF uploading.
 *
 * The tests are tightly bound to a tiny test jar, generated in module custom-udf-microtest and
 * copied to path {@link #CUSTOM_UDF_MICROTEST_PATH} in test-compile phase of maven processing.
 *
 * In each test case we mock catalogService and use NoopAuthorizer, with fileStorage set to a private
 * directory with randomized name under system tmp dir.  We then invoke processUdf() in a way similar to
 * how {@link UDFCatalogResource#addUDF} calls it when the GUI uploads a Custom UDF.
 * Any difficulty in the upload process will result in an exception at validation phase
 * (when processUdf() calls {@link UDFCatalogResource#validateUDF})
 * even though there are no Asserts evident in the test cases.
 *
 * Note: The de-duplication of jar uploads is a function of StreamCatalogService, so
 * we can't test it here as we spoof that class.
 */
public class UDFCatalogResourceTest {

  public static final String CUSTOM_UDF_MICROTEST_PATH = "target/generated-test-resources/customudfupload";
  public static final String MICROTEST_JAR_NAME = "custom-udf-microtest-jar-with-dependencies.jar";
  public static final String MICROTEST_PACKAGE = "hortonworks.hdf.sam.custom.test.udf";

  private static final Logger LOG = LoggerFactory.getLogger(UDFCatalogResourceTest.class);
  private static final StreamlineAuthorizer authorizer = new NoopAuthorizer();
  @Injectable private StreamCatalogService catalogService;

  String testStorageDirectory;
  FileStorage fileStorage;
  UDFCatalogResource catalogResource;

  @Before
  public void init() {
    final String uuid = UUID.randomUUID().toString();
    this.testStorageDirectory = System.getProperty("java.io.tmpdir") + File.separator + uuid;
    this.fileStorage = new LocalFileSystemStorage();

    Map<String, String> conf = new HashMap<>();
    conf.put(LocalFileSystemStorage.CONFIG_DIRECTORY, testStorageDirectory);
    fileStorage.init(conf);
    this.catalogResource = new UDFCatalogResource(authorizer, catalogService, fileStorage);
  }

  @After
  public void destroy() {
    //There is no FileStorage.destroy(), unfortunately.
    try {
      FileUtils.forceDelete(new File(testStorageDirectory));
    } catch (IOException ioe) {
      LOG.warn("Failed to delete test storage directory {} probably due to simult access.  See exception:"
              , testStorageDirectory, ioe);
    }
  }


  @Test
  public void testProcessUdfWithSimpleClass() throws Exception{
    String fnName = "FooTriMultiplierUDF";
    String className = getFullClassName(fnName);
    UDF udf = newTestUDF(fnName, className);
    InputStream inputStream = new FileInputStream(udf.getJarStoragePath());

    catalogResource.processUdf(inputStream, udf, true, false);
  }

  @Test
  public void testProcessUdfWithInnerClassCanonicalName() throws Exception{
    String fnName = "FooAdder";
    String fqdnName = "FooInnerclassTestUDFs$FooAdder";
    String canonicalName = "FooInnerclassTestUDFs.FooAdder";
    String className = getFullClassName(canonicalName);
    UDF udf = newTestUDF(fnName, className);
    InputStream inputStream = new FileInputStream(udf.getJarStoragePath());

    catalogResource.processUdf(inputStream, udf, true, false);
    Assert.assertEquals(getFullClassName(fqdnName), udf.getClassName());
  }

  @Test
  public void testProcessUdfWithInnerClassFqdnName() throws Exception{
    String fnName = "FooPredicateGTZ";
    String fqdnName = "FooInnerclassTestUDFs$FooPredicateGTZ";
    String canonicalName = "FooInnerclassTestUDFs.FooPredicateGTZ.FooAdder";
    String className = getFullClassName(fqdnName);
    UDF udf = newTestUDF(fnName, className);
    InputStream inputStream = new FileInputStream(udf.getJarStoragePath());

    catalogResource.processUdf(inputStream, udf, true, false);
    Assert.assertEquals(getFullClassName(fqdnName), udf.getClassName());
  }


  private UDF newTestUDF(String fnName, String className) {
    //provide the same info the user would give the UI for a UDF upload
    UDF udf = new UDF();
    udf.setName(fnName);
    udf.setDisplayName(fnName);
    udf.setDescription("Test Function named " + fnName);
    udf.setType(FUNCTION);
    udf.setClassName(className);
    udf.setJarStoragePath(CUSTOM_UDF_MICROTEST_PATH + File.separator + MICROTEST_JAR_NAME);
    udf.setBuiltin(false);
    return udf;
  }

  private String getFullClassName(String partialClassName) {
    return MICROTEST_PACKAGE + "." + partialClassName;
  }

}
