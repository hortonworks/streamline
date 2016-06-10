define(['require',
    'utils/LangSupport',
    'utils/Utils',
    'utils/Globals',
    'hbs!tmpl/topology/normalizationProcessorView'
], function(require, localization, Utils, Globals, tmpl) {
    'use strict';

    var NormalizationProcessorView = Marionette.LayoutView.extend({

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
            var data = this.model.toJSON();
            var newModel = new Backbone.Model(JSON.parse(JSON.stringify(data)));
            require(['views/topology/NormalizationProcessorConfig'], function(NormalizationProcessorConfig) {
                self.view = new NormalizationProcessorConfig({
                    model: newModel,
                    vent: self.vent,
                    sourceConfig: self.sourceConfig,
                    uiname: self.model.get('uiname'),
                    streamId: self.streamId,
                    editMode: self.editMode,
                    dsId: self.dsId
                });
                self.rForm.show(self.view);
            });
            if(!self.editMode) {
            self.$("#btnAdd").toggleClass("displayNone");
          }
        },
        evAdd: function(e) {
            var self = this;
            var err = this.view.validate();
            if (_.isEmpty(err)) {
                this.saveNormalizationProcessor();
            } else {
                _.each(err, function(msg) {
                    Utils.notifyError(msg);
                });
                return false;
            }
        },
        saveNormalizationProcessor: function() {
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

    return NormalizationProcessorView;
});
