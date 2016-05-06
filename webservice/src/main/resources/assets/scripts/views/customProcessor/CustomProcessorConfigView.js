define([
    'require',
    'hbs!tmpl/customProcessor/custom-processor-config',
    'utils/Utils',
    'modules/Modal',
    'views/customProcessor/ProcessorConfigForm',
    'models/VCustomProcessor',
    'utils/TableLayout',
    'utils/LangSupport',
    'bootbox',
    'utils/Globals',
    'modules/Vent',
    'x-editable'
], function(require, tmpl, Utils, Modal, ProcessorConfigForm, VCustomProcessor, VTableLayout, localization, bootbox, Globals, Vent, xEditable) {
    'use strict';

    var customProcessorConfigView = Marionette.LayoutView.extend({

        template: tmpl,

        templateHelpers: function() {},

        regions: {
            processorConfigForm: '#processorConfigForm',
            configFieldTable: '#configFieldTable'
        },

        events: {
            'click #add-config-field-btn': 'evAddConfigFields',
            'click #editAction': 'evEditConfig',
            'click #deleteAction': 'evDeleteConfig',
            'click #btnSave': 'evSaveProcessorConfig',
            'click #btnCancel': 'evCloseProcessorConfig',
            'click #schemaValidate': 'evValidateInputSchema',
            'click #add-tab': 'evAddTab'
        },

        initialize: function(options) {
            if (options.model) {
                this.model = options.model;
                this.editState = true;
            } else {
                this.model = new VCustomProcessor();
                this.editState = false;
            }
            this.vent = Vent;
            this.configFieldId = 1;
            this.configFieldCollection = new Backbone.Collection();
            _.each(this.model.get("configFields"), function(obj) {
                var model = new Backbone.Model(obj);
                model.set("id", this.configFieldId++);
                this.configFieldCollection.add(model);
            }, this);
            this.bindEvents();
            this.streamNames = [];
            this.isEditConfig = false;
            this.inputSchema = this.model.get("inputSchema");
            this.currentTabId = 1;
            this.tabIdCount = 1;
            this.tabNumbers = [1];
        },
        bindEvents: function() {
            var self = this;

            this.listenTo(this.vent, 'customProcessorConfig:SaveConfig', function(model) {
                if (self.isEditConfig) {
                    self.configFieldCollection.remove(model.get('id'));
                    self.configFieldCollection.add(model);
                } else {
                    model.set('id', self.configFieldId++);
                    self.configFieldCollection.add(model);
                }

            });

        },

        onRender: function() {
            var model = new Backbone.Model(),
                self = this;
            model.set(this.model.attributes);
            model.set("streamingEngine", "STORM");
            this.processorConfigView = new ProcessorConfigForm({
                model: model,
                editState: self.editState
            });
            this.processorConfigForm.show(this.processorConfigView);
            this.configFieldTable.show(
                new VTableLayout({
                    columns: this.getColumns(),
                    collection: this.configFieldCollection,
                    includePagination: false,
                    includeFooterRecords: false,
                    gridOpts: {
                        emptyText: 'No fields found.',
                        className: 'table table-backgrid table-bordered table-striped table-condensed'
                    }
                })
            );
            this.$el.find('.col-sm-3').removeClass('col-sm-3').addClass('col-sm-2');
            this.$el.find('.col-sm-9').removeClass('col-sm-9').addClass('col-sm-6');

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

            if (self.model.has("inputSchema")) {
                this.$("#inputSource").val(JSON.stringify(self.model.get("inputSchema"), null, "  "));
            } else this.$("#inputSource").val(JSON.stringify(self.sampleSchema, null, "  "));

            $("body").on("click", ".nav-link", function(e) {
                e.preventDefault();
                e.stopImmediatePropagation();
                var target = this,
                    message = 'Do you want to leave the page without saving?',
                    title = 'Confirm',
                    successCallback = function(){
                        window._preventNavigation = false;
                        $("body").off('click', '.nav-link');
                        target.click();
                    };
                Utils.ConfirmDialog(message, title, successCallback);
                return false;
            });

            this.renderOutputShema();
            this.$('.schema-tab[data-id="'+(this.tabIdCount - 1)+'"]').children('i').show();
            window._preventNavigation = true;
        },

        renderOutputShema: function() {
            var self = this;
            if (self.model.has("outputStreamToSchema")) {
                var streams = _.keys(self.model.get("outputStreamToSchema"));
                _.each(streams, function(name) {
                    self.streamNames.push(name);
                    var id = self.tabIdCount++;
                    var schema = self.model.get("outputStreamToSchema")[name];
                    self.renderTab(name, id);
                    self.$('.outputSource[data-id="' + id + '"]').val(JSON.stringify(schema, null, "  "));
                });
            } else {
                this.tabIdCount++;
                self.renderTab('Stream-1', 1);
                this.$('.outputSource[data-id="1"]').val(JSON.stringify(self.sampleSchema, null, "  "));
            }
        },

        bindClickEvents: function(id) {
            //outputSchema events
            var self = this;
            var elem = this.$('#tab-' + id);
            this.$('.schema-tab[data-id="' + id + '"] > .cancelSchema')
                .on("click", function() {
                    var anchor = $(this).siblings('a');
                    var streamName = anchor.children().html();
                    self.streamNames.splice(self.streamNames.indexOf(streamName), 1);
                    var t_id = anchor.children().data('id');
                    var $firstTab = $(".schema-tabs .nav-tabs li").children('a').first();
                    if($firstTab.parent().data('id') === t_id){
                        $firstTab = $($(".schema-tabs .nav-tabs li").children('a').get(1));
                    }
                    $("#tab-" + anchor.children().data('id')).remove();
                    $(this).off('click');
                    $(this).siblings().off('click');
                    self.$('validateOutput[data-id="' + t_id + '"]').off('click');
                    self.$('.schema-tab > a > span[data-id="'+t_id+'"]').off('click');
                    $(this).parent().remove();
                    if (self.tabNumbers.indexOf(t_id) !== -1) {
                        self.tabNumbers.splice(self.tabNumbers.indexOf(t_id), 1);
                    }
                    if($(".schema-tabs .nav-tabs li").children('a').not('#add-tab').length === 1){
                        $(".schema-tabs .nav-tabs li").children('a').not('#add-tab').siblings().hide();
                    }
                    $firstTab.click();
                    self.currentTabId = $firstTab.parent().data().id;
                });

            this.$(".schema-tab[data-id='" + id + "']").on("click", "a", function(e) {
                // e.stopPropagation();
                var t_id = $(this).children('span').data('id');
                self.$('.tab-pane.active').removeClass('active');
                self.$('#tab-' + t_id).addClass("active");
                self.$('.schema-tab.active').removeClass('active');
                $(this).parent().addClass('active');
                self.currentTabId = $(this).parent().data().id;
                $('.cancelSchema').hide();
                if($(".schema-tabs .nav-tabs li").children('a').not('#add-tab').length !== 1){
                    $(this).siblings().show();
                }
            });

            this.$('.validateOutput[data-id="' + id + '"]').on('click', function(e) {
                var id = $(e.currentTarget).data('id');
                self.evValidateOutputSchema(id);
            });

            var streamTitleElem = this.$('span[data-id="'+id+'"]');
            streamTitleElem.editable({
              mode:'popup',
              toggle: 'manual',
              validate: function(value) {
                  if(_.isEqual($.trim(value), '')) return 'Name is required';
                  if(self.streamNames.indexOf(value) !== -1) {
                    return 'Stream name should be unique throughout the custom processor';
                } else {
                    self.streamNames[self.streamNames.indexOf(streamTitleElem.html())] = value;
                }
              }
            });

            this.$('.schema-tab > a > span[data-id="'+id+'"]').on('click',function(e){
                if($(e.currentTarget).parents('li').hasClass('active')){
                    e.stopPropagation();
                    streamTitleElem.editable('toggle');
                }
            });

            streamTitleElem.on('save', function(e, params) {
                $(e.currentTarget).html(params.newValue);
            });
        },

        evAddTab: function(e) {
            e.preventDefault();
            if($(".schema-tabs .nav-tabs li").children('a').not('#add-tab').length === 1){
                $(".schema-tabs .nav-tabs li").children('a').not('#add-tab').siblings().show();
            }
            var id = this.tabIdCount++,
                name = 'Stream ' + id;
            this.renderTab(name, id);
            $('.cancelSchema').hide();
            this.$('.schema-tab[data-id="'+id+'"]').children('i').show();
        },
        renderTab: function(name, id) {

            this.$("#add-tab").closest('li').before('<li class="schema-tab" data-id="' + id + '"><a href="javascript:void(0);"><span data-id="'+id+'">' + name + '</span></a><i class="fa fa-times-circle cancelSchema" style="display:none;"></i></li>');
            this.$('.tab-pane[id="tab-' + this.currentTabId + '"]').removeClass("active");
            this.$("#add-tab").closest('ul').find('.active').removeClass('active');
            this.$("#add-tab").closest('ul').find('[data-id="' + id + '"]').addClass('active');
            this.$('.tab-content').append('<div role="tabpanel" class="tab-pane active" id="tab-' + id + '">' +
                '<div class="row">' +
                '<div class="col-sm-7">' +
                '<textarea data-id="' + id + '" rows="10" class="outputSource form-control"></textarea>' +
                '</div>' +
                '<div class="col-sm-5">' +
                '<button class="validateOutput btn btn-block btn-primary row-margin-bottom" id="outputBtn" data-id="' + id + '">' +
                '<i class="fa fa-chevron-left"></i> Validate JSON' +
                '</button>' +
                '<div data-id="' + id + '" class="outputResult json-message" role="alert"></div>' +
                '</div>' +
                '</div>' +
                '</div>');
            this.tabNumbers.push(id);
            this.bindClickEvents(id);
            this.currentTabId = id;
        },
        getColumns: function() {
            return [{
                name: 'name',
                cell: 'string',
                label: 'Name',
                hasTooltip: false,
                editable: false
            }, {
                name: 'isOptional',
                cell: 'string',
                label: 'Is Optional',
                hasTooltip: false,
                editable: false
            }, {
                name: 'type',
                cell: 'string',
                label: 'Type',
                hasTooltip: false,
                editable: false
            }, {
                name: 'defaultValue',
                cell: 'string',
                label: 'Default Value',
                hasTooltip: false,
                editable: false
            }, {
                name: 'isUserInput',
                cell: 'string',
                label: 'Is User Input',
                hasTooltip: false,
                editable: false
            }, {
                name: 'tooltip',
                cell: 'string',
                label: 'Tooltip',
                hasTooltip: false,
                editable: false
            }, {
                name: "Actions",
                cell: "Html",
                label: localization.tt('lbl.actions'),
                hasTooltip: false,
                editable: false,
                formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                    fromRaw: function(rawValue, model) {
                        if (model) {
                            return "<button type='default' title='Edit' class='btn btn-success btn-xs' id='editAction' data-id='" + model.get("id") + "'><i class='fa fa-edit'></i></button><button title='Delete' class='btn btn-danger btn-xs' id='deleteAction' data-id='" + model.get("id") + "'><i class='fa fa-trash'></i></button>";
                        }
                    }
                })
            }];
        },

        evAddConfigFields: function(model) {
            var self = this;

            if (_.isUndefined(model) || model.currentTarget) {
                model = new Backbone.Model();
                this.isEditConfig = false;
            }
            require(['views/customProcessor/ConfigFieldsLayoutView'], function(ConfigFieldsLayoutView) {
                var view = new ConfigFieldsLayoutView({
                    model: model,
                    vent: self.vent
                });

                var modal = new Modal({
                    title: 'Add Config Fields',
                    content: view,
                    contentWithFooter: true,
                    showFooter: false
                }).open();

                view.on('closeModal', function() {
                    modal.trigger('cancel');
                });
            });
        },

        evEditConfig: function(e) {
            var id = $(e.currentTarget).data().id,
                model = this.getModel(id);
            this.evAddConfigFields(model);
            this.isEditConfig = true;
        },

        evDeleteConfig: function(e) {
            var id = $(e.currentTarget).data().id,
                model = this.getModel(id);

            this.configFieldCollection.remove(model);
        },

        getModel: function(id) {
            var configModel,
                self = this;

            configModel = _.find(self.configFieldCollection.models, function(model) {
                if (model.get('id') == id)
                    return true;
            });
            return configModel;
        },

        evSaveProcessorConfig: function() {
            var errorsArr = this.processorConfigView.validate(),
                validInputSchema = this.evValidateInputSchema(),
                validOutputSchema = this.validateOutput();

            if (!errorsArr && validInputSchema && validOutputSchema && this.configFieldCollection.models.length) {
                this.saveCustomProcessor();
            } else {
                return false;
            }
        },
        saveCustomProcessor: function() {
            var formData = new FormData(),
                attrs = this.processorConfigView.getData(),
                configFields = [],
                obj = {},
                type = this.editState? 'PUT': 'POST',
                self = this;

            if (!_.isEqual(attrs.jarFileName.name.split('.').pop().toLowerCase(), 'jar')) {
                Utils.notifyError(localization.tt('dialogMsg.invalidFile'));
                return false;
            }
            // if(!_.isEqual(attrs.imageFileName.name.split('.').pop().toLowerCase(), 'png') 
            //     !_.isEqual(attrs.imageFileName.name.split('.').pop().toLowerCase(), 'jpeg')) {
            //   Utils.notifyError(localization.tt('dialogMsg.invalidFile'));
            //     return false;
            // }

            _.each(this.configFieldCollection.models, function(model) {
                var obj = JSON.parse(JSON.stringify(model.toJSON()));
                delete obj.id;
                configFields.push(obj);
            });

            obj.configFields = configFields;
            obj.inputSchema = this.inputSchema;
            obj.name = attrs.name;
            obj.streamingEngine = attrs.streamingEngine;
            obj.description = attrs.description;
            obj.customProcessorImpl = attrs.customProcessorImpl;
            obj.outputStreamToSchema = this.getOutputStreams();
            obj.imageFileName = attrs.imageFileName.name;
            obj.jarFileName = attrs.jarFileName.name;
            formData.append('jarFile', attrs.jarFileName);
            formData.append('imageFile', attrs.imageFileName);
            formData.append('customProcessorInfo', JSON.stringify(obj));
            var url = '/api/v1/catalog/system/componentdefinitions/PROCESSOR/custom';
            var successCallback = function(response) {
                var msg = 'New processor added successfully.';
                if(self.editState){
                    msg = 'Processor updated successfully.';
                }
                Utils.notifySuccess(msg);
                self.vent.trigger('controlPanel:Cancel');
            };
            var errorCallback = function(model, response, options) {
                Utils.showError(model, response);
            };
            Utils.uploadFile(url, formData, successCallback, errorCallback, type);
        },
        evCloseProcessorConfig: function() {
            this.vent.trigger('controlPanel:Cancel');
        },
        evValidateInputSchema: function() {
            try {
                var result = jsonlint.parse(this.$("#inputSource").val());
                if (result) {
                    this.$("#inputResult").html("JSON is valid!").removeClass("alert alert-danger").addClass("alert alert-success");
                    this.$("#inputSource").val(JSON.stringify(result, null, "  "));
                    this.inputSchema = result;
                }
                return true;
            } catch (e) {
                this.$("#inputResult").html(e.message).addClass("alert alert-danger");
                return false;
            }
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

        getOutputStreams: function() {
            var obj = {};
            for (var id in this.tabNumbers) {
                var streamName = $('.schema-tab[data-id="' + this.tabNumbers[id] + '"] > a > span').html();
                var streamData = jsonlint.parse(this.$(".outputSource[data-id='" + this.tabNumbers[id] + "']").val());
                obj[streamName] = streamData;
            }
            return obj;
        }

    });
    return customProcessorConfigView;
});
