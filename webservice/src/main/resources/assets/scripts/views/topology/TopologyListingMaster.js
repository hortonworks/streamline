define(['require',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'modules/Modal',
  'hbs!tmpl/topology/topologyListingMaster',
  'collection/VTopologyList',
  'utils/TableLayout',
  'bootbox',
], function(require, localization, Utils, Globals, Modal, tmpl, VTopologyList, TableLayout, bootbox) {
  'use strict';

  var TopologyListingView = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #deleteTopology'     : 'evDeleteTopology',
      'click #addTopology'        : 'evAddTopology'
    },

    regions: {
      tableLayout: '#rTable',
      rFilter: 'div[data-id="r_filter"]'
    },

    ui: {
      
    },

    initialize: function(options) {
      this.collection = new VTopologyList();
      this.collection.comparator = 'id';
    },

    onRender:function(){
      this.showTable();
      this.fetchData();
    },

    fetchData: function(){
      this.collection.fetch({
        reset:true
      });
    },

    showTable: function(){
      this.tableLayout.show(this.getTable());
    },

    getTable: function(){
      return new TableLayout({
        parentView: this,
        columns: this.getColumns(),
        collection: this.collection,
        includeFilter: true,
        gridOpts: {
          emptyText: localization.tt('msg.noTopologyFound'),
          className: 'table table-backgrid table-bordered table-striped table-condensed'
        }
      });
    },

    getColumns: function(){
      return [{
        name: 'name',
        cell: 'uri',
        label: localization.tt('lbl.topologyName'),
        hasTooltip: false,
        editable: false,
        href: function(model){
          return '#!/topology-editor/' + model.get('id');
        }
      }, {
      //   name: 'state',
      //   cell: 'string',
      //   label: localization.tt('lbl.state'),
      //   hasTooltip: false,
      //   editable: false
      // }, {
        name: 'timestamp',
        cell: 'string',
        label: localization.tt('lbl.lastUpdatedOn'),
        hasTooltip: false,
        editable: false,
        formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
            fromRaw: function(rawValue, model) {
              if (model) {
                return new Date(model.get('timestamp'));
              }
            }
          })
      }, {
          name: "actions",
          cell: "Html",
          label: localization.tt('lbl.actions'),
          hasTooltip: false,
          editable: false,
          formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
            fromRaw: function(rawValue, model) {
              if (model) {
                return "<button title='Delete' class='btn btn-danger btn-xs' data-id="+model.get('id')+" id='deleteTopology' type='default' ><i class='fa fa-trash'></i></button>";
              }
            }
          })
        }];
    },

    evAddTopology: function(e){
      require(['views/topology/CreateTopologyView'], function(CreateTopologyView){
        var view = new CreateTopologyView();

        var modal = new Modal({
          title: 'Create Topology',
          content: view,
          showFooter: false,
          escape: false
        }).open();

        view.on('closeModal',function(){
          modal.trigger('cancel');
        });
      });
    },
    evDeleteTopology: function(e){
      var self = this;
      bootbox.confirm("Do you really want to delete this topology ?", function(result){
        if(result){
          var model = self.getModel(e);
          model.destroy({
            success: function(model,response){
              Utils.notifySuccess(localization.tt('dialogMsg.topologyDeletedSuccessfully'));
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
        if(model.get('id') === id)
          return model;
      });
      return model;
    },

  });
  
  return TopologyListingView;
});