define([
    'require',
    'hbs!tmpl/tag/tagForm',
    'backbone.forms'
], function(require, tmpl) {
    'use strict';
    var TagForm = Backbone.Form.extend({
        template: tmpl,

        initialize: function(options) {
            _.extend(this, options);
            Backbone.Form.prototype.initialize.call(this, options);
        },
        schema: function() {
            return {
                name: {
                    type: 'Text',
                    title: 'Name*',
                    editorClass: 'form-control',
                    validators: ['required']
                },
                description: {
                    type: 'Text',
                    title: 'Description*',
                    editorClass: 'form-control',
                    validators: ['required']
                }
            };
        },
        onRender: function() {},
        getData: function() {
            var attrs = this.getValue();
            if (this.model.has('id')) {
                delete this.model.attributes.timestamp;
            }
            return this.model.set(attrs);
        },
        close: function() {}
    });

    return TagForm;
});
