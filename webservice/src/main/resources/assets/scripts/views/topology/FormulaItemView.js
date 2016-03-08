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
            var self = this;
            this.$('.field-1').select2({
                placeholder: 'Field',
                data: self.fieldsArr,
                templateResult: function(node) {
                    var styleText = "padding-left:" + (20 * node.level) + "px;";
                    if(node.disabled){
                        styleText += "font-weight: bold;"
                    }
                    var $result = $('<span style="'+styleText+'">' + node.text + '</span>');
                    return $result;
                }
            });
            this.$('.comparisonOp').select2({
                placeholder: 'Operator'
            });
            this.$('.logicalOp').select2({
                placeholder: 'AND/OR'
            });
            this.$('.field-2').select2({
                tags:true,
                placeholder: 'Constant/Field',
                data: self.fieldsArr,
                templateResult: function(node) {
                    var styleText = "padding-left:" + (20 * node.level) + "px;";
                    if(node.disabled){
                        styleText += "font-weight: bold;"
                    }
                    var $result = $('<span style="'+styleText+'">' + node.text + '</span>');
                    return $result;
                }
            });
            if(this.collection.models.length){
                var obj = this.collection.at(this.id-2);
                if(!_.isUndefined(obj)){
                    this.$el.find('.logicalOp').select2('val',obj.get('logical'));
                    this.$el.find('.field-1').select2('val',obj.get('field1'));
                    this.$el.find('.comparisonOp').select2('val',obj.get('comp'));
                    if(!_.find(this.fieldsArr, {val: obj.get('field2')})){
                        this.$el.find('.field-2').append('<option value="'+obj.get('secondOperand')+'">'+obj.get('field2')+'</option>');
                    }
                    this.$el.find('.field-2').select2('val',obj.get('field2'));
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
