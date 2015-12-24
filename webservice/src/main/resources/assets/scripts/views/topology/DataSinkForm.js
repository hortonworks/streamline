define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/topology/dataSinkForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var DataSinkForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, options);
      this.schemaObj = this.generateSchema();
      this.templateData = {
        fieldName: []
      };
      this.generateConfigSchema();
      Backbone.Form.prototype.initialize.call(this, options);
    },

    generateConfigSchema: function(){
      var self = this;
      _.each(this.model.get('config'), function(obj){
        var name = obj.name;
        
        self.schemaObj[name] = {
          type: 'Text',
          title: name+(obj.isOptional?'' : '*'),
          editorClass: 'form-control',
          placeholder: name,
          validators: (obj.isOptional ? [] : [{'type':'required','message': name+' can not be blank.'}])
        };
        if(_.isEqual(obj.type, 'number')) {
          self.schemaObj[name].type = 'Number';
        } else if(_.isEqual(obj.type, 'array.string')){
          self.schemaObj[name].type = 'Tag';
          self.schemaObj[name].getValue = function(values, model){
            if(values === ''){
              return [];
            } else {
              return values.split(',');
            }
          };
        } else if(_.isEqual(obj.type, 'boolean')){
          self.schemaObj[name].type = 'Radio';
          self.schemaObj[name].options = [{val: 'true', label: 'true'}, {val: 'false', label: 'false'}];
          self.schemaObj[name].editorClass = 'inline-element';
        }
        self.templateData.fieldName.push(name);
        if(!self.model.has(name)){
          self.model.set(name,  (obj.defaultValue) ? obj.defaultValue : '', {silent: true});
        }
      });
      self.model.set('firstTime', false);
    },

    generateSchema: function(){
      return {
        // name: {
        //   type: 'Text',
        //   title: localization.tt('lbl.sinkName')+'*',
        //   editorClass: 'form-control',
        //   placeHolder: localization.tt('lbl.name'),
        //   validators: [{'type':'required','message':'Name can not be blank.'}]
        // }
      };
    },

    schema: function () {
      return this.schemaObj;
    },

    render: function(options){
      Backbone.Form.prototype.render.call(this,options);
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
      attrs.type = this.type;
      return this.model.set(attrs);
    },

    close: function() {
    }
  });

  return DataSinkForm;
});