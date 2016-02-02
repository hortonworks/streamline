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
      if(this.model.has('_dataSourceId')) {
        this.setSourceName(this.model.has('dataSourceId'));
      }
    },

    setSourceName: function(flag){
      var dsModel = new VDatasource();
      dsModel.set('dataSourceId', this.model.get('_dataSourceId'));
      dsModel.set('id', this.model.get('_dataSourceId'));
      dsModel.fetch({async: false});
      this.model.set('_dataSourceName', dsModel.get('entity').dataSourceName);
      this.model.set('_parserName', dsModel.get('entity').parserName);
      this.model.set('_parserId', dsModel.get('entity').parserId);
      if(flag){
        this.model.set('dataSourceName', dsModel.get('entity').dataSourceName);
        this.model.set('parserName', dsModel.get('entity').parserName);
        this.model.set('parserId', dsModel.get('entity').parserId);
      }
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
      } else {
        return false;
      }
    },
    saveParserProcessor: function() {
      var data = this.view.getData();
      if(data.get('emptyParser')){
        // data.set('_parserId', data.get('parserId'));
        delete data.attributes.parserId;
      }
      if(data.get('emptySource')){
        // data.set('_dataSourceId', data.get('dataSourceId'));
        delete data.attributes.dataSourceId;
      }
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