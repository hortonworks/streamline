define(['utils/LangSupport',
  'hbs!tmpl/config/componentForm',
  'utils/Globals',
  'backbone.forms'
  ], function (localization, tmpl, Globals) {
  'use strict';

  var vComponentForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, _.pick(options, 'model', 'type'));
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      var typeArr = [{'val':'','label': '--'}];
      if(this.type==='STORM'){
        _.each(Globals.Component.Storm, function(obj){
          var tObj = {
            'val': obj.value,
            'label': obj.valStr
          };
          typeArr.push(tObj);
        });
      } else if(this.type==='KAFKA'){
        _.each(Globals.Component.Kafka, function(obj){
          var tObj = {
            'val': obj.value,
            'label': obj.valStr
          };
          typeArr.push(tObj);
        });
      }
      return {
        name: {
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
          title: localization.tt('lbl.type')+'*',
          options: typeArr,
          editorClass: 'form-control',
          pluginAttr: {
            minimumResultsForSearch: Infinity,
            placeholder: localization.tt('lbl.type')
          },
          validators: ['required']
        },
        hosts: {
          type: 'Tag',
          title: localization.tt('lbl.hosts')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.hosts'),
          validators: ['required']
        },
        port: {
          type: 'Number',
          title: localization.tt('lbl.port')+'*',
          editorClass: 'form-control',
          editorAttrs: {
            min: 1
          }, 
          placeHolder: localization.tt('lbl.port'),
          validators: ['required']
        }
      };
    },

    onRender: function(){},

    getData: function () {
      var attrs = this.getValue();
      if(this.model.id){
        delete this.model.attributes.timestamp;
        delete this.model.attributes.clusterId;
      }
      return this.model.set(attrs);
    },

    close: function () {}
  });

  return vComponentForm;
});