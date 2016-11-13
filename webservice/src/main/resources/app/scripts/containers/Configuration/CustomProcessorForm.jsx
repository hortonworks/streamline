import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import {BtnDelete, BtnEdit} from '../../components/ActionButtons';
import ConfigFieldsForm from './ConfigFieldsForm';
import CustomProcessorREST from '../../rest/CustomProcessorREST';
import OutputSchemaContainer from '../OutputSchemaContainer';
import {pageSize,toastOpt} from '../../utils/Constants';
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


CodeMirror.registerHelper("lint", "json", function(text) {
  var found = [];
  var {parser} = jsonlint;
  parser.parseError = function(str, hash) {
    var loc = hash.loc;
    found.push({from: CodeMirror.Pos(loc.first_line - 1, loc.first_column),
                to: CodeMirror.Pos(loc.last_line - 1, loc.last_column),
                message: str});
  };
  try { jsonlint.parse(text); }
  catch(e) {}
  return found;
});

export default class CustomProcessorForm extends Component {

	defaultObj = {
		streamingEngine: 'STORM',
		name: '',
		description: '',
		customProcessorImpl: '',
		jarFileName: '',
		inputSchema: '',
		outputStreamToSchema: [],
                topologyComponentUISpecification: [],
		fieldId: null,
		modalTitle: 'Add Config  Field'
	}

	constructor(props) {
		super(props);
		this.state = JSON.parse(JSON.stringify(this.defaultObj));
		this.idCount = 1;
		if(props.id)
			this.fetchProcessor(props.id);
		this.modalContent = ()=>{};
	}

	fetchProcessor(id) {
		CustomProcessorREST.getProcessor(id)
			.then((processor)=>{
				if(processor.responseCode !== 1000){
          FSReactToastr.error(
              <CommonNotification flag="error" content={processor.responseMessage}/>, '', toastOpt)
				} else {
                                        let {streamingEngine, name, description, customProcessorImpl, jarFileName, inputSchema, outputStreamToSchema, topologyComponentUISpecification} = processor.entities[0];
					inputSchema = JSON.stringify(inputSchema.fields, null, "  ");
					let arr = [],
						streamIds = _.keys(outputStreamToSchema);
					streamIds.map((key)=>{
						arr.push({
							streamId: key,
							fields: JSON.stringify(outputStreamToSchema[key].fields, null, "  ")
						})
					});
					outputStreamToSchema = arr;
                                        topologyComponentUISpecification.fields.map((o)=>{ o.id = this.idCount++; });
                                        let obj = {streamingEngine, name, description, customProcessorImpl, jarFileName, inputSchema, outputStreamToSchema};
          obj.topologyComponentUISpecification = topologyComponentUISpecification.fields;
					this.setState(obj);
				}
			})
	}


	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleJarUpload(event) {
		if(!event.target.files.length){
			this.setState(JSON.parse(JSON.stringify(this.defaultObj)));
			return;
		}
		let fileObj = event.target.files[0];
        this.setState({
			jarFileName: fileObj
        });
	}

	handleAddFields() {
		this.modalContent = ()=>{
				return <ConfigFieldsForm ref="addField" id={this.idCount++}/>
			};
		this.setState({
			fieldId: null,
			title: 'Add Config Field'
		}, ()=>{
			this.refs.ConfigFieldModal.show();
		});
	}

	handleConfigFieldsEdit(id){
                let obj = this.state.topologyComponentUISpecification.find((o) => o.id === id);

		this.modalContent = ()=>{
				return <ConfigFieldsForm ref="addField" id={id} fieldData={obj}/>
			};
		this.setState({
			fieldId: id,
			title: 'Edit Config Field'
		}, ()=>{
			this.refs.ConfigFieldModal.show();
		});
	}

	handleSaveConfigFieldModal(){
		if(this.refs.addField.validate()){
			let data = this.refs.addField.getConfigField();
			let arr = [];
			if(this.state.fieldId) {
                                let index = this.state.topologyComponentUISpecification.findIndex((o) => o.id === this.state.fieldId);
                                arr = this.state.topologyComponentUISpecification;
				arr[index] = data;
			} else {
                                arr = [...this.state.topologyComponentUISpecification, data];
			}
			this.setState({
                                topologyComponentUISpecification: arr
			})
			this.refs.ConfigFieldModal.hide();
		}
	}

	handleConfigFieldsDelete(id){
                this.setState({topologyComponentUISpecification: _.reject(this.state.topologyComponentUISpecification, (o) => o.id === id)});
	}

	validateData() {
		let validDataFlag = true;
		let {streamingEngine, name, description, customProcessorImpl, jarFileName,
                        topologyComponentUISpecification, inputSchema} = this.state;
		let outputStreams = this.refs.OutputSchemaContainer.getOutputStreams();

		if(streamingEngine === '' || name === '' || description === '' || customProcessorImpl === '' ||
                        jarFileName === '' || inputSchema === '' || outputStreams.length === 0 || topologyComponentUISpecification.length === 0)
			validDataFlag = false;
		return validDataFlag;
	}

	handleSave() {
		if(this.validateData()) {
                        let {streamingEngine, name, description, customProcessorImpl, jarFileName, topologyComponentUISpecification} = this.state;
			let inputSchema = {
				fields: JSON.parse(this.state.inputSchema)
			};
			let obj = {};
			let outputStreams = this.refs.OutputSchemaContainer.getOutputStreams();
			outputStreams.map((o)=>{
				obj[o.streamId] = {
					fields: JSON.parse(o.fields)
				};
			});
			let outputStreamToSchema = obj;

                        let configFieldsArr = topologyComponentUISpecification.map((o) => {
                                let {fieldName, uiName, isOptional, type, defaultValue, isUserInput, tooltip} = o;
                                return {fieldName, uiName, isOptional, type,	defaultValue, isUserInput, tooltip};
			});
                let customProcessorInfo = {streamingEngine, name, description, customProcessorImpl, inputSchema, outputStreamToSchema, topologyComponentUISpecification: {fields: configFieldsArr}, jarFileName: jarFileName.name};

			var formData = new FormData();
			formData.append('jarFile', jarFileName);
			formData.append('customProcessorInfo', JSON.stringify(customProcessorInfo));

			if(this.props.id){
				return CustomProcessorREST.putProcessor(this.props.id, {body: formData})
			} else {
				return CustomProcessorREST.postProcessor({body: formData});
			}

		}
	}

	handleInputSchemaChange(json){
		this.setState({inputSchema: json});
	}

	render() {
		const jsonoptions = {
			lineNumbers: true,
			mode: "application/json",
			styleActiveLine: true,
			gutters: ["CodeMirror-lint-markers"],
			lint: true
        };
		return (
			<div>
        <div className="row">
            <div className="col-sm-12">
                <div className="box">
                    <div className="box-body">
                      <form className="form-horizontal">
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Streaming Engine <span className="text-danger">*</span></label>
                          <div className="col-sm-5">
                            <input
                              name="streamingEngine"
                              placeholder="Streaming Engine"
                              onChange={this.handleValueChange.bind(this)}
                              type="text"
                              className={this.state.streamingEngine.trim() == "" ? "form-control invalidInput" : "form-control"}
                              value={this.state.streamingEngine}
                              disabled={true}
                                required={true}
                            />
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Name <span className="text-danger">*</span></label>
                          <div className="col-sm-5">
                            <input
                              name="name"
                              placeholder="Name"
                              onChange={this.handleValueChange.bind(this)}
                              type="text"
                              className={this.state.name.trim() == "" ? "form-control invalidInput" : "form-control"}
                              value={this.state.name}
                                required={true}
                                disabled = {this.props.id ? true : false}
                            />
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Description <span className="text-danger">*</span></label>
                          <div className="col-sm-5">
                            <input
                              name="description"
                              placeholder="Description"
                              onChange={this.handleValueChange.bind(this)}
                              type="text"
                              className={this.state.description.trim() == "" ? "form-control invalidInput" : "form-control"}
                              value={this.state.description}
                                required={true}
                            />
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Classname <span className="text-danger">*</span></label>
                          <div className="col-sm-5">
                            <input
                              name="customProcessorImpl"
                              placeholder="Classname"
                              onChange={this.handleValueChange.bind(this)}
                              type="text"
                              className={this.state.customProcessorImpl.trim() == "" ? "form-control invalidInput" : "form-control"}
                              value={this.state.customProcessorImpl}
                                required={true}
                            />
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Upload Jar <span className="text-danger">*</span></label>
                          <div className="col-sm-5">
                            <input
                              type="file"
                              name="jarFileName"
                              placeholder="Select Jar"
                              accept=".jar"
                              className={this.state.jarFileName == "" ? "form-control invalidInput" : "form-control"}
                              ref="jarFileName"
                              onChange={(event)=>{this.handleJarUpload.call(this, event)}}
                                required={true}
                            />
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Config Fields <span className="text-danger">*</span></label>
                          <div className="col-sm-5">
                            <button type="button" className="btn btn-sm btn-primary" onClick={this.handleAddFields.bind(this)}>Add Config Fields</button>
                          </div>
                          {this.state.topologyComponentUISpecification.length === 0 ? (
                            <div className="col-sm-4">
                              <p className="form-control-static text-danger">Please add Config Fields</p>
                            </div>)
                          : null}
                        </div>
                        <div className="row">
                          <div className="col-sm-10 col-sm-offset-2">
                            <Table
                                    className="table table-hover table-bordered"
                                    noDataText="No records found."
                                    currentPage={0}
                                    itemsPerPage={this.state.topologyComponentUISpecification.length > pageSize ? pageSize : 0} pageButtonLimit={5}>
                                      <Thead>
                                        <Th column="fieldName">Field Name</Th>
                                        <Th column="uiName">UI Name</Th>
                                        <Th column="isOptional">Is Optional</Th>
                                        <Th column="type">Type</Th>
                                        <Th column="defaultValue">Default Value</Th>
                                        <Th column="isUserInput">Is User Input</Th>
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
                                          <Td column="isUserInput">{obj.isUserInput}</Td>
                                          <Td column="tooltip">{obj.tooltip}</Td>
                                          <Td column="action">
                                            <div className="btn-action">
                                    <BtnEdit callback={this.handleConfigFieldsEdit.bind(this, obj.id)}/>
                                    <BtnDelete callback={this.handleConfigFieldsDelete.bind(this, obj.id)}/>
                                            </div>
                                          </Td>
                                        </Tr>
                                      )
                                    })}
                                  </Table>
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Input Schema <span className="text-danger">*</span></label>
                          <div className="col-sm-6">
                            <ReactCodemirror
                              ref="JSONCodemirror"
                              value={this.state.inputSchema}
                              onChange={this.handleInputSchemaChange.bind(this)}
                              options={jsonoptions}
                            />
                          </div>
                        </div>
                        <div className="form-group">
                          <label className="col-sm-2 control-label">Output Schema <span className="text-danger">*</span></label>
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
			<Modal ref="ConfigFieldModal" data-title={this.state.modalTitle} data-resolve={this.handleSaveConfigFieldModal.bind(this)}>
				{this.modalContent()}
			</Modal>
			</div>
			)
	}

}
