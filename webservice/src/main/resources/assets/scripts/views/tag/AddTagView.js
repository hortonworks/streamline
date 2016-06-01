define([
    'require',
    'hbs!tmpl/tag/addTagView',
    'models/VTag',
    'utils/Utils'
], function(require, tmpl, VTag, Utils) {
    'use strict';
    var TagView = Marionette.LayoutView.extend({
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
            require(['views/tag/TagForm'], function(TagForm) {
                self.view = new TagForm({
                    model: self.model
                });
                self.rForm.show(self.view);
            });
        },
        evSave: function(e) {
            var errs = this.view.validate();
            if (_.isEmpty(errs)) {
                this.saveTag();
            } else {
                return false;
            }
        },

        saveTag: function() {
            var self = this;
            var model = this.view.getData();
            var newFlag = true;
            if(model.has('id')){
                newFlag = false;
            }
            model.save({},{
                success: function(model, response, options) {
                    if(newFlag){
                        Utils.notifySuccess("Tag is added successfully");
                    } else {
                        Utils.notifySuccess("Tag is updated successfully");
                    }
                    self.evClose();
                },
                error: function(model, response, options) {
                    Utils.showError(model, response);
                    self.evClose();
                }
            });
        },

        evClose: function(e) {
            this.trigger('closeModal');
        }
    });

    return TagView;
});
