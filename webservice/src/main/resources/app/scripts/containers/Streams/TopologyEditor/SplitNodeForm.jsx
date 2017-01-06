import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Panel, Tab, Tabs} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import FileREST from '../../../rest/FileREST';
import TopologyREST from '../../../rest/TopologyREST';

export default class SplitNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
                versionId: PropTypes.number.isRequired,
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
			splitterClassName: '',
			showError: false,
			showErrorLabel: false,
			changedFields: []
		};
		this.state = obj;
	}

	fetchData() {
                let {topologyId, versionId, nodeType, nodeData} = this.props;
		let promiseArr = [
			FileREST.getAllFiles(),
                        TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId)
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

                                this.nodeData = results[1];
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
		let changedFields = this.state.changedFields;
		if(changedFields.indexOf("fileId") === -1)
			changedFields.push("fileId");
		if(obj){
			this.setState({fileName: obj.label, fileId: obj.id, changedFields: changedFields, showError: true, showErrorLabel: false});
		} else {
			this.setState({fileName: '', fileId: '', changedFields: changedFields, showError: true, showErrorLabel: false});
		}
	}

	handleValueChange(e) {
		let obj = {
			changedFields: this.state.changedFields,
			showError: true,
			showErrorLabel: false
		};
		obj[e.target.name] = e.target.value === '' ? '' : e.target.type === "number" ? Math.abs(e.target.value) : e.target.value;
		if(obj.changedFields.indexOf(e.target.name) === -1)
			obj.changedFields.push(e.target.name);
		this.setState(obj);
	}

	validateData(){
		let {fileId, splitterClassName, parallelism, changedFields} = this.state;
		if(splitterClassName === '' || fileId === '' ){
			if(splitterClassName === '' && changedFields.indexOf("splitterClassName") === -1)
				changedFields.push("splitterClassName");
			else if(changedFields.indexOf("fileId") === -1)
				changedFields.push("fileId");
			this.setState({showError: true, showErrorLabel: true, changedFields: changedFields});
			return false;
		}
		this.setState({showErrorLabel: false});
		return true;
	}

        handleSave(name, description){
                let {topologyId, versionId, nodeType} = this.props;
		let {fileId, splitterClassName, parallelism} = this.state;
		let nodeId = this.nodeData.id;
                return TopologyREST.getNode(topologyId, versionId, nodeType, nodeId)
			.then(data=>{
                                let splitConfigData = data.config.properties["split-config"];
				if(!splitConfigData){
					splitConfigData = {
						name: 'split-action',
						outputStreams: [],
						__type: 'com.hortonworks.streamline.streams.layout.component.impl.splitjoin.SplitAction'
					};
				}
				splitConfigData.jarId = fileId;
				splitConfigData.splitterClassName = splitterClassName;

                                data.config.properties["split-config"] = splitConfigData;
                                data.config.properties.parallelism = parallelism;
                                data.name = name;
                                data.description = description;

                                return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {body: JSON.stringify(data)})
			})
	}

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {showError, changedFields} = this.state;
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
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Splitter Class*</label>
								<div className="col-sm-6">
									<input
										name="splitterClassName"
										value={this.state.splitterClassName}
										onChange={this.handleValueChange.bind(this)}
										type="text"
										className={editMode && showError && changedFields.indexOf("splitterClassName") !== -1 && this.state.splitterClassName === '' ? "form-control invalidInput" : "form-control"}
										required={true}
										disabled={!this.state.editMode}
									/>
								</div>
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

					</Tab>
				</Tabs>
			</div>
		)
	}
}