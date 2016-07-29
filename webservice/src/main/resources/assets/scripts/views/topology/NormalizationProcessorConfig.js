define(['utils/LangSupport',
    'utils/Globals',
    'utils/Utils',
    'models/VParser',
    'models/VDatasource',
    'hbs!tmpl/topology/normalizationProcessorConfig',
    'bootstrap-switch'
], function(localization, Globals, Utils, VParser, VDatasource, tmpl) {
    'use strict';

    var NormalizationProcessorConfig = Marionette.LayoutView.extend({

        template: tmpl,

        events: {
            'click .select-list': 'evAddAllFields',
            'change .select-field': 'evMapOutputField',
            'keyup .mandatory-text': 'evAddDefaultValue',
            'change .input-stream-select': 'evSelectStream',
            'click .deleteField': 'evRemoveField'
        },

        templateHelpers: function(){
            return {
                editMode: this.editMode
            }
        },

        initialize: function(options) {
            _.extend(this, options);
            var self = this;
            this.transformers = [];
            this.inputSchemaConfigObject = {};
            this.inputFieldsArr = [];
            this.outputFieldsArr = [];
            this.mappedInputFieldIds = {};
            this.inputSchemaMappings = {};

            this.sourceConfig = JSON.parse(JSON.stringify(this.sourceConfig));

            var oldInputStream;
            switch (this.sourceConfig.currentType) {
                case 'PARSER':
                    var id = self.dsId || self.sourceConfig.dataSourceId || self.sourceConfig._dataSourceId;
                    if(self.model.has('newConfig')){
                        oldInputStream = this.model.get('newConfig').normalizationProcessorConfig.inputStreamsWithNormalizationConfig;
                        self.inputFieldsArr = oldInputStream[self.streamId].inputSchema.fields;
                    } else {
                        self.inputFieldsArr = self.getFeedSchema(id);
                    }
                    self.inputSchemaConfigObject[self.streamId] = {
                        "__type": "com.hortonworks.iotas.layout.design.normalization.FieldBasedNormalizationConfig",
                        inputSchema: {
                            fields: self.inputFieldsArr
                        },
                        transformers: [],
                        fieldsToBeFiltered: [],
                        newFieldValueGenerators: []
                    };
                    break;
                case 'RULE':
                    if(self.model.has('newConfig')){
                        oldInputStream = this.model.get('newConfig').normalizationProcessorConfig.inputStreamsWithNormalizationConfig;
                        if(!oldInputStream[self.streamId]){
                            var k = _.keys(oldInputStream);
                            var duplicateObj = JSON.parse(JSON.stringify(oldInputStream[k[0]]));
                            _.each(k, function(id){
                                delete oldInputStream[id];
                            })
                            oldInputStream[self.streamId] = duplicateObj;
                        }
                        self.inputFieldsArr = oldInputStream[self.streamId].inputSchema.fields;
                    } else {
                        self.inputFieldsArr = _.extend([], (self.sourceConfig.newConfig ? self.sourceConfig.newConfig.rulesProcessorConfig.declaredInput : self.sourceConfig.rulesProcessorConfig.declaredInput));
                    }
                    self.inputSchemaConfigObject[self.streamId] = {
                        "__type": "com.hortonworks.iotas.layout.design.normalization.FieldBasedNormalizationConfig",
                        inputSchema: {
                            fields: self.inputFieldsArr
                        },
                        transformers: [],
                        fieldsToBeFiltered: [],
                        newFieldValueGenerators: []
                    };
                    break;
                case 'CUSTOM':
                    var selectedStreams = _.where(self.sourceConfig.selectedStreams, { name: self.uiname }),
                        streamNames = _.map(selectedStreams, function(obj) {
                            return obj.streamName;
                        }),
                        inputStreams = _.where(self.sourceConfig.config, { name: "outputStreamToSchema" })[0].defaultValue;
                    self.inputStreams = _.pick(inputStreams, streamNames);
                    var streamIds = _.keys(self.inputStreams);
                    self.streamId = streamIds[0];

                    for (var i = 0; i < streamIds.length; i++) {
                        var obj = self.inputStreams[streamIds[i]];
                        self.inputSchemaConfigObject[streamIds[i]] = {
                            "__type": "com.hortonworks.iotas.layout.design.normalization.FieldBasedNormalizationConfig",
                            inputSchema: {
                                fields: obj.fields
                            },
                            transformers: [],
                            fieldsToBeFiltered: [],
                            newFieldValueGenerators: []
                        }
                    }

                    self.inputFieldsArr = [];
                    var id = 0;
                    for (var i = 0; i < streamNames.length; i++) {
                        var fields = self.inputStreams[streamNames[i]].fields;
                        for (var j = 0; j < fields.length; j++) {
                            self.inputFieldsArr.push({
                                name: fields[j].name,
                                optional: fields[j].optional,
                                type: fields[j].type,
                                isAdded: false
                            });
                        }
                    }

                    self.getStreamFields(streamIds[0]);
                    break;
            }

            var config;
            if(this.model.has('newConfig')){
                config = this.model.get('newConfig').normalizationProcessorConfig.inputStreamsWithNormalizationConfig;
            } else if(this.model.has('normalizationProcessorConfig')){
                config = this.model.get('normalizationProcessorConfig').inputStreamsWithNormalizationConfig;
            }

            if (config) {
                //existing data
                this.outputFieldsArr = this.model.get('newConfig').normalizationProcessorConfig.outputStreams[0].schema.fields;
                var streams = _.keys(config);
                for (var k = 0; k < streams.length; k++) {
                    var mappingsArr = config[streams[k]].transformers;
                    this.inputSchemaMappings[streams[k]] = {};
                    for (var l = 0; l < mappingsArr.length; l++) {
                        this.inputSchemaMappings[streams[k]][mappingsArr[l].inputField.name] = mappingsArr[l].outputField.name;
                        this.transformers.push({
                            streamId: streams[k],
                            inputField: mappingsArr[l].inputField,
                            outputField: mappingsArr[l].outputField,
                            converterScript: mappingsArr[l].converterScript
                        });

                        var input = _.find(this.inputFieldsArr, function(field) {
                            return mappingsArr[l].inputField.name == field.name;
                        });
                        if(input){
                            input.isAdded = true;
                        }
                        if(!this.mappedInputFieldIds[self.streamId]){
                            this.mappedInputFieldIds[self.streamId] = [];
                        }
                        this.mappedInputFieldIds[self.streamId].push(mappingsArr[l].inputField.name);
                    }
                }
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

        getStreamFields: function(streamName) {
            this.streamId = streamName;
            var fields = this.inputStreams[streamName].fields;
            this.inputSchemaArr = [];
            _.each(fields, function(data, i) {
                var input = _.find(this.inputFieldsArr, function(field) {
                    return field.name == data.name;
                });
                this.inputSchemaArr.push(input);
            }, this);

            this.renderInputFieldList(this.inputSchemaArr);
        },


        onRender: function(options) {
            var sourceType = this.sourceConfig.currentType,
                self = this;
            if (sourceType == 'RULE' || sourceType == 'PARSER') {
                this.$('.stream-select').hide();
                this.renderInputFieldList(this.inputFieldsArr);
            } else {
                var streams = _.keys(self.inputStreams);
                for (var i = 0; i < streams.length; i++) {
                    self.$('.input-stream-select').append('<option>' + streams[i] + '</option>');
                }
                this.renderInputFieldList(this.inputSchemaArr);
            }
            var accordion = this.$('[data-toggle="collapse"]');
            if(accordion.length){
                accordion.on('click', function(e){
                  $(e.currentTarget).children('i').toggleClass('fa-caret-right fa-caret-down');
                });
            }
            this.$('#np-parallelism').val(this.model.has('newConfig') ? this.model.get('newConfig').parallelism : 1);
            this.renderOutputSchema();
            this.$('[data-rel="tooltip"]').tooltip();
        },

        renderOutputSchema: function() {
            var self = this;

            self.$('.output-list').html('');
            _.each(this.outputFieldsArr, function(field) {
                if (field.optional) {
                    self.$('.output-list').append('<li class="optional" data-id="' + field.name + '">' +
                        '<a href="javascript:void(0)" class="editable"> ' + field.name + '</a>' +
                        (self.editMode ? '<a href="javascript:void(0)" class="deleteField pull-right"><i class="fa fa-trash"></i></a>' +
                        '<a href="javascript:void(0)" class="popover2 pull-right"><i class="fa fa-asterisk"></i></a>' +
                        '' : '')+'</li>');
                } else {
                    self.$('.output-list').append('<li class="mandatory" data-id="' + field.name + '">' +
                        '<a href="javascript:void(0)" class="editable"> ' + field.name + '</a>' +
                        (self.editMode ? '<a href="javascript:void(0)" class="deleteField pull-right"><i class="fa fa-trash"></i></a>' +
                        '' : '')+'</li>');
                }
            });
            if(self.editMode){
                this.bindOutputListEvents();
            }
        },

        bindInputListEvents: function() {
            var self = this;
            this.$('.input-field-list a').tooltip({
                placement: 'bottom'
            });

            this.$('.input-field-list a').popover({
                    placement: 'bottom',
                    html: true,
                    content: '<button class="btn btn-primary btn-sm mapExisting">Map To Existing</button><button class="btn btn-primary btn-sm addNew">Add New</button><hr/><div class="dropdown-holder"></div>'
                }).on('show.bs.popover', function() {
                    if (self.$('.popover').length)
                        self.$('.popover').popover('hide');
                })
                .on('shown.bs.popover', function(e) {

                    self.$('.mapExisting').on('click', function(e) {
                        self.$('.select-field').show();
                        self.$('.dropdown-holder').html('<select class="form-control select-field"><option selected disabled hidden></option></select>');

                        _.each(self.outputFieldsArr, function(field) {
                            self.$('.select-field').append('<option>' + field.name + '</option>');
                        });

                        var id = $(e.currentTarget).parents('li').data('id'),
                            inputField = _.find(self.inputFieldsArr, function(field) {
                                return id == field.name;
                            });

                        if(!self.inputSchemaMappings[self.streamId]){
                            self.inputSchemaMappings[self.streamId] = {};
                        }
                        if (self.inputSchemaMappings[self.streamId].hasOwnProperty(inputField.name))
                            self.$('.select-field').val(self.inputSchemaMappings[self.streamId][inputField.name]);

                    });

                    self.$('.addNew').on('click', function(e) {
                        self.$('.select-field').hide();
                        var fieldId = $(e.currentTarget).parents('li').data('id');
                        var field = _.find(self.inputFieldsArr, function(obj) {
                            return obj.name == fieldId;
                        });

                        if (field.isAdded || _.contains(self.mappedInputFieldIds[self.streamId], field.name))
                            return;

                        if (field.optional)
                            self.$('.output-list').append('<li class="optional" data-id="' + field.name + '">' +
                                '<a href="javascript:void(0)" class="editable"> ' + field.name + '</a>' +
                                '<a href="javascript:void(0)" class="popover2 pull-right"><i class="fa fa-asterisk"></i></a>' +
                                '<a href="javascript:void(0)" class="deleteField pull-right"><i class="fa fa-trash"></i></a>' +
                                '</li>');
                        else self.$('.output-list').append('<li class="mandatory" data-id="' + field.name + '">' +
                            '<a href="javascript:void(0)" class="editable"> ' + field.name + '</a>' +
                            '<a href="javascript:void(0)" class="deleteField pull-right"><i class="fa fa-trash"></i></a>' +
                            '</li>');

                        field.isAdded = true;
                        self.outputFieldsArr.push({
                            //id: field.id,
                            name: field.name,
                            type: field.type,
                            optional: field.optional
                        });
                        self.$('.editable').editable({
                            mode: 'inline',
                            validate: function(value) {
                                if ($.trim(value) == '') {
                                    return 'This field is required';
                                }
                            }
                        }).on('save', function(e, params) {
                            var id = $(e.currentTarget).parents('li').data('id'),
                                outputField = _.find(self.outputFieldsArr, function(field) {
                                    return id == field.name;
                                });
                            var transformerObj = _.find(self.transformers, function(field) {
                                return outputField.name == field.outputField.name;
                            });
                            // if(!this.inputSchemaMappings[self.streamId]){
                            //     this.inputSchemaMappings[self.streamId] = {};
                            // }
                            var v = _.values(self.inputSchemaMappings[self.streamId]);
                            var k = _.keys(self.inputSchemaMappings[self.streamId]);
                            for(var i = 0; i < v.length; i++){
                                if(v[i] === transformerObj.outputField.name){
                                    self.inputSchemaMappings[self.streamId][k[i]] = params.newValue;
                                }
                            }
                            transformerObj.outputField.name = params.newValue;
                            outputField.name = params.newValue;
                            $(e.currentTarget).parents('li').data('id', params.newValue);
                            self.renderInputFieldList(self.inputSchemaArr ? self.inputSchemaArr : self.inputFieldsArr);
                        });
                        self.bindOutputListEvents();
                        self.evMapOutputField(null, field.name);
                    });
                }).on('hide.bs.popover', function() {
                    self.$('.addNew').off('click');
                    self.$('.mapExisting').off('click');
                });
        },

        bindOutputListEvents: function() {
            var self = this;
            $('.popover2').popover({
                template: '<div class="popover" role="tooltip"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content" align="center"></div></div>',
                placement: 'left',
                html: true,
                content: '<input type="checkbox" class="output-switch" /></div><hr/><input type="text" class="form-control mandatory-text" placeholder="enter default value" />'
            }).on('shown.bs.popover', function(e) {
                var id = $(e.currentTarget).parents('li').data('id'),
                    outputField = _.find(self.outputFieldsArr, function(field) {
                        return id == field.name
                    });

                $('.output-switch').bootstrapSwitch({
                    size: 'mini',
                    onText: 'Mandatory',
                    offText: 'Optional',
                    onColor: 'danger',
                    offColor: 'warning',
                    onSwitchChange: function(e) {

                        if ($('.output-switch').bootstrapSwitch('state')) {
                            $('.mandatory-text').val(outputField.defaultValue);
                            $('.mandatory-text').prop("disabled", false);
                            outputField.optional = false;
                        } else {
                            $('.mandatory-text').prop("disabled", true).val('');
                            outputField.optional = true;
                            outputField.defaultValue = null;
                        }
                    }
                });
                $(".popover").position().left = $(".popover").position().left - 60;
                if (outputField.optional) {
                    $('.mandatory-text').prop("disabled", true).val('');
                } else {
                    $('.output-switch').bootstrapSwitch('state', true);
                    $('.mandatory-text').prop("disabled", false);
                    $('.mandatory-text').val(outputField.defaultValue ? outputField.defaultValue : '');
                }
            }).on('hide.bs.popover', function() {
                self.$('.bootstrap-switch').off('click');
            });

            this.$('.editable').editable({
                mode: 'inline',
                validate: function(value) {
                    if ($.trim(value) == '') {
                        return 'This field is required';
                    }
                }
            }).on('save', function(e, params) {
                var id = $(e.currentTarget).parents('li').data('id'),
                    outputField = _.find(self.outputFieldsArr, function(field) {
                        return id == field.name
                    });
                var transformerObj = _.find(self.transformers, function(field) {
                    return outputField.name == field.outputField.name;
                });
                
                var streamArr = _.keys(self.inputSchemaMappings);
                for(var s = 0; s < streamArr.length; s++){
                    var v = _.values(self.inputSchemaMappings[streamArr[s]]);
                    var k = _.keys(self.inputSchemaMappings[streamArr[s]]);
                    for(var i = 0; i < v.length; i++){
                        if(v[i] === transformerObj.outputField.name){
                            self.inputSchemaMappings[streamArr[s]][k[i]] = params.newValue;
                        }
                    }
                }

                for(var j = 0; j < self.transformers.length; j++){
                    if(self.transformers[j].outputField.name === outputField.name){
                        self.transformers[j].outputField.name = params.newValue;
                    }
                }
                outputField.name = params.newValue;
                $(e.currentTarget).parents('li').data('id', params.newValue);
                self.renderInputFieldList(self.inputSchemaArr ? self.inputSchemaArr : self.inputFieldsArr);
            });
        },

        renderInputFieldList: function(fieldsArr) {
            var self = this;
            this.$('.input-field-list').html('');
            for (var i = 0; i < fieldsArr.length; i++) {
                var field = fieldsArr[i],
                    className = field.optional ? 'optional' : 'mandatory',
                    associatedName = null,
                    badge='';
                if(self.inputSchemaMappings && self.inputSchemaMappings[self.streamId]){
                    associatedName = self.inputSchemaMappings[self.streamId][field.name];
                }
                if(associatedName){
                    var flag = true;
                    _.each(self.transformers, function(t){
                        if(flag){
                            if(t.streamId === self.streamId && t.outputField.name === associatedName && t.inputField.name === field.name){
                                flag = false;
                                badge = '<span class="label label-danger pull-right">'+associatedName+'</span>';
                            }
                        }
                    });
                }
                this.$('.input-field-list').append('<li class="' + className + '" data-id="' + field.name + '">'+
                    '<a href="javascript:void(0)" data-rel="popover"> ' + field.name + badge+ '</a>'+
                    '</li>');
            }
            this.$('.mapExisting').off('click');
            this.$('.addNew').off('click');
            var flag = true;
            _.each(fieldsArr, function(obj){
                if(flag){
                    if(!obj.isAdded){
                        flag = false;
                    }
                }
            });
            if(flag){
                this.$('#selectAll').addClass('fa-check-square-o').removeClass('fa-square-o');
            } else {
                this.$('#selectAll').addClass('fa-square-o').removeClass('fa-check-square-o');
            }
            if(this.editMode){
                this.bindInputListEvents();
            }
        },

        evAddAllFields: function() {
            var self = this;

            var fields = self.inputSchemaArr ? self.inputSchemaArr : self.inputFieldsArr;
            for (var i = 0; i < fields.length; i++) {

                var field = fields[i];
                if (field.isAdded)
                    continue;
                else field.isAdded = true;

                if (field.optional) {
                    self.$('.output-list').append('<li class="optional" data-id="' + field.name + '">' +
                        '<a href="javascript:void(0)" class="editable"> ' + field.name + '</a>' +
                        '<a href="javascript:void(0)" class="deleteField pull-right"><i class="fa fa-trash"></i></a>' +
                        '<a href="javascript:void(0)" class="popover2 pull-right"><i class="fa fa-asterisk"></i></a>' +
                        '</li>');
                } else self.$('.output-list').append('<li class="mandatory" data-id="' + field.name + '">' +
                    '<a href="javascript:void(0)" class="editable"> ' + field.name + '</a>' +
                    '<a href="javascript:void(0)" class="deleteField pull-right"><i class="fa fa-trash"></i></a>' +
                    '</li>');
                self.outputFieldsArr.push({
                    name: field.name,
                    type: field.type,
                    optional: field.optional
                });

                self.bindOutputListEvents();
                self.evMapOutputField(null, field.name);
            }
            this.$('#selectAll').addClass('fa-check-square-o').removeClass('fa-square-o');
        },

        evMapOutputField: function(e, fieldName) {
            var self = this;
            var value, id;
            if(e){
                value = $(e.currentTarget).val();
                id = $(e.currentTarget).parents('li').data('id');
            } else {
                value = fieldName;
                id = fieldName;
            }
            var inputArr = this.inputSchemaArr ? this.inputSchemaArr : this.inputFieldsArr;
            var inputField = _.find(inputArr, function(field) {
                return id == field.name
            });

            inputField.isAdded = true;

            if(!this.mappedInputFieldIds[self.streamId]){
                this.mappedInputFieldIds[self.streamId] = [];
            }
            if(this.mappedInputFieldIds[self.streamId].indexOf(inputField.name) === -1){
                this.mappedInputFieldIds[self.streamId].push(inputField.name);
            }

            if(!this.inputSchemaMappings[self.streamId]){
                this.inputSchemaMappings[self.streamId] = {};
            }
            var oldValue = this.inputSchemaMappings[self.streamId][inputField.name];
            this.inputSchemaMappings[self.streamId][inputField.name] = value;

            this.transformers = _.filter(this.transformers, function(field) {
                if(self.streamId === field.streamId){
                    return field.inputField.name !== inputField.name && field.outputField.name !== oldValue;   
                } else {
                    return true;
                }
            });

            var outputField = _.find(this.outputFieldsArr, function(field) {
                    return value == field.name;
                }),
                streamId = this.$(".input-stream-select").val() ? this.$(".input-stream-select").val() : this.streamId;
            this.transformers.push({
                streamId: streamId,
                inputField: {
                    name: inputField.name,
                    type: inputField.type,
                    optional: inputField.optional
                },
                outputField: {
                    name: outputField.name,
                    type: outputField.type,
                    optional: outputField.optional
                },
                converterScript: outputField.defaultValue ? outputField.defaultValue : null
            });
            self.renderInputFieldList(self.inputSchemaArr ? self.inputSchemaArr : self.inputFieldsArr);

        },

        evAddDefaultValue: function(e) {
            var id = $(e.currentTarget).parents('li').data('id'),
                outputField = _.find(this.outputFieldsArr, function(field) {
                    return id == field.name;
                });
            outputField.defaultValue = $(e.currentTarget).val();
        },

        evRemoveField: function(e) {
            var self = this;
            var id = $(e.currentTarget).parents('li').data('id'),
                outputField = _.find(this.outputFieldsArr, function(field) {
                    return id == field.name;
                });

            var v = _.values(self.inputSchemaMappings[self.streamId]);
            var k = _.keys(self.inputSchemaMappings[self.streamId]);
            var deleteKey = [];
            for(var i = 0; i < v.length; i++){
                if(v[i] === outputField.name){
                    deleteKey.push(k[i]);
                }
            }
            for(var dKey in deleteKey){
                delete self.inputSchemaMappings[self.streamId][deleteKey[dKey]];
            }
            
            var arr = _.filter(this.transformers, function(field) {
                return outputField.name == field.outputField.name;
            });
            for (var i = 0; i < arr.length; i++) {
                var obj = arr[i];

                var inputField = _.find(this.inputFieldsArr, function(field) {
                    return obj.inputField.name == field.name;
                });
                inputField.isAdded = false;
                self.mappedInputFieldIds[self.streamId].splice(self.mappedInputFieldIds[self.streamId].indexOf(inputField.name), 1);
            }


            this.transformers = _.filter(this.transformers, function(field) {
                return outputField.name != field.outputField.name;
            });

            // for (var key in this.inputSchemaMappings) {
            //     if (this.inputSchemaMappings[key] == outputField.name)
            //         delete this.inputSchemaMappings[key];
            // }

            this.outputFieldsArr.splice(this.outputFieldsArr.indexOf(outputField), 1);
            this.renderOutputSchema();
            this.renderInputFieldList(this.inputSchemaArr ? this.inputSchemaArr : this.inputFieldsArr);
        },

        evSelectStream: function(e) {
            this.getStreamFields($(e.currentTarget).val());
        },

        close: function() {},

        validate: function() {
            var err = [];
            if (this.outputFieldsArr.length == 0)
                err.push("Output schema cannot be empty.");
            return _.uniq(err);
        },
        getConfigData: function() {
            var outputFields = [],
                inputFields = [];
            //parallelism = this.$('.parallelism').val() ? this.$('.parallelism').val() : 1;

            _.each(this.outputFieldsArr, function(field) {
                outputFields.push({
                    name: field.name,
                    type: field.type,
                    optional: field.optional
                });
            }, this);


            for (var i = 0; i < this.transformers.length; i++) {
                var id = this.transformers[i].streamId;
                delete this.transformers[i].inputField.isAdded;
                delete this.transformers[i].outputField.isAdded;
                this.inputSchemaConfigObject[id].transformers.push({
                    inputField: this.transformers[i].inputField,
                    outputField: this.transformers[i].outputField,
                    converterScript: this.transformers[i].converterScript
                });
            }
            var keys = _.keys(this.inputSchemaConfigObject);
            for(var k in keys){
                var obj = this.inputSchemaConfigObject[keys[k]];
                for(var i = 0; i < obj.inputSchema.fields.length; i++){
                    if(!this.inputSchemaMappings[keys[k]] || !this.inputSchemaMappings[keys[k]][obj.inputSchema.fields[i].name]){
                        this.inputSchemaConfigObject[keys[k]].fieldsToBeFiltered.push(obj.inputSchema.fields[i].name);
                    }
                    delete obj.inputSchema.fields[i].isAdded;
                }
            }
            var config = {
                parallelism: parseInt(this.$('#np-parallelism').val()),
                normalizationProcessorConfig: {
                    type: "fineGrained",
                    outputStreams: [{
                        id: "normalized-output",
                        schema: {
                            fields: outputFields
                        }
                    }],
                    inputStreamsWithNormalizationConfig: this.inputSchemaConfigObject
                }
            };

            return config;
        }
    });

    return NormalizationProcessorConfig;
});
