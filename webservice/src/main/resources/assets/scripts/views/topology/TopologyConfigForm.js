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
          title: 'hbase.rootdir*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'hbase.rootdir can not be blank.'}]
        },
        parserPath: {
          type: 'Text',
          title: 'local.parser.jar.path*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'local.parser.jar.path can not be blank.'}]
        },
        notifierPath: {
          type: 'Text',
          title: 'local.notifier.jar.path*',
          editorClass: 'form-control',
          validators: [{'type':'required','message':'local.notifier.jar.path can not be blank.'}]
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