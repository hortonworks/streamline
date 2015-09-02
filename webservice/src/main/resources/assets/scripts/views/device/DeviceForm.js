define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/device/deviceForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var ParserForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      this.model = options.model;
      this.readOnlyFlag = options.readOnlyFlag;
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        dataSourceName: {
          type: 'Text',
          title: localization.tt('lbl.name')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.name'),
          validators: ['required']
        },
        description: {
          type: 'Text',
          title: localization.tt('lbl.description')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.description'),
          validators: ['required']
        },
        type: {
          type: 'Select2',
          title: localization.tt('lbl.deviceType')+'*',
          options: [
            {'val':'','label': '--'},
            {'val':'DEVICE','label': 'DEVICE'}
          ],
          editorClass: 'form-control',
          editorAttrs: {
            disabled: this.readOnlyFlag ? true : false
          },
          pluginAttr: {
            minimumResultsForSearch: Infinity,
            placeholder: localization.tt('lbl.deviceType')
          },
          validators: ['required']
        },
        tags: {
          type: 'Tag',
          title: localization.tt('lbl.tags')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.tags'),
          validators: ['required']
        },
        deviceId: {
          type: 'Text',
          title: localization.tt('lbl.deviceId')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.deviceId'),
          validators: ['required']
        },
        version: {
          type: 'Text',
          title: localization.tt('lbl.version')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.deviceVersion'),
          validators: ['required']
        }
      };
    },

    onRender: function(){
      console.log($el);
    },

    getData: function () {
      var attrs = this.getValue();
      var obj = {
        deviceId: attrs.deviceId,
        version: attrs.version
      };
      attrs.typeConfig = JSON.stringify(obj);
      delete attrs.deviceId;
      delete attrs.version;
      if(this.model.id){
        delete this.model.attributes.deviceId;
        delete this.model.attributes.version;
        delete this.model.attributes.timestamp;
        delete this.model.attributes.id;
      }
      return this.model.set(attrs);
    },

    close: function () {
      console.log('Closing form view');
    }
  });

  return ParserForm;
});