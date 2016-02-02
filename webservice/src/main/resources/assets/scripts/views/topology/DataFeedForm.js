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
        // 'fieldName': data.fieldArr
        'reqFieldName': data.reqFieldArr,
        'addFieldRows': data.addFieldRows
      };
      if(!this.model.has('devices') && this.model.has('_selectedTable')){
        this.setDevices();
      }
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
        devices: {
          type: 'Select2',
          title: localization.tt('lbl.selectDevices')+'*',
          options: deviceArr,
          editorClass: 'form-control',
          pluginAttr: {
            placeholder: localization.tt('lbl.devices'),
            allowClear: true,
          },
          validators: ['required']
        }
      };
      // data.fieldArr = [];
      data.reqFieldArr = [];
      data.addFieldRows = [];
      var fieldRow = [];
      _.each(this.model.get('config'), function(obj, i){
        var name = obj.name;

        data.schemaObj[name] = {
          type: 'Text',
          title: name+(obj.isOptional?'' : '*'),
          editorClass: 'form-control',
          placeholder: name,
          validators: (obj.isOptional ? [] : [{'type':'required','message': name+' can not be blank.'}])
        };
        
        if(_.isEqual(obj.type, 'number')) {
          data.schemaObj[name].type = 'Number';
        } else if(_.isEqual(obj.type, 'array.string')){
          data.schemaObj[name].type = 'Tag';
          data.schemaObj[name].getValue = function(values, model){
            if(values === ''){
              return [];
            } else {
              return values.split(',');
            }
          };
        } else if(_.isEqual(obj.type, 'boolean')){
          data.schemaObj[name].type = 'Radio';
          data.schemaObj[name].options = [{val: 'true', label: 'true'}, {val: 'false', label: 'false'}];
          data.schemaObj[name].editorClass = 'inline-element';
        }
        
        // data.fieldArr.push(name);
        fieldRow.push(name);
        if(!self.model.has(name)){
          self.model.set(name, !_.isNull(obj.defaultValue) ? obj.defaultValue : '', {silent: true});
        }
        if (i % 2 !== 0) {
          if (i < 4) {
            data.reqFieldArr.push(fieldRow);
            fieldRow = [];
          } else {
            data.addFieldRows.push(fieldRow);
            fieldRow = [];
          }
        }
      });
      self.model.set('firstTime', false);
      return data;
    },

    render: function(options){
      var self = this;
      Backbone.Form.prototype.render.call(this, options);
      // this.$('.sourceFields').find('.col-sm-3').removeClass('col-sm-3').addClass('col-sm-6');
      this.$el.find('.col-sm-9').removeClass('col-sm-9').addClass('col-sm-6');
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
      var accordion = this.$('[data-toggle="collapse"]');
      if(accordion.length){
        accordion.on('click', function(e){
          $(e.currentTarget).children('i').toggleClass('fa-caret-right fa-caret-down');
        });
      }
    },

    setDevices: function(){
      var self = this;
      var arr = [];
      _.each(this.model.get('_selectedTable'), function(obj){
        var tempModel = self.collection.get(obj.dataSourceId);
        self.model.set('devices', tempModel.get('dataSourceName'));
        arr.push({
          datasourceName: tempModel.get('dataSourceName'),
          parserName: tempModel.get('parserName'),
          feedType: tempModel.get('dataFeedType'),
          datasourceId: tempModel.get('dataSourceId'),
          parserId: tempModel.get('parserId')
        });
      });
      this.model.set('_selectedTable', arr);
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