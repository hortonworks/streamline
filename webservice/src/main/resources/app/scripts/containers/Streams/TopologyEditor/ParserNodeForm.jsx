import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import DeviceREST from '../../../rest/DeviceREST';
import ParserREST from '../../../rest/ParserREST';
import OutputSchema from '../../../components/OutputSchemaComponent';

export default class ParserNodeForm extends Component {
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
		this.fetchDevice();

		var obj = {
			dataSourceId: '',
			dataSourceName: '',
			parserId: '',
			parserName: '',
			dataSourceArr: [],
			parserArr: [],
			parallelism: 1,
			editMode: editMode,
			showSchema: false
		};
		this.state = obj;
	}

	fetchDevice(){
		let {topologyId, nodeType, nodeData, sourceNode} = this.props;
		TopologyREST.getNode(topologyId, 'sources', sourceNode.nodeId)
		.then((device)=>{
			let sourceId = device.entity.config.properties.dataSourceId;
			if(sourceId){
				this.fetchData(sourceId);
			}
		})
		.catch((err)=> {
			console.error(err);
		});
	}

	fetchData(deviceId) {
		let {topologyId, nodeType, nodeData} = this.props;
		let promiseArr = [
			DeviceREST.getDevice(deviceId),
			TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId)
		];

		Promise.all(promiseArr)
			.then((results)=>{
				let dsId = results[0].entity.dataSourceId,
					dsName = results[0].entity.dataSourceName,
					pId = results[0].entity.parserId,
					pName = results[0].entity.parserName;

				let dataSourceArr = [
					{id: 0, value: 'None', label:'None'},
					{id: dsId, value: dsName, label: dsName}
				],
				 parserArr = [
					{id: 0, value: 'None', label:'None'},
					{id: pId, value: pName, label: pName}
				];

				this.nodeData = results[1].entity;
				let configFields = results[1].entity.config.properties;
				let {dataSourceId, parserId, parallelism} = configFields;

				let stateObj = {
					dataSourceId: dataSourceId ? dsId : 0,
					dataSourceName: dataSourceId ? dsName : 'None',
					parserId: parserId ? pId: 0,
					parserName: parserId ? pName: 'None',
					dataSourceArr: dataSourceArr,
					parserArr: parserArr,
					parallelism: parallelism ? parallelism : 1,
				};

				if(this.nodeData.outputStreams.length === 0){
					this.saveStreams(pId);
				} else {
					stateObj.showSchema = true;
				}

				this.setState(stateObj);
			})
			.catch((err)=>{
				console.error(err);
			})
	}

	saveStreams(id){
		let self = this;
		let {topologyId, nodeType} = this.props;
		let passStreamData = { streamId: 'parsedTuplesStream', fields: [] };
		let failedStreamData = { streamId: 'failedTuplesStream', fields: [] };
		ParserREST.getParser(id)
			.then(schema=>{
				schema.entity.parserSchema.fields.map((obj)=>{
					passStreamData.fields.push({
						name: obj.name,
						type: obj.type
					});
				});
				Promise.all([TopologyREST.createNode(topologyId, 'streams', {body: JSON.stringify(passStreamData)}),
					TopologyREST.createNode(topologyId, 'streams', {body: JSON.stringify(failedStreamData)})])
					.then(results=>{
						self.nodeData.outputStreamIds = [];
						results.map(result=>{
							self.nodeData.outputStreamIds.push(result.entity.id);
						})
						TopologyREST.updateNode(topologyId, nodeType, self.nodeData.id, {body: JSON.stringify(this.nodeData)})
							.then((node)=>{
								self.nodeData = node.entity; 
								self.setState({showSchema: true});
							})
					})
			})
	}

	handleDeviceChange(obj) {
		if(obj){
			this.setState({
				dataSourceId: obj.id,
				dataSourceName: obj.value
			});
		} else {
			this.setState({
				dataSourceId: '',
				dataSourceName: ''
			});
		}
	}

	handleParserChange(obj) {
		if(obj){
			this.setState({
				parserId: obj.id,
				parserName: obj.value
			});
		} else {
			this.setState({
				parserId: '',
				parserName: ''
			});
		}
	}

	handleValueChange(e) {
		let obj = {};
		let value = e.target.value;
		obj[e.target.name] = (value === '' ? '' : parseInt(value, 10));
		this.setState(obj);
	}

	getData() {
		let obj = {
			parallelism: this.state.parallelism,
			parsedTuplesStream: "parsedTuplesStream",
            failedTuplesStream: "failedTuplesStream"
		};
		if(this.state.dataSourceId !== 0)
			obj['dataSourceId'] = this.state.dataSourceId;
		if(this.state.parserId !== 0)
			obj['parserId'] = this.state.parserId;
		return obj;
	}

	validateData(){
		let {dataSourceId, parserId, parallelism } = this.state;
		let validDataFlag = true;

		if(dataSourceId === '' || parserId === '' || parallelism === '') validDataFlag = false;
		return validDataFlag;
	}

	handleSave(name){
		let {topologyId, nodeType} = this.props;
		let data = this.getData();
		let nodeId = this.nodeData.id;
		this.nodeData.config.properties = data;
		this.nodeData.name = name;

		return TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)})
	}

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {showSchema} = this.state;
		return (
			<div>
				<Tabs id="parserForm" defaultActiveKey={1} className="schema-tabs">
					<Tab eventKey={1} title="Configuration">
						<form className="form-horizontal">
							<div className="form-group">
								<label className="col-sm-3 control-label">Source Name*</label>
								<div className="col-sm-6">
									<Select
										value={this.state.dataSourceName}
										options={this.state.dataSourceArr}
										onChange={this.handleDeviceChange.bind(this)}
										required={true}
										disabled={!this.state.editMode}
									/>
								</div>
								{this.state.editMode && this.state.dataSourceName === '' ?
										<div className="col-sm-3">
											<p className="form-control-static error-note">Source name cannot be blank.</p>
										</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Parser Name*</label>
								<div className="col-sm-6">
									<Select
										value={this.state.parserName}
										options={this.state.parserArr}
										onChange={this.handleParserChange.bind(this)}
										required={true}
										disabled={!this.state.editMode}
									/>
								</div>
								{this.state.editMode && this.state.parserName === '' ?
										<div className="col-sm-3">
											<p className="form-control-static error-note">Parser name cannot be blank.</p>
										</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Parallelism*</label>
								<div className="col-sm-6">
									<input
										name="parallelism"
										value={this.state.parallelism}
										onChange={this.handleValueChange.bind(this)}
										type="number"
										className="form-control"
									    required={true}
									    disabled={!this.state.editMode}
									    min="0"
									    inputMode="numeric"
									/>
								</div>
								{this.state.editMode && this.state.parallelism === '' ?
									<div className="col-sm-3">
										<p className="form-control-static error-note">Parallelism cannot be blank.</p>
									</div>
								: null}
							</div>
						</form>
					</Tab>
					<Tab eventKey={2} title="Output Streams">
						{showSchema ? 
							<OutputSchema 
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