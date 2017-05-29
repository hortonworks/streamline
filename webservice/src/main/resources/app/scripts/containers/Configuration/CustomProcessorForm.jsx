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
import ReactDOM from 'react-dom';
import {withRouter} from 'react-router';
import _ from 'lodash';
import {
  Table,
  Thead,
  Th,
  Tr,
  Td,
  unsafe
} from 'reactable';
import {FormGroup,InputGroup,FormControl,Button} from 'react-bootstrap';
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import ConfigFieldsForm from './ConfigFieldsForm';
import CustomProcessorREST from '../../rest/CustomProcessorREST';
import OutputSchemaContainer from '../OutputSchemaContainer';
import {pageSize, toastOpt} from '../../utils/Constants';
import FSReactToastr from '../../components/FSReactToastr';
import ReactCodemirror from 'react-codemirror';
import '../../utils/Overrides';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';
import Modal from '../../components/FSModal';
import BaseContainer from '../BaseContainer';
import CommonNotification from '../../utils/CommonNotification';
import Utils from '../../utils/Utils';

CodeMirror.registerHelper("lint", "json", function(text) {
  var found = [];
  var {parser} = jsonlint;
  parser.parseError = function(str, hash) {
    var loc = hash.loc;
    found.push({
      from: CodeMirror.Pos(loc.first_line - 1, loc.first_column),
      to: CodeMirror.Pos(loc.last_line - 1, loc.last_column),
      message: str
    });
  };
  try {
    jsonlint.parse(text);
  } catch (e) {}
  return found;
});

class CustomProcessorForm extends Component {

  defaultObj = {
    streamingEngine: 'STORM',
    name: '',
    description: '',
    customProcessorImpl: '',
    jarFileName: '',
    fileName: '',
    inputSchema: '',
    outputStreamToSchema: [],
    topologyComponentUISpecification: [],
    fieldId: null,
    modalTitle: 'Add Config  Field',
    showNameError: false,
    showCodeMirror : false,
    expandCodemirror : false,
    fieldsError : {
      whiteSpaceError:false,
      pName : false,
      desc : false,
      classNameType : false,
      fileError : false
    }
  };

  constructor(props) {
    super(props);
    this.extendObj = Object.assign({}, this.defaultObj, {fieldsChk: true});
    this.state = JSON.parse(JSON.stringify(this.extendObj));
    this.idCount = 1;
    if (props.id) {
      this.fetchProcessor(props.id);
    }
    this.modalContent = () => {};
    this.nextRoutes = '';
    this.navigateFlag = false;
  }

  fetchProcessor(id) {
    CustomProcessorREST.getProcessor(id).then((processor) => {
      if (processor.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={processor.responseMessage}/>, '', toastOpt);
      } else {
        let {
          streamingEngine,
          name,
          description,
          customProcessorImpl,
          jarFileName,
          inputSchema,
          outputStreamToSchema,
          topologyComponentUISpecification
        } = processor.entities[0];
        inputSchema = JSON.stringify(inputSchema.fields, null, "  ");
        let arr = [],
          streamIds = _.keys(outputStreamToSchema);
        streamIds.map((key) => {
          arr.push({
            streamId: key,
            fields: JSON.stringify(outputStreamToSchema[key].fields, null, "  ")
          });
        });
        outputStreamToSchema = arr;
        topologyComponentUISpecification.fields.map((o) => {
          o.id = this.idCount++;
        });
        let obj = {
          streamingEngine,
          name,
          description,
          customProcessorImpl,
          jarFileName,
          inputSchema,
          outputStreamToSchema,
          showCodeMirror : true,
          expandCodemirror : false
        };
        obj.topologyComponentUISpecification = topologyComponentUISpecification.fields;
        CustomProcessorREST.getCustomProcessorFile(jarFileName)
          .then((response)=>{
            let f = new File([response], jarFileName);
            obj.jarFileName = f;
            obj.fileName = f.name;
            this.setState(obj);
          });
        this.setState(obj);
      }
    });
  }

  componentDidMount() {
    this.props.router.setRouteLeaveHook(this.props.route, this.routerWillLeave);
  }
  componentWillUnmount() {
    this.props.popUpFlag(false);
    this.unmounted = true;
  }

  routerWillLeave = (nextLocation) => {
    if (!this.unmounted) {
      this.checkForData();
      this.nextRoutes = nextLocation.pathname;
      (!this.navigateFlag)
        ? this.refs.leaveConfigProcessor.show()
        : '';
      return this.navigateFlag;
    }
  }

  confirmLeave(flag) {
    this.props.popUpFlag(true);
    if (flag) {
      this.navigateFlag = true;
      this.setState({fieldsChk: false});
      this.refs.leaveConfigProcessor.hide();
      this.props.router.push(this.nextRoutes);
    } else {
      this.setState({fieldsChk: true});
      this.refs.leaveConfigProcessor.hide();
    }
  }

  handleValueChange(type,e) {
    let obj = {};
    obj.fieldsError = this.state.fieldsError;
    obj[e.target.name] = e.target.value;
    e.target.value.trim() === '' && type !== null
      ? obj.fieldsError[type] = true
      : obj.fieldsError[type] = false;
    this.setState(obj);
  }

  handleNameChange(type,e) {
    let obj = this.validateName(e.target.value);
    obj.fieldsError = this.state.fieldsError;
    obj[e.target.name] = e.target.value;
    e.target.value.trim() === ''
    ? obj.fieldsError[type] = true
    : obj.fieldsError[type] = false;
    this.setState(obj);
  }
  validateName(name) {
    let {processors, id} = this.props;
    let stateObj = {
      showNameError: false,
      fieldsError : this.state.fieldsError
    };
    if (name !== '') {
      let hasProcessor = processors.filter((o) => {
        return (o.name === name);
      });
      if (hasProcessor.length === 1 && name !== id) {
        stateObj.showNameError = true;
      }
      Utils.checkWhiteSpace(name)
        ? stateObj.fieldsError.whiteSpaceError = true
        : stateObj.fieldsError.whiteSpaceError = false;
    }
    return stateObj;
  }

  handleJarUpload(event) {
    const {fieldsError} = this.state;
    let fileObj = {};
    if (!event.target.files.length) {
      fileObj.name='';
      fieldsError.fileError = true;
    } else {
      fileObj = event.target.files[0];
      fieldsError.fileError = false;
    }
    this.setState({jarFileName: fileObj, fileName: fileObj.name,fieldsError:fieldsError});
  }

  handleUpload(e) {
    this.refs.jarFileName.click();
  }

  handleAddFields() {
    this.props.popUpFlag(true);
    this.modalContent = () => {
      return <ConfigFieldsForm ref="addField" id={this.idCount++}/>;
    };
    this.setState({
      fieldId: null,
      title: 'Add Config Field'
    }, () => {
      this.refs.ConfigFieldModal.show();
    });
  }

  handleConfigFieldsEdit(id) {
    let obj = this.state.topologyComponentUISpecification.find((o) => o.id === id);

    this.modalContent = () => {
      return <ConfigFieldsForm ref="addField" id={id} fieldData={JSON.parse(JSON.stringify(obj))}/>;
    };
    this.setState({
      fieldId: id,
      title: 'Edit Config Field'
    }, () => {
      this.refs.ConfigFieldModal.show();
    });
  }

  handleSaveConfigFieldModal() {
    if (this.refs.addField.validate()) {
      let data = this.refs.addField.getConfigField();
      let arr = [];
      if (this.state.fieldId) {
        let index = this.state.topologyComponentUISpecification.findIndex((o) => o.id === this.state.fieldId);
        arr = this.state.topologyComponentUISpecification;
        arr[index] = data;
      } else {
        arr = [
          ...this.state.topologyComponentUISpecification,
          data
        ];
      }
      this.setState({topologyComponentUISpecification: arr});
      this.refs.ConfigFieldModal.hide();
      this.props.popUpFlag(false);
    }
  }

  handleConfigFieldsDelete(id) {
    this.setState({
      topologyComponentUISpecification: _.reject(this.state.topologyComponentUISpecification, (o) => o.id === id)
    });
  }

  validateData() {
    let validDataFlag = true;
    let {
      streamingEngine,
      name,
      description,
      customProcessorImpl,
      jarFileName,
      topologyComponentUISpecification,
      inputSchema,
      fieldsError
    } = this.state;
    let outputStreams = this.refs.OutputSchemaContainer.getOutputStreams();
    let outputStreamFlag = false;
    outputStreams.map((o) => {
      let schema = o.fields.length > 0 ? JSON.parse(o.fields) : "";
      if (!(schema instanceof Array) || schema.length <= 0) {
        outputStreamFlag = true;
      }
    });
    if (outputStreamFlag) {
      FSReactToastr.warning(
        <strong>Output streams needs to be an array with atleast one field object.</strong>
      );
      validDataFlag = false;
    }

    if (name !== '') {
      let errorObj = this.validateName(name);
      if (errorObj.showNameError) {
        validDataFlag = false;
      }
    }

    if (streamingEngine === '' || name === '' || description === '' || customProcessorImpl === '' || jarFileName === '' || inputSchema === '' || outputStreams.length === 0 || Utils.checkWhiteSpace(name)) {
      name === '' ? fieldsError.pName = true : ''  ;
      description === '' ? fieldsError.desc = true :  '';
      customProcessorImpl === '' ? fieldsError.classNameType = true : '' ;
      jarFileName === '' ? fieldsError.fileError = true : '';
      validDataFlag = false;
    }
    return validDataFlag;
  }

  checkForData() {
    let {
      streamingEngine,
      name,
      description,
      customProcessorImpl,
      jarFileName,
      inputSchema,
      fieldsChk
    } = this.state;
    let outputStreams = this.refs.OutputSchemaContainer.getOutputStreams();
    const emptyVal = [
      name,
      description,
      customProcessorImpl,
      jarFileName,
      inputSchema
    ];
    if (streamingEngine === '' || name === '' || description === '' || customProcessorImpl === '' || jarFileName === '' || inputSchema === '' || outputStreams.length === 0 ) {
      if (fieldsChk) {
        let filterVal = emptyVal.filter(val => {
          return val.length !== 0;
        });
        (filterVal.length !== emptyVal.length)
          ? (filterVal.length === 0 && outputStreams.length === 1)
            ? this.navigateFlag = true
            : this.setState({
              fieldsChk: false
            }, function() {
              this.navigateFlag = false;
            })
          : this.navigateFlag = true;
      }
    }
  }

  handleSave() {
    if (this.validateData()) {
      let {
        streamingEngine,
        name,
        description,
        customProcessorImpl,
        jarFileName,
        topologyComponentUISpecification
      } = this.state;
      let inputSchema = {
        fields: JSON.parse(this.state.inputSchema)
      };
      let obj = {};
      let outputStreams = this.refs.OutputSchemaContainer.getOutputStreams();
      outputStreams.map((o) => {
        obj[o.streamId] = {
          fields: JSON.parse(o.fields)
        };
      });
      let outputStreamToSchema = obj;

      let configFieldsArr = topologyComponentUISpecification.map((o) => {
        let {
          fieldName,
          uiName,
          isOptional,
          type,
          defaultValue,
          isUserInput,
          tooltip
        } = o;
        return {
          fieldName,
          uiName,
          isOptional,
          type,
          defaultValue,
          isUserInput,
          tooltip
        };
      });

      let customProcessorInfo = {
        streamingEngine,
        name,
        description,
        customProcessorImpl,
        inputSchema,
        outputStreamToSchema,
        topologyComponentUISpecification: {
          fields: configFieldsArr
        },
        jarFileName: jarFileName.name
      };

      var formData = new FormData();
      formData.append('jarFile', jarFileName);
      formData.append('customProcessorInfo', JSON.stringify(customProcessorInfo));

      if (this.props.id) {
        return CustomProcessorREST.putProcessor(this.props.id, {body: formData});
      } else {
        return CustomProcessorREST.postProcessor({body: formData});
      }
    } else {
      return false;
    }
  }

  handleInputSchemaChange(json) {
    this.setState({inputSchema: json});
  }

  handleKeyPress = (event) => {
    if (event.key === "Enter") {
      this.refs.leaveConfigProcessor.state.show
        ? this.confirmLeave(this, true)
        : '';
      this.refs.ConfigFieldModal.state.show
        ? this.handleSaveConfigFieldModal()
        : '';
    }
  }

  handleFileChange = (file) => {
    if(file){
      const fileName = file.name;
      const reader = new FileReader();
      reader.onload = function(e) {
        if(Utils.validateJSON(reader.result)) {
          const tempInputSchema = JSON.stringify(JSON.parse(reader.result),null,"  ");
          this.setState({inputSchema : tempInputSchema,showCodeMirror:true});
        }
      }.bind(this);
      reader.readAsText(file);
    }
  }

  fileHandler = (type,e) => {
    e.preventDefault();
    e.stopPropagation();
    if(type === 'drop'){
      if(e.dataTransfer.files.length){
        this.handleFileChange(e.dataTransfer.files[0]);
      }
    } else {
      if(e.target.files.length){
        this.handleFileChange(e.target.files[0]);
      } else {
        cnosole.log();
      }
    }
  }

  hideCodeMirror = (e) => {
    e.preventDefault();
    this.setState({inputSchema : '',showCodeMirror:false});
  }

  outerDivClicked = (e) => {
    e.preventDefault();
    this.setState({showCodeMirror:true});
  }

  handleExpandClick = (e) => {
    e.preventDefault();
    const {expandCodemirror} = this.state;
    this.setState({expandCodemirror :!expandCodemirror});
  }

  render() {
    const {showCodeMirror,expandCodemirror,fieldsError} = this.state;
    const {whiteSpaceError,pName ,desc,classNameType,fileError} = fieldsError;
    const jsonoptions = {
      lineNumbers: true,
      mode: "application/json",
      styleActiveLine: true,
      gutters: ["CodeMirror-lint-markers"],
      lint: true,
      autofocus: true
    };
    return (
      <div>
        <div className="row">
          <div className="col-sm-12">
            <div className="box">
              <div className="box-body">
                <form className="form-horizontal">
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="streamEngineLabel">Streaming Engine
                      <span className="text-danger">*</span>
                    </label>
                    <div className="col-sm-5">
                      <input name="streamingEngine" placeholder="Streaming Engine" onChange={this.handleValueChange.bind(this,null)} type="text" className={this.state.streamingEngine.trim() == ""
                        ? "form-control invalidInput"
                        : "form-control"} value={this.state.streamingEngine} disabled={true} required={true}/>
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="nameLabel">Name
                      <span className="text-danger">*</span>
                    </label>
                    <div className="col-sm-5">
                      <input name="name" placeholder="Name" onChange={this.handleNameChange.bind(this,'pName')} type="text" className={pName
                        ? "form-control invalidInput"
                        : "form-control"} value={this.state.name} required={true} disabled={this.props.id
                        ? true
                        : false}/>
                    </div>
                    {this.state.showNameError
                      ? <p className="text-danger">Processor with this name is already present</p>
                      : ''}
                    {
                      whiteSpaceError
                      ? <p className="text-danger">Processor name with space is not allow.</p>
                      : ''
                    }
                  </div>
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="descriptionLabel">Description
                      <span className="text-danger">*</span>
                    </label>
                    <div className="col-sm-5">
                      <input name="description" placeholder="Description" onChange={this.handleValueChange.bind(this,'desc')} type="text" className={desc
                        ? "form-control invalidInput"
                        : "form-control"} value={this.state.description} required={true}/>
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="classLabel">Classname
                      <span className="text-danger">*</span>
                    </label>
                    <div className="col-sm-5">
                      <input name="customProcessorImpl" placeholder="Classname" onChange={this.handleValueChange.bind(this,'classNameType')} type="text" className={classNameType
                        ? "form-control invalidInput"
                        : "form-control"} value={this.state.customProcessorImpl} required={true}/>
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="uploadJarLabel">Upload Jar
                      <span className="text-danger">*</span>
                    </label>
                    <div className="col-sm-5">
                      <input type="file" name="jarFileName" placeholder="Select Jar" accept=".jar" className="hidden-file-input" ref="jarFileName"
                        onChange={(event) => {
                          this.handleJarUpload.call(this, event);
                        }}
                        required={true}/>
                      <div>
                        <InputGroup>
                          <InputGroup.Addon className="file-upload">
                            <Button
                              type="button"
                              className="browseBtn btn-primary"
                              onClick={this.handleUpload.bind(this)}
                            >
                              <i className="fa fa-folder-open-o"></i>&nbsp;Browse
                            </Button>
                          </InputGroup.Addon>
                          <FormControl
                            type="text"
                            placeholder="No file chosen"
                            disabled={true}
                            value={this.state.fileName}
                            className={fileError ? "form-control invalidInput" : "form-control"}
                          />
                        </InputGroup>
                      </div>
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="configFieldLabel">Config Fields
                      <span className="text-danger">*</span>
                    </label>
                    <div className="col-sm-5">
                      <button type="button" className="btn btn-sm btn-primary" onClick={this.handleAddFields.bind(this)}>Add Config Fields</button>
                    </div>
                  </div>
                  <div className="row">
                    <div className="col-sm-10 col-sm-offset-2">
                      <Table className="table table-hover table-bordered table-CP-configFields" noDataText="No records found." currentPage={0} itemsPerPage={this.state.topologyComponentUISpecification.length > pageSize
                        ? pageSize
                        : 0} pageButtonLimit={5}>
                        <Thead>
                          <Th column="fieldName">Field Name</Th>
                          <Th column="uiName">UI Name</Th>
                          <Th column="isOptional">Optional</Th>
                          <Th column="type">Type</Th>
                          <Th column="defaultValue">Default Value</Th>
                          <Th column="tooltip">Tooltip</Th>
                          <Th column="action">Actions</Th>
                        </Thead>
                        {this.state.topologyComponentUISpecification.map((obj, i) => {
                          return (
                            <Tr key={i}>
                              <Td column="fieldName">{obj.fieldName}</Td>
                              <Td column="uiName">{obj.uiName}</Td>
                              <Td column="isOptional">{obj.isOptional}</Td>
                              <Td column="type">{obj.type}</Td>
                              <Td column="defaultValue">{obj.defaultValue}</Td>
                              <Td column="tooltip">{obj.tooltip}</Td>
                              <Td column="action">
                                <div className="btn-action">
                                  <BtnEdit callback={this.handleConfigFieldsEdit.bind(this, obj.id)}/>
                                  <BtnDelete callback={this.handleConfigFieldsDelete.bind(this, obj.id)}/>
                                </div>
                              </Td>
                            </Tr>
                          );
                        })}
                      </Table>
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="inputSchemaLable">Input Schema
                      <span className="text-danger">*</span>
                    </label>
                    <div className={`${expandCodemirror ? 'col-md-10' : 'col-sm-6'}`}  onDrop={this.fileHandler.bind(this,'drop')} onDragOver={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      return false;
                    }}>
                      <a className="pull-right clear-link" href="javascript:void(0)" onClick={this.hideCodeMirror.bind(this)}> CLEAR </a>
                      <span className="pull-right" style={{margin: '-1px 5px 0'}}>|</span>
                      <a className="pull-right" href="javascript:void(0)" onClick={this.handleExpandClick.bind(this)}>
                        {expandCodemirror ? <i className="fa fa-compress"></i> : <i className="fa fa-expand"></i>}
                      </a>
                      {
                        showCodeMirror
                        ? <ReactCodemirror ref="JSONCodemirror" value={this.state.inputSchema} onChange={this.handleInputSchemaChange.bind(this)} options={jsonoptions}/>
                        : <div ref="browseFileContainer" className={"addSchemaBrowseFileContainer"}>
                            <div onClick={this.outerDivClicked.bind(this)} data-stest="inputSchemaBox">
                            <div className="main-title">Copy & Paste</div>
                            <div className="sub-title m-t-sm m-b-sm">OR</div>
                            <div className="main-title">Drag & Drop</div>
                            <div className="sub-title" style={{"marginTop": "-4px"}}>Files Here</div>
                            <div className="sub-title m-t-sm m-b-sm">OR</div>
                            <div  className="m-t-md">
                              <input type="file" ref="browseFile" accept=".json" className="inputfile" onClick={(e) => {
                                e.stopPropagation();
                              }} onChange={this.fileHandler.bind(this,'browser')}/>
                              <label htmlFor="file" className="btn btn-success">BROWSE</label>
                              </div>
                            </div>
                          </div>
                      }
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="col-sm-2 control-label" data-stest="outputSchemaLable">Output Schema
                      <span className="text-danger">*</span>
                    </label>
                    <div className="col-sm-10">
                      <OutputSchemaContainer ref="OutputSchemaContainer" streamData={this.state.outputStreamToSchema}/>
                    </div>
                  </div>
                  <div className="form-group">
                    <div className="col-sm-12 text-center">
                      <button type="button" className="btn btn-default" onClick={this.props.onCancel}>Cancel</button>{'\n'}
                      <button type="button" className="btn btn-success" onClick={this.props.onSave}>Save</button>
                    </div>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
        <Modal ref="ConfigFieldModal" onKeyPress={this.handleKeyPress} data-title={this.state.modalTitle} data-resolve={this.handleSaveConfigFieldModal.bind(this)} data-reject={()=>{this.refs.addField.refs.ConfigForm.clearErrors(); this.refs.ConfigFieldModal.hide();}}>
          {this.modalContent()}
        </Modal>
        <Modal ref="leaveConfigProcessor" onKeyPress={this.handleKeyPress} data-title="Confirm Box" dialogClassName="confirm-box" data-resolve={this.confirmLeave.bind(this, true)} data-reject={this.confirmLeave.bind(this, false)}>
          {< p > Your Processor Config setting is not saved! Are you sure you want to leave
            ? </p>}
        </Modal>
      </div>
    );
  }

}

export default withRouter(CustomProcessorForm, {withRef: true});
