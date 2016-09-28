import React from 'react';
import _ from 'lodash';
import {Components} from './Constants';
import TopologyREST from '../rest/TopologyREST';
import FSReactToastr from '../components/FSReactToastr';
//Sources
import DeviceNodeForm from '../containers/Streams/TopologyEditor/DeviceNodeForm';
import KafkaNodeForm from '../containers/Streams/TopologyEditor/KafkaNodeForm';
//Processors
import ParserNodeForm from '../containers/Streams/TopologyEditor/ParserNodeForm';
import RulesNodeForm from '../containers/Streams/TopologyEditor/RulesNodeForm';
import SplitNodeForm from '../containers/Streams/TopologyEditor/SplitNodeForm';
import StageNodeForm from '../containers/Streams/TopologyEditor/StageNodeForm';
import JoinNodeForm from '../containers/Streams/TopologyEditor/JoinNodeForm';
import CustomNodeForm from '../containers/Streams/TopologyEditor/CustomNodeForm';
import NormalizationNodeForm from '../containers/Streams/TopologyEditor/NormalizationNodeForm';
//Sinks
import HdfsNodeForm from '../containers/Streams/TopologyEditor/HdfsNodeForm';
import HbaseNodeForm from '../containers/Streams/TopologyEditor/HbaseNodeForm'
import NotificationNodeForm from '../containers/Streams/TopologyEditor/NotificationNodeForm'

const defineMarkers = function(svg){
	// define arrow markers for graph links
	let defs = svg.append('svg:defs')
	
	defs.append('svg:marker')
		.attr('id', 'end-arrow')
		.attr('viewBox', '0 -5 10 10')
		.attr('refX', "10")
		.attr('markerWidth', 6.5)
		.attr('markerHeight', 7.5)
		.attr('orient', 'auto')
		.append('svg:path')
		.attr('d', 'M0 -5 L10 0 L0 5')

	// define arrow markers for leading arrow
	defs.append('svg:marker')
		.attr('id', 'mark-end-arrow')
		.attr('viewBox', '0 -5 10 10')
		.attr('refX', 7)
		.attr('markerWidth', 6.5)
		.attr('markerHeight', 7.5)
		.attr('orient', 'auto')
		.append('svg:path')
		.attr('d', 'M0 -5 L10 0 L0 5')

	// define filter for gray(unconfigured) icons 
	defs.append('svg:filter')
		.attr('id', 'grayscale')
		.append('feColorMatrix')
			.attr('type', 'saturate')
			.attr('values', '0');
}

const isValidConnection = function(sourceNode, targetNode){
	let sourceType = sourceNode.currentType,
		sourceParent = sourceNode.parentType,
		targetType = targetNode.currentType,
		targetParent = targetNode.parentType;

	let resultObj = Components[sourceParent+'s'].filter((node)=>{
		if(node.name === sourceType) return node;
	});

	if(resultObj.length > 0){
		return resultObj[0].connectsTo.includes(targetType);
	} else {
		return false;
	}
}

const createNode = function(topologyId, data, callback, metaInfo, paths, edges, internalFlags, uinamesList){
	let promiseArr = [];
	
	data.map((o)=>{
		let nodeType = this.getNodeType(o.parentType);

		//Dynamic Names of nodes
		while(uinamesList.indexOf(o.uiname) !== -1){
			let arr = o.uiname.split('-');
			let count = 1;
			if(arr.length > 1){
				count = parseInt(arr[1], 10) + 1;
			}
			o.uiname = arr[0]+'-'+count;
		}
		uinamesList.push(o.uiname);
		//
		//
		let obj = {
			name: o.uiname,
			config: {},
			type: (o.currentType === Components.Datasources[0].name ? "KAFKA" : o.currentType.toUpperCase())
		}
		if(o.parentType === Components.Processor.value){
			obj["outputStreamIds"] = [];
		}
		promiseArr.push(TopologyREST.createNode(topologyId, nodeType, {body: JSON.stringify(obj)}));
	});
	
	//Make calls to create node or nodes
	Promise.all(promiseArr)
		.then((results)=>{

			results.map((o,i)=>{
				if(o.responseCode !== 1000){
					FSReactToastr.error(<strong>{o.responseMessage}</strong>);
				} else {
					data[i].nodeId = o.entity.id;
				}
				if(i > 0){
					//Creating edge link
					this.createEdge(data[i-1], data[i], paths, edges, internalFlags, callback, topologyId);
				}
			});

			this.saveMetaInfo(topologyId, data, metaInfo, callback);
		})
		.catch((err)=>{
			console.error(err);
		})

}

const saveMetaInfo = function(topologyId, nodes, metaInfo, callback){
	nodes.map((o)=>{
		let obj = {
			x: o.x,
			y: o.y,
			id: o.nodeId
		};
		metaInfo[this.getNodeType(o.parentType)].push(obj);
	})

	let data = {
		topologyId: topologyId,
		data: JSON.stringify(metaInfo)
	};

	TopologyREST.putMetaInfo(topologyId, {body: JSON.stringify(data)})
		.then(()=>{
			//call the callback to update the graph
			callback();
		})
		.catch((err)=>{
			console.error(err);
		})
}

const updateMetaInfo = function(topologyId, node, metaInfo){
	let metaArr = metaInfo[this.getNodeType(node.parentType)];
	let oldMetaObj = metaArr.filter((o)=>{return o.id === node.nodeId});
	if(oldMetaObj.length !== 0){
		oldMetaObj = oldMetaObj[0];
		oldMetaObj.x = node.x;
		oldMetaObj.y = node.y;
		if(node.streamId === 'failedTuplesStream'){
			oldMetaObj.streamId = node.streamId;
		} else {
			delete oldMetaObj.streamId;
		}
		let data = { topologyId: topologyId, data: JSON.stringify(metaInfo) };
		TopologyREST.putMetaInfo(topologyId, {body: JSON.stringify(data)});
	}
}

const removeNodeFromMeta = function(metaInfo, currentNode){
	let currentType = this.getNodeType(currentNode.parentType);
	let arr = metaInfo[currentType];
	let nodeMeta = arr.filter((o)=>{ return o.id === currentNode.nodeId});
	nodeMeta.map((o)=>{
		arr.splice(arr.indexOf(o), 1);
	})
	return metaInfo;
}

const createEdge = function(mouseDownNode, d, paths, edges, internalFlags, callback, topologyId){
	if(this.isValidConnection(mouseDownNode, d)){
		let newEdge = {
			source: mouseDownNode,
			target: d
		};
		let filtRes = paths.filter(function(d) {
			if (d.source === newEdge.target && d.target === newEdge.source) {
				edges.splice(edges.indexOf(d), 1);
			}
			return d.source === newEdge.source && d.target === newEdge.target;
		});
		if (!filtRes[0].length) {
			if(newEdge.source.currentType === Components.Processors[0].name){
				if(internalFlags.failedTupleDrag){
					newEdge.target.streamId = "failedTuplesStream";
				} else {
					newEdge.target.streamId = "parsedTuplesStream";
				}
			}
			let data = {
				fromId: newEdge.source.nodeId,
				toId: newEdge.target.nodeId,
				streamGroupings: []
			};

			//Creating edges without stream grouping
			TopologyREST.createNode(topologyId, 'edges', {body: JSON.stringify(data)})
				.then((edge)=>{
					newEdge.edgeId = edge.entity.id;
					edges.push(newEdge);
					//call the callback to update the graph
					callback();
				})
				.catch((err)=>{
					console.error(err);
				})
		}
	} else {
		FSReactToastr.error(<strong>{mouseDownNode.currentType} cannot be connected to {d.currentType}</strong>);
	}
}

const getNodeType = function(parentType){
	switch(parentType){
		case Components.Datasource.value:
			return 'sources'
		break;
		case Components.Processor.value:
			return 'processors'
		break;
		case Components.Sink.value:
			return 'sinks'
		break;
	}
}

const deleteNode = function(topologyId, currentNode, nodes, edges, internalFlags, updateGraphMethod, metaInfo, uinamesList){
	let promiseArr = [],
		nodePromiseArr = [],
		callback = null,
		currentType = currentNode.currentType;

	if(currentType === Components.Processors[0].name){
		//Check for parser
		FSReactToastr.warning(<strong>Parser can only be deleted if Device is deleted</strong>);
	} else if(currentType === Components.Processors[6].name){
		//Check for join
		FSReactToastr.warning(<strong>Join can only be deleted if Split is deleted</strong>);
	} else {
		//Check for stage to not allow delete if only one stage is present
		if(currentType === Components.Processors[5].name){
			let stageProcessors = nodes.filter((o)=>{return o.currentType === currentType});
			if(stageProcessors.length === 1){
				FSReactToastr.warning(<strong>Stage can only be deleted if Split is deleted</strong>);
				return false;
			}
		}

		//Get data of current node
		nodePromiseArr.push(TopologyREST.getNode(topologyId, this.getNodeType(currentNode.parentType), currentNode.nodeId))

		//Get data of connected nodes
		//Incase of source => get Parser
		//Incase of split => get all stage and only one join
		if(currentType === Components.Datasources[0].name || currentType === Components.Processors[4].name){
			let connectingEdges = edges.filter((obj)=>{ return obj.source == currentNode; });
			connectingEdges.map((o, i)=>{
				nodePromiseArr.push(TopologyREST.getNode(topologyId, this.getNodeType(o.target.parentType), o.target.nodeId))
				if(i === 0 && currentType === Components.Processors[4].name){
					//All stages connects to only one Join
					let stageJoinNodes = edges.filter((obj)=>{ return obj.source == o.target; });
					nodePromiseArr.push(TopologyREST.getNode(topologyId, this.getNodeType(stageJoinNodes[0].target.parentType), stageJoinNodes[0].target.nodeId))
				}
			})
		}

		Promise.all(nodePromiseArr)
			.then(results=>{
				let nodeData = results[0].entity;
				//Delete streams of all nodes
				results.map(result=>{
					let node = result.entity;
					if(node.outputStreams){
						node.outputStreams.map(stream=>{
							if(stream.id){
								promiseArr.push(TopologyREST.deleteNode(topologyId, 'streams', stream.id));
							}
						})
					}
				})

				
				//Delete Rules incase of Rule Processor
				if(nodeData.type === 'RULE'){
					if(nodeData.config.properties.rules){
						nodeData.config.properties.rules.map(ruleId=>{
							promiseArr.push(TopologyREST.deleteNode(topologyId, 'rules', ruleId));
						})
					}
				}

				//Remove metadata of to-be-deleted node
				//Make delete call
				function performAction(metaInfo, edgeTargetObj, promiseArr, topologyId, targetArr){
					metaInfo = this.removeNodeFromMeta(metaInfo, edgeTargetObj);
					promiseArr.push(TopologyREST.deleteNode(topologyId, this.getNodeType(edgeTargetObj.parentType), edgeTargetObj.nodeId));
					targetArr.push(edgeTargetObj);
				}
				//Delete other related nodes
				//For Device => Parser
				//For Split => Stage and Join
				let targetArr = [];
				if(currentType === Components.Datasources[0].name || currentType === Components.Processors[4].name){
					let connectingEdges = edges.filter((obj)=>{ return obj.source == currentNode; });
					connectingEdges.map((o, i)=>{
						performAction.call(this, metaInfo, o.target, promiseArr, topologyId, targetArr);
						if(i === 0 && currentType === Components.Processors[4].name){
							//All stages connects to only one Join
							let stageJoinNodes = edges.filter((obj)=>{ return obj.source == o.target; });
							performAction.call(this, metaInfo, stageJoinNodes[0].target, promiseArr, topologyId, targetArr);
						}
					})
				}
				
				//Delete Links
				let edgeArr = this.getEdges(edges, currentNode);
				targetArr.map((o)=>{
					let removeEdges = this.getEdges(edges, o);
					removeEdges.map((o)=>{
						let temp = edgeArr.filter((l)=>{return l.edgeId === o.edgeId});
						if(temp.length === 0){
							edgeArr.push(o);
						}
					});
				})
				edgeArr.map((o)=>{
					promiseArr.push(TopologyREST.deleteNode(topologyId, 'edges', o.edgeId));
				});

				//Delete data from metadata
				metaInfo = this.removeNodeFromMeta(metaInfo, currentNode);
				let metaData = {
					topologyId: topologyId,
					data: JSON.stringify(metaInfo)
				};
				promiseArr.push(TopologyREST.putMetaInfo(topologyId, {body: JSON.stringify(metaData)}));
				
				//Delete current node 
				promiseArr.push(TopologyREST.deleteNode(topologyId, this.getNodeType(currentNode.parentType), currentNode.nodeId));

				//If needed to reset any processor on delete - it comes here or in callback
				callback = function(){
					// Graph related Operations
					uinamesList.splice(uinamesList.indexOf(currentNode.uiname), 1);
					nodes.splice(nodes.indexOf(currentNode), 1);
					this.spliceLinksForNode(currentNode, edges);
					targetArr.map((o)=>{
						uinamesList.splice(uinamesList.indexOf(o.uiname), 1);
						nodes.splice(nodes.indexOf(o), 1);
						this.spliceLinksForNode(o, edges);
					})
					internalFlags.selectedNode = null;
					updateGraphMethod();
				}.bind(this)

				if(promiseArr.length > 0){
					//Make calls to delete node or nodes
					Promise.all(promiseArr)
						.then((results)=>{
							for(let i = 0; i < results.length; i++){
								if(results[i].responseCode !== 1000){
									FSReactToastr.error(<strong>{results[i].responseMessage}</strong>);
								}
							}
							//call the callback
							callback();
						})
				}

			})
	}
}

const getEdges = function(allEdges, currentNode){
	return allEdges.filter((l)=>{
		return (l.source === currentNode || l.target === currentNode);
	});
}

const deleteEdge = function(selectedEdge, topologyId, internalFlags, edges, updateGraphMethod){
	if(selectedEdge.source.currentType === Components.Datasources[0].name && selectedEdge.target.currentType === Components.Processors[0].name){
		FSReactToastr.warning(<strong>Link between Device and Parser cannot be deleted.</strong>);
	} else {
		let promiseArr = [TopologyREST.deleteNode(topologyId, 'edges', selectedEdge.edgeId)];
		if(selectedEdge.source.currentType === 'Rule'){
			promiseArr.push(TopologyREST.getNode(topologyId, 'processors', selectedEdge.source.nodeId));
		}
		Promise.all(promiseArr)
			.then((results)=>{
				if(results.length === 2){
					//Find the connected source rule
					let rulePromises = [];
					let ruleProcessorNode = results[1].entity;
					if(ruleProcessorNode.config.properties.rules){
						ruleProcessorNode.config.properties.rules.map(ruleId=>{
							rulePromises.push(TopologyREST.getNode(topologyId, 'rules', ruleId));
						})
					}
					Promise.all(rulePromises)
						.then(rulesResults=>{
							rulesResults.map(ruleEntity=>{
								let rule = ruleEntity.entity;
								if(rule.actions){
									//If source rule has target notification inside rule action,
									//then remove and update the rules.
									let index = null;
									rule.actions.map((a, i)=>{
										if(a.name === selectedEdge.target.uiname){
											index = i;
										}
									})
									if(index !== null){
										rule.actions.splice(index, 1);
										TopologyREST.updateNode(topologyId, 'rules', rule.id, {body: JSON.stringify(rule)});
									}
								}
							})
						})
				}
				edges.splice(edges.indexOf(selectedEdge), 1);
				internalFlags.selectedEdge = null;
				updateGraphMethod();
			})
	}
}

// remove edges associated with a node
const spliceLinksForNode = function(node, edges){
	let toSplice = this.getEdges(edges, node);
	toSplice.map(function(l) {
		edges.splice(edges.indexOf(l), 1);
	});
}

const replaceSelectNode = function(d3Node, nodeData, constants, internalFlags, rectangles){
	d3Node.classed(constants.selectedClass, true);
	if (internalFlags.selectedNode) {
		this.removeSelectFromNode(rectangles, constants, internalFlags);
	}
	internalFlags.selectedNode = nodeData;
}

const removeSelectFromNode = function(rectangles, constants, internalFlags){
	rectangles.filter(function(cd) {
		return cd.nodeId === internalFlags.selectedNode.nodeId;
	}).classed(constants.selectedClass, false);
	internalFlags.selectedNode = null;
}


const replaceSelectEdge = function(d3, d3Path, edgeData, constants, internalFlags, paths){
	d3Path.classed(constants.selectedClass, true);
	if (internalFlags.selectedEdge) {
		this.removeSelectFromEdge(d3, paths, constants, internalFlags);
	}
	internalFlags.selectedEdge = edgeData;
}

const removeSelectFromEdge = function(d3, paths, constants, internalFlags) {
	let path = paths.filter(function(cd) {
		return cd === internalFlags.selectedEdge;
	});
	let selectedPath = path[0][0];
	d3.select(selectedPath.previousSibling).classed(constants.selectedClass, false);
	internalFlags.selectedEdge = null;
}

const defineLinePath = function(p1, p2, flag){
	let segments = [],
		sourceX = p1.x,
		sourceY = p1.y,
		targetX = p2.x,
		targetY = p2.y;

	segments.push("M"+sourceX+','+sourceY);
	if(!flag){
		if(sourceX < targetX && sourceY === targetY){
			segments.push("H"+targetX);
		}else if(sourceX > targetX){
			segments.push("H"+(sourceX+20));
			segments.push("V"+((sourceY+targetY)/2));
			segments.push("H"+(targetX-20));
			segments.push("V"+(targetY));
			segments.push("H"+(targetX));
		} else {
			segments.push("H"+((sourceX+targetX)/2));
			segments.push("V"+(targetY));
			segments.push("H"+(targetX));
	  	}
	}else{
		segments.push("V"+(targetY));
		segments.push("H"+(targetX));
	}
	return segments.join(' ');
}

const showNodeModal = function(ModalScope, setModalContent, node, updateGraphMethod, allNodes, edges, linkShuffleOptions){
	let currentEdges = this.getEdges(edges, node);
	let scope = ModalScope(node);
	setModalContent(node, updateGraphMethod, this.getConfigContainer(node, scope.configData, scope.editMode, scope.topologyId, currentEdges, allNodes, linkShuffleOptions));
}

const getConfigContainer = function(node, configData, editMode, topologyId, currentEdges, allNodes, linkShuffleOptions){
	let nodeType = this.getNodeType(node.parentType);
	let sourceNodes = [], targetNodes = [];
	currentEdges.map((e)=>{ 
		if(e.target.nodeId === node.nodeId){
			//find source node of parser
			sourceNodes.push(e.source);
		} else if(e.source.nodeId === node.nodeId){
			//find target node of parser
			targetNodes.push(e.target)
		}
	});
	switch(node.currentType){
		case Components.Datasources[0].name: //Device
		return () => {
			return <DeviceNodeForm 
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
				/>;
		}
		break;
		case Components.Datasources[1].name: //Kafka
		return () => {
			return <KafkaNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
				/>;
		}
		break;
		case Components.Processors[0].name: //Parser
			return ()=>{
				return <ParserNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					sourceNode={sourceNodes[0]}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
				/>;
			}
		break;
		case Components.Processors[1].name: //Rule
			return ()=>{
				return <RulesNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					sourceNode={sourceNodes}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
				/>;
			}
		break;
		case Components.Processors[2].name: //Custom
			return ()=>{
				return <CustomNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					sourceNode={sourceNodes[0]}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
				/>;
			}
		break;
		case Components.Processors[3].name: //Normalization
			return ()=>{
				return <NormalizationNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
					currentEdges={currentEdges}
				/>;
			}
		break;
		case Components.Processors[4].name: //Split
			return ()=>{
				return <SplitNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					sourceNode={sourceNodes[0]}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
				/>;
			}
		break;
		case Components.Processors[5].name: //Stage
			return ()=>{
				return <StageNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
					currentEdges={currentEdges}
				/>;
			}
		break;
		case Components.Processors[6].name: //Join
			return ()=>{
				return <JoinNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					sourceNode={sourceNodes[0]}
					targetNodes={targetNodes}
					linkShuffleOptions={linkShuffleOptions}
				/>;
			}
		break;
		case Components.Sinks[0].name: //Hdfs
			return ()=>{
				return <HdfsNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
				/>
			}
		break;
		case Components.Sinks[1].name: //Hbase
			return ()=>{
				return <HbaseNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
				/>
			}
		break;
		case Components.Sinks[2].name: //Notification
			return ()=>{
				return <NotificationNodeForm
					ref="ConfigModal"
					nodeData={node}
					configData={configData}
					editMode={editMode}
					nodeType={nodeType}
					topologyId={topologyId}
					sourceNodes={sourceNodes}
				/>
			}
		break;
	}
}

const MouseUpAction = function(topologyId, d3node, d, metaInfo, internalFlags, constants, dragLine, paths, allNodes, edges, linkShuffleOptions, updateGraphMethod, elementType, getModalScope, setModalContent, rectangles){
	// reset the internalFlags
	internalFlags.shiftNodeDrag = false;
	d3node.classed(constants.connectClass, false);

	var mouseDownNode = internalFlags.mouseDownNode;

	// if (!mouseDownNode) return;

	dragLine.classed("hidden", true);

	if (mouseDownNode && mouseDownNode !== d) {
		// we're in a different node: create new edge for mousedown edge and add to graph
		this.createEdge(mouseDownNode, d, paths, edges, internalFlags, updateGraphMethod, topologyId);
		this.updateMetaInfo(topologyId, d, metaInfo);
	} else {
		if(elementType === 'rectangle'){
			// we're in the same node
			if (internalFlags.justDragged) {
				// dragged, not clicked
				internalFlags.justDragged = false;
			} else {
				// clicked, not dragged
				if(d3.event && d3.event.type === 'dblclick'){
					this.showNodeModal(getModalScope, setModalContent, d, updateGraphMethod, allNodes, edges, linkShuffleOptions);
				} else {
					// we're in the same node
					if (internalFlags.selectedEdge) {
						this.removeSelectFromEdge(d3, paths, constants, internalFlags);
					}
					var prevNode = internalFlags.selectedNode;

					if (!prevNode || prevNode.nodeId !== d.nodeId) {
						this.replaceSelectNode(d3node, d, constants, internalFlags, rectangles);
					} else {
						this.removeSelectFromNode(rectangles, constants, internalFlags);
					}
				}
			}
		} else if(elementType === 'circle'){
			// we're in the same node
			if (internalFlags.selectedEdge) {
				this.removeSelectFromEdge(d3, paths, constants, internalFlags);
			}
			var prevNode = internalFlags.selectedNode;

			if (!prevNode || prevNode.nodeId !== d.nodeId) {
				this.replaceSelectNode(d3node, d, constants, internalFlags, rectangles);
			} else {
				this.removeSelectFromNode(rectangles, constants, internalFlags);
			}
		}
	}
	internalFlags.failedTupleDrag = false;
	internalFlags.mouseDownNode = null;
	return;
}

const setShuffleOptions = function(linkConfigArr){
	let options = [];
	linkConfigArr.map((o)=>{
		options.push({
			label: o.subType, value: o.subType
		})
	});
	return options;
}

const syncNodeData = function(sources, processors, sinks, metadata){
	let nodeArr = [];
	this.generateNodeData(sources, Components.Datasources, Components.Datasource.value, metadata.sources, nodeArr);
	this.generateNodeData(processors, Components.Processors, Components.Processor.value, metadata.processors, nodeArr);
	this.generateNodeData(sinks, Components.Sinks, Components.Sink.value, metadata.sinks, nodeArr);
	return nodeArr;
}

const capitalizeFirstLetter = function(string){
	string = string.toLowerCase();
	return string.charAt(0).toUpperCase() + string.slice(1);
}

const generateNodeData = function(nodes, constantsArr, parentType, metadata, resultArr){
	for(let i = 0; i < nodes.length; i++){
		let currentType = this.capitalizeFirstLetter(nodes[i].type);
		let constObj = constantsArr.filter((o)=>{return o.name === currentType});
		let configuredFlag = _.keys(nodes[i].config.properties).length > 0 ? true : false;
		if(constObj.length === 0){
			console.error(currentType+" is not present in our application");
		} else {
			constObj = constObj[0];
		}

		let currentMetaObj = metadata.filter((o)=>{return o.id === nodes[i].id});
		if(currentMetaObj.length === 0){
			console.error("Failed to get meta data");
		} else {
			currentMetaObj = currentMetaObj[0];
		}
		
		let obj = {
			x: currentMetaObj.x,
			y: currentMetaObj.y,
			nodeId: nodes[i].id,
			parentType: parentType,
			currentType: currentType,
			uiname: nodes[i].name,
			imageURL: constObj.imgPath,
			isConfigured: configuredFlag
		}
		if(currentMetaObj.streamId){
			obj.streamId = currentMetaObj.streamId;
		}
		resultArr.push(obj);
	}
}

const syncEdgeData = function(edges, nodes){
	let edgesArr = [];
	edges.map((edge)=>{
		//Find source node
		let fromNode = nodes.filter((o)=>{ return o.nodeId === edge.fromId});
		if(fromNode.length !== 0) 
			fromNode = fromNode[0];
		else console.error("From node is missing");

		//Find target node
		let toNode = nodes.filter((o)=>{ return o.nodeId === edge.toId});
		if(toNode.length !== 0) 
			toNode = toNode[0];
		else console.error("To node is missing");

		edgesArr.push({
			source: fromNode,
			target: toNode,
			edgeId: edge.id
		});
	})
	return edgesArr;
}

const createLineOnUI = function(edge, constants){
	let arr = [],
		isFailedTupleflag = false;
	if(edge.target.streamId === "failedTuplesStream"){
		arr.push({x: (edge.source.x + constants.rectangleWidth / 2),y: (edge.source.y + constants.rectangleHeight)},
				 {x: edge.target.x, y: (edge.target.y + constants.rectangleHeight / 2)});
		isFailedTupleflag = true;
	} else {
		arr.push({x: (edge.source.x + constants.rectangleWidth),y: (edge.source.y + constants.rectangleHeight / 2)},
				 {x: edge.target.x, y: (edge.target.y + constants.rectangleHeight / 2)});
	}
	return this.defineLinePath(arr[0], arr[1], isFailedTupleflag);
}

const getNodeRectClass = function(data){
	if(data.parentType === Components.Datasource.value){
		return 'source';
	} else if(data.parentType === Components.Processor.value){
		return 'processor';
	} else if(data.parentType === Components.Sink.value){
		return 'datasink';
	}
}

const getNodeImgRectClass = function(data){
	if(data.parentType === Components.Datasource.value){
		return 'source-img';
	} else if(data.parentType === Components.Processor.value){
		return 'processor-img';
	} else if(data.parentType === Components.Sink.value){
		return 'datasink-img';
	}
}

export default {
	defineMarkers,
	isValidConnection,
	createNode,
	saveMetaInfo,
	updateMetaInfo,
	removeNodeFromMeta,
	createEdge,
	getNodeType,
	deleteNode,
	deleteEdge,
	getEdges,
	spliceLinksForNode,
	replaceSelectNode,
	removeSelectFromNode,
	replaceSelectEdge,
	removeSelectFromEdge,
	defineLinePath,
	showNodeModal,
	getConfigContainer,
	MouseUpAction,
	setShuffleOptions,
	syncNodeData,
	capitalizeFirstLetter,
	generateNodeData,
	syncEdgeData,
	createLineOnUI,
	getNodeRectClass,
	getNodeImgRectClass
};