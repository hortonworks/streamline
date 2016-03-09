define(['utils/LangSupport',
  'utils/Globals',
  'utils/Utils',
  'hbs!tmpl/customProcessor/processor-config-form',
  'backbone.forms'
  ], function (localization, Globals, Utils, tmpl) {
  'use strict';

  var vConfigComponentForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, options);
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        streamingEngine: {
          type: 'Text',
          title: 'Streaming Engine*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'Streaming Engine is required'}],
          editorAttrs: {
            disabled: 'disabled'
          }
         },
         name: {
          type: 'Text',
          title: 'Name*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'Name is required.'}],
          editorAttrs: this.editState? {disabled: 'disabled'} : {}
         },
         description: {
          type: 'Text',
          title: 'Description*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'Description is required'}]
        },
         customProcessorImpl: {
          type: 'Text',
          title: 'Classname*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':' Class Name is required'}]
        },        
        imageFileName: {
          type: 'Fileupload',
          title: 'Upload Image*',
          placeHolder: '',
          validators: [{'type':'required','message':'Select particular image file to upload.'}],
          elementId: 1
        },
        jarFileName: {
          type: 'Fileupload',
          title: 'Upload Jar*',
          placeHolder: '',
          validators: [{'type':'required','message':'Select particular processor jar to upload.'}],
          elementId: 2
        }
      };
    },

    onRender: function(){ },

    getData: function () {
      return this.getValue();
    },

    close: function () {}
  });

  return vConfigComponentForm;
});