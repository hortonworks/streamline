import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';

import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';

import TopologyREST from '../rest/TopologyREST';
import FSReactToastr from './FSReactToastr';
import TopologyUtils from '../utils/TopologyUtils';
import CommonNotification from '../utils/CommonNotification';
import {toastOpt} from '../utils/Constants'

CodeMirror.registerHelper("lint", "json", function(text) {
  var found = [];
  var {parser} = jsonlint;
  parser.parseError = function(str, hash) {
    var loc = hash.loc;
    found.push({from: CodeMirror.Pos(loc.first_line - 1, loc.first_column),
                to: CodeMirror.Pos(loc.last_line - 1, loc.last_column),
                message: str});
  };
  try { jsonlint.parse(text); }
  catch(e) {}
  return found;
});

export default class OutputSchema extends Component {
	static propTypes = {
		topologyId: PropTypes.string.isRequired,
		streamObj: PropTypes.object,
		connectedTargetNodes: PropTypes.array,
		linkShuffleOptions: PropTypes.array.isRequired
	};

	constructor(props){
		super(props);
		let {streamId = '', fields = [], grouping = 'SHUFFLE', connectsTo = [], forRule = [], groupingFields} = props.streamObj;
		let targetNodesArr = [];
		let rulesArr = [];
		let groupingFieldsArr = [];
		this.fetchNodes();
		this.props.connectedTargetNodes.map((n)=>{
			targetNodesArr.push({
				value: n.id,
				label: n.name
			});
		});
		fields.map((f)=>{
			groupingFieldsArr.push({value: f.name, label: f.name});
		});
		if(props.ruleProcessor){
			this.props.currentRulesArr.map(r=>{
				rulesArr.push({
					value: r.id,
					label: r.name
				})
			})
		}
		this.notificationNode = null;
		this.state = {
                        streamId,
			grouping,
			// groupingArray: this.props.linkShuffleOptions,
			groupingArray: [{value: "SHUFFLE", label: "SHUFFLE"},{value: "FIELDS", label: "FIELDS"}],
			//groupingArray: [{value: "SHUFFLE", label: "SHUFFLE"}],
			fields: JSON.stringify(fields, null, "  "),
			connectsTo,
			targetNodesArr,
			rulesArr,
			forRule,
			groupingFieldsArr: groupingFieldsArr,
			groupingFields: groupingFields ? groupingFields : [],
			showError: false,
			showErrorLabel: false,
			changedFields: []
		};
	}

	fetchNodes(){
		let promiseArr = [];
		this.props.connectedTargetNodes.map(c=>{
			let nodeType = TopologyUtils.getNodeType(c.parentType);
			promiseArr.push(TopologyREST.getNode(this.props.topologyId, nodeType, c.id));
		})
		Promise.all(promiseArr)
			.then(results=>{
				results.map(result=>{
					if(result.entity.type === 'NOTIFICATION'){
						this.notificationNode = result.entity;
					}
				})
			})
	}

	handleValueChange(e) {
		let obj = {};
                if(e.target.value.indexOf('-') > -1) {
                        return;
                }
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleJSONChange(json){
		let fields = this.getFieldsForGrouping(json);
		this.setState({
				fields: json,
				groupingFieldsArr: fields
		});
	}

	getFieldsForGrouping(json) {
		let arr = [],
			isValid = this.validateJSON(json);
		if(isValid) {
			let fields = JSON.parse(json);
			if(_.isArray(fields)) {
				fields.map((f)=>{
				if(f.name && f.name !== '')
					arr.push({value: f.name, label: f.name});
			})
			}
		}
		return arr;
	}

	validateJSON(json) {
		let isValid = true;
		try {
			JSON.parse(json);
		}
		catch(e) {isValid = false;}
		return isValid;
	}

	handleGroupingChange(obj) {
		if(obj){
			let fields = this.getFieldsForGrouping(this.state.fields);
			this.setState({grouping: obj.value, groupingFieldsArr: fields});
		} else {
			this.setState({grouping: ''});
		}
	}

	handleTargetNodeChange(arr) {
		let nodes = [];
		if(arr && arr.length){
			for(let t of arr){
				nodes.push(t.value);
			}
			this.setState({connectsTo: nodes});
		} else {
			this.setState({connectsTo: []});
		}
	}

	handleRuleChange(arr){
		let rules = [];
		if(arr && arr.length){
			for(let t of arr){
				rules.push(t.value);
			}
			this.setState({forRule: rules});
		} else {
			this.setState({forRule: []});
		}
	}

	handleGroupingFieldsChange(arr) {
		let groupingFields = [];
		if(arr && arr.length) {
			arr.map((f)=>{
				groupingFields.push(f.value);
			});
			this.setState({groupingFields: groupingFields});
		} else {
			this.setState({groupingFields: ''});
		}
	}

	validateData(){
		let {streamId, fields, changedFields} = this.state;
		let validDataFlag = true;
		if( streamId.trim() === '' || !this.validateJSON(fields) || !_.isArray(JSON.parse(fields))){
			validDataFlag = false;
			if(streamId.trim() === '' && changedFields.indexOf("streamId") === -1)
				changedFields.push('streamId');
		}
		if(this.props.connectedTargetNodes.length > 0){
			if(this.state.connectsTo.length === 0 ){
				validDataFlag = false;
			}
			else if(!this.state.grouping || this.state.grouping === ''){
				validDataFlag = false;
			} else if(this.props.ruleProcessor && this.state.forRule.length === 0) {
				validDataFlag = false;
			} else if(this.state.grouping === 'FIELDS' && this.state.groupingFields.length === 0) {
				validDataFlag = false;
			}
		}
		if(!validDataFlag)
			this.setState({showError: true, showErrorLabel: true, changedFields: changedFields});
		else this.setState({showErrorLabel: false});
		return validDataFlag;
	}

	handleSave(){
		let {topologyId, streamObj, connectedTargetNodes, nodeToOtherEdges} = this.props;
		let {streamId, fields, grouping, connectsTo} = this.state;
		let streamData = {streamId: streamId, fields: JSON.parse(fields)};

		let edgeGroupingData = {streamId: streamObj.id, grouping: grouping};
		let promiseArr = [];

		if(streamObj.id){
			//Update streams
			promiseArr.push(TopologyREST.updateNode(topologyId, 'streams', streamObj.id, {body: JSON.stringify(streamData)}));
		} else {
			//Add streams
			promiseArr.push(TopologyREST.createNode(topologyId, 'streams', {body: JSON.stringify(streamData)}));
		}

		return Promise.all(promiseArr)
			.then(results=>{
				let msg = results[0].entity.streamId + " " + (streamObj.id ? "updated" : "added") + ' successfully';
				FSReactToastr.success(<strong>{msg}</strong>);
				//Update edges with streams
				return this.updateEdges(results[0].entity.id, results[0].entity.streamId);
			})

	}

	updateEdges(id, streamName){
		let {topologyId, nodeToOtherEdges, streamObj, nodeData, nodeType, ruleProcessor, connectedTargetNodes}  = this.props;
		let {grouping, connectsTo, forRule, groupingFields} = this.state;
		let oldForRule = forRule;
		if(streamObj.forRule && streamObj.forRule.length){
			streamObj.forRule.map(id=>{
				if(oldForRule.indexOf(id) === -1){
					oldForRule.push(id);
				}
			})
		}
		oldForRule.sort();
		let edgeGroupingData = {streamId: id, grouping: grouping};
		if(grouping === "FIELDS")
			edgeGroupingData.fields = groupingFields;
		let promiseArr = [];
		//Add into node if its newly created stream
		if(!streamObj.id){
			nodeData.outputStreamIds = [];
			nodeData.outputStreams.map((stream)=>{
				nodeData.outputStreamIds.push(stream.id);
			});
			nodeData.outputStreamIds.push(id);
			delete nodeData.outputStreams;
			if(nodeData.type === 'SPLIT' || nodeData.type === 'STAGE' || nodeData.type === 'JOIN'){
				let type = nodeData.type.toLowerCase();
				let conf = nodeData.config.properties[type+'-config'];
				if(!conf){
					conf = {
						name: type+'-action',
						outputStreams: [streamName],
						__type: 'org.apache.streamline.streams.layout.component.impl.splitjoin.'+(nodeData.type === 'SPLIT' ? 'SplitAction' : (nodeData.type === 'STAGE' ? 'StageAction' : 'JoinAction'))
					}
				} else {
					conf.outputStreams.push(streamName);
				}
				nodeData.config.properties[type+'-config'] = conf;
			}
			promiseArr.push(TopologyREST.updateNode(topologyId, nodeType, nodeData.id, {body: JSON.stringify(nodeData)}));
		}
		//If rule/window processor, update rules with actions
		if(ruleProcessor || nodeData.type === 'WINDOW'){
			let promise = [];
			if(ruleProcessor){
				oldForRule.map(id=>{
					promise.push(TopologyREST.getNode(topologyId, 'rules', id));
				})
			} else {
				let windowId = nodeData.config.properties.rules && nodeData.config.properties.rules.length !== 0 ? nodeData.config.properties.rules[0] : null
				if(windowId){
					promise.push(TopologyREST.getNode(topologyId, 'windows', windowId))
				} else {
          FSReactToastr.error(
              <CommonNotification flag="error" content={"Save window processor before saving output streams."}/>, '', toastOpt)
				}
			}
			Promise.all(promise)
				.then(results=>{
					let rulesPromiseArr = [];
					results.map(result=>{
						let actions = result.entity.actions || [];

						// Delete all actions with current stream name
						let deleteActionsIndex = [];
						actions.map((a,i)=>{
							let index = a.outputStreams.indexOf(streamName);
							if(index !== -1){
								deleteActionsIndex.push(i)
							}
						})
						deleteActionsIndex.reverse().map(index=>{
							actions.splice(index, 1);
						})

						if(nodeData.type === 'WINDOW' || forRule.indexOf(result.entity.id) !== -1){
							//Add actions to only those who needs to be connected
							connectsTo.map(id=>{
								let targetObj = connectedTargetNodes.filter((n)=>{return n.id === id})[0];
								let targetName = targetObj.name;
								let actionObj = {
									outputStreams: [streamName],
									name: targetName //connected node name,
								};
								if(targetObj.currentType === 'Notification'){
									actionObj.outputFieldsAndDefaults = this.notificationNode.config.properties.fieldValues || {};
									actionObj.notifierName = this.notificationNode.config.properties.notifierName || '';
									actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.NotifierAction";
								} else {
									actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.TransformAction";
								}
								if(actions.length === 0){
									actions.push(actionObj);
								} else {
									let index = null;
									actions.map((a,i)=>{
										if(a.name === targetName && a.outputStreams.indexOf(streamName) !== -1){
											index = i;
										}
									})
									if(index !== null){
										actions[index] = actionObj
									} else {
										actions.push(actionObj);
									}
								}
							})
						}
						result.entity.actions = actions;
						if(nodeData.type === 'WINDOW'){
							rulesPromiseArr.push(TopologyREST.updateNode(topologyId, 'windows', result.entity.id, {body: JSON.stringify(result.entity)} ));
						} else {
							rulesPromiseArr.push(TopologyREST.updateNode(topologyId, 'rules', result.entity.id, {body: JSON.stringify(result.entity)} ));
						}
					})
					Promise.all(rulesPromiseArr)
				})
		}
		//updating stream association from edges
			nodeToOtherEdges.map(e=>{
				let removeFlag = false, addFlag = false;
				let edgeId = e.id;
				let targetNodeId = e.toId;
				if(connectsTo.indexOf(targetNodeId) !== -1){
					//Add/Update into this edge
					addFlag = true;
				} else {
					//Remove if exists from this edge
					removeFlag = true;
				}

				if(addFlag || removeFlag){
					let existingStream = null, indexOfStream = null;
					e.streamGroupings.map((g,i)=>{
						//find streamId
						if(g.streamId === id ){
							existingStream = g;
							indexOfStream = i;
						}
					})
					if(addFlag){
						if(!existingStream){
							//Add stream into edge
							e.streamGroupings.push(edgeGroupingData);
						} else {
							//Update stream into edge
							existingStream.grouping = grouping;
							if(grouping === "FIELDS")
								existingStream.fields = groupingFields;
							else delete existingStream.fields
						}
					} else if(removeFlag){
						//Remove stream from edge
						if(indexOfStream !== null){
							e.streamGroupings.splice(indexOfStream, 1);
						}
					}
					promiseArr.push(TopologyREST.updateNode(topologyId, 'edges', edgeId, {body: JSON.stringify(e)}));
				}
			})
			return Promise.all(promiseArr);
	}

	render(){
		let {schemaData, canAdd, ruleProcessor} = this.props;
		const jsonoptions = {
			lineNumbers: true,
			mode: "application/json",
			styleActiveLine: true,
			gutters: ["CodeMirror-lint-markers"],
			lint: true,
			readOnly: canAdd ? false : 'nocursor'
        }
        let {streamId, fields, grouping, groupingArray,
				connectsTo, targetNodesArr, rulesArr,
				forRule, groupingFields, groupingFieldsArr } = this.state;
		return (
			<form className="form-horizontal">
				<div className="form-group">
					<label className="col-sm-3 control-label">Stream ID*</label>
					<div className="col-sm-5">
						<input
							name="streamId"
							placeholder="Stream ID"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className={this.state.showError && this.state.changedFields.indexOf("streamId") !== -1 && streamId.trim() === '' ? "form-control invalidInput" : "form-control"}
							value={streamId}
						    required={true}
						    disabled={!canAdd}
						/>
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">JSON*</label>
					<div className="col-sm-5">
						<ReactCodemirror ref="JSONCodemirror" value={fields} onChange={this.handleJSONChange.bind(this)} options={jsonoptions} />
					</div>
				</div>
				<div className="form-group">
					<label className="col-sm-3 control-label">Grouping</label>
					<div className="col-sm-5">
						<Select
							value={grouping}
							options={groupingArray}
							onChange={this.handleGroupingChange.bind(this)}
						    required={true}
						    disabled={!targetNodesArr.length}
						/>
					</div>
				</div>
				{grouping === 'FIELDS' ?
				<div className="form-group">
					<label className="col-sm-3 control-label">Select Fields</label>
					<div className="col-sm-5">
						<Select
							value={groupingFields}
							options={groupingFieldsArr}
							onChange={this.handleGroupingFieldsChange.bind(this)}
							multi={true}
							required={true}
						/>
					</div>
				</div>
				: null
				}
                                {ruleProcessor ?
					<div className="form-group">
						<label className="col-sm-3 control-label">For Rule</label>
						<div className="col-sm-5">
							<Select
								value={forRule}
								options={rulesArr}
								onChange={this.handleRuleChange.bind(this)}
								multi={true}
								clearable={false}
								joinValues={true}
								required={true}
							/>
						</div>
					</div>
				: null}
				<div className="form-group">
					<label className="col-sm-3 control-label">Connects To</label>
					<div className="col-sm-5">
						<Select
							value={connectsTo}
							options={targetNodesArr}
							onChange={this.handleTargetNodeChange.bind(this)}
							multi={true}
							clearable={false}
							joinValues={true}
							required={true}
						/>
					</div>
				</div>
			</form>
		)
	}
}
