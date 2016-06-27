define(['require',
    'utils/LangSupport',
    'utils/Utils',
    'utils/Globals',
    'hbs!tmpl/topology/splitProcessorView'
], function(require, localization, Utils, Globals, tmpl) {
    'use strict';

    var SplitProcessorView = Marionette.LayoutView.extend({

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
            require(['views/topology/SplitProcessorConfig'], function(SplitProcessorConfig) {
                self.view = new SplitProcessorConfig({
                    model: self.model,
                    vent: self.vent,
                    sourceConfig: self.sourceConfig,
                    uiname: self.model.get('uiname'),
                    editMode: self.editMode,
                    connectedNodes: self.connectedNodes,
                    showOutputLinks: self.showOutputLinks
                });
                self.rForm.show(self.view);
            });
            if (!self.editMode) {
                self.$("#btnAdd").toggleClass("displayNone");
            }
        },
        evAdd: function(e) {
            var self = this;
            var isValid = this.view.validate();
            if (isValid) {
                this.saveSplitProcessor();
            } else {
                return false;
            }
        },
        saveSplitProcessor: function() {
            var self = this;
            var config = this.view.getConfigData();
            var selectedStreams = this.view.getSelectedStreams();

            this.model.set('selectedStreams', selectedStreams);
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

    return SplitProcessorView;
});
