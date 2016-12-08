import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import AggregateUdfREST from '../../../rest/AggregateUdfREST';
import Utils from '../../../utils/Utils';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants'

export default class WindowingAggregateNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
        versionId: PropTypes.number.isRequired,
		sourceNode: PropTypes.object.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired,
		currentEdges: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
        this.sourceNodesId = [props.sourceNode.nodeId];
        this.ruleTargetNodes = [];
		let {editMode} = props;
		this.fetchData();
		var obj = {
			parallelism: 1,
			editMode: editMode,
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
            outputStreamFields: [],
            argumentError : false,
            outputArr : []
		};
		this.state = obj;
    this.outputData = [];
	}

	fetchData(){
        let {topologyId, versionId, nodeType, nodeData, currentEdges, targetNodes} = this.props;
		let edgePromiseArr = [];
        let ruleTargetNodes = targetNodes.filter((o)=>{return o.currentType.toLowerCase() === 'rule'});
		currentEdges.map(edge=>{
			if(edge.target.nodeId === nodeData.nodeId){
                edgePromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'edges', edge.edgeId));
			}
		})
		Promise.all(edgePromiseArr)
			.then(edgeResults=>{
				let promiseArr = [
                    TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
					AggregateUdfREST.getAllUdfs()
				];
				let streamIdArr = []
				edgeResults.map(result=>{
                                        if(result.streamGroupings){
                                                result.streamGroupings.map(streamObj=>{
							if(streamIdArr.indexOf(streamObj.streamId) === -1){
								streamIdArr.push(streamObj.streamId);
                                promiseArr.push(TopologyREST.getNode(topologyId, versionId, 'streams', streamObj.streamId))
							}
						})
					}
				})
                let rulePromiseArr = []
                ruleTargetNodes.map((ruleNode)=>{
                rulePromiseArr.push(TopologyREST.getNode(topologyId, versionId, nodeType, ruleNode.nodeId));
            });
            Promise.all(rulePromiseArr)
                                .then((results)=>{
                                        results.map((o)=>{
                                                this.ruleTargetNodes.push(o);
                                        });
				});
				Promise.all(promiseArr)
					.then((results)=>{
                                                this.nodeData = results[0];
						let configFields = this.nodeData.config.properties;
						this.windowId = configFields.rules ? configFields.rules[0] : null;
						let fields = [];
						let streamsList = [];
						//Gather all aggregate functions only
						let udfList = this.udfList = [];
						results[1].entities.map((funcObj)=>{
							if(funcObj.type === 'AGGREGATE'){
								udfList.push(funcObj);
							}
						})
						results.map((result,i)=>{
							if(i > 1){
                                                                streamsList.push(result);
                                                                fields.push(...result.fields);
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
                            this.context.ParentForm.setState({outputStreamObj:this.streamData})
						} else {
							stateObj.outputStreamId = 'window_stream_'+this.nodeData.id;
							stateObj.outputStreamFields = [];
							let dummyStreamObj = {
								streamId: stateObj.outputStreamId,
								fields: stateObj.outputStreamFields
							}
                                                        TopologyREST.createNode(topologyId, versionId, 'streams', {body: JSON.stringify(dummyStreamObj)})
								.then(streamResult => {
                                                                        this.streamData = streamResult;
                                    this.context.ParentForm.setState({outputStreamObj:this.streamData})
									this.nodeData.outputStreamIds = [this.streamData.id];
                                                                        TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {body: JSON.stringify(this.nodeData)})
								})
						}
						if(this.windowId){
                            TopologyREST.getNode(topologyId, versionId, 'windows', this.windowId)
								.then((windowResult)=>{
                                                                        let windowData = windowResult;
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
                                                        TopologyREST.createNode(topologyId, versionId, 'windows', {body: JSON.stringify(dummyWindowObj)})
								.then((windowResult)=>{
                                                                        this.windowId = windowResult.id;
									this.nodeData.config.properties.rules = [this.windowId];
                                                                        TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {body: JSON.stringify(this.nodeData)});
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

	handleFieldChange(name, index, obj){
    const {outputArr} = this.state;
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
					let appendingName = '';
					if(fieldsArr[index].functionName !== ''){
						let obj = this.udfList.find((o)=>{ return o.name === fieldsArr[index].functionName; })
						appendingName = '_'+obj.displayName;
					}
					fieldsArr[index].outputFieldName = fieldsArr[index].args+appendingName;
				}
		}

    let outputStreamFields = this.getOutputFieldsForStream(oldData, fieldsArr[index]);
    outputStreamFields.then((res) => {
      this.streamData.fields = res;
      this.context.ParentForm.setState({outputStreamObj:this.streamData});
      this.setState({ outputStreamFields: res});
    });

    this.setState({outputFieldsArr: fieldsArr});
	}

  getOutputFieldsForStream(oldObj, newDataObj){
    let streamsArr = this.state.outputStreamFields;
    let obj = null;
    if(oldObj.outputFieldName !== ''){
      obj = this.outputData.filter((field)=>{return field.name === oldObj.outputFieldName;})[0];
    } else {
      obj = this.outputData.filter((field)=>{return field.name === oldObj.args;})[0];
    }
    if(obj){
      let fieldObj = this.state.keysList.find((field)=>{return field.name == newDataObj.args});
      if(newDataObj.functionName !== ''){
        obj.name = newDataObj.outputFieldName;
        obj.type = this.getReturnType(newDataObj.functionName, fieldObj);
      } else if(oldObj.functionName !== ''){
        obj.name = newDataObj.outputFieldName;
        obj.type = this.getReturnType(newDataObj.functionName, fieldObj);
      }
    } else {
      let o = this.outputData.filter((field)=>{return field.name === newDataObj.outputFieldName;});
      if(o.length === 0){
        let fieldObj = this.state.keysList.find((field)=>{return field.name == newDataObj.args});
        this.outputData.push({
          name: newDataObj.outputFieldName,
          type: this.getReturnType(newDataObj.functionName, fieldObj),
          optional: false
        })
      }
    }
    let flag = false,resolve;
    this.outputData.map(x => {
        return x.name.indexOf('_') === -1 ? flag = false : flag = true;
    });
    if(flag){
      this.setState({outputArr : this.outputData},() => {
        const filtered = this.outputData.filter((d) => {
          return this.state.outputStreamFields.indexOf(d) === -1
        });
        resolve(streamsArr.concat(filtered));
      });
    }
    const outputAction = new Promise((res,rej) => {
      resolve = res;
    })
    return outputAction;
  }

	getReturnType(functionName, fieldObj){
		let obj = this.udfList.find((o)=>{
			return o.name === functionName;
		})
		if(obj){
			if(obj.returnType){
          if(fieldObj){
            let argList = obj.argTypes[0].includes(fieldObj.type);
            (argList) ? this.setState({argumentError : false}) : this.setState({argumentError : true})
          }
				return obj.returnType;
			} else {
				return fieldObj.type;
			}
		} else if(fieldObj){
			return fieldObj.type;
		} else {
			return 'DOUBLE';
		}
	}
	addOutputFields(){
		if(this.state.editMode){
      const el = document.querySelector('.processor-modal-form ');
      const targetHt = el.scrollHeight;
      Utils.scrollMe(el,(targetHt+100),2000);

			let fieldsArr = this.state.outputFieldsArr;
			fieldsArr.push({args: '', functionName: '', outputFieldName: ''});
			this.setState({outputFieldsArr: fieldsArr});
		}
	}
	deleteFieldRow(index){
		if(this.state.editMode){
      let outputArr = this.state.outputArr;
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
      outputArr.splice(index, 1);
			this.streamData.fields = outputStreamFields;
                        this.context.ParentForm.setState({outputStreamObj:this.streamData})
                        this.setState({outputFieldsArr: fieldsArr, outputStreamFields: outputStreamFields,outputArr:outputArr});
		}
	}

	validateData(){
		let {selectedKeys, windowNum, outputFieldsArr, argumentError} = this.state;
		let validData = true;
		if(selectedKeys.length === 0 || windowNum === ''){
			validData = false;
		}
		outputFieldsArr.map((obj)=>{
			if(obj.args === '' || obj.outputFieldName === ''){
				validData = false;
			}
		})
                if(argumentError){
                        return false;
                }
		return validData;
	}

	handleSave(name){
		let {selectedKeys, windowNum, slidingNum, outputFieldsArr, durationType, slidingDurationType,
			intervalType, streamsList, parallelism} = this.state;
        let {topologyId, versionId, nodeType, nodeData} = this.props;
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
                return TopologyREST.getNode(topologyId, versionId, 'windows', this.windowId)
                .then((result)=>{
                let data = result;
                windowObj.actions = result.actions || [];

                if(this.props.sourceNode.currentType.toLowerCase() === 'rule' ||
                    this.props.sourceNode.currentType.toLowerCase() === 'window' ||
                    this.props.sourceNode.currentType.toLowerCase() === 'branch') {
                	let t = this.props.sourceNode.currentType.toLowerCase();
                    let type = t === 'rule' ? 'rules' : (t === 'window' ? 'windows' : 'branchrules');
                    let nodeName = this.nodeData.name;
                    TopologyREST.getAllNodes(topologyId, versionId, type).then((results)=>{
                        results.entities.map((nodeObj)=>{
							let actionObj = nodeObj.actions.find((a)=>{
								return a.name === nodeName;
                            });
							if(actionObj) {
								actionObj.name = name;
                                TopologyREST.updateNode(topologyId, versionId, type, nodeObj.id, {body: JSON.stringify(nodeObj)});
							}
						});
                    });
                }

                return TopologyREST.updateNode(topologyId, versionId, 'windows', this.windowId, {body: JSON.stringify(windowObj)})
                    .then(windowResult=>{
                    	return this.updateNode(windowResult, name);
                    })
                })
		}
	}
	updateNode(windowObj, name){
		let {parallelism, outputStreamFields} = this.state;
                let {topologyId, versionId, nodeType, nodeData} = this.props;
                return TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId)
				.then(result=>{
                                        let data = result;
                                        if(windowObj && windowObj){
                                                let windowData = windowObj;
						data.config.properties.parallelism = parallelism;
						data.config.properties.rules = [windowData.id];
						data.outputStreamIds = [this.streamData.id];
						data.name = name;
						let streamData = {
							streamId: this.streamData.streamId,
							fields: outputStreamFields
						}
						let promiseArr = [
                                                        TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {body: JSON.stringify(data)}),
                                                        TopologyREST.updateNode(topologyId, versionId, 'streams', this.streamData.id, {body: JSON.stringify(streamData)})
                        ]
                        this.ruleTargetNodes.map((ruleNode)=>{
						let streamObj = {
							streamId: ruleNode.outputStreams[0].streamId,
							fields: outputStreamFields
						}
                                promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'streams', ruleNode.outputStreams[0].id, {body: JSON.stringify(streamObj)}));
                        });
                        return Promise.all(promiseArr);
                            } else {
                                    FSReactToastr.error(
                                        <CommonNotification flag="error" content={windowObj.responseMessage}/>, '', toastOpt)
                            }
                        })
        }

	render() {
		let {parallelism, selectedKeys, keysList, editMode, intervalType, intervalTypeArr, windowNum, slidingNum,
            durationType, slidingDurationType, durationTypeArr, outputFieldsArr, functionListArr, outputStreamId, outputStreamFields,argumentError } = this.state;
		let {topologyId, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		return (
                                <form className="modal-form processor-modal-form form-overflow">
                                        <div className="form-group">
                                                <label>Select Keys <span className="text-danger">*</span></label>
                                                <div>
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
                                                <label>Window Interval Type <span className="text-danger">*</span></label>
                                                <div>
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
                                                <label>Window Interval <span className="text-danger">*</span></label>
                                                <div className="row">
                                                        <div className="col-sm-5">
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
                                                                <div className="col-sm-5">
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
                                        <div className="form-group">
                                                <label>Sliding Interval</label>
                                                <div className="row">
                                                        <div className="col-sm-5">
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
                                                                <div className="col-sm-5">
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
                                        <div className="form-group">
                                                <label>Parallelism</label>
                                                <div>
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
                                                {
                                                        (argumentError) ? <label className="color-error">The Aggregate Function is not supported by input</label> : ''
                                                }
                                                <div className="row">
                                                        <div className="col-sm-3 outputCaption">
                                                                <label>Input</label>
                                                        </div>
                                                        <div className="col-sm-3 outputCaption">
                                                                <label>Aggregate Function</label>
                                                                                                  </div>

                                                        <div className="col-sm-3 outputCaption">
                                                                <label>Output</label>
                                                        </div>
                                                                                    </div>
                                                {outputFieldsArr.map((obj, i)=>{
                                                        return(
                                                                <div key={i} className="row form-group">
                                                                        <div className="col-sm-3">
                                                                                <Select
                                                                                        className={outputFieldsArr.length-1 === i ? "menu-outer-top" : ''}
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
                                                                                <Select
                                                                                        className={outputFieldsArr.length-1 === i ? "menu-outer-top" : ''}
                                                                                        value={obj.functionName}
                                                                                        options={functionListArr}
                                                                                        onChange={this.handleFieldChange.bind(this, 'functionName', i)}
                                                                                        required={true}
                                                                                        disabled={!editMode}
                                                                                        valueKey="name"
                                                                                        labelKey="displayName"
                                                                                />
                                                                        </div>
                                                                        <div className="col-sm-3">
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
                                                                        {editMode ?
                                                                                <div className="col-sm-2">
                                                                                        <button className="btn btn-default btn-sm" type="button" onClick={this.addOutputFields.bind(this)}>
                                                                                                <i className="fa fa-plus"></i>
                                                                                        </button>&nbsp;
                                                                                        {i > 0 ?
												<button className="btn btn-sm btn-danger" type="button" onClick={this.deleteFieldRow.bind(this, i)}>
													<i className="fa fa-trash"></i>
												</button>
                                                                                        : null}
                                                                                </div>
                                                                        :null}

                                                                </div>
                                                        )
                                                })}
                                        </fieldset>
                                </form>
            )
        }
}

WindowingAggregateNodeForm.contextTypes = {
    ParentForm: React.PropTypes.object,
};
