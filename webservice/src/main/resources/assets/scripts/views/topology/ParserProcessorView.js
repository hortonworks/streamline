define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/parserProcessorView'
], function(require, vent, localization, Utils, Globals, tmpl) {
  'use strict';

  var ParserProcessorLayout = Marionette.LayoutView.extend({

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
      require(['views/topology/ParserProcessorForm'], function(ParserForm){
        self.view = new ParserForm({
          model: self.model
        });
        self.rForm.show(self.view);
      });
    },

    evAdd: function(e){
      var err = this.view.validate();
      if(_.isEmpty(err)){
        this.saveParserProcessor();
      }
    },
    saveParserProcessor: function(){
      var data = this.view.getData();
      console.log(data);
      this.vent.trigger('dataStream:SavedStep2', data);
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return ParserProcessorLayout;
});