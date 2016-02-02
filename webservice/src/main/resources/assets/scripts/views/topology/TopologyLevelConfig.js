define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/topologyLevelConfig'
], function(require, vent, localization, Utils, Globals, tmpl) {
  'use strict';

  var TopologyConfigLayout = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #btnCancel': 'evClose',
      'click #btnAdd': 'evSave'
    },

    regions: {
      rForm: '#rForm',
    },

    initialize: function(options) {
     _.extend(this, options); 
    },

    onRender:function(){
      var self = this;
      require(['views/topology/TopologyConfigForm'], function(TopologyConfigForm){
        self.view = new TopologyConfigForm({
          model: self.model
        });
        self.rForm.show(self.view);
      });
    },

    evSave: function(e){
      var err = this.view.validate();
      if(_.isEmpty(err)){
        this.saveConfig();
      } else {
        return false;
      }
    },
    saveConfig: function(){
      var data = this.view.getData();
      this.vent.trigger('topologyEditor:SaveConfig', data);
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return TopologyConfigLayout;
});