define(['utils/LangSupport',
  'utils/Globals',
  'utils/Utils',
  'hbs!tmpl/parser/parserForm',
  'backbone.forms'
  ], function (localization, Globals, Utils, tmpl) {
  'use strict';

  var ParserForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      this.bindEvents();
      Backbone.Form.prototype.initialize.call(this, options);
    },
    bindEvents: function(){
      var self = this;
      this.on('parserJar:change', function(form, editor){
        var jarObj = editor.getValue();
        if(!_.isUndefined(jarObj)){
          var formData = new FormData(),
            url = Globals.baseURL + '/api/v1/catalog/parsers/upload-verify';
          formData.append('parserJar', editor.getValue());
          var successCallback = function(response){
            var result = [''];
            Array.prototype.push.apply(result, response.entities);
            self.showOtherFields(result);
          };
          var errorCallback = function(model, response, options){
            Utils.showError(model, response);
          };
          Utils.uploadFile(url,formData,successCallback, errorCallback);
        } else {
          self.hideOtherFields();
        }
      });
    },

    showOtherFields: function(classNamesList){
      this.$('[data-fields="name"]').show();
      this.$('[data-fields="className"]').show();
      this.$el.find('#className').select2('destroy');
      this.$el.find('#className').select2(_.extend({}, this.schema.className.pluginAttr, {data: classNamesList}));
      this.$('[data-fields="version"]').show();
    },

    hideOtherFields: function(){
      this.$('[data-fields="name"]').hide();
      this.$('[data-fields="className"]').hide();
      this.$('[data-fields="version"]').hide();
    },

    schema: function () {
      return {
        parserJar: {
          type: 'Fileupload',
          title: localization.tt('lbl.parserJar')+'*',
          placeHolder: localization.tt('lbl.parserJar'),
          validators: [{'type':'required','message':'Select particular parser jar to upload.'}]
        },
        name: {
          type: 'Text',
          title: localization.tt('lbl.parserName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.parserName'),
          validators: [{'type':'required','message':'Parser name can not be blank.'}]
        },
        className: {
          type: 'Select2',
          title: localization.tt('lbl.className')+'*',
          options: [],
          editorClass: 'form-control',
          pluginAttr: {
            placeholder: localization.tt('lbl.className'),
            allowClear: true,
          },
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
      };
    },

    render: function(options){
      Backbone.Form.prototype.render.call(this,options);
      this.hideOtherFields();
      return this;
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