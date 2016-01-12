define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/device/deviceForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var AddDeviceForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      this.model = options.model;
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        dataSourceName: {
          type: 'Text',
          title: localization.tt('lbl.deviceName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.deviceName'),
          validators: ['required']
        },
        description: {
          type: 'Text',
          title: localization.tt('lbl.description')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.description'),
          validators: ['required']
        },
        // type: {
        //   type: 'Select2',
        //   title: localization.tt('lbl.deviceType')+'*',
        //   options: [
        //     {'val':'','label': '--'},
        //     {'val':'DEVICE','label': 'DEVICE'}
        //   ],
        //   editorClass: 'form-control',
        //   editorAttrs: {
        //     disabled: this.readOnlyFlag ? true : false
        //   },
        //   pluginAttr: {
        //     minimumResultsForSearch: Infinity,
        //     placeholder: localization.tt('lbl.deviceType')
        //   },
        //   validators: ['required']
        // },
        tags: {
          type: 'Tag',
          title: localization.tt('lbl.tags')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.tags'),
          validators: ['required']
        },
        id: {
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
        },
        dataFeedName: {
          type: 'Text',
          title: localization.tt('lbl.feedName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.feedName'),
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
        dataFeedType: {
          type: 'Text',
          title: localization.tt('lbl.feedType')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.feedType'),
          validators: ['required']
        }
      };
    },

    onRender: function(){},

    getData: function () {
      var attrs = this.getValue();
      var obj = {
        id: attrs.id,
        version: attrs.version
      };
      attrs.typeConfig = JSON.stringify(obj);
      delete attrs.id;
      delete attrs.version;
      if(this.model.id){
        delete this.model.attributes.id;
        delete this.model.attributes.version;
        delete this.model.attributes.timestamp;
      }
      if(this.model.has('entity')){
        delete this.model.attributes.entity;
        delete this.model.attributes.responseCode;
        delete this.model.attributes.responseMessage;
      }
      return this.model.set(attrs);
    },

    close: function () {
      console.log('Closing form view');
    }
  });

  return AddDeviceForm;
});