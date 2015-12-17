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
                fieldsArr : this.fieldsArr,
                id: this.id
            };
        },
        events: {
            'click .btnDelete': 'evDelete',
        },

        initialize: function(options) {
            _.extend(this, options);
        },

        onRender: function(){
            this.$('.field-1').select2({
                placeholder: 'Field'
            });
            this.$('.comparisonOp').select2({
                placeholder: 'Operator'
            });
            this.$('.logicalOp').select2({
                placeholder: 'AND/OR'
            });
            this.$('.field-2').select2({
                tags:true,
                placeholder: 'Constant/Field'
            });
        },

        evDelete: function() {
            this.collection.remove(this.model.id);
            this.vent.trigger('delete:Formula', {
                models: this.collection
            });
        }
    });
});
