define(['utils/LangSupport',
  'utils/Globals',
  'utils/Utils',
  'hbs!tmpl/customProcessor/config-fields',
  'backbone.forms'
  ], function (localization, Globals, Utils, tmpl) {
  'use strict';

  var ConfigFieldsLayoutView = Marionette.LayoutView.extend({

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
      require(['views/customProcessor/ConfigFieldsFormView'], function(ConfigFieldsFormView){
        self.view = new ConfigFieldsFormView({
          model: self.model
        });
        self.rForm.show(self.view);
      });
    },

    evAdd: function(e){
      var err = this.view.validate();
      if(_.isEmpty(err)){
        this.saveConfig();
      } else {
        return false;
      }
    },
    saveConfig: function(){
      var data = this.view.getData(),
        self = this;
      this.vent.trigger('customProcessorConfig:SaveConfig', self.model.set(data));
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });

  return ConfigFieldsLayoutView;
});