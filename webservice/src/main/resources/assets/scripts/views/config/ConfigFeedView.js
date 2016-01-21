define(['require',
    'modules/Vent',
    'hbs!tmpl/config/configFeedView',
    'utils/Utils',
    'utils/LangSupport'
  ], function(require, Vent, tmpl, Utils, localization){
  'use strict';

  var vConfigFeedView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    regions: {
      rForm: '#rForm',
    },
    
    events: {
      'click #btnCancel': 'evClose',
      'click #btnSave': 'evSave'
    },

    initialize: function (options) {
      _.extend(this, options);
      this.vent = Vent;
    },

    onRender: function () {
      var self = this;
      require(['views/config/ConfigComponentForm'], function(ConfigComponentForm){   
        self.view = new ConfigComponentForm({
          model: self.model,
          type: self.type
        });
        self.rForm.show(self.view);        
      });
    },

    evSave: function(e){

      var errs = this.view.validate();
      if(_.isEmpty(errs)){
        this.saveDataSource();
      } else {
        return false;
      }     
    },
    
    saveDataSource: function(){
      var data = this.view.getData();
  
      this.vent.trigger('click:Save', data);
      this.evClose();
    },
    
    evClose: function(e){
      this.trigger('closeModal');
    }
   
  });
  return vConfigFeedView;
});
