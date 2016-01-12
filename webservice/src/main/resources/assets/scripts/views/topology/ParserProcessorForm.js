define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/topology/parserProcessorForm',
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
      this.model.set('firstTime', false);
      // this.generateConfigSchema();
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
        self.templateData.fieldName.push(name);
        if(self.model.get('firstTime')){
          self.model.set(name, obj.defaultValue, {silent: true});
        }
      });
      self.model.set('firstTime', false);
    },

    generateSchema: function(){
      return {
        dataSourceName: {
          type: 'Text',
          title: 'Source Name',
          editorClass: 'form-control',
          editorAttrs: {
            readonly: 'readonly'
          },
          placeHolder: 'Source Name',
        },
        parserName: {
          type: 'Text',
          title: 'Parser Name',
          editorClass: 'form-control',
          editorAttrs: {
            readonly: 'readonly'
          },
          placeHolder: 'Parser Name',
        },
        parallelism: {
          type: 'Text',
          title: 'Parallelism',
          editorClass: 'form-control',
          placeHolder: 'Parallelism',
        },
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
      return this.model.set(attrs);
    },

    close: function() {
    }
  });

  return DataSinkForm;
});