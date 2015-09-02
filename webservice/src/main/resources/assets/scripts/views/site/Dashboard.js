define([
  'require',
  'hbs!tmpl/site/dashboard',
  'utils/Utils',
  'views/common/TopologyGraph'
  ], function(require, tmpl, Utils, TopologyGraphView){
  'use strict';

  var vDashboard = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    regions: {
      'rDAGGraph': '#dagGraph'
    },

    events: {
      'click .expand-link': function(e){
        Utils.expandPanel(e);
      }
    },

    ui: {},

    initialize: function (options) {
      console.log('Initialized vDashboard');
    },

    onRender: function () {
      this.showDAG();
      Utils.panelMinimize(this);
    },
    showDAG: function(){
      this.rDAGGraph.show(new TopologyGraphView({
        id: '1', //TODO - Dynamic ID of Topology
        width: 1021,
        height: 250
      }));
    }
  });
  return vDashboard;
});
