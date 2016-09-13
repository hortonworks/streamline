import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';

import TopologyREST from '../rest/TopologyREST';
import {BtnDelete, BtnEdit} from './ActionButtons';
import Modal, {Confirm} from './FSModal';
import OutputSchemaForm from './OutputSchemaForm';
import FSReactToastr from './FSReactToastr';
import {pageSize} from '../utils/Constants';

export default class OutputSchema extends Component {
	static propTypes = {
		topologyId: PropTypes.string.isRequired,
		nodeId: PropTypes.number.isRequired,
		nodeType: PropTypes.string.isRequired,
		editMode: PropTypes.bool.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired,
		canAdd: PropTypes.bool,
		canDelete: PropTypes.bool,
		maxStreamSize: PropTypes.number
	};

	constructor(props){
		super(props);
		this.state = {
			outputStreams: [],
			canAdd: typeof props.canAdd === 'boolean' ? props.canAdd : true,
			canDelete: typeof props.canDelete === 'boolean' ? props.canDelete : true,
			modalTitle: '',
			streamObj: {},
			ruleProcessor: false
		}
		this.currentRulesArr = [];
		this.fetchNode();
	}

	fetchNode(){
		let {topologyId, nodeType, nodeId, targetNodes} = this.props;
		let promiseArr = [
			TopologyREST.getNode(topologyId, nodeType, nodeId),
			TopologyREST.getAllNodes(topologyId, 'edges'),
			TopologyREST.getAllNodes(topologyId, 'rules')
		];

		Promise.all(promiseArr)
			.then((results)=>{
				results.map((result)=>{
					if(result.responseCode !== 1000){
						FSReactToastr.error(<strong>{result.responseMessage}</strong>);
					}
				})
				//Current Node Data
				this.nodeData = results[0].entity;

				//Find all target edges connected from current node
				let allEdges = results[1].entities;
				this.nodeToOtherEdges = allEdges.filter((e)=>{return e.fromId === nodeId});
				
				//Find all rules of current node
				if(this.nodeData.type === 'RULE'){
					let allRules = results[2].entities;
					let ruleIdArr = this.nodeData.config.properties.rules || [];
					this.currentRulesArr = allRules.filter(r=>{return ruleIdArr.indexOf(r.id) !== -1});
				}

				this.generateData(this.nodeData);
			})
	}

	generateData(nodeData){
		let {targetNodes} = this.props;
		let stateObj = {};
		//Building required data
		let outputStreams = nodeData.outputStreams;
		outputStreams.map((s)=>{
			s.grouping = 'SHUFFLE',
			s.connectsTo = [],
			s.forRule = []
		});

		//Check if processor is rule
		if(this.nodeData.type === 'RULE'){
			stateObj.ruleProcessor = true;
		}

		let connectedTargetNodes = [];
		this.nodeToOtherEdges.map((e)=>{
			e.streamGroupings.map((g)=>{
				let streamId = g.streamId;
				let grouping = g.grouping;
				let streamObj = outputStreams.filter((o)=>{return o.id === streamId})[0];
				streamObj.grouping = grouping;
				streamObj.connectsTo.push(e.toId);
				if(stateObj.ruleProcessor){
					//Add forRule array for rules action part containing stream name
					streamObj.forRule = [];
					this.currentRulesArr.map(c=>{
						if(c.actions === undefined || c.actions === null){
							c.actions = [];
						}
						c.actions.map(a=>{
							if(a.outputStreams.indexOf(streamObj.streamId) !== -1 && streamObj.forRule.indexOf(c.id) === -1){
								streamObj.forRule.push(c.id);
							}
						})
					})
				}
			})

			let targetObj = targetNodes.filter((t)=>{return t.nodeId === e.toId})[0];
			connectedTargetNodes.push({
				id: e.toId,
				name: targetObj.uiname,
				edgeId: e.id,
				parentType: targetObj.parentType,
				currentType: targetObj.currentType
			});
		})

		stateObj.outputStreams = outputStreams;
		stateObj.connectedTargetNodes = connectedTargetNodes;

		this.setState(stateObj);
	}

	deleteStream(streamId){
		let {topologyId, nodeType, nodeId} = this.props;
		let {outputStreams} = this.state;
		let outputStreamIds = [];
		
		outputStreams.map((stream)=>{
			outputStreamIds.push(stream.id);
		});
		
		outputStreamIds.splice(outputStreamIds.indexOf(streamId), 1);
		let outputStreamsObj = JSON.parse(JSON.stringify(this.nodeData.outputStreams));
		let streamsObj = outputStreamsObj.filter((o)=>{return o.id === streamId})[0];
		delete this.nodeData.outputStreams;
		this.nodeData.outputStreamIds = outputStreamIds;

		if(this.nodeData.type === 'SPLIT' || this.nodeData.type === 'STAGE' || this.nodeData.type === 'JOIN'){
			let type = this.nodeData.type.toLowerCase();
			let conf = this.nodeData.config.properties[type+'-config'];
			if(conf){
				conf.outputStreams.splice(conf.outputStreams.indexOf(streamsObj.streamId), 1);
				this.nodeData.config.properties[type+'-config'] = conf;
			}
		}

		let promiseArr = [];

		//Add/Removing stream association from node
		promiseArr.push(TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)}));
		
		//Removing stream association from edges
		this.nodeToOtherEdges.map(e=>{
			let edgeId = e.id;
			let index = [];
			e.streamGroupings.map((g,i)=>{
				if(g.streamId === streamId ){
					index.push(i);
				}
			})
			index.reverse().map(id=>{
				e.streamGroupings.splice(id, 1);
			})
			promiseArr.push(TopologyREST.updateNode(topologyId, 'edges', edgeId, {body: JSON.stringify(e)}));
		})

		//Remove rules association with the stream
		if(this.nodeData.type === 'RULE'){
			this.currentRulesArr.map(rule=>{
				if(rule.actions && rule.actions.length > 0){
					let indexArr = [];
					rule.actions.map((r,i)=>{
						if(r.outputStreams.indexOf(streamsObj.streamId) !== -1){
							indexArr.push(i);
						}
					})
					indexArr.reverse().map(num=>{
						rule.actions.splice(num, 1);
					})
					promiseArr.push(TopologyREST.updateNode(topologyId, 'rules', rule.id, {body: JSON.stringify(rule)}));
				}
			})
		}

		//Removing stream
		promiseArr.push(TopologyREST.deleteNode(topologyId, 'streams', streamId));
		
		Promise.all(promiseArr)
			.then((results)=>{
				results.map(result=>{
					if(result.responseCode !== 1000){
						FSReactToastr.error(<strong>{result.responseMessage}</strong>);
					}
				})
				this.fetchNode();
				FSReactToastr.success(<strong>Stream deleted successfully</strong>);
			})
		
	}

	handleAddSchema(id){
		if(this.props.editMode){
			let streamId = null;
			let streamObj = {};
			let modalTitle = 'Add New Stream';
			if(typeof id === 'number' || typeof id === 'string'){
				streamId = id;
				streamObj = this.state.outputStreams.filter((o)=>{return o.id === id})[0];
				modalTitle = 'Edit Stream';
			}
			this.setState({streamObj, modalTitle},()=>{
				this.refs.ouputSchemaModal.show();
			})
		}
	}

	handleSave(){
		if(this.refs.schemaModal.validateData()){
			this.refs.schemaModal.handleSave().then((results)=>{
				let stream = results[0];
				setTimeout(()=>{
					this.fetchNode();
				}, 500)
				this.refs.ouputSchemaModal.hide();
			})
		}
	}

	handleDelete(id){
		if(this.state.canDelete){
			this.refs.Confirm.show({
				title: 'Are you sure you want to delete stream ?'
			}).then((confirmBox)=>{
				confirmBox.cancel();
				this.deleteStream(id);
			},()=>{});
		}
	}

	render(){
		let {editMode, targetNodes, maxStreamSize = null} = this.props;
		let {outputStreams, canDelete, canAdd, ruleProcessor} = this.state;
		let showAddBtn = true;
		if(maxStreamSize){
			if(outputStreams.length === maxStreamSize){
				showAddBtn = false;
			}
		}
		return (<div>
			{editMode && canAdd && showAddBtn ?
				<div className="clearfix row-margin-bottom">
					<button type="button" onClick={this.handleAddSchema.bind(this)} className="btn btn-success pull-left">
						<i className="fa fa-plus"></i> Add New Stream
					</button>
				</div>
			: null}
			<div className="row">
				<div className="col-sm-12">
					<Table 
		              className="table table-hover table-bordered"
		              noDataText="No records found."
		              currentPage={0}
		              itemsPerPage={outputStreams.length > pageSize ? pageSize : 0} pageButtonLimit={5}>
		                <Thead>
							<Th column="streamId">Stream ID</Th>
							<Th column="fields">Fields</Th>
							<Th column="grouping">Grouping</Th>
							<Th column="forRule" className={!ruleProcessor ? 'displayNone' : null}>For Rule</Th>
							<Th column="connectsTo">Connects To</Th>
							<Th column="action" className={!editMode ? 'displayNone' : null}>Actions</Th>
		                </Thead>
		              {outputStreams.map((obj, i) => {
		              	let names = [];
		              	obj.connectsTo.map((id)=>{
		              		names.push(targetNodes.filter(t=>{return t.nodeId == id})[0].uiname);
		              	})
		              	let ruleNames = [];
		              	obj.forRule.map((id)=>{
		              		ruleNames.push(this.currentRulesArr.filter(c=>{return c.id == id})[0].name);
		              	})
		                return (
		                  <Tr key={i}>
		                    <Td column="streamId">{obj.streamId}</Td>
		                    <Td column="fields"><pre className="field-json">{JSON.stringify(obj.fields, null, "  ")}</pre></Td>
		                    <Td column="grouping">{obj.grouping}</Td>
		                    <Td column="forRule" className={!ruleProcessor ? 'displayNone' : null}>{ruleNames}</Td>
		                    <Td column="connectsTo">{names}</Td>
	                    	<Td column="action" className={!editMode ? 'displayNone' : null}>
		                    	<div className="btn-action">
		                    		<BtnEdit callback={this.handleAddSchema.bind(this, obj.id)}/>
		                    		{canDelete ? <BtnDelete callback={this.handleDelete.bind(this, obj.id)}/> : null}
		                    	</div>
		                    </Td> 
		                  </Tr>
		                )
		              })}
		            </Table>
				</div>
			</div>
			<Modal ref="ouputSchemaModal" bsSize="large" data-title={this.state.modalTitle} data-resolve={this.handleSave.bind(this)}>
				<OutputSchemaForm
					ref="schemaModal"
					topologyId={this.props.topologyId}
					streamObj={this.state.streamObj}
					connectedTargetNodes={this.state.connectedTargetNodes}
					linkShuffleOptions={this.props.linkShuffleOptions}
					nodeToOtherEdges={this.nodeToOtherEdges}
					nodeData={this.nodeData}
					nodeType={this.props.nodeType}
					canAdd={canAdd}
					ruleProcessor={ruleProcessor}
					currentRulesArr={this.currentRulesArr}
				/>
			</Modal>
			<Confirm ref="Confirm"/>
		</div>)
	}
}