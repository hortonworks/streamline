define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/dataFeedView'
], function(require, vent, localization, Utils, Globals, tmpl) {
  'use strict';

  var DataFeedLayout = Marionette.LayoutView.extend({

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
        this.saveDataFeed();
      } else {
        return false;
      }
    },
    saveDataFeed: function(){
      // var self = this;
      var data = this.view.getData();
      console.log(data);
      this.vent.trigger('topologyEditor:SaveDeviceSource', data);
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return DataFeedLayout;
});