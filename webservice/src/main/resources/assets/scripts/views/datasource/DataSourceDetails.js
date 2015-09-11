define(['require',
    'hbs!tmpl/datasource/dataSourceDetails',
    'utils/Utils',
    'models/VDatasource',
    'models/VDatafeed',
    'utils/LangSupport',
    'modules/Modal',
    'bootbox'
  ], function(require, tmpl, Utils, VDatasource, VDatafeed, localization, Modal, bootbox){
  'use strict';

  var vDataSourceDetailView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {
      return {
        dsModel: this.dsModel.attributes,
        showDF: this.dfModel.has('dataFeedId'),
        dfModel: this.dfModel.attributes
      };
    },

    regions: {

    },

    events: {
      'click #editDS': 'editDSAction',
      'click #editDF': 'editDFAction',
      'click #deleteDF': 'deleteDFAction',
      'click #addDF': 'evAddDFAction'
    },

    initialize: function (options) {
      _.extend(this, _.pick(options, 'dsModel'));
      this.dsModel.set('version', JSON.parse(this.dsModel.get('typeConfig')).version);
      this.dsModel.set('deviceId', JSON.parse(this.dsModel.get('typeConfig')).deviceId);
      this.dfModel = new VDatafeed();
      this.fetchFeed();
    },

    fetchFeed: function(){
      var self = this;
      this.dfModel.getModel({
        'dataSourceId': self.dsModel.get('dataSourceId'),
        async: false,
        success: function(model, response, options){
          if(model.entities.length){
            self.dfModel.set(model.entities[0]);
          }
        },
        error: function(model, response, options){
          if(!_.isString(response)){
            Utils.showError(response);
          }
        }
      });
    },

    onRender: function () {
    },
    editDSAction: function(){
      this.editAction(this.dsModel,false, true, false);
    },
    editDFAction: function(){
      this.editAction(this.dfModel, false, false, true);
    },
    editAction: function(model, newDFFlag , dsActionFlag, dfActionFlag){
      var self = this;
      require(['views/datasource/DataSourceFeedView'], function(DataSourceFeedView){
        var view = new DataSourceFeedView({
          model: model,
          editDSFlag: dsActionFlag,
          editDFFlag: dfActionFlag 
        });
        
        var modal = new Modal({
          title: (newDFFlag) ? 'Add Data Feed' : (dsActionFlag) ? 'Edit Datasource' : 'Edit Datafeed',
          content: view,
          showFooter: false,
        }).open();

        view.on('closeModal',function(){
          modal.trigger('cancel');
          if(newDFFlag){
            self.fetchFeed();
          }
          if(dsActionFlag){
            self.cleanUpDSModel();
          } else {
            self.cleanUpDFModel();
          }
          self.render();
        });
      });
    },
    evAddDFAction: function(){
      this.dfModel.set('dataSourceId', this.dsModel.get('dataSourceId'));
      this.editAction(this.dfModel,true, false, true);
    },
    cleanUpDSModel: function(){
      delete this.dsModel.attributes.entity;
      delete this.dsModel.attributes.responseCode;
      delete this.dsModel.attributes.responseMessage;
      this.dsModel.set('version', JSON.parse(this.dsModel.get('typeConfig')).version);
      this.dsModel.set('deviceId', JSON.parse(this.dsModel.get('typeConfig')).deviceId);
    },
    cleanUpDFModel: function(){
      delete this.dfModel.attributes.entity;
      delete this.dfModel.attributes.responseCode;
      delete this.dfModel.attributes.responseMessage;
    },
    deleteDFAction: function(){
      var self = this;
      bootbox.confirm("Do you really want to delete this data feed ?", function(result){
        if(result){
          self.dfModel.destroy({
            success: function(model,response){
              Utils.notifySuccess(localization.tt('dialogMsg.dataFeedDeletedSuccessfully'));
              self.dfModel.attributes = {};
              // self.fetchFeed();
              self.render();
            },
            error: function(model, response, options){
              Utils.showError(response);
            }
          });
        }
      });
    }
  });
  return vDataSourceDetailView;
});
