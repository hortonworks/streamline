define(['require',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'modules/Modal',
  'hbs!tmpl/topology/dataProcessorView'
], function(require, localization, Utils, Globals, Modal, tmpl) {
  'use strict';

  var DataProcessorView = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #btnCancel': 'evClose',
      'click #btnAdd': 'evAdd'
    },

    regions: {
      formulaForm: '.formulaForm'
    },

    initialize: function(options) {
      _.extend(this, options);
      this.bindEvents();
    },

    bindEvents: function(){
      var self = this;
      this.listenTo(this.vent, 'change:Formula', function(data){
        self.generateForumla(data);
      });
    },

    generateForumla: function(obj){
      console.log(obj);
    },

    onRender:function(){
      var self = this;
      this.$('[data-rel="tooltip"]').tooltip();
      require(['views/topology/FormulaCompositeView'], function(FormulaCompositeView){
        self.view = new FormulaCompositeView({
          vent: self.vent
        });
        self.formulaForm.show(self.view);
      });
    },
    evAdd: function(e){
      this.vent.trigger('dataStream:SavedStep2', {});
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return DataProcessorView;
});