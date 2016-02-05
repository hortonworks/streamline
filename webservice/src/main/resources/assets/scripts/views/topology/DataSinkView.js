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
      var self = this;
      var err = this.view.validate();
      if(_.isEmpty(err)){
        this.saveDataSink();
      } else {
        var errArr = _.keys(err);
        _.each(errArr, function(name){
          var target = self.$('[data-fields="'+name+'"]').parents('.panel.panel-default');
          if(target.length){
            if(target.find('a').hasClass('collapsed')){
              target.find('a').trigger('click');
            }
            target.addClass('error');
          }
        });
        return false;
      }
    },
    saveDataSink: function(){
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