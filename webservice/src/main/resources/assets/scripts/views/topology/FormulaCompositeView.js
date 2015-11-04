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
            _.each(Globals.Functions.Comparison, function(obj){
                self.comparisonOpArr.push({
                    val:obj.valStr,
                    lbl: obj.value
                });
            });
            _.each(Globals.Functions.Logical, function(obj){
                self.logicalOpArr.push({
                    val:obj.valStr,
                    lbl: obj.value
                });
            });
            return {
                comparisonArr : this.comparisonOpArr,
                logicalArr : this.logicalOpArr
            };
        },

        childView: FormulaItemView,

        childViewContainer: "div[data-id='addRowDiv']",

        childViewOptions: function() {
            return {
                collection: this.collection,
                comparisonArr: this.comparisonOpArr,
                logicalArr: this.logicalOpArr,
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
        	this.rowId = 2; //1 is for first row already rendered
        	this.collection = new Backbone.Collection();
        },

        onRender: function(){},
        evAddRow: function(){
        	this.collection.add(new Backbone.Model());
        },
        evChange: function(e){
            var currentTarget = $(e.currentTarget); 
            this.vent.trigger('change:Formula', {
                rowType: currentTarget.data().rowtype,
                value: currentTarget.val(),
                rowNum: currentTarget.data().row
            });
        },
	});
	return FormulaCompositeView;
});