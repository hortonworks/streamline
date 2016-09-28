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
			editMode: editMode,
			showSchema: true
		};
		configData.config.map(o => {
			obj.configFields[o.name] = o.defaultValue === null ? '' : o.defaultValue;
		});

		this.state = obj;
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
		let passStreamData = { streamId: 'parsedTuplesStream', fields: [
			{"name": "device_id", "type": "STRING", "optional": false },
			{"name": "locale","type": "STRING","optional": false},
			{"name": "software_version","type": "STRING","optional": false},
			{"name": "structure_id","type": "STRING","optional": false},
			{"name": "name","type": "STRING","optional": false},
			{"name": "name_long","type": "STRING","optional": false},
			{"name": "last_connection","type": "STRING","optional": false},
			{"name": "is_online","type": "STRING","optional": false},
			{"name": "can_cool","type": "STRING","optional": false},
			{"name": "can_heat","type": "STRING","optional": false},
			{"name": "is_using_emergency_heat","type": "STRING","optional": false},
			{"name": "has_fan","type": "STRING","optional": false},
			{"name": "fan_timer_active","type": "STRING","optional": false},
			{"name": "fan_timer_timeout","type": "STRING","optional": false},
			{"name": "has_leaf","type": "STRING","optional": false},
			{"name": "temperature_scale","type": "STRING","optional": false},
			{"name": "target_temperature_f","type": "STRING","optional": false},
			{"name": "target_temperature_c","type": "STRING","optional": false},
			{"name": "target_temperature_high_f","type": "STRING","optional": false},
			{"name": "target_temperature_high_c","type": "STRING","optional": false},
			{"name": "target_temperature_low_f","type": "STRING","optional": false},
			{"name": "target_temperature_low_c","type": "STRING","optional": false},
			{"name": "away_temperature_high_f","type": "STRING","optional": false},
			{"name": "away_temperature_high_c","type": "STRING","optional": false},
			{"name": "away_temperature_low_f","type": "STRING","optional": false},
			{"name": "away_temperature_low_c","type": "STRING","optional": false},
			{"name": "hvac_mode","type": "STRING","optional": false},
			{"name": "ambient_temperature_f","type": "STRING","optional": false},
			{"name": "ambient_temperature_c","type": "STRING","optional": false},
			{"name": "humidity","type": "STRING","optional": false},
			{"name": "hvac_state","type": "STRING","optional": false},
			{"name": "battery_state","type": "INTEGER","optional": false}
		]};
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

	handleValueChange(e) {
		let obj = this.state.configFields;
		obj[e.target.name] = e.target.type === "number" ? Math.abs(e.target.value) : e.target.value;
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
									onChange={this.handleValueChange.bind(this)}
									type={o.type}
									className="form-control"
								    required={true}
								    disabled={!editMode}
								    min={o.type === "number" ? "0" : null}
									inputMode={o.type === "number" ? "numeric" : null}
									/>
								</div>
								{editMode && this.state.configFields[o.name] === '' ?
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