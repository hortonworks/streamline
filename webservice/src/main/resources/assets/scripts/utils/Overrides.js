define(['require','bootstrap.filestyle','backbone.forms', 'backgrid'], function (require) {
  'use strict';
  
    /**
   * SELECT2
   *
   * Renders Select2 - jQuery based replacement for select boxes
   *
   * Requires an 'options.values' value on the schema.
   *  Can be an array of options, a function that calls back with the array of options, a string of HTML
   *  or a Backbone collection. If a collection, the models must implement a toString() method
   */
  Backbone.Form.editors.Select2 = Backbone.Form.editors.Select.extend({    
    initialize : function(options){
      this.pluginAttr = _.extend( {'width' : 'resolve'}, options.schema.pluginAttr || {});
      Backbone.Form.editors.Select.prototype.initialize.call(this,options);
      //this.$el.attr('type','')
    },
 
  render: function() {
    var self = this;
    this.setOptions(this.schema.options);
    setTimeout(function () {
        self.$el.select2(self.pluginAttr);
    },0);     

    return this;
  },
 
  });
    
  Backbone.Form.editors.Select2Remote = Backbone.Form.editors.Text.extend({      
    initialize : function(options){
      var userAttrs = {};
      if(_.isFunction(options.schema.pluginAttr)){
        userAttrs = options.schema.pluginAttr(this);
      } else {
        userAttrs = options.schema.pluginAttr;
      }
      
      this.pluginAttr = _.extend( {'width' : 'resolve'}, userAttrs || {});
      
      Backbone.Form.editors.Text.prototype.initialize.call(this,options);
      this.$el.attr('type','hidden');
    },
 
    render: function() {
        var self = this;
        if(this.value){
            // value is set to hidden input tag to call initSelection() function
            this.$el.val(this.value);
        }
        setTimeout(function () {
            self.$el.select2(self.pluginAttr);
        },0);
        return this;
    },
    
    getValue: function() {
        return this.$el.select2('val');
    },
    
    setValue: function(value) {
        this.$el.select2('val', value);
    }
 
  });

   Backbone.Form.editors.Fileupload = Backbone.Form.editors.Base.extend({
    initialize: function(options) {
      Backbone.Form.editors.Base.prototype.initialize.call(this, options);
      this.template = _.template('<input type="file" name="fileInput" class="filestyle">');
    },
    render: function() {
      this.$el.html(this.template);
      this.$(':file').filestyle();
      return this;
    },
    getValue: function() {
      return $('input[name="fileInput"]')[0].files[0];
    }
  });

     /**
   * Bootstrap-tag
   */

  Backbone.Form.editors.Tag = Backbone.Form.editors.Text.extend({		 
	  events: {
		  'change': function() {
			  this.trigger('change', this);
		  },
		  'focus': function() {
			  this.trigger('focus', this);
		  },
		  'blur': function() {
			  this.trigger('blur', this);
		  }
	  },

	  initialize: function(options) {
		  Backbone.Form.editors.Text.prototype.initialize.call(this, options);

		  this.pluginAttr = _.extend({}, options.schema.pluginAttr || {});
	  },
	  render: function() {
		  var self = this;
		  if(this.schema.options){
			  // convert options to source for the plugin
			  this.pluginAttr['source'] = this.getSource(this.schema.options);
		  }

		  this.setValue(this.value);

		  setTimeout(function () {
			  self.$el.tagsinput(self.pluginAttr);
		  },0);	

		  return this;
	  },

	  /**
	   * Set the source param.
	   */
	  getSource: function(options) {
		  var self = this;
		  var source;

		  //If a collection was passed, check if it needs fetching
		  if (options instanceof Backbone.Collection) {
			  var collection = options;

			  //Don't do the fetch if it's already populated
			  if (collection.length > 0) {
				  source= this._toSource(options);
			  } else {
				  collection.fetch({
					  success: function(collection) {
						  source = self._toSource(options);
					  }
				  });
			  }
		  }

		  //If a function was passed, run it to get the options
		  else if (_.isFunction(options)) {
			  options(function(result) {
				  source = self._toSource(result);
			  }, self);
		  }

		  //Otherwise, ready to go straight to renderOptions
		  else {
			  source = this._toSource(options);
		  }

		  return source;
	  },

	  _toSource : function(options) {
		  var source;
		  //Accept string
		  if (_.isString(options)) {
			  source = [options];
		  }

		  //Or array
		  else if (_.isArray(options)) {
			  source = options;
		  }

		  //Or Backbone collection
		  else if (options instanceof Backbone.Collection) {
			  source = new Array();
			  options.each(function(model) {
				  source.push(model.toString());
			  });
		  }

		  else if (_.isFunction(options)) {
			  options(function(opts) {
				  source = opts;
			  }, this);
		  }
		  return source;
	  },
	  getValue: function() {
		  if (this.schema.getValue) {
			  return this.schema.getValue(this.$el.val(),this.model);
		  } else {
			  return this.$el.val();
		  }
	  },

	  setValue: function(values) {
		  if (!_.isArray(values)) values = [values];
		  if (this.schema.setValue) {
			  this.$el.val(this.schema.setValue(values));
		  } else {
			  this.$el.val(values);
		  }
	  },

	  focus: function() {
		  if (this.hasFocus) return;
		  this.$el.focus();
	  },

	  blur: function() {
		  if (!this.hasFocus) return;
		  this.$el.blur();
	  },

  });

	  /*
   * HtmlCell renders any html code
   * @class Backgrid.HtmlCell
   * @extends Backgrid.Cell
   */
  var HtmlCell = Backgrid.HtmlCell = Backgrid.Cell.extend({

    /** @property */
    className: "html-cell",

    render: function() {
      this.$el.empty();
      var rawValue = this.model.get(this.column.get("name"));
      var formattedValue = this.formatter.fromRaw(rawValue, this.model);
      this.$el.append(formattedValue);
      this.delegateEvents();
      return this;
    }
  });

});