define(['utils/LangSupport',
  'hbs!tmpl/config/clusterForm',
  'backbone.forms'
  ], function (localization, tmpl) {
  'use strict';

  var vClusterForm = Backbone.Form.extend({

    template: tmpl,

    initialize: function (options) {
      _.extend(this, _.pick(options, 'model'));
      Backbone.Form.prototype.initialize.call(this, options);
    },

    schema: function () {
      return {
        name: {
          type: 'Text',
          title: localization.tt('lbl.name')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.name'),
          validators: ['required']
        },
        description: {
          type: 'Text',
          title: localization.tt('lbl.description')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.description'),
          validators: ['required']
        },
        type: {
          type: 'Select2',
          title: localization.tt('lbl.type')+'*',
          options: [
            {'val':'','label': '--'},
            {'val':'STORM','label': 'STORM'},
            {'val':'KAFKA','label': 'KAFKA'}
          ],
          editorClass: 'form-control',
          pluginAttr: {
            minimumResultsForSearch: Infinity,
            placeholder: localization.tt('lbl.type')
          },
          validators: ['required']
        },
        tags: {
          type: 'Tag',
          title: localization.tt('lbl.tags')+'*',
          editorClass: 'form-control',
          placeHolder: localization.tt('lbl.tags'),
          validators: ['required']
        }
      };
    },

    onRender: function(){},

    getData: function () {
      var attrs = this.getValue();
      if(this.model.id){
        delete this.model.attributes.timestamp;
      }
      return this.model.set(attrs);
    },

    close: function () {}
  });

  return vClusterForm;
});