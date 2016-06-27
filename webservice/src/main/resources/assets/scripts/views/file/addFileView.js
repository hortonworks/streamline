define([
    'require',
    'hbs!tmpl/file/addFileView',
    'utils/Utils',
    'utils/LangSupport',
    'utils/Globals'
], function(require, tmpl, Utils, localization, Globals) {
    'use strict';
    var AddFileView = Marionette.LayoutView.extend({
        template: tmpl,
        templateHelpers: function() {},
        regions: {
            rForm: '#rForm',
        },

        events: {
            'click #btnCancel': 'evClose',
            'click #btnSave': 'evSave'
        },

        initialize: function(options) {
            _.extend(this, options);
        },
        onRender: function() {
            var self = this;
            require(['views/file/fileUploadForm'], function(FileUploadForm) {
                self.view = new FileUploadForm({
                    model: self.model
                });
                self.rForm.show(self.view);
            });
        },
        evSave: function(e) {
            var errs = this.view.validate();
            if (_.isEmpty(errs)) {
                this.saveFile();
            } else {
                return false;
            }
        },

        saveFile: function() {
            var self = this,
                data = this.view.getData(),
                formData = new FormData(),
                url = Globals.baseURL + '/api/v1/catalog/files',
                obj = {};

            if (!_.isEqual(data.fileJar.name.split('.').pop().toLowerCase(), 'jar')) {
                Utils.notifyError(localization.tt('dialogMsg.invalidFile'));
                return false;
            }
            obj.name = data.name;
            obj.version = data.version;
            if(this.model.get('id'))
                obj.id = this.model.get('id');
            formData.append('file', data.fileJar);
            formData.append('fileInfo', new Blob([JSON.stringify(obj)], {type: 'application/json'}));

            var successCallback = function(response) {
                Utils.notifySuccess('File was added successfully.');
                self.evClose();
            };
            var errorCallback = function(model, response, options) {
                Utils.showError(model, response);
            };
            if(this.model.get('id'))
                Utils.uploadFile(url, formData, successCallback, errorCallback, 'PUT');
            else Utils.uploadFile(url, formData, successCallback, errorCallback);

        },

        evClose: function(e) {
            this.trigger('closeModal');
        }
    });

    return AddFileView;
});
