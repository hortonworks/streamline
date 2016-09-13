import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Radio} from 'react-bootstrap';
import TopologyREST from '../../../rest/TopologyREST';

export default class NotificationNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
		sourceNodes: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
		let {configData, editMode} = props;
		this.fetchData();
		if(typeof configData.config === 'string'){
			configData.config = JSON.parse(configData.config)
		}

		let obj = {
			configData: configData,
			configFields: {},
			editMode: editMode
		};
		configData.config.map(o => {
			if(o.type.search('array') !== -1){
				let s = {};
				o.defaultValue.map((d)=>{
					s[d.name] = d.defaultValue === null ? '' : d.defaultValue;
				})
				obj.configFields[o.name] = s;
			} else {
				obj.configFields[o.name] = o.defaultValue === null ? '' : o.defaultValue;
			}
		});

		this.rulesArr = [];
		this.state = obj;
		this.fetchRules();
	}

	fetchRules(){
		let {topologyId, sourceNodes} = this.props;
		let promiseArr = [];
		//Get all source nodes of notification and find rule processor
		//to update actions part if present
		sourceNodes.map((sourceNode)=>{
			promiseArr.push(TopologyREST.getNode(topologyId, 'processors', sourceNode.nodeId));
		})

		Promise.all(promiseArr)
			.then(results=>{
				let rulePromises = [];
				results.map(result=>{
					let data = result.entity;
					if(data.type === 'RULE'){
						if(data.config.properties.rules){
							data.config.properties.rules.map(ruleId=>{
								rulePromises.push(TopologyREST.getNode(topologyId, 'rules', ruleId));
							})
						}
					}
				})
				Promise.all(rulePromises)
					.then(ruleResults=>{
						ruleResults.map((rule, i)=>{
							this.rulesArr.push(rule.entity);
						})
					})
			})
	}

	fetchData(){
		let {topologyId, nodeType, nodeData} = this.props;
		let promiseArr = [
			TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId),
		];

		Promise.all(promiseArr)
			.then((results)=>{
				//#1. get node result
				this.nodeData = results[0].entity;
				let configFields = this.syncData(results[0].entity.config.properties);

				this.setState({configFields: configFields});
			})
			.catch((err)=>{
				console.error(err);
			})
	}

	syncData(data){
		let keys = _.keys(data);
		let configFields = this.state.configFields;
		let configKeys = _.keys(configFields);
		configKeys.map((c)=>{
			if(typeof configFields[c] !== 'object'){
				configFields[c] = data[c] || configFields[c];
			} else {
				let internalKey = _.keys(configFields[c]);
				internalKey.map((i)=>{
					if(data[c]){
						configFields[c][i] = data[c][i] || configFields[c][i]
					}
				})
			}
		})
		return configFields;
	}

	handleValueChange(e) {
		let obj = this.state.configFields;
		let value = e.target.value;
		let parentObjKey = e.target.dataset.parentobjkey;
		let result = null;
		if(value === ''){
			result = '';
		} else if(e.target.type === "number"){
			result = parseInt(value, 10);
		} else if(e.target.dataset.label === "true" || e.target.dataset.label === "true"){
			result = JSON.parse(e.target.dataset.label);
		} else {
			result = value;
		}
		if(parentObjKey){
			obj[parentObjKey][e.target.name] = result;
		} else {
			obj[e.target.name] = result
		}
		this.setState({configFields: obj});
	}

	validateData(){
		let {configFields} = this.state;
		let validDataFlag = true;
		let configKeys = _.keys(configFields);
		configKeys.map((c)=>{
			if(typeof configFields[c] !== 'object'){
				if(configFields[c] === '') validDataFlag = false; 
			} else {
				let internalKey = _.keys(configFields[c]);
				internalKey.map((i)=>{
					if(configFields[c][i] === '') validDataFlag = false;
				})
			}
		})
		return validDataFlag;
	}

	handleSave(){
		let {topologyId, nodeType} = this.props;
		let nodeId = this.nodeData.id;
		this.nodeData.config.properties = this.state.configFields;
		let promiseArr = [TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)})]
		if(this.rulesArr.length){
			this.rulesArr.map(rule=>{
				if(rule.actions){
					let updateFlag = false;
					rule.actions.map((a,i)=>{
						if(a.name === this.nodeData.name){
							a.notifierName = this.state.configFields.notifierName;
							a.outputFieldsAndDefaults = this.state.configFields.fieldValues
							updateFlag = true;
						}
					})
					if(updateFlag){
						promiseArr.push(TopologyREST.updateNode(topologyId, 'rules', rule.id, {body: JSON.stringify(rule)}))
					}
				}
			})
		}
		return Promise.all(promiseArr);
	}

	render() {
		return (
			<div>
				<form className="form-horizontal">
					<div className="row">
						<div className="col-sm-6">
							<div className="form-group">
								<label className="col-sm-4 control-label">Notifier Name</label>
								<div className="col-sm-8">
									<input
									name="notifierName"
									value={this.state.configFields.notifierName}
									type="text"
									className="form-control"
								    disabled={true}
									/>
								</div>
							</div>
						</div>
						<div className="col-sm-6">
							<div className="form-group">
								<label className="col-sm-4 control-label">Jar File Name</label>
								<div className="col-sm-8">
									<input
									name="jarFileName"
									value={this.state.configFields.jarFileName}
									type="text"
									className="form-control"
								    disabled={true}
									/>
								</div>
							</div>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-6">
							<div className="form-group">
								<label className="col-sm-4 control-label">Class Name</label>
								<div className="col-sm-8">
									<input
									name="className"
									value={this.state.configFields.className}
									type="text"
									className="form-control"
								    disabled={true}
									/>
								</div>
							</div>
						</div>
						<div className="col-sm-6">
							<div className="form-group">
								<label className="col-sm-4 control-label">Hbase Config Key</label>
								<div className="col-sm-8">
									<input
									name="hbaseConfigKey"
									value={this.state.configFields.hbaseConfigKey}
									type="text"
									className="form-control"
								    disabled={true}
									/>
								</div>
							</div>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-6">
							<div className="form-group">
								<label className="col-sm-4 control-label">Parallelism</label>
								<div className="col-sm-8">
									<input
									name="parallelism"
									value={this.state.configFields.parallelism}
									onChange={this.handleValueChange.bind(this)}
									type="number"
									className="form-control"
									required={true}
								    disabled={!this.state.editMode}
								    min="0"
									inputMode="numeric"
									/>
								</div>
							</div>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-12">
							<fieldset className="fieldset-default">
								<legend>Properties</legend>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Username *</label>
											<div className="col-sm-8">
												<input
												name="username"
												data-parentObjKey="properties"
												value={this.state.configFields.properties.username}
												onChange={this.handleValueChange.bind(this)}
												type="text"
												className="form-control"
											    required={true}
								    			disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.properties.username === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">Username cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Password *</label>
											<div className="col-sm-8">
												<input
												name="password"
												data-parentObjKey="properties"
												value={this.state.configFields.properties.password}
												onChange={this.handleValueChange.bind(this)}
												type="password"
												className="form-control"
											    required={true}
								    			disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.properties.password === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">Password cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
								</div>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Host *</label>
											<div className="col-sm-8">
												<input
												name="host"
												data-parentObjKey="properties"
												value={this.state.configFields.properties.host}
												onChange={this.handleValueChange.bind(this)}
												type="text"
												className="form-control"
											    required={true}
								    			disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.properties.host === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">Host cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Port *</label>
											<div className="col-sm-8">
												<input
												name="port"
												data-parentObjKey="properties"
												value={this.state.configFields.properties.port}
												onChange={this.handleValueChange.bind(this)}
												type="number"
												className="form-control"
											    required={true}
								    			disabled={!this.state.editMode}
								    			min="0"
									    		inputMode="numeric"
												/>
											</div>
											{this.state.editMode && this.state.configFields.properties.port === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">Port cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
								</div>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Start TLS</label>
											<div className="col-sm-8">
												<Radio 
													inline={true} 
													data-label="true" 
													name="starttls"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.starttls ? true: false}
													disabled={!this.state.editMode}>true
												</Radio>
												<Radio
													inline={true} 
													data-label="false" 
													name="starttls"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.starttls ? false : true}
													disabled={!this.state.editMode}>false
												</Radio>
											</div>
										</div>
									</div>
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Debug</label>
											<div className="col-sm-8">
												<Radio 
													inline={true} 
													data-label="true" 
													name="debug"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.debug ? true: false}
													disabled={!this.state.editMode}>true
												</Radio>
												<Radio
													inline={true} 
													data-label="false" 
													name="debug"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.debug ? false : true}
													disabled={!this.state.editMode}>false
												</Radio>
											</div>
										</div>
									</div>
								</div>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">SSL</label>
											<div className="col-sm-8">
												<Radio 
													inline={true} 
													data-label="true" 
													name="ssl"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.ssl ? true: false}
													disabled={!this.state.editMode}>true
												</Radio>
												<Radio
													inline={true} 
													data-label="false" 
													name="ssl"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.ssl ? false : true}
													disabled={!this.state.editMode}>false
												</Radio>
											</div>
										</div>
									</div>
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Auth</label>
											<div className="col-sm-8">
												<Radio 
													inline={true} 
													data-label="true" 
													name="auth"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.auth ? true: false}
													disabled={!this.state.editMode}>true
												</Radio>
												<Radio
													inline={true} 
													data-label="false" 
													name="auth"
													data-parentObjKey="properties"
													onChange={this.handleValueChange.bind(this)} 
													checked={this.state.configFields.properties.auth ? false : true}
													disabled={!this.state.editMode}>false
												</Radio>
											</div>
										</div>
									</div>
								</div>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Protocol</label>
											<div className="col-sm-8">
												<input
												name="protocol"
												data-parentObjKey="properties"
												value={this.state.configFields.properties.protocol}
												onChange={this.handleValueChange.bind(this)}
												type="text"
												className="form-control"
												required={true}
											    disabled={!this.state.editMode}
												/>
											</div>
										</div>
									</div>
								</div>
							</fieldset>
						</div>
					</div>
					<div className="row">
						<div className="col-sm-12">
							<fieldset className="fieldset-default">
								<legend>Field Values</legend>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">From *</label>
											<div className="col-sm-8">
												<input
												name="from"
												data-parentObjKey="fieldValues"
												value={this.state.configFields.fieldValues.from}
												onChange={this.handleValueChange.bind(this)}
												type="text"
												className="form-control"
											    required={true}
								    			disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.fieldValues.from === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">From cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">To *</label>
											<div className="col-sm-8">
												<input
												name="to"
												data-parentObjKey="fieldValues"
												value={this.state.configFields.fieldValues.to}
												onChange={this.handleValueChange.bind(this)}
												type="text"
												className="form-control"
											    required={true}
								    			disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.fieldValues.to === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">To cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
								</div>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Subject *</label>
											<div className="col-sm-8">
												<input
												name="subject"
												data-parentObjKey="fieldValues"
												value={this.state.configFields.fieldValues.subject}
												onChange={this.handleValueChange.bind(this)}
												type="text"
												className="form-control"
											    required={true}
								    			disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.fieldValues.subject === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">Subject cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Content type</label>
											<div className="col-sm-8">
												<input
												name="contentType"
												data-parentObjKey="fieldValues"
												value={this.state.configFields.fieldValues.contentType}
												onChange={this.handleValueChange.bind(this)}
												type="text"
												className="form-control"
								    			disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.fieldValues.contentType === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">Content type cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
								</div>
								<div className="row">
									<div className="col-sm-6">
										<div className="form-group">
											<label className="col-sm-4 control-label">Body *</label>
											<div className="col-sm-8">
												<textarea 
													name="body"
													data-parentObjKey="fieldValues"
													value={this.state.configFields.fieldValues.body}
													onChange={this.handleValueChange.bind(this)}
													className="form-control"
													required={true}
								    				disabled={!this.state.editMode}
												/>
											</div>
											{this.state.editMode && this.state.configFields.fieldValues.body === '' ?
												<div className="col-sm-8 col-sm-offset-4">
													<p className="form-control-static error-note">Body cannot be blank.</p>
												</div>
											: null}
										</div>
									</div>
								</div>
							</fieldset>
						</div>
					</div>
				</form>
			</div>
		)
	}
}