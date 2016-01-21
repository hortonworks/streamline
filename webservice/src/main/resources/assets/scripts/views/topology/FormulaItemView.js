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
            this.id = this.model.get('id');
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
            if(this.rulesArr.length > 1){
                var obj = this.rulesArr[this.id-1];
                if(!_.isUndefined(obj)){
                    this.$el.find('.logicalOp').select2('val',this.rulesArr[this.id-2].logicalOperator);
                    this.$el.find('.field-1').select2('val',obj.firstOperand.name);
                    this.$el.find('.comparisonOp').select2('val',obj.operation);
                    if(!_.find(this.fieldsArr, {val: obj.secondOperand})){
                        this.$el.find('.field-2').append('<option value="'+obj.secondOperand+'">'+obj.secondOperand+'</option>');
                    }
                    this.$el.find('.field-2').select2('val',obj.secondOperand);
                }
            }
        },

        evDelete: function() {
            this.collection.remove(this.model.id);
            this.vent.trigger('delete:Formula', {
                models: this.collection
            });
        }
    });
});
