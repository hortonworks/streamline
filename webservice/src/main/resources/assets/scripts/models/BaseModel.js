define(['require', 'utils/Utils', 'backbone'], function (require, Utils, Backbone) {
  'use strict';

var BaseModel = Backbone.Model.extend(
	/** @lends BaseModel.prototype */
	{
		/**
		 * BaseModel's initialize function
		 * @augments Backbone.Model
		 * @constructs
		 */
		initialize : function() {
			
		},
		bindErrorEvents :function(){
			this.bind("error", Utils.defaultErrorHandler);
		},
		/**
		 * toString for a model. Every model should implement this function.
		 */
		toString : function() {
			throw new Error('ERROR: toString() not defined for ' + this.modelName);
		},

		/**
		 * Silent'ly set the attributes. ( do not trigger events )
		 */
		silent_set: function(attrs) {
			return this.set(attrs, {
				silent: true
			});
		}
	},
	/** BaseModel's Static Attributes */
	{

		/**
		 * [nonCrudOperation description]
		 * @param  {[type]} url           [description]
		 * @param  {[type]} requestMethod [description]
		 * @param  {[type]} options       [description]
		 * @return {[type]}               [description]
		 */
		nonCrudOperation : function(url, requestMethod, options){
			return Backbone.sync.call(this, null, this, _.extend({
				url: url,
				type: requestMethod
			}, options));
		}
	});

	return BaseModel;
});