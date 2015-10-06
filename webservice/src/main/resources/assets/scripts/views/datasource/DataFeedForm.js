define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/datasource/dataFeedForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var DataFeedForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      this.model = options.model;
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        dataFeedName: {
          type: 'Text',
          title: localization.tt('lbl.dataFeedName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.dataFeedName'),
          validators: ['required']
        },
        parserId: {
          type: 'Number',
          title: localization.tt('lbl.parserId')+'*',
          editorClass: 'form-control',
          editorAttrs: {
            min: 1
          }, 
          placeHolder: localization.tt('lbl.parserId'),
          validators: ['required']
        },
        endpoint: {
          type: 'Text',
          title: localization.tt('lbl.endpoint')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.endpoint'),
          validators: ['required']
        },
      };
    },

    onRender: function(){
      
    },

    getData: function () {
      return this.model.set(this.getValue());
    },

    close: function () {
      console.log('Closing form view');
    }
  });

  return DataFeedForm;
});