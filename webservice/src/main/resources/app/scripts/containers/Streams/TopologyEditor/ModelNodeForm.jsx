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

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {OverlayTrigger, Popover} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import {toastOpt} from '../../../utils/Constants';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import ModelRegistryREST from '../../../rest/ModelRegistryREST';
import CommonNotification from '../../../utils/CommonNotification';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils  from '../../../utils/ProcessorUtils';

export default class ModelNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    configData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      streamObj: {},
      modelsNameArr: [],
      modelName: '',
      parallelism: 1,
      modelId: '',
      modelOutputField:[],
      outputKeysObjArr : [],
      outputFieldsList : [],
      outputKeys: [],
      outputStreamFields: [],
      emptyModelField:false
    };
    this.streamObj={};
    this.fetchDataAgain = false;
  }

  /*
    componentWillUpdate has been call very frequently in react ecosystem
    this.context.ParentForm.state has been SET through the API call in ProcessorNodeForm
    And we need to call getDataFromParentFormContext after the Parent has set its state so that inputStreamOptions are available
    to used.
    And this condition save us from calling three API
    1] get edge
    2] get streams
    3] get Node data with config.
  */
  componentWillUpdate() {
    if(this.context.ParentForm.state.inputStreamOptions.length > 0 && !(this.fetchDataAgain)){
      this.getDataFromParentFormContext();
    }
  }

  getDataFromParentFormContext() {
    this.fetchDataAgain = true;

    let {topologyId, versionId, nodeType, nodeData} = this.props;
    let promiseArr = [
      ModelRegistryREST.getAllModelRegistry()
    ];
    Promise.all(promiseArr).then((results) => {
      let stateObj = {};
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        }
      });

      // get the inputStream from parentForm Context
      const inputStreamFromContext = this.context.ParentForm.state.inputStreamOptions;
      let fields = [],unModifyList=[];
      inputStreamFromContext.map((stream, i) => {
        // modify fields if inputStreamFromContext >  1
        const obj = {
          name : inputStreamFromContext[i].streamId,
          fields : stream.fields,
          optional : false,
          type : "NESTED"
        };
        fields.push(obj);
        // UnModify fieldsList
        unModifyList.push(...stream.fields);
      });

      this.fieldsArr = fields;
      const tempFieldsArr = ProcessorUtils.getSchemaFields(fields, 0,false);
      // create a uniqueID for select2 to support duplicate label in options
      _.map(tempFieldsArr, (f) => {
        f.uniqueID = f.keyPath.split('.').join('-')+"_"+f.name;
      });
      const unModifyFieldList = ProcessorUtils.getSchemaFields(unModifyList, 0,false);

      stateObj.fieldList = unModifyFieldList;
      stateObj.outputFieldsList  = tempFieldsArr;

      // get the ProcessorNode from parentForm Context
      this.pmmlProcessorNode = this.context.ParentForm.state.processorNode;

      const models = results[0].entities;
      let nameArr = [];
      models.map(x => {
        if(this.pmmlProcessorNode.outputStreams.length && this.pmmlProcessorNode.config.properties.modelName === x.name){
          stateObj.modelId = x.id;
        }
        nameArr.push({label: x.name, value: x.id});
      });
      stateObj.modelsNameArr = nameArr;

      if (this.pmmlProcessorNode.outputStreams.length === 0) {
        this.streamObj = {
          streamId: this.props.configData.subType.toLowerCase() + '_stream_' + this.pmmlProcessorNode.id,
          fields: []
        };
        stateObj.streamObj = this.streamObj;
        this.context.ParentForm.setState({outputStreamObj: this.streamObj});
      } else {
        this.fetchSingleModel(stateObj.modelId).then((outputFields) => {
          this.filterSelectedStream(outputFields,this.pmmlProcessorNode.outputStreams[0]);
        });
      }
      stateObj.modelName = this.pmmlProcessorNode.config.properties.modelName;
      let o = stateObj.modelsNameArr.find((model) => {
        return model.label == stateObj.modelName;
      });
      if (o){
        stateObj.modelId = o.value;
      }
      stateObj.inputStreamArr = inputStreamFromContext[0];
      stateObj.description = this.pmmlProcessorNode.description;
      stateObj.parallelism = this.pmmlProcessorNode.config.properties.parallelism || 1;
      this.setState(stateObj);
    });
  }

  filterSelectedStream = (outputFields,outputStreamArr) => {
    const {outputFieldsList} = this.state;
    let output=[];
    const nestedFields = function(arr){
      _.map(arr, (field) => {
        if(field.fields){
          nestedFields(field.fields);
        } else {
          const index = _.findIndex(outputFields, (stream) => {
            return stream.name !== field.name;
          });
          if(index !== -1){
            const obj = _.find(outputFieldsList,(o) => { return o.name === field.name;});
            output.push(obj);
          }
        }
      });
    };
    nestedFields(outputStreamArr.fields);
    const tempOutPut = _.compact(output);
    const keyData = ProcessorUtils.createSelectedKeysHierarchy(tempOutPut,outputFieldsList);
    const mergeField = _.concat(outputFields,this.getFieldFromOutPutList(keyData));
    this.streamObj = {
      streamId: outputStreamArr.streamId,
      fields: mergeField
    };

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(tempOutPut);
    this.setState({outputKeysObjArr : tempOutPut, outputKeys: keys, outputStreamFields: keyData,streamObj : this.streamObj,modelOutputField:outputFields});
    this.context.ParentForm.setState({outputStreamObj: this.streamObj});
  }

  fetchSingleModel = (modelId) => {
    return ModelRegistryREST.getModelRegistryOutputFields(modelId).then((outputFields) => {
      if(outputFields.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={outputFields.responseMessage}/>, '', toastOpt);
      } else {
        return outputFields;
      }
    });
  }

  handleModelNameChange(obj) {
    this.fetchSingleModel(obj.value).then((outputFields) => {
      const {outputFieldsList,outputKeysObjArr} = this.state;
      const keyData = ProcessorUtils.createSelectedKeysHierarchy(outputKeysObjArr,outputFieldsList);
      let tempField=[],stateObj={};
      outputFields.length === 0 ?  stateObj.emptyModelField = true : stateObj.emptyModelField = false;
      this.streamObj.fields !== undefined && this.streamObj.fields.length
      ? tempField = _.concat(outputFields,this.getFieldFromOutPutList(keyData))
      : tempField = outputFields;
      this.streamObj.fields =  tempField;
      stateObj.modelName = obj.label;
      stateObj.modelId = obj.value;
      stateObj.streamObj = this.streamObj;
      stateObj.modelOutputField = outputFields;
      this.context.ParentForm.setState({outputStreamObj: this.streamObj});
      this.setState(stateObj);
    });
  }
  handleValueChange(e) {
    let value = e.target.type === "number"
      ? Math.abs(e.target.value)
      : e.target.value;
    this.setState({parallelism: value});
  }

  validateData() {
    if (this.streamObj.fields.length === 0) {
      FSReactToastr.error(
        <CommonNotification flag="error" content={"Output stream fields cannot be blank."}/>, '', toastOpt);
      return false;
    }
    return this.state.modelId != '' && this.state.outputKeys.length
      ? true
      : false;
  }

  handleSave(name, description) {
    let {topologyId, versionId, nodeType} = this.props;
    let {modelName, parallelism, streamObj} = this.state;
    let nodeId = this.pmmlProcessorNode.id;
    this.pmmlProcessorNode.config.properties.modelName = modelName;
    this.pmmlProcessorNode.config.properties.parallelism = parallelism;
    this.pmmlProcessorNode.description = description;
    this.pmmlProcessorNode.name = name;
    // outputStreams data is formated for the server
    const streamFields  = ProcessorUtils.generateOutputStreamsArr(this.streamObj.fields,0);
    if (this.pmmlProcessorNode.outputStreams.length > 0) {
      this.pmmlProcessorNode.outputStreams[0].fields = streamFields;
    } else {
      this.pmmlProcessorNode.outputStreams.push({fields: streamFields, streamId: streamObj.streamId, topologyId: topologyId});
    }
    return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.pmmlProcessorNode)
    });
  }

  /*
    renderFieldOption Method accept the node from the select2
    And modify the Select2 view list with nested look
  */
  renderFieldOption(node) {
    let styleObj = {
      paddingLeft: (10 * node.level) + "px"
    };
    if (node.disabled) {
      styleObj.fontWeight = "bold";
    }
    return (
      <span style={styleObj}>{node.name}</span>
    );
  }

  handleFieldsChange = (arr) => {
    if(arr.length > 0){
      // splice the last index of arr as it's a new object selected
      const last_IdData = arr.splice(arr.length - 1 ,1);
      // check the object left in the arr are the same which we splice from it
      // if yes then replace the object with which we have splice earlier
      const checkIndex = _.findIndex(arr , {name : last_IdData[0].name});
      (checkIndex !== -1) ? arr[checkIndex] = last_IdData[0] : arr = _.concat(arr , last_IdData[0]);
      this.setOutputFields(arr);
    } else {
      this.state.modelOutputField.length
      ? this.streamObj.fields = this.state.modelOutputField
      : this.streamObj.fields = [];
      this.setState({outputKeysObjArr : arr, outputKeys: [], outputStreamFields: [],outputGroupByDotKeys : []});
      this.context.ParentForm.setState({outputStreamObj: this.streamObj});
    }
  }

  handleSelectAllOutputFields = () => {
    let tempFields = _.cloneDeep(this.state.outputFieldsList);
    const allOutPutFields = ProcessorUtils.selectAllOutputFields(tempFields);
    this.setOutputFields(allOutPutFields);
  }

  setOutputFields = (arr) => {
    let {outputFieldsList,modelOutputField} = this.state;
    const keyData = ProcessorUtils.createSelectedKeysHierarchy(arr,outputFieldsList);
    let mergeField=[];
    modelOutputField.length
    ? mergeField = _.concat(modelOutputField,this.getFieldFromOutPutList(keyData))
    : mergeField = this.getFieldFromOutPutList(keyData);
    this.streamObj.fields = mergeField;

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
    this.setState({outputKeysObjArr : arr, outputKeys: keys, outputStreamFields: keyData,streamObj : this.streamObj});
    this.context.ParentForm.setState({outputStreamObj: this.streamObj});
  }

  getFieldFromOutPutList = (keyData) => {
    let tempFieldsArr=[];
    _.map(keyData,(o) => {
      tempFieldsArr = _.concat(tempFieldsArr, o.fields ? o.fields : o);
    });
    return tempFieldsArr;
  }

  render() {
    const {parallelism, modelsNameArr, modelId,outputKeysObjArr,outputFieldsList,emptyModelField} = this.state;
    const disabledFields = this.props.testRunActivated ? true : !this.props.editMode;
    return (
      <div className="modal-form processor-modal-form">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <form className="customFormClass" style={{marginTop:10}}>
            <div className="form-group row">
              <div className="col-sm-12">
               <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of the model</Popover>}>
                <label>Model Name
                  <span className="text-danger">*</span>
                </label>
               </OverlayTrigger>
                <Select ref={(ref) => {
                  this.modelNameRef = ref;
                }} value={modelId} options={modelsNameArr} onChange={this.handleModelNameChange.bind(this)} required={true} disabled={disabledFields} clearable={false}/>
              {
                emptyModelField
                ? <span className="text-info">Empty output fields for selected model.</span>
                : ''
              }
              </div>
            </div>
            <div className="form-group">
              <div className="col-sm-6">
                <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Output keys</Popover>}>
                  <label>Output Fields
                    <span className="text-danger">*</span>
                  </label>
                </OverlayTrigger>
              </div>
              <div className="col-sm-6">
                <label className="pull-right">
                  <a href="javascript:void(0)" onClick={this.handleSelectAllOutputFields}>Select All</a>
                </label>
              </div>
              <div className="row">
                <div className="col-sm-12">
                  <Select  value={outputKeysObjArr} options={outputFieldsList} onChange={this.handleFieldsChange.bind(this)} multi={true} required={true} disabled={disabledFields} valueKey="uniqueID" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                </div>
              </div>
            </div>
              {/*<div className="col-sm-12">
                <label>Parallelism</label>
                <input
                    name="parallelism"
                    value={parallelism}
                    onChange={this.handleValueChange.bind(this)}
                    type="number"
                    className="form-control"
                    required={true}
                    disabled={!editMode}
                    min="1"
                    inputMode="numeric"
                />
              </div>*/}
          </form>
        </Scrollbars>
      </div>
    );
  }
}

ModelNodeForm.contextTypes = {
  ParentForm: PropTypes.object
};
