define(['require', 'hbs!tmpl/site/footer'], function(require, tmpl){
  'use strict';

  var vFooter = Marionette.LayoutView.extend({
    template: tmpl,
    templateHelpers: function() {},
    regions: {},
    events: {},
    initialize: function(options) {
      console.log('Initialized vFooter');
    },
    onRender: function(){}
  });
  return vFooter;
});