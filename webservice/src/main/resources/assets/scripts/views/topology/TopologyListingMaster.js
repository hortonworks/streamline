define(['require',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'modules/Modal',
  'hbs!tmpl/topology/topologyListingMaster',
  'collection/VTopologyList',
  'utils/TableLayout'
], function(require, localization, Utils, Globals, Modal, tmpl, VTopologyList, TableLayout) {
  'use strict';

  var TopologyListingView = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #deleteTopology'     : 'evDeleteTopology'
    },

    regions: {
      tableLayout: '#rTable',
      rFilter: 'div[data-id="r_filter"]'
    },

    ui: {
      
    },

    initialize: function(options) {
      this.collection = new VTopologyList();
    },

    onRender:function(){
      this.showTable();
      this.fetchSummary();
    },

    fetchSummary: function(){
      // this.collection.fetch({reset:true});
      for(var i = 1 ; i <= 5; i++){
        var model = new Backbone.Model();
        model.set('dataStreamName', 'Topology '+i);
        model.set('dataStreamId', i);
        model.set('state', 'Topology 1');
        model.set('timestamp', new Date());
        this.collection.add(model);
      }
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
        name: 'dataStreamName',
        cell: 'uri',
        label: localization.tt('lbl.topologyName'),
        hasTooltip: false,
        editable: false,
        href: function(model){
          return '#!/topology-editor/' + model.get('dataStreamId');
        }
      }, {
        name: 'state',
        cell: 'string',
        label: localization.tt('lbl.state'),
        hasTooltip: false,
        editable: false
      }, {
        name: 'timestamp',
        cell: 'string',
        label: localization.tt('lbl.lastUpdatedOn'),
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
                return "<button title='Delete' class='btn btn-danger btn-xs' data-id="+model.get('dataStreamId')+" id='deleteAction' type='default' ><i class='fa fa-trash'></i></button>";
              }
            }
          })
        }];
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
    }

  });
  
  return TopologyListingView;
});