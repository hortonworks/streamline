import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import Utils from '../../../utils/Utils';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import { Scrollbars } from 'react-custom-scrollbars';

export default class JoinNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
        versionId: PropTypes.number.isRequired,
                sourceNode: PropTypes.array.isRequired,
		targetNodes: PropTypes.array.isRequired,
                linkShuffleOptions: PropTypes.array.isRequired,
                currentEdges: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
                let {editMode, sourceNode} = props;
		this.fetchData();

                this.sourceNodesId = [];

                sourceNode.map((n)=>{
                        this.sourceNodesId.push(n.nodeId);
                });

                let configDataFields = props.configData.topologyComponentUISpecification.fields;
                let joinOptions = _.find(configDataFields, {fieldName: 'jointype'}).options;
                let joinTypes = [];
                joinOptions.map((o)=>{
                        joinTypes.push({value: o, label: o});
                });

		var obj = {
			parallelism: 1,
			editMode: editMode,
                        fieldList: [],
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
                        outputKeys: [],
                        outputStreamFields: [],
                        joinFromStreamName: '',
                        joinFromStreamKey: '',
                        joinFromStreamKeys: [],
                        joinTypes: joinTypes,
                        joinStreams: [],
                        inputStreamsArr: []
		};
		this.state = obj;
	}

	fetchData() {
        let {topologyId, versionId, nodeType, nodeData, currentEdges} = this.props;
                let edgePromiseArr = [];
                let promiseArr = [TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId)];
                currentEdges.map(edge=>{
                        if(edge.target.nodeId === nodeData.nodeId){
                                edgePromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'edges', edge.edgeId));
                        }
                })

                Promise.all(edgePromiseArr)
                        .then(edgeResults=>{
                                edgeResults.map((edge)=>{
                                        if(edge.toId === nodeData.nodeId && this.sourceNodesId.indexOf(edge.fromId) !== -1){
                                                promiseArr.push(TopologyREST.getNode(topologyId, versionId, 'streams', edge.streamGroupings[0].streamId));
                                        }
                                })
                                Promise.all(promiseArr)
                                        .then((results)=>{
                                                this.nodeData = results[0];
                                                let configFields = this.nodeData.config.properties;
                                                let inputStreams = [], joinStreams = [];
                                                let fields = [];
                        results.map((result, i)=>{
				if(i > 0) {
                                        inputStreams.push(result);
                                result.fields.map((f)=>{
						if(fields.indexOf(f) === -1)
						fields.push(f);
					});
				}
                        });
                        //create rows for joins
                        inputStreams.map((s, i)=>{
				if(i > 0) {
					joinStreams.push({
						id: (i - 1),
						type: '',
						stream: '',
						key: '',
						with: '',
						streamOptions: [],
						keyOptions: [],
						withOptions: []
					});
				}
                        });

                        let stateObj = {
                                                        fieldList: fields,
                                                        parallelism: configFields.parallelism || 1,
                                                        outputKeys: configFields.outputKeys,
                                                        inputStreamsArr: inputStreams,
                                                        joinStreams: joinStreams
                                                }
                                        //set value for first row
                                                if(configFields.from) {
                                                        let fromObject = configFields.from;
                                                        stateObj.joinFromStreamName = fromObject.stream ? fromObject.stream : '';
                                                        stateObj.joinFromStreamKey = fromObject.key ? fromObject.key : '';
                                                        let selectedStream = _.find(inputStreams, {streamId: fromObject.stream});
                                                        if(selectedStream)
                                                                stateObj.joinFromStreamKeys = selectedStream.fields;
                                                        if(fromObject.stream) {
                                                                let joinStreamOptions = inputStreams.filter((s)=>{return s.streamId !== fromObject.stream});
                                                                let obj = inputStreams.find((s)=>{return s.streamId === fromObject.stream});
                                                                if(joinStreams.length) {
                                                                    let joinStream = inputStreams.find((s)=>{return s.streamId === configFields.joins[0].stream});
                                                                    joinStreams[0].streamOptions = joinStreamOptions;
                                                                    joinStreams[0].withOptions = [obj];
                                                                    joinStreams[0].keyOptions = joinStream.fields;
                                                                }
                                                        }
                                                }
                                        //set values of joins if saved
                                        if(configFields.joins) {
                        configFields.joins.map((o, id)=>{
				joinStreams[id].type = o.type;
				joinStreams[id].stream = o.stream;
				joinStreams[id].key = o.key;
				joinStreams[id].with = o.with;
				let streamObj = inputStreams.find((s)=>{return s.streamId !== o.stream});
				let selectedStream = _.find(inputStreams, {streamId: o.stream});
				let streamOptions = [];
                                                        let withOptions = [];
                                                        withOptions.push(streamObj);

                                                        configFields.joins.map((s, i)=>{
                                                                if(i <= id) {
                                                                        let obj = inputStreams.find((stream)=>{return stream.streamId === s.stream;});
                                                                        withOptions.push(obj);
                                                                }
                                                        });
                                                        streamOptions = _.difference(inputStreams, [...withOptions]);
                                                        let nextStream = joinStreams[id + 1];
                                                        if(nextStream) {
                                                                nextStream.streamOptions = streamOptions;
                                                                nextStream.withOptions = withOptions;
                                                                nextStream.keyOptions = selectedStream.fields;
                                                        }
                        });
                    }
                        if(configFields.window){
                                                        if(configFields.window.windowLength.class === '.Window$Duration'){
                                                                stateObj.intervalType = '.Window$Duration';
                                                                let obj = Utils.millisecondsToNumber(configFields.window.windowLength.durationMs);
                                                                stateObj.windowNum = obj.number;
                                                                stateObj.durationType = obj.type;
                                                                if(configFields.window.slidingInterval){
                                                                        let obj = Utils.millisecondsToNumber(configFields.window.slidingInterval.durationMs);
                                                                        stateObj.slidingNum = obj.number;
                                                                        stateObj.slidingDurationType = obj.type;
                                                                }
                                                        } else if(configFields.window.windowLength.class === '.Window$Count'){
                                                                stateObj.intervalType = '.Window$Count';
                                                                stateObj.windowNum = configFields.window.windowLength.count;
                                                                if(configFields.window.slidingInterval){
                                                                        stateObj.slidingNum = configFields.window.slidingInterval.count;
                                                                }
                                                        }
                                                }
                                                if(this.nodeData.outputStreams && this.nodeData.outputStreams.length > 0){
                                                    this.streamData = this.nodeData.outputStreams[0];
                                                    stateObj.outputStreamId = this.nodeData.outputStreams[0].streamId;
                                                    stateObj.outputStreamFields = JSON.parse(JSON.stringify(this.nodeData.outputStreams[0].fields));
                                                    this.context.ParentForm.setState({outputStreamObj:this.streamData})
                                                } else {
                                                    stateObj.outputStreamId = 'join_processor_stream_'+this.nodeData.id;
                                                    stateObj.outputStreamFields = [];
                                                    this.streamData = { streamId: stateObj.outputStreamId, fields: stateObj.outputStreamFields};
                                                    this.context.ParentForm.setState({outputStreamObj:this.streamData});
                                                }
                                                this.setState(stateObj);
                    })
			})
	}

        handleFieldsChange(arr) {
                let {outputKeys, outputStreamFields} = this.state;
                let tempArr = [];
                outputStreamFields.map(field=>{
                        if(outputKeys.indexOf(field.name) === -1){
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
                        this.setState({outputKeys: keys, outputStreamFields: tempArr});
                } else {
                        this.setState({outputKeys: [], outputStreamFields: tempArr});
                }
        this.context.ParentForm.setState({outputStreamObj:this.streamData})
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

        handleJoinFromStreamChange(obj) {
                if(obj) {
                        let {inputStreamsArr, joinStreams} = this.state;
                        let joinStreamOptions = inputStreamsArr.filter((s)=>{return s.streamId !== obj.streamId});
                        if(joinStreams.length) {
                                joinStreams.map((s)=>{
                                        s.stream = '';
                                        s.key = '';
                                        s.with = '';
                                        s.streamOptions = [];
                                        s.keyOptions = [];
                                        s.withOptions = [];
                                });
                                joinStreams[0].streamOptions = joinStreamOptions;
                                joinStreams[0].withOptions = [obj];
                                if(joinStreams.length === 1) {
                                    joinStreams[0].stream = joinStreamOptions[0].streamId;
                                    joinStreams[0].with = obj.streamId;
                                    joinStreams[0].keyOptions = joinStreamOptions[0].fields;
                                }
                        }
                        this.setState({joinFromStreamName: obj.streamId, joinFromStreamKeys: obj.fields, joinFromStreamKey: '', joinStreams: joinStreams});
                } else {
                        this.setState({joinFromStreamName: '', joinFromStreamKeys: [], joinFromStreamKey: ''});
                }
        }
        handleJoinFromKeyChange(obj) {
                if(obj) {
                        this.setState({joinFromStreamKey: obj.name});
                } else {
                        this.setState({joinFromStreamKey: ''});
                }
        }
        handleJoinTypeChange(key, obj){
                let {joinStreams} = this.state;
                if(obj) {
                        joinStreams[key].type = obj.value;
                        this.setState({joinStreams: joinStreams});
                } else {
                        joinStreams[key].type = '';
                        this.setState({joinStreams: joinStreams});
                }
        }
        handleJoinStreamChange(key, obj){
                let {inputStreamsArr, joinStreams, joinFromStreamName} = this.state;
                if(obj) {
                        joinStreams[key].stream = obj.streamId;
                        joinStreams[key].keyOptions = obj.fields;
                        let streamOptions = [];
                        let withOptions = [];
                        let streamObj = inputStreamsArr.find((stream)=>{return stream.streamId === joinFromStreamName;});
                        withOptions.push(streamObj);
                        joinStreams.map((s, i)=>{
                                if(i <= key) {
                                        let obj = inputStreamsArr.find((stream)=>{return stream.streamId === s.stream;});
                                        withOptions.push(obj);
                                }
                                if(i > key) {
                                        s.stream = '';
                                        s.key = '';
                                        s.with = '';
                                        s.streamOptions = [];
                                        s.keyOptions = [];
                                        s.withOptions = [];
                                }
                        });
                        streamOptions = _.difference(inputStreamsArr, [...withOptions]);
                        let nextStream = joinStreams[key + 1];
                        if(nextStream) {
                                nextStream.streamOptions = streamOptions;
                                nextStream.withOptions = withOptions;
                                nextStream.key = '';
                                nextStream.keyOptions = [];
                        }
                        this.setState({joinStreams: joinStreams});
                } else {
                        joinStreams[key].stream = '';
                        joinStreams[key].keyOptions = [];
                        this.setState({joinStreams: joinStreams});
                }
        }
        handleJoinKeyChange(key, obj){
                let {joinStreams} = this.state;
                if(obj) {
                        joinStreams[key].key = obj.name;
                        this.setState({joinStreams: joinStreams});
                } else {
                        joinStreams[key].key = '';
                        this.setState({joinStreams: joinStreams});
                }
        }
        handleJoinWithChange(key, obj){
                let {joinStreams} = this.state;
                if(obj) {
                        joinStreams[key].with = obj.streamId;
                        this.setState({joinStreams: joinStreams});
                } else {
                        joinStreams[key].with = '';
                        this.setState({joinStreams: joinStreams});
                }
        }

	validateData(){
                let {outputKeys, windowNum, joinStreams} = this.state;
                let validData = true;
                if(outputKeys.length === 0 || windowNum === ''){
                        validData = false;
		}
                joinStreams.map((s)=>{
                        if(s.stream === '' || s.type === '' || s.key === '' || s.with === '') {
                                validData = false;
                        }
                });
                return validData;
	}

        handleSave(name, description){
        let {topologyId, versionId, nodeType} = this.props;
                let {outputKeys, windowNum, slidingNum, durationType, slidingDurationType, intervalType, parallelism,
                        outputStreamFields, joinFromStreamName, joinFromStreamKey, joinStreams} = this.state;
                let configObj = {
                        from: {
                                stream: joinFromStreamName,
                                key: joinFromStreamKey
                        },
                        joins: [],
                        outputKeys: outputKeys,
                        window: {
                                windowLength:{
                                        class: intervalType,
                                },
                                slidingInterval: {
                                        class: intervalType
                                },
                                tsField: null,
                                lagMs: 0
                        },
                        outputStream: this.streamData.streamId
                };
                let promiseArr = [];

                joinStreams.map((s)=>{
                        configObj.joins.push({
                                type: s.type,
                                stream: s.stream,
                                key: s.key,
                                with: s.with
                        });
                });

                if(intervalType === '.Window$Duration'){
                        configObj.window.windowLength.durationMs = Utils.numberToMilliseconds(windowNum, durationType);
                        if(slidingNum !== ''){
                                configObj.window.slidingInterval = {
                                        class: intervalType,
                                        durationMs: Utils.numberToMilliseconds(slidingNum, slidingDurationType)
                                };
                        }
                } else if (intervalType === '.Window$Count'){
                        configObj.window.windowLength.count = windowNum;
                        if(slidingNum !== ''){
                                configObj.window.slidingInterval = {
                                        class: intervalType,
                                        count: slidingNum
                                };
                        }
                }
		let nodeId = this.nodeData.id;
        return TopologyREST.getNode(topologyId, versionId, nodeType, nodeId)
			.then(data=>{
                                data.config.properties = configObj;
                                data.config.properties.parallelism = parallelism;
                                if(data.outputStreams.length > 0){
                                    data.outputStreams[0].fields = outputStreamFields;
                                } else {
                                    data.outputStreams.push({
                                        fields: outputStreamFields,
                                        streamId: this.streamData.streamId
                                    })
                                }                                
                                data.name = name;
                                data.description = description;
                                // let streamData = {
                                //         streamId: this.streamData.streamId,
                                //         fields: outputStreamFields
                                // };
                                let promiseArr = [
                                        TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {body: JSON.stringify(data)}),
                                        // TopologyREST.updateNode(topologyId, versionId, 'streams', this.streamData.id, {body: JSON.stringify(streamData)})
                                        ]
                return Promise.all(promiseArr);
			})
	}

	render() {
		let {topologyId, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
                let { editMode, showError, fieldList, outputKeys, parallelism, intervalType, intervalTypeArr, windowNum, slidingNum,
            durationType, slidingDurationType, durationTypeArr, joinFromStreamName, joinFromStreamKey, inputStreamsArr,
		joinTypes} = this.state;
		return (
                        <div  className="modal-form processor-modal-form">
        <Scrollbars autoHide
          renderThumbHorizontal={props => <div {...props} style={{display : "none"}}/>}
          >
                                <form className="customFormClass">
                                        <div className="form-group row">
                                                        <div className="col-sm-3">
                                                                <label>Select Stream</label>
                                                                <Select
                                                                        value={joinFromStreamName}
                                                                        options={inputStreamsArr}
                                                                        onChange={this.handleJoinFromStreamChange.bind(this)}
                                                                        required={true}
                                                                        disabled={!editMode}
                                                                        clearable={false}
                                                                        backspaceRemoves={false}
                                                                        valueKey="streamId"
                                                                        labelKey="streamId"
                                                                />
							</div>
                                                        <div className="col-sm-3">
                                                                <label>Select Field {this.state.joinStreams.length ? (<strong>with</strong>) : ''}</label>
                                                                <Select
                                                                        value={joinFromStreamKey}
                                                                        options={this.state.joinFromStreamKeys}
                                                                        onChange={this.handleJoinFromKeyChange.bind(this)}
                                                                        required={true}
                                                                        disabled={!editMode || joinFromStreamName === ''}
                                                                        clearable={false}
                                                                        backspaceRemoves={false}
                                                                        valueKey="name"
                                                                        labelKey="name"
                                                                />
							</div>
                                        </div>
                                        {this.state.joinStreams.length ?
                                        <div className="form-group row no-margin">
                                            <div className="col-sm-3">
                                                <label>Join Type</label>
                                            </div>
                                            <div className="col-sm-3">
                                                <label>Select Stream</label>
                                            </div>
                                            <div className="col-sm-3">
                                                <label>Select Field</label>
                                            </div>
                                            <div className="col-sm-3">
                                                <label><strong>With</strong> Stream</label>
                                            </div>
                                        </div>
                                        : ''
                                        }
                                        {
                                                this.state.joinStreams.map((s, i)=>{
                                                        return (
                                                                        <div className="form-group row" key={i}>
                                                                                        <div className="col-sm-3">
                                                                                                <Select
                                                                                                        value={s.type}
                                                                                                        options={joinTypes}
                                                                                                        onChange={this.handleJoinTypeChange.bind(this, i)}
                                                                                                        required={true}
                                                                                                        disabled={!editMode}
                                                                                                        clearable={false}
                                                                                                        backspaceRemoves={false}
                                                                                                />
                                                                                        </div>
                                                                                        <div className="col-sm-3">
                                                                                                <Select
                                                                                                        value={s.stream}
                                                                                                        options={s.streamOptions}
                                                                                                        onChange={this.handleJoinStreamChange.bind(this, i)}
                                                                                                        required={true}
                                                                                                        disabled={!editMode || s.streamOptions.length === 0}
                                                                                                        clearable={false}
                                                                                                        backspaceRemoves={false}
                                                                                                        valueKey="streamId"
                                                                                                        labelKey="streamId"
                                                                                                />
                                                                                        </div>
                                                                                        <div className="col-sm-3">
                                                                                                <Select
                                                                                                        value={s.key}
                                                                                                        options={s.keyOptions}
                                                                                                        onChange={this.handleJoinKeyChange.bind(this, i)}
                                                                                                        required={true}
                                                                                                        disabled={!editMode || s.stream === '' || s.keyOptions.length === 0}
                                                                                                        clearable={false}
                                                                                                        backspaceRemoves={false}
                                                                                                        valueKey="name"
                                                                                                        labelKey="name"
                                                                                                />
                                                                                        </div>
                                                                                        <div className="col-sm-3">
                                                                                                <Select
                                                                                                        value={s.with}
                                                                                                        options={s.withOptions}
                                                                                                        onChange={this.handleJoinWithChange.bind(this, i)}
                                                                                                        required={true}
                                                                                                        disabled={!editMode || s.withOptions.length === 0}
                                                                                                        clearable={false}
                                                                                                        backspaceRemoves={false}
                                                                                                        valueKey="streamId"
                                                                                                        labelKey="streamId"
                                                                                                />
                                                                                        </div>
                                                                        </div>
                                                        );
                                                })
                                        }
                                        <div className="form-group">
                                            <div className="row">
                                                <div className="col-sm-6">
                                                        <label>Window Interval Type <span className="text-danger">*</span></label>
                                                        <Select
                                                                value={intervalType}
                                                                options={intervalTypeArr}
                                                                onChange={this.handleIntervalChange.bind(this)}
                                                                required={true}
                                                                disabled={!editMode}
                                                                clearable={false}
                                                        />
                                                </div>
                                                <div className="col-sm-6">
                                                    <label>Parallelism</label>
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
                                        </div>
                                        <div className="form-group row">
                                            <div className="col-sm-6">
                                                <label>Window Interval <span className="text-danger">*</span></label>
                                                    <div className="row">
                                                        <div className="col-sm-6">
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
                                                            <div className="col-sm-6">
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
                                            </div>
                                            <div className="col-sm-6">
                                                <label>Sliding Interval</label>
                                                <div className="row">
                                                    <div className="col-sm-6">
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
                                                        <div className="col-sm-6">
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
                                        </div>
                                        </div>
                                        <div className="form-group">
                                                <label>Output Fields <span className="text-danger">*</span></label>
                                                <div className="row">
                                                <div className="col-sm-12">
                                                        <Select
                                                                className="menu-outer-top"
                                                                value={outputKeys}
                                                                options={fieldList}
                                                                onChange={this.handleFieldsChange.bind(this)}
                                                                multi={true}
                                                                required={true}
                                                                disabled={!editMode}
                                                                valueKey="name"
                                                                labelKey="name"
                                                        />
                                                </div>
                                                </div>
                                        </div>
                                </form>
        </Scrollbars>
			</div>
		)
	}
}

JoinNodeForm.contextTypes = {
        ParentForm: React.PropTypes.object
};
