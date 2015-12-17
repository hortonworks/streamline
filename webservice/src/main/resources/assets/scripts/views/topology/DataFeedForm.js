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
      _.extend(this, options);
      this.collection = new VDatasourceList();
      var data = this.generateSchema();
      this.schemaObj = data.schemaObj;
      this.templateData = {
        'fieldName': data.fieldArr
      };
      Backbone.Form.prototype.initialize.call(this, options);
      this.bindEvents();
    },

    bindEvents: function(){
      var self = this;
      this.on('devices:change', function(form, editor){
        self.selectedDeviceArr = [];
        var table = '',
            val = editor.getValue(),
            model;
        if(_.isArray(val)){
          _.each(val, function(name){
            model = self.collection.where({dataSourceName: name});
            table += self.temp(model);
          });
        } else {
          model = self.collection.where({dataSourceName: val});
          table += self.temp(model);
        }
         self.$('#table_deviceInfo').html(table);
         if(this.selectedDeviceArr.length){
           this.$('#showDev').removeClass('displayNone');
         } else {
           this.$('#showDev').addClass('displayNone');
         }
      });
    },

    temp: function(model){
      var self = this;
      if(model.length){
        self.selectedDeviceArr.push({
          datasourceName: model[0].get('dataSourceName'),
          parserName: model[0].get('parserName'),
          feedType: model[0].get('dataFeedType'),
          datasourceId: model[0].get('dataSourceId'),
          parserId: model[0].get('parserId')
        });
        return self.generateTable(model[0].get('dataSourceName'), model[0].get('parserName'));
      }
    },

    schema: function() {
      return this.schemaObj;
    },

    generateSchema: function(){
      var self = this;
      this.collection.fetch({async: false});
      var data = {};
      var deviceArr = [{}];
      this.collection.each(function(model){
        var obj = {
          'val': model.get('dataSourceName'),
          'label': model.get('dataSourceName')
        };
        deviceArr.push(obj);
      });
      
      data.schemaObj = {
        // dataFeedName: {
        //   type: 'Text',
        //   title: localization.tt('lbl.name')+'*',
        //   editorClass: 'form-control',
        //   placeHolder: localization.tt('lbl.name'),
        //   validators: [{'type':'required','message':'Name can not be blank.'}]
        // },
        devices: {
          type: 'Select2',
          title: localization.tt('lbl.selectDevices')+'*',
          options: deviceArr,
          editorClass: 'form-control',
          // editorAttrs: {
          //   multiple: 'multiple',
          //   maximumSelectionLength: 1
          // },
          pluginAttr: {
            placeholder: localization.tt('lbl.devices'),
            allowClear: true,
            // maximumSelectionLength: 1
          },
          validators: ['required']
        }
      };
      data.fieldArr = [];
      _.each(this.model.get('config'), function(obj){
        var name = obj.name;

        data.schemaObj[name] = {
          type: 'Text',
          title: name+(obj.isOptional?'' : '*'),
          editorClass: 'form-control',
          placeholder: name,
          validators: (obj.isOptional ? [] : [{'type':'required','message': name+' can not be blank.'}])
        };
        
        if(typeof obj.defaultValue === 'number') {
          data.schemaObj[name].type = 'Number';
        } else if(_.isArray(obj.defaultValue)){
          data.schemaObj[name].type = 'Tag';
          data.schemaObj[name].getValue = function(values, model){
            if(values === ''){
              return [];
            } else {
              return values.split(',');
            }
          };
        }
        
        data.fieldArr.push(name);
        if(!self.model.has(name)){
          self.model.set(name, obj.defaultValue, {silent: true});
        }
      });
      self.model.set('firstTime', false);
      return data;
    },

    render: function(options){
      var self = this;
      Backbone.Form.prototype.render.call(this, options);
      this.$('.sourceFields').find('.col-sm-3').removeClass('col-sm-3').addClass('col-sm-6');
      this.$('.sourceFields').find('.col-sm-9').removeClass('col-sm-9').addClass('col-sm-6');
      if(this.model.has('_selectedTable')){
        self.selectedDeviceArr = this.model.get('_selectedTable');
        var table = '';
        _.each(this.selectedDeviceArr, function(obj){
          table = self.generateTable(obj.datasourceName, obj.parserName);
        });
         this.$('#table_deviceInfo').html(table);
         if(this.selectedDeviceArr.length){
           this.$('#showDev').removeClass('displayNone');
         } else {
           this.$('#showDev').addClass('displayNone');
         }
      }
    },

    generateTable: function(datasourceName, parserName){
      return "<tr><td>"+datasourceName+"</td><td>"+parserName+"</td></tr>";
    },

    getData: function() {
      var attrs = this.getValue();
      var configArr = this.model.get('config');
      for(var key in attrs){
        var obj = _.find(configArr, {name: key});
        if(obj){
          if(_.isEqual(attrs[key], obj.defaultValue)){
            delete attrs[key];
            delete this.model.attributes[key];
          } else if(typeof obj.defaultValue === 'boolean'){
            if(_.isEqual(attrs[key], obj.defaultValue.toString())){
              delete attrs[key];
              delete this.model.attributes[key];
            }
          }
        }
      }
      attrs._selectedTable = this.selectedDeviceArr;
      return this.model.set(attrs);
    },

    close: function() {
    }
  });

  return DataFeedForm;
});