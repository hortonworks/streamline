import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Panel, Tab, Tabs} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import FileREST from '../../../rest/FileREST';
import TopologyREST from '../../../rest/TopologyREST';
import OutputSchema from '../../../components/OutputSchemaComponent';

export default class SplitNodeForm extends Component {
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
			splitterClassName: ''
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

				let config = this.nodeData.config.properties['split-config'];
				if(config){
					let {splitterClassName, jarId} = config;
					stateObj.splitterClassName = splitterClassName || '';
					stateObj.fileId = jarId || '';
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
		let {fileId, splitterClassName, parallelism} = this.state;
		if(splitterClassName === '' || fileId === '' ){
			return false;
		}
		return true;
	}

	handleSave(name){
		let {topologyId, nodeType} = this.props;
		let {fileId, splitterClassName, parallelism} = this.state;
		let nodeId = this.nodeData.id;
		return TopologyREST.getNode(topologyId, nodeType, nodeId)
			.then(data=>{
				let splitConfigData = data.entity.config.properties["split-config"];
				if(!splitConfigData){
					splitConfigData = {
						name: 'split-action',
						outputStreams: [],
						__type: 'com.hortonworks.iotas.streams.layout.component.impl.splitjoin.SplitAction'
					};
				}
				splitConfigData.jarId = fileId;
				splitConfigData.splitterClassName = splitterClassName;

				data.entity.config.properties["split-config"] = splitConfigData;
				data.entity.config.properties.parallelism = parallelism;
				data.entity.name = name;

				return TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(data.entity)})
			})
	}

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		return (
			<div>
				<Tabs id="splitForm" defaultActiveKey={1} className="schema-tabs">
					<Tab eventKey={1} title="Configuration">
						<form className="form-horizontal">
							<div className="form-group">
								<label className="col-sm-3 control-label">Jar*</label>
								<div className="col-sm-6">
									<Select
										value={this.state.fileId}
										options={this.state.fileArr}
										onChange={this.handleFileChange.bind(this)}
										required={true}
										disabled={!this.state.editMode}
									/>
								</div>
								{this.state.editMode && this.state.fileId === '' ?
									<div className="col-sm-3">
										<p className="form-control-static error-note">Select a jar.</p>
									</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Splitter Class*</label>
								<div className="col-sm-6">
									<input
										name="splitterClassName"
										value={this.state.splitterClassName}
										onChange={this.handleValueChange.bind(this)}
										type="text"
										className="form-control"
										required={true}
										disabled={!this.state.editMode}
									/>
								</div>
								{this.state.editMode && this.state.splitterClassName === '' ?
									<div className="col-sm-3">
										<p className="form-control-static error-note">Splitter class cannot be blank.</p>
									</div>
								: null}
							</div>
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
						/>
					</Tab>
				</Tabs>
			</div>
		)
	}
}