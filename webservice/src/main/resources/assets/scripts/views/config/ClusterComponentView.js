define(['require',
    'hbs!tmpl/config/clusterComponentView',
    'utils/Utils',
    'models/VCluster',
    'models/VComponent',
    'utils/LangSupport'
  ], function(require, tmpl, Utils, VCluster, VComponent, localization){
  'use strict';

  var vClusterComponentView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {
      return {
        showWizard: (this.editClusterFlag || this.editComponentFlag) ? false : true,
        editCluster: this.editClusterFlag
      };
    },

    regions: {
      rForm: '#r_Form',
    },

    events: {
      'click #close': 'evClose',
      'click #next': 'evNext',
      'click #Save': 'evSave'
    },

    initialize: function (options) {
      _.extend(this, _.pick(options,'editClusterFlag', 'editComponentFlag', 'clusterType'));
      if(this.editComponentFlag){
        this.componentModel = options.model;
      } else {
        this.clusterModel = options.model;
      }
    },

    onRender: function () {
      var that = this;
      if(!this.editComponentFlag){
        require(['views/config/ClusterForm'], function(ClusterForm){
          that.clusterFormView = new ClusterForm({
            model: that.clusterModel
          });
          that.rForm.show(that.clusterFormView);
        });
      } else {
        this.$el.find('#next').addClass('displayNone');
        this.$el.find('#Save').removeClass('displayNone');
        require(['views/config/ComponentForm'], function(ComponentForm){
          that.componentFormView = new ComponentForm({
            model: that.componentModel,
            type: that.clusterType
          });
          that.rForm.show(that.componentFormView);
        });
      }
    },

    evNext: function(e){
      var that = this;
      var errs = that.clusterFormView.validate();
      if(_.isEmpty(errs)){
        that.saveCluster();
      } else {
        return false;
      }
    },
    saveCluster: function(){
      var that = this;
      var model = this.clusterFormView.getData();
      model.save({},{
        success:function(model, response, options){
          if(that.editClusterFlag) {
            Utils.notifySuccess(localization.tt('dialogMsg.clusterUpdatedSuccessfully'));
            that.trigger('closeModal');
          } else {
            Utils.notifySuccess(localization.tt('dialogMsg.newClusterAddedSuccessfully'));
            that.showComponentForm(new VCluster(response.entity));
          }
        }, 
        error: function(model, response, options){
          Utils.showError(response);
          that.trigger('closeModal');
        }
      });
    },
    showComponentForm: function(clusterModel){
      var that = this;
      $($.find('.modal-open .modal-header h3')).text('Add Component');
      this.$el.find('#next').addClass('displayNone');
      this.$el.find('#Save').removeClass('displayNone');
      this.$el.find('.tmm-current').removeClass('tmmm-current').addClass('tmm-success');
      $(this.$el.find('.stage')[1]).addClass('tmm-current');

      var componentModel = new VComponent();
      componentModel.urlRoot = VComponent.prototype.urlRoot + clusterModel.get('id') + "/components";
      componentModel.set('clusterId', clusterModel.get('id'));
      
      require(['views/config/ComponentForm'], function(ComponentForm){
        that.componentFormView = new ComponentForm({
          model: componentModel,
          type: clusterModel.get('type')
        });
        that.rForm.destroy();
        that.rForm.show(that.componentFormView);
      });
    },

    evSave: function(e){
      var errs = this.componentFormView.validate();
      if(_.isEmpty(errs)){
        this.saveComponent();
      } else {
        return false;
      }
    },
    
    saveComponent: function(){
      var that = this;
      var model = this.componentFormView.getData();
      model.save({},{
        success:function(model, response, options){
          if(that.editComponentFlag){
            Utils.notifySuccess(localization.tt('dialogMsg.componentUpdatedSuccessfully'));  
          } else {
            Utils.notifySuccess(localization.tt('dialogMsg.newComponentAddedSuccessfully'));  
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
  return vClusterComponentView;
});
