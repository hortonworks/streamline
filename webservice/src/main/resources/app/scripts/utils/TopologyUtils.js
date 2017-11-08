/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
import React from 'react';
import _ from 'lodash';
import {Components, toastOpt} from './Constants';
import d3 from 'd3';
import TopologyREST from '../rest/TopologyREST';
import FSReactToastr from '../components/FSReactToastr';
//Sources
import SourceNodeForm from '../containers/Streams/TopologyEditor/SourceNodeForm';
//Processors
import ProcessorNodeForm from '../containers/Streams/TopologyEditor/ProcessorNodeForm';
import RulesNodeForm from '../containers/Streams/TopologyEditor/RulesNodeForm';
import SplitNodeForm from '../containers/Streams/TopologyEditor/SplitNodeForm';
import StageNodeForm from '../containers/Streams/TopologyEditor/StageNodeForm';
import JoinNodeForm from '../containers/Streams/TopologyEditor/JoinNodeForm';
import CustomNodeForm from '../containers/Streams/TopologyEditor/CustomNodeForm';
import NormalizationNodeForm from '../containers/Streams/TopologyEditor/NormalizationNodeForm';
import WindowingAggregateNodeForm from '../containers/Streams/TopologyEditor/WindowingAggregateNodeForm';
import BranchNodeForm from '../containers/Streams/TopologyEditor/BranchNodeForm';
import ModelNodeForm from '../containers/Streams/TopologyEditor/ModelNodeForm';
import ProjectionProcessorContainer from '../containers/Streams/TopologyEditor/ProjectionProcessorContainer';
import RealTimeJoinNodeProcessor from '../containers/Streams/TopologyEditor/RealTimeJoinNodeProcessor';
//Sinks
import SinkNodeForm from '../containers/Streams/TopologyEditor/SinkNodeForm';
import CommonNotification from './CommonNotification';
import {deleteNodeIdArr} from '../utils/Constants';

const defineMarkers = function(svg) {
  // define arrow markers for graph links
  let defs = svg.append('svg:defs');

  defs.append('svg:marker').attr('id', 'end-arrow').attr('viewBox', '0 -5 10 10').attr('refX', "14").attr('markerWidth', 6.5).attr('markerHeight', 7.5).attr('orient', 'auto').append('svg:path').attr('d', 'M0 -5 L10 0 L0 5');

  // define arrow markers for leading arrow
  defs.append('svg:marker').attr('id', 'mark-end-arrow').attr('viewBox', '0 -5 10 10').attr('refX', 7).attr('markerWidth', 6.5).attr('markerHeight', 7.5).attr('orient', 'auto').append('svg:path').attr('d', 'M0 -5 L10 0 L0 5');

  // define filter for gray(unconfigured) icons
  defs.append('svg:filter').attr('id', 'grayscale').append('feColorMatrix').attr('type', 'saturate').attr('values', '0');

  // define filter for node shadow
  var filter = defs.append('svg:filter').attr('id', 'dropshadow').attr('x', 0).attr('y', 0).attr('width', '200%').attr('height', '200%');

  filter.append('feOffset').attr('result', 'offOut').attr('in', 'SourceAlpha').attr('dx', -4).attr('dy', -4);
  filter.append('feGaussianBlur').attr('result', 'blurOut').attr('in', 'offOut').attr('stdDeviation', 6);
  filter.append('feBlend').attr('in', 'SourceGraphic').attr('in2', 'blurOut').attr('mode', 'normal');
};

const isValidConnection = function(sourceNode, targetNode) {
  let validConnection = true;
  //       if((sourceNode.currentType.toLowerCase() !== 'split' && targetNode.currentType.toLowerCase() === 'stage') ||
  //               (sourceNode.currentType.toLowerCase() === 'stage' && targetNode.currentType.toLowerCase() !== 'join')
  //         ){
  //               validConnection = false;
  // }
  return validConnection;
};

const createNode = function(topologyId, versionId, data, callback, metaInfo, paths, edges, internalFlags, uinamesList, setLastChange) {
  let promiseArr = [];

  data.map((o) => {
    let nodeType = this.getNodeType(o.parentType);
    let customName = o.uiname;

    //Dynamic Names of nodes
    while (uinamesList.indexOf(o.uiname) !== -1) {
      let arr = o.uiname.split('-');
      let count = 1;
      if (arr.length > 1) {
        count = parseInt(arr[1], 10) + 1;
      }
      o.uiname = arr[0] + '-' + count;
    }
    uinamesList.push(o.uiname);
    //
    //
    if (o.currentType.toLowerCase() === 'custom') {
      if (metaInfo.customNames) {
        metaInfo.customNames.push({uiname: o.uiname, customProcessorName: customName});
      } else {
        metaInfo.customNames = [
          {
            uiname: o.uiname,
            customProcessorName: customName
          }
        ];
      }
    }
    let obj = {
      name: o.uiname,
      config: {},
      topologyComponentBundleId: o.topologyComponentBundleId
    };
    if (o.parentType === 'PROCESSOR') {
      obj["outputStreamIds"] = [];
    }
    promiseArr.push(TopologyREST.createNode(topologyId, versionId, nodeType, {body: JSON.stringify(obj)}));
    if (callback) {
      //call the callback to update the graph
      callback();
    }
  });

  //Make calls to create node or nodes
  Promise.all(promiseArr).then((results) => {

    results.map((o, i) => {
      if (o.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={o.responseMessage}/>, '', toastOpt);
      } else {
        data[i].nodeId = o.id;
      }
      if (i > 0) {
        //Creating edge link
        this.createEdge(data[i - 1], data[i], paths, edges, internalFlags, callback, topologyId, versionId);
      }
    });
    setLastChange(results[0].timestamp);
    this.saveMetaInfo(topologyId, versionId, data, metaInfo, callback);
  });

};

const saveMetaInfo = function(topologyId, versionId, nodes, metaInfo, callback) {
  if (nodes) {
    nodes.map((o) => {
      let obj = {
        x: o.x,
        y: o.y,
        id: o.nodeId
      };
      metaInfo[this.getNodeType(o.parentType)].push(obj);
    });
  }

  let data = {
    topologyId: topologyId,
    data: JSON.stringify(metaInfo)
  };

  TopologyREST.putMetaInfo(topologyId, versionId, {body: JSON.stringify(data)}).then(() => {
    if (callback) {
      //call the callback to update the graph
      callback();
    }
  });
};

const updateMetaInfo = function(topologyId, versionId, node, metaInfo) {
  let metaArr = metaInfo[this.getNodeType(node.parentType)];
  let oldMetaObj = metaArr.filter((o) => {
    return o.id === node.nodeId;
  });
  if (oldMetaObj.length !== 0) {
    oldMetaObj = oldMetaObj[0];
    oldMetaObj.x = node.x;
    oldMetaObj.y = node.y;
    if (node.streamId === 'failedTuplesStream') {
      oldMetaObj.streamId = node.streamId;
    } else {
      delete oldMetaObj.streamId;
    }
  } else {
    metaArr.push({x: node.x, y: node.y, id: node.nodeId});
  }
  let data = {
    topologyId: topologyId,
    data: JSON.stringify(metaInfo)
  };
  TopologyREST.putMetaInfo(topologyId, versionId, {body: JSON.stringify(data)});
};

const removeNodeFromMeta = function(metaInfo, currentNode) {
  let currentType = this.getNodeType(currentNode.parentType);
  let arr = metaInfo[currentType];
  let nodeMeta = arr.filter((o) => {
    return o.id === currentNode.nodeId;
  });
  let customNameArr = metaInfo.customNames;
  nodeMeta.map((o) => {
    arr.splice(arr.indexOf(o), 1);
  });
  if (customNameArr) {
    let customMeta = customNameArr.filter((o) => {
      return o.uiname === currentNode.uiname;
    });
    customMeta.map((o) => {
      customNameArr.splice(customNameArr.indexOf(o), 1);
    });
  }
  return metaInfo;
};

const createEdge = function(mouseDownNode, d, paths, edges, internalFlags, callback, topologyId, versionId, getEdgeConfigModal, setLastChange, drawLine) {
  if (this.isValidConnection(mouseDownNode, d)) {
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
    if (d.currentType.toLowerCase() === 'rule' || d.currentType.toLowerCase() === 'window' || d.currentType.toLowerCase() === 'projection' || d.currentType.toLowerCase() === 'pmml' || d.currentType.toLowerCase() === 'branch' || d.currentType.toLowerCase() === 'custom') {
      let filtEdges = paths.filter(function(d) {
        return newEdge.target === d.target;
      });
      if (filtEdges[0].length > 0) {
        drawLine.classed("hidden", true);
        FSReactToastr.info(
          <CommonNotification flag="error" content={"Cannot connect more than one edge to " + d.uiname}/>, '', toastOpt);
        return;
      }
    }

    if (!filtRes[0].length) {
      TopologyREST.getNode(topologyId, versionId, this.getNodeType(newEdge.source.parentType), newEdge.source.nodeId).then((result) => {
        setLastChange(result.timestamp);
        let nodeData = result;
        if (newEdge.source.currentType.toLowerCase() === 'window' || newEdge.source.currentType.toLowerCase() === 'rule' || newEdge.source.currentType.toLowerCase() === 'projection') {
          nodeData.type = newEdge.source.currentType.toUpperCase();
        }
        drawLine.classed('hidden', true);
        if (getEdgeConfigModal) {
          getEdgeConfigModal(topologyId, versionId, newEdge, edges, callback, nodeData);
        } else {
          console.error("Cannot find getEdgeConfigModal: from createEdge:TopologyUtils");
        }
      });
    }
  } else {
    FSReactToastr.error(
      <CommonNotification flag="error" content={mouseDownNode.currentType + " cannot be connected to " + d.currentType}/>, '', toastOpt);
  }
};

const getNodeType = function(parentType) {
  switch (parentType) {
  case 'SOURCE':
    return 'sources';
    break;
  case 'PROCESSOR':
    return 'processors';
    break;
  case 'SINK':
    return 'sinks';
    break;
  }
};

const spliceDeleteNodeArr = function(nodeId) {
  const index = deleteNodeIdArr.findIndex((x) => {
    return Number(x) === nodeId;
  });
  if (index !== -1) {
    deleteNodeIdArr.splice(index, 1);
  }
};

const deleteNode = function(topologyId, versionId, currentNode, nodes, edges, internalFlags, updateGraphMethod, metaInfo, uinamesList, setLastChange, topologyConfigMessageCB) {
  let promiseArr = [],
    nodePromiseArr = [],
    callback = null,
    currentType = currentNode.currentType;
  deleteNodeIdArr.push(currentNode.nodeId);

  //Get data of current node
  nodePromiseArr.push(TopologyREST.getNode(topologyId, versionId, this.getNodeType(currentNode.parentType), currentNode.nodeId));

  //Find out if the source of the current node is rules/windows
  //then update those processor by removing actions from it.
  let connectingNodes = edges.filter((obj) => {
    return obj.target == currentNode;
  });
  let actionsPromiseArr = [];
  let currentActionType = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
  let streamId = [];
  connectingNodes.map((o, i) => {
    if (o.source.currentType.toLowerCase() === 'rule' || o.source.currentType.toLowerCase() === 'window' ||
        o.source.currentType.toLowerCase() === 'branch' || o.source.currentType.toLowerCase() === 'projection') {
      let t = o.source.currentType.toLowerCase();
      let type = (t === 'rule' || t === "projection")
        ? 'rules'
        : t === 'branch'
          ? 'branchrules'
          : 'windows';
      streamId[i] = o.streamGrouping.streamId;
      let currentNodeEdges = edges.filter((obj) => {
        return obj.source === o.source && streamId[i] === obj.streamGrouping.streamId;
      });
      if (currentNode.currentType.toLowerCase() === 'notification') {
        currentActionType = "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction";
      } else {
        currentActionType = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
      }
      let rulesPromiseArr = [
        TopologyREST.getAllNodes(topologyId, versionId, 'streams'),
        TopologyREST.getAllNodes(topologyId, versionId, type)
      ];

      Promise.all(rulesPromiseArr).then((results) => {
        results[1].entities.map((nodeObj) => {
          let actionsArr = nodeObj.actions,
            actions = [],
            hasAction = false;
          let ruleStream = results[0].entities.find((s) => {
            return s.id === streamId[i];
          });
          actionsArr.map((a) => {
            if (a.outputStreams[0] === ruleStream.streamId && a.__type === currentActionType) {
              hasAction = true;
              if (currentNodeEdges.length > 0) {
                if (currentActionType === 'com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction') {
                  let notifierActionsArr = currentNodeEdges.filter((obj) => {
                    return obj.target !== currentNode && obj.target.currentType.toLowerCase() === 'notification';
                  });
                  if (notifierActionsArr.length > 0) {
                    let targetNode = notifierActionsArr[0].target;
                    let stream = results[0].entities.find((s) => {
                      return s.id === notifierActionsArr[0].streamGrouping.streamId;
                    });
                    actions.push({
                      __type: currentActionType,
                      name: 'notifierAction',
                      outputStreams: [stream.streamId],
                      outputFieldsAndDefaults: {},
                      notifierName: ''
                    });
                  }
                } else if (currentActionType === 'com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction') {
                  let transformActionsArr = currentNodeEdges.filter((obj) => {
                    return obj.target !== currentNode && obj.target.currentType.toLowerCase() !== 'notification';
                  });
                  if (transformActionsArr.length > 0) {
                    let targetNode = transformActionsArr[0].target;
                    let stream = results[0].entities.find((s) => {
                      return s.id === transformActionsArr[0].streamGrouping.streamId;
                    });
                    actions.push({
                      __type: currentActionType,
                      name: 'transformAction',
                      outputStreams: [stream.streamId],
                      transforms: []
                    });
                  }
                }
              }
            } else {
              actions.push(a);
            }
          });
          if (hasAction) {
            nodeObj.actions = actions;
            actionsPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, type, nodeObj.id, {body: JSON.stringify(nodeObj)}));
          }
        });
      });
    }
  });

  Promise.all(actionsPromiseArr).then((results) => {
    for (let i = 0; i < results.length; i++) {
      if (results[i].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={results[i].responseMessage}/>, '', toastOpt);
      }
    }
  });

  Promise.all(nodePromiseArr).then(results => {
    let nodeData = results[0];
    nodeData.type = currentNode.currentType;
    setLastChange(results[0].timestamp);

    callback = function() {
      // Graph related Operations
      uinamesList.splice(uinamesList.indexOf(currentNode.uiname), 1);
      nodes.splice(nodes.indexOf(currentNode), 1);
      this.spliceLinksForNode(currentNode, edges);
      internalFlags.selectedNode = null;
      this.spliceDeleteNodeArr(currentNode.nodeId);
      updateGraphMethod();
      if (topologyConfigMessageCB) {
        topologyConfigMessageCB(currentNode.nodeId);
      }
    }.bind(this);

    //Delete data from metadata
    metaInfo = this.removeNodeFromMeta(metaInfo, currentNode);
    let metaData = {
      topologyId: topologyId,
      data: JSON.stringify(metaInfo)
    };
    promiseArr.push(TopologyREST.putMetaInfo(topologyId, versionId, {body: JSON.stringify(metaData)}));

    //Delete Links
    // let edgeArr = this.getEdges(edges, currentNode);
    //
    // edgeArr.map((o)=>{
    //     promiseArr.push(TopologyREST.deleteNode(topologyId, 'edges', o.edgeId));
    // });

    Promise.all(promiseArr).then((edgeResults) => {
      let edgeAPISuccess = true;
      edgeResults.map((edge) => {
        if (edge.responseMessage !== undefined) {
          edgeAPISuccess = false;
          FSReactToastr.error(
            <CommonNotification flag="error" content={edge.responseMessage}/>, '', toastOpt);
        }
      });
      if (edgeAPISuccess) {
        let processorsPromiseArr = [];
        //Delete Rules incase of Rule Processor or Projection Processor
        if (nodeData.type.toLowerCase() === 'rule' || nodeData.type.toLowerCase() === 'projection') {
          if (nodeData.config.properties.rules) {
            nodeData.config.properties.rules.map(ruleId => {
              processorsPromiseArr.push(TopologyREST.deleteNode(topologyId, 'rules', ruleId));
            });
          }
        }

        //Delete Window incase of Window Processor
        if (nodeData.type.toLowerCase() === 'window') {
          if (nodeData.config.properties.rules) {
            nodeData.config.properties.rules.map(ruleId => {
              processorsPromiseArr.push(TopologyREST.deleteNode(topologyId, 'windows', ruleId));
            });
          }
        }

        //Delete Branch incase of Branch Processor
        if (nodeData.type.toLowerCase() === 'branch') {
          if (nodeData.config.properties.rules) {
            nodeData.config.properties.rules.map(ruleId => {
              processorsPromiseArr.push(TopologyREST.deleteNode(topologyId, 'branchrules', ruleId));
            });
          }
        }

        //Delete current node
        processorsPromiseArr.push(TopologyREST.deleteNode(topologyId, this.getNodeType(currentNode.parentType), currentNode.nodeId));

        Promise.all(processorsPromiseArr).then((processorResult) => {
          let processorAPISuccess = true;
          processorResult.map((processor) => {
            if (processor.responseMessage !== undefined) {
              processorAPISuccess = false;
              FSReactToastr.error(
                <CommonNotification flag="error" content={processor.responseMessage}/>, '', toastOpt);
            }
          });
          if (processorAPISuccess) {
            let streamsPromiseArr = [];
            //Delete streams of all nodes
            results.map(result => {
              let node = result;
              if (node.outputStreams) {
                node.outputStreams.map(stream => {
                  if (stream.id) {
                    streamsPromiseArr.push(TopologyREST.deleteNode(topologyId, 'streams', stream.id));
                  }
                });
              }
            });
            Promise.all(streamsPromiseArr).then((streamsResult) => {
              streamsResult.map((stream) => {
                if (stream.responseMessage !== undefined) {
                  FSReactToastr.error(
                    <CommonNotification flag="error" content={stream.responseMessage}/>, '', toastOpt);
                }
              });
              //call the callback
              callback();
            });
          }
        });
      }
    });
  });

};

const getEdges = function(allEdges, currentNode) {
  return allEdges.filter((l) => {
    return (l.source.nodeId === currentNode.nodeId || l.target.nodeId === currentNode.nodeId);
  });
};

const deleteEdge = function(selectedEdge, topologyId, versionId, internalFlags, edges, nodes, updateGraphMethod, setLastChange) {
  let targetNodeType = selectedEdge.target.parentType === 'PROCESSOR'
    ? 'processors'
    : 'sinks';
  let promiseArr = [
    TopologyREST.deleteNode(topologyId, 'edges', selectedEdge.edgeId),
    TopologyREST.getNode(topologyId, versionId, targetNodeType, selectedEdge.target.nodeId),
    TopologyREST.getAllNodes(topologyId, versionId, 'streams')
  ];
  if (selectedEdge.source.currentType.toLowerCase() === 'rule' || selectedEdge.source.currentType.toLowerCase() === 'window'
      || selectedEdge.source.currentType.toLowerCase() === 'branch' || selectedEdge.source.currentType.toLowerCase() === 'projection') {
    promiseArr.push(TopologyREST.getNode(topologyId, versionId, 'processors', selectedEdge.source.nodeId));
  }
  Promise.all(promiseArr).then((results) => {
    setLastChange(results[0].timestamp);
    if (selectedEdge.target.currentType.toLowerCase() === 'join') {
      let joinProcessorNode = results[1];
      if (_.keys(joinProcessorNode.config.properties).length > 0) {
        joinProcessorNode.config.properties.joins = [];
        joinProcessorNode.config.properties.from = {};
        TopologyREST.updateNode(topologyId, versionId, 'processors', joinProcessorNode.id, {body: JSON.stringify(joinProcessorNode)});
      }
    }
    if (results.length === 4) {
      //Find the connected source rule/window
      let rulePromises = [];
      let ruleProcessorNode = results[3];
      let ruleStream = results[2].entities.find((s) => {
        return s.id === selectedEdge.streamGrouping.streamId;
      });
      let currentNodeEdges = edges.filter((obj) => {
        return obj.source === selectedEdge.source && obj.streamGrouping.streamId === selectedEdge.streamGrouping.streamId;
      });
      let currentActionType = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
      let t = selectedEdge.source.currentType.toLowerCase();
      let type = t === 'window'
        ? 'windows'
        : (t === 'rule' || t === 'projection'
          ? 'rules'
          : 'branchrules');
      if (ruleProcessorNode.config.properties.rules) {
        ruleProcessorNode.config.properties.rules.map(ruleId => {
          rulePromises.push(TopologyREST.getNode(topologyId, versionId, type, ruleId));
        });
      }
      if (selectedEdge.target.currentType.toLowerCase() === 'notification') {
        currentActionType = "com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction";
      } else {
        currentActionType = "com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction";
      }
      Promise.all(rulePromises).then(rulesResults => {
        rulesResults.map(ruleEntity => {
          let rule = ruleEntity;
          if (rule.actions) {
            //If source rule has target notification inside rule action,
            //then remove and update the rules/window.
            let index = null;
            let actionObj = null;
            rule.actions.map((a, i) => {
              if (a.outputStreams[0] === ruleStream.streamId && a.__type === currentActionType) {
                index = i;
                if (currentNodeEdges.length > 0) {
                  if (currentActionType === 'com.hortonworks.streamline.streams.layout.component.rule.action.NotifierAction') {
                    let notifierActionsArr = currentNodeEdges.filter((obj) => {
                      return obj.target !== selectedEdge.target && obj.target.currentType.toLowerCase() === 'notification';
                    });
                    if (notifierActionsArr.length > 0) {
                      let targetNode = notifierActionsArr[0].target;
                      let stream = results[2].entities.find((s) => {
                        return s.id === notifierActionsArr[0].streamGrouping.streamId;
                      });
                      actionObj = {
                        __type: currentActionType,
                        name: 'notifierAction',
                        outputStreams: [stream.streamId],
                        notifierName: '',
                        outputFieldsAndDefaults: {}
                      };
                    }
                  } else if (currentActionType === 'com.hortonworks.streamline.streams.layout.component.rule.action.TransformAction') {
                    let transformActionsArr = currentNodeEdges.filter((obj) => {
                      return obj.target !== selectedEdge.target && obj.target.currentType.toLowerCase() !== 'notification';
                    });
                    if (transformActionsArr.length > 0) {
                      let targetNode = transformActionsArr[0].target;
                      let stream = results[2].entities.find((s) => {
                        return s.id === transformActionsArr[0].streamGrouping.streamId;
                      });
                      actionObj = {
                        __type: currentActionType,
                        name: 'transformAction',
                        outputStreams: [stream.streamId],
                        transforms: []
                      };
                    }
                  }
                }
              }
            });
            if (index !== null) {
              if (actionObj !== null) {
                rule.actions[index] = actionObj;
              } else {
                rule.actions.splice(index, 1);
              }
              TopologyREST.updateNode(topologyId, versionId, type, rule.id, {body: JSON.stringify(rule)});
            }
          }
        });
      });
    }
    edges.splice(edges.indexOf(selectedEdge), 1);
    internalFlags.selectedEdge = null;
    updateGraphMethod();
  });
};

// remove edges associated with a node
const spliceLinksForNode = function(node, edges) {
  let toSplice = this.getEdges(edges, node);
  toSplice.map(function(l) {
    edges.splice(edges.indexOf(l), 1);
  });
};

const replaceSelectNode = function(d3Node, nodeData, constants, internalFlags, rectangles) {
  d3Node.classed(constants.selectedClass, true);
  if (internalFlags.selectedNode) {
    this.removeSelectFromNode(rectangles, constants, internalFlags);
  }
  internalFlags.selectedNode = nodeData;
};

const removeSelectFromNode = function(rectangles, constants, internalFlags) {
  rectangles.filter(function(cd) {
    return cd.nodeId === internalFlags.selectedNode.nodeId;
  }).classed(constants.selectedClass, false);
  internalFlags.selectedNode = null;
};

const replaceSelectEdge = function(d3, d3Path, edgeData, constants, internalFlags, paths) {
  d3Path.classed(constants.selectedClass, true);
  if (internalFlags.selectedEdge) {
    this.removeSelectFromEdge(d3, paths, constants, internalFlags);
  }
  internalFlags.selectedEdge = edgeData;
};

const removeSelectFromEdge = function(d3, paths, constants, internalFlags) {
  let path = paths.filter(function(cd) {
    return cd === internalFlags.selectedEdge;
  });
  let selectedPath = path[0][0];
  d3.select(selectedPath.previousSibling).classed(constants.selectedClass, false);
  internalFlags.selectedEdge = null;
};

const defineLinePath = function(p1, p2, flag) {
  let segments = [],
    sourceX = p1.x,
    sourceY = p1.y,
    targetX = p2.x,
    targetY = p2.y;

  segments.push("M" + sourceX + ',' + sourceY);
  if (!flag) {
    if (sourceX < targetX && sourceY === targetY) {
      segments.push("H" + targetX);
    } else if (sourceX > targetX) {
      segments.push("H" + (sourceX + 20));
      segments.push("V" + ((sourceY + targetY) / 2));
      segments.push("H" + (targetX - 20));
      segments.push("V" + (targetY));
      segments.push("H" + (targetX));
    } else {
      segments.push("H" + ((sourceX + targetX) / 2));
      segments.push("V" + (targetY));
      segments.push("H" + (targetX));
    }
  } else {
    segments.push("V" + (targetY));
    segments.push("H" + (targetX));
  }
  return segments.join(' ');
};

const showNodeModal = function(ModalScope, setModalContent, node, updateGraphMethod, allNodes, edges, linkShuffleOptions) {
  let currentEdges = this.getEdges(edges, node);
  let scope = ModalScope(node);
  setModalContent(node, updateGraphMethod, this.getConfigContainer(node, scope.configData, scope.editMode, scope.topologyId, scope.versionId, scope.namespaceId, currentEdges, allNodes, linkShuffleOptions, edges, updateGraphMethod,scope.testRunActivated),currentEdges, allNodes);
};

const getConfigContainer = function(node, configData, editMode, topologyId, versionId, namespaceId, currentEdges, allNodes, linkShuffleOptions, edges, updateGraphMethod,testRunActivated) {
  let nodeType = this.getNodeType(node.parentType);
  let sourceNodes = [],
    targetNodes = [];
  currentEdges.map((e) => {
    if (e.target.nodeId === node.nodeId) {
      //find source node of parser
      sourceNodes.push(e.source);
    } else if (e.source.nodeId === node.nodeId) {
      //find target node of parser
      targetNodes.push(e.target);
    }
  });
  if (node.parentType === 'SOURCE') {
    return () => {
      return <SourceNodeForm ref="ConfigModal" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} namespaceId={namespaceId} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions}/>;
    };
  } else if (node.parentType === 'SINK') {
    return () => {
      return <SinkNodeForm ref="ConfigModal" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} namespaceId={namespaceId} sourceNodes={sourceNodes}/>;
    };
  } else if (node.parentType === 'PROCESSOR') {
    let childElement = null;
    switch (node.currentType.toUpperCase()) {
    case 'RULE': //Rule
      childElement = () => {
        return <RulesNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} graphEdges={edges} updateGraphMethod={updateGraphMethod}/>;
      };
      break;
    case 'CUSTOM': //Custom
      childElement = () => {
        return <CustomNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes[0]} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions}/>;
      };
      break;
    case 'NORMALIZATION': //Normalization
      childElement = () => {
        return <NormalizationNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} currentEdges={currentEdges}/>;
      };
      break;
    case 'SPLIT': //Split
      childElement = () => {
        return <SplitNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes[0]} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions}/>;
      };
      break;
    case 'STAGE': //Stage
      childElement = () => {
        return <StageNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} currentEdges={currentEdges}/>;
      };
      break;
    case 'JOIN': //Join
      childElement = () => {
        return <JoinNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} currentEdges={currentEdges} graphEdges={edges}/>;
      };
      break;
    case 'WINDOW': //Windowing
      childElement = () => {
        return <WindowingAggregateNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes[0]} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} currentEdges={currentEdges}/>;
      };
      break;
    case 'BRANCH': //Branch
      childElement = () => {
        return <BranchNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} graphEdges={edges} updateGraphMethod={updateGraphMethod}/>;
      };
      break;
    case 'PMML': //Pmml
      childElement = () => {
        return <ModelNodeForm ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId}/>;
      };
      break;
    case 'PROJECTION': //Projection
      childElement = () => {
        return <ProjectionProcessorContainer  ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes[0]} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} currentEdges={currentEdges}/>;
      };
      break;
    case 'RT-JOIN': //RT-JOIN
      childElement = () => {
        return <RealTimeJoinNodeProcessor  ref="ProcessorChildElement" nodeData={node} configData={configData} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNode={sourceNodes[0]} targetNodes={targetNodes} linkShuffleOptions={linkShuffleOptions} currentEdges={currentEdges}/>;
      };
      break;
    }
    return () => {
      return <ProcessorNodeForm ref="ConfigModal" nodeData={node} editMode={editMode} testRunActivated={testRunActivated} nodeType={nodeType} topologyId={topologyId} versionId={versionId} sourceNodes={sourceNodes} getChildElement={childElement}/>;
    };
  }
};

const MouseUpAction = function(topologyId, versionId, d3node, d, metaInfo, internalFlags, constants, dragLine, paths, allNodes, edges, linkShuffleOptions, updateGraphMethod, elementType, getModalScope, setModalContent, rectangles, getEdgeConfigModal, setLastChange) {
  // reset the internalFlags
  internalFlags.shiftNodeDrag = false;
  d3node.classed(constants.connectClass, false);

  var mouseDownNode = internalFlags.mouseDownNode;
  var hasSource = edges.filter((e) => {
    return e.target.nodeId === d.nodeId;
  });

  //cannot connect from unconfigured node
  if (!internalFlags.addEdgeFromNode) {
    internalFlags.addEdgeFromNode = true;
    return;
  }

  // if (!mouseDownNode) return;
  // dragLine.classed("hidden", true);

  if (mouseDownNode && mouseDownNode !== d) {
    // we're in a different node: create new edge for mousedown edge and add to graph
    if (hasSource.length && d.currentType.toLowerCase() === 'branch') {
      dragLine.classed("hidden", true);
      FSReactToastr.warning(
        <strong>Edge cannot be connected to Branch.</strong>
      );
    } else {
      this.createEdge(mouseDownNode, d, paths, edges, internalFlags, updateGraphMethod, topologyId, versionId, getEdgeConfigModal, setLastChange, dragLine);
    }
    this.updateMetaInfo(topologyId, versionId, d, metaInfo);
  } else {
    if (elementType === 'rectangle') {
      // we're in the same node
      if (internalFlags.justDragged) {
        // dragged, not clicked
        internalFlags.justDragged = false;
      } else {
        // clicked, not dragged
        if (d3.event && d3.event.type === 'dblclick') {
          let hasSource = edges.filter((e) => {
            return e.target.nodeId === d.nodeId;
          });
          if (d.parentType === 'SOURCE' || hasSource.length) {
            this.showNodeModal(getModalScope, setModalContent, d, updateGraphMethod, allNodes, edges, linkShuffleOptions);
          } else {
            FSReactToastr.warning(
              <strong>Connect and configure a source component</strong>
            );
          }
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
    } else if (elementType === 'circle') {
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
};

const KeyDownAction = function(d, internalFlags, allNodes, edges, linkShuffleOptions, updateGraphMethod, getModalScope, setModalContent, rectangles, constants) {

  var keyDownNode = internalFlags.keyDownNode;
  var hasSource = edges.filter((e) => {
    return e.target.nodeId === d.nodeId;
  });
  if (d.parentType === 'SOURCE' || hasSource.length) {
    this.showNodeModal(getModalScope, setModalContent, d, updateGraphMethod, allNodes, edges, linkShuffleOptions);
    if (internalFlags.selectedNode) {
      this.removeSelectFromNode(rectangles, constants, internalFlags);
    }
  } else {
    FSReactToastr.warning(
      <strong>Connect and configure a source component</strong>
    );
  }
  internalFlags.keyDownNode = null;
  internalFlags.lastKeyDown = -1;
  return;
};

const setShuffleOptions = function(linkConfigArr) {
  let options = [];
  linkConfigArr.map((o) => {
    options.push({label: o.subType, value: o.subType});
  });
  return options;
};

const syncNodeData = function(sources, processors, sinks, metadata, sourcesBundle, processorsBundle, sinksBundle) {
  let nodeArr = [];
  this.generateNodeData(sources, sourcesBundle, metadata.sources, nodeArr);
  this.generateNodeData(processors, processorsBundle, metadata.processors, nodeArr);
  this.generateNodeData(sinks, sinksBundle, metadata.sinks, nodeArr);
  return nodeArr;
};

const capitalizeFirstLetter = function(string) {
  string = string.toLowerCase();
  return string.charAt(0).toUpperCase() + string.slice(1);
};

const generateNodeData = function(nodes, componentBundle, metadata, resultArr) {
  for (let i = 0; i < nodes.length; i++) {
    let componentObj = componentBundle.filter(c => {
      return c.id === nodes[i].topologyComponentBundleId;
    })[0];
    let currentType = this.capitalizeFirstLetter(componentObj.subType);
    let configuredFlag = _.keys(nodes[i].config.properties).length > 0
      ? true
      : false;
    if (currentType === 'Window' && _.keys(nodes[i].config.properties).length === 1) {
      configuredFlag = false;
    }

    let currentMetaObj = metadata.filter((o) => {
      return o.id === nodes[i].id;
    });
    if (currentMetaObj.length === 0) {
      console.error("Failed to get meta data");
    } else {
      currentMetaObj = currentMetaObj[0];
    }

    let nodeLabel = componentObj.name;
    if (componentObj.subType.toLowerCase() === 'custom') {
      let config = componentObj.topologyComponentUISpecification.fields,
        name = _.find(config, {fieldName: "name"});
      nodeLabel = name.defaultValue || 'Custom';
    }

    let obj = {
      x: currentMetaObj.x,
      y: currentMetaObj.y,
      nodeId: nodes[i].id,
      parentType: componentObj.type,
      currentType: currentType,
      uiname: nodes[i].name,
      imageURL: 'styles/img/icon-' + componentObj.subType.toLowerCase() + '.png',
      isConfigured: configuredFlag,
      parallelismCount: nodes[i].config.properties.parallelism || 1,
      nodeLabel: nodeLabel,
      topologyComponentBundleId: componentObj.id,
      reconfigure : nodes[i].reconfigure
    };
    if (currentMetaObj.streamId) {
      obj.streamId = currentMetaObj.streamId;
    }

    if(!_.isEmpty(componentObj.eventLogData) && componentObj.eventLogData !== undefined){
      obj.eventLogData =  componentObj.eventLogData;
    }
    resultArr.push(obj);
  }
};

const syncEdgeData = function(edges, nodes) {
  let edgesArr = [];
  edges.map((edge) => {
    //Find source node
    let fromNode = nodes.filter((o) => {
      return o.nodeId === edge.fromId;
    });
    if (fromNode.length !== 0) {
      fromNode = fromNode[0];
    } else {
      console.error("From node is missing");
    }
    //Find target node
    let toNode = nodes.filter((o) => {
      return o.nodeId === edge.toId;
    });
    if (toNode.length !== 0) {
      toNode = toNode[0];
    } else {
      console.error("To node is missing");
    }

    edgesArr.push({source: fromNode, target: toNode, edgeId: edge.id, streamGrouping: edge.streamGroupings[0]});
  });
  return edgesArr;
};

const createLineOnUI = function(edge, constants) {
  let arr = [],
    isFailedTupleflag = false;
  if (edge.target.streamId === "failedTuplesStream") {
    arr.push({
      x: (edge.source.x + constants.rectangleWidth / 2),
      y: (edge.source.y + constants.rectangleHeight)
    }, {
      x: edge.target.x,
      y: (edge.target.y + constants.rectangleHeight / 2)
    });
    isFailedTupleflag = true;
  } else {
    arr.push({
      x: (edge.source.x + constants.rectangleWidth),
      y: (edge.source.y + constants.rectangleHeight / 2)
    }, {
      x: edge.target.x,
      y: edge.target.eventLogData === undefined
        ? (edge.target.y + constants.rectangleHeight / 2)
        : edge.target.eventLogData.length < 1
          ? (edge.target.y + constants.testNoDataRectHeight / 2)
          : (edge.target.y + constants.testDataRectHeight / 2)
    });
  }
  return this.defineLinePath(arr[0], arr[1], isFailedTupleflag);
};

const getNodeRectClass = function(data) {
  if (data.parentType === 'SOURCE') {
    return 'source';
  } else if (data.parentType === 'PROCESSOR') {
    return 'processor';
  } else if (data.parentType === 'SINK') {
    return 'datasink';
  }
};

const getNodeImgRectClass = function(data) {
  if (data.parentType === 'SOURCE') {
    return 'source-img';
  } else if (data.parentType === 'PROCESSOR') {
    return 'processor-img';
  } else if (data.parentType === 'SINK') {
    return 'datasink-img';
  }
};

const updateParallelismCount = function(topologyId, versionId, nodeData, setLastChange) {
  let currentType = this.getNodeType(nodeData.parentType);
  TopologyREST.getNode(topologyId, versionId, currentType, nodeData.nodeId).then((result) => {
    let data = result;
    data.config.properties.parallelism = nodeData.parallelismCount;
    TopologyREST.updateNode(topologyId, versionId, currentType, nodeData.nodeId, {body: JSON.stringify(data)}).then((newNodeData) => {
      setLastChange(newNodeData.timestamp);
    });
  });
};

const topologyFilter = function(entities, filterValue,entity) {
  let matchFilter = new RegExp(filterValue, 'i');
  return entities.filter(filteredList => !filterValue || matchFilter.test(filteredList[entity].name));
};

const getEdgeData = function(data, topologyId, versionId, callback) {
  TopologyREST.getNode(topologyId, versionId, 'streams', data.streamGrouping.streamId).then((result) => {
    let obj = {
      streamName: result.streamId,
      grouping: data.streamGrouping.grouping,
      groupingFields: data.streamGrouping.fields,
      edgeData: data
    };
    callback(obj);
  });
};

const getNodeStreams = function(topologyId, versionId, nodeId, parentType, edges, callback) {
  let streamData = {
    inputSchema: [],
    outputSchema: []
  };
  let nodeType = this.getNodeType(parentType);
  let promiseArr = [TopologyREST.getNode(topologyId, versionId, nodeType, nodeId)];

  let connectingEdges = edges.filter((obj) => {
    return obj.target.nodeId == nodeId;
  });
  connectingEdges.map((e) => {
    promiseArr.push(TopologyREST.getNode(topologyId, versionId, 'streams', e.streamGrouping.streamId));
  });

  Promise.all(promiseArr).then(results => {
    results.map((s, i) => {
      if (i === 0 && parentType !== 'SINK') {
        if (s.outputStreams.length) {
          streamData.outputSchema = s.outputStreams[0].fields;
        }
      } else if (i > 0) {
        streamData.inputSchema = [
          ...streamData.inputSchema,
          ...s.fields
        ];
      }
    });
    streamData.inputSchema = _.uniqWith(streamData.inputSchema, _.isEqual);
    callback(streamData);
  });
};

const updateGraphEdges = function(graphEdges, newEdges) {
  newEdges.map((edge) => {
    let currentEdge = graphEdges.find((e) => {
      return e.edgeId === edge.id;
    });
    currentEdge.streamGrouping = edge.streamGroupings[0];
  });
};

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
  KeyDownAction,
  setShuffleOptions,
  syncNodeData,
  capitalizeFirstLetter,
  generateNodeData,
  syncEdgeData,
  createLineOnUI,
  getNodeRectClass,
  getNodeImgRectClass,
  updateParallelismCount,
  topologyFilter,
  getEdgeData,
  getNodeStreams,
  updateGraphEdges,
  spliceDeleteNodeArr
};
