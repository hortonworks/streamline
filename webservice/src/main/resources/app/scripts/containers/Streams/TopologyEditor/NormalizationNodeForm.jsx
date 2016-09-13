import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Panel, Popover, OverlayTrigger, Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import OutputSchema from '../../../components/OutputSchemaComponent';
import Editable from '../../../components/Editable';

export default class NormalizationNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired,
		currentEdges: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
		let {configData, editMode} = props;
		this.fetchData();

		var obj = {
			editMode: editMode,
			showOptionalFields: false,
			parallelism: 1,
			inputStreamsArr: [],
			currentStream: '',
			inputSchemaFields: [],
			outputSchemaFields: [],
			inputStreams: [],
			outputFieldsMappingArr: [],
			transformers: [],
			showMappingFields: false,
			enableAddAll: true,
			showSchema: true
		};

		this.state = obj;
	}

	fetchData() {
		let {topologyId, nodeType, nodeData, currentEdges} = this.props;
		let edgePromiseArr = [];

		currentEdges.map(edge=>{
			if(edge.target.nodeId === nodeData.nodeId){
				edgePromiseArr.push(TopologyREST.getNode(topologyId, 'edges', edge.edgeId));
			}
		})

		Promise.all(edgePromiseArr)
			.then(edgeResults=>{
				let promiseArr = [
					TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId)
				];
				let streamIdArr = []
				edgeResults.map(result=>{
					if(result.entity.streamGroupings){
						result.entity.streamGroupings.map(streamObj=>{
							if(streamIdArr.indexOf(streamObj.streamId) === -1){
								streamIdArr.push(streamObj.streamId);
								promiseArr.push(TopologyREST.getNode(topologyId, 'streams', streamObj.streamId))
							}
						})
					}
				})
				Promise.all(promiseArr)
					.then((results)=>{
						this.nodeData = results[0].entity;
						let properties = this.nodeData.config.properties;
						let outputStreamsArr = [];
						results.map((result, i)=>{
							if(i > 0){
								outputStreamsArr.push(result.entity);
							}
						})
						if(outputStreamsArr.length > 0)
							this.getInputStreams(outputStreamsArr);

						let streams = _.keys(properties.normalizationConfig);
						let arr = [],
							outputSchemaArr = [],
							mappingsArr = [];
						streams.map((s)=>{
							let transformers = properties.normalizationConfig[s].transformers;
							transformers.map((o)=>{
								arr.push({
									streamId: s,
									inputField: o.inputField,
									outputField: o.outputField
								});
								if(!_.find(outputSchemaArr, {name: o.outputField.name})) {
									outputSchemaArr.push(o.outputField);
									mappingsArr.push({
										value: o.outputField.name,
										label: o.outputField.name
									});
								}
							});
						});

						let stateObj = {
							parallelism: properties.parallelism || 1,
							inputStreams: outputStreamsArr,
							transformers: arr,
							outputSchemaFields: outputSchemaArr,
							outputFieldsMappingArr: mappingsArr
						};
						this.setState(stateObj, this.getInputSchemaFields.bind(this));
					})
			})
	}

	getInputStreams(inputStreams) {
		let arr = [];
		inputStreams.map((s)=>{
			arr.push({
				value: s.streamId,
				label: s.streamId
			})
		});
		this.setState({inputStreamsArr: arr, currentStream: arr[0].value});
	}

	getInputSchemaFields(id) {
		let streamData = _.find(this.state.inputStreams, {streamId: id ? id : this.state.currentStream});
		this.setState({inputSchemaFields: streamData.fields});
	}

	showHidePanel() {
		this.setState({ showOptionalFields: !this.state.showOptionalFields });
	}

	getHeader() {
		let iconClass = this.state.showOptionalFields ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div>
			<i className={iconClass}></i> Optional Configuration
			</div>)
	}

	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.type === "number" && e.target.value !== '' ? parseInt(e.target.value, 10) : e.target.value;
		this.setState(obj);
	}

	handleStreamChange(obj) {

		this.setState({
			currentStream: obj.value
		});
		this.getInputSchemaFields(obj.value);
	}

	addAllFields = (e)=> {
		let transformArr= [],
			outputSchemaArr = [],
			mappingsArr = [];

		this.state.inputSchemaFields.map((f)=>{
			let optionObj = {},
			transformObj = {};
			if(_.find(this.state.outputSchemaFields, {name: f.name})) {
				return;
			} else {
				optionObj = {
					value: f.name,
					label: f.name
				};
				transformObj = {
					streamId: this.state.currentStream,
					inputField: f,
					outputField: f
				};
				outputSchemaArr.push(f);
				mappingsArr.push(optionObj);
				transformArr.push(transformObj);
			}
		});
		this.setState({
				outputSchemaFields: [...this.state.outputSchemaFields, ...outputSchemaArr],
				outputFieldsMappingArr: [...this.state.outputFieldsMappingArr, ...mappingsArr],
				transformers: [...this.state.transformers, ...transformArr],
				enableAddAll: false
		});
	}

	addFieldToOutputSchema = (e)=>{
		let name = e.target.dataset.id,
			arr = [],
			optionsArr = [],
			obj = {},
			optionObj = {},
			transformObj = {};

		if(_.find(this.state.outputSchemaFields, {name: name})) {
			this.setState({showMappingFields: false});
		} else {

			obj = _.find(this.state.inputSchemaFields, { name: name});
			optionObj = {
				value: obj.name,
				label: obj.name
			};
			transformObj = {
				streamId: this.state.currentStream,
				inputField: obj,
				outputField: obj
			}

			arr = [...this.state.outputSchemaFields, obj];
			if(!_.find(this.state.outputFieldsMappingArr, {value: optionObj.value}))
				optionsArr = [...this.state.outputFieldsMappingArr, optionObj];
			else optionsArr = [...this.state.outputFieldsMappingArr];

			this.setState({
				outputSchemaFields: arr,
				showMappingFields: false,
				outputFieldsMappingArr: optionsArr,
				transformers: [...this.state.transformers, transformObj]
			});
		}
	}

	mapInputField = ()=>{
		this.setState({
			showMappingFields: true
		});
	}

	deleteOutputField(name) {
		let outputField = _.find(this.state.outputSchemaFields, {name: name}),
			arr = this.state.outputSchemaFields.filter((f)=>{
					return f.name !== outputField.name
				}),
			tArr = this.state.transformers.filter((t)=>{
					return !_.isEqual(t.outputField, outputField);
				}),
			mappingsArr = [];
		let flag = tArr.filter(t=>{return t.streamId === this.state.currentStream;}).length === this.state.inputSchemaFields.length;
		arr.map((m)=>{
			mappingsArr.push({
				value: m.name,
				label: m.name
			});
		});
		this.setState({
			outputSchemaFields: arr,
			outputFieldsMappingArr: mappingsArr,
			transformers: tArr,
			enableAddAll: !flag
		});
	}

	handleMappingChange(f, obj) {
		let transformers = this.state.transformers,
			inputField = _.find(this.state.inputSchemaFields, {name: f}),
			t = _.findIndex(transformers, {streamId: this.state.currentStream, inputField: inputField}),
			outputField = _.find(this.state.outputSchemaFields, {name: obj.value});

		if(t !== -1)
			transformers[t].outputField = outputField;
		else transformers.push({
			inputField: inputField,
			outputField: outputField,
			streamId: this.state.currentStream
		});
		this.setState({
			transformers: transformers
		});
	}

	handleFieldNameChange(e) {
		let name = e.target.value,
			index = e.target.dataset.index;

		let {outputSchemaFields, transformers, outputFieldsMappingArr} = this.state;
		let field = outputSchemaFields[index],
			t = _.findIndex(transformers, {outputField: field});
		if(this.editableFieldName === undefined || this.editableFieldName === '')
				this.editableFieldName = field.name;
		this.editableFieldIndex = index;

		if(this.validateName(name, index)){

			outputSchemaFields[index].name = name;
			transformers[t].outputField.name = name;
			outputFieldsMappingArr[index] = {value: name, label: name};
			this.setState({
				outputSchemaFields: outputSchemaFields,
				transformers: transformers,
				outputFieldsMappingArr: outputFieldsMappingArr
			});
		}
	}

	validateName(name, index){
		let fieldNamesList = this.state.outputSchemaFields.map((f)=>{
			if(f.name !== name)
				return f.name;
		});
		if(name === ''){
			this.refs["fieldNameEditable"+index].setState({errorMsg: "Field name cannot be blank"});
			this.validateFlag = false;
			return false;
		} else if(name.search(' ') !== -1){
			this.refs["fieldNameEditable"+index].setState({errorMsg: "Field name cannot have space in between"});
			this.validateFlag = false;
			return false;
		} else if(fieldNamesList.indexOf(name) !== -1){
			this.refs["fieldNameEditable"+index].setState({errorMsg: "Field name is already present. Please use some other name."});
			this.validateFlag = false;
			return false;
		} else {
			this.refs["fieldNameEditable"+index].setState({errorMsg: ""});
			this.validateFlag = true;
			return true;
		}
	}

	saveFieldName(editable) {
		if(this.validateFlag){
			editable.hideEditor();
		}
		this.editableFieldIndex = '';
		this.editableFieldName = '';
	}

	handleEditableReject(editable) {
		let {outputSchemaFields, transformers, outputFieldsMappingArr} = this.state;

		if(this.editableFieldIndex !== undefined && this.editableFieldIndex !== '') {
			let field = outputSchemaFields[this.editableFieldIndex],
			t = _.findIndex(transformers, {outputField: field});

		outputSchemaFields[this.editableFieldIndex].name = this.editableFieldName;
		transformers[t].outputField.name = this.editableFieldName;
		outputFieldsMappingArr[this.editableFieldIndex] = {value: this.editableFieldName, label: this.editableFieldName};
		this.setState({
			outputSchemaFields: outputSchemaFields,
			transformers: transformers,
			outputFieldsMappingArr: outputFieldsMappingArr
		});
		}
		this.editableFieldIndex = '';
		this.editableFieldName = '';

		editable.setState({errorMsg: ""},()=>{
			editable.hideEditor();
		});
	}

	getData() {
		let config = {
			parallelism: this.state.parallelism,
			type: "fineGrained",
			normalizationConfig: {}
		};

		this.state.inputStreams.map((s)=>{
			let streamFields = this.state.transformers.filter((o)=>{return o.streamId === s.streamId});
			if(streamFields.length) {
				let obj = {
					__type: "com.hortonworks.iotas.streams.layout.component.impl.normalization.FieldBasedNormalizationConfig",
					transformers: [],
					fieldsToBeFiltered: [],
					newFieldValueGenerators: []
				};
				streamFields.map((f)=>{
					obj.transformers.push({
						inputField: f.inputField,
						outputField: f.outputField,
						converterScript: null
					});
				});
				config.normalizationConfig[s.streamId] = obj;
			}
		});
		return config;
	}

	validateData(){
		let validDataFlag = true;

		if(this.state.outputSchemaFields.length === 0)
			validDataFlag = false;
		return validDataFlag;
	}

	handleSave(name){
		let {topologyId, nodeType} = this.props;
		let data = this.getData();
		let nodeId = this.nodeData.id;

		return TopologyREST.getNode(topologyId, nodeType, nodeId)
			.then(result=>{
				let newData = result.entity;
				newData.config.properties = data;
				newData.name = name;
				return TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(newData)})
			})
	}

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {showSchema} = this.state;
		return (
			<div>
				<Tabs id="NormalizationForm" defaultActiveKey={1} className="schema-tabs">
					<Tab eventKey={1} title="Configuration">
						<form className="form-horizontal">
							<div className="form-group">
								<label className="col-sm-3 control-label">Select Stream</label>
								<div className="col-sm-6">
									<Select
										value={this.state.currentStream}
										options={this.state.inputStreamsArr}
										onChange={this.handleStreamChange.bind(this)}
										required={true}
										searchable={false}
										disabled={!this.state.editMode}
									/>
								</div>
							</div>
						</form>
						<div className="row">
							<div className="col-md-6">
								<div className="box success">
									<div className="box-head">
										<h4>Input Schema</h4>
										{editMode ?
											<div className="box-controls">
												<a href="javascript:void(0);"
													title="Add all fields"
													data-rel="tooltip"
													className="select-list"
													onClick={this.addAllFields}
												>
													<i id="selectAll" className={this.state.enableAddAll ? "fa fa-square-o" : "fa fa-check-square-o"}></i>
												</a>
											</div>
										: null}
									</div>
									<div className="box-body scrollable-body">
										<ul className="field-list input-field-list">
											{this.state.inputSchemaFields.length === 0?
												<span>No fields found.</span>
											: this.state.inputSchemaFields.map((f, i)=>{
												let t = _.find(this.state.transformers, {streamId: this.state.currentStream, inputField: f});
												return (
													<li key={i} className={f.isOptional ? "optional" : "mandatory"}>
														{editMode ? 
															<OverlayTrigger
																trigger="click"
																placement="bottom"
																overlay={
																	<Popover id="popover-trigger-click">
																		<button type="button" className="btn btn-primary btn-sm" onClick={this.mapInputField} data-id={f.name}>Map To Existing</button>{'\n'}
																		<button type="button" className="btn btn-primary btn-sm" onClick={this.addFieldToOutputSchema} data-id={f.name}>Add New</button>
																		<hr />
																		{this.state.showMappingFields ?
																		<Select
																			value={t ? t.outputField.name : ''}
																			options={this.state.outputFieldsMappingArr}
																			onChange={this.handleMappingChange.bind(this, f.name)}
																			searchable={false}
																			disabled={!this.state.editMode}
																			clearable={false}
																		/>
																		: null
																		}
																	</Popover>
																}
																onClick={()=>{
																	this.setState({showMappingFields: false});
																}}
															>
															<a href="javascript:void(0)">{f.name}</a>
															</OverlayTrigger>
														: <a href="javascript:void(0)">{f.name}</a> }
														{ t !== undefined ? (<span className="label label-danger pull-right">{t.outputField.name}</span>) : null }
													</li>
													)
												}
											)}
										</ul>
									</div>
								</div>
							</div>
							<div className="col-md-6">
								<div className="box success">
									<div className="box-head">
										<h4>Mapped Schema</h4>
									</div>
									<div className="box-body scrollable-body">
										<ul className="field-list output-list">
											{this.state.outputSchemaFields.map((f, i)=>{
												return (
													<li key={i} className={f.isOptional ? "optional" : "mandatory"}>
															{editMode ?
															<Editable
																ref={"fieldNameEditable"+i}
																inline={true}
																resolve={this.saveFieldName.bind(this)}
																reject={this.handleEditableReject.bind(this)}
															>
																<input defaultValue={f.name} data-index={i} onChange={this.handleFieldNameChange.bind(this)}/>
															</Editable>
															: f.name
															}
															{editMode ?
															<i className="fa fa-trash pull-right" onClick={this.deleteOutputField.bind(this, f.name)}></i>
															: null}
													</li>
													)
												})
											}
										</ul>
									</div>
								</div>
							</div>
						</div>
						<Panel
							header={this.getHeader()}
							collapsible
							expanded={this.state.showOptionalFields}
							onSelect={this.showHidePanel.bind(this)}
						>
							<div className="form-group">
								<label className="col-sm-3 control-label">Parallelism</label>
								<div className="col-sm-6">
									<input
										name="parallelism"
										value={this.state.parallelism}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
										disabled={!this.state.editMode}
										min="0"
										inputMode="numeric"
									/>
								</div>
							</div>
						</Panel>
					</Tab>
					<Tab eventKey={2} title="Output Streams">
						<OutputSchema
							ref="schema"
							topologyId={topologyId}
							editMode={editMode}
							nodeId={nodeData.nodeId}
							nodeType={nodeType}
							targetNodes={targetNodes}
							linkShuffleOptions={linkShuffleOptions}
						/>
					</Tab>
				</Tabs>
			</div>
			)
	}
}