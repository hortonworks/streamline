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

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import ReactDOM, {findDOMNode, render} from 'react-dom';
import {ItemTypes, Components, deleteNodeIdArr} from '../utils/Constants';
import {DragDropContext, DropTarget} from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import d3 from 'd3';
import d3Tip from 'd3-tip';
import $ from 'jquery';
import jQuery from 'jquery';
import TopologyUtils from '../utils/TopologyUtils';
import state from '../app_state';
import Utils from '../utils/Utils';
import EventLogComponent from '../containers/Streams/TestRunComponents/EventLogComponent';

window.$ = $;
window.jQuery = jQuery;

const componentTarget = {
  drop(props, monitor, component) {
    let parentRect = document.getElementsByClassName('graph-region')[0].getBoundingClientRect();
    const item = monitor.getItem();
    const delta = monitor.getClientOffset();
    let {x, y} = delta;
    x = x - parentRect.left;
    y = y - parentRect.top;
    component.createNode({
      x,
      y
    }, item);
  }
};

function collect(connect, monitor) {
  return {connectDropTarget: connect.dropTarget()};
}

@DropTarget(ItemTypes.Nodes, componentTarget, collect)
export default class TopologyGraphComponent extends Component {
  static propTypes = {
    connectDropTarget: PropTypes.func.isRequired,
    height: PropTypes.number.isRequired,
    width: PropTypes.number,
    data: PropTypes.object.isRequired,
    topologyId: PropTypes.string.isRequired,
    viewMode: PropTypes.bool.isRequired,
    getModalScope: PropTypes.func.isRequired,
    setModalContent: PropTypes.func.isRequired,
    getEdgeConfigModal: PropTypes.func,
    testRunActivated : PropTypes.bool.isRequired
  };

  constructor(props) {
    super(props);
    this.renderFlag = false;
    this.testRunActivated = props.testRunActivated;
    this.hideEventLog = props.hideEventLog;
  }

  componentWillReceiveProps(nextProps,previousProps){
    nextProps.testRunActivated !== previousProps.testRunActivated
    ? this.testRunActivated = nextProps.testRunActivated
    : '';
    nextProps.hideEventLog !== previousProps.hideEventLog
      ? this.hideEventLog = nextProps.hideEventLog
      :'';
  }

  componentWillUpdate(){
    if(this.testRunActivated){
      this.updateGraph();
    }
    if((!this.testRunActivated && !this.props.viewMode) || this.hideEventLog){
      this.toolTip.hide();
    }
  }

  componentWillUnmount() {
    d3.select('body').on("keydown", null).on("keyup", null);
    window.removeEventListener('keydown', this.handleKeyDown.bind(this), false);
    this.toolTip.hide();
  }

  state = {
    width: this.props.width || (window.innerWidth - 60),
    height: this.props.height
  };

  internalFlags = {
    selectedNode: null,
    selectedEdge: null,
    mouseDownNode: null,
    mouseDownLink: null,
    keyDownNode: null,
    justDragged: false,
    justScaleTransGraph: false,
    lastKeyDown: -1,
    shiftNodeDrag: false,
    failedTupleDrag: false,
    addEdgeFromNode: true
  };

  constants = {
    selectedClass: "selected",
    connectClass: "connect-node",
    rectangleGClass: "conceptG",
    graphClass: "graph",
    BACKSPACE_KEY: 8,
    DELETE_KEY: 46,
    SPACE_KEY: 32,
    ESCAPE_KEY: 27,
    TAB_KEY: 9,
    ENTER_KEY: 13,
    rectangleWidth: 145,
    rectangleHeight: 40,
    testDataRectHeight: 210,
    testNoDataRectHeight: 75
  };

  componentDidUpdate() {
    window.removeEventListener('keydown', this.handleKeyDown.bind(this), false);
    if(!this.props.viewMode) {
      window.addEventListener('keydown', this.handleKeyDown.bind(this), false);
    }
  }

  handleKeyDown(event) {
    if(!this.props.viewMode) {
      switch(event.keyCode) {
      case this.constants.SPACE_KEY:
        if(event.ctrlKey || event.metaKey) {
          event.preventDefault();
          state.showSpotlightSearch = true;
        }
        break;
      case this.constants.ESCAPE_KEY:
        state.showSpotlightSearch = false;
        break;
      }
    }
  }

  componentDidMount() {
    let thisGraph = this;
    let {width, height} = this.state;

    let svg = this.svg = d3.select(ReactDOM.findDOMNode(this)).attr('width', '100%').attr('height', '100%');

    TopologyUtils.defineMarkers(svg);

    let svgG = this.svgG = svg.append("g").classed(this.constants.graphClass, true);

    // displayed when dragging between nodes
    this.dragLine = svgG.append('svg:path').attr('class', 'link dragline hidden').attr('d', 'M0 0 L0 0').attr("stroke-dasharray", "5, 5").style('marker-end', 'url(#mark-end-arrow)');

    // svg nodes and edges
    this.paths = svgG.append("g").attr('class', 'link-group').selectAll("g");
    this.rectangles = svgG.append("g").selectAll("g");

    this.edgeStream = svgG.append('foreignObject').attr("class", "edge-stream").attr('width', 200).attr('height', 200).attr('x', 0).style('display', 'none').append("xhtml:body").attr('class', 'edge-details').html('<p><strong>ID:</strong> </p>' +
      '<p><strong>Grouping:</strong> </p>' +
      '<p><button class="btn btn-xs btn-warning editEdge">Edit</button>' +
      '<button class="btn btn-xs btn-warning deleteEdge">Delete</button></p>');

    this.toolTip = d3Tip().attr('class', 'd3-tip').offset([0, 10]).direction('e').html('');
    svgG.call(this.toolTip);
    $('.container.wrapper').append($('body > .d3-tip'));
    this.main_edgestream = d3.select('.edge-stream');
    this.drag = d3.behavior.drag().origin(function(d) {
      return {x: d.x, y: d.y};
    }).on("drag", function(args) {
      if (thisGraph.editMode || thisGraph.testRunActivated) {
        thisGraph.internalFlags.justDragged = true;
        thisGraph.dragMove.call(thisGraph, args);
      }
    }).on("dragend", function(node) {
      if (thisGraph.editMode || thisGraph.testRunActivated) {
        let {topologyId, versionId, versionsArr, metaInfo} = thisGraph;
        if (versionId) {
          let versionName = versionsArr.find((o) => {
            return o.id == versionId;
          }).name;
          if (versionName.toLowerCase() == 'current') {
            TopologyUtils.updateMetaInfo(topologyId, versionId, node, metaInfo);
          }
        }
      }
    });

    // listen for key events
    d3.select('body').on("keydown", function() {
      if (d3.event.target.nodeName === 'BODY') {
        thisGraph.svgKeyDown.call(thisGraph);
      }
    }).on("keyup", function() {
      if (d3.event.target.nodeName === 'BODY') {
        thisGraph.svgKeyUp.call(thisGraph);
      }
    });

    svg.on("mousedown", function(d) {
      if (thisGraph.setLastChange) {
        thisGraph.setLastChange(null);
      }
      thisGraph.svgMouseDown.call(thisGraph, d);
    });

    svg.on("mouseup", function(d) {
      thisGraph.svgMouseUp.call(thisGraph, d);
    });

    svg.on("mousemove", function(d) {
      if (d3.event.target.nodeName === 'svg' && !thisGraph.testRunActivated)
        {thisGraph.toolTip.hide();}
    });

    // listen for dragging - also used for zoom in/out via buttons
    this.dragSvg = d3.behavior.zoom().scaleExtent([0, 8]).on("zoom", function() {
      thisGraph.internalFlags.justScaleTransGraph = true;
      d3.select("." + thisGraph.constants.graphClass).attr("transform", "translate(" + thisGraph.dragSvg.translate() + ")" + "scale(" + thisGraph.dragSvg.scale() + ")");
    }).on("zoomend", function() {
      let sourceEvent = d3.event.sourceEvent;
      let gTranslate = thisGraph.dragSvg.translate(),
        gScaled = thisGraph.dragSvg.scale();

      thisGraph.metaInfo.graphTransforms = thisGraph.graphTransforms = {
        dragCoords: gTranslate,
        zoomScale: gScaled
      };
      clearTimeout(this.saveMetaInfoTimer);
      this.saveMetaInfoTimer = setTimeout(() => {
        let {topologyId, versionId, versionsArr, metaInfo, editMode} = thisGraph;
        if (versionId && versionsArr && sourceEvent !== null && sourceEvent.target.nodeName !== 'text') {
          let versionName = versionsArr.find((o) => {
            return o.id == versionId;
          }).name;
          if (versionName.toLowerCase() == 'current' && editMode && !thisGraph.testRunActivated) {
            TopologyUtils.saveMetaInfo(topologyId, versionId, null, metaInfo, null);
          }
        }
      }, 500);
    });

    this.dragSvg.translate(this.graphTransforms.dragCoords);
    this.dragSvg.scale(this.graphTransforms.zoomScale);
    this.dragSvg.event(svg);

    //NOTE - To use scroll for zoom in/out, uncomment the below line
    svg.call(this.dragSvg).on("dblclick.zoom", null);

    this.updateGraph();
    this.renderFlag = true;
    this.evt = document.createEvent("Events");
    this.evt.initEvent("click", true, true);
    this.eventTable = false;
  }

  zoomAction(zoomType) {
    let thisGraph = this,
      direction = 1,
      factor = 0.2,
      target_zoom = 1,
      center = [thisGraph.svg[0][0].clientWidth / 2,
        thisGraph.svg[0][0].clientHeight / 2
      ],
      zoom = thisGraph.dragSvg,
      extent = zoom.scaleExtent(),
      translate = zoom.translate(),
      translate0 = [],
      l = [],
      view = {
        x: translate[0],
        y: translate[1],
        k: zoom.scale()
      };

    direction = (zoomType === 'zoom_in')
      ? 1
      : -1;
    target_zoom = zoom.scale() * (1 + factor * direction);

    if (target_zoom < extent[0] || target_zoom > extent[1]) {
      return false;
    }

    translate0 = [
      (center[0] - view.x) / view.k,
      (center[1] - view.y) / view.k
    ];
    view.k = target_zoom;
    l = [
      translate0[0] * view.k + view.x,
      translate0[1] * view.k + view.y
    ];

    view.x += center[0] - l[0];
    view.y += center[1] - l[1];

    thisGraph.interpolateZoom([
      view.x, view.y
    ], view.k);
  }

  interpolateZoom(translate, scale) {
    let thisGraph = this,
      zoom = thisGraph.dragSvg;
    return d3.transition().duration(350).tween("zoom", function() {
      let iTranslate = d3.interpolate(zoom.translate(), translate),
        iScale = d3.interpolate(zoom.scale(), scale);
      return function(t) {
        zoom.scale(iScale(t)).translate(iTranslate(t));
        d3.select("." + thisGraph.constants.graphClass).attr("transform", "translate(" + thisGraph.dragSvg.translate() + ")" + "scale(" + thisGraph.dragSvg.scale() + ")");
        thisGraph.metaInfo.graphTransforms = thisGraph.graphTransforms = {
          dragCoords: thisGraph.dragSvg.translate(),
          zoomScale: thisGraph.dragSvg.scale()
        };
        clearTimeout(this.saveMetaInfoTimer);
        this.saveMetaInfoTimer = setTimeout(() => {
          let {topologyId, versionId, versionsArr, metaInfo, editMode} = thisGraph;
          if (versionId && versionsArr) {
            let versionName = versionsArr.find((o) => {
              return o.id == versionId;
            }).name;
            if (versionName.toLowerCase() == 'current' && editMode && !thisGraph.testRunActivated) {
              TopologyUtils.saveMetaInfo(topologyId, versionId, null, metaInfo, null);
            }
          }
        }, 500);
      };
    });
  }

  dragMove(d) {
    let {internalFlags, constants} = this;
    if (internalFlags.shiftNodeDrag) {
      if (internalFlags.failedTupleDrag) {
        this.dragLine.attr('d', 'M' + (d.x + constants.rectangleWidth / 2) + ',' + (d.y +  this.calculateHeight.call(this,d) + 10) + 'L' + d3.mouse(this.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
      } else {
        this.dragLine.attr('d', 'M' + (d.x + constants.rectangleWidth + this.calculateHeight.call(this,d)) + ',' + (d.y + this.calculateHeight.call(this,d) / 2) + 'L' + d3.mouse(this.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
      }
    } else {
      d.x = d3.mouse(this.svgG.node())[0] - constants.rectangleWidth / 2;
      d.y = d3.mouse(this.svgG.node())[1] - this.calculateHeight.call(this,d) / 2;
      this.updateGraph();
    }
  }

  pathMouseDown(d3path, d) {
    if(!this.props.testRunActivated){
      let {internalFlags, constants, paths, rectangles} = this;
      d3.event.stopPropagation();
      internalFlags.mouseDownLink = d;

      if (internalFlags.selectedNode) {
        TopologyUtils.removeSelectFromNode(rectangles, this.customConstantObj.call(this,d), internalFlags);
      }

      let prevEdge = internalFlags.selectedEdge;
      if (!prevEdge || prevEdge !== d) {
        TopologyUtils.replaceSelectEdge(d3, d3path, d, this.customConstantObj.call(this,d), internalFlags, paths);
        this.main_edgestream.style('display', 'block');
        d3.select('.edge-stream').attr('x', this.getBoundingBoxCenter(d3path)[0] - 100);
        d3.select('.edge-stream').attr('y', this.getBoundingBoxCenter(d3path)[1]);
        TopologyUtils.getEdgeData(d, this.topologyId, this.versionId, this.setEdgeData.bind(this));
      } else {
        TopologyUtils.removeSelectFromEdge(d3, paths, this.customConstantObj.call(this,d), internalFlags);
        this.edgeStream.style('display', 'none');
        this.main_edgestream.style('display', 'none');
      }
    }
  }

  setEdgeData(obj) {
    let thisGraph = this;
    let name = obj.streamName;
    if (name.length > 18)
      {name = name.slice(0, 17) + '...';}

    if (thisGraph.editMode) {
      this.edgeStream.html('<div><p><strong>Stream:</strong> ' + name + '</p>' + '<p><strong>Grouping:</strong> ' + obj.grouping + '</p>' + '<p><button class="btn btn-xs btn-success editEdge"><i class="fa fa-pencil"></i> Edit</button> ' + '<button class="btn btn-xs btn-danger deleteEdge"><i class="fa fa-trash"></i> Delete</button></p>' + '</div>');
      this.main_edgestream.style('display', 'block');
      this.edgeStream.style('display', 'block');
      d3.select('.editEdge').on("click", function() {
        this.getEdgeConfigModal(this.topologyId, this.versionId, obj.edgeData, this.edges, this.updateGraph, null, obj.streamName, obj.grouping, obj.groupingFields);
        this.edgeStream.style('display', 'none');
        this.main_edgestream.style('display', 'none');
      }.bind(this));
      d3.select('.deleteEdge').on("click", function() {
        this.deleteEdge(this.internalFlags.selectedEdge);
        this.edgeStream.style('display', 'none');
        this.main_edgestream.style('display', 'none');
      }.bind(this));
    } else {
      this.edgeStream.html('<div><p><strong>Stream:</strong> ' + name + '</p>' + '<strong>Grouping:</strong> ' + obj.grouping + '</div>');
      this.main_edgestream.style('display', 'block');
      this.edgeStream.style('display', 'block');
    }
  }

  showNodeStreams(d, node, data) {
    if (data.inputSchema.length === 0 && data.outputSchema.length === 0) {
      this.toolTip.hide();
      return;
    }
    var thisGraph = this;
    var inputFieldsHtml = '',
      outputFieldsHtml = '';
    data.inputSchema.map((s) => {
      return (inputFieldsHtml += '<li>' + s.name + (s.optional
        ? ''
        : '<span class="text-danger">*</span>') + '<span class="output-type">' + s.type + '</span></li>');
    });
    data.outputSchema.map((s) => {
      return (outputFieldsHtml += '<li>' + s.name + (s.optional
        ? ''
        : '<span class="text-danger">*</span>') + '<span class="output-type">' + s.type + '</span></li>');
    });
    thisGraph.toolTip.html(function(d) {
      return ('<div class="schema-tooltip clearfix"><h3>Schema</h3>' + (inputFieldsHtml === ''
        ? ''
        : '<div class="input-schema"><h4>Input</h4><ul class="schema-list">' + inputFieldsHtml + '</ul></div>') + (outputFieldsHtml === ''
        ? ''
        : '<div class="output-schema"><h4>Output</h4><ul class="schema-list">' + outputFieldsHtml + '</ul></div>') + '</div>');
    });
    thisGraph.toolTip.show(data, node);
  }

  // mousedown on node
  rectangleMouseDown(d3node, d) {
    let {internalFlags} = this;
    d3.event.stopPropagation();
    internalFlags.mouseDownNode = d;
    this.edgeStream.style('display', 'none');
    this.main_edgestream.style('display', 'none');
  }

  //mousedown on circle
  circleMouseDown(d3node, d) {
    if(!this.props.testRunActivated){
      let {internalFlags, constants, paths} = this;
      d3.event.stopPropagation();
      internalFlags.mouseDownNode = d;
      this.edgeStream.style('display', 'none');
      this.main_edgestream.style('display', 'none');
      if (internalFlags.selectedEdge) {
        TopologyUtils.removeSelectFromEdge(d3, paths, this.customConstantObj.call(this,d), internalFlags);
      }
      internalFlags.failedTupleDrag = false;
      if (d3.event.currentTarget.getAttribute('data-failedTuple') === 'true') {
        internalFlags.failedTupleDrag = true;
      }
      internalFlags.shiftNodeDrag = true;
      if (!d.isConfigured) {
        this.dragLine.classed('hidden', true);
        internalFlags.addEdgeFromNode = false;
        return;
      }
      // reposition dragged directed edge
      this.dragLine.classed('hidden', false).attr('d', 'M' + d.x + Math.round(constants.rectangleWidth / 2) + ',' + d.y + this.calculateHeight(this,d) + 'L' + d.x + Math.round(constants.rectangleWidth / 2) + ',' + d.y + this.calculateHeight(this,d));
      return;
    }
  }

  // mouseup on nodes
  rectangleMouseUp(d3node, d) {
    let {
      topologyId,
      versionId,
      internalFlags,
      constants,
      dragLine,
      paths,
      edges,
      rectangles,
      getModalScope,
      setModalContent,
      nodes,
      linkShuffleOptions,
      metaInfo,
      getEdgeConfigModal,
      setLastChange
    } = this;
    return TopologyUtils.MouseUpAction(topologyId, versionId, d3node, d, metaInfo, internalFlags, constants, dragLine, paths, nodes, edges, linkShuffleOptions, this.updateGraph.bind(this), 'rectangle', getModalScope, setModalContent, rectangles, getEdgeConfigModal, setLastChange);
  }

  // keydown on selected node
  rectangleKeyDown(d3node, d) {
    let {
      internalFlags,
      edges,
      getModalScope,
      setModalContent,
      nodes,
      linkShuffleOptions,
      rectangles,
      constants
    } = this;
    return TopologyUtils.KeyDownAction(d, internalFlags, nodes, edges, linkShuffleOptions, this.updateGraph.bind(this), getModalScope, setModalContent, rectangles, constants);
  }

  // mouseup on circle
  circleMouseUp(d3node, d) {
    let {
      topologyId,
      versionId,
      internalFlags,
      constants,
      dragLine,
      paths,
      edges,
      rectangles,
      getModalScope,
      setModalContent,
      nodes,
      linkShuffleOptions,
      metaInfo,
      getEdgeConfigModal,
      setLastChange
    } = this;
    return TopologyUtils.MouseUpAction(topologyId, versionId, d3node, d, metaInfo, internalFlags, constants, dragLine, paths, nodes, edges, linkShuffleOptions, this.updateGraph.bind(this), 'circle', getModalScope, setModalContent, rectangles, getEdgeConfigModal, setLastChange);
  }

  // mousedown on main svg
  svgMouseDown() {
    this.internalFlags.graphMouseDown = true;
    let {paths, constants, internalFlags} = this;
    if (!d3.event.target.closest('.edge-details') && internalFlags.selectedEdge) {
      TopologyUtils.removeSelectFromEdge(d3, paths, constants, internalFlags);
      this.edgeStream.style('display', 'none');
      this.main_edgestream.style('display', 'none');
    }
    state.showSpotlightSearch = false;
  }

  // mouseup on main svg
  svgMouseUp() {
    let {internalFlags} = this;
    if (internalFlags.justScaleTransGraph) {
      // dragged not clicked
      internalFlags.justScaleTransGraph = false;
    } else if (internalFlags.shiftNodeDrag) {
      // dragged from node
      internalFlags.shiftNodeDrag = false;
      this.dragLine.classed("hidden", true);
    }
    internalFlags.graphMouseDown = false;
  }

  createNode(delta, itemObj) {
    let {
      internalFlags,
      constants,
      nodes,
      topologyId,
      versionId,
      metaInfo,
      paths,
      edges,
      uinamesList,
      setLastChange
    } = this;
    let {imgUrl, parentType, name, currentType, topologyComponentBundleId} = itemObj;
    internalFlags.graphMouseDown = true;
    d3.event = dropevent;
    var xycoords = d3.mouse(this.svgG.node());
    d3.event = null;
    let d = {
      x: xycoords[0] + (constants.rectangleWidth / 2) - constants.rectangleWidth,
      y: xycoords[1] - (constants.rectangleHeight / 2) - 5.5,
      parentType: itemObj.type,
      currentType: itemObj.nodeType,
      uiname: itemObj.nodeLable,
      imageURL: itemObj.imgPath,
      isConfigured: false,
      parallelismCount: 1,
      nodeLabel: itemObj.nodeLable,
      topologyComponentBundleId: itemObj.topologyComponentBundleId
    };
    nodes.push(d);
    let createNodeArr = [d];
    TopologyUtils.createNode(topologyId, versionId, createNodeArr, this.updateGraph.bind(this), metaInfo, paths, edges, internalFlags, uinamesList, setLastChange);
    internalFlags.graphMouseDown = false;
  }
  /*
    addComponentToGraph method accepts object with component details
    get co-ordinates for the new node and make call to create node in graph
  */
  addComponentToGraph(itemObj) {
    let {
      internalFlags,
      constants,
      nodes,
      topologyId,
      versionId,
      metaInfo,
      paths,
      edges,
      uinamesList,
      setLastChange
    } = this;
    let d = {
      parentType: itemObj.type,
      currentType: itemObj.nodeType,
      uiname: itemObj.nodeLabel,
      imageURL: itemObj.imgPath,
      isConfigured: false,
      parallelismCount: 1,
      nodeLabel: itemObj.nodeLabel,
      topologyComponentBundleId: itemObj.topologyComponentBundleId
    };
    let xcoord = 15, ycoord = 15;
    if(nodes.length > 0) {
      xcoord = _.minBy(nodes, function(o) {return o.x;}).x + (constants.rectangleWidth / 2) - constants.rectangleWidth + 40;
      ycoord = _.maxBy(nodes, function(o) {return o.y;}).y - (constants.rectangleHeight / 2) - 5.5 + 40;
    }
    d.x = xcoord;
    d.y = ycoord;
    nodes.push(d);
    let createNodeArr = [d];
    TopologyUtils.createNode(topologyId, versionId, createNodeArr, this.updateGraph.bind(this), metaInfo, paths, edges, internalFlags, uinamesList, setLastChange);
  }

  // keydown on main svg
  svgKeyDown() {
    let {internalFlags, constants, rectangles} = this;
    // make sure repeated key presses don't register for each keydown
    if (internalFlags.lastKeyDown !== -1)
      {return;}

    internalFlags.lastKeyDown = d3.event.keyCode;
    var selectedEdge = internalFlags.selectedEdge;
    var selectedNode = internalFlags.selectedNode;

    switch (d3.event.keyCode) {
    case constants.BACKSPACE_KEY:
    case constants.DELETE_KEY:
      d3.event.preventDefault();
      if (selectedEdge) {
        this.deleteEdge(selectedEdge);
      } else if(selectedNode) {
        this.deleteNode(selectedNode);
      }
      break;
    case constants.TAB_KEY:
      d3.event.preventDefault();
      if(selectedNode) {
        let nodesArr = Utils.sortArray(Utils.sortArray(JSON.parse(JSON.stringify(this.nodes)), 'y', true), 'x', true);
        let i = _.findIndex(nodesArr, {nodeId: selectedNode.nodeId});
        if(i < nodesArr.length - 1){
          selectedNode = nodesArr[i + 1];
        } else {
          selectedNode = nodesArr[0];
        }
        let d3node = rectangles.filter(function(cd) {
          return cd.nodeId === selectedNode.nodeId;
        });
        TopologyUtils.replaceSelectNode(d3node, selectedNode, constants, internalFlags, rectangles);
      } else {
        let nodesArr = Utils.sortArray(Utils.sortArray(JSON.parse(JSON.stringify(this.nodes)), 'y', true), 'x', true);
        if(nodesArr.length > 0){
          selectedNode = nodesArr[0];
          let node = rectangles.filter(function(cd) {
            return cd.nodeId === selectedNode.nodeId;
          });
          TopologyUtils.replaceSelectNode(node, selectedNode, constants, internalFlags, rectangles);
        }
      }
      break;
    case constants.ENTER_KEY:
      d3.event.preventDefault();
      if(selectedNode) {
        let d3Node = rectangles.filter(function(cd) {
          return cd.nodeId === selectedNode.nodeId;
        });
        internalFlags.keyDownNode = selectedNode;
        internalFlags.lastKeyDown = -1;
        this.rectangleKeyDown(d3Node, selectedNode);
      }
      break;
    }
  }

  deleteNode(selectedNode) {
    let {
      topologyId,
      versionId,
      nodes,
      edges,
      internalFlags,
      updateGraph,
      metaInfo,
      uinamesList,
      setLastChange,
      topologyConfigMessageCB
    } = this;
    TopologyUtils.deleteNode(topologyId, versionId, selectedNode, nodes, edges, internalFlags, updateGraph.bind(this), metaInfo, uinamesList, setLastChange, topologyConfigMessageCB);
  }

  deleteEdge(selectedEdge) {
    let {
      topologyId,
      versionId,
      internalFlags,
      edges,
      nodes,
      updateGraph,
      setLastChange
    } = this;
    TopologyUtils.deleteEdge(selectedEdge, topologyId, versionId, internalFlags, edges, nodes, updateGraph.bind(this), setLastChange);
    this.edgeStream.style('display', 'none');
    this.main_edgestream.style('display', 'none');
  }

  svgKeyUp() {
    this.internalFlags.lastKeyDown = -1;
  }

  clonePaths() {
    let element = document.querySelectorAll('.link:not(.hidden)');
    for (let i = 0, len = element.length; i < len; i++) {
      let cloneElem = element[i].cloneNode();
      cloneElem.style['marker-end'] = 'url(#end-arrow)';
      cloneElem.setAttribute('stroke-width', '2.3');
      cloneElem.setAttribute('class', 'link visible-link');
      cloneElem.removeAttribute('stroke-opacity');
      cloneElem.removeAttribute('data-toggle');
      element[i].parentNode.insertBefore(cloneElem, element[i]);
    }
  }

  getBoundingBoxCenter(selection) {
    var element = selection.node(),
      bbox = element.getBBox();
    return [
      bbox.x + bbox.width / 2,
      bbox.y + bbox.height / 2
    ];
  }
  deleteNodePool(id) {
    const flag = deleteNodeIdArr.findIndex((x) => {
      return Number(x) === id;
    });
    return flag !== -1
      ? true
      : false;
  }

  showNodeTypeToolTip(d,node){
    let thisGraph = this;
    thisGraph.toolTip.attr("class","d3-tip nodeTypeToolTip").direction('n' ).html(function(d) {
      return (
        "<div class='showType-tooltip clearfix'>"+
          "<h3>"+d.uiname+"</h3>"+
        "</div>"
      );
    });
    clearTimeout(timeOut);
    let timeOut = setTimeout(function(){
      thisGraph.toolTip.show(d ,node);
    },700);
  }

  calculateHeight = (d) => {
    const thisGraph = this;
    let num = thisGraph.constants.rectangleHeight - 1;
    if(thisGraph.testRunActivated && d.eventLogData !== undefined){
      d.eventLogData.length
      ? num = thisGraph.constants.testDataRectHeight
      : num = thisGraph.constants.testNoDataRectHeight;
    }
    return num;
  }

  customConstantObj = (d) => {
    const thisGraph = this;
    let obj = _.cloneDeep(thisGraph.constants);
    let nodeData = d;
    if(d.source){
      const index = _.findIndex(thisGraph.nodes,function(n){
        return n.uiname === d.source.uiname;
      });
      if(index !== -1){
        nodeData = thisGraph.nodes[index];
      }
    }
    obj.rectangleHeight = thisGraph.calculateHeight.call(thisGraph,nodeData);
    obj.rectangleWidth  = thisGraph.constants.rectangleWidth;
    return obj;
  }

  updateGraph() {
    let that = this;
    var duplicateLinks = document.getElementsByClassName('visible-link');
    while (duplicateLinks.length > 0) {
      duplicateLinks[0].remove();
    }
    var thisGraph = this,
      constants = thisGraph.constants,
      internalFlags = thisGraph.internalFlags;

    // change every nodes y: value for viewMode
    if (that.props.viewMode) {
      let flag = true;
      thisGraph.nodes.map(x => {
        return x.y > 300
          ? flag = true
          : flag = false;
      });
      if (flag) {
        thisGraph.nodes.map(x => {
          return x.y = (x.y / 3);
        });
      }
    }

    thisGraph.paths = thisGraph.paths.data(thisGraph.edges, function(d) {
      return String(d.source.nodeId) + "+" + String(d.target.nodeId);
    });
    var paths = thisGraph.paths;
    // update existing paths
    paths.classed(constants.selectedClass, function(d) {
      return d === internalFlags.selectedEdge;
    }).attr("d", function(d) {
      return TopologyUtils.createLineOnUI(d, thisGraph.customConstantObj.call(thisGraph,d));
    });

    // add new paths
    paths.enter().append("path").classed("link", true).attr("d", function(d) {
      return TopologyUtils.createLineOnUI(d, thisGraph.customConstantObj.call(thisGraph,d));
    }).attr("stroke-opacity", "0.0001").attr("stroke-width", "10").attr("data-name", function(d) {
      return d.source.nodeId + '-' + d.target.nodeId;
    }).attr('data-source-target',function(d){
      return d.source.uiname;
    }).on("mouseover", function(d) {
      if (!thisGraph.editMode) {
        let elem = document.querySelectorAll('.visible-link[d="' + this.getAttribute('d') + '"]')[0];
        let d3path = d3.select(elem);
        d3.select('.edge-stream').attr('x', thisGraph.getBoundingBoxCenter(d3path)[0] - 100);
        d3.select('.edge-stream').attr('y', thisGraph.getBoundingBoxCenter(d3path)[1]);
        TopologyUtils.getEdgeData(d, thisGraph.topologyId, thisGraph.versionId, thisGraph.setEdgeData.bind(thisGraph));
      }
    }).on("mouseout", function(d) {
      if (!thisGraph.editMode) {
        thisGraph.edgeStream.style('display', 'none');
        thisGraph.main_edgestream.style('display', 'none');
      }
    }).on("mousedown", function(d) {
      if (thisGraph.editMode) {
        let elem = document.querySelectorAll('.visible-link[d="' + this.getAttribute('d') + '"]')[0];
        thisGraph.pathMouseDown.call(thisGraph, d3.select(elem), d);
      }
    }).on("mouseup", function(d) {
      if (thisGraph.editMode) {
        internalFlags.mouseDownLink = null;
      }
    });



    // remove old links
    paths.exit().remove();

    //clone the paths or links to make hover on them with some hidden margin
    thisGraph.clonePaths();



    // update existing nodes
    thisGraph.rectangles = thisGraph.rectangles.data(thisGraph.nodes, function(d) {
      return d.nodeId;
    });
    thisGraph.rectangles.attr("transform", function(d) {
      return "translate(" + d.x + "," + d.y + ")";
    });

    thisGraph.rectangles.selectAll('rect').attr('class', function(d){
      let classStr = "node-rectangle "+ TopologyUtils.getNodeRectClass(d);
      classStr += d.reconfigure ?  ' reconfig-node ' : '' ;
      return classStr;
    }).attr("filter", function(d) {
      if (!d.isConfigured) {
        return "url(#grayscale)";
      } else {
        return "";
      }
    }).attr("filter", function(d) {
      return "url(#dropshadow)";
    }).attr("height" , function(d){
      return thisGraph.calculateHeight.call(thisGraph,d);
    });

    thisGraph.rectangles.selectAll('image').attr("filter", function(d) {
      return "url(#grayscale)";
    }).attr("xlink:href", function(d) {
      return that.deleteNodePool(d.nodeId)
        ? "styles/img/start-loader.gif"
        : d.nodeId
          ? thisGraph.testRunActivated && !thisGraph.hideEventLog
            ? "styles/img/start-loader.gif"
            : d.imageURL
          : "styles/img/start-loader.gif";
    });
    thisGraph.rectangles.selectAll('circle').attr("filter", function(d) {
      if (!d.isConfigured) {
        return thisGraph.testRunActivated ? thisGraph.props.eventLogData.length ? '' : "url(#grayscale)" : "url(#grayscale)";
      } else {
        return "";
      }
    }).attr('cy' , function(d){
      return thisGraph.calculateHeight.call(thisGraph,d) / 2;
    });
    thisGraph.rectangles.selectAll('text.node-title').text(function(d) {
      // append "Test" if testRunActivated is true for source and sink
      const titleNode = TopologyUtils.getNodeRectClass(d);
      let title = thisGraph.testRunActivated ? (titleNode === "source" || titleNode === "datasink") ? `TEST-${d.parentType}` : d.uiname : d.uiname;
      if (title.length > 11) {
        return title.slice(0, 10) + '...';
      } else {
        return title;
      };
    }).attr("filter", function(d) {
      if (!d.isConfigured) {
        return "url(#grayscale)";
      } else {
        return "";
      }
    });
    thisGraph.rectangles.selectAll('text.parallelism-count').text(function(d) {
      return d.parallelismCount.toString().length < 2
        ? "0" + d.parallelismCount
        : d.parallelismCount;
    });

    if(!thisGraph.testRunActivated || (thisGraph.testRunActivated && thisGraph.props.eventLogData.length === 0)){
      // delete all eventLogData foreignObject on dom
      d3.selectAll('foreignObject.test-eventlog').remove();
    }

    //add new nodes
    var newGs = thisGraph.rectangles.enter().append("g");
    newGs.classed(constants.rectangleGClass, true).attr("transform", function(d) {
      return "translate(" + d.x + "," + d.y + ")";
    });

    //Outer Rectangle
    newGs.append("rect").attr("width", function(d){
      return constants.rectangleWidth + thisGraph.calculateHeight.call(thisGraph,d);
    })
    .attr("height", function(d){
      return thisGraph.calculateHeight.call(thisGraph,d);
    })
    .attr("class", function(d) {
      let classStr = "node-rectangle "+ TopologyUtils.getNodeRectClass(d);
      classStr += d.reconfigure ?  ' reconfig-node ' : '' ;
      return classStr;
    }).attr("filter", function(d) {
      if (!d.isConfigured) {
        return "url(#grayscale)";
      } else {
        return "";
      }
    }).attr("filter", function(d) {
      return "url(#dropshadow)";
    }).on("mouseover", function(d) {
      if (thisGraph.editMode) {
        d3.select(this.parentElement).select('text.fa.fa-times').style('display', thisGraph.testRunActivated ? 'none' : 'block');
      } else {
        TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, this));
      }
    }).on("mouseout", function(d) {
      if (thisGraph.editMode) {
        d3.select(this.parentElement).select('text.fa.fa-times').style('display', 'none');
      } else {
        thisGraph.toolTip.hide();
      }
    }).on('mousedown', function(d) {
      if (thisGraph.editMode) {
        thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
      }
    }).on('mouseup', function(d) {
      if (thisGraph.editMode) {
        thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
      }
    }).on('dblclick', function(d) {
      thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
      thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
    }).call(thisGraph.drag);
    //Image
    newGs.append("image").attr("xlink:href", function(d) {
      return d.nodeId
        ? d.imageURL
        : "styles/img/start-loader.gif";
    }).attr("width", constants.rectangleHeight - 15).attr("height", constants.rectangleHeight - 15).attr("x", 8).attr("y", 7).attr("filter", function(d) {
      return "url(#grayscale)";
    }).on("mouseover", function(d) {
      if (thisGraph.editMode) {
        d3.select(this.parentElement).select('text.fa.fa-times').style('display', thisGraph.testRunActivated ? 'none' : 'block');
      } else {
        TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, d3.select(this.parentElement).select('rect')));
      }
    }).on("mouseout", function(d) {
      if (thisGraph.editMode) {
        d3.select(this.parentElement).select('text.fa.fa-times').style('display', 'none');
      }
    }).on('mousedown', function(d) {
      if (thisGraph.editMode) {
        thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
      }
    }).on('mouseup', function(d) {
      if (thisGraph.editMode) {
        thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
      }
    }).on('dblclick', function(d) {
      thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
      thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
    }).on('error', function(d) {
      d.imageURL = 'styles/img/icon-' + d.parentType.toLowerCase() + '.png';
      thisGraph.updateGraph();
    }).call(thisGraph.drag);
    //Parallelism Icons
    newGs.append("text").attr("class", "fa fa-caret-right").attr("x", "173px").attr("y", "26px").text(function(d) {
      return '\uf0da';
    }).on("click", function(d) {
      if (thisGraph.editMode && !thisGraph.testRunActivated) {
        let value = parseInt(d.parallelismCount, 10) + 1;
        d.parallelismCount = value <= 1
          ? 1
          : value;
        clearTimeout(thisGraph.clickTimeout);
        thisGraph.clickTimeout = setTimeout(function() {
          TopologyUtils.updateParallelismCount(thisGraph.topologyId, this.versionId, d, thisGraph.setLastChange);
        }, 500);
        thisGraph.updateGraph();
      }
    });
    newGs.append("text").attr("class", "fa fa-caret-left").attr("x", "143px").attr("y", "26px").text(function(d) {
      return '\uf0d9';
    }).on("click", function(d) {
      if (thisGraph.editMode && !thisGraph.testRunActivated) {
        let value = parseInt(d.parallelismCount, 10) - 1;
        d.parallelismCount = value <= 1
          ? 1
          : value;
        clearTimeout(thisGraph.clickTimeout);
        thisGraph.clickTimeout = setTimeout(function() {
          TopologyUtils.updateParallelismCount(thisGraph.topologyId, this.versionId, d, thisGraph.setLastChange);
        }, 500);
        thisGraph.updateGraph();
      }
    });
    newGs.append("text").attr("class", "parallelism-count").attr("x", "162px").attr("y", "24px").attr("text-anchor", "middle").text(function(d) {
      return d.parallelismCount.toString().length < 2
        ? "0" + d.parallelismCount
        : d.parallelismCount;
    });
    //RHS Circle
    newGs.append("circle").attr("cx", function(d) {
      if (d.parentType !== 'SINK') {
        return (constants.rectangleWidth + thisGraph.calculateHeight.call(thisGraph,d) + 3.5);
      }
    }).attr("cy", function(d) {
      if (d.parentType !== 'SINK') {
        return thisGraph.calculateHeight.call(thisGraph,d) / 2;
      }
    }).attr("r", function(d) {
      if (d.parentType !== 'SINK') {
        return '5';
      }
    }).attr("class", function(d) {
      return TopologyUtils.getNodeRectClass(d);
    }).attr("filter", function(d) {
      if (!d.isConfigured) {
        return "url(#grayscale)";
      } else {
        return "";
      }
    }).on("mouseover", function(d) {
      if (internalFlags.shiftNodeDrag) {
        d3.select(this).classed(constants.connectClass, true);
      }
    }).on("mouseout", function(d) {
      d3.select(this).classed(constants.connectClass, false);
    }).on("mousedown", function(d) {
      if (thisGraph.editMode) {
        thisGraph.circleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
      }
    }).on("mouseup", function(d) {
      if (thisGraph.editMode) {
        thisGraph.circleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
      }
    }).call(thisGraph.drag);

    //LHS Circle
    newGs.append("circle").attr("cx", -3.5).attr("cy", function(d) {
      if (d.parentType !== 'SOURCE') {
        return (thisGraph.calculateHeight.call(thisGraph,d) / 2);
      }
    }).attr("r", function(d) {
      if (d.parentType !== 'SOURCE') {
        return '5';
      }
    }).attr("class", function(d) {
      return TopologyUtils.getNodeRectClass(d);
    }).attr("filter", function(d) {
      if (!d.isConfigured) {
        return "url(#grayscale)";
      } else {
        return "";
      }
    }).on("mouseup", function(d) {
      if (thisGraph.editMode) {
        thisGraph.circleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
      }
    });

    // For Test Mode Events EventObject append on rectangle parentNode
    if(thisGraph.testRunActivated){
      if(thisGraph.props.eventLogData.length){
        const p = d3.selectAll('rect')
          .data(thisGraph.nodes);
        p[0].forEach(function(c){
          const parentNode = d3.select(c.parentNode);
          const cNode = d3.select(c);
          const data = cNode.data();
          const testEvent = parentNode.selectAll('foreignObject')
            .data(data);
          testEvent.exit().remove();
          testEvent.enter().append("foreignObject")
          .attr("class", "test-eventlog")
          .style("display" , "block")
          .attr("width" , thisGraph.constants.rectangleWidth + 40 )
          .attr("height", 200)
          .attr('x', function(d){return 0;})
          .attr('y', function(d){return 40;});
          // ReactDOM render methods
          render(<EventLogComponent  eventLog={data[0]} eventPaginationClick={thisGraph.props.handleEventPaginationClick} />, testEvent.node());
        });
      }
    }

    //Label Text
    newGs.each(function(d) {
      // append "Test" if testRunActivated is true for source and sink
      const titleNode = TopologyUtils.getNodeRectClass(d);
      let gEl = d3.select(this),
        title = thisGraph.testRunActivated ? (titleNode === "source" || titleNode === "datasink") ? `TEST-${d.parentType}` : d.uiname : d.uiname,
        words = title.split(/\s+/g),
        nwords = words.length,
        nodeTitle = '';
      for (var i = 0; i < words.length; i++) {
        nodeTitle += words[i] + ' ';
      }
      let el = gEl.append("text").attr("class", function(d) {
        return 'node-title ' + TopologyUtils.getNodeRectClass(d);
      }).attr("filter", function(d) {
        if (!d.isConfigured) {
          return "url(#grayscale)";
        } else {
          return "";
        }
      }).attr("dx", function(d) {
        return (constants.rectangleHeight);
      }).attr("dy", function(d) {
        return ((constants.rectangleHeight / 2) - 2);
      }).on("mouseover", function(d) {
        if (thisGraph.editMode) {
          !thisGraph.testRunActivated && nodeTitle.trim().length > 11 ? thisGraph.showNodeTypeToolTip.call(thisGraph,d, this) : '';
          d3.select(this.parentElement).select('text.fa.fa-times').style('display', thisGraph.testRunActivated ? 'none' : 'block');
        } else {
          TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, d3.select(this.parentElement).select('rect')));
        }
      }).on("mouseout", function(d) {
        if (thisGraph.editMode) {
          d3.select(this.parentElement).select('text.fa.fa-times').style('display', 'none');
        }
      }).on('mousedown', function(d) {
        if (thisGraph.editMode) {
          thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
        }
      }).on('mouseup', function(d) {
        if (thisGraph.editMode) {
          thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
        }
      }).on('dblclick', function(d) {
        thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
        thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
      }).call(thisGraph.drag);
      if (nodeTitle.trim().length > 11) {
        nodeTitle = nodeTitle.trim().slice(0, 10) + '...';
      } else {
        nodeTitle = nodeTitle.trim();
      }
      el.text(nodeTitle.trim());
    });

    //label text for node type
    newGs.each(function(d) {
      let gEl = d3.select(this),
        title = d.nodeLabel.length > 15
          ? d.nodeLabel.slice(0, 10) + '...'
          : d.nodeLabel;
      let el = gEl.append("text").attr("class", function(d) {
        return 'node-type-label';
      }).attr("filter", function(d) {
        if (!d.isConfigured) {
          return "url(#grayscale)";
        } else {
          return "";
        }
      }).attr("dx", function(d) {
        return (constants.rectangleHeight);
      }).attr("dy", function(d) {
        return ((constants.rectangleHeight - 7));
      }).on("mouseover", function(d) {
        if (thisGraph.editMode) {
          d3.select(this.parentElement).select('text.fa.fa-times').style('display', thisGraph.testRunActivated ? 'none' : 'block');
        } else {
          TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, d3.select(this.parentElement).select('rect')));
        }
      }).on("mouseout", function(d) {
        if (thisGraph.editMode) {
          d3.select(this.parentElement).select('text.fa.fa-times').style('display', 'none');
        }
      }).on('mousedown', function(d) {
        if (thisGraph.editMode) {
          thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
        }
      }).on('mouseup', function(d) {
        if (thisGraph.editMode) {
          thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
        }
      }).on('dblclick', function(d) {
        thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
        thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
      }).call(thisGraph.drag);
      el.text(title.trim().toUpperCase());
    });

    //Delete Icon
    newGs.append("text").attr("class", "fa fa-times").attr("x", "-4px").attr("y", "5px").text(function(d) {
      return '\uf00d';
    }).style("display", "none").on("mouseover", function(d) {
      if (thisGraph.editMode) {
        this.style.display = thisGraph.testRunActivated ? 'none' : 'block';
      }
    }).on("mouseout", function(d) {
      if (thisGraph.editMode) {
        this.style.display = 'none';
      }
    }).on("mousedown", function(d) {
      if (thisGraph.editMode) {
        thisGraph.deleteNode(d);
      }
    });
    // remove old nodes
    thisGraph.rectangles.exit().remove();
  }

  render() {
    const {
      connectDropTarget,
      topologyId,
      versionId,
      versionsArr,
      viewMode,
      data
    } = this.props;
    this.editMode = !viewMode;
    this.topologyId = topologyId;
    this.versionId = versionId;
    this.versionsArr = versionsArr;
    this.nodes = data.nodes;
    this.uinamesList = data.uinamesList;
    this.edges = data.edges;
    this.metaInfo = data.metaInfo;
    this.linkShuffleOptions = data.linkShuffleOptions;
    this.graphTransforms = this.props.viewMode
      ? {
        dragCoords: [
          0, 0
        ],
        zoomScale: 0.8
      }
      : data.metaInfo.graphTransforms || {
        dragCoords: [
          0, 0
        ],
        zoomScale: 0.8
      };
    this.getModalScope = this.props.getModalScope;
    this.setModalContent = this.props.setModalContent;
    this.getEdgeConfigModal = this.props.getEdgeConfigModal;
    this.setLastChange = this.props.setLastChange;
    this.topologyConfigMessageCB = this.props.topologyConfigMessageCB;
    if (this.renderFlag) {
      d3.select("." + this.constants.graphClass).attr("transform", "translate(" + this.graphTransforms.dragCoords + ")" + "scale(" + this.graphTransforms.zoomScale + ")");
      this.dragSvg.translate(this.graphTransforms.dragCoords);
      this.dragSvg.scale(this.graphTransforms.zoomScale);
      this.dragSvg.event(this.svg);
    }
    return connectDropTarget(
      <svg className="topology-graph" onDragOver={(event) => {
        return window.dropevent = event.nativeEvent;
      }}></svg>
    );
  }
}
TopologyGraphComponent.defaultProps = {
  testRunActivated : false
};
