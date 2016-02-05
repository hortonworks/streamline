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
      var self = this;
      this.arrObject = [];
      this.schemaObj = {};
      this.notificationFlag = _.isEqual(this.type, 'NOTIFICATION');
      this.templateData = {
        fieldName: [],
        notification: this.notificationFlag
      };
      this.model.set('firstTime', false);
      if(this.notificationFlag){
        this.generateNotificationSchema();
      } else {
        this.generateSchema(this.model.get('config'));
      }
      // this.arrObject.map(function(obj){self.generateSchema(obj.defaultValue);});
      // this.generateSchema(this.arrObject);
      Backbone.Form.prototype.initialize.call(this, options);
    },

    generateSchema: function(configArr){
      var self = this;
      _.each(configArr, function(obj){
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
        if(_.isEqual(obj.type, 'array.object')){
          self.arrObject.push(obj);
          delete self.schemaObj[name];
        } else {
          self.templateData.fieldName.push(name);
        }
        if(!self.model.has(name)){
          self.model.set(name, !_.isNull(obj.defaultValue) ? obj.defaultValue : '', {silent: true});
        }
      });
    },

    generateNotificationSchema: function(){
      var configObj = this.model.get('config'),
          self = this;
      _.each(configObj, function(obj){
        if(!self.model.has(obj.name) && obj.type !== 'array.object'){
          self.model.set(obj.name, !_.isNull(obj.defaultValue) ? obj.defaultValue : '', {silent: true});
        }
        if(obj.type === 'array.object'){
          self.currentObjName = obj.name;
          _.each(obj.defaultValue, function(o){
            if(!self.model.has(o.name)){
              var value = self.model.has(self.currentObjName) ? self.model.get(self.currentObjName)[o.name] : (!_.isNull(o.defaultValue) ? o.defaultValue : '');
              self.model.set(o.name, value, {silent: true});
            }
          });
        }
      });
      this.schemaObj = {
        notifierName: {
          type: 'Text',
          title: 'notifierName*',
          editorClass: 'form-control',
          editorAttrs: {
            disabled: 'disabled'
          }
        },
        jarFileName: {
          type: 'Text',
          title: 'jarFileName*',
          editorClass: 'form-control',
          editorAttrs: {
            disabled: 'disabled'
          }
        },
        className: {
          type: 'Text',
          title: 'className*',
          editorClass: 'form-control',
          editorAttrs: {
            disabled: 'disabled'
          }
        },
        hbaseConfigKey: {
          type: 'Text',
          title: 'hbaseConfigKey',
          editorClass: 'form-control',
          editorAttrs: {
            disabled: 'disabled'
          }
        },
        username: {
          type: 'Text',
          title: 'username*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'username can not be blank.'}]
        },
        password: {
          type: 'Password',
          title: 'password*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'password can not be blank.'}]
        },
        host: {
          type: 'Text',
          title: 'host*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'host can not be blank.'}]
        },
        port: {
          type: 'Number',
          title: 'port*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'port can not be blank.'}]
        },
        ssl: {
          type: 'Radio',
          title: 'ssl',
          options: [{val: 'true', label: 'true'}, {val: 'false', label: 'false'}],
          editorClass: 'inline-element'
        },
        starttls: {
          type: 'Radio',
          title: 'starttls',
          options: [{val: 'true', label: 'true'}, {val: 'false', label: 'false'}],
          editorClass: 'inline-element'
        },
        debug: {
          type: 'Radio',
          title: 'debug',
          options: [{val: 'true', label: 'true'}, {val: 'false', label: 'false'}],
          editorClass: 'inline-element'
        },
        protocol: {
          type: 'Text',
          title: 'protocol',
          editorClass: 'form-control'
        },
        auth: {
          type: 'Radio',
          title: 'auth',
          options: [{val: 'true', label: 'true'}, {val: 'false', label: 'false'}],
          editorClass: 'inline-element'
        },
        from: {
          type: 'Text',
          title: 'from*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'from can not be blank.'},
                      {'type': 'email', 'message': 'Invalid email address'}]
        },
        to: {
          type: 'Text',
          title: 'to*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'to can not be blank.'},
                      {'type': 'email', 'message': 'Invalid email address'}]
        },
        subject: {
          type: 'Text',
          title: 'subject*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'subject can not be blank.'}]
        },
        contentType: {
          type: 'Text',
          title: 'contentType',
          editorClass: 'form-control'
        },
        body: {
          type: 'TextArea',
          title: 'body*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'body can not be blank.'}]
        },
        parallelism: {
          type: 'Number',
          title: 'parallelism',
          editorClass: 'form-control'
        },
      };
      return this.schemaObj;
    },

    schema: function () {
      return this.schemaObj;
    },

    render: function(options){
      Backbone.Form.prototype.render.call(this,options);
      var accordion = this.$('[data-toggle="collapse"]');
      if(accordion.length){
        accordion.on('click', function(e){
          $(e.currentTarget).children('i').toggleClass('fa-caret-right fa-caret-down');
        });
      }
    },

    getData: function() {
      var attrs = this.getValue(),
          self = this;
      if(this.notificationFlag){
        var propertiesObj = {
          username: attrs.username,
          password: attrs.password,
          host: attrs.host,
          port: attrs.port,
          ssl: attrs.ssl,
          starttls: attrs.starttls,
          debug: attrs.debug,
          protocol: attrs.protocol,
          auth: attrs.auth
        };
        var fieldObj = {
          from: attrs.from,
          to: attrs.to,
          subject: attrs.subject,
          contentType: attrs.contentType,
          body: attrs.body
        };
        delete this.model.attributes.username;
        delete this.model.attributes.password;
        delete this.model.attributes.host;
        delete this.model.attributes.port;
        delete this.model.attributes.ssl;
        delete this.model.attributes.starttls;
        delete this.model.attributes.debug;
        delete this.model.attributes.protocol;
        delete this.model.attributes.auth;
        delete this.model.attributes.from;
        delete this.model.attributes.to;
        delete this.model.attributes.subject;
        delete this.model.attributes.contentType;
        delete this.model.attributes.body;
        this.model.set('properties', propertiesObj);
        this.model.set('fieldValues', fieldObj);
        this.model.set('parallelism', attrs.parallelism);
        this.model.set('type', this.type);
        return this.model;
      } else {
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
      }
      attrs.type = this.type;
      return this.model.set(attrs);
    },

    close: function() {
    }
  });

  return DataSinkForm;
});