/*
 * Copyright (c) 2015 EPMWare Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * EPMWare Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with EPMWare Inc.
 */

/**************************************************************************
-- Purpose: Bootstrap Modal wrapper for use with Backbone.
--
-- Author: Bhavir Shah (Original author: Charles Davison <charlie@powmedia.co.uk>)
--
-- Change History:
-- Modified by       Date         Notes
 =========================================
-- Bhavir Shah   2014-02-27     Initial
-- Bhavir Shah   2014-08-06     Toggling 'Ok' button state on triggered events
**************************************************************************/

/**
 * @file Bootstrap Modal wrapper for use with Backbone.
 *
 * Takes care of instantiation, manages multiple modals,
 * adds several options and removes the element from the DOM when closed
 *
 * @extends Backbone.View
 *
 * @fires shown: Fired when the modal has finished animating in
 * @fires hidden: Fired when the modal has finished animating out
 * @fires cancel: The user dismissed the modal
 * @fires ok: The user clicked OK
 */

define(function(require) {

  var Backbone = require('backbone');

  //Set custom template settings
  var _interpolateBackup = _.templateSettings;
  _.templateSettings = {
    interpolate: /\{\{(.+?)\}\}/g,
    evaluate: /<%([\s\S]+?)%>/g
  };

  var template = _.template(''+
    '<div class="modal-dialog modal-lg"><div class="modal-content">'+
    '<% if (title) { %>'+
      '<div class="modal-header">'+
        '<% if (allowCancel) { %>'+
          '<button type="button" class="close">&times;</button>'+
        '<% } %>'+
        '<h4>{{title}}</h4>'+
      '</div>'+
    '<% } %>'+
    '<div class="modal-body">{{content}}</div>'+
    '<% if (showFooter) { %>'+
      '<div class="modal-footer">'+
        '<button type="button" class="btn ok btn-primary">{{okText}}</button>'+
        '<% if (allowCancel) { %>'+
          '<% if (cancelText) { %>'+
            '<button type="button" class="btn btn-default cancel">{{cancelText}}</button>'+
          '<% } %>'+
        '<% } %>'+
      '</div>'+
    '<% } %>'+
    '</div></div>');

  //Reset to users' template settings
  _.templateSettings = _interpolateBackup;

  /**
   * Creates a new Modal instance
   */
  var Modal = Backbone.View.extend({
  /** @lends Modal */

    className: 'modal',

    events: {
      'click .close': function(event) {
        event.preventDefault();

        this.trigger('cancel');

        if (this.options.content && this.options.content.trigger) {
          this.options.content.trigger('cancel', this);
        }
      },
      'click .cancel': function(event) {
        event.preventDefault();

        this.trigger('cancel');

        if (this.options.content && this.options.content.trigger) {
          this.options.content.trigger('cancel', this);
        }
      },
      'click .ok': function(event) {
        event.preventDefault();

        this.trigger('ok');

        if (this.options.content && this.options.content.trigger) {
          this.options.content.trigger('ok', this);
        }

        if (this.options.okCloses) {
          this.close();
        }
      },
      'keypress': function(event) {
        if (this.options.enterTriggersOk && event.which == 13) {
          event.preventDefault();

          this.trigger('ok');

          if (this.options.content && this.options.content.trigger) {
            this.options.content.trigger('ok', this);
          }

          if (this.options.okCloses) {
            this.close();
          }
        }
      }
    },

    /**
     * Creates an instance of a Bootstrap Modal
     *
     * @see http://twitter.github.com/bootstrap/javascript.html#modals
     *
     * @param {Object} options
     * @param {String|View} [options.content]     Modal content. Default: none
     * @param {String} [options.title]            Title. Default: none
     * @param {String} [options.okText]           Text for the OK button. Default: 'OK'
     * @param {String} [options.cancelText]       Text for the cancel button. Default: 'Cancel'. If passed a falsey value, the button will be removed
     * @param {Boolean} [options.allowCancel]     Whether the modal can be closed, other than by pressing OK. Default: true
     * @param {Boolean} [options.escape]          Whether the 'esc' key can dismiss the modal. Default: true, but false if options.cancellable is true
     * @param {Boolean} [options.animate]         Whether to animate in/out. Default: false
     * @param {Function} [options.template]       Compiled underscore template to override the default one
     * @param {Boolean} [options.enterTriggersOk] Whether the 'enter' key will trigger OK. Default: false
     * @param {String} [options.mainClass]        Adds class to <div class="modal"> (modified)
     * @param {Boolean} [options.staticBackdrop]  Whether to keep backdrop static or not. Default: true
     * @param {Boolean} [options.bindButtonEvents]Whether to bind events to footer buttons or not if content is a view. Default: true
     */
    initialize: function(options) {
      this.options = _.extend({
        title: null,
        okText: 'OK',
        focusOk: false,
        okCloses: true,
        cancelText: 'Cancel',
        showFooter: true,
        allowCancel: true,
        escape: true,
        animate: false,
        template: template,
        enterTriggersOk: false,
        staticBackdrop: true,
        bindButtonEvents: true
      }, options);

      // bind events for ok and close button button
      if (this.options.bindButtonEvents===true) {
        this.bindButtonEvents();
      }
    },

    bindButtonEvents: function(){
      var options = this.options,
        content = options.content;
      //Bind button events on main content, if it's a view
      if (content && content.$el) {
        this.listenTo(content, "toggle:okBtn", function(isEnable){
          this.toggleButtonState('ok',!!isEnable);
        }, this);

        this.listenTo(content, "toggle:cancelBtn", function(isEnable){
          this.toggleButtonState('cancel',!!isEnable);
        }, this);
      }
    },

    /**
     * Creates the DOM element
     */
    render: function() {
      var $el = this.$el,
          options = this.options,
          content = options.content;

      //Create the modal container
      $el.html(options.template(options));

      var $content = this.$content = $el.find('.modal-body');

      //Insert the main content if it's a view
      if (content && content.$el) {
        content.render();
        $el.find('.modal-body').html(content.$el);
      }

      if (options.mainClass){
        $el.addClass(options.mainClass);
      }

      if (options.animate){
        $el.addClass('fade');
      }

      this.isRendered = true;

      return this;
    },

    /**
     * Renders and shows the modal
     *
     * @param {Function} [cb]     Optional callback that runs only when OK is pressed.
     */
    open: function(cb) {
      if (!this.isRendered){
        this.render();
      }

      var self = this,
          $el = this.$el;

      //Create it
      $el.modal(_.extend({
        keyboard: this.options.allowCancel,
        backdrop: this.options.staticBackdrop ? 'static' : true
      }, this.options.modalOptions));

      //Focus OK button
      $el.one('shown.bs.modal', function() {
        if (self.options.focusOk) {
          $el.find('.btn.ok').focus();
        }

        if (self.options.content && self.options.content.trigger) {
          self.options.content.trigger('shown', self);
        }

        self.trigger('shown');
      });

      //Adjust the modal and backdrop z-index; for dealing with multiple modals
      var numModals = Modal.count,
          $backdrop = $('.modal-backdrop:eq('+numModals+')'),
          backdropIndex = parseInt($backdrop.css('z-index'),10),
          elIndex = parseInt($backdrop.css('z-index'), 10);

      $backdrop.css('z-index', backdropIndex + numModals);
      this.$el.css('z-index', elIndex + numModals);

      if (!this.options.staticBackdrop) {
        $backdrop.one('click', function() {
          if (self.options.content && self.options.content.trigger) {
            self.options.content.trigger('cancel', self);
          }

          self.trigger('cancel');
        });

        $(document).one('keyup.dismiss.modal', function (e) {
          e.which == 27 && self.trigger('cancel');

          if (self.options.content && self.options.content.trigger) {
            e.which == 27 && self.options.content.trigger('shown', self);
          }
        });
      }

      this.on('cancel', function() {
        self.close();
      });

      Modal.count++;

      //Run callback on OK if provided
      if (cb) {
        self.on('ok', cb);
      }

      $el.find("[autofocus]:first").focus();

      return this;
    },

    /**
     * Closes the modal
     */
    close: function() {
      var self = this,
          $el = this.$el;

      //Check if the modal should stay open
      if (this._preventClose) {
        this._preventClose = false;
        return;
      }

      $el.one('hidden.bs.modal', function onHidden(e) {
        // Ignore events propagated from interior objects, like bootstrap tooltips
        if(e.target !== e.currentTarget){
          return $el.one('hidden', onHidden);
        }
        self.remove();

        if (self.options.content && self.options.content.trigger) {
          self.options.content.trigger('hidden', self);

          // TODO FIX - Memory leak and same listener registered twice issue
          //self.options.content.close();
        }

        self.trigger('hidden');
      });

      $el.modal('hide');

      Modal.count--;
    },

    /**
     * Stop the modal from closing.
     * Can be called from within a 'close' or 'ok' event listener.
     */
    preventClose: function() {
      this._preventClose = true;
    },

    /**
     * [toggleButtonState description]
     * @param  {String}  buttonClass  it should be either 'ok' or 'cancel'
     * @param  {Boolean} isEnable flag for enabling/disabling button
     * @return {Object} this context
     */
    toggleButtonState: function(buttonClass, isEnable){
      if (buttonClass==="ok" || buttonClass==="cancel") {
        if (isEnable) {
          this.$('.'+buttonClass).removeAttr('disabled');
        } else {
          this.$('.'+buttonClass).attr('disabled', 'disabled');
        }
      }
      return this;
    }
  }, {
    //STATICS

    //The number of modals on display
    count: 0
  });


  // //EXPORTS
  // //CommonJS
  // if (typeof require == 'function' && typeof module !== 'undefined' && exports) {
  //   module.exports = Modal;
  // }

  // //AMD / RequireJS
  // if (typeof define === 'function' && define.amd) {
  //   return define(function() {
  //     Backbone.BootstrapModal = Modal;
  //   })
  // }

  // //Regular; add to Backbone.Bootstrap.Modal
  // else {
  //   Backbone.BootstrapModal = Modal;
  // }
  return Modal;
});

// })(jQuery, _, backbone);
