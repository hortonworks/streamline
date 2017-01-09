/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.streamline.registries.model.service;

import com.codahale.metrics.annotation.Timed;
import org.apache.streamline.common.exception.service.exception.request.BadRequestException;
import org.apache.streamline.common.util.WSUtils;
import org.apache.streamline.registries.model.data.MLModelInfo;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/v1/catalog")
@Produces(MediaType.APPLICATION_JSON)
public final class MLModelRegistryResource {
    private static final Logger LOG = LoggerFactory.getLogger(MLModelRegistryResource.class);
    private final MLModelRegistryService modelRegistryService;

    public MLModelRegistryResource(MLModelRegistryService modelRegistryService) {
        this.modelRegistryService = modelRegistryService;
    }

    @GET
    @Path("ml/models")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response listModelInfos()  {
        return WSUtils.respondEntities(modelRegistryService.listModelInfos(), OK);
    }

    /**
     * Add a model info
     * <p>
     * modelInfo.json
     * {
     *     "name": "ModelName"
     * }
     * curl -sS -X POST -F modelInfo="@modelInfo.json;type=text/json" -F pmmlFile=@modelFile http://localhost:8080/api/v1/catalog/ml/models
     * </p>
     * <pre>
     *{
     *      "responseCode":1000,
     *      "responseMessage":"Success",
     *      "entities":{
     *                     "id": 1,
     *                     "modelName": "model",
     *                     "pmmlFileName":"modelFile"
     *                     "timestamp":...
     *                 }
     *}
     * </pre>
     */
    @POST
    @Path("ml/models")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Timed
    public Response addModelInfo(@FormDataParam("modelInfo") final MLModelInfo modelInfo,
                                 @FormDataParam("pmmlFile") final InputStream pmmlInputStream,
                                 @FormDataParam("pmmlFile") final FormDataContentDisposition fileDisposition) throws IOException, SAXException, JAXBException {
        if (pmmlInputStream == null) {
            throw BadRequestException.missingParameter("pmmlFile");
        }
        try {
            MLModelInfo createdModelInfo = modelRegistryService.addModelInfo(modelInfo, pmmlInputStream, fileDisposition.getFileName());
            return WSUtils.respondEntity(createdModelInfo, CREATED);
        } catch (Exception exception) {
            LOG.debug("Error occured while adding the pmml model", exception);
            throw exception;
        } finally {
            try {
                if (pmmlInputStream != null) {
                    pmmlInputStream.close();
                }
            } catch (IOException exception) {
                LOG.debug("Error while closing the pmml file stream", exception);
                throw exception;
            }
        }
    }

    /**
     * Add a model info
     * modelInfo.json
     * {
     *     "name": "ChangedModelName"
     * }
     *
     * <p>
     * curl -sS -X PUT -F modelInfo="@modelInfo.json;type=text/json" http://localhost:8080/api/v1/catalog/ml/models/1
     * </p>
     * <pre>
     *{
     *      "responseCode":1000,
     *      "responseMessage":"Success",
     *      "entities":{
     *                     "id": 1,
     *                     "modelName": "model",
     *                     "pmmlFileName":"modelFile"
     *                     "timestamp":...
     *                 }
     *}
     * </pre>
     */
    @PUT
    @Path("/ml/models/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Timed
    public Response addOrUpdateModelInfo(
            @PathParam("id") final Long modelId,
            @FormDataParam("modelInfo") final MLModelInfo modelInfo,
            @FormDataParam("pmmlFile") final InputStream pmmlInputStream,
            @FormDataParam("pmmlFile") final FormDataContentDisposition fileDisposition) throws IOException, SAXException, JAXBException {
        if (pmmlInputStream == null) {
            throw BadRequestException.missingParameter("pmmlFile");
        }

        try {
            MLModelInfo createdModelInfo = modelRegistryService.addOrUpdateModelInfo(
                    modelId, modelInfo, pmmlInputStream, fileDisposition.getFileName());
            return WSUtils.respondEntity(createdModelInfo, CREATED);
        } catch (Exception exception) {
            LOG.debug("Error occured while adding the pmml model", exception);
            throw exception;
        } finally {
            try {
                if (pmmlInputStream != null) {
                    pmmlInputStream.close();
                }
            } catch (IOException exception) {
                LOG.debug("Error while closing the pmml file stream", exception);
                throw exception;
            }
        }
    }

    /**
     * Add a model info
     * <p>
     * curl -sS -X DELETE http://localhost:8080/api/v1/catalog/ml/models/1
     * </p>
     * <pre>
     *{
     *      "responseCode":1000,
     *      "responseMessage":"Success",
     *      "entities":{
     *                     "id": 1,
     *                     "modelName": "model",
     *                     "pmmlFileName":"modelFile"
     *                     "timestamp":...
     *                 }
     *}
     * </pre>
     */
    @DELETE
    @Path("ml/models/{id}")
    @Timed
    public Response removeModelInfo(@PathParam("id") final Long modelId) {
        MLModelInfo removedModelInfo = modelRegistryService.removeModelInfo(modelId);
        return WSUtils.respondEntity(removedModelInfo, OK);
    }

    /**
     * Get a model info by name
     * <p>
     * curl -sS -X GET http://localhost:8080/api/v1/catalog/ml/models/names/testModel
     * </p>
     * <pre>
     *{
     *      "responseCode":1000,
     *      "responseMessage":"Success",
     *      "entities":{
     *                     "id": 1,
     *                     "modelName": "model",
     *                     "pmmlFileName":"modelFile"
     *                     "timestamp":...
     *                 }
     *}
     * </pre>
     */
    @GET
    @Path("ml/models/names/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response getModelInfoByName(@PathParam("name") final String modelName) {
        MLModelInfo modelInfo = modelRegistryService.getModelInfo(modelName);
        return WSUtils.respondEntity(modelInfo, OK);
    }

    /**
     * Get a model info by id
     * <p>
     * curl -sS -X GET http://localhost:8080/api/v1/catalog/models/1
     * </p>
     * <pre>
     *{
     *      "responseCode":1000,
     *      "responseMessage":"Success",
     *      "entities":{
     *                     "id": 1,
     *                     "modelName": "model",
     *                     "pmmlFileName":"modelFile"
     *                     "timestamp":...
     *                 }
     *}
     * </pre>
     */
    @GET
    @Path("ml/models/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response getModelInfoById(@PathParam("id") final Long modelId) {
        MLModelInfo modelInfo = modelRegistryService.getModelInfo(modelId);
        return WSUtils.respondEntity(modelInfo, OK);
    }

    /**
     * Get model fields by id
     * <p>
     * curl -sS -X GET http://localhost:8080/api/v1/catalog/ml/models/1/fields/output
     * </p>
     * <pre>
     *{
     *      "responseCode":1000,
     *      "responseMessage":"Success",
     *      "entities":[
     *                      {"name": "fieldName", "type": "fieldType"}
     *                      ...
     *                 ]
     *}
     * </pre>
     */
    @GET
    @Path("ml/models/{id}/fields/output")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response getModelOutputFieldsById(@PathParam("id") final Long modelId) throws Exception {
        MLModelInfo modelInfo = modelRegistryService.getModelInfo(modelId);
        final List<MLModelField> fieldNames = modelRegistryService.getModelOutputFields(modelInfo);
        return WSUtils.respondEntity(fieldNames, OK);
    }

    /**
     * Get model fields by id
     * <p>
     * curl -sS -X GET http://localhost:8080/api/v1/catalog/ml/models/1/fields/input
     * </p>
     * <pre>
     *{
     *      "responseCode":1000,
     *      "responseMessage":"Success",
     *      "entities":[
     *                      {"name": "fieldName", "type": "fieldType"}
     *                      ...
     *                      ...
     *                 ]
     *}
     * </pre>
     */
    @GET
    @Path("ml/models/{id}/fields/input")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Response getModelInputFieldsById(@PathParam("id") final Long modelId) throws Exception {
        MLModelInfo modelInfo = modelRegistryService.getModelInfo(modelId);
        final List<MLModelField> fieldNames = modelRegistryService.getModelInputFields(modelInfo);
        return WSUtils.respondEntity(fieldNames, OK);
    }

    /*
    * API endpoint to get the model file contents associated with the id.
    * <p>
    *     curl -sS -X GET http://localhost:8080/api/v1/catalog/ml/models/pmml/{name}
    * </p>
    */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("ml/models/pmml/{name}")
    @Timed
    public Response getMLModelContents(@PathParam("name") String modelName) {
        MLModelInfo modelInfo = modelRegistryService.getModelInfo(modelName);
        return WSUtils.respondEntity(modelInfo.getPmml(), OK);
    }
}
