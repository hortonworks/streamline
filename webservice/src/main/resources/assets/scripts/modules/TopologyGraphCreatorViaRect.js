define(['require', 
	'utils/Globals', 
	'bootbox', 
	'd3', 
	'd3-tip'],
	function(require, Globals, bootbox, d3) {
	'use strict';

	var TopologyGraphCreator = function(options) {
		//pass elem, data{node,edges}, vent
		var thisGraph = this;
		_.extend(thisGraph, options);
		thisGraph.bindEvents();
		var svg = d3.select('#'+thisGraph.elem.attr('id')).append('svg')
					.attr('width', thisGraph.elem.width() ? thisGraph.elem.width() : 1220)
					.attr('height', thisGraph.elem.height() ? thisGraph.elem.height() : 600);

		var nodes = thisGraph.data.nodes,
			edges = thisGraph.data.edges;
		
		thisGraph.idct = 0;

		thisGraph.nodes = nodes || [];
		thisGraph.edges = edges || [];

		thisGraph.state = {
			selectedNode: null,
			selectedEdge: null,
			mouseDownNode: null,
			mouseDownLink: null,
			justDragged: false,
			justScaleTransGraph: false,
			lastKeyDown: -1,
			shiftNodeDrag: false,
			selectedText: null,
			failedTupleDrag: false
		};

		// define arrow markers for graph links
		var defs = svg.append('svg:defs');
		defs.append('svg:marker')
			.attr('id', 'end-arrow')
			.attr('viewBox', '0 -5 10 10')
			.attr('refX', "10")
			.attr('markerWidth', 3.5)
			.attr('markerHeight', 3.5)
			.attr('orient', 'auto')
			.append('svg:path')
			.attr('d', 'M0,-5L10,0L0,5');

		// define arrow markers for leading arrow
		defs.append('svg:marker')
			.attr('id', 'mark-end-arrow')
			.attr('viewBox', '0 -5 10 10')
			.attr('refX', 7)
			.attr('markerWidth', 3.5)
			.attr('markerHeight', 3.5)
			.attr('orient', 'auto')
			.append('svg:path')
			.attr('d', 'M0,-5L10,0L0,5');

		thisGraph.svg = svg;
		thisGraph.svgG = svg.append("g")
			.attr('transform','translate('+ (thisGraph.elem.width() ? thisGraph.elem.width() : 1220) + ',' + (thisGraph.elem.height() ? thisGraph.elem.height() : 600) + ')')
			.classed(thisGraph.consts.graphClass, true);
		var svgG = thisGraph.svgG;

		// displayed when dragging between nodes
		thisGraph.dragLine = svgG.append('svg:path')
			.attr('class', 'link dragline hidden')
			.attr('d', 'M0,0L0,0')
			.style('marker-end', 'url(#mark-end-arrow)');

		// svg nodes and edges 
		thisGraph.paths = svgG.append("g").selectAll("g");
		thisGraph.rectangles = svgG.append("g").selectAll("g");

		thisGraph.drag = d3.behavior.drag()
			.origin(function(d) {
				return {
					x: d.x,
					y: d.y
				};
			})
			.on("drag", function(args) {
				thisGraph.state.justDragged = true;
				thisGraph.dragmove.call(thisGraph, args);
			})
			.on("dragend", function() {
				// todo check if edge-mode is selected
			});

		// listen for key events
		d3.select(window).on("keydown", function() {
				thisGraph.svgKeyDown.call(thisGraph);
			})
			.on("keyup", function() {
				thisGraph.svgKeyUp.call(thisGraph);
			});
		svg.on("mousedown", function(d) {
			thisGraph.svgMouseDown.call(thisGraph, d);
		});
		svg.on("mouseup", function(d) {
			thisGraph.svgMouseUp.call(thisGraph, d);
		});

		// listen for dragging
		// var dragSvg = d3.behavior.zoom()
		// 	.on("zoom", function() {
		// 		if (d3.event.sourceEvent.shiftKey) {
		// 			// TODO  the internal d3 state is still changing
		// 			return false;
		// 		} else {
		// 			thisGraph.zoomed.call(thisGraph);
		// 		}
		// 		return true;
		// 	})
		// 	.on("zoomstart", function() {
		// 		var ael = d3.select("#" + thisGraph.consts.activeEditId).node();
		// 		if (ael) {
		// 			ael.blur();
		// 		}
		// 		if (!d3.event.sourceEvent.shiftKey) d3.select('body').style("cursor", "move");
		// 	})
		// 	.on("zoomend", function() {
		// 		d3.select('body').style("cursor", "auto");
		// 	});

		// svg.call(dragSvg).on("dblclick.zoom", null);

		// listen for resize
		window.onresize = function() {
			thisGraph.updateWindow(svg);
		};
	};

	TopologyGraphCreator.prototype.bindEvents = function(){
		var thisGraph = this;
		this.vent.listenTo(this.vent, 'change:editor-submenu', function(obj){
			thisGraph.nodeTitle = obj.title;
			thisGraph.nodeParentType = obj.parentStep;
			thisGraph.currentStep = obj.currentStep;
			thisGraph.icon = obj.icon;
			thisGraph.nodeId = obj.id;
			if(!_.isUndefined(obj.otherId)) thisGraph.otherId = obj.otherId ;
			d3.event = obj.event;
			thisGraph.createNode();
		});
	};

	TopologyGraphCreator.prototype.setIdCt = function(idct) {
		this.idct = idct;
	};

	TopologyGraphCreator.prototype.consts = {
		selectedClass: "selected",
		connectClass: "connect-node",
		rectangleGClass: "conceptG",
		graphClass: "graph",
		activeEditId: "active-editing",
		BACKSPACE_KEY: 8,
		DELETE_KEY: 46,
		ENTER_KEY: 13,
		nodeRadius: 40,
		rectangleWidth: 130,
		rectangleHeight: 70
	};

	/* PROTOTYPE FUNCTIONS */

	TopologyGraphCreator.prototype.dragmove = function(d) {
		var thisGraph = this;
		if (thisGraph.state.shiftNodeDrag) {
			if(thisGraph.state.failedTupleDrag){
				thisGraph.dragLine.attr('d', 'M' + (d.x + thisGraph.consts.rectangleWidth / 2)+ ',' + (d.y + thisGraph.consts.rectangleHeight) + 'L' + d3.mouse(thisGraph.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
			} else {
				thisGraph.dragLine.attr('d', 'M' + (d.x + thisGraph.consts.rectangleWidth )+ ',' + (d.y + thisGraph.consts.rectangleHeight / 2) + 'L' + d3.mouse(thisGraph.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
			}
		} else {
			d.x = d3.mouse(thisGraph.svgG.node())[0] - thisGraph.consts.rectangleWidth / 2;
			d.y = d3.mouse(thisGraph.svgG.node())[1] - thisGraph.consts.rectangleHeight / 2;
			thisGraph.updateGraph();
		}
	};

	TopologyGraphCreator.prototype.clearGraph = function(skipPrompt) {
		var thisGraph = this,
			doClear = true;
		if (!skipPrompt) {
			doClear = bootbox.confirm("Press OK to delete this graph");
		}
		if (doClear) {
			thisGraph.nodes = [];
			thisGraph.edges = [];
			thisGraph.updateGraph();
		}
	};

	/* select all text in element: taken from http://stackoverflow.com/questions/6139107/programatically-select-text-in-a-contenteditable-html-element */
	TopologyGraphCreator.prototype.selectElementContents = function(el) {
		var range = document.createRange();
		range.selectNodeContents(el);
		var sel = window.getSelection();
		sel.removeAllRanges();
		sel.addRange(range);
	};


	/* insert svg line breaks: taken from http://stackoverflow.com/questions/13241475/how-do-i-include-newlines-in-labels-in-d3-charts */
	TopologyGraphCreator.prototype.insertTitleLinebreaks = function(gEl, title) {
		var words = title.split(/\s+/g),
			nwords = words.length,
			thisGraph = this;
		var el = gEl.append("text")
			.attr("text-anchor", "middle")
			.attr("style","fill:#black;")
			.attr("dx", function(d){
				return (thisGraph.consts.rectangleWidth / 2) + 10;
			})
			.attr("dy", function(d){
				return (thisGraph.consts.rectangleHeight / 2) + 5;
			});

		for (var i = 0; i < words.length; i++) {
			var tspan = el.append('tspan').text(words[i]);
		}
	};

	TopologyGraphCreator.prototype.insertIcon = function(gEl, icon){
		var thisGraph = this;
		var el = gEl.append("text")
			.attr("text-anchor", "middle")
			.attr("dx", function(d){
				return 30;
			})
			.attr("dy", function(d){
				return (thisGraph.consts.rectangleHeight / 2) + 10;
			})
			.attr('style','font-family: FontAwesome; font-size: 24px; fill: #fff;')
			.html(icon);
	};


	// remove edges associated with a node
	TopologyGraphCreator.prototype.spliceLinksForNode = function(node) {
		var thisGraph = this,
			toSplice = thisGraph.edges.filter(function(l) {
				return (l.source === node || l.target === node);
			});
		toSplice.map(function(l) {
			thisGraph.edges.splice(thisGraph.edges.indexOf(l), 1);
		});
	};

	TopologyGraphCreator.prototype.replaceSelectEdge = function(d3Path, edgeData) {
		var thisGraph = this;
		d3Path.classed(thisGraph.consts.selectedClass, true);
		if (thisGraph.state.selectedEdge) {
			thisGraph.removeSelectFromEdge();
		}
		thisGraph.state.selectedEdge = edgeData;
	};

	TopologyGraphCreator.prototype.replaceSelectNode = function(d3Node, nodeData) {
		var thisGraph = this;
		d3Node.classed(this.consts.selectedClass, true);
		if (thisGraph.state.selectedNode) {
			thisGraph.removeSelectFromNode();
		}
		thisGraph.state.selectedNode = nodeData;
	};

	TopologyGraphCreator.prototype.removeSelectFromNode = function() {
		var thisGraph = this;
		thisGraph.rectangles.filter(function(cd) {
			return cd.id === thisGraph.state.selectedNode.id;
		}).classed(thisGraph.consts.selectedClass, false);
		thisGraph.state.selectedNode = null;
	};

	TopologyGraphCreator.prototype.removeSelectFromEdge = function() {
		var thisGraph = this;
		thisGraph.paths.filter(function(cd) {
			return cd === thisGraph.state.selectedEdge;
		}).classed(thisGraph.consts.selectedClass, false);
		thisGraph.state.selectedEdge = null;
	};

	TopologyGraphCreator.prototype.pathMouseDown = function(d3path, d) {
		var thisGraph = this,
			state = thisGraph.state;
		d3.event.stopPropagation();
		state.mouseDownLink = d;

		if (state.selectedNode) {
			thisGraph.removeSelectFromNode();
		}

		var prevEdge = state.selectedEdge;
		if (!prevEdge || prevEdge !== d) {
			thisGraph.replaceSelectEdge(d3path, d);
		} else {
			thisGraph.removeSelectFromEdge();
		}
	};

	// mousedown on node
	TopologyGraphCreator.prototype.rectangleMouseDown = function(d3node, d) {
		var thisGraph = this,
			state = thisGraph.state;
		d3.event.stopPropagation();
		state.mouseDownNode = d;
	};

	//mousedown on circle
	TopologyGraphCreator.prototype.circleMouseDown = function(d3node, d) {
		var thisGraph = this,
			state = thisGraph.state;
		d3.event.stopPropagation();
		state.mouseDownNode = d;
		state.failedTupleDrag = false;
		if(d3.event.currentTarget.getAttribute('data-failedTuple') === 'true'){
			state.failedTupleDrag = true;
		}
		state.shiftNodeDrag = true;
		// reposition dragged directed edge
		thisGraph.dragLine.classed('hidden', false)
			.attr('d', 'M' + d.x + thisGraph.consts.rectangleWidth / 2 + ',' + d.y + thisGraph.consts.rectangleHeight + 'L' + d.x + thisGraph.consts.rectangleWidth / 2 + ',' + d.y + thisGraph.consts.rectangleHeight);
		return;
	};

	// mouseup on nodes
	TopologyGraphCreator.prototype.rectangleMouseUp = function(d3node, d) {
		var thisGraph = this,
			state = thisGraph.state,
			consts = thisGraph.consts;
		// reset the states
		state.shiftNodeDrag = false;
		d3node.classed(consts.connectClass, false);

		var mouseDownNode = state.mouseDownNode;

		if (!mouseDownNode) return;

		thisGraph.dragLine.classed("hidden", true);

		if (mouseDownNode !== d) {
			// we're in a different node: create new edge for mousedown edge and add to graph
			var newEdge = {
				source: mouseDownNode,
				target: d
			};
			var filtRes = thisGraph.paths.filter(function(d) {
				if (d.source === newEdge.target && d.target === newEdge.source) {
					thisGraph.edges.splice(thisGraph.edges.indexOf(d), 1);
				}
				return d.source === newEdge.source && d.target === newEdge.target;
			});
			if (!filtRes[0].length) {
				if(newEdge.source.currentType === 'Parser'){
					if(thisGraph.state.failedTupleDrag){
						newEdge.target.streamId = "failedTuplesStream";
					} else {
						newEdge.target.streamId = "parsedTuplesStream";
					}
				}
				thisGraph.edges.push(newEdge);
				thisGraph.vent.trigger('topologyLink', thisGraph.edges);
				thisGraph.updateGraph();
			}
		} else {
			// we're in the same node
			if (state.justDragged) {
				// dragged, not clicked
				state.justDragged = false;
			} else {
				// clicked, not dragged
				if (d3.event.shiftKey) {
					if (state.selectedEdge) {
						thisGraph.removeSelectFromEdge();
					}
					var prevNode = state.selectedNode;

					if (!prevNode || prevNode.id !== d.id) {
						thisGraph.replaceSelectNode(d3node, d);
					} else {
						thisGraph.removeSelectFromNode();
					}
				} else {
					thisGraph.vent.trigger('click:topologyNode', 
						{
							parentType: d.parentType, 
							currentType: d.currentType, 
							nodeId: d.nodeId
						}
					);
				}
			}
		}
		state.failedTupleDrag = false;
		state.mouseDownNode = null;
		return;

	}; // end of rectangles mouseup

	// mouseup on circle
	TopologyGraphCreator.prototype.circleMouseUp = function(d3node, d) {
		var thisGraph = this,
			state = thisGraph.state,
			consts = thisGraph.consts;
		// reset the states
		state.shiftNodeDrag = false;
		d3node.classed(consts.connectClass, false);

		var mouseDownNode = state.mouseDownNode;

		if (!mouseDownNode) return;

		thisGraph.dragLine.classed("hidden", true);

		if (mouseDownNode !== d) {
			var newEdge = {
				source: mouseDownNode,
				target: d
			};
			var filtRes = thisGraph.paths.filter(function(d) {
				if (d.source === newEdge.target && d.target === newEdge.source) {
					thisGraph.edges.splice(thisGraph.edges.indexOf(d), 1);
				}
				return d.source === newEdge.source && d.target === newEdge.target;
			});
			if (!filtRes[0].length) {
				if(newEdge.source.currentType === 'Parser'){
					if(thisGraph.state.failedTupleDrag){
						newEdge.target.streamId = "failedTuplesStream";
					} else {
						newEdge.target.streamId = "parsedTuplesStream";
					}
				}
				thisGraph.edges.push(newEdge);
				thisGraph.vent.trigger('topologyLink', thisGraph.edges);
				thisGraph.updateGraph();
			}
		} else {
			// we're in the same node
			if (state.selectedEdge) {
				thisGraph.removeSelectFromEdge();
			}
			var prevNode = state.selectedNode;

			if (!prevNode || prevNode.id !== d.id) {
				thisGraph.replaceSelectNode(d3node, d);
			} else {
				thisGraph.removeSelectFromNode();
			}
		}
		state.failedTupleDrag = false;
		state.mouseDownNode = null;
		return;

	};

	// mousedown on main svg
	TopologyGraphCreator.prototype.svgMouseDown = function() {
		this.state.graphMouseDown = true;
	};

	// mouseup on main svg
	TopologyGraphCreator.prototype.svgMouseUp = function() {
		var thisGraph = this,
			state = thisGraph.state;
		if (state.justScaleTransGraph) {
			// dragged not clicked
			state.justScaleTransGraph = false;
		} else if (state.shiftNodeDrag) {
			// dragged from node
			state.shiftNodeDrag = false;
			thisGraph.dragLine.classed("hidden", true);
		}
		state.graphMouseDown = false;
	};

	TopologyGraphCreator.prototype.createNode = function(){
		var thisGraph = this,
			state = thisGraph.state;
		state.graphMouseDown = true;
		var xycoords = d3.mouse(thisGraph.svgG.node()),
			d = {
				id: thisGraph.idct++,
				title: thisGraph.nodeTitle,
				x: xycoords[0] - thisGraph.consts.rectangleWidth / 2,
				y: xycoords[1] - thisGraph.consts.rectangleHeight / 2,
				parentType: thisGraph.nodeParentType,
				currentType: thisGraph.currentStep,
				icon: thisGraph.icon,
				nodeId: thisGraph.nodeId
			};
		thisGraph.nodes.push(d);
		if(d.currentType === 'Device'){
			var newObject = jQuery.extend(true, {}, d);
			newObject.id = thisGraph.idct++;
			newObject.nodeId = thisGraph.otherId;
			newObject.title = 'Parser';
			newObject.x += 300;
			newObject.parentType = 'Processor';
			newObject.currentType = 'Parser';
			thisGraph.nodes.push(newObject);
			thisGraph.edges.push({source: d, target: newObject});
			thisGraph.vent.trigger('topologyLink', thisGraph.edges);
		}
		thisGraph.updateGraph();
		state.graphMouseDown = false;
	};

	// keydown on main svg
	TopologyGraphCreator.prototype.svgKeyDown = function() {
		var thisGraph = this,
			state = thisGraph.state,
			consts = thisGraph.consts;
		// make sure repeated key presses don't register for each keydown
		if (state.lastKeyDown !== -1) return;

		state.lastKeyDown = d3.event.keyCode;
		var selectedNode = state.selectedNode,
			selectedEdge = state.selectedEdge;

		switch (d3.event.keyCode) {
			// case consts.BACKSPACE_KEY:
			case consts.DELETE_KEY:
				d3.event.preventDefault();
				if (selectedNode) {
					thisGraph.nodes.splice(thisGraph.nodes.indexOf(selectedNode), 1);
					thisGraph.spliceLinksForNode(selectedNode);
					thisGraph.vent.trigger('delete:topologyNode', 
						{
							parentType: selectedNode.parentType, 
							currentType: selectedNode.currentType, 
							nodeId: selectedNode.nodeId,
							linkArr: thisGraph.edges
						}
					);
					state.selectedNode = null;
					thisGraph.updateGraph();
				} else if (selectedEdge) {
					thisGraph.edges.splice(thisGraph.edges.indexOf(selectedEdge), 1);
					thisGraph.vent.trigger('topologyLink', thisGraph.edges);
					state.selectedEdge = null;
					thisGraph.updateGraph();
				}
				break;
		}
	};

	TopologyGraphCreator.prototype.svgKeyUp = function() {
		this.state.lastKeyDown = -1;
	};

	// call to propagate changes to graph
	TopologyGraphCreator.prototype.updateGraph = function() {

		var thisGraph = this,
			consts = thisGraph.consts,
			state = thisGraph.state;

		thisGraph.paths = thisGraph.paths.data(thisGraph.edges, function(d) {
			return String(d.source.id) + "+" + String(d.target.id);
		});
		var paths = thisGraph.paths;
		// update existing paths
		paths.style('marker-end', 'url(#end-arrow)')
			.classed(consts.selectedClass, function(d) {
				return d === state.selectedEdge;
			})
			.attr("d", function(d) {
				if(d.target.streamId === "failedTuplesStream"){
					return "M" + (d.source.x + consts.rectangleWidth / 2) + "," + (d.source.y + consts.rectangleHeight)  + "L" + d.target.x + "," + (d.target.y + consts.rectangleHeight / 2);
				} else {
					return "M" + (d.source.x + consts.rectangleWidth) + "," + (d.source.y + consts.rectangleHeight / 2)  + "L" + d.target.x + "," + (d.target.y + consts.rectangleHeight / 2);
				}
			});

		// add new paths
		paths.enter()
			.append("path")
			.style('marker-end', 'url(#end-arrow)')
			.classed("link", true)
			.attr("d", function(d) {
				if(d.target.streamId === "failedTuplesStream"){
					return "M" + (d.source.x + consts.rectangleWidth / 2) + "," + (d.source.y + consts.rectangleHeight)  + "L" + d.target.x + "," + (d.target.y + consts.rectangleHeight / 2);
				} else {
					return "M" + (d.source.x + consts.rectangleWidth) + "," + (d.source.y + consts.rectangleHeight / 2)  + "L" + d.target.x + "," + (d.target.y + consts.rectangleHeight / 2);
				}
			})
			.on("mousedown", function(d) {
				thisGraph.pathMouseDown.call(thisGraph, d3.select(this), d);
			})
			.on("mouseup", function(d) {
				state.mouseDownLink = null;
			});

		// remove old links
		paths.exit().remove();

		// update existing nodes
		thisGraph.rectangles = thisGraph.rectangles.data(thisGraph.nodes, function(d) {
			return d.id;
		});
		thisGraph.rectangles.attr("transform", function(d) {
			return "translate(" + d.x + "," + d.y + ")";
		});
		
		//add new rectangles
		var newGs = thisGraph.rectangles.enter()
				.append("g");
			newGs.classed(consts.rectangleGClass, true)
				.attr("transform", function(d) {
				return "translate(" + d.x + "," + d.y + ")";
			});
			newGs.append("rect")
			    .attr("rx", 10)
			    .attr("ry", 10)
			    .attr("width", consts.rectangleWidth)
			    .attr("height", consts.rectangleHeight)
			    .on("mouseover", function(d) {
					if (state.shiftNodeDrag) {
						d3.select(this).classed(consts.connectClass, true);
					}
				})
				.on("mouseout", function(d) {
					d3.select(this).classed(consts.connectClass, false);
				})
				.on("mousedown", function(d) {
					thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
				})
				.on("mouseup", function(d) {
					thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
				})
				.call(thisGraph.drag);

			newGs.append("circle")
				.attr("cx", function (d) { 
					if(d.parentType !== 'Data Sink')
			    		return (consts.rectangleWidth); 
			    	else
			    		return '';
				})
		        .attr("cy", function (d) { 
		        	if(d.parentType !== 'Data Sink')
		        		return consts.rectangleHeight / 2;
		        	else
			    		return ''; 
		        })
		        .attr("r", function (d) { 
		        	if(d.parentType !== 'Data Sink')
			    		return '5';
			    	else
			    		return '0';
			    })
		        .style("fill", "black")
		        .on("mouseover", function(d) {
					if (state.shiftNodeDrag) {
						d3.select(this).classed(consts.connectClass, true);
					}
				})
				.on("mouseout", function(d) {
					d3.select(this).classed(consts.connectClass, false);
				})
				.on("mousedown", function(d) {
					thisGraph.circleMouseDown.call(thisGraph, d3.select(this), d);
				})
				.on("mouseup", function(d) {
					thisGraph.circleMouseUp.call(thisGraph, d3.select(this), d);
				})
				.call(thisGraph.drag);

			newGs.append("circle")
				.attr("cx", function (d) { 
					if(d.currentType === 'Parser')
			    		return (consts.rectangleWidth / 2); 
			    	else
			    		return '';
				})
		        .attr("cy", function (d) { 
		        	if(d.currentType === 'Parser')
		        		return consts.rectangleHeight;
		        	else
			    		return ''; 
		        })
		        .attr("r", function (d) { 
		        	if(d.currentType === 'Parser')
			    		return '5';
			    	else
			    		return '0';
			    })
			    .attr("data-failedTuple", true)
		        .style("fill", "red")
		        .style("stroke", "red")
		        .on("mouseover", function(d) {
					if (state.shiftNodeDrag) {
						d3.select(this).classed(consts.connectClass, true);
					}
				})
				.on("mouseout", function(d) {
					d3.select(this).classed(consts.connectClass, false);
				})
				.on("mousedown", function(d) {
					thisGraph.circleMouseDown.call(thisGraph, d3.select(this), d);
				})
				.on("mouseup", function(d) {
					thisGraph.circleMouseUp.call(thisGraph, d3.select(this), d);
				})
				.call(thisGraph.drag);

		    newGs.append("circle")
		        .attr("cy", function (d) { 
		        	if(d.parentType !== 'Datasource')
			    		return consts.rectangleHeight / 2;
		        })
		        .attr("r", function (d) { 
		        	if(d.parentType !== 'Datasource')
			    		return '5';
			    })
		        .style("fill", "black")
		        .on("mouseup", function(d) {
					thisGraph.circleMouseUp.call(thisGraph, d3.select(this), d);
				});
		        
		        

		newGs.each(function(d) {
			thisGraph.insertIcon(d3.select(this), d.icon);
			thisGraph.insertTitleLinebreaks(d3.select(this), d.title);
		});

		// remove old nodes
		thisGraph.rectangles.exit().remove();
	};

	TopologyGraphCreator.prototype.zoomed = function() {
		this.state.justScaleTransGraph = true;
		var a = d3.event.sourceEvent.x+","+d3.event.sourceEvent.y;
		d3.select("." + this.consts.graphClass)
			.attr("transform", "translate(" + a + ") scale(" + d3.event.scale + ")");
	};

	TopologyGraphCreator.prototype.updateWindow = function(svg) {
		var thisGraph = this;
		var x = thisGraph.elem.width();
		var y = thisGraph.elem.height();
		svg.attr("width", x).attr("height", y);
	};
	return TopologyGraphCreator;
});