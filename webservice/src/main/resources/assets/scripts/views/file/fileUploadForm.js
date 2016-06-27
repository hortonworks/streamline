define([
    'require',
    'hbs!tmpl/file/fileUploadForm',
    'utils/Utils',
    'backbone.forms'
], function(require, tmpl, Utils) {
    'use strict';
    var FileUploadForm = Backbone.Form.extend({
        template: tmpl,

        initialize: function(options) {
            _.extend(this, options);
            Backbone.Form.prototype.initialize.call(this, options);
            if(this.model.has('id')){
                this.model.set('fileJar', this.model.get('storedFileName'))
            }
        },
        schema: function() {
            return {
                name: {
                    type: 'Text',
                    title: 'Name*',
                    editorClass: 'form-control',
                    placeHolder: 'Name',
                    validators: [{ 'type': 'required', 'message': 'Name can not be blank.' }]
                },
                fileJar: {
                    type: 'Fileupload',
                    title: 'Upload jar*',
                    placeHolder: '',
                    validators: [{ 'type': 'required', 'message': 'Select particular jar to upload.' }]
                },
                version: {
                    type: 'Number',
                    title: 'Version*',
                    editorClass: 'form-control',
                    editorAttrs: {
                        min: 0
                    },
                    placeHolder: 'Version',
                    validators: [{ 'type': 'required', 'message': 'Version can not be blank.' }]
                },
            };
        },

        onRender: function() {
        },

        getData: function() {
          return this.getValue();
        },

        close: function() {}
    });

    return FileUploadForm;
});
