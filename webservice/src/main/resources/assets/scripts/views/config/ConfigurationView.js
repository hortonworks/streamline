define([
  'require',
  'hbs!tmpl/config/configurationView',
  'utils/Utils',
  'collection/VClusterList',
  'models/VCluster',
  'utils/TableLayout',
  'utils/LangSupport',
  'modules/Modal',
  'bootbox'
  ], function(require, tmpl, Utils, VClusterList, VCluster, VTableLayout, localization, Modal, bootbox){
  'use strict';

  var vConfigView = Marionette.LayoutView.extend({
    template: tmpl,
    templateHelpers: function() {},
    regions: {
      tableLayout: '#rTable',
      rFilter: 'div[data-id="r_filter"]'
    },
    events: {
      'click #addCluster': 'evAddCluster',
      'click .expand-link': function(e){
        if(e.currentTarget.children.item().classList.contains('fa-expand')){
          this.$el.find('#addCluster').hide(); 
        } else {
          this.$el.find('#addCluster').show();
        }
        Utils.expandPanel(e);
      },
      'click #editAction': 'evEditAction',
      'click #deleteAction': 'evDeleteAction'
    },
    
    initialize: function(options) {
      this.collection = new VClusterList();
    },
    
    onRender: function(){
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
          emptyText: localization.tt('msg.noClusterFound'),
          className: 'table table-backgrid table-bordered table-striped table-condensed'
        }
      });
    },

    getColumns: function(){
      return [{
        name: 'name',
        cell: 'uri',
        label: localization.tt('lbl.clusterName'),
        hasTooltip: false,
        editable: false,
        href: function(model) {
          return '#!/configuration/' + model.get('id');
        },
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
          name: "actions",
          cell: "Html",
          label: localization.tt('lbl.actions'),
          hasTooltip: false,
          editable: false,
          formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
            fromRaw: function(rawValue, model) {
              if (model) {
                return "<button type='default' title='Edit' class='btn btn-success btn-xs' data-id="+model.get('id')+" id='editAction'><i class='fa fa-edit'></i></button><button title='Delete' class='btn btn-danger btn-xs' data-id="+model.get('id')+" id='deleteAction' type='default' ><i class='fa fa-trash'></i></button>";
              }
            }
          })
        }];
    },

    evAddCluster: function(model){
      var self = this;
      if(model.currentTarget){
        model = new VCluster();
      }

      require(['views/config/ClusterComponentView'], function(ClusterComponentView){
        var view = new ClusterComponentView({
          model: model,
          editClusterFlag: model.has('id'),
          editComponentFlag: false
        });

        var modal = new Modal({
          title: (model.has('id')) ? 'Edit Cluster' : 'Create Cluster',
          content: view,
          showFooter: false,
          okCloses : false
        }).open();

        view.on('closeModal',function(){
          modal.trigger('cancel');
          self.fetchData();
        });

      });
    },

    evEditAction: function(e){
      var model = this.getModel(e);
      this.evAddCluster(model);
    },

    evDeleteAction: function(e){
      var self = this;
      bootbox.confirm("Do you really want to delete this cluster ?", function(result){
        if(result){
          var model = self.getModel(e);
          model.destroy({
            success: function(model,response){
              Utils.notifySuccess(localization.tt('dialogMsg.clusterDeletedSuccessfully'));
              self.fetchData();
            },
            error: function(model, response, options){
              Utils.showError(response);
            }
          });
        }
      });
    },

    getModel: function(e){
      var currentTarget = $(e.currentTarget);
      var id = currentTarget.data().id;
      return this.collection.get(id);
    },

  });
  return vConfigView;
});