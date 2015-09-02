define(['require',
    'hbs!tmpl/datasource/datasourceView',
    'utils/Utils',
    'collection/VDatasourceList',
    'models/VDatasource',
    'utils/TableLayout',
    'utils/LangSupport',
    'bootbox'
  ], function(require, tmpl, Utils, VDatasourceList, VDatasource, VTableLayout, localization, bootbox){
  'use strict';

  var vDatasourceView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    regions: {
      tableLayout: '#rTable',
      rFilter: 'div[data-id="r_filter"]'
    },

    events: {
      'click #addDatasource': 'evAddDatasource',
      'click .expand-link': function(e){
        if(e.currentTarget.children.item().classList.contains('fa-expand')){
          this.$el.find('#addDatasource').hide(); 
        } else {
          this.$el.find('#addDatasource').show();
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
      this.collection.fetch();
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
          emptyText: localization.tt('msg.noDatasourceFound'),
          className: 'table table-backgrid table-bordered table-striped table-condensed'
        }
      });
    },
    getColumns: function(){
      return [{
        name: 'dataSourceName',
        cell: 'string',
        label: localization.tt('lbl.datasourceName'),
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
    evAddDatasource: function(model){
      var that = this;
      if(_.isUndefined(model) || model.currentTarget){
        model = new VDatasource();
      }
      require(['views/device/DeviceForm'],function(DeviceFormView){
        if(that.view){
          that.onDialogClosed();
        }
        that.view = new DeviceFormView({
          model: model
        }).render();
        bootbox.dialog({
          message: that.view.el,
          title:'<div class="navbar">'+
                    '<div class="navbar-inner">'+
                        '<ul class="nav nav-pills">'+
                            '<li class="active"><a href="javascript:void(0);" data-toggle="tab" aria-expanded="true">Step 1</a></li>'+
                            '<li class=""><a href="javascript:void(0);" data-toggle="tab" aria-expanded="false">Step 2</a></li>'+
                        '</ul>'+
                    '</div>'+
                '</div>'+
                '<div id="bar" class="progress">'+
                    '<div class="progress-bar" style="width: 50%;"></div>'+
                '</div>',
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
                  that.saveDatasource();
                } else {
                  return false;
                }
              }
            }
          }
        });
      });
    },
    saveDatasource: function(){
      var that = this;
      var model = this.view.getData();
      var msg;
      if(model.id){
        msg = localization.tt('dialogMsg.datasourceUpdatedSuccessfully');
      } else {
        msg = localization.tt('dialogMsg.newDatasourceAddedSuccessfully');
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
      this.evAddDatasource(model);
    },
    evDeleteAction: function(e){
      var model = this.getModel(e);
      var that = this;
      model.destroy({
        success: function(model,response){
          Utils.notifySuccess(localization.tt('dialogMsg.dataSourceDeletedSuccessfully'));
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
  return vDatasourceView;
});
