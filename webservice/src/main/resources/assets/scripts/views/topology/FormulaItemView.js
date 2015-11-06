define(['require',
    'utils/Globals',
    'hbs!tmpl/topology/formulaItemView'
],function(require, Globals, tmpl) {
    'use strict';

    return Marionette.ItemView.extend(
    {
        template: tmpl,
        templateHelpers: function(){
            return {
                comparisonArr : this.comparisonArr,
                logicalArr : this.logicalArr,
                id: this.id
            };
        },
        events: {
            'click .btnDelete': 'evDelete',
            'change .ruleRow': 'evChange'
        },

        initialize: function(options) {
            _.extend(this, options);
        },

        onRender: function() {

        },

        evChange: function(e){
            var currentTarget = $(e.currentTarget); 
            this.vent.trigger('change:Formula', {
                rowType: currentTarget.data().rowtype,
                value: currentTarget.val(),
                rowNum: currentTarget.data().row
            });
        },

        evDelete: function() {
            this.model.destroy();
        }
    });
});
