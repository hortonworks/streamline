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
import {Tabs, Tab, Radio, OverlayTrigger, Popover} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import CustomProcessorREST from '../../../rest/CustomProcessorREST';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils from '../../../utils/ProcessorUtils';

export default class CustomNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    configData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    sourceNode: PropTypes.object.isRequired,
    targetNodes: PropTypes.array.isRequired,
    linkShuffleOptions: PropTypes.array.isRequired
  };

  constructor(props) {
    super(props);
    let {configData, editMode} = props;
    this.customConfig = configData.topologyComponentUISpecification.fields;
    let id = _.find(this.customConfig, {fieldName: "name"}).defaultValue;
    this.parallelism = _.find(this.customConfig, {fieldName: "parallelism"}).defaultValue;
    this.fetchData(id);
    this.fetchDataAgain = false;
    this.mappingObj = {};
    var obj = {
      editMode: editMode,
      showSchema: true,
      userInputs: [],
      showError: false,
      showErrorLabel: false,
      outputKeys: [],
      fieldList: [],
      showLoading:true
    };

    this.customConfig.map((o) => {
      if (o.type === "boolean"){
        obj[o.fieldName] = o.defaultValue;
      }else{
        obj[o.fieldName] = o.defaultValue
          ? o.defaultValue
          : '';
      }
      if (o.isUserInput) {
        obj.userInputs.push(o);
      }
    });
    this.state = obj;
    this.streamObj={};
  }

  componentWillUpdate() {
    if(this.context.ParentForm.state.inputStreamOptions.length > 0 && !(this.fetchDataAgain)){
      this.getDataFromParentFormContext(this.parallelism);
    }
  }

  fetchData(id){
    CustomProcessorREST.getProcessor(id).then((custom) => {
      if(custom.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={custom.responseMessage}/>, '', toastOpt);
      } else {
        let {
          name,
          description,
          customProcessorImpl,
          imageFileName,
          jarFileName,
          inputSchema,
          outputSchema,
          configFields
        } = custom.entities[0];

        let stateObj = {
          name: name,
          description: description,
          customProcessorImpl: customProcessorImpl,
          imageFileName: imageFileName,
          jarFileName: jarFileName,
          inputSchema: inputSchema,
          outputSchema: outputSchema
        };

        if(this.context.ParentForm.state.inputStreamOptions.length){
          this.getDataFromParentFormContext(this.parallelism);
        }
        this.setState(stateObj);
      }
    });
  }

  getDataFromParentFormContext(defaultParallelism){
    this.fetchDataAgain = true;
    let stateObj={};
    this.nodeData = this.context.ParentForm.state.processorNode;
    let properties = this.nodeData.config.properties;
    if (!properties.parallelism) {
      properties.parallelism = defaultParallelism;
    }
    stateObj.parallelism = properties.parallelism;
    stateObj.localJarPath = properties.localJarPath;

    this.state.userInputs.map((i) => {
      if (i.type === "boolean") {
        stateObj[i.fieldName] = (properties[i.fieldName]) === true
          ? true
          : false;
      } else {
        stateObj[i.fieldName] = properties[i.fieldName]
          ? properties[i.fieldName]
          : (i.defaultValue || '');
      }
    });

    if (this.nodeData.outputStreams.length === 0) {
      this.saveStreams();
    } else {
      if(this.nodeData.outputStreams[0].fields.length === 1 && this.nodeData.outputStreams[0].fields[0].name == 'dummyStrField'){
        this.nodeData.outputStreams[0].fields.pop();
      }
      this.streamObj = this.nodeData.outputStreams[0];
      this.context.ParentForm.setState({outputStreamObj: this.streamObj});
    }
    let keysList = this.inputStream = this.context.ParentForm.state.inputStreamOptions[0];
    if(properties.inputSchemaMap && properties.inputSchemaMap[this.inputStream.streamId]){
      this.mappingObj = properties.inputSchemaMap[this.inputStream.streamId];
    }

    keysList = JSON.parse(JSON.stringify(keysList));

    Array.prototype.push.apply(keysList.fields,this.state.outputSchema.fields);
    stateObj.outputKeys = this.streamObj.fields;
    stateObj.fieldList = _.unionBy(keysList.fields,'name');
    stateObj.showLoading = false;
    this.setState(stateObj);
  }
  saveStreams = () => {
    let self = this;
    let {topologyId, nodeType, versionId,nodeData} = this.props;
    let streamData = {},
      streams = [],
      promiseArr = [];
    // nodeID is added to make streamId unique
    streams.push({streamId: 'custom_processor_stream_'+nodeData.nodeId, fields: [{name:'dummyStrField', type: 'STRING'}]});

    streams.map((s) => {
      promiseArr.push(TopologyREST.createNode(topologyId, versionId, 'streams', {body: JSON.stringify(s)}));
    });

    Promise.all(promiseArr).then(results => {
      self.nodeData.outputStreamIds = [];
      results.map(result => {
        self.nodeData.outputStreamIds.push(result.id);
      });

      TopologyREST.updateNode(topologyId, versionId, nodeType, self.nodeData.id, {
        body: JSON.stringify(this.nodeData)
      }).then((node) => {
        self.nodeData = node;
        self.setState({showSchema: true});
        this.context.ParentForm.setState({outputStreamObj: []});
      });
    });
  }

  handleValueChange(fieldObj, e) {
    let obj = {
      showError: true,
      showErrorLabel: false
    };
    obj[e.target.name] = e.target.type === "number" && e.target.value !== ''
      ? Math.abs(e.target.value)
      : e.target.value;
    if (!fieldObj.isOptional) {
      if (e.target.value === ''){
        fieldObj.isInvalid = true;
      }else{
        delete fieldObj.isInvalid;
      }
    }
    this.setState(obj);
  }

  handleRadioBtn(e) {
    let obj = {};
    obj[e.target.dataset.name] = e.target.dataset.label === "true"
      ? true
      : false;
    this.setState(obj);
  }

  getData() {
    let obj = {},
      customConfig = this.customConfig;

    customConfig.map((o) => {
      obj[o.fieldName] = this.state[o.fieldName];
    });
    return obj;
  }

  validateData() {
    let validDataFlag = true;

    this.state.userInputs.map((o) => {
      if (!o.isOptional && this.state[o.fieldName] === '') {
        validDataFlag = false;
        o.isInvalid = true;
      }
    });
    if (!validDataFlag) {
      this.setState({showError: true, showErrorLabel: true});
    } else {
      this.setState({showErrorLabel: false});
    }
    return validDataFlag;
  }

  handleSave(name, description) {
    let {topologyId, nodeType, versionId} = this.props;
    let data = this.getData();
    let nodeId = this.nodeData.id;
    this.nodeData.config.properties = data;
    this.nodeData.name = name;
    this.nodeData.description = description;

    // outputStreams data is formated for the server
    const streamFields  = ProcessorUtils.generateOutputStreamsArr(this.streamObj.fields,0);

    if(this.nodeData.outputStreams.length > 0){
      this.nodeData.outputStreams[0].fields = streamFields;
    } else {
      this.nodeData.outputStreams.push({fields: streamFields, streamId: this.streamObj.streamId});
    }

    this.nodeData.config.properties.outputStreamToSchema = {
      [this.nodeData.outputStreams[0].streamId]: {fields: streamFields}
    };

    this.nodeData.config.properties.inputSchemaMap = {
      [this.inputStream.streamId]: this.mappingObj
    };

    return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.nodeData)
    });
  }

  handleOutputKeysChange(arr){
    if(arr.length){
      this.setState({outputKeys : arr});
      this.streamObj.fields = arr;
      this.context.ParentForm.setState({outputStreamObj: this.streamObj});
    } else {
      this.setState({outputKeys : []});
      this.streamObj.fields = [];
      this.context.ParentForm.setState({outputStreamObj: this.streamObj});
    }
  }

  getMappingOptions(fieldObj) {
    const options = this.inputStream.fields.filter(f=>{
      return f.type === fieldObj.type;
    }) || [];
    const v = options.find(f=>{
      return f.name === fieldObj.name;
    });
    const value = this.mappingObj[fieldObj.name] || (v && v.name || '');
    this.mappingObj[fieldObj.name] = value;
    return {options, value};
  }

  handleMappingChange(obj, value){
    this.mappingObj[obj.name] = value.name;
    this.forceUpdate();
  }

  render() {
    let {
      topologyId,
      editMode,
      nodeType,
      nodeData,
      targetNodes,
      linkShuffleOptions
    } = this.props;
    let {showSchema, showError, showErrorLabel, outputKeys, fieldList,showLoading} = this.state;
    const disabledFields = this.props.testRunActivated ? true : !editMode;
    return (
      <div className="modal-form processor-modal-form">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <form className="customFormClass">
            {
              showLoading
              ? <div className="loading-img text-center">
                  <img src="styles/img/start-loader.gif" alt="loading" style={{
                    marginTop: "140px"
                  }}/>
                </div>
              : <div>
                  {this.state.userInputs.map((f, i) => {
                    return (
                      <div className="form-group" key={i}>
                        {f.fieldName !== "parallelism"
                          ? <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">{f.tooltip}</Popover>}>
                              <label>{f.uiName} {f.isOptional
                                ? null
                                : <span className="text-danger">*</span>}
                              </label>
                            </OverlayTrigger>
                          : ''
                        }
                        <div>
                          {f.type === "boolean"
                            ? [< Radio key = "1" inline = {
                                true
                              }
                              data-label = "true" data-name = {
                                f.fieldName
                              }
                              onChange = {
                                this.handleRadioBtn.bind(this)
                              }
                              checked = {
                                this.state[f.fieldName]
                                  ? true
                                  : false
                              }
                              disabled = {
                                disabledFields
                              }> true </Radio>,
                              <Radio
                                  key="2"
                                  inline={true}
                                  data-label="false"
                                  data-name={f.name}
                                  onChange={this.handleRadioBtn.bind(this)}
                                  checked={this.state[f.fieldName] ? false : true}
                                  disabled={disabledFields}>false
                              </Radio>
                            ]
                            : f.fieldName !== "parallelism"
                              ? <input name={f.fieldName} value={this.state[f.fieldName]} onChange={this.handleValueChange.bind(this, f)} type={f.type} className={!f.isOptional && showError && f.isInvalid
                                  ? "form-control invalidInput"
                                  : "form-control"} required={f.isOptional
                                  ? false
                                  : true} disabled={disabledFields} min={(f.type === "number" && f.fieldName === "parallelism")
                                  ? 1
                                  : f.type === "number"
                                    ? 0
                                    : null} inputMode={f.type === "number"
                                  ? "numeric"
                                  : null}/>
                              : ''
                            }
                        </div>
                      </div>
                    );
                  })
                }
                <div className="form-group">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Map each field in Custom Processor input schema to a field in incoming event schema</Popover>}>
                    <label>Input Schema Mapping</label>
                  </OverlayTrigger>
                  <div className="row">
                    {this.state.inputSchema.fields && this.state.inputSchema.fields.map((f, i)=>{
                      const {options, value} = this.getMappingOptions(f);
                      return <div key={i} className="m-b-xs col-md-12">
                          <div className="col-md-4" style={{lineHeight: '2.5'}}>{f.name}</div>
                          <div className="col-md-8"><Select value={value} options={options} onChange={this.handleMappingChange.bind(this, f)} required={true} disabled={disabledFields} valueKey="name" labelKey="name" /></div>
                        </div>;
                    })}
                  </div>
                </div>
                <div className="form-group">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Choose output fields. It can be a field from either incoming event schema or Custom Processor output schema</Popover>}>
                  <label>Output Fields
                    <span className="text-danger">*</span>
                  </label>
                  </OverlayTrigger>
                    <Select className="menu-outer-top" value={outputKeys} options={fieldList} onChange={this.handleOutputKeysChange.bind(this)} multi={true} required={true} disabled={disabledFields} valueKey="name" labelKey="name"/>
                </div>
                </div>
            }
          </form>
        </Scrollbars>
      </div>
    );
  }
}
CustomNodeForm.contextTypes = {
  ParentForm: PropTypes.object
};
