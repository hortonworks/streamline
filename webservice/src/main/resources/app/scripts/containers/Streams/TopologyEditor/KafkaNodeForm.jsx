import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Panel, Radio, Tabs, Tab} from 'react-bootstrap';
import OutputSchema from '../../../components/OutputSchemaComponent';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import TopologyREST from '../../../rest/TopologyREST';
import ParserREST from '../../../rest/ParserREST';
import FSReactToastr from '../../../components/FSReactToastr';

export default class KafkaNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired
	};

	constructor(props) {
		super(props);
		let {configData, editMode} = props;
		if(typeof configData.config === 'string'){
			configData.config = JSON.parse(configData.config)
		}
		let configObj = this.getConfigFields(configData.config);
		let obj = {
			configData: configData,
			configFields: {},
			reqFieldArr: configObj.reqFieldArr,
			fieldRowArr: configObj.fieldRowArr,
			showOptionalFields: false,
			editMode: editMode,
			showSchema: true,
			showError: false,
			showErrorLabel: false
		};
		configData.config.map(o => {
			obj.configFields[o.name] = o.defaultValue === null ? '' : o.defaultValue;
		});

		this.validTopicName = true;

		this.state = obj;
	}
	componentDidMount(){
		this.fetchData();
	}

	fetchData(){
		let {topologyId, nodeType, nodeData} = this.props;
		let promiseArr = [
			TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId)
		];

		Promise.all(promiseArr)
			.then((results)=>{
				this.nodeData = results[0].entity;
				let configFields = this.syncData(results[0].entity.config.properties);
				let showSchema = false

				if(this.nodeData.outputStreams.length === 0) {
					this.saveStream();
				} else {
					this.streamObj = this.nodeData.outputStreams[0];
					showSchema = true;
				}

				this.setState({configFields: configFields, showSchema: showSchema});
			})
	}

	syncData(data){
		let keys = _.keys(data);
		let configFields = this.state.configFields;
		keys.map((k)=>{
			configFields[k] = data[k];
		});
		return configFields;

	}

	getConfigFields(config) {
		let reqFieldArr = [],
			fieldRowArr = [],
			rowArr = [];
		config.map((obj,i) => {
			if(!obj.isOptional)
				reqFieldArr.push({
					name: obj.name,
					type: obj.type === 'number' ? "number" : (obj.type === "boolean" ? "radio" : "text"),
					defaultValue: obj.defaultValue
				});
			else {
				rowArr.push({
					name: obj.name,
					type: obj.type === 'number' ? "number" : (obj.type === "boolean" ? "radio" : "text"),
					defaultValue: obj.defaultValue
				});
				if(rowArr.length == 2){
					fieldRowArr.push(rowArr)
					rowArr = [];
				}
			}
		});
		if(rowArr.length)
			fieldRowArr.push(rowArr);
		return {
			reqFieldArr: reqFieldArr,
			fieldRowArr: fieldRowArr
		};
	}

	saveStream(){
		let self = this;
		let {topologyId, nodeType} = this.props;
		let passStreamData = { streamId: 'kafka_stream_'+this.nodeData.id, fields: []};
		TopologyREST.createNode(topologyId, 'streams', {body: JSON.stringify(passStreamData)})
			.then(result=>{
				self.nodeData.outputStreamIds = [];
				self.nodeData.outputStreamIds.push(result.entity.id);
				TopologyREST.updateNode(topologyId, nodeType, self.nodeData.id, {body: JSON.stringify(self.nodeData)})
					.then((node)=>{
						self.nodeData = node.entity;
						self.setState({showSchema: true});
					})
			})
	}

	showHidePanel() {
		this.setState({ showOptionalFields: !this.state.showOptionalFields });
	}

	getHeader() {
		let iconClass = this.state.showOptionalFields ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div> <i className={iconClass}></i> Optional Configurations </div>)
	}

	handleValueChange(fieldObj, e) {
		let obj = this.state.configFields;
		obj[e.target.name] = e.target.type === "number" ? Math.abs(e.target.value) : e.target.value;
		let requiredField = _.find(this.state.reqFieldArr, (f)=>{return f.name === e.target.name});
		if(requiredField && e.target.value === '') fieldObj.isInvalid = true;
		else delete fieldObj.isInvalid;
		if(e.target.name === 'topic'){
			let topicName = e.target.value;
			clearTimeout(this.topicNameTimeout);
			this.topicNameTimeout = setTimeout(()=>{
				this.getSchemaFromTopic(topicName);
			}, 1000);
		}
		this.setState({configFields: obj, showError: true, showErrorLabel: false});
	}

	getSchemaFromTopic(topicName){
		let {topologyId} = this.props;
		let resultArr = [];
		if(topicName === ''){
			document.querySelector("input[name=topic]").className = "form-control invalidInput";
			this.validTopicName = false;
			this.showStreams(resultArr);
		} else {
			TopologyREST.getSchemaForKafka(topicName)
				.then(result=>{
					if(result.responseCode !== 1000){
						document.querySelector("input[name=topic]").className = "form-control invalidInput";
						this.validTopicName = false;
					} else {
						document.querySelector("input[name=topic]").className = "form-control";
						this.validTopicName = true;
						resultArr = result.entity;
						if(typeof resultArr === 'string'){
							resultArr = JSON.parse(resultArr);
						}
					}
					this.showStreams(resultArr);
				})
		}
	}

	showStreams(resultArr){
		this.streamObj = {
			streamId: 'kafka_stream_'+this.nodeData.id,
			fields: resultArr,
			id: this.nodeData.outputStreams[0].id
		};

		this.refs.schema.nodeData.outputStreams = [JSON.parse(JSON.stringify(this.streamObj))];
		this.refs.schema.generateData(this.refs.schema.nodeData);
	}

	handleRadioBtn(e) {
		let obj = this.state.configFields;
		obj[e.target.dataset.name] = e.target.dataset.label === "true" ? true : false;
		this.setState({configFields: obj});
	}

	getData() {
		let data = {},
			configArr = this.state.configData.config,
			newFieldsObj = this.state.configFields;
		//Find the updated values from default ones
		configArr.map((o)=>{
			if(o.defaultValue !== null) {
				if(newFieldsObj[o.name] !== o.defaultValue){
					data[o.name] = newFieldsObj[o.name];
				}
			} else if(newFieldsObj[o.name] !== '') {
				data[o.name] = newFieldsObj[o.name];
			}
		})
		return data;
	}

	validateData(){
		let {reqFieldArr, configFields} = this.state;
		let validDataFlag = true;

		reqFieldArr.map((o)=>{
			if(configFields[o.name] === '') {
				validDataFlag = false;
				o.isInvalid = true;
			}
		});
		if(!this.validTopicName){
			validDataFlag = false;
		}
		if(!validDataFlag)
			this.setState({showError: true, showErrorLabel: true});
		else this.setState({showErrorLabel: false});
		return validDataFlag;
	}

	handleSave(name){
		let {topologyId, nodeType} = this.props;
		let nodeId = this.nodeData.id;
		let data = this.getData();
		this.nodeData.config.properties = data;
		this.nodeData.name = name;
		let o = {
			fields: this.streamObj.fields,
			streamId: this.streamObj.streamId,
			id: this.nodeData.outputStreams[0].id,
			topologyId: topologyId
		}
		this.nodeData.outputStreams = [o];
		let promiseArr = [
			TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)}),
			TopologyREST.updateNode(topologyId, 'streams', this.nodeData.outputStreams[0].id, {body: JSON.stringify(this.streamObj)})
		];
		return Promise.all(promiseArr);
	}

	render() {
		let {topologyId, nodeType, targetNodes, linkShuffleOptions, editMode, nodeData} = this.props;
		return (
		<div>
			<Tabs id="KafkaForm" defaultActiveKey={1} className="schema-tabs">
			<Tab eventKey={1} title="Configuration">
				<form className="form-horizontal">
					{this.state.reqFieldArr.map(o=>{
						return (
							<div className="form-group" key={o.name}>
								<label className="col-sm-3 control-label">{o.name}*</label>
								<div className="col-sm-6">
									<input
									name={o.name}
									value={this.state.configFields[o.name]}
									onChange={this.handleValueChange.bind(this, o)}
									type={o.type}
									className={this.state.showError && o.isInvalid ? "form-control invalidInput" : "form-control"}
								    required={true}
								    disabled={!editMode}
								    min={o.type === "number" ? "0" : null}
									inputMode={o.type === "number" ? "numeric" : null}
									/>
								</div>
							</div>
						)
					})
					}
				</form>
				<Panel header={this.getHeader()} collapsible expanded={this.state.showOptionalFields} onSelect={this.showHidePanel.bind(this)}>
				 	{
					 	this.state.fieldRowArr.map((arr, i)=>{
					 		return (
					 			<div className="row" key={i}>
						 			{arr.map((o, i)=>{
						 				let value = this.state.configFields[o.name];
						 				return (
						 					<div className="col-sm-6" key={o.name}>
							 					<div className="form-group">
													<label className="col-sm-6 control-label">{o.name}</label>
													<div className="col-sm-12">
														{o.type === "radio" ?
															[
																<Radio
																	key="1"
																	inline={true}
																	data-label="true"
																	data-name={o.name}
																	onChange={this.handleRadioBtn.bind(this)}
																	checked={value ? true: false}
																	disabled={!editMode}>true
																</Radio>,
																<Radio
																	key="2"
																	inline={true}
																	data-label="false"
																	data-name={o.name}
																	onChange={this.handleRadioBtn.bind(this)}
																	checked={value ? false : true}
																	disabled={!editMode}>false
																</Radio>
															]
															:
																<input
																	name={o.name}
																	value={this.state.configFields[o.name]}
																	onChange={this.handleValueChange.bind(this, o)}
																	type={o.type}
																	className="form-control"
																	disabled={!this.state.editMode}
																	min={o.type === "number" ? "0" : null}
									    							inputMode={o.type === "number" ? "numeric" : null}
																/>
														}
													</div>
												</div>
											</div>
						 				)
						 			})}
					 			</div>
					 		)
					 	})
				 	}
				</Panel>
			</Tab>
			<Tab eventKey={2} title="Output Streams">
						{this.state.showSchema ?
						<OutputSchema
							ref="schema"
							topologyId={topologyId}
							editMode={editMode}
							nodeId={nodeData.nodeId}
							nodeType={nodeType}
							targetNodes={targetNodes}
							linkShuffleOptions={linkShuffleOptions}
							canAdd={false}
							canDelete={false}
						/>
						: null}
					</Tab>
			</Tabs>
		</div>
		)
	}
}