define(['require',
    'hbs!tmpl/site/header',
    'modules/Vent'
  ], function(require, tmpl, Vent){
  'use strict';

  var vHeader = Marionette.LayoutView.extend({
    template: tmpl,
    templateHelpers: function() {},
    regions: {},
    events: {
      'click #menu-toggle': 'menuToggleAction',
    },
    initialize: function(options) {
      this.vent = Vent;
    },
    menuToggleAction: function(e){
      this.vent.trigger('sidebar-menu-toggle');
    },
    onRender: function(){},
    
  });
  return vHeader;
});