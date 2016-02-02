define(['utils/LangSupport',
  'utils/Globals',
  'utils/Utils',
  'hbs!tmpl/clusterConfig/componentForm',
  'backbone.forms'
  ], function (localization, Globals, Utils, tmpl) {
  'use strict';

  var vConfigComponentForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, options);
      if(!this.model.has('id')){
        var obj = {
          name: 'Auto_'+options.type+'_Component',
          description: 'Auto generated description for component'
        };
        this.model.set(obj);
      }
      this.model.set('clusterType', options.type);
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      var typeArr = [],
          Components = Globals.Component;

      if(this.type === 'KAFKA') {
         typeArr = this.filterType(Utils.GlobalEnumToArray(Components.Kafka), this.componentArr);
      } else if(this.type === 'STORM') {
        typeArr = this.filterType(Utils.GlobalEnumToArray(Components.Storm), this.componentArr);
      } else if(this.type === 'HDFS') {
        typeArr = this.filterType(Utils.GlobalEnumToArray(Components.HDFS), this.componentArr);
      }
      return {
         clusterType: {
          type: 'Text',
          title: 'Cluster Type*',   
          editorClass: 'form-control', 
          editorAttrs: {
            disabled: 'disabled'
          },
          validators: ['required']         
         },
         type: {
          type: 'Select',
          title: localization.tt('lbl.componentType')+'*',      
          editorClass: 'form-control',           
          options: typeArr,       
          validators: ['required']
        },
        hosts: {
          type: 'Tag',
          title: localization.tt('lbl.hosts')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.hosts'),
          validators: [{'type':'required','message':'Atleast one host name is required.'}]
        },
        port: {
          type: 'Number',
          title: localization.tt('lbl.port')+'*',
          editorClass: 'form-control',
          editorAttrs: {
            min: 1
          }, 
          placeHolder: localization.tt('lbl.port'),
          validators: [{'type':'required','message':'Port number can not be blank.'}]
        }
      };
    },

    filterType: function(actualArr, componentArr){
      if(componentArr.length){
        _.each(componentArr, function(obj){
          actualArr = actualArr.filter(function(o){return (o.val !== obj.get('type'));});
        });
      }
      if(this.model.has('id')){
        actualArr.push({val: this.model.get('type'), label: this.model.get('type')});
      }
      return actualArr;
    },

    onRender: function(){ },

    getData: function () {
      var attrs = this.getValue();
      delete this.model.attributes.clusterType;
      if(this.model.has('id')){
        delete this.model.attributes.timestamp;
      }
      return this.model.set(attrs);
    },    

    close: function () {}
  });

  return vConfigComponentForm;
});