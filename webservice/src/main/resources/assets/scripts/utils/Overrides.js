define(['require','bootstrap.filestyle','backbone.forms', 'backgrid', 'bootstrap-multiselect', 'jquery-nestable'], function (require) {
  'use strict';
  
    $.fn.popover.defaults = {
      animation: true,
      container: true,
      content: "",
      delay: 0,
      html: true,
      placement: "right",
      selector: false,
      template: '<div class="popover"><div class="arrow"></div><h3 class="popover-title"></h3><div class="popover-content"></div></div>',
      title: "",
      trigger: "click"
    };

    var nestableOrg = $.fn.nestable;
    $.fn.nestable = function(params){
      var lists  = this;
        lists.each(function()
        {
          var $this = $(this);
          nestableOrg.call($this, params)
          var nestable = $this.data('nestable');
          nestable.dragStop = function(e){
            var el = this.dragEl.children(this.options.itemNodeName).first();
            el[0].parentNode.removeChild(el[0]);
            this.placeEl.replaceWith(el);

            this.dragEl.remove();
            var currentElemId = el.data('id');
            var parentId = el.parent().parent().data('id');
            this.el.trigger('change', {currentId: currentElemId, parentId: parentId});
            if (this.hasNewRoot) {
                this.dragRootEl.trigger('change');
            }
            this.reset();
          }
        });

        return lists;
    }

    /**
     * Bootstrap Multiselect
     */
    Backbone.Form.editors.MultiSelect = Backbone.Form.editors.Select.extend({    
      initialize : function(options){
        var attrObj = {
          numberDisplayed: 5,
          buttonWidth: '100%',
          enableFiltering: true,
          filterBehavior: 'value'
        };
        this.pluginAttr = _.extend(attrObj, options.schema.pluginAttr || {});
        Backbone.Form.editors.Select.prototype.initialize.call(this,options);
      },
   
      render: function() {
        var self = this;
        this.setOptions(this.schema.options);
        setTimeout(function () {
            self.$el.multiselect(self.pluginAttr);
        },0);     

        return this;
      },
      getValue: function() {
        return this.$el.val();
      },
      // setValue: function(value){
      //   throw "need to implement setValue functionality to multiselect.";
      // }
   
    });
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
        self.$el.parents('.bootbox').removeAttr('tabindex'); 
        self.$el.select2(self.pluginAttr);
        if(self.value)
          self.$el.select2('val', self.value);
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
    events: {
      'change': function(event) {
        this.trigger('change', this);
      },
    },
    initialize: function(options) {
      Backbone.Form.editors.Base.prototype.initialize.call(this, options);
      var id = this.id = options.schema.elementId || undefined;
      this.template = _.template('<input type="file" id="'+ id +'" class="filestyle">');
    },
    render: function() {
      this.$el.html(this.template);
      this.$(':file').filestyle();
      if(this.model && this.model.has(this.key) && this.model.get(this.key) !== ''){
        this.setValue(this.model.get(this.key));
      }
      return this;
    },
    getValue: function() {
      return $('input[type="file"][id="'+this.id+'"]')[0].files[0];
    },
    setValue: function(value){
      this.$el.find('input[type="text"]').attr('value', value);
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

  var UriCell = Backgrid.UriCell = Backgrid.Cell.extend({
    className: "uri-cell",
    title: null,
    target: "_blank",

    initialize: function(options) {
      UriCell.__super__.initialize.apply(this, arguments);
      this.title = options.title || this.title;
      this.target = options.target || this.target;
    },

    render: function() {
      this.$el.empty();
      var rawValue = this.model.get(this.column.get("name"));
      var href = _.isFunction(this.column.get("href")) ? this.column.get('href')(this.model) : this.column.get('href');
      var klass = this.column.get("klass");
      var formattedValue = this.formatter.fromRaw(rawValue, this.model);
      this.$el.append($("<a>", {
        tabIndex: -1,
        href: href,
        title: this.title || formattedValue,
        'class': klass
      }).text(formattedValue));

      if (this.column.has("iconKlass")) {
        var iconKlass = this.column.get("iconKlass");
        var iconTitle = this.column.get("iconTitle");
        this.$el.find('a').append('<i class="' + iconKlass + '" title="' + iconTitle + '"></i>');
      }
      this.delegateEvents();
      return this;
    }

  });

});