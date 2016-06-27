define(['utils/LangSupport',
    'utils/Globals',
    'utils/Utils',
    'hbs!tmpl/topology/joinProcessorConfig',
    'collection/VFileList',
    'bootstrap-switch'
], function(localization, Globals, Utils, tmpl, VFileList) {
    'use strict';

    var JoinProcessorConfig = Marionette.LayoutView.extend({

        template: tmpl,

        events: {
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

            this.filesCollection = new Backbone.Collection();

            var fileCollection = new VFileList();
            fileCollection.fetch({
                async: false,
                success: function(collection) {
                    self.filesCollection.set(collection.models);
                }
            });
        },

        onRender: function() {
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

            var accordion = this.$('[data-toggle="collapse"]');
            if (accordion.length) {
                accordion.on('click', function(e) {
                    $(e.currentTarget).children('i').toggleClass('fa-caret-right fa-caret-down');
                });
            }

            this.$(".selectJar").html('<option></opiton>');
            if(this.filesCollection.models.length == 0)
                this.$(".select-jar-holder").append('<div data-error>No files found.</div>');
            _.each(this.filesCollection.models, function(model) {
                this.$(".selectJar").append('<option>' + model.get('name') + '</option>');
            }, this);

            this.$(".selectJar").select2({
                placeholder: "Select Jar",
                allowClear: true,
                minimumResultsForSearch: Infinity
            });

            this.declaredInput = [];
            for(var i = 0;i < this.sourceConfig.length;i++) {
                var inputStream = this.sourceConfig[i].newConfig ? this.sourceConfig[i].newConfig.rulesProcessorConfig.rules[0].actions[0].outputStreams : this.sourceConfig[i].rulesProcessorConfig.rules[0].actions[0].outputStreams,
                    streamName = _.keys(inputStream)[0],
                    fields = inputStream[streamName].fields;
                    this.declaredInput = _.union(this.declaredInput, fields);
            }

            var rulesProcessorConfig;
            if (this.model.has('newConfig')) {
                rulesProcessorConfig = this.model.get('newConfig').rulesProcessorConfig;
            } else if(this.model.has('rulesProcessorConfig')) {
                rulesProcessorConfig = this.model.get('rulesProcessorConfig');
            }

            if (rulesProcessorConfig) {
                var config = rulesProcessorConfig.rules[0].actions[0],
                    parallelism = this.model.get('newConfig') ? this.model.get('newConfig').parallelism : (this.model.get('parallelism') ? this.model.get('parallelism') : 1),
                    jarId = config.jarId,
                    outputStreams = config.outputStreams,
                    joinerClassName = config.joinerClassName;

                this.$(".parallelism").val(parallelism);
                this.$(".joiner-class").val(joinerClassName);

                var fileModel = _.find(this.filesCollection.models, function(model) {
                    return model.get('id') == jarId;
                });
                if(fileModel)
                    this.$(".selectJar").select2('val', fileModel.get('name'));
                if (config.eventExpiryInterval)
                    this.$(".event-expiry-interval").val(config.eventExpiryInterval);
                if (config.groupExpiryInterval)
                    this.$(".group-expiry-interval").val(config.groupExpiryInterval);
                var streamName = _.keys(outputStreams)[0];
                    this.$('.schema-tab > a > span').html(streamName);
                    this.$('.outputSource').val(JSON.stringify(outputStreams[streamName], null, "  "));

            } else {
                this.$('.outputSource').val(JSON.stringify(this.sampleSchema, null, "  "));
                this.$(".parallelism").val(1);
            }

            this.$('.editable').editable({
                mode: 'popup',
                disabled : !this.editMode
            });

            if (!this.editMode) {
                this.$(".selectJar, .joiner-class, .parallelism, .group-expiry-interval, .event-expiry-interval, .outputSource").prop("disabled", true);
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

            if (this.$(".joiner-class").val() == '') {
                isValid = false;
                this.$(".joiner-class-holder").append('<div data-error>Class cannot be blank.</div>');
            }
            if (this.$(".selectJar").val() == '') {
                isValid = false;
                this.$(".select-jar-holder").append('<div data-error>Select a jar name.</div>');
            }
            if (!this.evValidateOutputSchema()) {
                isValid = false;
            }
            return isValid;
        },

        getConfigData: function() {
            var jarName = this.$(".selectJar").val(),
                actions = [{
                    __type: "com.hortonworks.iotas.layout.design.splitjoin.JoinAction",
                    name: "join-action",
                    outputStreams: null,
                    jarId: '',
                    joinerClassName: '',
                    groupExpiryInterval: null,
                    eventExpiryInterval: null
                }],
                parallelism = this.$(".parallelism").val() ? this.$(".parallelism").val() : 1;


            var obj = {},
                streamName = $('.schema-tab > a > span').html(),
                streamData = jsonlint.parse(this.$(".outputSource").val());
            obj[streamName] = streamData;

            actions[0].outputStreams = obj;
            actions[0].joinerClassName = this.$(".joiner-class").val();
            actions[0].groupExpiryInterval = this.$(".group-expiry-interval").val() ? this.$(".group-expiry-interval").val() : null;
            actions[0].eventExpiryInterval = this.$(".event-expiry-interval").val() ? this.$(".event-expiry-interval").val() : null;

            var fileModel = _.find(this.filesCollection.models, function(model) {
                return model.get('name') == jarName;
            });
            actions[0].jarId = fileModel.get('id');

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

    return JoinProcessorConfig;
});
