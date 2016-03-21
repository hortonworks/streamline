define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/topology/customProcessorForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var CustomProcessorForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, options);
      this.schemaObj = [];
      this.templateData = {
        'fieldName': [],
      };
      this.model.set('firstTime', false);
      this.generateSchema(this.model.get('config'));
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function() {
      return this.schemaObj;
    },

    generateSchema: function(configArr){
      var self = this;
      _.each(configArr, function(obj){
        if(obj.isUserInput){
          var name = obj.name;
        
          self.schemaObj[name] = {
            type: 'Text',
            title: name+(obj.isOptional?'' : '*'),
            editorClass: 'form-control',
            editorAttrs: self.editMode ? {} : {disabled: 'disabled'},
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
          if(_.isEqual(obj.type, 'array.object')){
            self.arrObject.push(obj);
            delete self.schemaObj[name];
          } else {
            self.templateData.fieldName.push(name);
          }
          if(!self.model.has(name)){
            self.model.set(name, !_.isNull(obj.defaultValue) ? obj.defaultValue : '', {silent: true});
          }
        }
      });
    },

    render: function(options){
      var self = this;
      Backbone.Form.prototype.render.call(this, options);
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
      return this.model.set(attrs);
    },

    close: function() {
    }
  });

  return CustomProcessorForm;
});