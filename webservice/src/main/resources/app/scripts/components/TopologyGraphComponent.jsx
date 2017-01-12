import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import { ItemTypes, Components } from '../utils/Constants';
import { DragDropContext, DropTarget } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import d3 from 'd3';
import d3Tip from 'd3-tip';
import $ from 'jquery';
import jQuery from 'jquery';
import TopologyUtils from '../utils/TopologyUtils'

window.$ = $;
window.jQuery = jQuery;

const componentTarget = {
	drop(props, monitor, component) {
        let parentRect = document.getElementsByClassName('graph-region')[0].getBoundingClientRect();
		const item = monitor.getItem();
		const delta = monitor.getClientOffset();
		let {x , y} = delta;
		x = x - parentRect.left;
		y = y - parentRect.top;
                component.createNode({x, y}, item);
	}
};

function collect(connect, monitor) {
	return {
		connectDropTarget: connect.dropTarget(),
	};
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
        getEdgeConfigModal: PropTypes.func
	};

	constructor(props){
		super(props);
		this.renderFlag = false;
	}

	componentWillUnmount(){
		d3.select('body').on("keydown", null).on("keyup", null);
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
        rectangleWidth: 145,
        rectangleHeight: 40
	};

	componentDidMount(){
		let thisGraph = this;
		let {width, height} = this.state;

		let svg = this.svg = d3.select(ReactDOM.findDOMNode(this))
            .attr('width', '100%')
            .attr('height', '100%');

		TopologyUtils.defineMarkers(svg);

        let svgG = this.svgG = svg.append("g")
			.classed(this.constants.graphClass, true);

		// displayed when dragging between nodes
		this.dragLine = svgG.append('svg:path')
			.attr('class', 'link dragline hidden')
			.attr('d', 'M0 0 L0 0')
			.attr("stroke-dasharray", "5, 5")
			.style('marker-end', 'url(#mark-end-arrow)');

                // svg nodes and edges
		this.paths = svgG.append("g").attr('class','link-group').selectAll("g");
		this.rectangles = svgG.append("g").selectAll("g");

        this.edgeStream = svgG
            .append('foreignObject')
            .attr("class", "edge-stream")
            .attr('width', 200)
            .attr('height', 200)
            .append("xhtml:body")
            .attr('class', 'edge-details')
            .style('display','none')
            .html('<p><strong>ID:</strong> </p>'+
                    '<p><strong>Grouping:</strong> </p>'+
                    '<p><button class="btn btn-xs btn-warning editEdge">Edit</button>'+
                    '<button class="btn btn-xs btn-warning deleteEdge">Delete</button></p>');

        this.toolTip = d3Tip()
            .attr('class', 'd3-tip')
            .offset([0, 10])
            .direction('e')
            .html('');
        svgG.call(this.toolTip);
        $('.container.wrapper').append($('body > .d3-tip'));

		this.drag = d3.behavior.drag()
			.origin(function(d) {
				return {
					x: d.x,
					y: d.y
				};
			})
			.on("drag", function(args) {
				if(thisGraph.editMode) {
					thisGraph.internalFlags.justDragged = true;
					thisGraph.dragMove.call(thisGraph, args);
				}
			})
			.on("dragend", function(node){
				if(thisGraph.editMode){
                                        let {topologyId, versionId, versionsArr, metaInfo} = thisGraph;
                                        if(versionId){
				let versionName = versionsArr.find((o)=>{return o.id == versionId}).name;
                                                if(versionName.toLowerCase() == 'current'){
				TopologyUtils.updateMetaInfo(topologyId, versionId, node, metaInfo);
				}
			}
				}
			});

		// listen for key events
		d3.select('body').on("keydown", function() {
                        if(d3.event.target.nodeName === 'BODY'){
                                thisGraph.svgKeyDown.call(thisGraph);
                        }
                })
                .on("keyup", function() {
                        if(d3.event.target.nodeName === 'BODY'){
                                thisGraph.svgKeyUp.call(thisGraph);
                        }
                });

		svg.on("mousedown", function(d) {
            if(thisGraph.setLastChange){
            	thisGraph.setLastChange(null);
            }
			thisGraph.svgMouseDown.call(thisGraph, d);
		});

		svg.on("mouseup", function(d) {
			thisGraph.svgMouseUp.call(thisGraph, d);
		});

                svg.on("mousemove", function(d) {
                        if(d3.event.target.nodeName === 'svg')
                                thisGraph.toolTip.hide();
                })

        // listen for dragging - also used for zoom in/out via buttons
        this.dragSvg = d3.behavior.zoom().scaleExtent([0, 8]).on("zoom", function(){
            thisGraph.internalFlags.justScaleTransGraph = true;
            d3.select("." + thisGraph.constants.graphClass)
                .attr("transform", "translate(" + thisGraph.dragSvg.translate() + ")" + "scale(" + thisGraph.dragSvg.scale() + ")");
        }).on("zoomend", function() {
            let sourceEvent = d3.event.sourceEvent;
            let gTranslate = thisGraph.dragSvg.translate(),
                gScaled = thisGraph.dragSvg.scale();

            thisGraph.metaInfo.graphTransforms = thisGraph.graphTransforms = {
                dragCoords: gTranslate,
                zoomScale: gScaled
            };
            clearTimeout(this.saveMetaInfoTimer);
            this.saveMetaInfoTimer = setTimeout(()=>{
                let {topologyId, versionId, versionsArr, metaInfo, editMode} = thisGraph;
				if(versionId && versionsArr && sourceEvent !== null){
					let versionName = versionsArr.find((o)=>{return o.id == versionId}).name;
	                if(versionName.toLowerCase() == 'current' && editMode){
						TopologyUtils.saveMetaInfo(topologyId, versionId, null, metaInfo, null);
					}
				}
            },500)
        });

        this.dragSvg.translate(this.graphTransforms.dragCoords);
        this.dragSvg.scale(this.graphTransforms.zoomScale);
        this.dragSvg.event(svg);

        //NOTE - To use scroll for zoom in/out, uncomment the below line
		svg.call(this.dragSvg).on("dblclick.zoom", null);

		this.updateGraph();
		this.renderFlag = true;
	}

	zoomAction(zoomType){
	    let thisGraph = this,
	            direction = 1,
	    factor = 0.2,
	    target_zoom = 1,
	    center = [thisGraph.svg[0][0].clientWidth / 2, thisGraph.svg[0][0].clientHeight / 2],
	    zoom = thisGraph.dragSvg,
	    extent = zoom.scaleExtent(),
	    translate = zoom.translate(),
	    translate0 = [],
	    l = [],
	    view = {x: translate[0], y: translate[1], k: zoom.scale()};

	    direction = (zoomType === 'zoom_in') ? 1 : -1;
	    target_zoom = zoom.scale() * (1 + factor * direction);

	    if (target_zoom < extent[0] || target_zoom > extent[1]) { return false; }

	    translate0 = [(center[0] - view.x) / view.k, (center[1] - view.y) / view.k];
	    view.k = target_zoom;
	    l = [translate0[0] * view.k + view.x, translate0[1] * view.k + view.y];

	    view.x += center[0] - l[0];
	    view.y += center[1] - l[1];

	    thisGraph.interpolateZoom([view.x, view.y], view.k);
	}

	interpolateZoom(translate, scale){
	    let thisGraph = this,
	        zoom = thisGraph.dragSvg;
	    return d3.transition().duration(350).tween("zoom", function () {
	        let iTranslate = d3.interpolate(zoom.translate(), translate),
	            iScale = d3.interpolate(zoom.scale(), scale);
	        return function (t) {
	            zoom
	                .scale(iScale(t))
	                .translate(iTranslate(t));
	            d3.select("." + thisGraph.constants.graphClass)
                    .attr("transform", "translate(" + thisGraph.dragSvg.translate() + ")" + "scale(" + thisGraph.dragSvg.scale() + ")");
	            thisGraph.metaInfo.graphTransforms = thisGraph.graphTransforms = {
                    dragCoords: thisGraph.dragSvg.translate(),
                    zoomScale: thisGraph.dragSvg.scale()
	            };
	            clearTimeout(this.saveMetaInfoTimer);
	            this.saveMetaInfoTimer = setTimeout(()=>{
                        let {topologyId, versionId, versionsArr, metaInfo, editMode} = thisGraph;
					if(versionId && versionsArr){
						let versionName = versionsArr.find((o)=>{return o.id == versionId}).name;
                                                if(versionName.toLowerCase() == 'current' && editMode){
							TopologyUtils.saveMetaInfo(topologyId, versionId, null, metaInfo, null);
						}
					}
	            },500)
	        };
	    });
	}

	dragMove(d){
		let {internalFlags, constants} = this;
		if (internalFlags.shiftNodeDrag) {
			if(internalFlags.failedTupleDrag){
				this.dragLine.attr('d', 'M' + (d.x + constants.rectangleWidth / 2)+ ',' + (d.y + constants.rectangleHeight + 10) + 'L' + d3.mouse(this.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
			} else {
				this.dragLine.attr('d', 'M' + (d.x + constants.rectangleWidth + constants.rectangleHeight )+ ',' + (d.y + constants.rectangleHeight / 2) + 'L' + d3.mouse(this.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
			}
		} else {
			d.x = d3.mouse(this.svgG.node())[0] - constants.rectangleWidth / 2;
			d.y = d3.mouse(this.svgG.node())[1] - constants.rectangleHeight / 2;
			this.updateGraph();
		}
	}

	pathMouseDown(d3path, d) {
		let {internalFlags, constants, paths, rectangles} = this;
		d3.event.stopPropagation();
		internalFlags.mouseDownLink = d;

		if (internalFlags.selectedNode) {
			TopologyUtils.removeSelectFromNode(rectangles, constants, internalFlags);
		}

		let prevEdge = internalFlags.selectedEdge;
		if (!prevEdge || prevEdge !== d) {
			TopologyUtils.replaceSelectEdge(d3, d3path, d, constants, internalFlags, paths);
            d3.select('.edge-stream').attr('x', this.getBoundingBoxCenter(d3path)[0]-100);
            d3.select('.edge-stream').attr('y', this.getBoundingBoxCenter(d3path)[1]);
            TopologyUtils.getEdgeData(d, this.topologyId, this.versionId, this.setEdgeData.bind(this));
		} else {
			TopologyUtils.removeSelectFromEdge(d3, paths, constants, internalFlags);
                        this.edgeStream.style('display', 'none');
		}
	}

    setEdgeData(obj) {
        let thisGraph = this;
        let name = obj.streamName;
        if(name.length > 18)
            name = name.slice(0, 17) + '...';

        if(thisGraph.editMode) {
            this.edgeStream.html('<div><p><strong>Stream:</strong> '+name+'</p>'+
                '<p><strong>Grouping:</strong> '+obj.grouping+'</p>'+
                '<p><button class="btn btn-xs btn-success editEdge"><i class="fa fa-pencil"></i> Edit</button> '+
                '<button class="btn btn-xs btn-danger deleteEdge"><i class="fa fa-trash"></i> Delete</button></p>'+
                '</div>');
            this.edgeStream.style('display', 'block');
            d3.select('.editEdge')
                .on("click", function() {
                    this.getEdgeConfigModal(this.topologyId, this.versionId, obj.edgeData, this.edges, this.updateGraph, null, obj.streamName, obj.grouping, obj.groupingFields);
                    this.edgeStream.style('display', 'none');
                }.bind(this));
            d3.select('.deleteEdge')
                .on("click", function() {
                    this.deleteEdge(this.internalFlags.selectedEdge);
                    this.edgeStream.style('display', 'none');
                }.bind(this));
        } else {
                        this.edgeStream.html('<div><p><strong>Stream:</strong> '+name+'</p>'+
                '<strong>Grouping:</strong> '+obj.grouping+
                '</div>');
            this.edgeStream.style('display', 'block');
        }
    }

    showNodeStreams(d, node, data) {
	if(data.inputSchema.length === 0 && data.outputSchema.length === 0) {
		this.toolTip.hide();
		return;
	}
	var thisGraph = this;
	var inputFieldsHtml = '', outputFieldsHtml = '';
	data.inputSchema.map((s)=>{
                                        return (inputFieldsHtml += '<li>'+s.name+(s.optional ? '':'<span class="text-danger">*</span>')+
                                                '<span class="output-type">'+s.type+'</span></li>');
                                });
	data.outputSchema.map((s)=>{
                                        return (outputFieldsHtml += '<li>'+s.name+(s.optional ? '':'<span class="text-danger">*</span>')+
                                                '<span class="output-type">'+s.type+'</span></li>');
                                });
        thisGraph.toolTip
                .html(function(d){
                return ('<div class="schema-tooltip clearfix"><h3>Schema</h3>'+
                        (inputFieldsHtml === '' ? '' : '<div class="input-schema"><h4>Input</h4><ul class="schema-list">'+inputFieldsHtml+'</ul></div>')+
                        (outputFieldsHtml === '' ? '' : '<div class="output-schema"><h4>Output</h4><ul class="schema-list">'+outputFieldsHtml+'</ul></div>')+
                        '</div>'
                        );
                });
        thisGraph.toolTip.show(data, node);
    }

	// mousedown on node
	rectangleMouseDown(d3node, d) {
		let {internalFlags} = this;
		d3.event.stopPropagation();
		internalFlags.mouseDownNode = d;
                this.edgeStream.style('display', 'none');
	}

	//mousedown on circle
	circleMouseDown(d3node, d) {
                let {internalFlags, constants, paths} = this;
		d3.event.stopPropagation();
		internalFlags.mouseDownNode = d;
                this.edgeStream.style('display', 'none');
                if(internalFlags.selectedEdge) {
                        TopologyUtils.removeSelectFromEdge(d3, paths, constants, internalFlags);
                }
		internalFlags.failedTupleDrag = false;
		if(d3.event.currentTarget.getAttribute('data-failedTuple') === 'true'){
			internalFlags.failedTupleDrag = true;
		}
		internalFlags.shiftNodeDrag = true;
                if(!d.isConfigured) {
                        this.dragLine.classed('hidden', true);
                        internalFlags.addEdgeFromNode = false;
                        return;
                }
		// reposition dragged directed edge
		this.dragLine.classed('hidden', false)
                        .attr('d', 'M' + d.x + Math.round(constants.rectangleWidth / 2) + ',' + d.y + constants.rectangleHeight + 'L' + d.x + Math.round(constants.rectangleWidth / 2) + ',' + d.y + constants.rectangleHeight);
		return;
	}

	// mouseup on nodes
	rectangleMouseUp(d3node, d) {
        let {topologyId, versionId, internalFlags, constants, dragLine, paths, edges, rectangles, getModalScope, setModalContent, nodes, linkShuffleOptions, metaInfo, getEdgeConfigModal, setLastChange} = this;
                return TopologyUtils.MouseUpAction(topologyId, versionId, d3node, d, metaInfo, internalFlags,
                        constants, dragLine, paths, nodes, edges, linkShuffleOptions, this.updateGraph.bind(this),
                        'rectangle', getModalScope, setModalContent, rectangles, getEdgeConfigModal, setLastChange);
	}

	// mouseup on circle
	circleMouseUp(d3node, d) {
        let {topologyId, versionId, internalFlags, constants, dragLine, paths, edges, rectangles, getModalScope, setModalContent, nodes, linkShuffleOptions, metaInfo, getEdgeConfigModal, setLastChange} = this;
                return TopologyUtils.MouseUpAction(topologyId, versionId, d3node, d, metaInfo, internalFlags,
			constants, dragLine, paths, nodes, edges, linkShuffleOptions, this.updateGraph.bind(this),
                        'circle', getModalScope, setModalContent, rectangles, getEdgeConfigModal, setLastChange);
	}

	// mousedown on main svg
	svgMouseDown() {
		this.internalFlags.graphMouseDown = true;
        let {paths, constants, internalFlags} = this;
        if(!event.target.closest('.edge-details') && internalFlags.selectedEdge) {
            TopologyUtils.removeSelectFromEdge(d3, paths, constants, internalFlags);
            this.edgeStream.style('display', 'none');
        }
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

    createNode(delta, itemObj){
                let {internalFlags, constants, nodes, topologyId, versionId, metaInfo, paths, edges, uinamesList, setLastChange} = this;
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

	// keydown on main svg
	svgKeyDown() {
		let {internalFlags, constants} = this;
		// make sure repeated key presses don't register for each keydown
		if (internalFlags.lastKeyDown !== -1) return;

		internalFlags.lastKeyDown = d3.event.keyCode;
		var selectedEdge = internalFlags.selectedEdge;
		// var selectedNode = internalFlags.selectedNode;

		switch (d3.event.keyCode) {
			case constants.BACKSPACE_KEY:
			case constants.DELETE_KEY:
				d3.event.preventDefault();
				if (selectedEdge) {
					this.deleteEdge(selectedEdge);
				// } else if (selectedNode){
				// 	this.deleteNode(selectedNode);
				}
				break;
		}
	}

	deleteNode(selectedNode){
        let {topologyId, versionId, nodes, edges, internalFlags, updateGraph, metaInfo, uinamesList, setLastChange} = this;
        TopologyUtils.deleteNode(topologyId, versionId, selectedNode, nodes, edges, internalFlags, updateGraph.bind(this), metaInfo, uinamesList, setLastChange);
	}

	deleteEdge(selectedEdge){
        let {topologyId, versionId, internalFlags, edges, nodes, updateGraph, setLastChange} = this;
        TopologyUtils.deleteEdge(selectedEdge, topologyId, versionId, internalFlags, edges, nodes, updateGraph.bind(this), setLastChange);
        this.edgeStream.style('display', 'none');
	}

	svgKeyUp() {
		this.internalFlags.lastKeyDown = -1;
	}

	clonePaths(){
		let element = document.querySelectorAll('.link:not(.hidden)');
		for(let i = 0, len = element.length ; i < len; i++){
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
            return [bbox.x + bbox.width/2, bbox.y + bbox.height/2];
        }

	updateGraph() {
    let that = this;
		var duplicateLinks = document.getElementsByClassName('visible-link')
		while(duplicateLinks.length > 0){
			duplicateLinks[0].remove();
		}
		var thisGraph = this,
			constants = thisGraph.constants,
			internalFlags = thisGraph.internalFlags;

    // change every nodes y: value for viewMode
    if(that.props.viewMode){
      let flag  = true;
      thisGraph.nodes.map(x => {
          return x.y > 300 ? flag = true :  flag = false
      });
      if(flag){
        thisGraph.nodes.map(x => {
          return x.y = (x.y/4);
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
			return TopologyUtils.createLineOnUI(d, constants);
		});

		// add new paths
		paths.enter()
			.append("path")
			.classed("link", true)
			.attr("d", function(d){
				return TopologyUtils.createLineOnUI(d, constants);
			})
			.attr("stroke-opacity", "0.0001")
			.attr("stroke-width", "10")
			.attr("data-name", function(d){ return d.source.nodeId +'-'+d.target.nodeId; })
            .on("mouseover", function(d) {
                if(!thisGraph.editMode) {
                    let elem = document.querySelectorAll('.visible-link[d="'+this.getAttribute('d')+'"]')[0];
                    let d3path = d3.select(elem);
                    d3.select('.edge-stream').attr('x', thisGraph.getBoundingBoxCenter(d3path)[0]-100);
                    d3.select('.edge-stream').attr('y', thisGraph.getBoundingBoxCenter(d3path)[1]);
                    TopologyUtils.getEdgeData(d, thisGraph.topologyId, thisGraph.versionId, thisGraph.setEdgeData.bind(thisGraph));
                }
            })
            .on("mouseout", function(d) {
                if(!thisGraph.editMode) {
                    thisGraph.edgeStream.style('display', 'none');
                }
            })
			.on("mousedown", function(d){
				if(thisGraph.editMode) {
					let elem = document.querySelectorAll('.visible-link[d="'+this.getAttribute('d')+'"]')[0];
					thisGraph.pathMouseDown.call(thisGraph, d3.select(elem), d);
				}
			})
			.on("mouseup", function(d) {
				if(thisGraph.editMode){
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

		thisGraph.rectangles.selectAll('rect')
            .attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; })
            .attr("filter", function(d){ return "url(#dropshadow)"; });
		thisGraph.rectangles.selectAll('image')
            .attr("filter", function(d){ return "url(#grayscale)"; });
		thisGraph.rectangles.selectAll('circle')
			.attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; });
		thisGraph.rectangles.selectAll('text.node-title')
            .text(function(d){
                if(d.uiname.length > 11) {return d.uiname.slice(0, 10) + '...';} else return d.uiname;
            })
			.attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; });
		thisGraph.rectangles.selectAll('text.parallelism-count')
			.text(function(d){
				return d.parallelismCount.toString().length < 2 ? "0"+d.parallelismCount : d.parallelismCount;
			});

		//add new nodes
		var newGs = thisGraph.rectangles.enter()
				.append("g");
			newGs.classed(constants.rectangleGClass, true)
				.attr("transform", function(d) {
				return "translate(" + d.x + "," + d.y + ")";
			});

        //Outer Rectangle
        newGs.append("rect").attr("width", constants.rectangleWidth + constants.rectangleHeight).attr("height",constants.rectangleHeight-1)
            .attr("class", function(d){ return 'node-rectangle ' + TopologyUtils.getNodeRectClass(d);})
            .attr("filter", function(d){ if(!d.isConfigured) return "url(#grayscale)"; else return ""; })
            .attr("filter", function(d){ return "url(#dropshadow)"; })
            .on("mouseover", function(d){
                if(thisGraph.editMode) {
                        d3.select(this.parentElement).select('text.fa.fa-times').style('display','block');
                } else {
                        TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, this));
                }
            })
            .on("mouseout", function(d){
                if(thisGraph.editMode){
                        d3.select(this.parentElement).select('text.fa.fa-times').style('display','none');
                        } else {
                                thisGraph.toolTip.hide();
                        }
            })
            .on('mousedown', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d); })
            .on('mouseup', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);})
            .on('dblclick', function(d){
                thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
                thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
            })
            .call(thisGraph.drag);
        //Image
        newGs.append("image").attr("xlink:href", function(d){return d.imageURL;})
            .attr("width", constants.rectangleHeight - 15).attr("height", constants.rectangleHeight - 15).attr("x", 8).attr("y", 7)
            .attr("filter", function(d){ return "url(#grayscale)"; })
            .on("mouseover", function(d){
				if(thisGraph.editMode) {
					d3.select(this.parentElement).select('text.fa.fa-times').style('display','block');
				} else {
					TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, d3.select(this.parentElement).select('rect')));
				}
            })
            .on("mouseout", function(d){
				if(thisGraph.editMode) {
				 d3.select(this.parentElement).select('text.fa.fa-times').style('display','none');
				}
            })
            .on('mousedown', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);})
            .on('mouseup', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);})
            .on('dblclick', function(d){
                thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
                thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
            })
            .call(thisGraph.drag);
        //Parallelism Icons
        newGs.append("text").attr("class","fa fa-caret-up").attr("x","165px").attr("y","13px")
            .text(function(d){return '\uf0d8';})
            .on("click", function(d){
                if(thisGraph.editMode){
                    let value = parseInt(d.parallelismCount, 10) + 1;
                    d.parallelismCount = value <= 0 ? 0 : value;
                    clearTimeout(thisGraph.clickTimeout);
                    thisGraph.clickTimeout = setTimeout(function(){
                        TopologyUtils.updateParallelismCount(thisGraph.topologyId, this.versionId, d, thisGraph.setLastChange);
                    },500)
                    thisGraph.updateGraph();
                }
            });
        newGs.append("text").attr("class","fa fa-caret-down").attr("x","165px").attr("y","35px")
            .text(function(d){return '\uf0d7';})
            .on("click", function(d){
                if(thisGraph.editMode){
                    let value = parseInt(d.parallelismCount, 10) - 1;
                    d.parallelismCount = value <= 0 ? 0 : value;
                    clearTimeout(thisGraph.clickTimeout);
                    thisGraph.clickTimeout = setTimeout(function(){
                        TopologyUtils.updateParallelismCount(thisGraph.topologyId, this.versionId, d, thisGraph.setLastChange);
                    },500)
                    thisGraph.updateGraph();
                }
            });
        newGs.append("text").attr("class","parallelism-count").attr("x","163px").attr("y","24px")
            .text(function(d){return d.parallelismCount.toString().length < 2 ? "0"+d.parallelismCount : d.parallelismCount;});
        //RHS Circle
        newGs.append("circle")
            .attr("cx", function (d) { if(d.parentType !== 'SINK') return (constants.rectangleWidth + constants.rectangleHeight + 3.5); })
            .attr("cy", function (d) { if(d.parentType !== 'SINK') return constants.rectangleHeight / 2; })
            .attr("r", function (d) { if(d.parentType !== 'SINK') return '5'; })
            .attr("class", function(d){ return TopologyUtils.getNodeRectClass(d);})
            .attr("filter", function(d){ if(!d.isConfigured) return "url(#grayscale)"; else return ""; })
            .on("mouseover", function(d) {
                if (internalFlags.shiftNodeDrag) {
                    d3.select(this).classed(constants.connectClass, true);
                }
            })
            .on("mouseout", function(d) {
                d3.select(this).classed(constants.connectClass, false);
            })
            .on("mousedown", function(d) {
                if(thisGraph.editMode){
                        thisGraph.circleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
                }
            })
            .on("mouseup", function(d) {
                if(thisGraph.editMode){
                    thisGraph.circleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
                }
            })
            .call(thisGraph.drag);

        //LHS Circle
        newGs.append("circle")
            .attr("cx", -3.5)
            .attr("cy", function (d) { if(d.parentType !== 'SOURCE') return (constants.rectangleHeight / 2); })
            .attr("r", function (d) { if(d.parentType !== 'SOURCE') return '5'; })
            .attr("class", function(d){ return TopologyUtils.getNodeRectClass(d);})
            .attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; })
            .on("mouseup", function(d) {
                if(thisGraph.editMode){
                    thisGraph.circleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
                }
            });

        //Label Text
        newGs.each(function(d) {
            let gEl = d3.select(this),
                title = d.uiname,
                words = title.split(/\s+/g),
                nwords = words.length,
                nodeTitle = '';
            let el = gEl.append("text")
                .attr("class", function(d){ return 'node-title '+TopologyUtils.getNodeRectClass(d);})
                .attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; })
                .attr("dx", function(d){ return (constants.rectangleHeight); })
                .attr("dy", function(d){ return ((constants.rectangleHeight / 2) - 2); })
                .on("mouseover", function(d){
					if(thisGraph.editMode) {
						 d3.select(this.parentElement).select('text.fa.fa-times').style('display','block');
					} else {
						TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, d3.select(this.parentElement).select('rect')));
					}
                })
     	       .on("mouseout", function(d){
                    if(thisGraph.editMode) {
						d3.select(this.parentElement).select('text.fa.fa-times').style('display','none');
                    }
                })
                .on('mousedown', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);})
				.on('mouseup', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);})
                .on('dblclick', function(d){
					thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
                    thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
				})
				.call(thisGraph.drag);
            for (var i = 0; i < words.length; i++) {
                nodeTitle += words[i]+' ';
            }
            if(nodeTitle.trim().length > 11) {
                nodeTitle = nodeTitle.trim().slice(0, 10) + '...';
            } else nodeTitle = nodeTitle.trim();
            el.text(nodeTitle.trim());
        });

        //label text for node type
        newGs.each(function(d) {
            let gEl = d3.select(this),
                title = d.nodeLabel.length > 15 ? d.nodeLabel.slice(0, 10) + '...' : d.nodeLabel ;
            let el = gEl.append("text")
                .attr("class", function(d){ return 'node-type-label';})
                .attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; })
                .attr("dx", function(d){ return (constants.rectangleHeight); })
                .attr("dy", function(d){ return ((constants.rectangleHeight - 7)); })
                .on("mouseover", function(d){
			if(thisGraph.editMode) {
				d3.select(this.parentElement).select('text.fa.fa-times').style('display','block');
			} else {
				TopologyUtils.getNodeStreams(thisGraph.topologyId, thisGraph.versionId, d.nodeId, d.parentType, thisGraph.edges, thisGraph.showNodeStreams.bind(thisGraph, d, d3.select(this.parentElement).select('rect')));
			}
                })
                .on("mouseout", function(d){
			if(thisGraph.editMode) {
				d3.select(this.parentElement).select('text.fa.fa-times').style('display','none');
			}
                })
                .on('mousedown', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);})
                .on('mouseup', function(d){ if(thisGraph.editMode) thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);})
                .on('dblclick', function(d){
                    thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
                    thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
                })
                .call(thisGraph.drag);
            el.text(title.trim());
        });

        //Delete Icon
        newGs.append("text").attr("class","fa fa-times").attr("x","-4px").attr("y","5px")
            .text(function(d){return '\uf00d';}).style("display","none")
            .on("mouseover",function(d){if(thisGraph.editMode) this.style.display = 'block'})
            .on("mouseout",function(d){if(thisGraph.editMode) this.style.display = 'none'})
            .on("mousedown",function(d){if(thisGraph.editMode) thisGraph.deleteNode(d)})
		// remove old nodes
		thisGraph.rectangles.exit().remove();
	}

	render(){
        const { connectDropTarget, topologyId, versionId, versionsArr, viewMode, data } = this.props;
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
                                                dragCoords: [0,0],
						zoomScale: 0.8
					}
                            : data.metaInfo.graphTransforms || { dragCoords: [0,0],	zoomScale: 0.8};
		this.getModalScope = this.props.getModalScope;
		this.setModalContent = this.props.setModalContent;
        this.getEdgeConfigModal = this.props.getEdgeConfigModal;
        this.setLastChange = this.props.setLastChange;
		if(this.renderFlag){
            d3.select("." + this.constants.graphClass)
                .attr("transform", "translate(" + this.graphTransforms.dragCoords + ")" + "scale(" + this.graphTransforms.zoomScale + ")");
            this.dragSvg.translate(this.graphTransforms.dragCoords);
            this.dragSvg.scale(this.graphTransforms.zoomScale);
            this.dragSvg.event(this.svg);

			this.updateGraph();
		}
		return connectDropTarget(
                        <svg className="topology-graph" onDragOver={(event) => {
          return window.dropevent = event.nativeEvent}}></svg>
		)
	}
}
