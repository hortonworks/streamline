define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/topology/parserProcessorForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var ParserForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, options);
      if(this.model.get('firstTime')){
        this.model.set('dataSourceId', this.model.get('_dataSourceId'));
        this.model.set('parserId', this.model.get('_parserId'));
      }
      this.model.set('firstTime', false);
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        dataSourceId: {
          type: 'Select2',
          title: 'Source Name',
          editorClass: 'form-control',
          pluginAttr: {
            placeholder: 'Source Name',
            allowClear: true,
          },
          options: [{val: '0', label:'None'}, {val: this.model.get('_dataSourceId'), label: this.model.get('_dataSourceName')}]
        },
        parserId: {
          type: 'Select2',
          title: 'Parser Name',
          editorClass: 'form-control',
          pluginAttr: {
            placeholder: 'Parser Name',
            allowClear: true,
          },
          options: [{val: '0', label:'None'}, {val: this.model.get('_parserId'), label: this.model.get('_parserName')}]
        },
        parallelism: {
          type: 'Number',
          title: 'Parallelism',
          editorClass: 'form-control',
          placeHolder: 'Parallelism',
        },
      };
    },

    render: function(options){
      Backbone.Form.prototype.render.call(this,options);
    },

    getData: function() {
      var attrs = this.getValue();
      if(attrs.dataSourceId === '0' || _.isNull(attrs.dataSourceId)){
        this.model.set('emptySource', true);
      } else {
        this.model.set('emptySource', false);
      }
      if(attrs.parserId === '0' || _.isNull(attrs.parserId)){
        this.model.set('emptyParser', true);
      } else {
        this.model.set('emptyParser', false);
      }
      attrs.dataSourceId = parseInt(attrs.dataSourceId);
      attrs.parserId = parseInt(attrs.parserId);
      return this.model.set(attrs);
    },

    close: function() {
    }
  });

  return ParserForm;
});