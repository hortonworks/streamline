define(['require', 'utils/Globals', 'd3', 'd3-tip'], function(require, Globals, d3) {
  'use strict'

  var topologyGraphView = Marionette.LayoutView.extend({
    template: _.template('<div id="topoGraph"></div>'),

    initialize: function(options) {
      this.width = options.width;
      this.height = options.height;
      this.modal = options.modalFlag;
      // this.fetchData(options.id);
      this.graphData = {};
    },
    fetchData: function(topologyId){
      var that = this;
      Backbone.ajax({
        url: Globals.baseURL + "/api/v1/topology/" + topologyId + "/visualization",
        async: false,
        success: function(data, status, jqXHR) {
          if(_.isString(data)){
            data = JSON.parse(data);
          }
          that.graphData = data;
        }
      });
    },
    onRender: function() {
      var that = this;
      this.nodeArray = [];
      this.linkArray = [];
      _.each(_.keys(this.graphData), function(key){
        if(!key.startsWith('__')){
          that.graphData[key].id = key;
          that.graphData[key].type = that.graphData[key][":type"];
          that.nodeArray.push(that.graphData[key]);
        }
      });
      var spoutObjArr = _.where(this.nodeArray,{"type":"spout"});
      if(spoutObjArr.length > 1){
        this.nodeArray[this.nodeArray.length -1].x = (this.nodeArray.length >=6) ? 50 : 150;
        this.nodeArray[this.nodeArray.length -1].y = 100;
        this.nodeArray[this.nodeArray.length -1].fixed = true;
      } else if(spoutObjArr.length == 1){
        spoutObjArr[0].x = (this.nodeArray.length >=6) ? 50 : 150;
        spoutObjArr[0].y = 100;
        spoutObjArr[0].fixed = true;
      }
      _.each(that.nodeArray, function(obj){
        that.tempObj = obj;
        _.each(obj[":inputs"], function(inputObj){
          if(!inputObj[":component"].startsWith("__")){
            var obj = {
              "source": _.findWhere(that.nodeArray,{"id":inputObj[":component"]}), 
              "target": that.tempObj
            };
            that.linkArray.push(obj);
          }
        });
      });
      this.nodeArray = [
        {id: "spout", reflexive: true, type: "spout",x: 200, y:100, fixed: true, ":latency": 10,":capacity": 0.123123,":transferred":1},
        {id: "split", reflexive: true, type: "bolt", ":capacity": 1.12, ":latency": 10,":transferred":1},
        {id: "count", reflexive: true, type: "bolt", ":capacity": 0.123123, ":latency": 10,":transferred":1}
      ],
      this.linkArray = [
        {source: this.nodeArray[0], target: this.nodeArray [1], left: false, right: true },
        {source: this.nodeArray [1], target: this.nodeArray [2], left: false, right: true }
      ];
      if(this.nodeArray.length && this.linkArray.length){
        this.renderGraph(that.nodeArray, that.linkArray);
      } else {
        this.$el.find('#topoGraph').html('<h4><center>No Topology Found.</center></h4>')
      }
    },

    renderGraph: function(nodes, links){
      var that = this;
      this.nodes = nodes;
      var width  = this.width,
          height = this.height,
          radius = (nodes.length >= 6 ) ? 20.75 : 30.75,
          colors = {"spout":"#2685ee", 'bolt':"#81c04d", "capacitor":"#ef553a"};

      var elem;
      if(this.modal){
        this.$el.find('#topoGraph').addClass('graph-modal');
        elem = this.$el.find('.graph-modal')[0];
      } else {
        elem = this.$el.find('#topoGraph')[0];
      }

      var svg = d3.select(elem)
        .append('svg')
        .attr('width', '100%')
        .attr('height', height);

      // init D3 force layout
      var force = d3.layout.force()
          .nodes(nodes)
          .links(links)
          .size([width, height])
          .linkDistance(150)
          .charge(-500)
          .on('tick', tick);

      //Set up tooltip
      this.tip = d3.tip()
        .attr('class', function(){
          if(that.modal){
            return 'd3-tip modal-tip';
          }else{
            return 'd3-tip';
          }
        })
        .offset([-10, 0])
        .html(function (d) {
            var tip = "<ul>";
            if(d[":capacity"] !== null) tip += "<li>Capacity: " + d[":capacity"].toFixed(2) + "</li>" ;
            if(d[":latency"] !== null) tip += "<li>Latency: " + d[":latency"].toFixed(2) + "</li>" ;
            if(d[":transferred"] !== null) tip += "<li>Transferred: " + d[":transferred"].toFixed(2) + "</li>" ;
            tip += "</ul>";
          return tip;
        });
      svg.call(this.tip);

      // define arrow markers for graph links
      svg.append('svg:defs').append('svg:marker')
          .attr('id', 'end-arrow')
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 6)
          .attr('markerWidth', 3)
          .attr('markerHeight', 3)
          .attr('orient', 'auto')
        .append('svg:path')
          .attr('d', 'M0,-5L10,0L0,5')
          .attr('fill', '#000');

      svg.append('svg:defs').append('svg:marker')
          .attr('id', 'start-arrow')
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 4)
          .attr('markerWidth', 3)
          .attr('markerHeight', 3)
          .attr('orient', 'auto')
        .append('svg:path')
          .attr('d', 'M10,-5L0,0L10,5')
          .attr('fill', '#000');

      // handles to link and node element groups
      var path = svg.append('svg:g').selectAll('path'),
          circle = svg.append('svg:g').selectAll('g');

      var selectedNode = null;

      // update force layout (called automatically each iteration)
      function tick() {
        // draw directed edges with proper padding from node centers
        path.attr('d', function(d) {
          var deltaX = d.target.x - d.source.x,
              deltaY = d.target.y - d.source.y,
              dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
              normX = deltaX / dist,
              normY = deltaY / dist,
              sourcePadding = (that.nodes.length >=6) ? 12 : 22,
              targetPadding = (that.nodes.length >=6) ? 22 : 32,
              sourceX = d.source.x + (sourcePadding * normX),
              sourceY = d.source.y + (sourcePadding * normY),
              targetX = d.target.x - (targetPadding * normX),
              targetY = d.target.y - (targetPadding * normY);
          return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
        });

        circle.attr('transform', function(d) {
          return 'translate(' + Math.max(radius, Math.min(width - radius, d.x)) + ',' + Math.max(radius, Math.min(height - radius, d.y)) + ')';
        });
      }

      // update graph (called when needed)
      function restart() {
        // path (link) group
        path = path.data(links);

        // update existing links
        path.style('marker-start', function(d) { return ''; })
          .style('marker-end', function(d) { return 'url(#end-arrow)'; });


        // add new links
        path.enter().append('svg:path')
          .attr('class', 'link')
          .style('marker-start', function(d) { return ''; })
          .style('marker-end', function(d) { return 'url(#end-arrow)'; });

        // remove old links
        path.exit().remove();


        // circle (node) group
        // NB: the function arg is crucial here! nodes are known by id, not by index!
        circle = circle.data(nodes, function(d) { return d.id; });

        // update existing nodes (reflexive & selected visual states)
        circle.selectAll('circle')
          .style('fill', function(d) { 
            if(d[":capacity"] && d[":capacity"] > 1){
              return (d === selectedNode) ? d3.rgb(colors['capacitor']).brighter().toString() : colors['capacitor'];
            } else {
              return (d === selectedNode) ? d3.rgb(colors[d.type]).brighter().toString() : colors[d.type];
            }})
          .classed('reflexive', 'true');

        // add new nodes
        var g = circle.enter().append('svg:g');

        g.append('svg:circle')
          .attr('class', 'node')
          .attr('r', radius - .75)
          .style('fill', function(d) { 
            if(d[":capacity"] && d[":capacity"] > 1){
              return (d === selectedNode) ? d3.rgb(colors['capacitor']).brighter().toString() : colors['capacitor'];
            } else {
              return (d === selectedNode) ? d3.rgb(colors[d.type]).brighter().toString() : colors[d.type];
            }})
          // .style('stroke', function(d) { 
          //   if(d.type == 'bolt'){
          //     if(d[":capacity"] && d[":capacity"] > 1){
          //       return '#ed4122 !important';
          //     } else {
          //       return '#74b440 !important';
          //     }
          //   } else if(d.type =='spout'){
          //     return '#1278e8 !important';
          //   }
          //   // return d3.rgb(colors[d.type]).darker().toString(); 
          // })
          .classed('reflexive', 'true')
          .on('mouseover', function(d) {
            // enlarge target node
            d3.select(this).attr('transform', 'scale(1.1)');
            that.tip.show(d);
          })
          .on('mouseout', function(d) {
            // unenlarge target node
            d3.select(this).attr('transform', '');
            that.tip.hide();
          });

        // show node IDs
        g.append('svg:text')
            .attr('x', 0)
            .attr('y', function(d){ return (d.type == 'bolt') ? 8 : 12;})
            .attr('class', 'id')
            .attr('style', function(d){
              if(d.type == 'bolt'){
                return 'font-family: FontAwesome; font-size: 24px; fill: #ffffff;';
              } else {
                return 'font-family: icomoon; font-size: 24px; fill: #ffffff;';
              }
            })
            .html(function(d) { 
              if(d.type == 'bolt'){
                return '&#xf0e7;';
              } else {
                return '&#xe600;';
              }
            });

        g.append("svg:text")
            .attr("dx", function(d){
              if(that.nodes.length >= 6)
                return 25; 
              else 
                return 35;
            })
            .text(function(d) { return d.id; });

        // remove old nodes
        circle.exit().remove();
      }

      // only respond once per keydown
      var lastKeyDown = -1;

      function keydown() {

        if(lastKeyDown !== -1) return;
        lastKeyDown = d3.event.keyCode;

        // ctrl
        if(d3.event.keyCode === 17) {
          d3.event.preventDefault();
          that.tip.hide();
          circle.call(force.drag);
          svg.classed('ctrl', true);
        }

      }

      function keyup() {
        lastKeyDown = -1;

        // ctrl
        if(d3.event.keyCode === 17) {
          circle
            .on('mousedown.drag', null)
            .on('touchstart.drag', null);
          svg.classed('ctrl', false);
        }
      }

      d3.select(window)
        .on('keydown', keydown)
        .on('keyup', keyup);

      // app starts here
      restart();
      force.start();
      for(var i = 300 ; i > 0; --i)force.tick();
      force.stop();
    }

  });

  return topologyGraphView;
});
