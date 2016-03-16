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

    render: function(options){ 
      Backbone.Form.prototype.render.call(this, options);
      var tooltipData = [
        {id: 'streamingEngine', val: 'Streaming Engine for this custom processor'},
        {id: 'name', val: 'Name of the custom processor should be unique and is used to identify the custom processor'},
        {id: 'description', val:'Description of the custom processor'},
        {id: 'imageFileName', val: 'Unique name of the file that will be used to upload the image. Using same file for different Custom Processor will override the previous one.'},
        {id: 'jarFileName', val: 'Unique name of the jar file that will be used to upload the jar. Using same file for different Custom Processor will override the previous one.'},
        {id: 'customProcessorImpl', val: 'Fully qualified class name implementing the interface'}
      ];
 
      _.each(tooltipData, function(field) {
        this.$('[name="'+field.id+'"]').tooltip({'trigger':'hover', 'title': field.val});
      }, this); 
    },

    getData: function () {
      return this.getValue();
    },

    close: function () {}
  });

  return vConfigComponentForm;
});