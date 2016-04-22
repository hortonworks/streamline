define(['utils/LangSupport',
  'utils/Globals',
  'utils/Utils',
  'hbs!tmpl/device/deviceForm',
  'collection/VParserList',
  'backbone.forms'
  ], function (localization, Globals, Utils, tmpl, VParserList) {
  'use strict';

  var AddDeviceForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      this.model = options.model;
      this.collection = new VParserList();
      this.getParsers();
      Backbone.Form.prototype.initialize.call(this, options);
    },

    getParsers: function(){
      this.collection.fetch({reset: true, async: false});
    },

    schema: function () {
      var parserArr = [{}],
          feedTypeArr = [{}];
      _.each(this.collection.models, function(model){
        var obj = {
          'val': model.get('id'),
          // 'label': model.get('name')
          'label': model.get('name') + ' (Version - ' + model.get('version') + ')'
        };
        parserArr.push(obj);
      });

      Array.prototype.push.apply(feedTypeArr, Utils.GlobalEnumToArray(Globals.Feed.Type));

      return {
        dataSourceName: {
          type: 'Text',
          title: localization.tt('lbl.deviceName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.deviceName'),
          validators: [{'type':'required','message':'Device name can not be blank.'}]
        },
        description: {
          type: 'Text',
          title: localization.tt('lbl.description')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.description'),
          validators: [{'type':'required','message':'Description can not be blank.'}]
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
        //   validators: [{'type':'required','message':'Classname can not be blank.'}]
        // },
        tags: {
          type: 'Tag',
          title: localization.tt('lbl.tags')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.tags'),
          validators: [{'type':'required','message':'Tag should have atleast one tag.'}]
        },
        deviceId: {
          type: 'Text',
          title: localization.tt('lbl.deviceId')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.deviceId'),
          validators: [{'type':'required','message':'Device id can not be blank.'}]
        },
        version: {
          type: 'Number',
          title: localization.tt('lbl.version')+'*',
          editorClass: 'form-control',
          editorAttrs: {
            min:0
          },
          placeHolder: localization.tt('lbl.deviceVersion'),
          validators: [{'type':'required','message':'Version can not be blank.'}]
        },
        dataFeedName: {
          type: 'Text',
          title: localization.tt('lbl.feedName')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.feedName'),
          validators: [{'type':'required','message':'Feed name can not be blank.'}]
        },
        parserId: {
          type: 'Select2',
          title: localization.tt('lbl.parser')+'*',
          options: parserArr,
          editorClass: 'form-control',
          pluginAttr: {
            placeholder: localization.tt('lbl.parser'),
            allowClear: true,
          },
          validators: [{'type':'required','message':'Parser name can not be blank.'}]
        },
        dataFeedType: {
          type: 'Select2',
          title: localization.tt('lbl.feedType')+'*',
          options: feedTypeArr,
          editorClass: 'form-control',
          pluginAttr: {
            placeholder: localization.tt('lbl.feedType'),
            allowClear: true,
          },
          validators: [{'type':'required','message':'Feed type can not be blank.'}]
        }
      };
    },

    onRender: function(){},

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