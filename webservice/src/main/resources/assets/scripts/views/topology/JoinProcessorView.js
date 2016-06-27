define(['require',
    'utils/LangSupport',
    'utils/Utils',
    'utils/Globals',
    'hbs!tmpl/topology/joinProcessorView'
], function(require, localization, Utils, Globals, tmpl) {
    'use strict';

    var JoinProcessorView = Marionette.LayoutView.extend({

        template: tmpl,

        templateHelpers: function() {
            var self = this;
        },

        events: {
            'click #btnCancel': 'evClose',
            'click #btnAdd': 'evAdd'
        },

        regions: {
            rForm: '#rForm',
        },

        initialize: function(options) {
            _.extend(this, options);
        },

        onRender: function() {
            var self = this;
            require(['views/topology/JoinProcessorConfig'], function(JoinProcessorConfig) {
                self.view = new JoinProcessorConfig({
                    model: self.model,
                    vent: self.vent,
                    sourceConfig: self.sourceConfig,
                    uiname: self.model.get('uiname'),
                    editMode: self.editMode
                });
                self.rForm.show(self.view);
            });
            if(!self.editMode) {
            self.$("#btnAdd").toggleClass("displayNone");
          }
        },
        evAdd: function(e) {
            var self = this;
            var isValid = this.view.validate();
            if (isValid) {
                this.saveJoinProcessor();
            } else {
                return false;
            }
        },
        saveJoinProcessor: function() {
            var self = this;
            var config = this.view.getConfigData();
            this.model.set('firstTime', false);
            this.model.set("newConfig", config);
            this.vent.trigger('topologyEditor:SaveProcessor', this.model);
            this.evClose();
        },
        evClose: function(e) {
            this.view = null;
            this.trigger('closeModal');
        }

    });

    return JoinProcessorView;
});
