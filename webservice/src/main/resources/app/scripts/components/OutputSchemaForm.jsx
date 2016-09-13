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
		let {streamId = '', fields = [], grouping = '', connectsTo = [], forRule = []} = props.streamObj;
		let targetNodesArr = [];
		let rulesArr = [];
		this.fetchNodes();
		this.props.connectedTargetNodes.map((n)=>{
			targetNodesArr.push({
				value: n.id,
				label: n.name
			});
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
			// groupingArray: [{value: "SHUFFLE", label: "SHUFFLE"},{value: "FIELDS", label: "FIELDS"}],
			groupingArray: [{value: "SHUFFLE", label: "SHUFFLE"}],
			fields: JSON.stringify(fields, null, "  "),
			connectsTo,
			targetNodesArr,
			rulesArr,
			forRule
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
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	handleJSONChange(json){
		this.setState({fields: json});
	}

	handleGroupingChange(obj) {
		if(obj){
			this.setState({grouping: obj.value});
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
			this.setState({connectsTo: ''});
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
			this.setState({forRule: ''});
		}
	}

	validateData(){
		let {streamId, fields} = this.state;
		if( streamId === '' || fields === ''){
			return false;
		}
		if(this.props.connectedTargetNodes.length > 0 && this.state.connectsTo.length === 0 ){
			return false;
		} else if(!this.state.grouping || this.state.grouping === ''){
			return false;
		}
		if(this.props.ruleProcessor && this.state.forRule.length === 0){
			return false
		}
		return true;
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
		let {grouping, connectsTo, forRule} = this.state;
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
						__type: 'com.hortonworks.iotas.streams.layout.component.impl.splitjoin.'+(nodeData.type === 'SPLIT' ? 'SplitAction' : (nodeData.type === 'STAGE' ? 'StageAction' : 'JoinAction'))
					}
				} else {
					conf.outputStreams.push(streamName);
				}
				nodeData.config.properties[type+'-config'] = conf;
			}
			promiseArr.push(TopologyREST.updateNode(topologyId, nodeType, nodeData.id, {body: JSON.stringify(nodeData)}));
		}
		//If rule processor, update rules with actions
		if(ruleProcessor){
			let promise = [];
			oldForRule.map(id=>{
				promise.push(TopologyREST.getNode(topologyId, 'rules', id));
			})
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

						if(forRule.indexOf(result.entity.id) !== -1){
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
									actionObj.__type = "com.hortonworks.iotas.streams.layout.component.rule.action.NotifierAction";
								} else {
									actionObj.__type = "com.hortonworks.iotas.streams.layout.component.rule.action.TransformAction";
								}
								if(actions.length === 0){
									actions.push(actionObj);
								} else {
									let index = null;
									actions.map((a,i)=>{
										if(a.name === targetName && a.outputStreams.indexOf(streamName)){
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
						rulesPromiseArr.push(TopologyREST.updateNode(topologyId, 'rules', result.entity.id, {body: JSON.stringify(result.entity)} ));
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
				forRule } = this.state;
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
							className="form-control"
							value={streamId}
						    required={true}
						    disabled={!canAdd}
						/>
					</div>
					{streamId === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Stream ID</p>
						</div>
					: null}
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
						/>
					</div>
					{grouping === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Select any one type of grouping</p>
						</div>
					: null}
				</div>
				{ruleProcessor ? 
					<div className="form-group">
						<label className="col-sm-3 control-label">For Rule*</label>
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
						{forRule === '' ?
							<div className="col-sm-4">
								<p className="form-control-static error-note">Please Select atleast one rule for this stream</p>
							</div>
						: null}
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
					{connectsTo === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Select any one of the target nodes</p>
						</div>
					: null}
				</div>
			</form>
		)
	}
}