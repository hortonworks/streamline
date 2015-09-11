define(['require', 'hbs!tmpl/site/sidebar', 'modules/Vent'], function(require, tmpl, Vent){
  'use strict';

  var vSidebar = Marionette.LayoutView.extend({
    template: tmpl,
    templateHelpers: function() {},
    regions: {},
    events: {},
    initialize: function(options) {
      _.bindAll(this, 'highlightTab');
      this.vent = Vent;
      this.appState = options.appState;
      this.appState.on('change:currentTab', this.highlightTab);
      this.bindEvents();
    },
    bindEvents: function(){
      this.listenTo(this.vent, 'sidebar-menu-toggle', function(){
        $('#wrapper').toggleClass('toggled');
      });
    },
    highlightTab : function(){
      this.$('ul > li').siblings('.active').removeClass('active');
      if(this.appState.get('currentTab')){
        this.$('li#tab' + this.appState.get('currentTab')).addClass('active');
      }
    },
    onRender: function(){
      this.highlightTab();
    }
  });
  return vSidebar;
});