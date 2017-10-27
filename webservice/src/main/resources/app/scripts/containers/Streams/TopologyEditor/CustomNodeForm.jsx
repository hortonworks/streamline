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
import {Table, Thead, Th, Tr, Td, unsafe} from 'reactable';
import {Tabs, Tab, Radio, OverlayTrigger, Popover} from 'react-bootstrap';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import CustomProcessorREST from '../../../rest/CustomProcessorREST';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils from '../../../utils/ProcessorUtils';
import Modal from '../../../components/FSModal';
import AddOutputFieldsForm from './AddOutputFieldsForm';

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
    this.idCount = 1;
    var obj = {
      editMode: editMode,
      showSchema: true,
      userInputs: [],
      showError: false,
      showErrorLabel: false,
      outputKeys: [],
      fieldList: [],
      showLoading:true,
      newOpFields: [],
      fieldId: null,
      modalTitle: 'Add New Output Field'
    };
    this.modalContent = () => {};

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
          outputSchema: outputSchema,
          hasOutputSchema: !!outputSchema
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
    stateObj.outputKeys = this.streamObj.fields ? JSON.parse(JSON.stringify(this.streamObj.fields)) : [];

    if(this.state.outputSchema){
      Array.prototype.push.apply(keysList.fields, this.state.outputSchema.fields);
    } else {
      stateObj.newOpFields = _.remove(stateObj.outputKeys, function(obj){ return !_.find(keysList.fields, obj); });
      stateObj.newOpFields.map((field)=>{
        field.id = this.idCount++;
      });
    }

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

    if(this.state.inputSchema){
      this.nodeData.config.properties.inputSchemaMap = {
        [this.inputStream.streamId]: this.mappingObj
      };
    }

    return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {
      body: JSON.stringify(this.nodeData)
    });
  }

  handleOutputKeysChange(arr){
    let fields = [];
    if(arr.length){
      fields = arr;
    }
    this.setState({outputKeys : fields});

    if(this.state.newOpFields.length){
      let tempFieldsArr = JSON.parse(JSON.stringify(fields));
      Array.prototype.push.apply(tempFieldsArr, this.state.newOpFields);
      this.streamObj.fields = tempFieldsArr;
    } else {
      this.streamObj.fields = fields;
    }
    this.context.ParentForm.setState({outputStreamObj: this.streamObj});
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
    this.mappingObj[obj.name] = value ? value.name : '';
    this.forceUpdate();
  }

  handleAddNewField(){
    this.modalContent = () => {
      return <AddOutputFieldsForm ref="addField" id={this.idCount++}/>;
    };
    this.setState({
      fieldId: null,
      modalTitle: 'Add Output Field'
    }, () => {
      this.refs.OpFieldModal.show();
    });
  }

  handleNewFieldsEdit(id){
    let obj = this.state.newOpFields.find((o) => o.id === id);

    this.modalContent = () => {
      return <AddOutputFieldsForm ref="addField" id={id} fieldData={JSON.parse(JSON.stringify(obj))}/>;
    };
    this.setState({
      fieldId: id,
      modalTitle: 'Edit Output Field'
    }, () => {
      this.refs.OpFieldModal.show();
    });
  }

  handleNewFieldsDelete(id) {
    let fields = _.reject(this.state.newOpFields, (o) => o.id === id);
    this.setState({
      newOpFields: fields
    });
    this.updateOutputStream(fields);
  }

  handleSaveOpFieldModal(){
    if (this.refs.addField.validate()) {
      let data = this.refs.addField.getConfigField();
      let arr = [];
      if (this.state.fieldId) {
        let index = this.state.newOpFields.findIndex((o) => o.id === this.state.fieldId);
        arr = this.state.newOpFields;
        arr[index] = data;
      } else {
        arr = [
          ...this.state.newOpFields,
          data
        ];
      }
      this.setState({newOpFields: arr});
      this.updateOutputStream(arr);

      this.refs.OpFieldModal.hide();
    }
  }

  updateOutputStream(arr){
    let combinedFields = [];
    Array.prototype.push.apply(combinedFields, this.state.outputKeys);
    Array.prototype.push.apply(combinedFields, arr);
    this.streamObj.fields = combinedFields;
    this.context.ParentForm.setState({outputStreamObj: this.streamObj});
  }

  handleSelectAllOutputFields = () => {
    this.handleOutputKeysChange(this.state.fieldList);
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
                {this.state.inputSchema ?
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
                : ''}
                <div className="form-group">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Choose output fields. It can be a field from either incoming event schema or Custom Processor output schema</Popover>}>
                    <label>Output Fields
                      {this.state.hasOutputSchema ? <span className="text-danger">*</span> : null}
                    </label>
                  </OverlayTrigger>
                  <label className="pull-right">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Select All Keys</Popover>}>
                      <a href="javascript:void(0)" onClick={this.handleSelectAllOutputFields}>Select All</a>
                    </OverlayTrigger>
                  </label>
                  <div>
                    <Select className="" value={outputKeys} options={fieldList} onChange={this.handleOutputKeysChange.bind(this)} multi={true} required={true} disabled={disabledFields} valueKey="name" labelKey="name"/>
                  </div>
                </div>
                {
                !this.state.hasOutputSchema ?
                  (
                    <div>
                      <div className="form-group row">
                        <div className="col-sm-4" style={{"marginRight": "-40px"}}>
                          <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">New fields to be added on the fly to the output schema.</Popover>}>
                            <label>New output fields</label>
                          </OverlayTrigger>
                        </div>
                        <div className="col-sm-2">
                          <button className="btn btn-success btn-xs" type="button" onClick={this.handleAddNewField.bind(this)}>
                            <i className="fa fa-plus"></i>
                          </button>
                        </div>
                      </div>
                      <div className="row">
                        <div className="col-sm-12">
                          <Table className="table table-hover table-bordered table-CP-configFields"
                            noDataText="No fields added."
                          >
                            <Thead>
                              <Th column="field">Field Name</Th>
                              <Th column="type">Type</Th>
                              <Th column="isOptional">Optional</Th>
                              <Th column="action">Actions</Th>
                            </Thead>
                            {this.state.newOpFields.map((obj, i) => {
                              return (
                                <Tr key={i}>
                                  <Td column="field">{obj.name}</Td>
                                  <Td column="type">{obj.type}</Td>
                                  <Td column="isOptional">{obj.optional}</Td>
                                  <Td column="action">
                                    <div className="btn-action">
                                      <BtnEdit callback={this.handleNewFieldsEdit.bind(this, obj.id)}/>
                                    <BtnDelete callback={this.handleNewFieldsDelete.bind(this, obj.id)}/>
                                    </div>
                                  </Td>
                                </Tr>
                              );
                            })}
                          </Table>
                        </div>
                      </div>
                    </div>
                  ) : ''
                }
                </div>
            }
          </form>
          <Modal
            ref="OpFieldModal"
            onKeyPress={this.handleKeyPress}
            data-title={this.state.modalTitle}
            data-resolve={this.handleSaveOpFieldModal.bind(this)}
            data-reject={()=>{
              this.refs.addField.refs.OutputFieldForm.clearErrors();
              this.refs.OpFieldModal.hide();
            }}
          >
            {this.modalContent()}
          </Modal>
        </Scrollbars>
      </div>
    );
  }
}
CustomNodeForm.contextTypes = {
  ParentForm: PropTypes.object
};
