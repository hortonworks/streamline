define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/topology/topologyConfigForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var TopologyConfigForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, options);
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        rootdir: {
          type: 'Text',
          title: 'hbase.rootdir',
          editorClass: 'form-control'
        },
        parserPath: {
          type: 'Text',
          title: 'local.parser.jar.path',
          editorClass: 'form-control'
        },
        notifierPath: {
          type: 'Text',
          title: 'local.notifier.jar.path',
          editorClass: 'form-control'
        },
      };
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

  return TopologyConfigForm;
});