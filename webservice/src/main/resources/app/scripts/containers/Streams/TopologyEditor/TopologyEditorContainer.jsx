import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import update from 'react/lib/update';
import { DragDropContext, DropTarget } from 'react-dnd';
import { ItemTypes, Components } from '../../../utils/Constants';
import HTML5Backend from 'react-dnd-html5-backend';
import BaseContainer from '../../BaseContainer';
import {Link} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import {OverlayTrigger, Tooltip, Accordion, Panel} from 'react-bootstrap';
import Switch from 'react-bootstrap-switch';
import ComponentNodeContainer from './ComponentNodeContainer';
import TopologyConfig from './TopologyConfigContainer';
import TopologyGraphComponent from '../../../components/TopologyGraphComponent';
import FSReactToastr from '../../../components/FSReactToastr';
import {observer} from 'mobx-react';
import {observable} from 'mobx';
import _ from 'lodash';
import TopologyUtils from '../../../utils/TopologyUtils';
import Modal from '../../../components/FSModal';
import Editable from '../../../components/Editable';
import state from '../../../app_state';

const componentTarget = {
	drop(props, monitor, component) {
		const item = monitor.getItem();
		const delta = monitor.getDifferenceFromInitialOffset();
		const left = Math.round(item.left + delta.x);
		const top = Math.round(item.top + delta.y);

		component.moveBox(left, top);
	}
};

function collect(connect, monitor) {
	return {
		connectDropTarget: connect.dropTarget(),
	};
}

@DragDropContext(HTML5Backend)
@DropTarget(ItemTypes.ComponentNodes, componentTarget, collect)
@observer
class EditorGraph extends Component{
	static propTypes = {
		connectDropTarget: PropTypes.func.isRequired
	};
	constructor(props) {
		super(props);
		let left = window.innerWidth - 300;
		this.state = {
			boxes: {
				top: 50, 
				left: left
			}
		};
	}
	moveBox(left, top) {
		this.setState(update(this.state, {
			boxes: {
				$merge: {
					left: left,
					top: top
				}
			}
		}));
	}
	render(){
		const actualHeight = (window.innerHeight - 185)+'px';
    	const { connectDropTarget , viewMode, topologyId, graphData, getModalScope, setModalContent} = this.props;
    	const { boxes } = this.state;
		return connectDropTarget(
			<div>
				<div className="graph-bg" style={{height: actualHeight}}>
					<TopologyGraphComponent 
						height={parseInt(actualHeight, 10)}
						data={graphData}
						topologyId={topologyId}
						viewMode={viewMode}
						getModalScope={getModalScope}
						setModalContent={setModalContent}
					/>
				</div>
				{!viewMode && state.showComponentNodeContainer ? 
					<ComponentNodeContainer 
			        	left={boxes.left}
		                top={boxes.top}
		                hideSourceOnDrag={true}
		                viewMode={viewMode}
		                customProcessors={this.props.customProcessors}
					/>
				: null}
			</div>
		)
	}
}

@observer
export default class TopologyEditorContainer extends Component {
	constructor(props){
		super(props);
		this.breadcrumbData = {
			title: 'Topology Editor',
			linkArr: [
				{title: 'Streams'},
				{title: 'Topology Editor'}
			]
		};
		this.topologyId = this.props.params.id;
		this.fetchData();
	}

	@observable viewMode = true;
	@observable modalTitle = '';
	modalContent = ()=>{};

	showHideComponentNodeContainer(){
		state.showComponentNodeContainer = !state.showComponentNodeContainer;
	}

	state = {
		topologyName: '',
		altFlag: true
	}
	
	fetchData(){
		let promiseArr = [];
		
		promiseArr.push(TopologyREST.getTopology(this.topologyId));
		promiseArr.push(TopologyREST.getSourceComponent());
		promiseArr.push(TopologyREST.getProcessorComponent());
		promiseArr.push(TopologyREST.getSinkComponent());
		promiseArr.push(TopologyREST.getLinkComponent());
		promiseArr.push(TopologyREST.getAllNodes(this.topologyId, 'sources'));
		promiseArr.push(TopologyREST.getAllNodes(this.topologyId, 'processors'));
		promiseArr.push(TopologyREST.getAllNodes(this.topologyId, 'sinks'));
		promiseArr.push(TopologyREST.getAllNodes(this.topologyId, 'edges'));
		promiseArr.push(TopologyREST.getMetaInfo(this.topologyId));
		
		Promise.all(promiseArr)
			.then((resultsArr)=>{
				let allNodes = [];
				let data = resultsArr[0].entity;
				this.topologyName = data.name;
				this.topologyConfig = JSON.parse(data.config);
				
				this.sourceConfigArr = resultsArr[1].entities;
				this.processorConfigArr = resultsArr[2].entities;
				this.sinkConfigArr = resultsArr[3].entities;
				this.linkConfigArr = resultsArr[4].entities;

				this.graphData.linkShuffleOptions = TopologyUtils.setShuffleOptions(this.linkConfigArr);

				let sourcesNode = resultsArr[5].entities || [];
				let processorsNode = resultsArr[6].entities || [];
				let sinksNode = resultsArr[7].entities || [];
				let edgesArr = resultsArr[8].entities || [];

				this.graphData.metaInfo = JSON.parse(resultsArr[9].entity.data);
				
				this.graphData.nodes = TopologyUtils.syncNodeData(sourcesNode, processorsNode, sinksNode, this.graphData.metaInfo);
				
				this.graphData.uinamesList = [];
				this.graphData.nodes.map(node=>{ this.graphData.uinamesList.push(node.uiname); })

				this.graphData.edges = TopologyUtils.syncEdgeData(edgesArr, this.graphData.nodes);
				
				this.setState({topologyName: this.topologyName});
				this.customProcessors = this.getCustomProcessors();
			})
			.catch((err)=>{
				console.error(err);
			});
		this.graphData = { 
			nodes: [], 
			edges: [],
			uinamesList: [],
			graphTransforms: {
				dragCoords: [0,0],
				zoomScale: 1
			},
			linkShuffleOptions: [],
			metaInfo: {
				sources: [],
				processors: [],
				sinks: []
			}
		};
	}
	handleModeChange(value){
		this.viewMode = !this.viewMode;
	}
	showConfig(){
		this.refs.ConfigModal.show();
	}
	handleNameChange(e){
		let name = e.target.value;
		this.validateName(name);
		this.setState({topologyName: name});
	}
	validateName(name){
		if(name === ''){
			this.refs.topologyNameEditable.setState({errorMsg: "Topology name cannot be blank"});
			return false;
		} else if(name.search(' ') !== -1){
			this.refs.topologyNameEditable.setState({errorMsg: "Topology name cannot have space in between"});
			return false;
		} else {
			this.refs.topologyNameEditable.setState({errorMsg: ""});
			return true;
		}
	}
	saveTopologyName(){
		let {topologyName} = this.state;
		if(this.validateName(topologyName)){
			let data = {
				name: topologyName,
				config: JSON.stringify(this.topologyConfig)
			}
			TopologyREST.putTopology(this.topologyId, {body: JSON.stringify(data)})
				.then(topology=>{
					if(topology.responseCode !== 1000){
						FSReactToastr.error(<strong>{topology.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Topology name updated successfully</strong>);
						this.topologyName = topology.entity.name;
						this.topologyConfig = JSON.parse(topology.entity.config);
					}
					this.refs.topologyNameEditable.hideEditor();
				})
		}
	}
	handleEditableReject(){
		this.setState({topologyName: this.topologyName});
		this.refs.topologyNameEditable.setState({errorMsg: ""},()=>{
			this.refs.topologyNameEditable.hideEditor();
		});
	}
	handleSaveConfig(){
		if(this.refs.topologyConfig.validate()){
			this.refs.topologyConfig.handleSave()
				.then(config=>{
					this.refs.ConfigModal.hide();
					if(config.responseCode !== 1000){
						FSReactToastr.error(<strong>{config.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Configuration updated successfully</strong>)
						this.topologyName = config.entity.name;
						this.topologyConfig = JSON.parse(config.entity.config);
						this.setState({topologyName: this.topologyName});
					}
				});
		}
	}
	getModalScope(node){
		let obj = {
			editMode: !this.viewMode,
			topologyId: this.topologyId
		}, config = [];
		switch(node.parentType){
			case Components.Datasource.value:
				config = this.sourceConfigArr.filter((o)=>{ return o.subType === 'KAFKA'})
				if(config.length > 0) config = config[0];
				obj.configData = config;
			break;
			case Components.Processor.value:
				config = this.processorConfigArr.filter((o)=>{ return o.subType === node.currentType.toUpperCase()})
				//Check for custom processor
				if(node.currentType.toLowerCase() === Components.Processors[2].name.toLowerCase()){
					let index = null;
					config.map((c,i)=>{
						let configArr = JSON.parse(c.config);
						configArr.map(o=>{
							if(o.name === 'name' && o.defaultValue === node.uiname){
								index = i;
							}
						})
					})
					if(index !== null){
						config = config[index];
					} else {
						console.error("Not able to get Custom Processor Configurations");
					}
				} else {
					//For all the other processors except CP
					if(config.length > 0) config = config[0];
				}
				obj.configData = config;
			break;
			case Components.Sink.value:
				config = this.sinkConfigArr.filter((o)=>{ return o.subType === node.currentType.toUpperCase()})
				if(config.length > 0) config = config[0];
				obj.configData = config;
			break;
		}
		return obj;
	}
	deployTopology(){
		this.refs.BaseContainer.refs.Confirm.show({
			title: 'Are you sure you want to deploy this topology ?'
		}).then((confirmBox)=>{
			TopologyREST.validateTopology(this.topologyId)
				.then(result=>{
					if(result.responseCode !== 1000){
						FSReactToastr.error(<strong>{result.responseMessage}</strong>);
					} else {
						TopologyREST.deployTopology(this.topologyId)
							.then(topology=>{
								if(topology.responseCode !== 1000){
									FSReactToastr.error(<strong>{topology.responseMessage}</strong>);
								} else {
									FSReactToastr.success(<strong>Topology Deployed Successfully</strong>);
								}
							})
					}
					confirmBox.cancel();
				})
		},()=>{})
	}
	killTopology(){
		this.refs.BaseContainer.refs.Confirm.show({
			title: 'Are you sure you want to kill this topology ?'
		}).then((confirmBox)=>{
			TopologyREST.killTopology(this.topologyId)
				.then(topology=>{
					if(topology.responseCode !== 1000){
						FSReactToastr.error(<strong>{topology.responseMessage}</strong>);
					} else {
						FSReactToastr.success(<strong>Topology Killed Successfully</strong>);
					}
					confirmBox.cancel();
				})
		},()=>{})
	}
	setModalContent(node, updateGraphMethod, content){
		if(typeof content === 'function'){
			this.modalContent = content;
			this.setState({altFlag: !this.state.altFlag},()=>{
				this.node = node;
				this.modalTitle = this.node.uiname;
				this.refs.NodeModal.show();
				this.updateGraphMethod = updateGraphMethod;
			});
		}
	}
	handleSaveNodeName(editable) {
		if(this.validateNodeName(this.modalTitle))
			editable.hideEditor();
	}
	handleRejectNodeName(editable) {
		this.modalTitle = this.node.uiname;
		editable.hideEditor();
	}
	handleNodeNameChange(e) {
		let isValid = this.validateNodeName(e.target.value);
		this.modalTitle = e.target.value;
	}
	validateNodeName(name) {
		let nodeNamesList = this.graphData.uinamesList;
		if(name === ''){
			this.refs.editableNodeName.setState({errorMsg: "Node name cannot be blank"});
			return false;
		} else if(name.search(' ') !== -1){
			this.refs.editableNodeName.setState({errorMsg: "Node name cannot have space in between"});
			return false;
		} else if(nodeNamesList.indexOf(name) !== -1){
			this.refs.editableNodeName.setState({errorMsg: "Node name is already present. Please use some other name."});
			this.validateFlag = false;
			return false;
		} else {
			this.refs.editableNodeName.setState({errorMsg: ""});
			return true;
		}
	}
	handleSaveNodeModal(){
		if(!this.viewMode){
			if(this.refs.ConfigModal.validateData()){
				//Make the save request
				this.refs.ConfigModal.handleSave(this.modalTitle).then((savedNode)=>{
					if(savedNode instanceof Array){
						savedNode = savedNode[0];
					}
					if(savedNode.responseCode !== 1000){
						FSReactToastr.error(<strong>{savedNode.responseMessage}</strong>);
					} else {
						this.node.isConfigured = true;
						let i = this.graphData.uinamesList.indexOf(this.node.uiname);
						this.node.uiname = savedNode.entity.name;
						if(i > -1)
							this.graphData.uinamesList[i] = this.node.uiname;

						//Show notifications from the view
						FSReactToastr.success(<strong>{this.node.uiname} updated successfully.</strong>);
						//render graph again
						this.updateGraphMethod();
					}
					this.refs.NodeModal.hide();
				})
			}
		} else {
			this.refs.NodeModal.hide();
		}
	}
	getCustomProcessors() {
		return this.processorConfigArr.filter((o)=>{ return o.subType === 'CUSTOM'});
	}
 	render() {
 		let nodeType = this.node ? this.node.currentType : '';
	    return (
	        <BaseContainer ref="BaseContainer" routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
	            <div className="row">
	            	<div className="col-sm-12">
	            		<div className="box">
	            			<div className="box-head">
								{!this.viewMode ? 
									<Editable id="topologyName" 
										ref="topologyNameEditable" 
										inline={false} 
										resolve={this.saveTopologyName.bind(this)}
										reject={this.handleEditableReject.bind(this)}
									>
										<input defaultValue={this.state.topologyName} onChange={this.handleNameChange.bind(this)}/>
									</Editable>
								: 
									<span style={{color: "#43a047"}}>{this.state.topologyName}</span>
								}
				                {!this.viewMode ?
				                    <div className="box-controls">
				                        <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Run</Tooltip>}>
											<a href="javascript:void(0);" className="play" onClick={this.deployTopology.bind(this)}><i className="fa fa-play"></i></a>
				                        </OverlayTrigger>

				                        <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Kill</Tooltip>}>
											<a href="javascript:void(0);" className="remove" onClick={this.killTopology.bind(this)}><i className="fa fa-times"></i></a>
				                        </OverlayTrigger>
				                    </div>
	            				: null}
	            				{!this.viewMode ? 
	            					<div className="box-controls">
										{!this.viewMode && !state.showComponentNodeContainer ? 
											<OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Components</Tooltip>}>
												<a href="javascript:void(0);" className="show-node-list" onClick={this.showHideComponentNodeContainer.bind(this)}><i className="fa fa-th-list"></i></a>
											</OverlayTrigger>
										: null}
				                        
				                        <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Configure</Tooltip>}>
				                        	<a href="javascript:void(0);" className="config" onClick={this.showConfig.bind(this)}><i className="fa fa-gear"></i></a>
				                        </OverlayTrigger>
				                    </div>
				                : null}
			                    <div className="box-controls">
					                <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">Switch to Edit Mode</Tooltip>}>
						                <Switch
						                	onText="View Mode"
						                	offText="Edit Mode"
						                	offColor="success"
						                	onColor="default"
						                	state={this.viewMode}
						                	onChange={this.handleModeChange.bind(this)}
						                	size="mini"
						                	handleWidth={70}
						                />
						            </OverlayTrigger>
					            </div>
	            			</div>
	            			<EditorGraph 
	            				graphData={this.graphData}
	            				viewMode={this.viewMode}
	            				topologyId={this.topologyId}
	            				getModalScope={this.getModalScope.bind(this)}
	            				setModalContent={this.setModalContent.bind(this)}
	            				customProcessors={this.customProcessors}
	            			/>
	            		</div>
	            	</div>
	            </div>
	            <Modal ref="ConfigModal" data-title="Topology Configuration" data-resolve={this.handleSaveConfig.bind(this)}>
					<TopologyConfig ref="topologyConfig" topologyId={this.topologyId} data={this.topologyConfig} topologyName={this.state.topologyName}/>
				</Modal>
	            <Modal ref="NodeModal"
						bsSize="large"
						data-title={
						this.viewMode?
						this.modalTitle
						: (nodeType === "Custom" ? this.modalTitle : (<Editable
								ref="editableNodeName"
								inline={true}
								resolve={this.handleSaveNodeName.bind(this)}
								reject={this.handleRejectNodeName.bind(this)}
								>
									<input defaultValue={this.modalTitle} onChange={this.handleNodeNameChange.bind(this)}/>
							</Editable>)
						)
					}
					data-resolve={this.handleSaveNodeModal.bind(this)}>
	            	{this.modalContent()}
	            </Modal>
	        </BaseContainer>
	    )
	}
}
