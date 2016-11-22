import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Panel, Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';

export default class StageFormNode extends Component{
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
                versionId: PropTypes.number.isRequired,
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
			maxCacheSize: '',
			showError: false,
			showErrorLabel: false,
			changedFields: []
		};
		this.state = obj;
	}

	fetchData() {
                let {topologyId, versionId, nodeType, nodeData} = this.props;
		let promiseArr = [
                        TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId)
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
                let {topologyId, versionId, currentEdges, nodeData} = this.props;
		let promiseArr = [], streamPromiseArr = [];
		let fields = [];
		currentEdges.map(edge=>{
			if(edge.target.nodeId === nodeData.nodeId){
                                promiseArr.push(TopologyREST.getNode(topologyId, versionId, 'edges', edge.edgeId));
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
                                        streamPromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'streams', id))
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
		let changedFields = this.state.changedFields;
		if(changedFields.indexOf("transform") === -1)
			changedFields.push("transform");
		if(obj){
			this.setState({transform: obj.value, changedFields: changedFields, showError: true,	showErrorLabel: false});
		} else {
			this.setState({transform: '', changedFields: changedFields, showError: true, showErrorLabel: false});
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

	handleFields(arr) {
		let fields = [];
		let changedFields = this.state.changedFields;
		if(changedFields.indexOf("transformFields") === -1)
			changedFields.push("transformFields");
		if(arr && arr.length){
			for(let f of arr){
				fields.push(f.value);
			}
			this.setState({transformFields: fields, changedFields: changedFields, showError: true, showError: false});
		} else {
			this.setState({transformFields: [], changedFields: changedFields, showError: true, showError: false});
		}
	}

	validateData(){
		let {transformFields, transform, entryExpirationInterval, entryRefreshInterval, maxCacheSize, changedFields} = this.state;
		let validateDataFlag = true;

		if(transform !== ''){
			if(transform === 'enrichment'){
				if(entryExpirationInterval === '' || entryRefreshInterval === '' || maxCacheSize === ''){
					validateDataFlag = false;
				}
				if(entryExpirationInterval === '' && changedFields.indexOf("entryExpirationInterval") === -1)
					changedFields.push("entryExpirationInterval");
				if(entryRefreshInterval === '' && changedFields.indexOf("entryRefreshInterval") === -1)
					changedFields.push("entryRefreshInterval");
				if(maxCacheSize === '' && changedFields.indexOf("maxCacheSize") === -1)
					changedFields.push("maxCacheSize");
			}
		} else if(transform === '')
			validateDataFlag = false;
		if(transformFields.length == 0)
			validateDataFlag = false;
		
		this.setState({showError: true, showErrorLabel: true, changedFields: changedFields});
		return validateDataFlag;
	}

	handleSave(name){
                let {topologyId, versionId, nodeType} = this.props;
		let {parallelism, transformFields, transform, entryExpirationInterval, entryRefreshInterval, maxCacheSize} = this.state;
		let nodeId = this.nodeData.id;
                return TopologyREST.getNode(topologyId, versionId, nodeType, nodeId)
			.then(data=>{
				let stageConfigData = data.entity.config.properties["stage-config"];
				if(!stageConfigData){
					stageConfigData = {
						name: "stage-action",
						outputStreams: [],
						__type: "org.apache.streamline.streams.layout.component.impl.splitjoin.StageAction"
					}
				}
				stageConfigData.transforms = [];
				let transformObj = {};
				if(transform === 'projection'){
					transformObj = {
						name: "projection-transform",
						__type: "org.apache.streamline.streams.layout.component.rule.action.transform.ProjectionTransform",
						projectionFields: transformFields
					};
				} else {
					transformObj = {
						name: "enrichment-transform",
						__type: "org.apache.streamline.streams.layout.component.rule.action.transform.EnrichmentTransform",
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
				data.entity.name = name;

                                return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {body: JSON.stringify(data.entity)});
			})
	}

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {transformTypesArr, transformFieldsArr, transformFields, transform, parallelism,
			entryExpirationInterval, entryRefreshInterval, maxCacheSize, showError, showErrorLabel, changedFields} = this.state;
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
											className={editMode && showError && changedFields.indexOf("entryExpirationInterval") !== -1 && entryExpirationInterval === '' ? "form-control invalidInput" : "form-control"}
										    disabled={!editMode}
										    min="0"
											inputMode="numeric"
										/>
									</div>
								</div>,
								<div key="2" className="form-group">
									<label className="col-sm-3 control-label">Entry Refresh Interval *</label>
									<div className="col-sm-6">
										<input
											name="entryRefreshInterval"
											value={entryRefreshInterval}
											onChange={this.handleValueChange.bind(this)}
											type="number"
											className={editMode && showError && changedFields.indexOf("entryRefreshInterval") !== -1 && entryRefreshInterval === '' ? "form-control invalidInput" : "form-control"}
										    disabled={!editMode}
										    min="0"
											inputMode="numeric"
										/>
									</div>
								</div>,
								<div key="3" className="form-group">
									<label className="col-sm-3 control-label">Max Cache Size *</label>
									<div className="col-sm-6">
										<input
											name="maxCacheSize"
											value={maxCacheSize}
											onChange={this.handleValueChange.bind(this)}
											type="number"
											className={editMode && showError && changedFields.indexOf("maxCacheSize") !== -1 && maxCacheSize === '' ? "form-control invalidInput" : "form-control"}
										    disabled={!editMode}
										    min="0"
											inputMode="numeric"
										/>
									</div>
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

					</Tab>
				</Tabs>
			</div>
		)
	}
}
