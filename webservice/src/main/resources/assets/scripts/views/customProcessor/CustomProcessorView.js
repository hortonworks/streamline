define([
  'require',
  'hbs!tmpl/customProcessor/custom-processor',
  'utils/Utils',
  'models/VCustomProcessor',
  'collection/VCustomProcessorList',
  'utils/TableLayout',
  'utils/LangSupport',
  'bootbox',
  'utils/Globals',
  'modules/Vent'
  ], function(require, tmpl, Utils, CustomProcessorModel, VCustomProcessorList, VTableLayout, localization, bootbox, Globals, Vent){
  'use strict';

  var customProcessorView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {
    },

    regions: {
      tableLayout: '#rTable'
    },

    events: {
      'click #deleteAction': 'evDeleteProcessor'
    },

    initialize: function (options) {
      // var model = new CustomProcessorModel();
      this.collection = new VCustomProcessorList();
      this.vent =  Vent;
      this.collection.fetch({reset: true});
    },

    onRender: function () {
      this.tableLayout.show(this.getTable());
    },
    getTable: function(){
      return new VTableLayout({
        columns: this.getColumns(),
        collection: this.collection,
        gridOpts: {
          className: 'table table-backgrid table-bordered table-striped table-condensed'
        },
        includePagination: false,
        includeFooterRecords: false
      });
    },
    getColumns: function(){
      return [{
        name: 'name',
        cell: 'string',
        label: 'Name',
        hasTooltip: false,
        editable: false
      }, {
        name: 'description',
        cell: 'string',
        label: 'Description',
        hasTooltip: false,
        editable: false
      }, {
        name: 'jarFileName',
        cell: 'string',
        label: 'Jar File Name',
        hasTooltip: false,
        editable: false
      }, {
          name: "Actions",
          cell: "Html",
          label: localization.tt('lbl.actions'),
          hasTooltip: false,
          editable: false,
          formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
            fromRaw: function(rawValue, model) {
              if (model) {
                return "<a href='#!/custom-processor/edit/"+model.get('name')+"' title='Edit' class='btn btn-success btn-xs'><i class='fa fa-edit'></i></a><a href='javascript:void(0)' title='Delete' class='btn btn-danger btn-xs' id='deleteAction' data-name='"+model.get("name")+"' type='default' ><i class='fa fa-trash'></i></a>";
              }
            }
          })
        }];
    },
    evDeleteProcessor: function(e) {
      var name = $(e.currentTarget).data('name'),
          model = this.getModel(name),
          self = this;

      model.destroyModel({
            id: name,
            success: function(model,response){
              Utils.notifySuccess('Custom processor deleted successfully');
              self.collection.fetch({reset: true});
            },
            error: function(model, response, options){
              Utils.showError(model, response);
            }
          });
    },
    getModel: function(name){
      var model = _.find(this.collection.models, function(model){
        if(model.get('name') === name)
          return model;
      });
      return model;
    },

  });
  return customProcessorView;
});
