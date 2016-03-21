define(['require', 
	'utils/Globals',
	'utils/Utils',
	'bootbox', 
	'd3',
	'd3-tip'],
	function(require, Globals, Utils, bootbox, d3) {
	'use strict';

	var TopologyGraphCreator = function(options) {
		//pass elem, data{node,edges}, vent
		var thisGraph = this;
		_.extend(thisGraph, options);
		thisGraph.bindEvents();
		this.width = thisGraph.elem.width() ? thisGraph.elem.width() : 1220;
		this.height = thisGraph.elem.height() ? thisGraph.elem.height() : 450;
		var svg = d3.select('#'+thisGraph.elem.attr('id')).append('svg')
					.attr('width', this.width)
					.attr('height', this.height);

		var nodes = thisGraph.data.nodes,
			edges = thisGraph.data.edges;

		thisGraph.nodes = nodes || [];
		thisGraph.edges = edges || [];

		thisGraph.graphTransforms = thisGraph.data.graphTransforms;
		thisGraph.lineFunction = d3.svg.line()
								.x(function(d){ return d.x; })
								.y(function(d){ return d.y; })
								.interpolate("step");

		thisGraph.pathdef = function(p1, p2, flag){
			var segments = [],
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
			return segments.toString().split(',').join(' ');
		};

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
			.attr('markerWidth', 6.5)
			.attr('markerHeight', 7.5)
			.attr('orient', 'auto')
			.append('svg:path')
			.attr('d', 'M0 -5 L10 0 L0 5');

		// define arrow markers for leading arrow
		defs.append('svg:marker')
			.attr('id', 'mark-end-arrow')
			.attr('viewBox', '0 -5 10 10')
			.attr('refX', 7)
			.attr('markerWidth', 6.5)
			.attr('markerHeight', 7.5)
			.attr('orient', 'auto')
			.append('svg:path')
			.attr('d', 'M0 -5 L10 0 L0 5');

		defs.append('svg:filter')
			.attr('id', 'grayscale')
			.append('feColorMatrix')
				.attr('type', 'saturate')
				.attr('values', '0');

		thisGraph.svg = svg;
		thisGraph.svgG = svg.append("g")
			// .attr('transform','translate('+ (thisGraph.elem.width() ? thisGraph.elem.width() : 1220) + ',' + (thisGraph.elem.height() ? thisGraph.elem.height() : 600) + ')')
			.classed(thisGraph.consts.graphClass, true);
		var svgG = thisGraph.svgG;

		// displayed when dragging between nodes
		thisGraph.dragLine = svgG.append('svg:path')
			.attr('class', 'link dragline hidden')
			.attr('d', 'M0 0 L0 0')
			.attr("stroke-dasharray", "5, 5")
			.style('marker-end', 'url(#mark-end-arrow)');

		// svg nodes and edges 
		thisGraph.paths = svgG.append("g").attr('class','link-group').selectAll("g");
		thisGraph.rectangles = svgG.append("g").selectAll("g");

		thisGraph.drag = d3.behavior.drag()
			.origin(function(d) {
				return {
					x: d.x,
					y: d.y
				};
			})
			.on("drag", function(args) {
				if(thisGraph.editMode) {
					thisGraph.state.justDragged = true;
					thisGraph.dragmove.call(thisGraph, args);
				}
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
			// thisGraph.hideShuffle();
		});

		// listen for dragging
		thisGraph.dragSvg = d3.behavior.zoom().scaleExtent([0, 8]).on("zoom", function(){
			thisGraph.state.justScaleTransGraph = true;
			d3.select("." + thisGraph.consts.graphClass)
				.attr("transform", "translate(" + thisGraph.dragSvg.translate() + ")" + "scale(" + thisGraph.dragSvg.scale() + ")");
		}).on("zoomend", function() {
			var gTranslate = thisGraph.dragSvg.translate(),
				gScaled = thisGraph.dragSvg.scale();

			thisGraph.graphTransforms = {
				dragCoords: gTranslate,
				zoomScale: gScaled
			};
			thisGraph.vent.trigger('topologyTransforms', thisGraph.graphTransforms);
		});

		thisGraph.dragSvg.translate(thisGraph.graphTransforms.dragCoords);
		thisGraph.dragSvg.scale(thisGraph.graphTransforms.zoomScale);
		thisGraph.dragSvg.event(svg);

		svg.call(thisGraph.dragSvg).on("dblclick.zoom", null);

		// listen for resize
		window.onresize = function() {
			thisGraph.updateWindow(svg);
		};
	};

	TopologyGraphCreator.prototype.bindEvents = function(){
		var thisGraph = this;
		this.vent.listenTo(this.vent, 'topologyEditor:DropAction', function(obj){
			thisGraph.nodeObject = obj.nodeObj;
			thisGraph.uiname = obj.uiname;
			thisGraph.customName = obj.customName;
			d3.event = obj.event;
			thisGraph.createNode();
		});
		this.vent.listenTo(this.vent, 'TopologyEditorMaster:Zoom', function(zoomType){
			var direction = 1,
		        factor = 0.2,
		        target_zoom = 1,
		        center = [thisGraph.width / 2, thisGraph.height / 2],
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

		    thisGraph.graphTransforms = {
				dragCoords: zoom.translate(),
				zoomScale: zoom.scale()
			};
			thisGraph.vent.trigger('topologyTransforms', thisGraph.graphTransforms);
		});
		this.vent.listenTo(this.vent, 'saveNodeConfig', function(obj) {
			var currentNode = _.findWhere(thisGraph.nodes, {uiname: obj.uiname});
			if(currentNode){
				currentNode.isConfigured = true;
			}
			thisGraph.updateGraph();
		});
	};

	TopologyGraphCreator.prototype.interpolateZoom = function(translate, scale){
		var thisGraph = this,
			zoom = thisGraph.dragSvg;
	    return d3.transition().duration(350).tween("zoom", function () {
	        var iTranslate = d3.interpolate(zoom.translate(), translate),
	            iScale = d3.interpolate(zoom.scale(), scale);
	        return function (t) {
	            zoom
	                .scale(iScale(t))
	                .translate(iTranslate(t));
	            d3.select("." + thisGraph.consts.graphClass)
					.attr("transform", "translate(" + thisGraph.dragSvg.translate() + ")" + "scale(" + thisGraph.dragSvg.scale() + ")");
	        };
	    });
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
		rectangleWidth: 48,
		rectangleHeight: 58
	};

	/* PROTOTYPE FUNCTIONS */

	TopologyGraphCreator.prototype.dragmove = function(d) {
		var thisGraph = this;
		if (thisGraph.state.shiftNodeDrag) {
			if(thisGraph.state.failedTupleDrag){
				thisGraph.dragLine.attr('d', 'M' + (d.x + thisGraph.consts.rectangleWidth / 2)+ ',' + (d.y + thisGraph.consts.rectangleHeight + 10) + 'L' + d3.mouse(thisGraph.svgG.node())[0] + ',' + d3.mouse(this.svgG.node())[1]);
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
			thisGraph = this,
			nodeTitle = '';
		var el = gEl.append("text")
			.attr("text-anchor", "middle")
			.attr("class", "node-title")
			.attr("dx", function(d){
				return (thisGraph.consts.rectangleWidth / 2);
			})
			.attr("dy", function(d){
				return (thisGraph.consts.rectangleHeight) + 4;
			});

		for (var i = 0; i < words.length; i++) {
			nodeTitle += words[i]+' ';
		}
		el.text(nodeTitle.trim());
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
		var path = thisGraph.paths.filter(function(cd) {
			return cd === thisGraph.state.selectedEdge;
		});
		var selectedPath = $(path[0][0]);
		d3.select(selectedPath.siblings(("[d='"+$(selectedPath).attr('d')+"']"))[0]).classed(thisGraph.consts.selectedClass, false);
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
			if(Utils.isValidTopologyConnection(mouseDownNode, d, thisGraph.state.failedTupleDrag)){
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
					var data = {edges: thisGraph.edges};
					if(newEdge.source.currentType === Globals.Topology.Editor.Steps.Processor.Substeps[0].valStr){
						if(thisGraph.state.failedTupleDrag){
							newEdge.target.streamId = "failedTuplesStream";
						} else {
							newEdge.target.streamId = "parsedTuplesStream";
						}
					} else if(newEdge.source.currentType === 'RULE' || newEdge.source.currentType === 'CUSTOM'){
						thisGraph.vent.trigger('topologyGraph:RuleToOtherNode', newEdge);
					}
					thisGraph.edges.push(newEdge);
					thisGraph.vent.trigger('topologyLink', data);
					thisGraph.updateGraph();
				}
			} else {
				Utils.notifyError(mouseDownNode.currentType + ' cannot be connected to '+ d.currentType);
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

					if (!prevNode || prevNode.uiname !== d.uiname) {
						thisGraph.replaceSelectNode(d3node, d);
					} else {
						thisGraph.removeSelectFromNode();
					}
				} else {
					thisGraph.vent.trigger('click:topologyNode', 
						{
							parentType: d.parentType,
							currentType: d.currentType,
							customName: d.customName,
							uiname: d.uiname
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
			if(Utils.isValidTopologyConnection(mouseDownNode, d, thisGraph.state.failedTupleDrag)){
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
					var data = {edges: thisGraph.edges};
					if(newEdge.source.currentType === Globals.Topology.Editor.Steps.Processor.Substeps[0].valStr){
						if(thisGraph.state.failedTupleDrag){
							newEdge.target.streamId = "failedTuplesStream";
						} else {
							newEdge.target.streamId = "parsedTuplesStream";
						}
					} else if(newEdge.source.currentType === 'RULE' || newEdge.source.currentType === 'CUSTOM'){
						thisGraph.vent.trigger('topologyGraph:RuleToOtherNode', newEdge);
					}
					thisGraph.edges.push(newEdge);
					thisGraph.vent.trigger('topologyLink', data);
					thisGraph.updateGraph();
				}
			} else {
				Utils.notifyError(mouseDownNode.currentType + ' cannot be connected to '+ d.currentType);
			}
		} else {
			// we're in the same node
			if (state.selectedEdge) {
				thisGraph.removeSelectFromEdge();
			}
			var prevNode = state.selectedNode;

			if (!prevNode || prevNode.uiname !== d.uiname) {
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
				x: xycoords[0] - thisGraph.consts.rectangleWidth / 2,
				y: xycoords[1] - thisGraph.consts.rectangleHeight / 2,
				parentType: thisGraph.nodeObject.parentType,
				currentType: thisGraph.nodeObject.valStr,
				uiname: thisGraph.uiname,
				customName: thisGraph.customName,
				imageURL: thisGraph.nodeObject.imgUrl,
				isConfigured: false
			};
		thisGraph.nodes.push(d);
		if(d.currentType === Globals.Topology.Editor.Steps.Datasource.Substeps[0].valStr){
			thisGraph.createParserNode(d);
		}
		thisGraph.updateGraph();
		state.graphMouseDown = false;
	};

	TopologyGraphCreator.prototype.createParserNode = function(d) {
		var thisGraph = this,
			newObject = jQuery.extend(true, {}, d);
		newObject.x += 200;
		newObject.uiname = thisGraph.nodeObject.parserUiname;
		newObject.parentType = Globals.Topology.Editor.Steps.Processor.Substeps[0].parentType;
		newObject.currentType = Globals.Topology.Editor.Steps.Processor.Substeps[0].valStr;
		newObject.imageURL = Globals.Topology.Editor.Steps.Processor.Substeps[0].imgUrl;
		thisGraph.nodes.push(newObject);
		thisGraph.edges.push({source: d, target: newObject});
		thisGraph.vent.trigger('topologyLink', {edges: thisGraph.edges});
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
					thisGraph.deleteNode(selectedNode);
				} else if (selectedEdge) {
					thisGraph.deleteEdge(selectedEdge);
				}
				break;
		}
	};

	TopologyGraphCreator.prototype.deleteNode = function(selectedNode){
		var callback, triggerData, thisGraph = this, state = thisGraph.state;
		switch(selectedNode.parentType){
			case 'Datasource':
				if(_.isEqual(selectedNode.currentType, 'DEVICE')){
					var deviceToParserObj = _.find(thisGraph.edges, function(obj){
						return obj.source == selectedNode;
					});
					if(!_.isUndefined(deviceToParserObj)){
						callback = function(){
							thisGraph.nodes.splice(thisGraph.nodes.indexOf(deviceToParserObj.source), 1);
							thisGraph.spliceLinksForNode(deviceToParserObj.source);
							thisGraph.nodes.splice(thisGraph.nodes.indexOf(deviceToParserObj.target), 1);
							thisGraph.spliceLinksForNode(deviceToParserObj.target);
							state.selectedNode = null;
							thisGraph.updateGraph();
						};
						triggerData = {
							data: [deviceToParserObj.source, deviceToParserObj.target],
							callback: callback
						};
						var parserToRuleObj = _.find(thisGraph.edges, function(obj){
							return (obj.source == deviceToParserObj.target && obj.target.currentType === 'RULE');
						});
						if(! _.isUndefined(parserToRuleObj)){
							parserToRuleObj.target.isConfigured = false;
							triggerData.resetRule = parserToRuleObj.target;
						}
						thisGraph.vent.trigger('delete:topologyNode', triggerData);
					}
				}
			break;
			case 'Processor':
				if(_.isEqual(selectedNode.currentType, 'PARSER')){
					Utils.notifyInfo('Parser can only be deleted if Source is deleted.');
				} else if(_.isEqual(selectedNode.currentType, 'RULE') || _.isEqual(selectedNode.currentType, 'CUSTOM')){
					callback = function(){
						thisGraph.nodes.splice(thisGraph.nodes.indexOf(selectedNode), 1);
						thisGraph.spliceLinksForNode(selectedNode);
						state.selectedNode = null;
						thisGraph.updateGraph();
					};
					triggerData = {
						data: [selectedNode],
						callback: callback
					};
					var ruleToRuleObj = _.find(thisGraph.edges, function(obj){
						return (obj.target == selectedNode && obj.source.currentType === 'RULE');
					});
					if(ruleToRuleObj){
						triggerData.resetRuleAction = ruleToRuleObj.source;
					}
					var customToRuleObj = _.find(thisGraph.edges, function(obj){
						return (obj.target == selectedNode && obj.source.currentType === 'CUSTOM');
					});
					if(customToRuleObj){
						triggerData.resetCustomAction = customToRuleObj.source;
					}
					thisGraph.vent.trigger('delete:topologyNode', triggerData);
				}
			break;
			case 'DataSink':
				callback = function(){
					thisGraph.nodes.splice(thisGraph.nodes.indexOf(selectedNode), 1);
					thisGraph.spliceLinksForNode(selectedNode);
					state.selectedNode = null;
					thisGraph.updateGraph();
				};
				triggerData = {
					data: [selectedNode],
					callback: callback
				};
				var ruleToSinkObj = _.find(thisGraph.edges, function(obj){
					return (obj.target == selectedNode && obj.source.currentType === 'RULE');
               	});
               	if(ruleToSinkObj){
					triggerData.resetRuleAction = ruleToSinkObj.source;
				}
				var customToSinkObj = _.find(thisGraph.edges, function(obj){
					return (obj.target == selectedNode && obj.source.currentType === 'CUSTOM');
				});
				if(customToSinkObj){
					triggerData.resetCustomAction = customToSinkObj.source;
				}
				thisGraph.vent.trigger('delete:topologyNode', triggerData);
			break;
		}
		thisGraph.vent.trigger('topologyLink', {edges: thisGraph.edges});
	};

	TopologyGraphCreator.prototype.deleteEdge = function(selectedEdge){
		var thisGraph = this, state = thisGraph.state;
		var triggerData = {
			callback: function(){
				thisGraph.edges.splice(thisGraph.edges.indexOf(selectedEdge), 1);
				thisGraph.vent.trigger('topologyLink', {edges: thisGraph.edges});
				state.selectedEdge = null;
				thisGraph.updateGraph();
			}
		};
		if(selectedEdge.source.currentType === 'DEVICE' && selectedEdge.target.currentType === 'PARSER'){
			Utils.notifyInfo('Link between Device and Parser cannot be deleted.');
		} else {
			if(selectedEdge.source.currentType === 'PARSER' && selectedEdge.target.currentType === 'RULE'){
				selectedEdge.target.isConfigured = false;
				triggerData.resetRule = selectedEdge.target;
			} else if(selectedEdge.source.currentType === 'RULE' && selectedEdge.target.parentType === 'DataSink'){
				triggerData.resetRuleAction = selectedEdge.source;
				triggerData.data = [selectedEdge.target];
			} else if(selectedEdge.source.currentType === 'CUSTOM' && selectedEdge.target.currentType === 'RULE'){
				selectedEdge.target.isConfigured = false;
				triggerData.resetRule = selectedEdge.target;
			} else if(selectedEdge.source.currentType === 'CUSTOM' && selectedEdge.target.parentType === 'DataSink'){
				triggerData.resetCustomAction = selectedEdge.source;
				triggerData.data = [selectedEdge.target];
			}
			thisGraph.vent.trigger('delete:topologyEdge', triggerData);
		}
	};

	TopologyGraphCreator.prototype.svgKeyUp = function() {
		this.state.lastKeyDown = -1;
	};

	// call to propagate changes to graph
	TopologyGraphCreator.prototype.updateGraph = function() {
		$('.visible-link').remove();
		var thisGraph = this,
			consts = thisGraph.consts,
			state = thisGraph.state;

		thisGraph.paths = thisGraph.paths.data(thisGraph.edges, function(d) {
			return String(d.source.uiname) + "+" + String(d.target.uiname);
		});
		var paths = thisGraph.paths;
		// update existing paths
		paths.classed(consts.selectedClass, function(d) {
				return d === state.selectedEdge;
			})
			.attr("d", function(d) {
				var arr = [];
				var flag = false;
				if(d.target.streamId === "failedTuplesStream"){
					arr.push({x: (d.source.x + consts.rectangleWidth / 2),y: (d.source.y + consts.rectangleHeight + 10)},
							 {x: d.target.x, y: (d.target.y + consts.rectangleHeight / 2)});
					flag = true;
				} else {
					arr.push({x: (d.source.x + consts.rectangleWidth),y: (d.source.y + consts.rectangleHeight / 2)},
							 {x: d.target.x, y: (d.target.y + consts.rectangleHeight / 2)});
				}
				return thisGraph.pathdef(arr[0], arr[1], flag);
				// return thisGraph.lineFunction(arr);
			});

		// add new paths
		paths.enter()
			.append("path")
			.classed("link", true)
			.attr("d", function(d){
				var arr = [];
				var flag = false;
				if(d.target.streamId === "failedTuplesStream"){
					arr.push({x: (d.source.x + consts.rectangleWidth / 2),y: (d.source.y + consts.rectangleHeight + 10)},
							 {x: d.target.x, y: (d.target.y + consts.rectangleHeight / 2)});
					flag = true;
				} else {
					arr.push({x: (d.source.x + consts.rectangleWidth),y: (d.source.y + consts.rectangleHeight / 2)},
							 {x: d.target.x, y: (d.target.y + consts.rectangleHeight / 2)});
				}
				return thisGraph.pathdef(arr[0], arr[1], flag);
			})
			.attr("stroke-opacity", "0.0001")
			.attr("stroke-width", "15")
			.attr("data-toggle", "popover")
			.attr("data-name", function(d){ return d.source.uiname +'-'+d.target.uiname; })
			.on("mouseover", function(d){
				if(thisGraph.editMode) {
					// $('[data-uiname="'+d.source.uiname+'-'+d.target.uiname+'"]').show();
				}
			})
			.on("mouseout", function(d){
				if(thisGraph.editMode) {
					// $('[data-uiname="'+d.source.uiname+'-'+d.target.uiname+'"]').hide();
				}
			})
			.on("mousedown", function(d) {
				if(thisGraph.editMode) {
					if(d3.event.shiftKey){
						var elem = $(this).parent().find('.visible-link[d="'+$(this).attr("d")+'"]')[0];
						thisGraph.pathMouseDown.call(thisGraph, d3.select(elem), d);
					} else {
						// thisGraph.showShuffle(d);
					}
				}
			})
			.on("mouseup", function(d) {
				if(thisGraph.editMode){
					if(d3.event.shiftKey){
						state.mouseDownLink = null;
					} else {
						// $('[data-uiname="'+d.source.uiname+'-'+d.target.uiname+'"][data-toggle="popover"]').popover('show');
						// thisGraph.bindPopoverDropdown(d);
					}
				}
			});

		paths.append('text')
            .attr("class", "fa fa-random")
            .attr("x", function(d){
				return thisGraph.getBoundingBoxCenter(d3.select($(this).parent()[0]))[0] - 8;
            })
            .attr("y", function(d){
				return thisGraph.getBoundingBoxCenter(d3.select($(this).parent()[0]))[1] + 7;
            })
            .text(function(d) {
				return '\uf074';
			})
			.attr('data-uiname', function(d){ return d.source.uiname +'-'+d.target.uiname; })
            .style("display","none")
            .style("font-size","large")
            .attr("data-toggle", "popover")
            .attr("data-type", "shuffle")
			.on('mouseover', function(d){
				if(thisGraph.editMode) {
					// $(this).show();
				}
		    })
			.on('mouseout', function(d){
				if(thisGraph.editMode) {
					// $(this).hide();
				}
		    })
		    .on("mousedown", function(d) {
		    	if(thisGraph.editMode) {
		    		if(d3.event.shiftKey){
						var elem = $(this).parent().find('.visible-link[d="'+$(this).attr("d")+'"]')[0];
						thisGraph.pathMouseDown.call(thisGraph, d3.select(elem), d);
					} else {
						// thisGraph.showShuffle(d);
					}
		    	}
			})
			.on("mouseup", function(d) {
				if(thisGraph.editMode) {
					if(d3.event.shiftKey){
						state.mouseDownLink = null;
					} else {
						// $('[data-uiname="'+d.source.uiname+'-'+d.target.uiname+'"][data-toggle="popover"]').popover('show');
						// thisGraph.bindPopoverDropdown(d);
					}
				}
			});

		// remove old links
		paths.exit().remove();

		//set shuffle icon on links
		this.setLinkIcon();

		//adding dropdown for toggle
		this.showShuffle();

		//clone the paths or links to make hover on them with some hidden margin
		thisGraph.clonePaths();

		// update existing nodes
		thisGraph.rectangles = thisGraph.rectangles.data(thisGraph.nodes, function(d) {
			return d.uiname;
		});
		thisGraph.rectangles.attr("transform", function(d) {
			return "translate(" + d.x + "," + d.y + ")";
		});
		thisGraph.rectangles.select('text.node-title').text(function(d) {
			return d.uiname;
		});

		thisGraph.rectangles.selectAll('image')
			.attr("filter", function(d){
				if(!d.isConfigured){
					return "url(#grayscale)";
				} else return "";
			});
		thisGraph.rectangles.selectAll('circle')
			.attr("filter", function(d){
				if(!d.isConfigured){
					return "url(#grayscale)";
				} else return "";
			});

		//add new nodes
		var newGs = thisGraph.rectangles.enter()
				.append("g");
			newGs.classed(consts.rectangleGClass, true)
				.attr("transform", function(d) {
				return "translate(" + d.x + "," + d.y + ")";
			});
			newGs.append("image")
				.attr("xlink:href", function(d){
					return d.imageURL;
				})
				.attr("filter", function(d){
					if(!d.isConfigured){
						return "url(#grayscale)";
					} else return "";
				})
				.attr("width", "48px")
				.attr("height", "48px")
			    .on("mouseover", function(d) {
			    	if(thisGraph.editMode){
			    		$(this).css("opacity", "0.75");
						$(this).siblings('text.fa-times').show();
			    	}
				})
				.on("mouseout", function(d) {
					if(thisGraph.editMode){
						$(this).css("opacity", "1");
						$(this).siblings('text.fa-times').hide();
					}
				})
				.on("mousedown", function(d) {
					thisGraph.rectangleMouseDown.call(thisGraph, d3.select(this.parentNode), d);
				})
				.on("mouseup", function(d) {
					thisGraph.rectangleMouseUp.call(thisGraph, d3.select(this.parentNode), d);
				})
				.call(thisGraph.drag);

            newGs.append('text')
                .attr("class", "fa fa-times")
                .attr("x","36px")
                .attr("y","10px")
                .text(function(d) {
					return '\uf00d';
				})
                .style("display","none")
                .style("font-size","large")
				.on('mouseover', function(d){
					if(thisGraph.editMode){
						$(this).show();
					}
			    })
				.on('mouseout', function(d){
					if(thisGraph.editMode){
						$(this).hide();
					}
			    })
				.on('mousedown', function(d){
					if(thisGraph.editMode){
						thisGraph.deleteNode(d);
					}
			    });

			newGs.append("circle")
				.attr("cx", function (d) { 
					if(d.parentType !== Globals.Topology.Editor.Steps.DataSink.valStr)
			    		return (consts.rectangleWidth); 
			    	else
			    		return '';
				})
		        .attr("cy", function (d) { 
		        	if(d.parentType !== Globals.Topology.Editor.Steps.DataSink.valStr)
		        		return consts.rectangleHeight / 2;
		        	else
			    		return ''; 
		        })
		        .attr("r", function (d) {
					if(d.parentType !== Globals.Topology.Editor.Steps.DataSink.valStr)
			    		return '4.5';
			    	else
			    		return '0';
			    })
		        .attr("class", function(d){
					if(d.parentType === Globals.Topology.Editor.Steps.Datasource.valStr){
						return 'source';
					} else if(d.parentType === Globals.Topology.Editor.Steps.Processor.valStr){
						return 'processor';
					} else if(d.parentType === Globals.Topology.Editor.Steps.DataSink.valStr){
						return 'datasink';
					}
				})
				.attr("filter", function(d){
					if(!d.isConfigured){
						return "url(#grayscale)";
					} else return "";
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
					if(thisGraph.editMode){
						thisGraph.circleMouseDown.call(thisGraph, d3.select(this), d);
					}
				})
				.on("mouseup", function(d) {
					if(thisGraph.editMode){
						thisGraph.circleMouseUp.call(thisGraph, d3.select(this), d);
					}
				})
				.call(thisGraph.drag);

			newGs.append("circle")
				.attr("cx", function (d) { 
					if(d.currentType === Globals.Topology.Editor.Steps.Processor.Substeps[0].valStr)
			    		return (consts.rectangleWidth / 2); 
			    	else
			    		return '';
				})
		        .attr("cy", function (d) { 
		        	if(d.currentType === Globals.Topology.Editor.Steps.Processor.Substeps[0].valStr)
		        		return consts.rectangleHeight + 10;
		        	else
			    		return ''; 
		        })
		        .attr("r", function (d) { 
		        	if(d.currentType === Globals.Topology.Editor.Steps.Processor.Substeps[0].valStr)
			    		return '4.5';
			    	else
			    		return '0';
			    })
			    .attr("filter", function(d){
					if(!d.isConfigured){
						return "url(#grayscale)";
					} else return "";
				})
			    .attr("data-failedTuple", true)
		        .style("fill", "red")
		        .on("mouseover", function(d) {
					if (state.shiftNodeDrag) {
						d3.select(this).classed(consts.connectClass, true);
					}
				})
				.on("mouseout", function(d) {
					d3.select(this).classed(consts.connectClass, false);
				})
				.on("mousedown", function(d) {
					if(thisGraph.editMode){
						thisGraph.circleMouseDown.call(thisGraph, d3.select(this), d);
					}
				})
				.on("mouseup", function(d) {
					if(thisGraph.editMode){
						thisGraph.circleMouseUp.call(thisGraph, d3.select(this), d);
					}
				})
				.call(thisGraph.drag);

		    newGs.append("circle")
		        .attr("cy", function (d) { 
		        	if(d.parentType !== Globals.Topology.Editor.Steps.Datasource.valStr)
			    		return consts.rectangleHeight / 2;
		        })
		        .attr("r", function (d) { 
		        	if(d.parentType !== Globals.Topology.Editor.Steps.Datasource.valStr)
			    		return '4.5';
			    })
		        .attr("class", function(d){
					if(d.parentType === Globals.Topology.Editor.Steps.Datasource.valStr){
						return 'source';
					} else if(d.parentType === Globals.Topology.Editor.Steps.Processor.valStr){
						return 'processor';
					} else if(d.parentType === Globals.Topology.Editor.Steps.DataSink.valStr){
						return 'datasink';
					}
				})
				.attr("filter", function(d){
					if(!d.isConfigured){
						return "url(#grayscale)";
					} else return "";
				})
		        .on("mouseup", function(d) {
		        	if(thisGraph.editMode){
		        		thisGraph.circleMouseUp.call(thisGraph, d3.select(this), d);
		        	}
				});
		        
		        

		newGs.each(function(d) {
			// thisGraph.insertIcon(d3.select(this), d.iconContent);
			thisGraph.insertTitleLinebreaks(d3.select(this), d.uiname);
		});

		// remove old nodes
		thisGraph.rectangles.exit().remove();
	};

	TopologyGraphCreator.prototype.showShuffle = function(){
		var thisGraph = this;
		var shuffleArr = thisGraph.data.linkShuffleOptions;
		var html = "<div><select class='link-shuffle'>";
		for(var i = 0, len = shuffleArr.length; i < len; i++){
			html += "<option value='"+shuffleArr[i].val+"'>"+shuffleArr[i].label+"</option>";
		}
		html += "</select></div>";
		$('[data-toggle="popover"][data-type="shuffle"]').popover({
			title: "Select Grouping",
    		html: true,
    		content: html,
    		container: "body",
    		placement: "top"
    	});
    	$('.link-shuffle').select2();
	};

	TopologyGraphCreator.prototype.hideShuffle = function(){
		$('.link-shuffle').off('change');
		$('[data-toggle="popover"][data-type="shuffle"]').popover('hide');
	};

	TopologyGraphCreator.prototype.bindPopoverDropdown = function(d){
		$('.link-shuffle').on('change', function(e){
    		console.log(d);
    		console.log(e);
    	});
	};

	TopologyGraphCreator.prototype.unbindPopoverDropdown = function(){
		$('.link-shuffle').off('change');
	};

	TopologyGraphCreator.prototype.clonePaths = function(){
		var element = $('svg path.link').not('.hidden');
		for(var i = 0, len = element.length ; i < len; i++){
			var cloneElem = $(element[i]).clone();
			cloneElem.css('marker-end', 'url(#end-arrow)')
				.attr("stroke-dasharray", "5, 5")
				.attr('stroke-width', '2')
				.removeAttr('stroke-opacity')
				.removeAttr('data-toggle')
				.attr('class', 'link visible-link');
			cloneElem.insertBefore(element[i]);
		}
	};

	TopologyGraphCreator.prototype.setLinkIcon = function(){
		$('.link-group > text').remove();
		var shuffleElement = $('path > text').detach();
		$('.link-group').append(shuffleElement);
	};

	TopologyGraphCreator.prototype.getBoundingBoxCenter = function(selection) {
	    // get the DOM element from a D3 selection
	    // you could also use "this" inside .each()
	    var element = selection.node(),
	        // use the native SVG interface to get the bounding box
	        bbox = element.getBBox();
	    // return the center of the bounding box
	    return [bbox.x + bbox.width/2, bbox.y + bbox.height/2];
	};

	TopologyGraphCreator.prototype.zoomed = function() {
		this.state.justScaleTransGraph = true;
		d3.select("." + this.consts.graphClass)
			.attr("transform", "translate(" + this.dragSvg.translate() + ")" + "scale(" + this.dragSvg.scale() + ")");
	};

	TopologyGraphCreator.prototype.updateWindow = function(svg) {
		var thisGraph = this;
		var x = thisGraph.elem.width();
		var y = thisGraph.elem.height();
		svg.attr("width", x).attr("height", y);
	};
	return TopologyGraphCreator;
});