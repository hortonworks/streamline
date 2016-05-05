define(['require',
    'hbs!tmpl/site/control-panel',
    'modules/Vent',
    'utils/Utils'
], function(require, tmpl, Vent, Utils) {
    'use strict';

    var vControlPanelView = Marionette.LayoutView.extend({
        template: tmpl,
        templateHelpers: function() {},
        regions: {
            configTab: '#config',
            customTab: '#custom'
        },
        events: {
            'click #cluster-config': 'evShowClusterConfig',
            'click #custom-processor': 'showCustomProcessors',
            'click #add-btn': 'evAddProcessor',
            'click #editAction': 'evEditProcessor'
        },
        initialize: function(options) {
            _.extend(this, options);
            this.vent = Vent;
            this.bindEvents();
        },
        bindEvents: function() {
            var self = this;
            this.listenTo(this.vent, 'controlPanel:Cancel', function() {
                self.customProcessorConfigView = null;
                self.showCustomProcessors();
                window._preventNavigation = false;
                $("body").off('click', '.nav-link');
            });
        },
        onRender: function() {
            this.showClusterConfig();
        },

        evShowClusterConfig: function(e) {
          var self = this;
            if($(e.currentTarget).parent().hasClass("active"))
              return;
            if(window._preventNavigation){
              e.preventDefault();
              e.stopImmediatePropagation();
              var target = e.currentTarget,
              message = 'Are you sure you want to leave this page ?',
              title = 'Confirm',
              successCallback = function(){
                    window._preventNavigation = false;
                    $("body").off('click', '.nav-link');
                    self.showClusterConfig();
              };
              Utils.ConfirmDialog(message, title, successCallback);
            }
        },

        showClusterConfig: function(e) {
          if(!_.isUndefined(e) && $(e.currentTarget).parent().hasClass("active"))
            return;
          this.$("#cluster-config").tab('show');
            var self = this;
            require(['collection/VClusterList', 'views/clusterConfig/ComponentMaster'], function(VClusterList, configView) {
                var collection = new VClusterList(),
                    createStormCluster = true,
                    createKafkaCluster = true,
                    createHdfsCluster = true;
                collection.fetch({
                    async: false,
                    success: function(collection, response, options) {
                        if (collection.models.length) {
                            _.each(collection.models, function(model) {
                                if (model.get('type') === 'STORM') {
                                    createStormCluster = false;
                                } else if (model.get('type') === 'KAFKA') {
                                    createKafkaCluster = false;
                                } else if (model.get('type') === 'HDFS') {
                                    createHdfsCluster = false;
                                }
                            });
                        }
                    }
                });
                self.configTab.show(new configView({
                    clusterCollection: collection,
                    createStormCluster: createStormCluster,
                    createKafkaCluster: createKafkaCluster,
                    createHdfsCluster: createHdfsCluster
                }));
            });
        },
        showCustomProcessors: function(e) {
          if(!_.isUndefined(e) && $(e.currentTarget).parent().hasClass("active"))
            return;
          this.$("#custom-processor").tab('show');
            var self = this;
            require(['views/customProcessor/CustomProcessorView'], function(CustomProcessorView) {
                self.customTab.show(new CustomProcessorView());
            });
        },
        evAddProcessor: function(e) {
            var self = this;
            require(['views/customProcessor/CustomProcessorConfigView'], function(CustomProcessorConfigView) {
                self.customProcessorConfigView = new CustomProcessorConfigView();
                self.customTab.show(self.customProcessorConfigView);
            });
        },
        evEditProcessor: function(e) {
            var id = $(e.currentTarget).data('name'),
                self = this;
            require(['models/VCustomProcessor'], function(VCustomProcessor) {
                var vCustomProcessorModel = new VCustomProcessor();
                vCustomProcessorModel.getCustomProcessor({
                    id: id,
                    success: function(model, response, options) {
                        vCustomProcessorModel.set(model.entities[0]);
                        delete vCustomProcessorModel.attributes.entities;
                        delete vCustomProcessorModel.attributes.responseCode;
                        delete vCustomProcessorModel.attributes.responseMessage;
                        require(['views/customProcessor/CustomProcessorConfigView'], function(CustomProcessorConfigView) {
                            self.customTab.show(new CustomProcessorConfigView({
                                model: vCustomProcessorModel
                            }));
                        });
                    },
                    error: function(model, response, options) {
                        Utils.showError(model, response);
                    }
                });
            });
        }

    });
    return vControlPanelView;
});