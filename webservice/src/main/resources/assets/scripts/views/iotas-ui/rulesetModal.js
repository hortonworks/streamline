define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'hbs!tmpl/iotas-ui/ruleset',
  'jsPlumb',
  'codemirror',
  'sql'
], function(require, vent, localization, tmpl, jsPlumb, codemirror, sql) {
  'use strict';

  var rulesetModal = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'change #isAlert': 'evIsAlert',
      'click .fa-save': 'evSaveRule',
    },

    evSaveRule:function(e){
      console.log('save rule');
    },
    evIsAlert:function(e){
      console.log('is alert');
    },
    ui: {
      'AddNewExpn'   : '#AddNewExpn',
      'expression'   : '#expression1',
      'expressionDef': '#equation',
      'saveRule'      : '#saveRule',
      'clearRule'      : '#clearRule',
      'action'       : '#action',
      'formula'      : '#formula',
      'processorTitle': '#processorTitle input',
      'processorList' :  '.processorList tbody',
      'ruleSet' :  '#ruleSet',
      'ruleName' :  '#ruleName',
      'expType' : '#expType',
      'code'   : '#code',
      'resize'   : '#resize',
      'result'   : '#result',
      'editor'   : '.editor',
      'list'   : '.list',
      'rules'   : '#rules',
    },

    regions: {
    },

    initialize: function(collection) {
        this.availableFields = collection;
        this.expressionList = [];
        this.processorSet = [];
        this.alertFunctions = ["is greater than", "is lesses than", "is equal to"];
        this.logicalFunctions = ["AND", "OR"];
        this.expid = 1;
        this.expressionDivs = [];
    },

    onRender:function(){
        var that = this;
        this.displayAvailableFields($(this.ui.expression));
        $(this.ui.expression).find('select').select2();
        this.displayExpressionDiv($(this.ui.expression));

        this.ui.resize.find('.fa-chevron-right').hide();
        this.ui.rules.find('#action').hide();
        this.ui.rules.find('#expDef').hide();
        this.ui.rules.find('.fa-save').parent().hide();

        this.ui.rules.find('#ruleName').on('change', function(){
            console.log('change:ruleName');
            if(this.value != ""){
              that.ui.rules.find('#action').show();
            }else{
              that.ui.rules.find('#action').hide();
              that.ui.rules.find('#action').val("");
              that.ui.rules.find('#expDef').hide();
              that.removeAddedExpressions();
            }
        });

        this.ui.rules.find('#action').on('change', function(){
            console.log('change : action');
            if(this.value != ""){
                that.ui.rules.find('#expDef').show();
            }else{
                that.ui.rules.find('#expDef').hide();
            }
        });

        this.ui.rules.find('.fa-save');

        this.ui.rules.find('').on('change', function(){

        });
        $(this.ui.resize).find('.fa-chevron-right').on('click', function() {
            console.log('show rulelist');
            $(that.ui.resize).find('.fa-chevron-left').show();
            $(that.ui.resize).find('.fa-chevron-right').hide();
            $(that.ui.list).show();
            $(that.ui.editor).removeClass('col-md-11').addClass('col-md-8');
        });
        $(this.ui.resize).find('.fa-chevron-left').on('click', function() {
            console.log('hide rulelist');
            $(that.ui.resize).find('.fa-chevron-right').show();
            $(that.ui.resize).find('.fa-chevron-left').hide();
            $(that.ui.list).hide();
            $(that.ui.editor).removeClass('col-md-8').addClass('col-md-11');
        });

        $(this.ui.AddNewExpn).on('click', function() {
            that.expid++;
            var row = $(this).parent().parent();
                row.after('<div id="expression'+that.expid+'">'+
                              '<div class="row row-margin-bottom">'+
                                  '<div class="col-md-3">'+
                                      '<select id="conditionalOp" class="form-control">'+
                                          '<option selected disabled value="">Select a function</option>'+
                                          '<option value="AND">AND</option>'+
                                          '<option value="OR">OR</option>'+
                                      '</select>'+
                                  '</div>'+
                              '</div>'+
                              '<div class="row row-margin-bottom" style="margin-right: -10px;margin-bottom: 10px;">'+
                                  '<ul class="expression col-md-11">'+
                                      '<li class="paraAlert" id="field1">'+
                                          '<select class="form-control" style="width:150px;">'+
                                              '<option value="" selected disabled>Select a field...</option>'+
                                          '</select>'+
                                      '</li>'+
                                      '<li class="para" id="operation">'+
                                          '<select class="form-control pull-left" style="width:150px;">'+
                                              '<option value="greater than">greater than</option>'+
                                              '<option value="less than">less than</option>'+
                                              '<option value="equal to">equal to</option>'+
                                              '<option value="not equal to">not equal to</option>'+
                                              '<option value="contains">contains</option>'+
                                          '</select>'+
                                      '</li>'+
                                      '<li class="paraAlert" id="constant">'+
                                          '<input type="text" class="form-control" placeHolder="Enter constant" style="width:100px;"></input>'+
                                      '</li>'+
                                      '<li class="para" id="field2">'+
                                          '<select class="form-control" style="width:150px;">'+
                                              '<option value="" selected disabled>Select a field...</option>'+
                                              '<option value="Use a constant">Use a constant</option>'+
                                          '</select>'+
                                      '</li>'+
                                  '</ul>'+
                                  '<div class="col-md-1">'+
                                      '<button class="btn btn-sm btn-danger" id="removeNewRule">'+
                                      '<i class="fa fa-remove"></i>'+
                                      '</button>'+
                                  '</div>'+
                              '</div>'+
                          '</div>'
                      );
                  that.displayAvailableFields($('#expression'+that.expid));
                  $('#expression'+that.expid).find('select').select2();
                  that.displayExpressionDiv($('#expression'+that.expid))
                  that.expressionDivs.push($('#expression'+that.expid));

                  $('#removeNewRule').on('click', function() {
                      $(this).parent().parent().parent().remove();
                      that.expid--;
                  });
          });

          $(this.ui.saveRule).on('click', function(){
              var actions = that.ui.action.val();
              console.log('click save');
              var tempProcessor = {'id':that.ui.ruleName.val(),
                                      'formula':that.calculateFormula(),
                                      'action' : actions,
                                    };
              that.processorSet.push(tempProcessor);
              if(actions == 'Store'){
                actions = '</i><i class = "fa fa-database">';
              }else if(actions == 'Notify'){
                actions = '<i class = "fa fa-bell">';
              }else if (actions == 'Process') {
                actions = '<i class = "fa fa-cog">';
              }
              $(that.ui.processorList).append('<tr>'+
                                                  '<td>'+tempProcessor.id+'</td>'+
                                                  '<td>'+actions+'</td>'+
                                              '</tr>');
              that.clearRuleDef();
          });

          $(this.ui.clearRule).on('click', function(){
              console.log('click clear');
              that.clearRuleDef();
          });

          var c = codemirror.fromTextArea(this.$el.find('#code')[0] , {
              mode: 'text/x-sql',
              lineNumbers: true,
              extraKeys: {"Ctrl-Space": "autocomplete"},
        });
      },

      removeAddedExpressions:function(){
          for (var i = 0; i < this.expressionDivs.length; i++) {
            this.expressionDivs[i].remove();
          };
      },

      clearRuleDef:function(){
          this.ui.rules.find('#ruleName').val("");
          this.ui.rules.find('#action').val("");
          this.ui.rules.find('#constant').val("");
          this.ui.rules.find('#action').hide();
          this.removeAddedExpressions();
          this.ui.rules.find('#field1 select').val(null).trigger('change');
          this.ui.rules.find('#operation select').val(null).trigger('change');
          this.ui.rules.find('#field2 select').val(null).trigger('change');
          this.ui.rules.find('#field1').hide();
          this.ui.rules.find('#field2').hide();
          this.ui.rules.find('#constant').hide();
          this.ui.rules.find('.fa-save').parent().hide();
          this.ui.formula.val("");
          this.expressionList = [];
          this.displayExpressionDiv($(this.ui.expression));
      },

      displayExpressionDiv:function(div){
          var that = this;
          var tempExpression = {
            'conditionalOp':"",
            'field1':"",
            'operation':"",
            'field2':"",
          }

          that.expressionList.push(tempExpression);
          //div.find('select').select2();

          div.find('#field1').hide();
          div.find('#field2').hide();
          div.find('#constant').hide();

          div.find('#field1 select').on('change', function(){
              if(this.value!= null && this.value!= ''){
                  tempExpression.field1 = this.value;
                  if(div.find('#field2 select').val() != null && div.find('#field1 select').val() != null){
                      that.ui.formula.val(that.calculateFormula(false));
                      that.ui.rules.find('.fa-save').parent().show();
                  }
              }
          });

          div.find('#conditionalOp').on('change', function(){
                  tempExpression.conditionalOp = this.value;
                  div.find('#field1 select').val(null).trigger('change');
                  div.find('#field2 select').val(null).trigger('change');
                  div.find('#operation select').val(null).trigger('change');
                  that.ui.rules.find('.fa-save').parent().hide();
                  that.ui.formula.val("");
          });

          div.find('#operation select').on('change', function(){
              if(this.value!= null && this.value!= ''){
                  tempExpression.operation = this.value;
                  if (this.selectedIndex <= 12 && this.selectedIndex >= 0) {
                      div.find('#field1 select').val(null).trigger('change');
                      div.find('#field2 select').val(null).trigger('change');
                      div.find('#field2').show();
                      div.find('#field1').show();
                      that.ui.rules.find('.fa-save').parent().hide();
                  } /*else {
                      div.find('#field1').hide();
                      div.find('#field2').show();
                      div.find('#field2 select').val(null).trigger('change');
                      that.ui.formula.val("");
                      that.ui.rules.find('.fa-save').parent().hide();
                  }*/
              }
          });

          div.find('#field2 select').on('change', function(){
              if(this.value!= null && this.value!= ''){
                  if(div.find('#field1 select').val() == null && div.find('#field2 select').val() != null && div.find('#field2 select').val() != 'Use a constant'){
                       tempExpression.field2 = this.value;
                       that.ui.formula.val(that.calculateFormula(true));
                       that.ui.rules.find('.fa-save').parent().show();
                  }else if(div.find('#field1 select').val() != null && div.find('#field2 select').val() != 'Use a constant'){
                      tempExpression.field2 = this.value;
                      that.ui.formula.val(that.calculateFormula(false));
                      that.ui.rules.find('.fa-save').parent().show();
                  }
                  if(div.find('#field2 select').val() == 'Use a constant'){
                      div.find('#constant').show();
                  }else{
                    div.find('#constant input').val(null);
                    div.find('#constant').hide();
                  }
              }
          });

          div.find('#constant').on('change', function(){
              tempExpression.field2 = div.find('#constant input').val();
              if(div.find('#field1 select').val() != "" && div.find('#constant input').val() != ""){
                  that.ui.formula.val(that.calculateFormula());
                  that.ui.rules.find('.fa-save').parent().show();
              }
          });

          this.expressionDiv = div;
      },

      displayAvailableFields:function(divId){
          for (var i = this.availableFields.length - 1; i >= 0; i--) {
              $(divId).find('#field1 select').append('<option data-value="'+this.availableFields[i]+'">'+this.availableFields[i]+'</li>');
          };
          for (var i = this.availableFields.length - 1; i >= 0; i--) {
              $(divId).find('#field2 select').append('<option data-value="'+this.availableFields[i]+'">'+this.availableFields[i]+'</li>')
          }
      },

      calculateFormula:function(){
          var tempList = this.expressionList;
          var finalFormula = "";
          for (var i = 0; i < tempList.length; i++) {
              var temp =  tempList[i];
              if ( ( temp.field1 != "" ) && ( temp.operation != "" ) && ( temp.field2 != "" ) ) {
                  var formula = " is ( "+temp.field1+" ) "+temp.operation+" ( "+temp.field2+" )";
                  if (temp.conditionalOp != "") {
                    formula = "\n   "+temp.conditionalOp+"\n"+formula;
                  };
                  finalFormula = finalFormula.concat(formula);
              }else if ( ( temp.field2 != "" ) && ( temp.operation != "" ) ) {
                  var formula = " "+temp.operation+" ( "+temp.field2+" )";
                  if (temp.conditionalOp != "") {
                    formula = "\n   "+temp.conditionalOp+"\n"+formula;
                  };
                  finalFormula = finalFormula.concat(formula);
              }
          };

          return(finalFormula);
      },
  });
  return rulesetModal;
});