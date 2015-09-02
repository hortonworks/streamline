define(['utils/LangSupport',
  'utils/Globals',
  'hbs!tmpl/common/configurationForm',
  'backbone.forms'
  ], function (localization, Globals, tmpl) {
  'use strict';

  var ConfigForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        nimbusHostPort: {
          type: 'Text',
          title: localization.tt('lbl.nimbusHostPort')+'*',
          editorClass: 'form-control',
          placeHolder: 'Hostname:Port',
          validators: ['required']
        },
        kafkaBrokerHost: {
          type: 'Tag',
          title: localization.tt('lbl.kafkaBrokerHost')+'*',
          editorClass: 'form-control',
          placeHolder: 'Hostname:Port',
          validators: ['required']
        },
        parserJarLocation: {
          type: 'Text',
          title: localization.tt('lbl.parserJarLocation')+'*',
          editorClass: 'form-control',
          placeHolder: 'Jar Location',
          validators: ['required']
        }
      };
    },

    getData: function () {
      return this.getValue();
    },

    close: function () {
      console.log('Closing form view');
    }
  });

  return ConfigForm;
});