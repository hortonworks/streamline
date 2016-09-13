import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Panel, Tab, Tabs} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import FileREST from '../../../rest/FileREST';
import TopologyREST from '../../../rest/TopologyREST';
import OutputSchema from '../../../components/OutputSchemaComponent';

export default class JoinNodeForm extends Component {
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
		this.fetchData();
		if(typeof configData.config === 'string'){
			configData.config = JSON.parse(configData.config)
		}
		let config = configData.config;
		let parallelismObj = config.filter((f)=>{ return f.name === 'parallelism'});
		if(parallelismObj.length){
			parallelismObj = parallelismObj[0]
		}
		var obj = {
			parallelism: 1,
			editMode: editMode,
			fileArr: [],
			fileName: '',
			fileId:'',
			joinerClassName: '',
			groupExpiryInterval: '',
			eventExpiryInterval: ''
		};
		this.state = obj;
	}

	fetchData() {
		let {topologyId, nodeType, nodeData} = this.props;
		let promiseArr = [
			FileREST.getAllFiles(),
			TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId)
		];

		Promise.all(promiseArr)
			.then((results)=>{
				let fileData= results[0].entities,
					arr = [],
					stateObj = {};
				results[0].entities.map((entity)=>{
						arr.push({
							id: entity.id,
							value: entity.id,
							label: entity.name
						});
				});
				stateObj.fileArr = arr;

				this.nodeData = results[1].entity;
				let {parallelism} = this.nodeData.config.properties;
				stateObj.parallelism = parallelism || 1;

				let config = this.nodeData.config.properties['join-config'];
				if(config){
					let {joinerClassName, jarId, groupExpiryInterval, eventExpiryInterval} = config;
					stateObj.joinerClassName = joinerClassName || '';
					stateObj.fileId = jarId || '';
					stateObj.groupExpiryInterval = groupExpiryInterval || '';
					stateObj.eventExpiryInterval = eventExpiryInterval || '';
				}
				this.setState(stateObj);
			})
	}

	handleFileChange(obj) {
		if(obj){
			this.setState({fileName: obj.label, fileId: obj.id});
		} else {
			this.setState({fileName: '', fileId: ''});
		}
	}

	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value === '' ? '' : e.target.type === "number" ? parseInt(e.target.value, 10) : e.target.value;
		this.setState(obj);
	}

	validateData(){
		let {fileId, joinerClassName, eventExpiryInterval, groupExpiryInterval} = this.state;
		if(joinerClassName === '' || fileId === '' || eventExpiryInterval === '' || groupExpiryInterval === ''){
			return false;
		}
		return true;
	}

	handleSave(){
		let {topologyId, nodeType} = this.props;
		let {fileId, joinerClassName, parallelism, eventExpiryInterval, groupExpiryInterval} = this.state;
		let nodeId = this.nodeData.id;
		return TopologyREST.getNode(topologyId, nodeType, nodeId)
			.then(data=>{
				let joinConfigData = data.entity.config.properties["join-config"];
				if(!joinConfigData){
					joinConfigData = {
						name: 'join-action',
						outputStreams: [],
						__type: 'com.hortonworks.iotas.streams.layout.component.impl.splitjoin.JoinAction'
					};
				}
				joinConfigData.jarId = fileId;
				joinConfigData.joinerClassName = joinerClassName;
				joinConfigData.eventExpiryInterval = eventExpiryInterval;
				joinConfigData.groupExpiryInterval = groupExpiryInterval;

				data.entity.config.properties["join-config"] = joinConfigData;
				data.entity.config.properties.parallelism = parallelism;

				return TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(data.entity)})
			})
	}

	render() {
		let {topologyId, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {fileId, fileArr, joinerClassName, editMode, eventExpiryInterval, groupExpiryInterval, parallelism} = this.state;
		return (
			<div>
				<Tabs id="joinForm" defaultActiveKey={1} className="schema-tabs">
					<Tab eventKey={1} title="Configuration">
						<form className="form-horizontal">
							<div className="form-group">
								<label className="col-sm-3 control-label">Jar*</label>
								<div className="col-sm-6">
									<Select
										value={fileId}
										options={fileArr}
										onChange={this.handleFileChange.bind(this)}
										required={true}
										disabled={!editMode}
									/>
								</div>
								{editMode && fileId === '' ?
									<div className="col-sm-3">
										<p className="form-control-static error-note">Select a jar.</p>
									</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Joiner Class*</label>
								<div className="col-sm-6">
									<input
										name="joinerClassName"
										value={joinerClassName}
										onChange={this.handleValueChange.bind(this)}
										type="text"
										className="form-control"
										required={true}
										disabled={!editMode}
									/>
								</div>
								{editMode && joinerClassName === '' ?
								<div className="col-sm-3">
									<p className="form-control-static error-note">Splitter class cannot be blank.</p>
								</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Group Expiry Interval*</label>
								<div className="col-sm-6">
									<input
										name="groupExpiryInterval"
										value={groupExpiryInterval}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
										disabled={!editMode}
										min="0"
										inputMode="numeric"
									/>
								</div>
								{editMode && joinerClassName === '' ?
								<div className="col-sm-3">
									<p className="form-control-static error-note">Group Expiry Interval cannot be blank.</p>
								</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Event Expiry Interval*</label>
								<div className="col-sm-6">
									<input
										name="eventExpiryInterval"
										value={eventExpiryInterval}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
										disabled={!editMode}
										min="0"
										inputMode="numeric"
									/>
								</div>
								{editMode && joinerClassName === '' ?
								<div className="col-sm-3">
									<p className="form-control-static error-note">Event Expiry Interval cannot be blank.</p>
								</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Parallelism</label>
								<div className="col-sm-6">
									<input
										name="parallelism"
										value={parallelism}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
										disabled={!editMode}
										min="0"
										inputMode="numeric"
									/>
								</div>
							</div>
						</form>
					</Tab>
					<Tab eventKey={2} title="Output Streams">
						<OutputSchema
							topologyId={topologyId}
							editMode={editMode}
							nodeId={nodeData.nodeId}
							nodeType={nodeType}
							targetNodes={targetNodes}
							linkShuffleOptions={linkShuffleOptions}
							maxStreamSize={1}
						/>
					</Tab>
				</Tabs>
			</div>
		)
	}
}