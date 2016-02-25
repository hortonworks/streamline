define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/dataFeedView'
], function(require, vent, localization, Utils, Globals, tmpl) {
  'use strict';

  var DataSourceLayout = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #btnCancel': 'evClose',
      'click #btnAdd': 'evAdd'
    },

    regions: {
      rForm: '#rForm',
    },

    initialize: function(options) {
     _.extend(this, options); 
    },

    onRender:function(){
      var self = this;
      require(['views/topology/DataFeedForm'], function(DataFeedForm){
        self.view = new DataFeedForm({
          model: self.model
        });
        self.rForm.show(self.view);
      });
    },

    evAdd: function(e){
      var err = this.view.validate();
      if(_.isEmpty(err)){
        this.saveDevice();
      } else {
        return false;
      }
    },
    saveDevice: function(){
      var data = this.view.getData();
      this.vent.trigger('topologyEditor:SaveDeviceSource', data);
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return DataSourceLayout;
});