define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'modules/Modal',
  'hbs!tmpl/topology/dataStreamMaster',
  'models/VTopology',
  'modules/TopologyGraphCreator'
], function(require, Vent, localization, Utils, Globals, Modal, tmpl, VTopology, TopologyGraphCreator) {
  'use strict';

  var DataStreamEditorLayout = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      // 'click .quick-button'       : 'evClickSubMenu',
      'click #submitDatastream'   : 'evSubmitAction',
      'click #deployDatastream'   : 'evDeployAction',
      'click #killDatastream'     : 'evKillAction'
    },

    ui: {
      'btnDS'         : '#btnDS',
      'btnProcessor'  : '#btnProcessor',
      'btnDataSink'   : '#btnDataSink',
      // 'btnSubmit'     : '#submitDatastream',
      // 'btnDeploy'     : '#deployDatastream',
      'editorSubMenu' : '#editorSubhead',
      'graphEditor'   : '#graphEditor'
    },

    initialize: function(options) {
      _.extend(this, options);
      this.tempCount = 1;
      if(!this.model){
        this.model = new VTopology();
      }
      this.vent = Vent;
      this.bindEvents();
    },

    bindEvents: function(){
      var self = this;
      this.listenTo(this.vent, 'dataStream:SavedStep1', function(data){
        self.step1Data = data;
      });
      this.listenTo(this.vent, 'dataStream:SavedStep2', function(data){
        self.step2Data = data;
      });
      this.listenTo(this.vent, 'dataStream:SavedStep3', function(data){
        self.step3Data = data;
      });
      this.listenTo(this.vent, 'click:topologyNode', function(data){
        if(_.isEqual(data.parentType, Globals.Topology.Editor.Steps.Datasource.valStr)){
          self.evDSAction();
        } else if(_.isEqual(data.parentType, Globals.Topology.Editor.Steps.Processor.valStr)){
          self.evProcessorAction();
        } else if(_.isEqual(data.parentType, Globals.Topology.Editor.Steps.DataSink.valStr)){
          self.evDataSinkAction(data.currentType);
        }
      });
    },

    bindDomEvents: function(){
      var self = this;
      //Toggle Event
      this.$('.modal-actions .btnToggle').on('click', function(e){
        if(self.selStepBtn[0] === e.currentTarget){
          self.$('.box-subhead').slideToggle();  
        } else {
          if(!self.$('.box-subhead').is(':hidden')){
            self.$('.box-subhead').slideToggle();  
          }
          setTimeout(function(){
            if(e.currentTarget === self.ui.btnDS[0]){
              self.generateSubmenu('step1');
            } else if(e.currentTarget === self.ui.btnProcessor[0]){
              self.generateSubmenu('step2');
            } else if(e.currentTarget === self.ui.btnDataSink[0]){
              self.generateSubmenu('step3');
            }
            self.$('.box-subhead').slideToggle();
          }, 0);
        }
        if($(e.currentTarget).hasClass('active')){
          $(e.currentTarget).removeClass('active');
        } else {
          $(e.currentTarget).siblings('.active').removeClass('active');
          $(e.currentTarget).addClass('active');
        }
      });

      //Tooltip
      this.$('[data-rel="tooltip"]').tooltip();
      this.$('#infoHelp').popover({
        html: true,
        content: '<p><strong>Drag & Drop</strong> to Create <strong>Node</strong></p><p><strong>Click</strong> on <strong>Node</strong> to <strong>Configure</strong> it</p><p><strong>Drag</strong> the <strong>Node</strong> to <strong>Move</strong></p><p><strong>Press Shift + Click</strong> on <strong>Source Node</strong> and <strong>Drag</strong> to <strong>Target Node</strong> to create a <strong>Link</strong></p><p><strong>Click</strong> on <strong>Link</strong> and <strong>Press Delete/Backspace</strong> to <strong>Delete a Link</strong></p><p><strong>Press Shift + Click</strong> on <strong>Node</strong> and <strong>Press Delete/Backspace</strong> to <strong>Delete a Node</strong></p>',
        placement: 'left',
        trigger: 'hover'
      });
    },

    bindSubMenuDrag: function(){
      this.$('.quick-button').draggable({
        revert: "invalid",
        helper: function (e) {
            //Code here
            return $('<div data-mainmenu="'+e.currentTarget.dataset.mainmenu+'" data-submenu="'+e.currentTarget.dataset.submenu+'"></div>').append('<i class="'+$(e.currentTarget).children().attr('class')+'"></i>');
        }
      });
      this.$('[data-rel="tooltip"]').tooltip();
    },

    generateSubmenu: function(step){
      var arr = [], msg = '', self = this;
      switch(step){
        case 'step1':
          self.selStepBtn = self.ui.btnDS;
          arr = Globals.Topology.Editor.Steps.Datasource.Substeps;
        break;
        case 'step2':
          self.selStepBtn = self.ui.btnProcessor;
          arr = Globals.Topology.Editor.Steps.Processor.Substeps;
        break;
        case 'step3':
          self.selStepBtn = self.ui.btnDataSink;
          arr = Globals.Topology.Editor.Steps.DataSink.Substeps;
        break;
      }
      _.each(arr, function(obj){
        msg += '<dv class="quick-button btn" data-rel="tooltip" title="'+obj.valStr+'" data-submenu="'+obj.valStr+'" data-mainmenu="'+obj.mainStep+'"><i class="'+obj.iconClass+'"></i></dv>'; // '+obj.valStr+'
      });
      self.ui.editorSubMenu.html(msg);
      // this.$('[data-rel="tooltip"]').tooltip();
      this.bindSubMenuDrag();
    },

    onRender:function(){
      var self = this;
      this.selStepBtn = this.ui.btnDS;
      this.bindDomEvents();
      this.bindSubMenuDrag();
      setTimeout(function(){self.renderGraphGenerator();}, 0);
      setTimeout(function(){
        self.$('#graphEditor svg').droppable({
            drop: function(event, ui){
              var mainMenu = ui.helper.data().mainmenu.split(' ').join('');
              var submenu = ui.helper.data().submenu;
              var icon = _.findWhere(Globals.Topology.Editor.Steps[mainMenu].Substeps, {valStr:submenu});
              self.vent.trigger('change:editor-submenu', {
                title: submenu,
                parentStep: ui.helper.data().mainmenu,
                icon: submenu ? icon.iconContent : '',
                currentStep: submenu,
                event: event
              });
            }
        });
      },0);
    },

    renderGraphGenerator: function(){
      var self = this;
      var data = {
        nodes: [],
        edges: []
      };
      var graph = new TopologyGraphCreator({
        elem: this.ui.graphEditor,
        data: data,
        vent: this.vent
      });
      graph.updateGraph();
    },

    evClickSubMenu: function(e){
      // if($(e.currentTarget).hasClass('active')){
      //   this.selSubStep = undefined;
      //   $(e.currentTarget).removeClass('active');
      // } else {
      //   this.selSubStep = e.currentTarget.dataset.submenu;
      //   $(e.currentTarget).siblings('.active').removeClass('active');
      //   $(e.currentTarget).addClass('active');
      // }

      // var mainMenu = e.currentTarget.dataset.mainmenu.split(' ').join('');
      // var icon = _.findWhere(Globals.Topology.Editor.Steps[mainMenu].Substeps, {valStr:this.selSubStep});
      // this.vent.trigger('change:editor-submenu', {
      //   title: this.selSubStep,
      //   parentStep: e.currentTarget.dataset.mainmenu,
      //   icon: this.selSubStep ? icon.iconContent : '',
      //   currentStep: this.selSubStep
      // });
    },

    evDSAction: function(e){
      var self = this;
      require(['views/topology/DataFeedView'], function(DataFeedView){
        self.showModal(new DataFeedView({
          vent: self.vent
        }), 'Source');
      });
    },
    evProcessorAction: function(e){
      var self = this;
      require(['views/topology/DataProcessorView'], function(DataProcessorView){
        self.showModal(new DataProcessorView({
          vent: self.vent
        }), 'Processor');
      });
    },
    evDataSinkAction: function(type){
      var self = this;
      require(['views/topology/DataSinkView'], function(DataSinkView){
        self.showModal(new DataSinkView({
          vent: self.vent,
          type: type
        }), 'Sink');
      });
    },
    showModal: function(view, title){
      var self = this;
      if(this.view){
        this.view = null;
      }
      this.view = view;
      var modal = new Modal({
        title: title,
        content: self.view,
        showFooter: false,
        escape: false,
        //todo - find a beter way to add class
        mainClass: _.isEqual(title, 'Processor') ? 'modal-lg' : ''
      }).open();

      this.view.on('closeModal', function(){
        modal.trigger('cancel');
      });
    },

    evSubmitAction: function(e){
      var self = this;
      var data = {
                  "catalogRootUrl": window.location.origin+"/api/v1/catalog",
                  "dataSources": [
                    { 
                      "uiname": "kafkaDataSource",
                      "id": 1,
                      "type": "KAFKA",
                      "config": {
                        "zkUrl": "localhost:2181",
                        "topic": "nest-topic"
                      }
                    }
                  ],
                  
                  "processors": [
                    {
                      "uiname": "tuplesProcessor",
                      "config": [
                        {
                          "uiname": "goodTuplesRule",
                          "type": "RULE",
                          "id": 1,
                          "config": {
                            "ruleName": "successful-tuples"
                          }
                        },
                        {
                          "uiname": "badTuplesRule",
                          "type": "RULE",
                          "id": 2,
                          "config": {
                            "ruleName": "failed-tuples"
                          }
                        }
                      ]
                    }
                  ],
                 
                  "dataSinks": [
                    {
                      "uiname": "hbasesink",
                      "type": "HBASE",
                      "config": {
                        "rootDir": "hdfs://localhost:9000/hbase",
                        "table": "nest",
                        "columnFamily": "cf",
                        "rowKey": "device_id"
                      }
                    },
                    {
                      "uiname": "hdfssink",
                      "type": "HDFS",
                      "config": {
                        "fsUrl": "file:///",
                        "path": "/tmp/failed-tuples",
                        "name": "data"
                      }
                    }
                  ],
                  
                  "links": [
                    {
                      "uiname": "kafkaDataSource->tuplesProcessor",
                      "from": "kafkaDataSource",
                      "to": "tuplesProcessor"
                    },
                    {
                      "uiname": "tuplesProcessor-goodTuplesRule->hbasesink",
                      "from": "goodTuplesRule",
                      "to": "hbasesink"
                    },
                    {
                      "uiname": "tuplesProcessor-badTuplesRule->hdfssink",
                      "from": "badTuplesRule",
                      "to": "hdfssink"
                    }
                  ]
              };
      var tData = JSON.stringify(data);
      this.model.set({
        dataStreamName: 'topology'+this.tempCount++,
        json: tData
      });
      this.model.save({},{
        success: function(model, response, options){
          self.dataStreamId = response.entity.dataStreamId;
          self.$('#deployDatastream').removeAttr('disabled');
          Utils.notifySuccess('Topology submitted successfully.');
        },
        error: function(model, response, options){
          Utils.showError(model, response);
        }
      });
    },
    evDeployAction: function(e){
      if(this.dataStreamId){
        this.model.deployTopology({
          id: this.dataStreamId,
          success: function(model, response, options){
            self.$('#deployDatastream').attr('disabled',true);
            self.$('#killDatastream').removeAttr('disabled');
            Utils.notifySuccess('Topology deployed successfully.');
          },
          error: function(model, response, options){
            Utils.showError(model, response);
          }
        });
      }
    },
    evKillAction: function(e){
      if(this.dataStreamId){
        this.model.killTopology({
          id: this.dataStreamId,
          success: function(model, response, options){
            self.$('#submitDatastream').removeAttr('disabled');
            self.$('#deployDatastream').removeAttr('disabled');
            Utils.notifySuccess('Topology killed successfully.');
          },
          error: function(model, response, options){
            Utils.showError(model, response);
          }
        });
      }
    }

  });
  
  return DataStreamEditorLayout;
});