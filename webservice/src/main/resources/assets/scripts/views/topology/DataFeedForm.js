define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/topology/dataFeedForm',
  'collection/VDatasourceList',
  'backbone.forms'
  ], function (localization, Globals, tmpl, VDatasourceList) {
  'use strict';

  var DataFeedForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      this.collection = new VDatasourceList();
      Backbone.Form.prototype.initialize.call(this, options);
      this.bindEvents();
    },

    bindEvents: function(){
      var self = this;
      this.on('devices:change', function(form, editor){
        var table = '',
            val = editor.getValue();
        _.each(val, function(name){
          var model = self.collection.where({dataSourceName: name});
          if(model){
            table += "<tr><td>"+model[0].get('dataSourceName')+"</td><td>"+model[0].get('parserId')+"</td></tr>";
          }
        });
        self.$('#table_deviceInfo').html(table);
      });
    },

    schema: function() {
      this.collection.fetch({async: false});
      var deviceArr = [];
      this.collection.each(function(model){
        var obj = {
          'val': model.get('dataSourceName'),
          'label': model.get('dataSourceName')
        };
        deviceArr.push(obj);
      });
      
      return {
        dataFeedName: {
          type: 'Text',
          title: localization.tt('lbl.name')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.name'),
          validators: [{'type':'required','message':'Name can not be blank.'}]
        },
        devices: {
          type: 'MultiSelect',
          title: localization.tt('lbl.selectDevices')+'*',
          options: deviceArr,
          editorClass: 'form-control bs-multiselect',
          editorAttrs: {
            multiple: 'multiple'
          },
          pluginAttr: {
            placeholder: localization.tt('lbl.devices'),
          },
          validators: ['required']
        },
        // parserName: {
        //   type: 'Text',
        //   title: localization.tt('lbl.parserName')+'*',
        //   editorClass: 'form-control',
        //   placeHolder: localization.tt('lbl.parserName'),
        //   validators: [{'type':'required','message':'Parser name can not be blank.'}]
        // },
        // parserId: {
        //   type: 'Number',
        //   title: localization.tt('lbl.parserId')+'*',
        //   editorClass: 'form-control',
        //   placeHolder: localization.tt('lbl.parserId'),
        //   validators: [{'type':'required','message':'Parser id can not be blank.'}]
        // }
      };
    },

    onRender: function(){},

    getData: function() {
      return this.getValue();
    },

    close: function() {
    }
  });

  return DataFeedForm;
});