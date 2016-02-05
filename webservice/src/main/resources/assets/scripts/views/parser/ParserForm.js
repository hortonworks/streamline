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
        name: {
          type: 'Text',
          title: localization.tt('lbl.parserName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.parserName'),
          validators: [{'type':'required','message':'Parser name can not be blank.'}]
        },
        className: {
          type: 'Text',
          title: localization.tt('lbl.className')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.className'),
          validators: [{'type':'required','message':'Classname can not be blank.'}]
        },
        version: {
          type: 'Number',
          title: localization.tt('lbl.version')+'*',
          editorClass: 'form-control',
          editorAttrs: {
            min:0
          },
          placeHolder: localization.tt('lbl.parserVersion'),
          validators: [{'type':'required','message':'Version can not be blank.'}]
        },
        parserJar: {
          type: 'Fileupload',
          title: localization.tt('lbl.parserJar')+'*',
          placeHolder: localization.tt('lbl.parserJar'),
          validators: [{'type':'required','message':'Select particular parser jar to upload.'}]
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