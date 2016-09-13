import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Panel, Radio} from 'react-bootstrap';
import DeviceREST from '../../../rest/DeviceREST';
import TopologyREST from '../../../rest/TopologyREST';
import FSReactToastr from '../../../components/FSReactToastr';

export default class DeviceNodeForm extends Component {
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
		this.fetchData();
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
			dataSourceArr: [],
			dataSourceId: '',
			dataSourceName: configData.dataSourceName ? props.config.dataSourceName: '',
			editMode: editMode
		};
		configData.config.map(o => {
			obj.configFields[o.name] = o.defaultValue === null ? '' : o.defaultValue;
		});

		this.state = obj;
	}

	fetchData(){
		let {topologyId, nodeType, nodeData} = this.props;
		let promiseArr = [
			TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId),
			DeviceREST.getAllDevices()
		];

		Promise.all(promiseArr)
			.then((results)=>{
				//#1. get node result
				this.nodeData = results[0].entity;
				let configFields = this.syncData(results[0].entity.config.properties);

				//#2. get all devices result
				let arr = [];
				results[1].entities.map(entity => {
			    	arr.push({
			    		id: entity.dataSourceId,
			    		value: entity.dataSourceId,
			    		label: entity.dataSourceName
			    	})
			    });

			    let dataSourceId = configFields.dataSourceId || '';
				this.setState({dataSourceArr: arr, configFields: configFields, dataSourceId: dataSourceId});
			})
			.catch((err)=>{
				console.error(err);
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

	showHidePanel() {
		this.setState({ showOptionalFields: !this.state.showOptionalFields });
	}

	getHeader() {
		let iconClass = this.state.showOptionalFields ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div> <i className={iconClass}></i> Optional Configurations </div>)
	}

	handleDeviceChange(obj) {
		if(obj){
			this.setState({dataSourceName: obj.label, dataSourceId: obj.value});
		} else {
			this.setState({dataSourceName: '', dataSourceId: ''});
		}
	}

	handleValueChange(e) {
		let obj = this.state.configFields;
		obj[e.target.name] = e.target.value === '' ? '' : e.target.type === "number" ? parseInt(e.target.value, 10) : e.target.value;
		this.setState({configFields: obj});
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
			if(newFieldsObj[o.name] !== o.defaultValue){
				data[o.name] = newFieldsObj[o.name];
			}
		})
		
		data.dataSourceId = this.state.dataSourceId;
		return data;
	}

	validateData(){
		let {reqFieldArr, dataSourceId, configFields} = this.state;
		let validDataFlag = true;
		
		reqFieldArr.map((o)=>{
			if(configFields[o.name] === '') validDataFlag = false;
		});
		
		if(dataSourceId === '') validDataFlag = false;
		
		return validDataFlag;
	}

	handleSave(name){
		let {topologyId, nodeType} = this.props;
		let nodeId = this.nodeData.id;
		let data = this.getData();
		this.nodeData.config.properties = data;
		this.nodeData.name = name;
		return TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)})
	}

	render() {
		return (
			<div>
				<form className="form-horizontal">
					<div className="form-group">
						<label className="col-sm-3 control-label">Select Device Name*</label>
						<div className="col-sm-6">
							<Select
								value={this.state.dataSourceId}
								options={this.state.dataSourceArr}
								onChange={this.handleDeviceChange.bind(this)}
								required={true}
								disabled={!this.state.editMode}
							/>
						</div>
						{this.state.editMode && this.state.dataSourceId === '' ?
								<div className="col-sm-3">
									<p className="form-control-static error-note">Device name cannot be blank.</p>
								</div>
						: null}
					</div>
					{this.state.reqFieldArr.map(o=>{
						return (
							<div className="form-group" key={o.name}>
								<label className="col-sm-3 control-label">{o.name}*</label>
								<div className="col-sm-6">
									<input
									name={o.name}
									value={this.state.configFields[o.name]}
									onChange={this.handleValueChange.bind(this)}
									type={o.type}
									className="form-control"
								    required={true}
								    disabled={!this.state.editMode}
								    min={o.type === "number" ? "0" : null}
									inputMode={o.type === "number" ? "numeric" : null}
									/>
								</div>
								{this.state.editMode && this.state.configFields[o.name] === '' ?
								<div className="col-sm-3">
									<p className="form-control-static error-note">{o.name} cannot be blank.</p>
								</div>
								: null}
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
																	disabled={!this.state.editMode}>true
																</Radio>,
																<Radio
																	key="2"
																	inline={true} 
																	data-label="false" 
																	data-name={o.name} 
																	onChange={this.handleRadioBtn.bind(this)} 
																	checked={value ? false : true}
																	disabled={!this.state.editMode}>false
																</Radio>
															]
															: 
																<input
																	name={o.name}
																	value={this.state.configFields[o.name]}
																	onChange={this.handleValueChange.bind(this)}
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
			</div>
		)
	}
}