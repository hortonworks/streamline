define(['utils/LangSupport',
  'utils/Globals',
  'utils/Utils',
  'hbs!tmpl/customProcessor/config-fields-form',
  'backbone.forms'
  ], function (localization, Globals, Utils, tmpl) {
  'use strict';

  var ConfigFieldsFormView = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, options);
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
         name: {
          type: 'Text',
          title: 'Name',
          editorClass: 'form-control',
          validators: [{type: 'required', message: 'Field name is required' }]
         },
         isOptional: {
          type: 'Checkbox',
          title: 'Is Optional',
          validators: []
        },
        type: {
          type: 'Select',
          title: 'Type',
          options: [{'val': 'string', 'label': 'String'},
            {'val': 'number', 'label': 'Number'},
            {'val': 'boolean', 'label': 'Boolean'},
            // {'val': 'object', 'label': 'Object'},
            // {'val': 'array.string', 'label': 'Array.String'},
            // {'val': 'array.number', 'label': 'Array.Number'},
            // {'val': 'array.boolean', 'label': 'Array.Boolean'},
            // {'val': 'array.object', 'label': 'Array.Object'}
          ],
          editorClass: 'form-control',
          placeHolder: '',
          validators: [{type: 'required', message: 'Field type is required' }]
        },
        defaultValue: {
          type: 'Text',
          title: 'Default Value',
          editorClass: 'form-control',
          placeHolder: '',
          validators: []
        },
        isUserInput: {
          type: 'Checkbox',
          title: 'Is User Input',
          editorClass: '',
          validators: []
         },
         tooltip: {
          type: 'Text',
          title: 'Tooltip',
          editorClass: 'form-control',
          validators: [{type: 'required', message: 'Tooltip is required' }]
         }
      };
    },

    onRender: function(){ },

    getData: function () {
       return this.getValue();
    },

    close: function () {}
  });

  return ConfigFieldsFormView;
});