define(['utils/LangSupport',
    'utils/Globals',
    'utils/Utils',
    'hbs!tmpl/topology/splitProcessorConfig',
    'models/VParser',
    'models/VDatasource',
    'collection/VFileList',
    'bootstrap-switch'
], function(localization, Globals, Utils, tmpl, VParser, VDatasource, VFileList) {
    'use strict';

    var SplitProcessorConfig = Marionette.LayoutView.extend({

        template: tmpl,

        events: {
            'click #add-tab': 'evAddTab'
        },

        templateHelpers: function() {
            return {
                editMode: this.editMode
            }
        },

        initialize: function(options) {
            _.extend(this, options);
            var self = this;

            this.tabIdCount = 1;
            this.currentTabId = 1;
            this.tabNumbers = [1];
            this.streamNames = [];
            this.filesCollection = new Backbone.Collection();

            var fileCollection = new VFileList();
            fileCollection.fetch({
                async: false,
                success: function(collection) {
                    self.filesCollection.set(collection.models);
                }
            });

            switch (this.sourceConfig.currentType) {
                case 'PARSER':
                    var id = self.dsId || self.sourceConfig.dataSourceId || self.sourceConfig._dataSourceId || self.model.get('dataSourceId');
                    self.declaredInput = self.getFeedSchema(id);
                    break;
                case 'RULE':
                    self.declaredInput = _.extend([], (self.sourceConfig.newConfig ? self.sourceConfig.newConfig.rulesProcessorConfig.declaredInput : self.sourceConfig.rulesProcessorConfig.declaredInput));
                    break;
                case 'CUSTOM':
                    var selectedStreams = _.where(self.sourceConfig.selectedStreams, { name: self.uiname }),
                        streamNames = _.map(selectedStreams, function(obj) {
                            return obj.streamName;
                        }),
                        inputStreams = _.where(self.sourceConfig.config, { name: "outputStreamToSchema" })[0].defaultValue;
                    self.inputStreams = _.pick(inputStreams, streamNames);

                    self.declaredInput = [];
                    var id = 0;
                    for (var i = 0; i < streamNames.length; i++) {
                        var fields = self.inputStreams[streamNames[i]].fields;
                        for (var j = 0; j < fields.length; j++) {
                            self.declaredInput.push({
                                name: fields[j].name,
                                optional: fields[j].optional,
                                type: fields[j].type
                            });
                        }
                    }
                    break;
                case 'NORMALIZATION':
                    self.declaredInput = _.extend([], (self.sourceConfig.newConfig ? self.sourceConfig.newConfig.normalizationProcessorConfig.outputStreams[0].schema.fields : self.sourceConfig.normalizationProcessorConfig.outputStreams[0].schema.fields));
                    break;
            }


        },

        getFeedSchema: function(dsId) {
            var self = this,
                fieldsArr = [],
                parserModel = new VParser();

            var dsModel = new VDatasource();
            dsModel.set('dataSourceId', dsId);
            dsModel.set('id', dsId);
            dsModel.fetch({ async: false });

            parserModel.getSchema({
                parserId: dsModel.get('entity').parserId,
                async: false,
                success: function(model, response, options) {
                    fieldsArr = model.entity.fields;
                },
                error: function(model, response, options) {
                    Utils.showError(model, response);
                }
            });
            return fieldsArr;
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
            if (this.filesCollection.models.length == 0)
                this.$(".select-jar-holder").append('<div data-error>No files found.</div>');
            _.each(this.filesCollection.models, function(model) {
                this.$(".selectJar").append('<option>' + model.get('name') + '</option>');
            }, this);

            this.$(".selectJar").select2({
                placeholder: "Select Jar",
                allowClear: true,
                minimumResultsForSearch: Infinity
            });

            this.$('.schema-tab[data-id="' + (this.tabIdCount - 1) + '"]').children('i').show();

            var rulesProcessorConfig;
            if (this.model.has('newConfig')) {
                rulesProcessorConfig = this.model.get('newConfig').rulesProcessorConfig;
            } else if (this.model.has('rulesProcessorConfig')) {
                rulesProcessorConfig = this.model.get('rulesProcessorConfig');
            }

            if (rulesProcessorConfig) {
                var config = rulesProcessorConfig.rules[0].actions[0],
                    parallelism = this.model.get('newConfig') ? this.model.get('newConfig').parallelism : (this.model.get('parallelism') ? this.model.get('parallelism') : 1),
                    jarId = config.jarId,
                    outputStreams = config.outputStreams,
                    splitterClassName = config.splitterClassName;

                this.$(".parallelism").val(parallelism);
                this.$(".splitter-class").val(splitterClassName);

                var fileModel = _.find(this.filesCollection.models, function(model) {
                    return model.get('id') == jarId;
                });
                if (fileModel)
                    this.$(".selectJar").select2('val', fileModel.get('name'));
                this.renderOutputShema(outputStreams);
            } else {
                this.renderOutputShema();
                this.$(".parallelism").val(1);
            }

            if (!this.editMode) {
                this.$(".selectJar, .splitter-class, .parallelism, .outputSource, .target-nodes").prop("disabled", true);
            }

            this.$(".target-nodes").select2({
                placeholder: "Select Processor",
                allowClear: true
            });

            if (this.showOutputLinks) {
                var selectedStreams = this.model.get('selectedStreams');
                _.each(selectedStreams, function(obj) {
                    this.$('.target-nodes[data-stream="' + obj.streamName + '"]').select2('val', obj.name);
                }, this);
            }
        },

        renderOutputShema: function(outputStreams) {
            var self = this;
            if (outputStreams) {
                var streams = _.keys(outputStreams);
                _.each(streams, function(name) {
                    self.streamNames.push(name);
                    var id = self.tabIdCount++;
                    var schema = outputStreams[name];
                    self.renderTab(name, id);
                    self.$('.outputSource[data-id="' + id + '"]').val(JSON.stringify(schema, null, "  "));
                });
            } else {
                this.tabIdCount++;
                this.renderTab('Stream-1', 1);
                this.$('.outputSource[data-id="1"]').val(JSON.stringify(this.sampleSchema, null, "  "));
            }
        },

        renderTab: function(name, id) {

            this.$("#add-tab").closest('li').before('<li class="schema-tab" data-id="' + id + '"><a href="javascript:void(0);"><span data-id="' + id + '">' + name + '</span></a>' + (this.editMode ? '<i class="fa fa-times-circle cancelSchema" style="display:none;"></i>' : '') + '</li>');
            this.$('.tab-pane[id="tab-' + this.currentTabId + '"]').removeClass("active");
            this.$("#add-tab").closest('ul').find('.active').removeClass('active');
            this.$("#add-tab").closest('ul').find('[data-id="' + id + '"]').addClass('active');
            this.$('.tab-content').append('<div role="tabpanel" class="tab-pane active" id="tab-' + id + '">' +
                '<div class="row">' +
                '<div class="col-sm-7">' +
                '<textarea data-id="' + id + '" rows="10" class="outputSource form-control"></textarea>' +
                '</div>' +
                '<div class="col-sm-5">' +
                (this.editMode ?
                    ('<button class="validateOutput btn btn-block btn-primary row-margin-bottom" id="outputBtn" data-id="' + id + '">' +
                        '<i class="fa fa-chevron-left"></i> Validate JSON' +
                        '</button>') : '') +
                '<div data-id="' + id + '" class="outputResult json-message" role="alert"></div>' +
                '</div>' +
                '</div>' +
                '</div>');
            this.tabNumbers.push(id);
            this.bindClickEvents(id);
            this.currentTabId = id;

            var selectOptions = '<option></opiton>';
            for (var i = 0; i < this.connectedNodes.length; i++) {
                selectOptions += '<option>' + this.connectedNodes[i] + '</option>';
            }
            this.$('.stream-connection-list').append('<div class="row box-body">' +
                '<div class="col-sm-2 pull-left">' +
                '<label class="control-label">' + name + '</label>' +
                '</div>' +
                '<div class="col-sm-3 pull-right">' +
                '<select class="form-control target-nodes" data-stream="' + name + '">' + selectOptions + '</select>' +
                '</div>' +
                '<div class="col-sm-3 pull-right">' +
                '<label class="control-label">connect to:</label>' +
                '</div>' +
                '</div>');
            this.$(".target-nodes").select2({
                placeholder: "Select Processor",
                allowClear: true
            });
        },

        bindClickEvents: function(id) {
            var self = this;
            var elem = this.$('#tab-' + id);
            this.$('.schema-tab[data-id="' + id + '"] > .cancelSchema')
                .on("click", function() {
                    var anchor = $(this).siblings('a');
                    var streamName = anchor.children().html();
                    self.streamNames.splice(self.streamNames.indexOf(streamName), 1);
                    var t_id = anchor.children().data('id');
                    var $firstTab = $(".schema-tabs .nav-tabs li").children('a').first();
                    if ($firstTab.parent().data('id') === t_id) {
                        $firstTab = $($(".schema-tabs .nav-tabs li").children('a').get(1));
                    }
                    $("#tab-" + anchor.children().data('id')).remove();
                    $(this).off('click');
                    $(this).siblings().off('click');
                    self.$('validateOutput[data-id="' + t_id + '"]').off('click');
                    self.$('.schema-tab > a > span[data-id="' + t_id + '"]').off('click');
                    $(this).parent().remove();
                    if (self.tabNumbers.indexOf(t_id) !== -1) {
                        self.tabNumbers = _.reject(self.tabNumbers, function(num){ return num == t_id; });
                    }
                    if ($(".schema-tabs .nav-tabs li").children('a').not('#add-tab').length === 1) {
                        $(".schema-tabs .nav-tabs li").children('a').not('#add-tab').siblings().hide();
                    }
                    $('.target-nodes[data-stream="'+streamName+'"]').parents('.box-body').remove();
                    $firstTab.click();
                    self.currentTabId = $firstTab.parent().data().id;
                });

            this.$(".schema-tab[data-id='" + id + "']").on("click", "a", function(e) {
                //e.stopPropagation();
                var t_id = $(this).children('span').data('id');
                self.$('.tab-pane.active').removeClass('active');
                self.$('#tab-' + t_id).addClass("active");
                self.$('.schema-tab.active').removeClass('active');
                $(this).parent().addClass('active');
                self.currentTabId = $(this).parent().data().id;
                $('.cancelSchema').hide();
                if ($(".schema-tabs .nav-tabs li").children('a').not('#add-tab').length !== 1) {
                    $(this).siblings().show();
                }
            });

            this.$('.validateOutput[data-id="' + id + '"]').on('click', function(e) {
                var id = $(e.currentTarget).data('id');
                self.evValidateOutputSchema(id);
            });

            var streamTitleElem = this.$('span[data-id="' + id + '"]');
            streamTitleElem.editable({
                mode: 'popup',
                toggle: 'manual',
                disabled: !self.editMode,
                validate: function(value) {
                    if (_.isEqual($.trim(value), '')) return 'Name is required';
                    if (self.streamNames.indexOf(value) !== -1) {
                        return 'Stream name should be unique';
                    } else {
                        var oldValue = streamTitleElem.html();
                        self.streamNames[self.streamNames.indexOf(oldValue)] = value;
                        var elem = $('.target-nodes[data-stream="'+oldValue+'"]').parents('.box-body');
                        elem.find('label')[0].textContent = value;
                        elem.find('.target-nodes').attr('data-stream', value);
                    }
                }
            });

            this.$('.schema-tab > a > span[data-id="' + id + '"]').on('click', function(e) {
                if ($(e.currentTarget).parents('li').hasClass('active')) {
                    e.stopPropagation();
                    streamTitleElem.editable('toggle');
                }
            });

            streamTitleElem.on('save', function(e, params) {
                $(e.currentTarget).html(params.newValue);
            });
        },

        evValidateOutputSchema: function(id) {
            try {
                var result = jsonlint.parse(this.$(".outputSource[data-id='" + id + "']").val());
                if (result) {
                    this.$(".outputResult[data-id='" + id + "']").html("JSON is valid!").removeClass("alert alert-danger").addClass("alert alert-success");
                    this.$(".outputSource[data-id='" + id + "']").val(JSON.stringify(result, null, "  "));
                }
                return true;
            } catch (e) {
                this.$(".outputResult[data-id='" + id + "']").html(e.message).addClass("alert alert-danger");
                return false;
            }
        },

        evAddTab: function(e) {
            if (!this.editMode)
                return;
            e.preventDefault();
            if ($(".schema-tabs .nav-tabs li").children('a').not('#add-tab').length === 1) {
                $(".schema-tabs .nav-tabs li").children('a').not('#add-tab').siblings().show();
            }
            var id = this.tabIdCount++,
                name = 'Stream ' + id;
            this.renderTab(name, id);
            $('.cancelSchema').hide();
            this.$('.schema-tab[data-id="' + id + '"]').children('i').show();
        },

        validate: function() {
            var isValid = true;

            this.$("[data-error]").remove();

            if (this.$(".splitter-class").val() == '') {
                isValid = false;
                this.$(".splitter-class-holder").append('<div data-error>Class cannot be blank.</div>');
            }
            if (this.$(".selectJar").val() == '') {
                isValid = false;
                this.$(".select-jar-holder").append('<div data-error>Select a jar name.</div>');
            }
            if (!this.validateOutput()) {
                isValid = false;
                this.$(".schema-tabs").append('<div data-error>Enter valid output schema.</div>');
            }
            if (!this.validateStreamLinks()) {
                isValid = false;
                Utils.notifyError("Two streams cannot be connected to the same node.");
            }
            return isValid;
        },

        validateOutput: function() {
            if (this.tabNumbers.length) {
                var flag = true;
                for (var id in this.tabNumbers) {
                    if (flag) {
                        flag = this.evValidateOutputSchema(this.tabNumbers[id]);
                    }
                }
                return flag;
            } else {
                return true;
            }
        },

        validateStreamLinks: function() {
            var streamTargets = [],
                selectedStreams = this.getSelectedStreams(),
                flag = true;
            _.each(selectedStreams, function(obj) {
                if (_.contains(streamTargets, obj.name))
                    flag = false;
                else streamTargets.push(obj.name);
            });
            return flag;
        },

        getOutputStreams: function() {
            var obj = {};
            for (var id in this.tabNumbers) {
                var streamName = $('.schema-tab[data-id="' + this.tabNumbers[id] + '"] > a > span').html();
                var streamData = jsonlint.parse(this.$(".outputSource[data-id='" + this.tabNumbers[id] + "']").val());
                obj[streamName] = streamData;
            }
            return obj;
        },

        getSelectedStreams: function() {
            var selectedStreams = [],
                keys = _.keys(this.getOutputStreams()),
                self = this;
            _.each(keys, function(key) {
                var targetNode = self.$('.target-nodes[data-stream="' + key + '"]').val();
                if (targetNode) {
                    selectedStreams.push({
                        name: targetNode,
                        streamName: key
                    });
                }
            });
            return selectedStreams;
        },

        getConfigData: function() {
            var jarName = this.$(".selectJar").val(),
                actions = [{
                    __type: "com.hortonworks.iotas.layout.design.splitjoin.SplitAction",
                    name: "split-action",
                    outputStreams: null,
                    jarId: '',
                    splitterClassName: ''
                }],
                parallelism = this.$(".parallelism").val() ? this.$(".parallelism").val() : 1;

            actions[0].outputStreams = this.getOutputStreams();
            actions[0].splitterClassName = this.$(".splitter-class").val();

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

    return SplitProcessorConfig;
});
