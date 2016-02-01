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
      this.model.set('firstTime', false);
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        dataSourceName: {
          type: 'Text',
          title: 'Source Name',
          editorClass: 'form-control',
          editorAttrs: {
            readonly: 'readonly'
          },
          placeHolder: 'Source Name',
        },
        parserName: {
          type: 'Text',
          title: 'Parser Name',
          editorClass: 'form-control',
          editorAttrs: {
            readonly: 'readonly'
          },
          placeHolder: 'Parser Name',
        },
        parallelism: {
          type: 'Text',
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
      return this.model.set(attrs);
    },

    close: function() {
    }
  });

  return ParserForm;
});