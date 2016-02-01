define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/parserProcessorView',
  'models/VDatasource'
], function(require, vent, localization, Utils, Globals, tmpl, VDatasource) {
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
      if (!this.model.has('dataSourceName')) {
        this.setSourceName();
      }
    },

    setSourceName: function(){
      var dsModel = new VDatasource();
      dsModel.set('dataSourceId', this.model.get('dataSourceId'));
      dsModel.set('id', this.model.get('dataSourceId'));
      dsModel.fetch({async: false});
      this.model.set('dataSourceName', dsModel.get('entity').dataSourceName);
      this.model.set('parserName', dsModel.get('entity').parserName);
    },

    onRender: function() {
      var self = this;
      require(['views/topology/ParserProcessorForm'], function(ParserForm) {
        self.view = new ParserForm({
          model: self.model
        });
        self.rForm.show(self.view);
      });
    },

    evAdd: function(e) {
      var err = this.view.validate();
      if (_.isEmpty(err)) {
        this.saveParserProcessor();
      }
    },
    saveParserProcessor: function() {
      var data = this.view.getData();
      console.log(data);
      this.vent.trigger('topologyEditor:SaveProcessor', data);
      this.evClose();
    },
    evClose: function(e) {
      this.trigger('closeModal');
    }

  });

  return ParserProcessorLayout;
});