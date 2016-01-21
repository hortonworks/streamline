define(['require',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/dataSinkView'
], function(require, localization, Utils, Globals, tmpl) {
  'use strict';

  var DataSinkView = Marionette.LayoutView.extend({

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
      require(['views/topology/DataSinkForm'], function(DataSinkForm){
        self.view = new DataSinkForm({
          model: self.model,
          type: self.type
        });
        self.rForm.show(self.view);
      });
    },
    evAdd: function(e){
      var err = this.view.validate();
      if(_.isEmpty(err)){
        this.saveDataSink();
      }
    },
    saveDataSink: function(){
      // var self = this;
      var data = this.view.getData();
      console.log(data);
      this.vent.trigger('topologyEditor:SaveSink', data);
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return DataSinkView;
});