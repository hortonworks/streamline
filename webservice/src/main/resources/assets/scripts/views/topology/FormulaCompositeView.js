define(['require',
    'utils/Globals',
    'utils/Utils',
    'hbs!tmpl/topology/formulaCompositeView',
    'views/topology/FormulaItemView'
], function(require, Globals, Utils, tmpl, FormulaItemView){
    'use strict';
    var FormulaCompositeView = Marionette.CompositeView.extend({
        template: tmpl,
        className: 'panel-body',
        templateHelpers: function(){
            var self = this;
            this.comparisonOpArr = [];
            this.logicalOpArr = [];

            _.each(_.findWhere(self.config, {name: 'operations'}).values, function(obj){
                self.comparisonOpArr.push({
                    val:obj,
                    lbl: obj
                });
            });
            _.each(_.findWhere(self.config, {name: 'logicalOperators'}).values, function(obj){
                self.logicalOpArr.push({
                    val:obj,
                    lbl: obj
                });
            });
            return {
                comparisonArr : this.comparisonOpArr,
                logicalArr : this.logicalOpArr,
                sinkConnected: this.sinkConnected,
                sinkList: this.connectedSink
            };
        },

        childView: FormulaItemView,

        childViewContainer: "div[data-id='addRowDiv']",

        childViewOptions: function() {
            return {
                collection: this.collection,
                comparisonArr: this.comparisonOpArr,
                logicalArr: this.logicalOpArr,
                fieldsArr : this.fieldsArr,
                // id: this.rowId++,
                vent: this.vent
            };
        },

        events: {
            'click #addNewRule': 'evAddRow',
            'change .ruleRow': 'evChange',
            // 'click #saveRule': 'evAddRule',
            // 'click #closeRule': 'evCloseRule'
        },

        initialize: function(options){
            _.extend(this, options);

            this.rowId = 2; //1 is for first row already rendered
            this.firstFormulaModel = new Backbone.Model({firstModel: true});
            this.collection = new Backbone.Collection();
            if(this.rulesArr.length){

                this.firstFormulaModel.set({
                    field1: this.rulesArr[0].get('firstOperand'),
                    field2: this.rulesArr[0].get('secondOperand'),
                    comp: this.rulesArr[0].get('operation')
                });

                for(var i = 1; i < this.rulesArr.length; i++){
                    var obj = this.rulesArr[i];
                    var model = new Backbone.Model({id: this.rowId++});
                    model.set('field1', obj.get('firstOperand'));
                    model.set('comp', obj.get('operation'));
                    model.set('field2', obj.get('secondOperand'));
                    model.set('logical', obj.get('logicalOperator'));
                    this.collection.add(model);
                }
            }
            this.bindEvents();
            this.fieldsArr = [];
            this.getFieldData(this.fields, 0);
        },
        getFieldData: function(fields, level) {

            _.each(fields, function(obj) {
                if(obj.type === 'NESTED') {
                    this.fieldsArr.push({
                        id: obj.name,
                        text: obj.name,
                        val: obj.name,
                        level: level,
                        disabled: true
                    });
                    this.getFieldData(obj.fields, level + 1);
                } else {
                    this.fieldsArr.push({
                        id: obj.name,
                        text: obj.name,
                        val: obj.name,
                        level: level,
                        disabled: false
                    });
                }
            }, this);
        },

        bindEvents: function(){
            var self = this;
            this.listenTo(this.vent,'delete:Formula', function(data){
                self.evChange(data);
            });
        },

        generateFormula: function(models){
            var firstModel = this.firstFormulaModel,
            msg = '';

        if(firstModel.has('field1')) {
            msg += '<span class="formulaField">('+firstModel.get('field1')+')</span>';
        } else {
            msg += '<span class="formulaError"> (Missing Field) </span>';
        }

        if(firstModel.has('comp')) {
            msg += '<span class="formulaComparison"> '+firstModel.get('comp')+' </span>';
        } else {
            msg += '<span class="formulaError"> Missing Operator </span>';
        }

        if(firstModel.has('field2')) {
            msg += '<span class="formulaField">('+firstModel.get('field2')+')</span>';
        } else {
            msg += '<span class="formulaError"> (Missing Field) </span>';
        }

      _.each(models, function(model){
        if(model.has('logical')){
            msg += '<br><span class="formulaLogical"> '+model.get('logical')+' </span><br>';
        } else if(!model.has('firstModel')){
            msg += '<br><span class="formulaError"> Missing Operator </span><br>';
        }

        if(model.has('field1')){
            msg += '<span class="formulaField">('+model.get('field1')+')</span>';
        } else {
            msg += '<span class="formulaError"> (Missing Field) </span>';
        }

        if(model.has('comp')){
            msg += '<span class="formulaComparison"> '+model.get('comp')+' </span>';
        } else {
            msg += '<span class="formulaError"> Missing Operator </span>';
        }

        if(model.has('field2')){
            msg += '<span class="formulaField">('+model.get('field2')+')</span>';
        } else {
            msg += '<span class="formulaError"> (Missing Field) </span>';
        }
        });
        this.$('#previewFormula').html(msg);
        },

        onRender: function(){
            var self = this;
            this.$('.field-1').select2({
                placeholder: 'Field',
                data: self.fieldsArr,
                templateResult: function(node) {
                    var styleText = "padding-left:" + (20 * node.level) + "px;";
                    if(node.disabled){
                        styleText += "font-weight: bold;";
                    }
                    var $result = $('<span style="'+styleText+'">' + node.text + '</span>');
                    return $result;
                }
            });
            this.$('.comparisonOp').select2({
                placeholder: 'Operator'
            });
            this.$('.field-2').select2({
                tags:true,
                placeholder: 'Constant/Field',
                data: self.fieldsArr,
                templateResult: function(node) {
                    var styleText = "padding-left:" + (20 * node.level) + "px;";
                    if(node.disabled){
                        styleText += "font-weight: bold;";
                    }
                    var $result = $('<span style="'+styleText+'">' + node.text + '</span>');
                    return $result;
                }
            });
            this.$("#rule-name").val(this.ruleName);
            if(this.rulesArr.length){
                var obj = this.firstFormulaModel;
                this.$el.find('.field-1').select2('val',obj.get('field1'));
                this.$el.find('.comparisonOp').select2('val',obj.get('comp'));
                if(!_.find(this.fieldsArr, {text: obj.get('field2')})){
                    this.$el.find('.field-2').append('<option value="'+obj.get('field2')+'">'+obj.get('field2')+'</option>');
                }
                this.$el.find('.field-2').select2('val',obj.get('field2'));
            }
            this.$('.sinkConnect').select2();
            if(this.sinkConnected){
                var arr = [];
                _.each(this.ruleConnectsTo, function(obj){
                    arr.push(obj.name);
                });
                this.$('.sinkConnect').select2('val', arr);
            }
            this.generateFormula(this.collection.models);
            this.addRuleActionButtons();
        },
        addRuleActionButtons: function(){
          var self = this;
          if(!$('#saveRule').length){
            var html = '<button class="rule-action btn btn-default" id="closeRule">Cancel</button>'+
                     '<button class="rule-action btn btn-success" id="saveRule">Save</button>';
            $('.modal-footer').append(html);
            $('#saveRule').on('click', function(e){
                self.evAddRule(e);
            });
            $('#closeRule').on('click', function(e){
                self.evCloseRule(e);
            });
          }
        },
        evAddRow: function(){
            this.collection.add(new Backbone.Model({id: this.rowId++}));
            this.generateFormula(this.collection.models);
        },
        evChange: function(e){
            if(e.currentTarget){
                var currentTarget = $(e.currentTarget);
                if(currentTarget.data().row == 1){
                    this.firstFormulaModel.set(currentTarget.data().rowtype, currentTarget.val());
                } else {
                    this.collection.get(currentTarget.data().row).set(currentTarget.data().rowtype, currentTarget.val());
                }
            } else {
                this.collection = e.models;
            }

            var tempArr = [];
            tempArr.push(this.firstFormulaModel);
            Array.prototype.push.apply(tempArr, this.collection.models);
            this.vent.trigger('change:Formula', {
                models: tempArr
            });
        },
        evAddRule: function(e) {
            var self = this,
            tempArr = [],
            ruleName = this.$("#rule-name").val(),
            ruleConnectsTo = this.$(".sinkConnect").val();

            tempArr.push(this.firstFormulaModel);
              if(this.collection.length)
                Array.prototype.push.apply(tempArr, this.collection.models);

            if(self.validate(tempArr, ruleName)) {
                self.ruleName = ruleName;
                var actionArr = [];
                _.each(ruleConnectsTo, function(name){
                    actionArr.push({name: name});
                });
                self.vent.trigger('RuleLayout:addRule', {
                    models: tempArr,
                    ruleId: self.ruleId,
                    ruleName: self.ruleName,
                    ruleConnectsTo: actionArr
                });

             } else {
                Utils.notifyError('Some fields are empty.');
                return false;
            }

        },
        evCloseRule: function() {
            this.vent.trigger('RuleLayout:closeRule', {});
        },

        validate: function(formulaArr, ruleName) {

        var flag = true;

        if(_.isEmpty(ruleName)) {
            flag = false;
        }

        _.each(formulaArr, function(model){
            if(flag && model.get('field1') && model.get('comp') && model.get('field2')){
                if(!model.get('firstModel')){
                    if(!model.get('logical')){
                        flag = false;
                    }
                }
            } else {
            flag = false;
            }
        });
        return flag;
        }
    });
    return FormulaCompositeView;
});