define(['require',
    'utils/Globals',
    'utils/TopologyUtils',
    'hbs!tmpl/topology/ruleLayoutView',
    'views/topology/FormulaCompositeView'
], function(require, Globals, TopologyUtils, tmpl, FormulaCompositeView){
    'use strict';
    var RuleLayoutView = Marionette.LayoutView.extend({
    template: tmpl,

    regions: {
        ruleForm: '.ruleForm'
    },

    events: {
        'click #addRule': 'evAddRule',
        'click #deleteRule': 'evDelRule',
        'click #editRule': 'evEditRule'
    },

    initialize: function(options){
        _.extend(this, options);
        this.bindEvents();
        this.ruleCount = 1;
        this.ruleArr = [];
        this.prepareRule();
        this.sinkConnected = false;
        if(this.linkedToRule.length){
          this.sinkConnected = true;
        }
        this.isEditState = false;
        this.vent.trigger('RuleProcessor:update', this.ruleCollection);
    },

    prepareRule: function(){
      this.ruleCollection = new Backbone.Collection();
        for (var i = 0;i < this.rulesArr.length;i++) {
          var ruleObj = this.rulesArr[i],
              expression = ruleObj.condition.expression,
              tempArr = [];

          var tempObj = expression;
          var flag = true;

            while(flag) {
              if(tempObj.first.value) {
                flag = false;
                tempArr.push(new Backbone.Model({
                  operation: tempObj.operator,
                  firstOperand: tempObj.first.value.name,
                  secondOperand: tempObj.second.value.name ? tempObj.second.value.name : tempObj.second.value
                }));
                continue;
              }
              var secondObj = tempObj.second;
                tempArr.push(new Backbone.Model({
                  operation: secondObj.operator,
                  firstOperand: secondObj.first.value.name,
                  secondOperand: secondObj.second.value.name ? secondObj.second.value.name : secondObj.second.value,
                  logicalOperator: tempObj.operator
                }));
              tempObj = tempObj.first;
            }

          tempArr = tempArr.reverse();
          tempArr[0].set('firstModel', true);
          var ruleModel = new Backbone.Model({
            config: tempArr,
            ruleName: ruleObj.name,
            ruleId: this.ruleCount++,
            ruleConnectsTo: ruleObj.actions
          });

          this.ruleCollection.push(ruleModel);

        }

      },

    onRender: function(){
      var rulePreview = '',
        ruleCollection = this.ruleCollection.models;
      if(!ruleCollection.length)
        this.$(".rule-list-heading").append('<h5>No rules found</h5>');

      this.$(".ruleList").append(TopologyUtils.generateFormulaPreview(ruleCollection));
      if(!this.editMode) {
        this.$("#addRule, button").toggleClass("displayNone");
      }
    },

    bindEvents: function() {
        var self = this,
        ruleModelArr = [];

        this.listenTo(this.vent, 'change:Formula', function(data){
            self.generateFormula(data.models);
        });

        this.listenTo(this.vent, 'RuleLayout:addRule', function(data){

                var models = data.models,
                    rulePreview = '',
                    tempObj,
                    tempArr = [],
                    ruleModel;

                _.each(models, function(model, id) {
                    tempObj = {
                      firstOperand: '',
                      secondOperand: '',
                      operation: ''
                    };
                    if(model.get('firstModel')) {
                      _.extend(tempObj, {firstModel: model.get("firstModel")});
                    } else {
                      _.extend(tempObj, {logicalOperator: model.get("logical")});
                    }

                  tempObj.firstOperand = model.get("field1");
                  tempObj.secondOperand = model.get("field2");
                  tempObj.operation = model.get("comp");
                  tempArr.push(new Backbone.Model(tempObj));
                });

          if(self.isEditState) {
            ruleModel = _.find(self.ruleCollection.models, function(model) {
            if(model.get('ruleId') == data.ruleId) {
                return model;
              }
            });
            ruleModel.set("config", tempArr);
            ruleModel.set("ruleName", data.ruleName);
            ruleModel.set("ruleConnectsTo", data.ruleConnectsTo);
          } else {
            ruleModel = new Backbone.Model({
                  config: tempArr,
                  ruleId: self.ruleCount++,
                  ruleName: data.ruleName,
                  ruleConnectsTo: data.ruleConnectsTo
                });
            self.ruleCollection.add(ruleModel);
          }
            self.isEditState = false;
            self.closeRuleForm();
            self.render();
            self.vent.trigger('RuleProcessor:update', self.ruleCollection);
        });

        this.listenTo(this.vent, 'RuleLayout:closeRule', function(){
          self.isEditState = false;
          self.closeRuleForm();
        });
    },

    generateFormula: function(models){

      var self = this;
      var msg = '';
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

    evAddRule: function(){
        var self = this;
        this.view = new FormulaCompositeView({
          vent: self.vent,
          fields: self.fields,
          config: self.config,
          rulesArr: [],
          ruleId: self.ruleCount,
          ruleName: '',
          sinkConnected: self.sinkConnected,
          connectedSink: self.connectedSink
        });
        this.ruleForm.show(this.view);
        this.$("#addRule, .btn-action").addClass("hidden").hide();
        $('#saveRule, #closeRule').show();
        $("#btnAdd, #btnCancel").hide();
        this.$(".ruleForm").addClass('panel panel-default');
    },

    evEditRule: function(e) {
      var self = this,
          ruleId = $(e.currentTarget).data().id;

      var ruleModel = _.find(this.ruleCollection.models, function(model) {
        if(model.get('ruleId') == ruleId) {
          return model;
        }
      });
      this.isEditState = true;
      this.view = new FormulaCompositeView({
        vent: self.vent,
        fields: self.fields,
        config: self.config,
        rulesArr: ruleModel.get('config'),
        ruleId: ruleModel.get('ruleId'),
        ruleName: ruleModel.get('ruleName'),
        sinkConnected: self.sinkConnected,
        connectedSink: self.connectedSink,
        ruleConnectsTo: ruleModel.get('ruleConnectsTo')
      });
      this.ruleForm.show(this.view);

      this.$("#addRule, .btn-action").addClass("hidden").hide();
      $("#btnAdd, #btnCancel").hide();
      $('#saveRule, #closeRule').show();
      this.$(".ruleForm").addClass('panel panel-default');
    },

    evDelRule: function(e) {
     var ruleId = $(e.currentTarget).data().id;

     var model = _.find(this.ruleCollection.models, function(model) {
          if(model.get('ruleId') == ruleId) {
            return model;
          }
      });
      model.destroy();
      this.vent.trigger('RuleProcessor:update', this.ruleCollection);
      this.render();
    },

    closeRuleForm: function() {
        this.$(".ruleForm").html('');
        this.$("#addRule, .btn-action").removeClass("hidden").show();
        this.$(".ruleForm").removeClass('panel panel-default');
        $('#saveRule, #closeRule').hide();
        $("#btnAdd, #btnCancel").show();
    },

    validate: function(){

      if(!this.ruleCollection.models.length)
        return false;
      else return true;
    }

    });
    return RuleLayoutView;
});