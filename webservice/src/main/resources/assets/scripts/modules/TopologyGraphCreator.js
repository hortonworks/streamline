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
			selectedText: null
		};

		// define arrow markers for graph links
		var defs = svg.append('svg:defs');
		defs.append('svg:marker')
			.attr('id', 'end-arrow')
			.attr('viewBox', '0 -5 10 10')
			.attr('refX', "36")
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
			.classed(thisGraph.consts.graphClass, true);
		var svgG = thisGraph.svgG;

		// displayed when dragging between nodes
		thisGraph.dragLine = svgG.append('svg:path')
			.attr('class', 'link dragline hidden')
			.attr('d', 'M0,0L0,0')
			.style('marker-end', 'url(#mark-end-arrow)');

		// svg nodes and edges 
		thisGraph.paths = svgG.append("g").selectAll("g");
		thisGraph.circles = svgG.append("g").selectAll("g");

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
		circleGClass: "conceptG",
		graphClass: "graph",
		activeEditId: "active-editing",
		BACKSPACE_KEY: 8,
		DELETE_KEY: 46,
		ENTER_KEY: 13,
		nodeRadius: 40
	};

	/* PROTOTYPE FUNCTIONS */

	TopologyGraphCreator.prototype.dragmove = function(d) {
		var thisGraph = this;
		if (thisGraph.state.shiftNodeDrag) {
			thisGraph.dragLine.attr('d', 'M' + d.x + ',' + d.y + 'L' + d3.mouse(thisGraph.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
		} else {
			d.x += d3.event.dx;
			d.y += d3.event.dy;
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
			nwords = words.length;
		var el = gEl.append("text")
			.attr("text-anchor", "middle")
			.attr("style","fill:#fff;")
			.attr("dy", "20");
			// .attr("dy", "-" + (nwords - 1) * 7.5);

		for (var i = 0; i < words.length; i++) {
			var tspan = el.append('tspan').text(words[i]);
			if (i > 0)
				tspan.attr('x', 0).attr('dy', '20');
				// tspan.attr('x', 0).attr('dy', '15');
		}
	};

	TopologyGraphCreator.prototype.insertIcon = function(gEl, icon){
		var el = gEl.append("text")
			.attr("text-anchor", "middle")
			.attr("dy", "0")
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
		thisGraph.circles.filter(function(cd) {
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
	TopologyGraphCreator.prototype.circleMouseDown = function(d3node, d) {
		var thisGraph = this,
			state = thisGraph.state;
		d3.event.stopPropagation();
		state.mouseDownNode = d;
		if (d3.event.shiftKey) {
			state.shiftNodeDrag = d3.event.shiftKey;
			// reposition dragged directed edge
			thisGraph.dragLine.classed('hidden', false)
				.attr('d', 'M' + d.x + ',' + d.y + 'L' + d.x + ',' + d.y);
			return;
		}
	};

	/* place editable text on node in place of svg text */
	TopologyGraphCreator.prototype.changeTextOfNode = function(d3node, d) {
		var thisGraph = this,
			consts = thisGraph.consts,
			htmlEl = d3node.node();
		d3node.selectAll("text").remove();
		var nodeBCR = htmlEl.getBoundingClientRect(),
			curScale = nodeBCR.width / consts.nodeRadius,
			placePad = 5 * curScale,
			useHW = curScale > 1 ? nodeBCR.width * 0.71 : consts.nodeRadius * 1.42;
		// replace with editableconent text
		var d3txt = thisGraph.svg.selectAll("foreignObject")
			.data([d])
			.enter()
			.append("foreignObject")
			.attr("x", nodeBCR.left + placePad)
			.attr("y", nodeBCR.top + placePad)
			.attr("height", 2 * useHW)
			.attr("width", useHW)
			.append("xhtml:p")
			.attr("id", consts.activeEditId)
			.attr("contentEditable", "true")
			.text(d.title)
			.on("mousedown", function(d) {
				d3.event.stopPropagation();
			})
			.on("keydown", function(d) {
				d3.event.stopPropagation();
				if (d3.event.keyCode == consts.ENTER_KEY && !d3.event.shiftKey) {
					this.blur();
				}
			})
			.on("blur", function(d) {
				d.title = this.textContent;
				thisGraph.insertTitleLinebreaks(d3node, d.title);
				d3.select(this.parentElement).remove();
			});
		return d3txt;
	};

	// mouseup on nodes
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
				thisGraph.edges.push(newEdge);
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
					// shift-clicked node: edit text content
					// var d3txt = thisGraph.changeTextOfNode(d3node, d);
					// var txtNode = d3txt.node();
					// thisGraph.selectElementContents(txtNode);
					// txtNode.focus();
				} else {
					thisGraph.vent.trigger('click:topologyNode', {parentType: d.parentType, currentType: d.currentType});
				}
			}
		}
		state.mouseDownNode = null;
		return;

	}; // end of circles mouseup

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
		} else if (state.graphMouseDown && d3.event.shiftKey && thisGraph.nodeTitle) {
			// clicked not dragged from svg
			// this.createNode();
			// make title of text immediently editable
			// var d3txt = thisGraph.changeTextOfNode(thisGraph.circles.filter(function(dval) {
			// 		return dval.id === d.id;
			// 	}), d),
			// 	txtNode = d3txt.node();
			// thisGraph.selectElementContents(txtNode);
			// txtNode.focus();
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
				x: xycoords[0],
				y: xycoords[1],
				parentType: thisGraph.nodeParentType,
				currentType: thisGraph.currentStep,
				icon: thisGraph.icon
			};
		thisGraph.nodes.push(d);
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
			case consts.BACKSPACE_KEY:
			case consts.DELETE_KEY:
				d3.event.preventDefault();
				if (selectedNode) {
					thisGraph.nodes.splice(thisGraph.nodes.indexOf(selectedNode), 1);
					thisGraph.spliceLinksForNode(selectedNode);
					state.selectedNode = null;
					thisGraph.updateGraph();
				} else if (selectedEdge) {
					thisGraph.edges.splice(thisGraph.edges.indexOf(selectedEdge), 1);
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
				return "M" + d.source.x + "," + d.source.y + "L" + d.target.x + "," + d.target.y;
			});

		// add new paths
		paths.enter()
			.append("path")
			.style('marker-end', 'url(#end-arrow)')
			.classed("link", true)
			.attr("d", function(d) {
				return "M" + d.source.x + "," + d.source.y + "L" + d.target.x + "," + d.target.y;
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
		thisGraph.circles = thisGraph.circles.data(thisGraph.nodes, function(d) {
			return d.id;
		});
		thisGraph.circles.attr("transform", function(d) {
			return "translate(" + d.x + "," + d.y + ")";
		});

		// add new nodes
		var newGs = thisGraph.circles.enter()
			.append("g");

		newGs.classed(consts.circleGClass, true)
			.attr("transform", function(d) {
				return "translate(" + d.x + "," + d.y + ")";
			})
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
			.attr("r", String(consts.nodeRadius));

		newGs.each(function(d) {
			thisGraph.insertIcon(d3.select(this), d.icon);
			thisGraph.insertTitleLinebreaks(d3.select(this), d.title);
		});

		// remove old nodes
		thisGraph.circles.exit().remove();
	};

	TopologyGraphCreator.prototype.zoomed = function() {
		this.state.justScaleTransGraph = true;
		d3.select("." + this.consts.graphClass)
			.attr("transform", "translate(" + d3.event.translate + ") scale(" + d3.event.scale + ")");
	};

	TopologyGraphCreator.prototype.updateWindow = function(svg) {
		var thisGraph = this;
		var x = thisGraph.elem.width();
		var y = thisGraph.elem.height();
		svg.attr("width", x).attr("height", y);
	};
	return TopologyGraphCreator;
});