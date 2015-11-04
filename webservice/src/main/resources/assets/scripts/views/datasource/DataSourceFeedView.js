define(['require',
    'hbs!tmpl/datasource/dataSourceFeedView',
    'utils/Utils',
    'utils/LangSupport',
    'models/VDatasource',
    'models/VDatafeed'
  ], function(require, tmpl, Utils, localization){
  'use strict';

  var vDataSourceFeedView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    regions: {
      rForm: '#rForm',
    },

    events: {
      'click #btnCancel': 'evClose',
      'click #btnSave': 'evSave'
    },

    initialize: function (options) {
      _.extend(this, _.pick(options, 'model', 'isEdit'));
    },

    onRender: function () {
      var that = this;
      require(['views/device/DeviceForm'], function(AddDeviceFormView){
        that.view = new AddDeviceFormView({
          model: that.model
        });
        that.rForm.show(that.view);
      });
    },

    evSave: function(e){
      var errs = this.view.validate();
      if(_.isEmpty(errs)){
        this.saveDataSource();
      } else {
        return false;
      }
    },
    
    saveDataSource: function(){
      var that = this;
      var model = this.view.getData();
      model.save({},{
        success:function(model, response, options){
          if(that.isEdit){
            Utils.notifySuccess(localization.tt('dialogMsg.datasourceUpdatedSuccessfully'));  
          } else {
            Utils.notifySuccess(localization.tt('dialogMsg.newDatasourceAddedSuccessfully'));  
          }
          that.trigger('closeModal');
        }, 
        error: function(model, response, options){
          Utils.showError(model, response);
          that.trigger('closeModal');
        }
      });
    },
    
    evClose: function(e){
      this.trigger('closeModal');
    }
   
  });
  return vDataSourceFeedView;
});
