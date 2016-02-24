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
			this.rulesProcessor = this.ruleProcessorObj[0];
			this.syncRuleData();
		},

		syncRuleData: function(){
			var ruleProcessor = this.rulesProcessor.newConfig ? this.rulesProcessor.newConfig.rulesProcessorConfig : this.rulesProcessor.rulesProcessorConfig;
			var arr = [];
			_.each(ruleProcessor.rules, function(o, i){
				var obj = {
					config: o.condition.conditionElements,
					ruleConnectsTo: o.actions ? o.actions : [],
					ruleId: i,
					ruleName: o.name
				};
				arr.push(new Backbone.Model(obj));
			});
			this.html = TopologyUtils.generateFormulaPreview(arr, true);
			
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
			this.evCancelAction();
		},
		evCancelAction: function(e){
			this.trigger('closeModal');
		}
	});
	return ruleToOtherNodeView;
});