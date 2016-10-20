import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import { ItemTypes, Components } from '../utils/Constants';
import { DragDropContext, DropTarget } from 'react-dnd';
import HTML5Backend from 'react-dnd-html5-backend';
import d3 from 'd3';
import TopologyUtils from '../utils/TopologyUtils'

const componentTarget = {
	drop(props, monitor, component) {
		let parentRect = document.getElementsByClassName('box')[0].getBoundingClientRect();
		const item = monitor.getItem();
		const delta = monitor.getClientOffset();
		let {x , y} = delta;
		x = x - parentRect.left;
		y = y - parentRect.top;
		component.createNode({x, y}, item.imgPath, item.type, item.name, item.nodeType);
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
		setModalContent: PropTypes.func.isRequired
	};

	constructor(props){
		super(props);
		this.renderFlag = false;
	}

	componentWillUnmount(){
		d3.select('body').on("keydown", null).on("keyup", null);
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
					let {topologyId, metaInfo} = thisGraph;
					TopologyUtils.updateMetaInfo(topologyId, node, metaInfo);
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
			thisGraph.svgMouseDown.call(thisGraph, d);
		});

		svg.on("mouseup", function(d) {
			thisGraph.svgMouseUp.call(thisGraph, d);
		});

		// listen for dragging
		// this.dragSvg = d3.behavior.zoom().scaleExtent([0, 8]).on("zoom", function(){
		// 	thisGraph.internalFlags.justScaleTransGraph = true;
		// 	d3.select("." + thisGraph.constants.graphClass)
		// 		.attr("transform", "translate(" + thisGraph.dragSvg.translate() + ")" + "scale(" + thisGraph.dragSvg.scale() + ")");
		// }).on("zoomend", function() {
		// 	let gTranslate = thisGraph.dragSvg.translate(),
		// 		gScaled = thisGraph.dragSvg.scale();

		// 	thisGraph.graphTransforms = {
		// 		dragCoords: gTranslate,
		// 		zoomScale: gScaled
		// 	};
		// 	console.info("Save graph transform values.");
		// });

		// this.dragSvg.translate(this.graphTransforms.dragCoords);
		// this.dragSvg.scale(this.graphTransforms.zoomScale);
		// this.dragSvg.event(svg);

		// svg.call(this.dragSvg).on("dblclick.zoom", null);

		this.updateGraph();
		this.renderFlag = true;
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
		} else {
			TopologyUtils.removeSelectFromEdge(d3, paths, constants, internalFlags);
		}
	}

	// mousedown on node
	rectangleMouseDown(d3node, d) {
		let {internalFlags} = this;
		d3.event.stopPropagation();
		internalFlags.mouseDownNode = d;
	}

	//mousedown on circle
	circleMouseDown(d3node, d) {
		let {internalFlags, constants} = this;
		d3.event.stopPropagation();
		internalFlags.mouseDownNode = d;
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
		let {topologyId, internalFlags, constants, dragLine, paths, edges, rectangles, getModalScope, setModalContent, nodes, linkShuffleOptions, metaInfo} = this;
		return TopologyUtils.MouseUpAction(topologyId, d3node, d, metaInfo, internalFlags, 
			constants, dragLine, paths, nodes, edges, linkShuffleOptions, this.updateGraph.bind(this), 
			'rectangle', getModalScope, setModalContent, rectangles);
	}

	// mouseup on circle
	circleMouseUp(d3node, d) {
		let {topologyId, internalFlags, constants, dragLine, paths, edges, rectangles, getModalScope, setModalContent, nodes, linkShuffleOptions, metaInfo} = this;
		return TopologyUtils.MouseUpAction(topologyId, d3node, d, metaInfo, internalFlags, 
			constants, dragLine, paths, nodes, edges, linkShuffleOptions, this.updateGraph.bind(this),
			'circle', getModalScope, setModalContent, rectangles);
	}

	// mousedown on main svg
	svgMouseDown() {
		this.internalFlags.graphMouseDown = true;
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

	createNode(delta, imgUrl, parentType, name, currentType){
		let {internalFlags, constants, nodes, topologyId, metaInfo, paths, edges, uinamesList} = this;
		internalFlags.graphMouseDown = true;
		// var xycoords = d3.mouse(thisGraph.svgG.node()),
		let d = {
			x: delta.x + (constants.rectangleWidth / 2) - constants.rectangleWidth,
			y: delta.y - (constants.rectangleHeight / 2) - constants.rectangleHeight - 5.5,
			parentType: parentType,
			currentType: currentType,
			uiname: name,
			imageURL: imgUrl,
			isConfigured: false,
                        parallelismCount: 1,
                        nodeLabel: name
		};
		nodes.push(d);
		let createNodeArr = [d];
		if(d.currentType === Components.Datasources[0].name){ // Device => Parser
			let parserObj = this.createParserNode(d);
			createNodeArr.push(parserObj);
		} else if (d.currentType === Components.Processors[4].name){ // Split => Stage => Join
			let stageJoinArr = this.createStageJoinNode(d);
			createNodeArr.push(...stageJoinArr);
		}
		TopologyUtils.createNode(topologyId, createNodeArr, this.updateGraph.bind(this), metaInfo, paths, edges, internalFlags, uinamesList);
		internalFlags.graphMouseDown = false;
	}

	createParserNode(d) {
		let newObject = JSON.parse(JSON.stringify(d));
		newObject.x += 200;
		newObject.uiname = 'Parser';
		newObject.parentType = Components.Processor.value;
		newObject.currentType = Components.Processors[0].name;
		newObject.imageURL = Components.Processors[0].imgPath;
		this.nodes.push(newObject);
		return newObject;
	}
	createStageJoinNode(d){
		let arr = [];

		let stageObj = JSON.parse(JSON.stringify(d));
                stageObj.x += 220;
		stageObj.uiname = 'Stage';
		stageObj.parentType = Components.Processor.value;
		stageObj.currentType = Components.Processors[5].name;
		stageObj.imageURL = Components.Processors[5].imgPath;
		this.nodes.push(stageObj);
		arr.push(stageObj);

		let joinObj = JSON.parse(JSON.stringify(d));
                joinObj.x += 440;
		joinObj.uiname = 'Join';
		joinObj.parentType = Components.Processor.value;
		joinObj.currentType = Components.Processors[6].name;
		joinObj.imageURL = Components.Processors[6].imgPath;
		this.nodes.push(joinObj);
		arr.push(joinObj);

		return arr;
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
		let {topologyId, nodes, edges, internalFlags, updateGraph, metaInfo, uinamesList} = this;
		TopologyUtils.deleteNode(topologyId, selectedNode, nodes, edges, internalFlags, updateGraph.bind(this), metaInfo, uinamesList);
	}

	deleteEdge(selectedEdge){
		let {topologyId, internalFlags, edges, updateGraph} = this;
		TopologyUtils.deleteEdge(selectedEdge, topologyId, internalFlags, edges, updateGraph.bind(this));
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

	updateGraph() {
		var duplicateLinks = document.getElementsByClassName('visible-link')
		while(duplicateLinks.length > 0){
			duplicateLinks[0].remove();
		}
		var thisGraph = this,
			constants = thisGraph.constants,
			internalFlags = thisGraph.internalFlags;

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
			// .attr("d", d3.svg.line()
			// 	.interpolate('cardinal')
			// 	.x(function(d) {
			// 		console.log(d);
			// 		return x(d.date); 
			// 	})
			// 	.y(function(d) {
			// 		console.log(d);
			// 		return y(d.close); 
			// 	})
			// )
			.attr("d", function(d){
				return TopologyUtils.createLineOnUI(d, constants);
			})
			.attr("stroke-opacity", "0.0001")
			.attr("stroke-width", "10")
			.attr("data-name", function(d){ return d.source.nodeId +'-'+d.target.nodeId; })
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
			.attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; });
		thisGraph.rectangles.selectAll('image')
                        .attr("filter", function(d){ return "url(#grayscale)"; });
		thisGraph.rectangles.selectAll('circle')
			.attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; });
		thisGraph.rectangles.selectAll('text.node-title')
                        .text(function(d){
                                if(d.uiname.length > 14) {return d.uiname.slice(0, 13) + '...';} else return d.uiname;
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
				.on("mouseover", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','block'); })
				.on("mouseout", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','none'); })
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
				.on("mouseover", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','block'); })
                                .on("mouseout", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','none'); })
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
							TopologyUtils.updateParallelismCount(thisGraph.topologyId, d);
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
							TopologyUtils.updateParallelismCount(thisGraph.topologyId, d);
						},500)
						thisGraph.updateGraph();
					}
				});
                        newGs.append("text").attr("class","parallelism-count").attr("x","163px").attr("y","24px")
				.text(function(d){return d.parallelismCount.toString().length < 2 ? "0"+d.parallelismCount : d.parallelismCount;});
			//RHS Circle
			newGs.append("circle")
                                .attr("cx", function (d) { if(d.parentType !== Components.Sink.value) return (constants.rectangleWidth + constants.rectangleHeight + 3.5); })
		        .attr("cy", function (d) { if(d.parentType !== Components.Sink.value) return constants.rectangleHeight / 2; })
                        .attr("r", function (d) { if(d.parentType !== Components.Sink.value) return '5'; })
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
			//Failed Parser Circle
			newGs.append("circle")
				.attr("cx", function (d) { if(d.currentType === Components.Processors[0].name) return (constants.rectangleWidth / 2);})
		        .attr("cy", function (d) { if(d.currentType === Components.Processors[0].name) return constants.rectangleHeight;})
                        .attr("r", function (d) { if(d.currentType === Components.Processors[0].name) return '5';})
			    .attr("filter", function(d){ if(!d.isConfigured) return "url(#grayscale)"; else return ""; })
			    .attr("data-failedTuple", true)
		        .style("fill", "red")
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
		        .attr("cy", function (d) { if(d.parentType !== Components.Datasource.value) return (constants.rectangleHeight / 2); })
                        .attr("r", function (d) { if(d.parentType !== Components.Datasource.value) return '5'; })
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
					.on("mouseover", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','block'); })
					.on("mouseout", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','none'); })
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
                                if(nodeTitle.trim().length > 14) {
                                        nodeTitle = nodeTitle.trim().slice(0, 13) + '...';
                                } else nodeTitle = nodeTitle.trim();
                                el.text(nodeTitle.trim());
                        });

                        //label text for node type
                        newGs.each(function(d) {
                                let gEl = d3.select(this),
                                        title = d.nodeLabel;
                                let el = gEl.append("text")
                                        .attr("class", function(d){ return 'node-type-label';})
                                        .attr("filter", function(d){ if(!d.isConfigured){ return "url(#grayscale)"; } else return ""; })
                                        .attr("dx", function(d){ return (constants.rectangleHeight); })
                                        .attr("dy", function(d){ return ((constants.rectangleHeight - 7)); })
                                        .on("mouseover", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','block'); })
                                        .on("mouseout", function(d){ if(thisGraph.editMode) d3.select(this.parentElement).select('text.fa.fa-times').style('display','none'); })
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
		const { connectDropTarget, topologyId, viewMode, data } = this.props;
		this.editMode = !viewMode;
		this.topologyId = topologyId;
		this.nodes = data.nodes;
		this.uinamesList = data.uinamesList;
		this.edges = data.edges;
		this.metaInfo = data.metaInfo;
		this.linkShuffleOptions = data.linkShuffleOptions;
		this.graphTransforms = data.graphTransforms || {
			dragCoords: [0,0],
			zoomScale: 1
		};
		this.getModalScope = this.props.getModalScope;
		this.setModalContent = this.props.setModalContent;
		if(this.renderFlag){
			this.updateGraph();
		}
		return connectDropTarget(
			<svg className="topology-graph"></svg>
		)
	}
}