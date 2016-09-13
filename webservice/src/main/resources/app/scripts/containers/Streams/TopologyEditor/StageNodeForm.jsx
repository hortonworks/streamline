import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Panel, Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import OutputSchema from '../../../components/OutputSchemaComponent'

export default class StageFormNode extends Component{
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired,
		currentEdges: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
		let {configData, editMode} = props;
		this.fetchData();
		this.fetchFieldsFromSource();
		if(typeof configData.config === 'string'){
			configData.config = JSON.parse(configData.config)
		}
		let config = configData.config;
		let parallelismObj = config.filter((f)=>{ return f.name === 'parallelism'});
		if(parallelismObj.length){
			parallelismObj = parallelismObj[0]
		}
		let obj = {
			transformTypesArr:[
				{value: "enrichment", label: "Enrichment"},
				{value: "projection", label: "Projection"}
			],
			transformFieldsArr: [],
			transformFields: [],
			transform: '',
			parallelism: 1,
			entryExpirationInterval: '',
			entryRefreshInterval: '',
			maxCacheSize: ''
		};
		this.state = obj;
	}

	fetchData() {
		let {topologyId, nodeType, nodeData} = this.props;
		let promiseArr = [
			TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId)
		];
		let stateObj = {};

		Promise.all(promiseArr)
			.then((results)=>{
				this.nodeData = results[0].entity;
				let {parallelism} = results[0].entity.config.properties;
				stateObj.parallelism = parallelism || 1;

				let config = this.nodeData.config.properties['stage-config'];
				if(config && config.transforms){
					let transformObj = config.transforms[0];
					if(transformObj){
						if(transformObj.name === 'projection-transform'){
							stateObj.transform = 'projection';
							stateObj.transformFields = transformObj.projectionFields;
						} else if(transformObj.name === 'enrichment-transform'){
							stateObj.transform = 'enrichment';
							stateObj.transformFields = transformObj.fieldsToBeEnriched;
							stateObj.entryExpirationInterval = transformObj.entryExpirationInterval;
							stateObj.entryRefreshInterval = transformObj.entryRefreshInterval;
							stateObj.maxCacheSize = transformObj.maxCacheSize;
						}
					}
				}

				this.setState(stateObj);
			})
	}

	fetchFieldsFromSource(){
		let {topologyId, currentEdges, nodeData} = this.props;
		let promiseArr = [], streamPromiseArr = [];
		let fields = [];
		currentEdges.map(edge=>{
			if(edge.target.nodeId === nodeData.nodeId){
				promiseArr.push(TopologyREST.getNode(topologyId, 'edges', edge.edgeId));
			}
		})
		Promise.all(promiseArr)
			.then(results=>{
				let streamIds = [];
				results.map(result=>{
					let {streamGroupings} = result.entity;
					if(streamGroupings){
						streamGroupings.map(stream=>{
							if(streamIds.indexOf(stream.streamId) === -1){
								streamIds.push(stream.streamId)
							}
						})
					}
				})
				streamIds.map(id=>{
					streamPromiseArr.push(TopologyREST.getNode(topologyId, 'streams', id))
				})
				Promise.all(streamPromiseArr)
					.then(streamResults=>{
						streamResults.map(stream=>{
							if(stream.entity.fields){
								stream.entity.fields.map(fieldObj=>{
									if(fields.indexOf(fieldObj.name) === -1){
										fields.push({value: fieldObj.name, label: fieldObj.name});
									}
								})
							}
						})
						this.setState({transformFieldsArr: fields});
					})
			})
	}

	handleTransformChange(obj) {
		if(obj){
			this.setState({transform: obj.value});
		} else {
			this.setState({transform: ''});
		}
	}

	handleValueChange(e) {
		let obj = {};
		obj[e.target.name] = e.target.value === '' ? '' : e.target.type === "number" ? parseInt(e.target.value, 10) : e.target.value;
		this.setState(obj);
	}

	handleFields(arr) {
		let fields = [];
		if(arr && arr.length){
			for(let f of arr){
				fields.push(f.value);
			}
			this.setState({transformFields: fields});
		} else {
			this.setState({transformFields: []});
		}
	}

	validateData(){
		let {transformFields, transform, entryExpirationInterval, entryRefreshInterval, maxCacheSize} = this.state;
		if(transform !== '' && transformFields.length > 0){
			if(transform === 'enrichment'){
				if(entryExpirationInterval === '' || entryRefreshInterval === '' || maxCacheSize === ''){
					return false;
				}
			}
			return true;
		}
		return false
	}

	handleSave(){
		let {topologyId, nodeType} = this.props;
		let {parallelism, transformFields, transform, entryExpirationInterval, entryRefreshInterval, maxCacheSize} = this.state;
		let nodeId = this.nodeData.id;
		return TopologyREST.getNode(topologyId, nodeType, nodeId)
			.then(data=>{
				let stageConfigData = data.entity.config.properties["stage-config"];
				if(!stageConfigData){
					stageConfigData = {
						name: "stage-action",
						outputStreams: [],
						__type: "com.hortonworks.iotas.streams.layout.component.impl.splitjoin.StageAction"
					}
				}
				stageConfigData.transforms = [];
				let transformObj = {};
				if(transform === 'projection'){
					transformObj = {
						name: "projection-transform",
						__type: "com.hortonworks.iotas.streams.layout.component.rule.action.transform.ProjectionTransform",
						projectionFields: transformFields
					};
				} else {
					transformObj = {
						name: "enrichment-transform",
						__type: "com.hortonworks.iotas.streams.layout.component.rule.action.transform.EnrichmentTransform",
						fieldsToBeEnriched: transformFields,
						entryExpirationInterval: entryExpirationInterval,
						entryRefreshInterval: entryRefreshInterval,
						maxCacheSize: maxCacheSize,
						transformDataProvider: {}
					};
				}
				stageConfigData.transforms.push(transformObj);

				data.entity.config.properties["stage-config"] = stageConfigData;
				data.entity.config.properties.parallelism = parallelism;

				return TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(data.entity)});
			})
	}

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {transformTypesArr, transformFieldsArr, transformFields, transform, parallelism,
			entryExpirationInterval, entryRefreshInterval, maxCacheSize} = this.state;
		return (
			<div>
				<Tabs id="stageForm" defaultActiveKey={1} className="schema-tabs">
					<Tab eventKey={1} title="Configuration">
						<form className="form-horizontal">
							<div className="form-group">
								<label className="col-sm-3 control-label">Transform*</label>
								<div className="col-sm-6">
									<Select
										value={transform}
										options={transformTypesArr}
										onChange={this.handleTransformChange.bind(this)}
										required={true}
										searchable={false}
										disabled={!editMode}
									/>
								</div>
								{editMode && transform === '' ?
										<div className="col-sm-3">
											<p className="form-control-static error-note">Select a transform.</p>
										</div>
								: null}
							</div>
							<div className="form-group">
								<label className="col-sm-3 control-label">Fields*</label>
								<div className="col-sm-6">
									<Select
										value={transformFields}
										options={transformFieldsArr}
										onChange={this.handleFields.bind(this)}
										required={true}
										multi={true}
										clearable={false}
										joinValues={true}
										disabled={!editMode}
									/>
								</div>
								{editMode && transformFields.length === 0 ?
									<div className="col-sm-3">
										<p className="form-control-static error-note">Fields cannot be blank.</p>
									</div>
								: null}
							</div>
							{transform === 'enrichment' ?
								[<div key="1" className="form-group">
									<label className="col-sm-3 control-label">Entry Expiration Interval *</label>
									<div className="col-sm-6">
										<input
											name="entryExpirationInterval"
											value={entryExpirationInterval}
											onChange={this.handleValueChange.bind(this)}
											type="number"
											className="form-control"
										    disabled={!editMode}
										    min="0"
											inputMode="numeric"
										/>
									</div>
									{editMode && entryExpirationInterval === '' ?
										<div className="col-sm-3">
											<p className="form-control-static error-note">Expiration interval cannot be blank.</p>
										</div>
									: null}
								</div>,
								<div key="2" className="form-group">
									<label className="col-sm-3 control-label">Entry Refresh Interval *</label>
									<div className="col-sm-6">
										<input
											name="entryRefreshInterval"
											value={entryRefreshInterval}
											onChange={this.handleValueChange.bind(this)}
											type="number"
											className="form-control"
										    disabled={!editMode}
										    min="0"
											inputMode="numeric"
										/>
									</div>
									{editMode && entryRefreshInterval === '' ?
										<div className="col-sm-3">
											<p className="form-control-static error-note">Refresh interval cannot be blank.</p>
										</div>
									: null}
								</div>,
								<div key="3" className="form-group">
									<label className="col-sm-3 control-label">Max Cache Size *</label>
									<div className="col-sm-6">
										<input
											name="maxCacheSize"
											value={maxCacheSize}
											onChange={this.handleValueChange.bind(this)}
											type="number"
											className="form-control"
										    disabled={!editMode}
										    min="0"
											inputMode="numeric"
										/>
									</div>
									{editMode && maxCacheSize === '' ?
										<div className="col-sm-3">
											<p className="form-control-static error-note">Max cache size cannot be blank.</p>
										</div>
									: null}
								</div>]
							: null}
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
