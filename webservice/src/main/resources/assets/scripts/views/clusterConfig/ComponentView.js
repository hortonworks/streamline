define(['require',
    'modules/Vent',
    'hbs!tmpl/clusterConfig/componentView',
    'utils/Utils',
    'utils/LangSupport'
  ], function(require, Vent, tmpl, Utils, localization){
  'use strict';

  var vComponentView = Marionette.LayoutView.extend({

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
    },

    onRender: function () {
      var self = this;
      require(['views/clusterConfig/ComponentForm'], function(ComponentForm){   
        self.view = new ComponentForm({
          model: self.model,
          type: self.type,
          componentArr: self.componentArr
        });
        self.rForm.show(self.view);        
      });
    },

    evSave: function(e){

      var errs = this.view.validate();
      if(_.isEmpty(errs)){
        this.saveComponent();
      } else {
        return false;
      }     
    },
    
    saveComponent: function(){
      var data = this.view.getData();
  
      this.vent.trigger('component:Save', data);
      this.evClose();
    },
    
    evClose: function(e){
      this.trigger('closeModal');
    }
   
  });
  return vComponentView;
});
