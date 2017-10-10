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

package com.hortonworks.streamline.registries.model.service;

import org.apache.commons.io.IOUtils;
import com.hortonworks.registries.common.QueryParam;
import com.hortonworks.streamline.common.exception.service.exception.request.EntityNotFoundException;
import com.hortonworks.streamline.registries.model.data.MLModel;
import com.hortonworks.registries.storage.Storable;
import com.hortonworks.registries.storage.StorageManager;
import com.hortonworks.registries.storage.util.StorageUtils;
import org.dmg.pmml.Field;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.IOUtil;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.ResultFeatureType;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class MLModelRegistryService {
    private static final Logger LOG = LoggerFactory.getLogger(MLModelRegistryService.class);
    private static final String ML_MODEL_NAME_SPACE = new MLModel().getNameSpace();
    private final StorageManager storageManager;
    public MLModelRegistryService(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public Collection<MLModel> listModelInfos() {
        return storageManager.list(ML_MODEL_NAME_SPACE);
    }

    public Collection<MLModel> listModelInfo(List<QueryParam> params) {
        return storageManager.find(ML_MODEL_NAME_SPACE, params);
    }

    public MLModel addModelInfo(
            MLModel modelInfo, InputStream pmmlInputStream, String fileName) throws IOException, SAXException, JAXBException{
        if (modelInfo.getId() == null) {
            modelInfo.setId(storageManager.nextId(ML_MODEL_NAME_SPACE));
        }

        LOG.debug("Adding model " + modelInfo.getName());
        modelInfo.setTimestamp(System.currentTimeMillis());
        modelInfo.setPmml(IOUtils.toString(pmmlInputStream, Charset.defaultCharset()));
        modelInfo.setUploadedFileName(fileName);

        validateModelInfo(modelInfo);
        this.storageManager.add(modelInfo);
        return modelInfo;
    }

    public MLModel addOrUpdateModelInfo(
            Long modelId, MLModel modelInfo,
            InputStream pmmlInputStream,
            String fileName) throws IOException, SAXException, JAXBException {
        modelInfo.setId(modelId);
        modelInfo.setTimestamp(System.currentTimeMillis());
        modelInfo.setPmml(IOUtils.toString(pmmlInputStream, Charset.defaultCharset()));
        modelInfo.setUploadedFileName(fileName);

        validateModelInfo(modelInfo);
        this.storageManager.addOrUpdate(modelInfo);
        return modelInfo;
    }

    public MLModel getModelInfo(String name) {
        List<QueryParam> queryParams = Collections.singletonList(new QueryParam(MLModel.NAME, name));
        Collection<MLModel> modelInfos = this.storageManager.find(ML_MODEL_NAME_SPACE, queryParams);
        if (modelInfos.size() == 0) {
            throw EntityNotFoundException.byName(name);
        }
        return modelInfos.iterator().next();
    }
    public MLModel getModelInfo(Long modelId) {
        MLModel modelInfo = new MLModel();
        modelInfo.setId(modelId);
        MLModel storedModelInfo = this.storageManager.get(modelInfo.getStorableKey());
        if (storedModelInfo == null) {
            throw EntityNotFoundException.byId(modelId.toString());
        }

        return storedModelInfo;
    }

    public MLModel removeModelInfo(Long modelId) {
        MLModel modelInfo = new MLModel();
        modelInfo.setId(modelId);
        MLModel removedModelInfo = this.storageManager.remove(modelInfo.getStorableKey());
        if (removedModelInfo == null) {
            throw EntityNotFoundException.byId(modelId.toString());
        }

        return removedModelInfo;
    }

    public List<MLModelField> getModelOutputFields(MLModel modelInfo) throws IOException, SAXException, JAXBException {
        return doGetOutputFieldsForPMMLStream(modelInfo.getPmml());
    }

    private List<MLModelField> doGetOutputFieldsForPMMLStream(String pmmlContents) throws SAXException, JAXBException, UnsupportedEncodingException {
        List<MLModelField> fieldNames = new ArrayList<>();
        PMMLManager pmmlManager = new PMMLManager(IOUtil.unmarshal(new ByteArrayInputStream(pmmlContents.getBytes("UTF-8"))));
        Evaluator modelEvaluator = (ModelEvaluator<?>) pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());
        modelEvaluator.getPredictedFields().forEach((f) -> fieldNames.add(getModelField(modelEvaluator.getDataField(f))));

        modelEvaluator.getOutputFields().forEach((f) -> {
            OutputField outputField = modelEvaluator.getOutputField(f);
            ResultFeatureType resultFeatureType = outputField.getFeature();
            if (resultFeatureType != ResultFeatureType.PREDICTED_VALUE &&
                    resultFeatureType != ResultFeatureType.PREDICTED_DISPLAY_VALUE) {
                fieldNames.add(getModelField(outputField));
            }
        });

        return fieldNames;
    }

    public List<MLModelField> getModelInputFields(MLModel modelInfo) throws IOException, SAXException, JAXBException {
        return doGetInputFieldsFromPMMLStream(modelInfo.getPmml());
    }

    private List<MLModelField> doGetInputFieldsFromPMMLStream(String pmmlContents) throws SAXException, JAXBException, UnsupportedEncodingException {
        final List<MLModelField> fieldNames = new ArrayList<>();
        PMMLManager pmmlManager = new PMMLManager(IOUtil.unmarshal(new ByteArrayInputStream(pmmlContents.getBytes("UTF-8"))));
        Evaluator modelEvaluator = (ModelEvaluator<?>) pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());
        for (FieldName predictedField: modelEvaluator.getActiveFields()) {
            fieldNames.add(getModelField(modelEvaluator.getDataField(predictedField)));
        }
        return fieldNames;
    }

    private MLModelField getModelField(Field dataField) {
        return new MLModelField(dataField.getName().getValue(), dataField.getDataType().toString());
    }

    private void validateModelInfo(MLModel modelInfo) throws SAXException, JAXBException, UnsupportedEncodingException {
        List<MLModelField> outputFields = doGetOutputFieldsForPMMLStream(modelInfo.getPmml());
        if (outputFields.isEmpty()) {
            throw new RuntimeException(
                    String.format("PMML File %s does not support empty output", modelInfo.getUploadedFileName()));
        }
        StorageUtils.ensureUnique(modelInfo, this::listModelInfo, QueryParam.params(
                MLModel.NAME, modelInfo.getName()));
    }
}