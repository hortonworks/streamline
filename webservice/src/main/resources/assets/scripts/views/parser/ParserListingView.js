define([
  'require',
  'hbs!tmpl/parser/parserListing',
  'utils/Utils',
  'collection/VParserList',
  'utils/TableLayout',
  'utils/LangSupport',
  'bootbox',
  'utils/Globals'
  ], function(require, tmpl, Utils, VParserList, VTableLayout, localization, bootbox, Globals){
  'use strict';

  var vParserListing = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function() {},

    regions: {
      tableLayout: '#rTable',
      rFilter: 'div[data-id="r_filter"]'
    },

    events: {
      'click #addParser': 'evAddParser',
      'click #deleteAction': 'evDeleteAction'
    },

    initialize: function (options) {
      this.collection = new VParserList();
      this.collection.comparator = 'id';
    },

    onRender: function () {
      this.showTable();
      this.fetchSummary();
    },

    getTable: function(){
      return new VTableLayout({
        parentView: this,
        columns: this.getColumns(),
        collection: this.collection,
        includeFilter: true,
        gridOpts: {
          emptyText: localization.tt('msg.noParserFound'),
          className: 'table table-backgrid table-bordered table-striped table-condensed'
        }
      });
    },

    fetchSummary: function(){
      this.collection.fetch({reset:true});
    },

    showTable: function(){
      this.tableLayout.show(this.getTable());
    },
    getColumns: function(){
      return [{
        name: 'name',
        cell: 'string',
        label: localization.tt('lbl.parserName'),
        hasTooltip: false,
        editable: false
      }, {
        name: 'version',
        cell: 'string',
        label: localization.tt('lbl.version'),
        hasTooltip: false,
        editable: false
      }, {
        name: 'className',
        cell: 'string',
        label: localization.tt('lbl.className'),
        hasTooltip: false,
        editable: false
      }, {
        name: 'jarStoragePath',
        cell: 'string',
        label: localization.tt('lbl.jarStoragePath'),
        hasTooltip: false,
        editable: false
      }, {
          name: "actions",
          cell: "Html",
          label: localization.tt('lbl.actions'),
          hasTooltip: false,
          editable: false,
          formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
            fromRaw: function(rawValue, model) {
              if (model) {
                return "<button title='Delete' class='btn btn-danger btn-xs' data-id="+model.get('id')+" id='deleteAction' type='default' ><i class='fa fa-trash'></i></button>";
              }
            }
          })
        }];
    },
    evAddParser: function(){
      var that = this;
      require(['views/parser/ParserForm'],function(ParserFormView){
        if(that.view){
          that.onDialogClosed();
        }
        that.view = new ParserFormView().render();
        bootbox.dialog({
          message: that.view.el,
          title: localization.tt('h.addParser'),
          className: 'parser-dialog',
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
                  return that.saveParser();
                } else {
                  return false;
                }
              }
            }
          }
        });
      });
    },
    saveParser: function(){
      var attrs = this.view.getData(),
          formData = new FormData(),
          obj = {},
          url = Globals.baseURL + '/api/v1/catalog/parsers',
          that = this;
      if(!_.isEqual(attrs.parserJar.name.split('.').pop().toLowerCase(),'jar')){
        Utils.notifyError(localization.tt('dialogMsg.invalidFile'));
        return false;
      }
      formData.append('parserJar', attrs.parserJar);
      obj.name = attrs.name;
      obj.className = attrs.className;
      obj.version = attrs.version;
      formData.append('parserInfo', JSON.stringify(obj));
      formData.append('schemaFromParserJar', true);
      
      var successCallback = function(response){
        Utils.notifySuccess(localization.tt('dialogMsg.newParserAddedSuccessfully'));
        that.fetchSummary();
      };
      var errorCallback = function(model, response, options){
        Utils.showError(model, response);
      };
      Utils.uploadFile(url,formData,successCallback, errorCallback);
    },
    evDeleteAction: function(e){
      var that = this;
      bootbox.confirm("Are you sure you want to delete this parser ?", function(result) {
        if(result){
          var model = that.getModel(e);
          model.destroy({
            success: function(model,response){
              Utils.notifySuccess(localization.tt('dialogMsg.parserDeletedSuccessfully'));
              that.fetchSummary();
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
        if(model.get('id') === id)
          return model;
      });
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
  return vParserListing;
});
