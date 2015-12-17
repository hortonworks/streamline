define(['require',
	'utils/Globals',
	'hbs!tmpl/topology/formulaCompositeView',
	'views/topology/FormulaItemView'
], function(require, Globals, tmpl, FormulaItemView){
	'use strict';
	var FormulaCompositeView = Marionette.CompositeView.extend({
		template: tmpl,
		templateHelpers: function(){
            var self = this;
            this.comparisonOpArr = [];
            this.logicalOpArr = [];
            this.fieldsArr = [];

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
             _.each(this.fields, function(obj){
                self.fieldsArr.push({
                    val:obj.name,
                    lbl: obj.name
                 });
             });
            return {
                comparisonArr : this.comparisonOpArr,
                logicalArr : this.logicalOpArr,
                fieldsArr : this.fieldsArr
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
                id: this.rowId++,
                vent: this.vent
            };
        },

        events: {
        	'click #addNewRule': 'evAddRow',
        	'change .ruleRow': 'evChange'
        },

        initialize: function(options){
        	_.extend(this, options);
            this.firstFormulaModel = new Backbone.Model({firstModel: true});
        	this.rowId = 2; //1 is for first row already rendered
        	this.collection = new Backbone.Collection();
            this.bindEvents();
        },

        bindEvents: function(){
            var self = this;
            this.listenTo(this.vent,'delete:Formula', function(data){
                self.evChange(data);
            });
        },

        generateForumla: function(collection){
          console.log(dataCollection);
        },

        onRender: function(){
            this.$('.field-1').select2({
                placeholder: 'Field'
            });
            this.$('.comparisonOp').select2({
                placeholder: 'Operator'
            });
            this.$('.field-2').select2({
                tags:true,
                placeholder: 'Constant/Field'
            });
        },
        evAddRow: function(){
        	this.collection.add(new Backbone.Model({id: this.rowId}));
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
            // if(this.collection.length){
            Array.prototype.push.apply(tempArr, this.collection.models);
            // }
            this.vent.trigger('change:Formula', {
                models: tempArr
            });
        },
	});
	return FormulaCompositeView;
});