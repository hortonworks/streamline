define(['require',
    'hbs!tmpl/site/header',
    'modules/Vent',
    'views/common/ConfigurationForm',
    'bootbox'
  ], function(require, tmpl, Vent, ConfigurationForm, bootbox){
  'use strict';

  var vHeader = Marionette.LayoutView.extend({
    template: tmpl,
    templateHelpers: function() {},
    regions: {},
    events: {
      'click #menu-toggle': 'menuToggleAction',
      // 'click #config': 'evConfigView'
    },
    initialize: function(options) {
      this.vent = Vent;
    },
    menuToggleAction: function(e){
      this.vent.trigger('sidebar-menu-toggle');
    },
    onRender: function(){},
    evConfigView: function(){
      var that = this;
      if (this.view) {
        this.onDialogClosed();
      }
      this.view = new ConfigurationForm().render();
      bootbox.dialog({
        message: this.view.$el,
        title: 'Configuration',
        className: 'topology-modal',
        onEscape: true,
        buttons: {
          cancel: {
            label: 'Close',
            className: 'btn-default',
            callback: function() {
              that.onDialogClosed();
            }
          },
          success: {
            label: 'Save',
            className: 'btn-success',
            callback: function(){
              var errs = that.view.validate();
              if(_.isEmpty(errs)){
                that.saveConfig();
              } else return false;
            }
          }
        }
      });
    },
    saveConfig: function(){
      var attrs = this.view.getData();
      console.log(attrs);
    },
    onDialogClosed: function(){
      if (this.view) {
        this.view.close();
        this.view.remove();
        this.view = null;
      }
    }
  });
  return vHeader;
});