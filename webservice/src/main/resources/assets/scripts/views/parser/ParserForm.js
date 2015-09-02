define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/parser/parserForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var ParserForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        parserName: {
          type: 'Text',
          title: localization.tt('lbl.parserName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.parserName'),
          validators: ['required']
        },
        className: {
          type: 'Text',
          title: localization.tt('lbl.className')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.className'),
          validators: ['required']
        },
        version: {
          type: 'Text',
          title: localization.tt('lbl.version')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.parserVersion'),
          validators: ['required']
        },
        parserJar: {
          type: 'Fileupload',
          title: localization.tt('lbl.parserJar')+'*',
          placeHolder: localization.tt('lbl.parserJar'),
          validators: ['required']
        }
      };
    },

    getData: function () {
      return this.getValue();
    },

    close: function () {
      console.log('Closing form view');
    }
  });

  return ParserForm;
});