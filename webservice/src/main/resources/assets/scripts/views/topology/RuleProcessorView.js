define(['require',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'modules/Modal',
  'models/VParser',
  'models/VDatasource',
  'hbs!tmpl/topology/ruleProcessorView'
], function(require, localization, Utils, Globals, Modal, VParser, VDatasource, tmpl) {
  'use strict';

  var RuleProcessorView = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #btnCancel': 'evClose',
      'click #btnAdd': 'evAdd'
    },

    regions: {
      formulaForm: '.formulaForm'
    },

    initialize: function(options) {
      _.extend(this, options);
      this.bindEvents();
      if(this.model.has('declaredInput')){
          //For RULE-TO-RULE
        this.fieldsArr = this.model.get("declaredInput");
      } else {
        //For PARSER-TO-RULE
        this.getFeedSchema();
      }
    },

    getFeedSchema:function (options){
      var self = this;
      this.parserModel = new VParser();

      var dsModel = new VDatasource();
      dsModel.set('dataSourceId', this.model.get('dataSourceId'));
      dsModel.set('id', this.model.get('dataSourceId'));
      dsModel.fetch({async: false});

      this.parserModel.getSchema({
        parserId: dsModel.get('entity').parserId,
        async: false,
        success: function(model, response, options){
          self.fieldsArr = model.entity.fields;
        },
        error: function(model, response, options){
          Utils.showError(model, response);
        }
      });
    },

    bindEvents: function(){
      var self = this;
      this.listenTo(this.vent, 'RuleProcessor:update', function(data){
          self.ruleCollection = data.models;
      });
    },

    onRender:function(){
      var self = this;
      this.$('[data-rel="tooltip"]').tooltip();
      require(['views/topology/RuleLayoutView'], function(RuleLayoutView){

        self.view = new RuleLayoutView({
          vent: self.vent,
          fields: self.fieldsArr,
          config: self.model.get('config'),
          rulesArr: self.model.has('newConfig') ? self.model.attributes.newConfig.rulesProcessorConfig.rules : [],
          linkedToRule: self.linkedToRule,
          connectedSink: self.connectedSink
        });
        self.formulaForm.show(self.view);
      });
    },
    evAdd: function(e){
      if(this.view.validate()){
        var ruleArr = [];

        for(var i = 0;i < this.ruleCollection.length; i++) {
          var ruleConfig = this.ruleCollection[i].get("config"),
              ruleName = this.ruleCollection[i].get("ruleName"),
              actions = [];
          if(this.ruleCollection[i].has('ruleConnectsTo')){
            actions = this.ruleCollection[i].get('ruleConnectsTo');
          }
          var id = new Date().getTime();
          var ruleObj = {
              name: ruleName,
              id: id+(i+1),
              ruleProcessorName: this.model.get('uiname'),
              condition: {
                conditionElements: []
              },
              actions: actions,
              description: "Auto-Generated for "+this.model.get('uiname')
            };

          _.each(ruleConfig, function(model){
            var obj = {
              firstOperand: {
                name: model.get('firstOperand'),
                type: 'INTEGER'
              },
              operation: model.get('operation'),
              secondOperand: model.get('secondOperand')
            };
            if(!model.get('firstModel')){
              ruleObj.condition.conditionElements[ruleObj.condition.conditionElements.length - 1].logicalOperator = (model.has('logicalOperator') ? model.get('logicalOperator') : null);
            }
            ruleObj.condition.conditionElements.push(obj);
          });
          ruleArr.push(ruleObj);
        }

        var config = {
          parallelism: 1,
          rulesProcessorConfig: {
            rules: ruleArr,
            name: this.model.get('uiname'),
            id: new Date().getTime(),
            description: "Auto-Generated for "+this.model.get('uiname'),
            declaredInput: this.fieldsArr
          }
        };
        this.model.set('firstTime', false);
        this.model.set('newConfig', config);
        this.vent.trigger('topologyEditor:SaveProcessor', this.model);
        this.evClose();
      } else {
        Utils.notifyError('Add the rule to processor');
        return false;
      }
    },

    evClose: function(e){
      this.stopListening(this.vent, 'RuleLayout:addRule');
      this.stopListening(this.vent, 'RuleLayout:closeRule');
      this.stopListening(this.vent, 'RuleProcessor:update');
      this.trigger('closeModal');
    }

  });

  return RuleProcessorView;
});