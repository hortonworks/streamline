define(['utils/LangSupport',
    'utils/Globals',
    'utils/Utils',
    'hbs!tmpl/topology/stageProcessorConfig',
    'bootstrap-switch'
], function(localization, Globals, Utils, tmpl) {
    'use strict';

    var StageProcessorConfig = Marionette.LayoutView.extend({

        template: tmpl,

        events: {
            'change .select-transform': 'evSelectTransformType',
            'change .select-fields': 'evSelectFields',
            'click #outputBtn': 'evValidateOutputSchema'
        },

        templateHelpers: function() {
            return {
                editMode: this.editMode
            };
        },

        initialize: function(options) {
            _.extend(this, options);
            var self = this;
        },

        onRender: function() {

            var self = this,
                accordion = this.$('[data-toggle="collapse"]');
            if (accordion.length) {
                accordion.on('click', function(e) {
                    $(e.currentTarget).children('i').toggleClass('fa-caret-right fa-caret-down');
                });
            }

            this.sampleSchema = {
                "fields": [{
                    "name": "childField1",
                    "type": "INTEGER"
                }, {
                    "name": "childField2",
                    "type": "BOOLEAN"
                }, {
                    "name": "topLevelStringField",
                    "type": "STRING"
                }]
            };

            var inputStream = this.sourceConfig.newConfig ? this.sourceConfig.newConfig.rulesProcessorConfig.rules[0].actions[0].outputStreams : this.sourceConfig.rulesProcessorConfig.rules[0].actions[0].outputStreams,
                selectedStream = _.find(this.sourceConfig.selectedStreams, function(o){return self.model.get('uiname') == o.name}),
                streamName = selectedStream ? selectedStream.streamName : null,
                fields = streamName ? inputStream[streamName].fields : [];

            _.each(fields, function(obj) {
                this.$(".select-fields").append('<option>' + obj.name + '</option>');
            }, this);

            this.declaredInput = _.extend([], fields);

            this.$(".select-fields").select2();
            this.$('.editable').editable({
                mode: 'popup',
                placement: 'right',
                disabled: !this.editMode
            });

            var rulesProcessorConfig;
            if (this.model.has('newConfig')) {
                rulesProcessorConfig = this.model.get('newConfig').rulesProcessorConfig;
            } else if (this.model.has('rulesProcessorConfig')) {
                rulesProcessorConfig = this.model.get('rulesProcessorConfig');
            }

            if (rulesProcessorConfig) {
                var config = rulesProcessorConfig.rules[0].actions[0],
                    outputStreams = config.outputStreams,
                    transformObj = config.transforms[0],
                    parallelism = this.model.get('newConfig') ? this.model.get('newConfig').parallelism : (this.model.get('parallelism') ? this.model.get('parallelism') : 1),
                    streamName = _.keys(outputStreams)[0];
                this.$('.schema-tab > a > span').html(streamName);
                this.$('.outputSource').val(JSON.stringify(outputStreams[streamName], null, "  "));

                if (transformObj.name == 'enrichment') {
                    this.$(".select-transform").val('Enrichment');
                    this.$(".select-fields").select2('val', transformObj.enrichmentFields);
                } else {
                    this.$(".select-transform").val('Projection');
                    this.$(".select-fields").select2('val', transformObj.projectionFields);
                }
                this.$(".parallelism").val(parallelism);
            } else {
                this.$('.outputSource').val(JSON.stringify(this.sampleSchema, null, "  "));
                this.$(".parallelism").val(1);
            }

            if (!this.editMode) {
                this.$(".select-transform, .select-fields, .parallelism, .outputSource").prop("disabled", true);
            }
        },

        evSelectTransformType: function(e) {
            var transformType = $(e.currentTarget).val();
            if (transformType == 'Enrichment') {
                //this.$(".transform-fields-label").html('Enrichment Fields');
            } else {
                //this.$(".transform-fields-label").html('Projection Fields');
            }
        },

        evValidateOutputSchema: function() {
            try {
                var result = jsonlint.parse(this.$(".outputSource").val());
                if (result) {
                    this.$(".outputResult").html("JSON is valid!").removeClass("alert alert-danger").addClass("alert alert-success");
                    this.$(".outputSource").val(JSON.stringify(result, null, "  "));
                }
                return true;
            } catch (e) {
                this.$(".outputResult").html(e.message).addClass("alert alert-danger");
                return false;
            }
        },

        validate: function() {
            var isValid = true;
            this.$("[data-error]").remove();

            if (!this.$(".select-fields").val()) {
                isValid = false;
                this.$(".select-fields-holder").append('<div data-error>Fields cannot be empty.</div>');
            }

            if (!this.evValidateOutputSchema()) {
                isValid = false;
            }
            return isValid;
        },

        getConfigData: function() {
            var transformType = this.$(".select-transform").val(),
                actions = [],
                parallelism = this.$(".parallelism").val() ? this.$(".parallelism").val() : 1,
                fields = this.$(".select-fields").val();


            if (transformType == 'Projection') {
                actions.push({
                    __type: "com.hortonworks.iotas.layout.design.splitjoin.StageAction",
                    name: "stage-action",
                    outputStreams: null,
                    transforms: [{
                        __type: "com.hortonworks.iotas.layout.design.transform.ProjectionTransform",
                        name: "projection",
                        projectionFields: fields
                    }]
                });
            } else {
                actions.push({
                    __type: "com.hortonworks.iotas.layout.design.splitjoin.StageAction",
                    name: "stage-action",
                    outputStreams: null,
                    transforms: [{
                        __type: "com.hortonworks.iotas.layout.design.transform.EnrichmentTransform",
                        name: "enrichment",
                        enrichmentFields: fields
                    }]
                });
            }

            var obj = {},
                streamName = $('.schema-tab > a > span').html(),
                streamData = jsonlint.parse(this.$(".outputSource").val());
            obj[streamName] = streamData;

            actions[0].outputStreams = obj;

            var config = {
                parallelism: parallelism,
                rulesProcessorConfig: {
                    rules: [{
                        name: "rule",
                        id: new Date().getTime() + 1,
                        ruleProcessorName: this.model.get('uiname'),
                        condition: null,
                        actions: actions,
                        description: "Auto-Generated for RULE"
                    }],
                    name: this.model.get('uiname'),
                    id: new Date().getTime(),
                    declaredInput: this.declaredInput,
                    config: ""
                }
            };

            return config;
        }

    });

    return StageProcessorConfig;
});
