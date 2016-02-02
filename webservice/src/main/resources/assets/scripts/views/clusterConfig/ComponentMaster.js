define([
  'require',
  'hbs!tmpl/clusterConfig/componentMaster',
  'utils/Utils',
  'utils/Globals',
  'collection/VComponentList',
  'models/VCluster',
  'models/VComponent',
  'utils/TableLayout',
  'utils/LangSupport',
  'modules/Modal',
  'modules/Vent',
  'bootbox'
], function(require, tmpl, Utils, Globals, VComponentList, VCluster, VComponent, VTableLayout, localization, Modal, Vent, bootbox) {
  'use strict';

  var vConfigView = Marionette.LayoutView.extend({
    template: tmpl,

    templateHelpers: function() {
      return {
        stormArr: this.stormArr,
        kafkaArr: this.kafkaArr,
        hdfsArr: this.hdfsArr,
        showStorm: this.stormArr.length ? true : false,
        showKafka: this.kafkaArr.length ? true : false,
        showHdfs: this.hdfsArr.length ? true : false,
      };
    },

    events: {
      "click .addNewConfig": "evAddConfig",
      "click .editConfig": "evEditConfig",
      "click .deleteConfig": "evDelConfig"
    },

    initialize: function(options) {
      var self = this;
      _.extend(this, options);
      this.stormArr = [];
      this.kafkaArr = [];
      this.hdfsArr = [];
      this.vent = Vent;
      this.bindEvents();
      if(this.clusterCollection.models.length){
        _.each(this.clusterCollection.models, function(model){
          self.fetchComponent(model.get('type'), model.get('id'));
        });
      }
    },

    fetchComponent: function(type, id){
      var compCollection = new VComponentList();
      var self = this;
      compCollection.url = VComponentList.prototype.url + id + "/components";
      compCollection.fetch({
        async: false
      });
      if(compCollection.models.length){
        if(type === 'STORM'){
          this.stormArr = [];
          Array.prototype.push.apply(this.stormArr, compCollection.models);
        } else if(type ==='KAFKA'){
          this.kafkaArr = [];
          Array.prototype.push.apply(this.kafkaArr, compCollection.models);
        } else if(type ==='HDFS'){
          this.hdfsArr = [];
          Array.prototype.push.apply(this.hdfsArr, compCollection.models);
        }
      } else {
        if(type === 'STORM'){
          this.stormArr = [];
        } else if(type ==='KAFKA'){
          this.kafkaArr = [];
        } else if(type ==='HDFS'){
          this.hdfsArr = [];
        }
      }
    },

    onRender: function() {
      $('#cluster-config').toggleClass('current');

      this.$('[data-rel="tooltip"]').tooltip({placement: 'bottom'});
      if(this.stormArr.length === Globals.Component.Storm.length){
        this.$('[data-id="STORM"]').hide();
      }
      if(this.kafkaArr.length === Globals.Component.Kafka.length){
        this.$('[data-id="KAFKA"]').hide();
      }
      if(this.hdfsArr.length === Globals.Component.HDFS.length){
        this.$('[data-id="HDFS"]').hide();
      }
    },

    bindEvents: function() {
      var self = this;

      this.listenTo(this.vent, 'component:Save', function(model) {
        var type = model.get('clusterType');
        var clusterObj = self.clusterCollection.models.find(function(m){return m.get('type') === type;});
        delete model.attributes.clusterId;
        if(type === 'KAFKA'){
          if(self.createKafkaCluster){
            self.createCluster(type, model);
          } else {
            self.createComponent(clusterObj, model);
          }
        } else if (type === 'STORM'){
          if(self.createStormCluster){
            self.createCluster(type, model);
          } else {
            self.createComponent(clusterObj, model);
          }
        } else if (type === 'HDFS'){
          if(self.createHdfsCluster){
            self.createCluster(type, model);
          } else {
            self.createComponent(clusterObj, model);
          }
        }
      });
    },

    evAddConfig: function(event) {
      var currentTarget = $(event.currentTarget),
        clusterType = currentTarget.data().id,
        model = new VComponent();
      this.showModal(model, clusterType);
    },

    showModal: function(model, type){
      var self = this;
      var componentArr;
      switch(type){
        case 'STORM':
          componentArr = this.stormArr;
        break;
        case 'KAFKA':
          componentArr = this.kafkaArr;
        break;
        case 'HDFS':
          componentArr = this.hdfsArr;
        break;
      }
      require(['views/clusterConfig/ComponentView'], function(ComponentView) {
        var view = new ComponentView({
          model: model,
          vent: self.vent,
          componentArr: componentArr, 
          type: type
        });

        var modal = new Modal({
          title: (model.has('id')) ? 'Edit Component' : 'Add Component',
          content: view,
          contentWithFooter: true,
          showFooter: false,
          escape: false
        }).open();

        view.on('closeModal', function() {
          modal.trigger('cancel');
        });
      });
    },

    createCluster: function(type, componentModel){
      var self = this;
      var model = new VCluster();
      model.set('name','auto_'+type+'_cluster');
      model.set('description', 'This is auto generated cluster');
      model.set('type',type);
      model.set('tags','autogenerated,'+type);
      model.save({},{
        success:function(model, response, options){
          var clusterObj = response.entity;
          if(type === 'KAFKA'){
            self.createKafkaCluster = false;
          } else if (type === 'STORM'){
            self.createStormCluster = false;
          } else if (type === 'HDFS'){
            self.createHdfsCluster = false;
          }
          self.clusterCollection.add(new VCluster(clusterObj));
          self.createComponent(clusterObj, componentModel);
        }, 
        error: function(model, response, options){
          Utils.showError(model, response);
         }
      });
    },

    createComponent: function(clusterObj, componentModel){
      var self = this,
        msg,
        options = {},
        data = {},
        clusterType = componentModel.get('clusterType'),
        componentId = componentModel.has('id') ? componentModel.get('id') : undefined;
      delete componentModel.attributes.clusterType;
      componentModel.set('config', '');
      if(componentId){
        delete componentModel.attributes.id;
        data = JSON.stringify(componentModel.toJSON());
        msg = localization.tt('dialogMsg.componentUpdatedSuccessfully');
      } else {
        data = JSON.stringify([componentModel.toJSON()]);
        msg = localization.tt('dialogMsg.newComponentAddedSuccessfully');
      }
      componentModel.deploy({
        id: clusterObj.id,
        editState: componentId ? true : false,
        modelId: componentId,
        data: data,
        dataType:'json',
        contentType: 'application/json',
        success:function(model, response, options){
          self.fetchComponent(clusterType, clusterObj.id);
          Utils.notifySuccess(msg);
           self.render();
         },
        error: function(model, response, options){
          Utils.showError(model, response);
        }
      });
    },

    evEditConfig: function(event) {
      var model = this.getModel({
        id: $(event.currentTarget).data().id,
        type: $(event.currentTarget).data().type
      });

      this.showModal(model, $(event.currentTarget).data().type);
    },

    evDelConfig: function(event) {
      var self = this;
      bootbox.confirm("Do you really want to delete this component ?", function(result){
        if(result){
          var currentTarget = $(event.currentTarget),
              clusterType = currentTarget.data().type,
              id = currentTarget.data().id,
              model = self.getModel({
                id: id,
                type: clusterType
              });
          model.destroyModel({
            clusterId: model.get('clusterId'),
            componentId: model.get('id'),          
            data: JSON.stringify(model.toJSON()),
            dataType:'json',
            contentType: 'application/json',
            success: function(model,response, options){            
              Utils.notifySuccess(localization.tt('dialogMsg.componentDeletedSuccessfully'));
              self.fetchComponent(clusterType, model.entity.clusterId);
              self.render();                         
            },
            error: function(model, response, options){
              Utils.showError(model, response);
            }
          });
        }
      });
    },

    getModel: function(obj) {
      var id = obj.id,
          type = obj.type,
          configModel;

      if (type === 'KAFKA') {
        configModel = _.find(this.kafkaArr, function(model) {
          if(model.get('id') == id)
            return model;
        });
      } else if (type === 'STORM') {
        configModel = _.find(this.stormArr, function(model) {
          if (model.get('id') == id)
            return model;
        });
      } else if (type === 'HDFS') {
        configModel = _.find(this.hdfsArr, function(model) {
          if (model.get('id') == id)
            return model;
        });
      }
      return configModel;
    },

    destroy: function(){
      this.stopListening(this.vent, 'component:Save');
      $('#cluster-config').toggleClass('current');
    }

  });
  return vConfigView;
});