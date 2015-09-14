define(['require',
    'hbs!tmpl/config/clusterDetails',
    'utils/Utils',
    'models/VCluster',
    'collection/VComponentList',
    'models/VComponent',
    'utils/LangSupport',
    'modules/Modal',
    'bootbox'
  ], function(require, tmpl, Utils, VCluster, VComponentList, VComponent, localization, Modal, bootbox){
  'use strict';

  var vClusterDetailView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {
      return {
        clusterModel: this.clusterModel.attributes,
        showComp: this.componentList.models.length ? true : false,
        componentList: this.componentList.models
      };
    },

    regions: {

    },

    events: {
      'click #editCluster': 'editClAction',
      'click #editComp': 'editCompAction',
      'click #deleteComp': 'deleteCompAction',
      'click #addComp': 'evAddCompAction'
    },

    initialize: function (options) {
      _.extend(this, _.pick(options, 'clusterModel'));
      this.componentList = new VComponentList();
      this.componentList.url = VComponentList.prototype.url + this.clusterModel.get('id') + "/components";
      this.fetchComponent();
    },

    fetchComponent: function(){
      this.componentList.fetch({
        async: false
      });
    },

    onRender: function () {
    },
    editClAction: function(){
      this.editAction(this.clusterModel, false, true, false);
    },
    evAddCompAction: function(){
      var model = new VComponent();
      var id = this.clusterModel.get('id');
      model.urlRoot = VComponent.prototype.urlRoot + id + "/components";
      this.editAction(model, true, false, true);
    },
    editCompAction: function(e){
      var model = this.getModel($(e.currentTarget).data().id);
      this.editAction(model, false, false, true);
    },
    editAction: function(model, addCompFlag, clusterActionFlag, compActionFlag){
      var self = this;
      require(['views/config/ClusterComponentView'], function(ClusterComponentView){
        var view = new ClusterComponentView({
          model: model,
          editClusterFlag: clusterActionFlag,
          editComponentFlag: compActionFlag,
          clusterType: self.clusterModel.get('type')
        });
        
        var modal = new Modal({
          title: (addCompFlag) ? 'Add Component' : (clusterActionFlag) ? 'Edit Cluster' : 'Edit Component',
          content: view,
          showFooter: false,
        }).open();

        view.on('closeModal',function(){
          modal.trigger('cancel');
          if(addCompFlag){
            self.fetchComponent();
          }
          if(clusterActionFlag){
            self.cleanUpClusterModel();
          }
          self.render();
        });
      });
    },
    getModel: function(id){
      var model = this.componentList.get(id);
      var arr = model.urlRoot.split('/'); 
      if(arr[arr.length - 1] === ""){
        model.urlRoot = model.urlRoot + '' +this.clusterModel.get('id') + '/components'; 
      }
      return this.componentList.get(id);
    },
    cleanUpClusterModel: function(){
      delete this.clusterModel.attributes.entity;
      delete this.clusterModel.attributes.responseCode;
      delete this.clusterModel.attributes.responseMessage;
    },
    deleteCompAction: function(e){
      var self = this;
      var model = this.getModel($(e.currentTarget).data().id);
      bootbox.confirm("Do you really want to delete this component ?", function(result){
        if(result){
          model.destroy({
            success: function(model,response){
              Utils.notifySuccess(localization.tt('dialogMsg.componentDeletedSuccessfully'));
              self.fetchComponent();
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
  return vClusterDetailView;
});
