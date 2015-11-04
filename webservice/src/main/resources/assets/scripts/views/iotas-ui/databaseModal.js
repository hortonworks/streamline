define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'hbs!tmpl/iotas-ui/databaseModal',
  'jsPlumb'
], function(require, vent, localization, tmpl, jsPlumb) {
  'use strict';

  var databseModal = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #saveProperties' : 'saveProperties',
      'click #saveProperties' : 'saveProperties',
    },

    ui: {
      'processor'     : '#processor',
      'action'        : '#action',
      'rule'          : '#rule',
      'formula'       : '#formula',
      'alertOptions'  : '#alertOptions',
      'storeOptions'  : '#storageOptions',
      'properties'     : '#properties',
    },

    regions: {
    },

    initialize: function(collection) {
      this.DataSinks = [];
      this.dsCout = 1;
      this.processors = collection;
      this.storageOptionsProperties = [
                                      {
                                        'name' : 'HBase',
                                        'properties' : [
                                                        'rootDir',
                                                        'table',
                                                        'columnFamily',
                                                        'rowKey'
                                                        ]
                                      },
                                      {
                                        'name' : 'Hive',
                                        'properties' : [
                                                        'Driver',
                                                        'Username',
                                                        'Password'
                                                        ]
                                      },
                                       {
                                        'name' : 'HDFS',
                                        'properties' : [
                                                        'fsUrl',
                                                        'path',
                                                        'name'
                                                        ]
                                      },
                                      {
                                        'name' : 'E-mail',
                                        'properties' : [
                                                        'name',
                                                        'e-mail id'
                                                        ]
                                      },
                                      ]
    },

    onRender:function(){
      var that = this;
      this.ui.action.hide();
      this.ui.rule.hide();
      this.ui.formula.hide();
      this.ui.alertOptions.hide();
      this.ui.storeOptions.hide();
      this.ui.properties.hide();
      if(this.processors){
        var processors = this.processors;
        for (var i = processors.length - 1; i >= 0; i--) {
          this.ui.processor.find('select').append('<option>'+processors[i].id+'</option>');
        };
      }

      this.ui.processor.on('change', function(){
          var tempRules = null;
          for (var i = 0; i < that.processors.length; i++) {
              if(that.processors[i].id == $(this).find('select').val()){
                  tempRules = that.processors[i].rules;
              }
          };
          
          that.ui.rule.show();
          for (var i = tempRules.length - 1; i >= 0; i--) {
              that.ui.rule.find('select').append('<option>'+tempRules[i].id+'</option>');
          };
      });

      this.ui.rule.on('change', function(){
          var tempProcessors = that.processors;
          for (var i = 0; i < tempProcessors.length; i++) {
            if(tempProcessors[i].id == that.ui.processor.find('select').val()){
              var tempRules = tempProcessors[i].rules;
                for (var i = 0;i < tempRules.length; i++) {
                  if(tempRules[i].id == $(this).find('select').val()){
                      that.ui.action.find('input').val(tempRules[i].action);
                      that.ui.action.show();
                      if(tempRules[i].action == 'Notify'){
                        that.ui.alertOptions.show();
                        that.ui.storeOptions.hide();
                      }
                      if(tempRules[i].action == 'Store'){
                        that.ui.storeOptions.show();
                        that.ui.alertOptions.hide();
                      }
                      that.ui.formula.show();
                      that.ui.formula.find('textarea').val(tempRules[i].formula);
                  }
                };
            }
          };
      });

      this.ui.storeOptions.on('change', function(){
        that.ui.alertOptions.hide();
        var opt =$(this).find('select').val();
        var temp =that.storageOptionsProperties;
        for (var i = 0; i < temp.length; i++) {
          if(opt == temp[i].name){
            that.ui.properties.find('tbody tr').remove();
            var properties = that.currentProperties = temp[i].properties;
            for (var i = 0; i < properties.length; i++) {
             that.ui.properties.find('tbody').append('<tr>'+
                                                            '<td>'+properties[i]+'</td>'+
                                                            '<td><input id="'+properties[i]+'" type="text"></input></td>'+
                                                      '</tr>');
            };
            that.ui.properties.show();
          };

        };
      });

      this.ui.alertOptions.on('change', function(){
        that.ui.storeOptions.hide();
        var opt =$(this).find('select').val();
        var temp =that.storageOptionsProperties;
        for (var i = 0; i < temp.length; i++) {
          if(opt == temp[i].name){
            that.ui.properties.find('tbody tr').remove();
            var properties = that.currentProperties = temp[i].properties;
            for (var i = 0; i < properties.length; i++) {
             that.ui.properties.find('tbody').append('<tr>'+
                                                            '<td>'+properties[i]+'</td>'+
                                                            '<td><input id="'+properties[i]+'" type="text"></input></td>'+
                                                      '</tr>');
            };
            that.ui.properties.show();
          };

        };
      });
    },

    saveProperties:function(){
        console.log(this.currentProperties);
        var tempDS = {};
        tempDS.dsId = "dataSink"+this.dsCout++;
        tempDS.processorId = $(this.ui.processor).find('select').val();
        tempDS.rule = $(this.ui.rule).find('select').val();
        tempDS.action = $(this.ui.action).find('input').val();
        tempDS.created = false;
        tempDS.storeOptions = $(this.ui.storeOptions).find('select').val();
        tempDS.alertOptions = $(this.ui.alertOptions).find('select').val();
        tempDS.endPoint = {
                                type : $(this.ui.storeOptions).find('select').val() ? this.ui.storeOptions.find('select').val() : this.ui.alertOptions.find('select').val(),
                                properties : [],
                              };
        for (var i = 0; i < this.currentProperties.length; i++) {
          tempDS.endPoint.properties.push(this.$('#'+this.currentProperties[i]).val());
        };
        this.DataSinks.push(tempDS);
    },

    resetProperties:function(){
      console.log('resetProperties');
    },

    getDataSink:function(sinkId){
      if(this.DataSinks.length){
        var tempDS = this.DataSinks;
        for (var i = 0; i < tempDS.length; i++) {
          if(tempDS[i] == sinkId){
            return tempDS;
            break;
          }
        };
      }
      return {};
    },

  });
  return databseModal;
});