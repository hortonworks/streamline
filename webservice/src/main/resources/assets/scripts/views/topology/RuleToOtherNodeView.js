define(['require',
		'hbs!tmpl/topology/ruleToOtherNodeView',
		'utils/TopologyUtils'], function(require, tmpl, TopologyUtils){
	'use strict';
	var ruleToOtherNodeView = Marionette.LayoutView.extend({
		template: tmpl,

		templateHelper: function(){
			return {

			};
		},

		regions: {},

		events: {
			'click li.check-rule': 'evSelectRule',
			'click #btnCancel': 'evCancelAction',
			'click #btnAdd': 'evSaveAction'
		},

		initialize: function(options){
			_.extend(this, options);
			if(this.currentType === 'RULE'){
				this.rulesProcessor = this.processorObj[0];
				this.syncRuleData();
			} else if(this.currentType === 'CUSTOM'){
				this.customProcessor = this.processorObj[0];
				this.syncCustomData();
			}
		},

		syncRuleData: function(){
			var ruleProcessor = this.rulesProcessor.newConfig ? this.rulesProcessor.newConfig.rulesProcessorConfig : this.rulesProcessor.rulesProcessorConfig;
			var arr = [];
			_.each(ruleProcessor.rules, function(o, i){
				var obj = {
					config: this.getRuleExpressions(o.condition.expression),
					ruleConnectsTo: o.actions ? o.actions : [],
					ruleId: i,
					ruleName: o.name
				};
				arr.push(new Backbone.Model(obj));
			}, this);
			this.html = TopologyUtils.generateFormulaPreview(arr, true);

		},
		getRuleExpressions: function(expression) {
			var tempArr = [],
				tempObj = expression,
				flag = true;

            while(flag) {
              if(tempObj.first.value) {
                flag = false;
                tempArr.push({
                  operation: tempObj.operator,
                  firstOperand: {
                  	name: tempObj.first.value.name
                  },
                  secondOperand: tempObj.second.value.name ? tempObj.second.value.name : tempObj.second.value
                });
                continue;
              }
              var secondObj = tempObj.second;
                tempArr.push({
                  operation: secondObj.operator,
                  firstOperand: {
                  	name: secondObj.first.value.name
                  },
                  secondOperand: secondObj.second.value.name ? secondObj.second.value.name : secondObj.second.value,
                  logicalOperator: tempObj.operator
                });
              tempObj = tempObj.first;
            }

          tempArr = tempArr.reverse();
          tempArr[0].firstModel = true;
          return tempArr;
		},
		syncCustomData: function(){
			var self = this;
			this.html = '';
			var outputStreams = _.findWhere(this.customProcessor.config, {name: "outputStreamToSchema"});
			if(outputStreams){
				var keys = _.keys(outputStreams.defaultValue);
				for(var i = 0; i < keys.length; i++){
					self.html += '<li data-id="' + keys[i] +
	                '" class="check-rule list-group-item"><div class="row"><div class="col-sm-11"><b>' + keys[i] +
	                ':  </b>' + JSON.stringify(outputStreams.defaultValue[keys[i]]) + '</div>'+
	                '<div class="btn-group btn-group-sm col-sm-1"><i class="fa fa-check"></i></div></div></li>';
				}
			}
		},
		onRender: function(){
			this.$('.ruleList').append(this.html);
		},
		evSelectRule: function(e){
			var currentTarget = $(e.currentTarget);
			currentTarget.toggleClass('selected');
			currentTarget.find('.fa-check').toggleClass('selected');
		},
		evSaveAction: function(e){
			var self = this;
			var selectedElem = this.$el.find('.check-rule.selected');
			if(this.currentType === 'RULE'){
				var obj = this.rulesProcessor.newConfig ? this.rulesProcessor.newConfig.rulesProcessorConfig : this.rulesProcessor.rulesProcessorConfig;
				_.each(selectedElem, function(e){
					var id = $(e).data().id;
					if(_.isArray(obj.rules[id].actions)){
						obj.rules[id].actions.push({name: self.sinkName});
					} else {
						obj.rules[id].actions = [{name: self.sinkName}];
					}
				});
				this.vent.trigger('topologyEditor:SaveProcessor', new Backbone.Model(this.rulesProcessor));
			} else if(this.currentType === 'CUSTOM'){
				_.each(selectedElem, function(e){
					var id = $(e).data().id;
					if(_.isArray(self.customProcessor.selectedStreams)){
						self.customProcessor.selectedStreams.push({name: self.sinkName, streamName: id});
					} else {
						self.customProcessor.selectedStreams = [{name: self.sinkName, streamName: id}];
					}
				});
			}
			this.evCancelAction();
		},
		evCancelAction: function(e){
			this.trigger('closeModal');
		}
	});
	return ruleToOtherNodeView;
});