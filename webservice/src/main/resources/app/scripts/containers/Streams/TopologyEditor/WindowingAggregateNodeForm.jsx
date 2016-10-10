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
                        outputFieldsArr: [{args: '', functionName: '', outputFieldName: ''}],
                        functionListArr: [],
                        outputStreamId: '',
                        outputStreamFields: []
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
                                                let configFields = this.nodeData.config.properties;
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
                                                        keysList: JSON.parse(JSON.stringify(fields)),
                                                        parallelism: configFields.parallelism || 1,
                                                        functionListArr: udfList
                                                }
                                                //Find output streams and set appropriate fields
                                                //else create streams with blank values
                                                if(this.nodeData.outputStreams && this.nodeData.outputStreams.length > 0){
                                                        this.streamData = this.nodeData.outputStreams[0];
                                                        stateObj.outputStreamId = this.nodeData.outputStreams[0].streamId;
                                                        stateObj.outputStreamFields = JSON.parse(JSON.stringify(this.nodeData.outputStreams[0].fields));
                                                } else {
                                                        stateObj.outputStreamId = 'window_stream_'+this.nodeData.id;
                                                        stateObj.outputStreamFields = [];
                                                        let dummyStreamObj = {
                                                                streamId: stateObj.outputStreamId,
                                                                fields: stateObj.outputStreamFields
                                                        }
                                                        TopologyREST.createNode(topologyId, 'streams', {body: JSON.stringify(dummyStreamObj)})
                                                                .then(streamResult => {
                                                                        this.streamData = streamResult.entity;
                                                                        this.nodeData.outputStreamIds = [this.streamData.id];
                                                                        TopologyREST.updateNode(topologyId, nodeType, nodeData.nodeId, {body: JSON.stringify(this.nodeData)})
                                                                })
                                                }
                                                if(this.windowId){
                                                        TopologyREST.getNode(topologyId, 'windows', this.windowId)
                                                                .then((windowResult)=>{
                                                                        let windowData = windowResult.entity;
                                                                        if(windowData.projections.length === 0){
                                                                                stateObj.outputFieldsArr = [{args:'', functionName: '', outputFieldName: ''}];
                                                                        } else {
                                                                                stateObj.outputFieldsArr = [];
                                                                                windowData.projections.map(o=>{
                                                                                        if(o.expr){
                                                                                                if(windowData.groupbykeys.indexOf(o.expr) !== -1){
                                                                                                        delete o.expr
                                                                                                } else {
                                                                                                        o.args = o.expr;
                                                                                                        delete o.expr;
                                                                                                        stateObj.outputFieldsArr.push(o);
                                                                                                }
                                                                                        } else {
                                                                                                o.args = o.args[0];
                                                                                                stateObj.outputFieldsArr.push(o);
                                                                                        }
                                                                                })

                                                                        }
                                                                        stateObj.selectedKeys = windowData.groupbykeys;
                                                                        this.windowAction = windowData.actions;
                                                                        if(windowData.window){
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
                                                                        }
                                                                        this.setState(stateObj);
                                                                })
                                                } else {
                                                        //Creating window object so output streams can get it
                                                        let dummyWindowObj = {
                                                                name: 'window_auto_generated',
                                                                description: 'window description auto generated',
                                                                projections:[],
                                                                streams: [],
                                                                actions: [],
                                                                groupbykeys:[]
                                                        }
                                                        TopologyREST.createNode(topologyId, 'windows', {body: JSON.stringify(dummyWindowObj)})
                                                                .then((windowResult)=>{
                                                                        this.windowId = windowResult.entity.id;
                                                                        this.nodeData.config.properties.parallelism = 1;
                                                                        this.nodeData.config.properties.rules = [this.windowId];
                                                                        TopologyREST.updateNode(topologyId, nodeType, nodeData.nodeId, {body: JSON.stringify(this.nodeData)});
                                                                        this.setState(stateObj);
                                                                })
                                                }
                                        })
                        })

        }

        handleKeysChange(arr){
                let {selectedKeys, outputStreamFields} = this.state;
                let tempArr = [];
                outputStreamFields.map(field=>{
                        if(selectedKeys.indexOf(field.name) === -1){
                                tempArr.push(field);
                        }
                })
                tempArr.push(...arr);
                this.streamData.fields = tempArr;
                let keys = [];
                if(arr && arr.length){
                        for(let k of arr){
                                keys.push(k.name);
                        }
                        this.setState({selectedKeys: keys, outputStreamFields: tempArr});
                } else {
                        this.setState({selectedKeys: [], outputStreamFields: tempArr});
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
                let oldData = JSON.parse(JSON.stringify(fieldsArr[index]));
                if(name === 'outputFieldName'){
                        fieldsArr[index][name] = this.refs.outputFieldName.value;
                } else {
                        if(obj){
                                fieldsArr[index][name] = obj.name;
                        } else {
                                fieldsArr[index][name] = '';
                        }
                        if(fieldsArr[index].args !== ''){
                                fieldsArr[index].outputFieldName = fieldsArr[index].args+( fieldsArr[index].functionName !== '' ? '_'+fieldsArr[index].functionName : '');
                        }
                }
                let outputStreamFields = this.getOutputFieldsForStream(oldData, fieldsArr[index]);
                this.streamData.fields = outputStreamFields;
                this.setState({outputFieldsArr: fieldsArr, outputStreamFields: outputStreamFields});
        }
        getOutputFieldsForStream(oldObj, newDataObj){
                let streamsArr = JSON.parse(JSON.stringify(this.state.outputStreamFields));
                let obj = null;
                if(oldObj.outputFieldName !== ''){
                        obj = streamsArr.filter((field)=>{return field.name === oldObj.outputFieldName;})[0];
                } else {
                        obj = streamsArr.filter((field)=>{return field.name === oldObj.args;})[0];
                }
                if(obj){
                        let fieldObj = this.state.keysList.find((field)=>{return field.name == newDataObj.args});
                        if(newDataObj.functionName !== ''){
                                if(newDataObj.functionName === 'MIN' || newDataObj.functionName === 'MAX'){
                                        obj.type = fieldObj ? fieldObj.type : 'DOUBLE';
                                } else {
                                        obj.type = 'DOUBLE';
                                }
                                obj.name = newDataObj.outputFieldName;
                                //TODO - set type
                        } else if(oldObj.functionName !== ''){
                                obj.name = newDataObj.outputFieldName;
                                if(oldObj.functionName === 'MIN' || oldObj.functionName === 'MAX'){
                                        obj.type = fieldObj ? fieldObj.type : 'DOUBLE';
                                } else {
                                        obj.type = 'DOUBLE';
                                }
                        }
                } else {
                        let o = streamsArr.filter((field)=>{return field.name === newDataObj.outputFieldName;});
                        if(o.length === 0){
                                let fieldObj = this.state.keysList.find((field)=>{return field.name == newDataObj.args});
                                streamsArr.push({
                                        name: newDataObj.outputFieldName,
                                        type: fieldObj ? fieldObj.type : 'DOUBLE',
                                        optional: false
                                })
                        }
                }
                return streamsArr;
        }
        addOutputFields(){
                if(this.state.editMode){
                        let fieldsArr = this.state.outputFieldsArr;
                        fieldsArr.push({args: '', functionName: '', outputFieldName: ''});
                        this.setState({outputFieldsArr: fieldsArr});
                }
        }
        deleteFieldRow(index){
                if(this.state.editMode){
                        let fieldsArr = this.state.outputFieldsArr;
                        let outputStreamFields = this.state.outputStreamFields;
                        let o = fieldsArr[index];
                        if(o.outputFieldName !== ''){
                                let streamObj = outputStreamFields.filter((field)=>{return field.name === o.outputFieldName;})[0];
                                if(streamObj){
                                        let streamObjIndex = outputStreamFields.indexOf(streamObj);
                                        if(streamObjIndex !== -1){
                                                outputStreamFields.splice(streamObjIndex, 1);
                                        }
                                }
                        }
                        fieldsArr.splice(index, 1);
                        this.streamData.fields = outputStreamFields;
                        this.setState({outputFieldsArr: fieldsArr, outputStreamFields: outputStreamFields});
                }
        }

	validateData(){
		let {selectedKeys, windowNum, outputFieldsArr} = this.state;
		let validData = true;
		if(selectedKeys.length === 0 || windowNum === ''){
                        validData = false;
                }
                outputFieldsArr.map((obj)=>{
                        if(obj.args === '' || obj.outputFieldName === ''){
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
                        let o = {};
                        if(!obj.functionName || obj.functionName === ''){
                                o.expr = obj.args;
                        } else {
                                o.args=[obj.args];
                                o.functionName=obj.functionName;
                        }
                        o.outputFieldName = obj.outputFieldName;
                        windowObj.projections.push(o);
                })
                selectedKeys.map((field)=>{
                        let o = {
                                expr: field
                        };
                        if(windowObj.projections.indexOf(o) === -1){
                                windowObj.projections.push(o);
                        }
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
                }
        }
        updateNode(windowObj){
                let {parallelism, outputStreamFields} = this.state;
                let {topologyId, nodeType, nodeData} = this.props;
                return TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId)
                                .then(result=>{
					let data = result.entity;
					if(windowObj && windowObj.responseCode === 1000){
                                                let windowData = windowObj.entity;
                                                data.config.properties.parallelism = parallelism;
                                                data.config.properties.rules = [windowData.id];
                                                data.outputStreamIds = [this.streamData.id];
                                                let streamData = {
                                                        streamId: this.streamData.streamId,
                                                        fields: outputStreamFields
                                                }
                                                let promiseArr = [
                                                        TopologyREST.updateNode(topologyId, nodeType, nodeData.nodeId, {body: JSON.stringify(data)}),
                                                        TopologyREST.updateNode(topologyId, 'streams', this.streamData.id, {body: JSON.stringify(streamData)})
                                                ]
                                                return Promise.all(promiseArr);
                                        } else {
                                                FSReactToastr.error(<strong>{windowObj.responseMessage}</strong>);
                                        }
                                })
        }

        render() {
                let {parallelism, selectedKeys, keysList, editMode, intervalType, intervalTypeArr, windowNum, slidingNum,
                        durationType, slidingDurationType, durationTypeArr, outputFieldsArr, functionListArr, outputStreamId, outputStreamFields } = this.state;
                let {topologyId, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
                return (
                        <Tabs id="WindowForm" defaultActiveKey={1} className="schema-tabs">
                                <Tab eventKey={1} title="Configuration">
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
                                                                                                value={obj.args}
                                                                                                options={keysList}
                                                                                                onChange={this.handleFieldChange.bind(this, 'args', i)}
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
                            canAdd={false}
                            canDelete={false}
                            windowOutputStreams={this.streamData}
                                                />
                                        </Tab>
                        </Tabs>
                )
        }
}