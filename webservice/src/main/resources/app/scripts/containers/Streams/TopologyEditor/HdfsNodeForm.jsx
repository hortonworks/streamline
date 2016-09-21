import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import TopologyREST from '../../../rest/TopologyREST';

export default class HdfsNodeForm extends Component {
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
		keys.map((k)=>{
			configFields[k] = data[k];
		});
		return configFields;

	}

	getConfigFields(config) {
		let reqFieldArr = [],
			optionalFieldArr = [];
		config.map((obj,i) => {
			if(!obj.isOptional)
				reqFieldArr.push({
					name: obj.name,
					type: obj.type === 'number' ? "number" : (obj.type === "boolean" ? "radio" : "text"),
					defaultValue: obj.defaultValue
				});
			else {
				optionalFieldArr.push({
					name: obj.name,
					type: obj.type === 'number' ? "number" : (obj.type === "boolean" ? "radio" : "text"),
					defaultValue: obj.defaultValue
				});
			}
		});
		return {
			reqFieldArr: reqFieldArr,
			fieldRowArr: optionalFieldArr
		};
	}

	handleValueChange(e) {
		let obj = this.state.configFields;
		let value = e.target.value;
		obj[e.target.name] = value === '' ? '' : (e.target.type === "number" ? parseInt(value, 10) : value)
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

		return data;
	}

	validateData(){
		let {reqFieldArr, configFields} = this.state;
		let validDataFlag = true;
		
		reqFieldArr.map((o)=>{
			if(configFields[o.name] === '') validDataFlag = false;
		});
		
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

	generateFields(fieldsArr, requiredFlag){
		return fieldsArr.map((o)=>{
			return (
				<div className="form-group" key={o.name}>
					<label className="col-sm-3 control-label">{o.name}{requiredFlag ? '*' : null}</label>
					<div className="col-sm-6">
						<input
						name={o.name}
						value={this.state.configFields[o.name]}
						onChange={this.handleValueChange.bind(this)}
						type={o.type}
						className="form-control"
					    required={requiredFlag}
					    disabled={!this.state.editMode}
					    min={o.type === "number" ? "0" : null}
						inputMode={o.type === "number" ? "numeric" : null}
						/>
					</div>
					{requiredFlag && this.state.editMode && this.state.configFields[o.name] === '' ?
					<div className="col-sm-3">
						<p className="form-control-static error-note">{o.name} cannot be blank.</p>
					</div>
					: null}
				</div>
			)
		})
	}

	render() {
		return (
			<div>
				<form className="form-horizontal">
					{this.generateFields(this.state.reqFieldArr, true)}
					{this.generateFields(this.state.fieldRowArr, false)}
				</form>
			</div>
		)
	}
}