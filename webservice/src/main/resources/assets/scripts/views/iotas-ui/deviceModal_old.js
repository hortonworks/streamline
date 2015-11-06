define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'hbs!tmpl/iotas-ui/deviceModal',
  'jsPlumb'
], function(require, vent, localization, tmpl, jsPlumb) {
  'use strict';

  var IotasEditorLayout = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #parserList' : 'showDetails',
    },

    ui: {
    },

    regions: {
    },

    initialize: function() {
    },

    onRender:function(){
      this.$('#deviceList').select2(
          {placeholder: "Select a device/devices"}
        );
    },

    showDetails: function(){
      if(this.$('#parserList').val() === 'Add new parser'){
        this.$('#parserDetails').show();
      }else{
        this.$('#parserDetails').hide();
      }
    },

  });
  
  return IotasEditorLayout;
});