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
      if(_.isEqual(this.type, Globals.Topology.Editor.Steps.DataSink.Substeps[0].valStr)){
        //HDFS TYPE
        this.schemaObj = this.generateHdfsSchema();
        this.templateData = {
          fieldName: ['fsURL','path','name']
        };
      } else if(_.isEqual(this.type, Globals.Topology.Editor.Steps.DataSink.Substeps[1].valStr)){
        //HBASE TYPE
        this.schemaObj = this.generateHbaseSchema();
        this.templateData = {
          fieldName: ['rootDir','table','columnFamily', 'rowKey']
        };
      }
      Backbone.Form.prototype.initialize.call(this, options);
    },

    generateHdfsSchema: function(){
      return {
        fsURL: {
          type: 'Text',
          title: localization.tt('lbl.fsURL')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.fsURL'),
          validators: [{'type':'required','message':'fsURL can not be blank.'}]
        },
        path: {
          type: 'Text',
          title: localization.tt('lbl.path')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.path'),
          validators: [{'type':'required','message':'Path can not be blank.'}]
        },
        name: {
          type: 'Text',
          title: localization.tt('lbl.name')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.name'),
          validators: [{'type':'required','message':'Name can not be blank.'}]
        }
      };
    },

    generateHbaseSchema: function(){
      return {
        rootDir: {
          type: 'Text',
          title: localization.tt('lbl.rootDir')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.rootDir'),
          validators: [{'type':'required','message':'Field can not be blank.'}]
        },
        table: {
          type: 'Text',
          title: localization.tt('lbl.table')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.table'),
          validators: [{'type':'required','message':'Field can not be blank.'}]
        },
        columnFamily: {
          type: 'Text',
          title: localization.tt('lbl.columnFamily')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.name'),
          validators: [{'type':'required','message':'Field can not be blank.'}]
        },
        rowKey: {
          type: 'Text',
          title: localization.tt('lbl.rowKey')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.rowKey'),
          validators: [{'type':'required','message':'Field can not be blank.'}]
        }
      };
    },

    schema: function () {
      return this.schemaObj;
    },

    render: function(options){
      Backbone.Form.prototype.render.call(this,options);
    },

    getData: function() {
      return this.getValue();
    },

    close: function() {
    }
  });

  return DataSinkForm;
});