define(['require',
    'hbs!tmpl/datasource/dataSourceFeedView',
    'utils/Utils',
    'models/VDatasource',
    'models/VDatafeed',
    'utils/LangSupport'
  ], function(require, tmpl, Utils, VDatasource, VDatafeed, localization){
  'use strict';

  var vDataSourceFeedView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {
      return {
        showWizard: (this.editDSFlag || this.editDFFlag) ? false : true,
        editDataSource: this.editDSFlag
      };
    },

    regions: {
      rForm: '#dsForm',
    },

    events: {
      'click #close': 'evClose',
      'click #next': 'evNext',
      'click #Save': 'evSave'
    },

    initialize: function (options) {
      _.extend(this, _.pick(options,'editDSFlag', 'editDFFlag'));
      if(this.editDFFlag){
        this.dfModel = options.model;
      } else {
        this.dsModel = options.model;
      }
    },

    onRender: function () {
      var that = this;
      if(!this.editDFFlag){
        require(['views/datasource/DataSourceForm'], function(DataSourceForm){
          that.dataSourceFormView = new DataSourceForm({
            model: that.dsModel
          });
          that.rForm.show(that.dataSourceFormView);
        });
      } else {
        this.$el.find('#next').addClass('displayNone');
        this.$el.find('#Save').removeClass('displayNone');
        require(['views/datasource/DataFeedForm'], function(DataFeedForm){
          that.dataFeedFormView = new DataFeedForm({
            model: that.dfModel
          });
          that.rForm.show(that.dataFeedFormView);
        });
      }
    },

    evNext: function(e){
      var that = this;
      var errs = that.dataSourceFormView.validate();
      if(_.isEmpty(errs)){
        that.saveDataSource();
      } else {
        return false;
      }
    },
    saveDataSource: function(){
      var that = this;
      var model = this.dataSourceFormView.getData();
      model.save({},{
        success:function(model, response, options){
          if(that.editDSFlag) {
            Utils.notifySuccess(localization.tt('dialogMsg.datasourceUpdatedSuccessfully'));
            that.trigger('closeModal');
          } else {
            Utils.notifySuccess(localization.tt('dialogMsg.newDatasourceAddedSuccessfully'));
            that.showDataFeedForm(new VDatasource(response.entity));
          }
        }, 
        error: function(model, response, options){
          Utils.showError(response);
          that.trigger('closeModal');
        }
      });
    },
    showDataFeedForm: function(dsModel){
      var that = this;
      $($.find('.modal-open .modal-header h3')).text('Create Data Feed');
      this.$el.find('#next').addClass('displayNone');
      this.$el.find('#Save').removeClass('displayNone');
      this.$el.find('.tmm-current').removeClass('tmmm-current').addClass('tmm-success');
      $(this.$el.find('.stage')[1]).addClass('tmm-current');

      var feedModel = new VDatafeed();
      feedModel.set('dataSourceId', dsModel.get('dataSourceId'));
      
      require(['views/datasource/DataFeedForm'], function(DataFeedForm){
        that.dataFeedFormView = new DataFeedForm({
          model: feedModel
        });
        that.rForm.destroy();
        that.rForm.show(that.dataFeedFormView);
      });
    },

    evSave: function(e){
      var errs = this.dataFeedFormView.validate();
      if(_.isEmpty(errs)){
        this.saveDataFeed();
      } else {
        return false;
      }
    },
    
    saveDataFeed: function(){
      var that = this;
      var model = this.dataFeedFormView.getData();
      model.save({},{
        success:function(model, response, options){
          if(that.editDFFlag){
            Utils.notifySuccess(localization.tt('dialogMsg.dataFeedUpdatedSuccessfully'));  
          } else {
            Utils.notifySuccess(localization.tt('dialogMsg.newDataFeedAddedSuccessfully'));  
          }
          that.trigger('closeModal');
        }, 
        error: function(model, response, options){
          Utils.showError(response);
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
