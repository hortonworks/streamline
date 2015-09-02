define(['require',
    'hbs!tmpl/device/deviceCatalog',
    'utils/Utils',
    'collection/VDatasourceList',
    'models/VDatasource',
    'utils/TableLayout',
    'utils/LangSupport',
    'bootbox'
  ], function(require, tmpl, Utils, VDatasourceList, VDatasource, VTableLayout, localization, bootbox){
  'use strict';

  var vDeviceCatalogView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    regions: {
      tableLayout: '#rTable',
      rFilter: 'div[data-id="r_filter"]'
    },

    events: {
      'click #addDevice': 'evAddDevice',
      'keyup #srchDevice': 'evSearchDevice',
      'click .expand-link': function(e){
        if(e.currentTarget.children.item().classList.contains('fa-expand')){
          this.$el.find('#addDevice').hide(); 
        } else {
          this.$el.find('#addDevice').show();
        }
        Utils.expandPanel(e);
      },
      'click #editAction': 'evEditAction',
      'click #deleteAction': 'evDeleteAction'
    },

    initialize: function (options) {
      this.collection = new VDatasourceList();
    },

    onRender: function () {
      this.showTable();
      this.fetchData();
      Utils.panelMinimize(this);
    },
    fetchData: function(){
      var that = this;
      this.collection.fetchDeviceType({
        success:function(collection, response, options){
          that.collection.reset(collection.entities);
        }, 
        error: function(collection, response, options){
          var msg;
          if(response === 'error'){
            msg = collection.responseJSON.responseMessage;
          } else {
            if(response.responseJSON.responseMessage){
              msg = response.responseJSON.responseMessage;
            } else {
              msg = response.responseJSON.message;
            }
          }
          Utils.notifyError(msg);
        }
      });
    },
    showTable: function(){
      this.tableLayout.show(this.getTable());
    },
    getTable: function(){
      return new VTableLayout({
        parentView: this,
        columns: this.getColumns(),
        collection: this.collection,
        includeFilter: true,
        gridOpts: {
          emptyText: localization.tt('msg.noDeviceFound'),
          className: 'table table-backgrid table-bordered table-striped table-condensed'
        }
      });
    },
    getColumns: function(){
      return [{
        name: 'dataSourceName',
        cell: 'string',
        label: localization.tt('lbl.deviceName'),
        hasTooltip: false,
        editable: false
      },{
        name: 'description',
        cell: 'string',
        label: localization.tt('lbl.description'),
        hasTooltip: false,
        editable: false
      },{
        name: 'type',
        cell: 'string',
        label: localization.tt('lbl.type'),
        hasTooltip: false,
        editable: false
      }, {
        name: 'tags',
        cell: 'string',
        label: localization.tt('lbl.tags'),
        hasTooltip: false,
        editable: false
      }, {
        name: 'version',
        cell: 'string',
        label: localization.tt('lbl.version'),
        hasTooltip: false,
        editable: false,
        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
          fromRaw: function (rawValue,model) {
            return JSON.parse(model.get('typeConfig')).version;
          }
        })
      }, {
        name: 'typeConfig',
        cell: 'string',
        label: localization.tt('lbl.deviceId'),
        hasTooltip: false,
        editable: false,
        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
          fromRaw: function (rawValue) {
            return JSON.parse(rawValue).deviceId;
          }
        }),
      }, {
          name: "actions",
          cell: "Html",
          label: localization.tt('lbl.actions'),
          hasTooltip: false,
          editable: false,
          formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
            fromRaw: function(rawValue, model) {
              if (model) {
                return "<button type='default' title='Edit' class='btn btn-success btn-xs' data-id="+model.get('dataSourceId')+" id='editAction'><i class='fa fa-edit'></i></button><button title='Delete' class='btn btn-danger btn-xs' data-id="+model.get('dataSourceId')+" id='deleteAction' type='default' ><i class='fa fa-trash'></i></button>";
              }
            }
          })
        }];
    },
    evAddDevice: function(model){
      var that = this;
      if(_.isUndefined(model) || model.currentTarget){
        model = new VDatasource();
        model.set('type','DEVICE');
      }
      require(['views/device/DeviceForm'],function(DeviceFormView){
        if(that.view){
          that.onDialogClosed();
        }
        that.view = new DeviceFormView({
          model: model,
          readOnlyFlag: true
        }).render();
        bootbox.dialog({
          message: that.view.el,
          title: localization.tt('h.addNewDevice'),
          className: 'device-dialog',
          buttons: {
            cancel: {
              label: localization.tt('lbl.close'),
              className: 'btn-default',
              callback: function(){
                that.onDialogClosed();
              }
            },
            success: {
              label: localization.tt('lbl.add'),
              className: 'btn-success',
              callback: function(){
                var errs = that.view.validate();
                if(_.isEmpty(errs)){
                  that.saveDevice();
                } else {
                  return false;
                }
              }
            }
          }
        });
      });
    },
    saveDevice: function(){
      var that = this;
      var model = this.view.getData();
      var msg;
      if(model.id){
        msg = localization.tt('dialogMsg.deviceUpdatedSuccessfully');
      } else {
        msg = localization.tt('dialogMsg.newDeviceAddedSuccessfully');
      }
      model.save({},{
        success:function(model, response, options){
          Utils.notifySuccess(msg);
          that.fetchData();
        }, 
        error: function(model, response, options){
          var msg;
          if(response.responseJSON.responseMessage){
            msg = response.responseJSON.responseMessage;
          } else {
            msg = response.responseJSON.message;
          }
          Utils.notifyError(msg);
        }
      });
    },
    evEditAction: function(e){
      var model = this.getModel(e);
      this.evAddDevice(model);
    },
    evDeleteAction: function(e){
      var model = this.getModel(e);
      var that = this;
      model.destroy({
        success: function(model,response){
          Utils.notifySuccess(localization.tt('dialogMsg.deviceDeletedSuccessfully'));
          that.fetchData();
        },
        error: function(model, response, options){
          var msg;
          if(response.responseJSON.responseMessage){
            msg = response.responseJSON.responseMessage;
          } else {
            msg = response.responseJSON.message;
          }
          Utils.notifyError(msg);
        }
      });
    },
    getModel: function(e){
      var currentTarget = $(e.currentTarget);
      var id = currentTarget.data().id;
      var model = _.findWhere(this.collection.models, function(model){
        if(model.get('dataSourceId') === id)
          return model;
      });
      model.set('id',model.get('dataSourceId'));
      model.set('deviceId',JSON.parse(model.attributes.typeConfig).deviceId);
      model.set('version',JSON.parse(model.attributes.typeConfig).version);
      return model;
    },
    onDialogClosed: function(){
      if (this.view) {
        this.view.close();
        this.view.remove();
        this.view = null;
      }
    }
  });
  return vDeviceCatalogView;
});
