define(['require',
    'hbs!tmpl/device/deviceCatalog',
    'utils/Utils',
    'collection/VDatasourceList',
    'models/VDatasource',
    'utils/TableLayout',
    'utils/LangSupport',
    'modules/Modal',
    'bootbox'
  ], function(require, tmpl, Utils, VDatasourceList, VDatasource, VTableLayout, localization, Modal, bootbox){
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
      'click #editAction': 'evEditAction',
      'click #deleteAction': 'evDeleteAction'
    },

    initialize: function (options) {
      this.collection = new VDatasourceList();
      this.collection.comparator = 'dataSourceId';
    },

    onRender: function () {
      this.showTable();
      this.fetchData();
    },
    fetchData: function(){
      var that = this;
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
          emptyText: localization.tt('msg.noDeviceFound'),
          className: 'table table-backgrid table-bordered table-striped table-condensed'
        }
      });
    },
    getColumns: function(){
      return [{
        name: 'dataSourceName',
        cell: 'uri',
        label: localization.tt('lbl.deviceName'),
        hasTooltip: false,
        editable: false,
        href: function(model) {
          return '#!/device-catalog/' + model.get('dataSourceId');
        },
      },{
        name: 'description',
        cell: 'string',
        label: localization.tt('lbl.description'),
        hasTooltip: false,
        editable: false
      // },{
      //   name: 'type',
      //   cell: 'string',
      //   label: localization.tt('lbl.type'),
      //   hasTooltip: false,
      //   editable: false
      }, {
        name: 'tags',
        cell: 'string',
        label: localization.tt('lbl.tags'),
        hasTooltip: false,
        editable: false
      }, {
        name: 'model',
        cell: 'string',
        label: localization.tt('lbl.deviceModel'),
        hasTooltip: false,
        editable: false,
        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
          fromRaw: function (rawValue,model) {
            return JSON.parse(model.get('typeConfig')).model;
          }
        })
      }, {
        name: 'typeConfig',
        cell: 'string',
        label: localization.tt('lbl.deviceMake'),
        hasTooltip: false,
        editable: false,
        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
          fromRaw: function (rawValue) {
            return JSON.parse(rawValue).make;
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
                // return "<button title='Delete' class='btn btn-danger btn-xs' data-id="+model.get('dataSourceId')+" id='deleteAction' type='default' ><i class='fa fa-trash'></i></button>";
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

      require(['views/datasource/DataSourceFeedView'], function(DataSourceFeedView){
        var view = new DataSourceFeedView({
          model: model,
          isEdit: model.has('dataSourceId')
        });
        
        var modal = new Modal({
          title: (model.has('dataSourceId')) ? 'Edit Device' : 'Add Device',
          content: view,
          contentWithFooter: true,
          showFooter: false
        }).open();

        view.on('closeModal',function(){
          modal.trigger('cancel');
          that.fetchData();
        });
      });
    },
    evEditAction: function(e){
      var model = this.getModel(e);
      this.evAddDevice(model);
    },
    evDeleteAction: function(e){
      var self = this;
      bootbox.confirm("Do you really want to delete this device ?", function(result){
        if(result){
          var model = self.getModel(e);
          model.destroy({
            success: function(model,response){
              Utils.notifySuccess(localization.tt('dialogMsg.deviceDeletedSuccessfully'));
              self.fetchData();
            },
            error: function(model, response, options){
              Utils.showError(model, response);
            }
          });
        }
      });
    },
    getModel: function(e){
      var currentTarget = $(e.currentTarget);
      var id = currentTarget.data().id;
      var model = _.find(this.collection.models, function(model){
        if(model.get('dataSourceId') === id)
          return model;
      });
      // model.set('id',model.get('dataSourceId'));
      model.set('make',JSON.parse(model.attributes.typeConfig).make);
      model.set('model',JSON.parse(model.attributes.typeConfig).model);
      return model;
    },
  });
  return vDeviceCatalogView;
});
