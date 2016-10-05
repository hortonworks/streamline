import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import AggregateUdfREST from '../../../rest/AggregateUdfREST';
import OutputSchema from '../../../components/OutputSchemaComponent';
import Utils from '../../../utils/Utils';

export default class WindowingAggregateNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		// configData: PropTypes.object,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
		sourceNode: PropTypes.object.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired,
		currentEdges: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
		let {editMode} = props;
		this.fetchData();
		var obj = {
			parallelism: 1,
			editMode: editMode,
			showSchema: false,
			selectedKeys: [],
			streamsList: [],
			keysList: [],
			intervalType: ".Window$Duration",
			intervalTypeArr: [
				{value: ".Window$Duration", label: "Time"},
				{value: ".Window$Count", label: "Count"}
			],
			windowNum: '',
			slidingNum: '',
			durationType: "Seconds",
			slidingDurationType: "Seconds",
			durationTypeArr: [
				{value: "Seconds", label: "Seconds"},
				{value: "Minutes", label: "Minutes"},
				{value: "Hours", label: "Hours"},
			],
			outputFieldsArr: [{inputFieldName: '', functionName: '', outputFieldName: ''}],
			functionListArr: []
		};
		this.state = obj;
	}

	fetchData(){
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
					TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId),
					AggregateUdfREST.getAllUdfs()
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
						let configFields = results[0].entity.config.properties;
						this.windowId = configFields.rules ? configFields.rules[0] : null;
						let fields = [];
						let streamsList = [];
						let udfList = results[1].entities;
						results.map((result,i)=>{
							if(i > 1){
								streamsList.push(result.entity);
								fields.push(...result.entity.fields);
							}
						})
						let stateObj = {
							streamsList: streamsList,
							keysList: fields,
							parallelism: configFields.parallelism || 1,
							functionListArr: udfList
						}
						if(this.windowId){
							TopologyREST.getNode(topologyId, 'windows', this.windowId)
								.then((windowResult)=>{
									let windowData = windowResult.entity;
									stateObj.outputFieldsArr = windowData.projections;
									stateObj.selectedKeys = windowData.groupbykeys;
									this.windowAction = windowData.actions;
									if(windowData.window.windowLength.class === '.Window$Duration'){
										stateObj.intervalType = '.Window$Duration';
										let obj = Utils.millisecondsToNumber(windowData.window.windowLength.durationMs);
										stateObj.windowNum = obj.number;
										stateObj.durationType = obj.type;
										if(windowData.window.slidingInterval){
											let obj = Utils.millisecondsToNumber(windowData.window.slidingInterval.durationMs);
											stateObj.slidingNum = obj.number;
											stateObj.slidingDurationType = obj.type;
										}
									} else if(windowData.window.windowLength.class === '.Window$Count'){
										stateObj.intervalType = '.Window$Count';
										stateObj.windowNum = windowData.window.windowLength.count;
										if(windowData.window.slidingInterval){
											stateObj.slidingNum = windowData.window.slidingInterval.count;
										}
									}
									this.setState(stateObj);
								})
						} else {
							this.setState(stateObj);
						}
					})
			})

	}

	handleKeysChange(arr){
		let keys = [];
		if(arr && arr.length){
			for(let k of arr){
				keys.push(k.name);
			}
			this.setState({selectedKeys: keys});
		} else {
			this.setState({selectedKeys: []});
		}
	}

	handleIntervalChange(obj){
		if(obj){
			this.setState({intervalType: obj.value});
		} else {
			this.setState({intervalType: ""});
		}
	}

	handleDurationChange(obj){
		if(obj){
			this.setState({durationType: obj.value, slidingDurationType: obj.value});
		} else {
			this.setState({durationType: "", slidingDurationType: ""});
		}
	}

	handleSlidingDurationChange(obj){
		if(obj){
			this.setState({slidingDurationType: obj.value});
		} else {
			this.setState({slidingDurationType: ""});
		}
	}

	handleValueChange(e){
		let obj = {};
		let name = e.target.name;
		let value = e.target.type === "number" ? Math.abs(e.target.value) : e.target.value;
		obj[name] = value;
		if(name === 'windowNum'){
			obj['slidingNum'] = value;
		}
		this.setState(obj);
	}

	handleFieldChange(name, index, obj){
		let fieldsArr = this.state.outputFieldsArr;
		if(name === 'outputFieldName'){
			fieldsArr[index][name] = this.refs.outputFieldName.value;
		} else {
			fieldsArr[index][name] = obj.name;
			if(fieldsArr[index].inputFieldName !== '' && fieldsArr[index].functionName!== ''){
				fieldsArr[index].outputFieldName = fieldsArr[index].inputFieldName+'_'+fieldsArr[index].functionName;
			}
		}
		this.setState({outputFieldsArr: fieldsArr});
	}
	addOutputFields(){
		if(this.state.editMode){
			let fieldsArr = this.state.outputFieldsArr;
			fieldsArr.push({inputFieldName: '', functionName: '', outputFieldName: ''});
			this.setState({outputFieldsArr: fieldsArr});
		}
	}
	deleteFieldRow(index){
		if(this.state.editMode){
			let fieldsArr = this.state.outputFieldsArr;
			fieldsArr.splice(index, 1);
			this.setState({outputFieldsArr: fieldsArr});
		}
	}

	validateData(){
		let {selectedKeys, windowNum, outputFieldsArr} = this.state;
		let validData = true;
		if(selectedKeys.length === 0 || windowNum === ''){
			validData = false;
		}
		outputFieldsArr.map((obj)=>{
			if(obj.inputFieldName === '' || obj.functionName === '' || obj.outputFieldName === ''){
				validData = false;
			}
		})
		return validData;
	}

	handleSave(){
		let {selectedKeys, windowNum, slidingNum, outputFieldsArr, durationType, slidingDurationType,
			intervalType, streamsList, parallelism} = this.state;
		let {topologyId, nodeType, nodeData} = this.props;
		let windowObj = {
			name: 'window_auto_generated',
			description: 'window description auto generated',
			projections:[],
			streams: [],
			groupbykeys: selectedKeys,
			window:{
				windowLength:{
					class: intervalType,
				}
			},
			actions:this.windowAction || []
		}

		//Adding stream names into data
		streamsList.map((stream)=>{
			stream.fields.map((field)=>{
				if(selectedKeys.indexOf(field.name) !== -1){
					if(windowObj.streams.indexOf(stream.streamId) === -1){
						windowObj.streams.push(stream.streamId);
					}
				}
			})
		})
		//Adding projections aka output fields into data
		outputFieldsArr.map((obj)=>{
			windowObj.projections.push({
				inputFieldName: obj.inputFieldName,
				functionName: obj.functionName,
				outputFieldName: obj.outputFieldName
			})
		})
		//Syncing window object into data
		if(intervalType === '.Window$Duration'){
			windowObj.window.windowLength.durationMs = Utils.numberToMilliseconds(windowNum, durationType);
			if(slidingNum !== ''){
				windowObj.window.slidingInterval = {
					class: intervalType,
					durationMs: Utils.numberToMilliseconds(slidingNum, slidingDurationType)
				};
			}
		} else if (intervalType === '.Window$Count'){
			windowObj.window.windowLength.count = windowNum;
			if(slidingNum !== ''){
				windowObj.window.slidingInterval = {
					class: intervalType,
					count: slidingNum
				};
			}
		}
		if(this.windowId){
			return TopologyREST.getNode(topologyId, 'windows', this.windowId)
					.then((result)=>{
						let data = result.entity;
						windowObj.actions = result.entity.actions || [];
						return TopologyREST.updateNode(topologyId, 'windows', this.windowId, {body: JSON.stringify(windowObj)})
								.then(windowResult=>{
									return this.updateNode(windowResult);
								})
					})
		} else {
			return TopologyREST.createNode(topologyId, 'windows', {body: JSON.stringify(windowObj)})
				.then(windowResult=>{
					return this.updateNode(windowResult);
				})
		}
	}
	updateNode(windowObj){
		let {parallelism} = this.state;
		let {topologyId, nodeType, nodeData} = this.props;
		return TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId)
				.then(result=>{
					let data = result.entity;
					if(windowObj && windowObj.responseCode === 1000){
						let windowData = windowObj.entity;
						data.config.properties.parallelism = parallelism;
						data.config.properties.rules = [windowData.id];
						return TopologyREST.updateNode(topologyId, nodeType, nodeData.nodeId, {body: JSON.stringify(data)});
					} else {
						FSReactToastr.error(<strong>{windowObj.responseMessage}</strong>);
					}
				})
	}

	render() {
		let {parallelism, selectedKeys, keysList, editMode, intervalType, intervalTypeArr, windowNum, slidingNum,
			durationType, slidingDurationType, durationTypeArr, outputFieldsArr, functionListArr } = this.state;
		let {topologyId, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		return (
			<Tabs id="RulesForm" defaultActiveKey={1} className="schema-tabs">
				<Tab eventKey={1} title="Configuration">
					<div>
						<form className="form-horizontal">
							<div className="form-group">
								<label className="col-sm-3 control-label">Select Keys*</label>
								<div className="col-sm-6">
									<Select
										value={selectedKeys}
										options={keysList}
										onChange={this.handleKeysChange.bind(this)}
										multi={true}
										required={true}
										disabled={!editMode}
										valueKey="name"
										labelKey="name"
									/>
								</div>
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Window Interval Type*</label>
								<div className="col-sm-3">
									<Select
										value={intervalType}
										options={intervalTypeArr}
										onChange={this.handleIntervalChange.bind(this)}
										required={true}
										disabled={!editMode}
										clearable={false}
									/>
								</div>
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Window Interval*</label>
								<div className="col-sm-3">
									<input
										name="windowNum"
										value={windowNum}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
										required={true}
										disabled={!editMode}
										min="0"
										inputMode="numeric"
									/>
								</div>
								{intervalType === '.Window$Duration' ? 
									<div className="col-sm-3">
										<Select
											value={durationType}
											options={durationTypeArr}
											onChange={this.handleDurationChange.bind(this)}
											required={true}
											disabled={!editMode}
											clearable={false}
										/>
									</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Sliding Interval</label>
								<div className="col-sm-3">
									<input
										name="slidingNum"
										value={slidingNum}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
										required={true}
										disabled={!editMode}
										min="0"
										inputMode="numeric"
									/>
								</div>
								{intervalType === '.Window$Duration' ? 
									<div className="col-sm-3">
										<Select
											value={slidingDurationType}
											options={durationTypeArr}
											onChange={this.handleSlidingDurationChange.bind(this)}
											required={true}
											disabled={!editMode}
											clearable={false}
										/>
									</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Parallelism</label>
								<div className="col-sm-3">
									<input
										name="parallelism"
										value={parallelism}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
										required={true}
										disabled={!editMode}
										min="0"
										inputMode="numeric"
									/>
								</div>
							</div>
							<fieldset className="fieldset-default">
								<legend>Output Fields</legend>
								{editMode ?
									<button className="btn btn-success btn-sm" type="button" onClick={this.addOutputFields.bind(this)}>
										<i className="fa fa-plus-circle"></i> Add Output Field
									</button>
								:null}
								<div className="clearfix row-margin-bottom"></div>
								{outputFieldsArr.map((obj, i)=>{
									return(
										<div key={i} className="form-group">
											<div className="col-sm-4">
												{i === 0 ? <label>Input</label>: null}
												<Select
													value={obj.inputFieldName}
													options={keysList}
													onChange={this.handleFieldChange.bind(this, 'inputFieldName', i)}
													required={true}
													disabled={!editMode}
													valueKey="name"
													labelKey="name"
													clearable={false}
												/>
											</div>
											<div className="col-sm-3">
												{i === 0 ? <label>Aggregate Function</label>: null}
												<Select
													value={obj.functionName}
													options={functionListArr}
													onChange={this.handleFieldChange.bind(this, 'functionName', i)}
													required={true}
													disabled={!editMode}
													valueKey="name"
													labelKey="name"
													clearable={false}
												/>
											</div>
											<div className="col-sm-4">
												{i === 0 ? <label>Output</label>: null}
												<input
													name="outputFieldName"
													value={obj.outputFieldName}
													ref="outputFieldName"
													onChange={this.handleFieldChange.bind(this, 'outputFieldName', i)}
													type="text"
													className="form-control"
													required={true}
													disabled={!editMode}
												/>
											</div>
											{i > 0 && editMode?
												<div className="col-sm-1">
													<button className="btn btn-sm btn-danger" type="button" onClick={this.deleteFieldRow.bind(this, i)}>
														<i className="fa fa-trash"></i>
													</button>
												</div>
											: null}
										</div>
									)
								})}
							</fieldset>
						</form>
					</div>
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
		)
	}
}