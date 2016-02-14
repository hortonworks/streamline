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
      this.getFeedSchema();
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
          self.parserModel.schema = model.entity.fields;
        },
        error: function(model, response, options){
          Utils.showError(model, response);
        }
      });
    },

    bindEvents: function(){
      var self = this;
      this.listenTo(this.vent, 'change:Formula', function(data){
        self.generateForumla(data.models);
      });
    },

    generateForumla: function(models){
      this.formulaModelArr = models;
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

    onRender:function(){
      var self = this;
      this.$('[data-rel="tooltip"]').tooltip();
      require(['views/topology/FormulaCompositeView'], function(FormulaCompositeView){
        self.view = new FormulaCompositeView({
          vent: self.vent,
          fields: self.parserModel.schema,
          config: self.model.get('config'),
          rulesArr: self.model.has('newConfig') ? self.model.attributes.newConfig.rulesProcessorConfig.rules[0].condition.conditionElements : []
        });
        self.formulaForm.show(self.view);
      });
    },
    evAdd: function(e){
      if(this.validateAll()){
        var ruleObj = {
          name: this.model.get('uiname'),
          id: new Date().getTime(),
          ruleProcessorName: this.model.get('uiname'),
          condition: {
            conditionElements: []
          },
          actions: (this.model.has('newConfig') ? this.model.get('newConfig').rulesProcessorConfig.rules[0].actions : []),
          // action: {
          //   components: (this.model.has('newConfig')) ? this.model.get('newConfig').rulesProcessorConfig.rules[0].action.components : [],
          //   declaredOutput: this.parserModel.schema
          // },
          description: "Auto-Generated for "+this.model.get('uiname')
        };
        _.each(this.formulaModelArr, function(model){
          var obj = {
            firstOperand: {
              name: model.get('field1'),
              type: 'INTEGER'
            },
            operation: model.get('comp'),
            secondOperand: model.get('field2')
          };
          if(!model.get('firstModel')){
            ruleObj.condition.conditionElements[ruleObj.condition.conditionElements.length - 1].logicalOperator = (model.has('logical') ? model.get('logical') : null);
          }
          ruleObj.condition.conditionElements.push(obj);
        });
        
        var config = {
          parallelism: 1,
          rulesProcessorConfig: {
            rules: [ruleObj],
            name: this.model.get('uiname'),
            id: new Date().getTime(),
            description: "Auto-Generated for "+this.model.get('uiname'),
            declaredInput: this.parserModel.schema
          }
        };
        this.model.set('newConfig', config);
        this.model.formulaModelArr = this.formulaModelArr;
        this.vent.trigger('topologyEditor:SaveProcessor', this.model);
        this.evClose();
      } else {
        Utils.notifyError('Some fields are empty.');
        return false;
      }
    },
    validateAll: function(){
      var self = this;
      var flag = true;
      _.each(this.formulaModelArr, function(model){
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
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return RuleProcessorView;
});