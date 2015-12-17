define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'modules/Modal',
  'hbs!tmpl/topology/dataStreamMaster',
  'models/VTopology',
  'modules/TopologyGraphCreatorViaRect',
  'x-editable',
], function(require, Vent, localization, Utils, Globals, Modal, tmpl, VTopology, TopologyGraphCreator, xEditable) {
  'use strict';

  var DataStreamEditorLayout = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #submitDatastream'   : 'evSubmitAction',
      'click #deployDatastream'   : 'evDeployAction',
      'click #killDatastream'     : 'evKillAction'
    },

    ui: {
      'btnDS'         : '#btnDS',
      'btnProcessor'  : '#btnProcessor',
      'btnDataSink'   : '#btnDataSink',
      'editorSubMenu' : '#editorSubhead',
      'graphEditor'   : '#graphEditor'
    },

    initialize: function(options) {
      _.extend(this, options);
      this.tempCount = 1;
      this.dsCount = 0;
      this.pCount = 0;
      this.sCount = 0;
      this.dsArr = [];
      this.processorArr = [];
      this.sinkArr = [];
      this.linkArr = [];
      if(!this.model){
        this.model = new VTopology();
      }
      this.getAllConfigurations();
      this.vent = Vent;
      this.bindEvents();
    },

    configSortComparator:function(a,b) {
        if ( a.isOptional < b.isOptional )
            return -1;
        if ( a.isOptional > b.isOptional )
            return 1;
        return 0;
    },

    getAllConfigurations: function(){
      this.sourceConfigArr = [];
      this.processorConfigArr = [];
      this.sinkConfigArr = [];
      this.linkConfigArr = [];
      var self = this;

      this.model.getSourceComponent({
        success: function(model, response, options){
          self.sourceConfigArr = model.entities;
        },
        error: function(model, response, options){
          Utils.notifyError('Source Component Configurations '+options);
        }
      });

      this.model.getProcessorComponent({
        success: function(model, response, options){
          self.processorConfigArr = model.entities;
        },
        error: function(model, response, options){
          Utils.notifyError('Processor Component Configurations '+options);
        }
      });
      
      this.model.getSinkComponent({
        success: function(model, response, options){
          self.sinkConfigArr = model.entities;
        },
        error: function(model, response, options){
          Utils.notifyError('Sink Component Configurations '+options);
        }
      });

      this.model.getLinkComponent({
        success: function(model, response, options){
          self.linkConfigArr = model.entities;
        },
        error: function(model, response, options){
          Utils.notifyError('Link Component Configurations '+options);
        }
      });
    },

    bindEvents: function(){
      var self = this;
      
      this.listenTo(this.vent, 'dataStream:SavedStep1', function(data){
        self.dsArr[data.get('_nodeId')] = data.toJSON();
      });
      
      this.listenTo(this.vent, 'dataStream:SavedStep2', function(data){
        self.processorArr[data.get('_nodeId')] = data.toJSON();
      });
      
      this.listenTo(this.vent, 'dataStream:SavedStep3', function(data){
        self.syncSinkData(data);
        self.sinkArr[data.get('_nodeId')] = data.toJSON();
      });

      this.listenTo(this.vent, 'click:topologyNode', function(data){
        if(!_.isString(data.nodeId) || !data.nodeId.startsWith('Parser')){
          var model = new Backbone.Model();
          var nodeId = data.nodeId;
          switch(data.parentType){
            //Source
            case Globals.Topology.Editor.Steps.Datasource.valStr:
              if(this.dsArr[nodeId]){
                model.set(this.dsArr[nodeId]);
              } else {
                model.set('_nodeId',nodeId);
                model.set('firstTime',true);
                model.set('uiname', 'Source');
                model.set('currentType', data.currentType);
                self.setHiddenConfigFields(model, _.findWhere(this.sourceConfigArr, {subType: 'KAFKA'}));
              }
              self.evDSAction(model);
            break;

            //Processor
            case Globals.Topology.Editor.Steps.Processor.valStr:
              if(this.processorArr[nodeId]){
                model.set(this.processorArr[nodeId]);
              } else {
                model.set('_nodeId',nodeId);
                model.set('firstTime',true);
                model.set('uiname', data.currentType);
                model.set('currentType', data.currentType);
                if(_.isEqual(data.currentType, "Parser")){
                  self.setHiddenConfigFields(model, _.findWhere(this.processorConfigArr, {subType: 'PARSER'}));
                } else if(_.isEqual(data.currentType, "Rule")){
                  self.setHiddenConfigFields(model, _.findWhere(this.processorConfigArr, {subType: 'RULE'}));
                }
              }
              if(this.verifyLink(model.get('currentType'), model.get('_nodeId'))){
                self.evProcessorAction(model);
              }
            break;

            //Sink
            case Globals.Topology.Editor.Steps.DataSink.valStr:
              if(this.sinkArr[nodeId]){
                model.set(this.sinkArr[nodeId]);
              } else {
                model.set('_nodeId',nodeId);
                model.set('firstTime',true);
                model.set('uiname', data.currentType);
                model.set('currentType', data.currentType);
                self.setHiddenConfigFields(model, _.findWhere(this.sinkConfigArr, {subType: data.currentType}));
              }
              if(this.verifyLink(model.get('currentType'), model.get('_nodeId'))){
                self.evDataSinkAction(model, data.currentType);
              }
            break;
          }
        }
      });

      this.listenTo(this.vent, 'delete:topologyNode', function(data){
        var nodeId = data.nodeId;
        if(_.isEqual(data.parentType, Globals.Topology.Editor.Steps.Datasource.valStr)){
          if(this.dsArr[nodeId]){
            this.dsArr[nodeId] = undefined;
          }
        } else if(_.isEqual(data.parentType, Globals.Topology.Editor.Steps.Processor.valStr)){
          if(this.processorArr[nodeId]){
            this.processorArr[nodeId] = undefined;
          }
        } else if(_.isEqual(data.parentType, Globals.Topology.Editor.Steps.DataSink.valStr)){
          if(this.sinkArr[nodeId]){
            this.sinkArr[nodeId] = undefined;
          }
        }
        this.linkArr = data.linkArr;
      });

      this.listenTo(this.vent, 'topologyLink', function(linkArr){
        this.linkArr = linkArr;
      });
    },

    verifyLink: function(type, nodeId){
      var obj = this.linkArr.filter(function(obj){
        return (obj.target.currentType === type && obj.target.nodeId === nodeId);
      });
      if(!obj.length){
        Utils.notifyError('Connect the node to configre.');
        return false;
      } else {
        return true;
      }
    },

    setHiddenConfigFields: function(model, obj){
      if(obj){
        var configFields = JSON.parse(obj.config);
        var hiddenFields = {
          type: obj.subType,
          transformationClass: obj.transformationClass
        };
        model.set('hiddenFields', hiddenFields);
        model.set('config', configFields);
      }
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
        content: '<p><strong>Drag & Drop</strong> to Create <strong>Node</strong></p><p><strong>Click</strong> on <strong>Node</strong> to <strong>Configure</strong> it</p><p><strong>Drag</strong> the <strong>Node</strong> to <strong>Move</strong></p><p><strong>Press Shift + Click</strong> on <strong>Source Node</strong> and <strong>Drag</strong> to <strong>Target Node</strong> to create a <strong>Link</strong></p><p><strong>Click</strong> on <strong>Link</strong> and <strong>Press Delete</strong> to <strong>Delete a Link</strong></p><p><strong>Press Shift + Click</strong> on <strong>Node</strong> and <strong>Press Delete</strong> to <strong>Delete a Node</strong></p>',
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
              var mainmenu = ui.helper.data().mainmenu.split(' ').join('');
              var submenu = ui.helper.data().submenu;
              var icon = _.findWhere(Globals.Topology.Editor.Steps[mainmenu].Substeps, {valStr:submenu});
              var id, otherId;
              if(_.isEqual(mainmenu, Globals.Topology.Editor.Steps.Datasource.valStr)){
                id = self.dsCount++;
                self.dsArr[id] = undefined;
                otherId = self.pCount++;
                self.processorArr[otherId] = undefined;
              } else if(_.isEqual(mainmenu, Globals.Topology.Editor.Steps.Processor.valStr)){
                id = self.pCount++;
                self.processorArr[id] = undefined;
              } else if(_.isEqual(ui.helper.data().mainmenu, Globals.Topology.Editor.Steps.DataSink.valStr)){
                id = self.sCount++;
                self.sinkArr[id] = undefined;
              }

              self.vent.trigger('change:editor-submenu', {
                title: submenu,
                parentStep: ui.helper.data().mainmenu,
                icon: submenu ? icon.iconContent : '',
                currentStep: submenu,
                id: id,
                otherId: otherId,
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

    evDSAction: function(model){
      if(model.has('config')){
        model.get('config').sort(this.configSortComparator);
      }
      var self = this;
      var obj = {
        iconClass: "fa fa-server",
        titleHtmlFlag: true,
        titleName: model.get('uiname'),
        type: 'Source'
      };
      require(['views/topology/DataFeedView'], function(DataFeedView){
        self.showModal(new DataFeedView({
          model: model,
          vent: self.vent
        }), obj);
      });
    },
    evProcessorAction: function(model){
      var self = this;
      var obj = {
        iconClass: "fa fa-cog",
        titleHtmlFlag: true,
        titleName: model.get('uiname'),
        type: "Processor"
      };
      switch(model.get('currentType')){
        case 'Parser':
          if(this.syncParserData(model)){
            require(['views/topology/ParserProcessorView'], function(ParserProcessorView){
              self.showModal(new ParserProcessorView({
                model: model,
                vent: self.vent
              }), obj);
            });
          }
        break;

        case 'Rule':
          if(this.syncRuleData(model)){
            require(['views/topology/RuleProcessorView'], function(RuleProcessorView){
              self.showModal(new RuleProcessorView({
                model: model,
                vent: self.vent
              }), obj);
            });
          }
        break;
      }
    },
    syncParserData: function(model){
      var obj = this.linkArr.filter(function(obj){
        return (obj.target.currentType === 'Parser' && obj.target.nodeId === model.get('_nodeId'));
      });
      if(obj.length){
        var sourceData = this.dsArr[obj[0].source.nodeId];
        if(!sourceData){
          Utils.notifyError("Configure the connected source node first.");
          return false;
        } else {
          model.set('parserId', sourceData._selectedTable[0].parserId);
          model.set('dataSourceId', sourceData._selectedTable[0].datasourceId);
          model.set('parserName', sourceData._selectedTable[0].parserName);
          model.set('dataSourceName', sourceData._selectedTable[0].datasourceName);
          if(!model.has('parallelism')){
            model.set('parallelism', 1);
          }
          return true;
        }
      }
    },
    syncRuleData: function(model){
      var obj = this.linkArr.filter(function(obj){
        return (obj.target.currentType === 'Rule' && obj.target.nodeId === model.get('_nodeId'));
      });
      if(obj.length){
        var sourceData = this.processorArr[obj[0].source.nodeId];
        if(!sourceData){
          Utils.notifyError("Configure the connected node first.");
          return false;
        } else {
          model.set('parserId', sourceData.parserId);
          model.set('dataSourceId', sourceData.dataSourceId);
          return true;
        }
      }
    },
    syncSinkData: function(model, validationFlag){
      var obj = this.linkArr.filter(function(obj){
        return (obj.target.parentType === 'Data Sink' && obj.target.nodeId === model.get('_nodeId'));
      });
      if(obj.length){
        if(obj[0].source.parentType === 'Processor'){
          var sourceData = this.processorArr[obj[0].source.nodeId];
          if(!sourceData){
            Utils.notifyError("Configure the connected node first.");
            return false;
          }

          if(obj[0].source.currentType === 'Rule' && !validationFlag){
            sourceData.newConfig.rulesProcessorConfig.rules[0].action.components = [{
              name: model.get('uiname'),
              id: new Date().getTime(),
              description: 'Auto-Generated For '+model.get('uiname'),
              declaredInput: sourceData.newConfig.rulesProcessorConfig.declaredInput
            }];
          }
          return true;
        }
        // if(obj[0].source.currentType === 'Rule'){
        //   var sourceData = this.processorArr[obj[0].source.nodeId];
        //   if(!sourceData){
        //     Utils.notifyError("Configure the connected node first.");
        //     return false;
        //   } else if(!validationFlag){
        //     sourceData.newConfig.rulesProcessorConfig.rules[0].action.components = [{
        //       name: model.get('uiname'),
        //       id: new Date().getTime(),
        //       description: 'Auto-Generated For '+model.get('uiname'),
        //       declaredInput: sourceData.newConfig.rulesProcessorConfig.declaredInput
        //     }];
        //     return true;
        //   }
        // }
      }
    },
    evDataSinkAction: function(model, type){
      if(model.has('config')){
        model.get('config').sort(this.configSortComparator);
      }
      var self = this;
      var obj = {
        iconClass: "fa fa-database",
        titleHtmlFlag: true,
        titleName: model.get('uiname'),
        type: "Sink"
      };
      if(this.syncSinkData(model, true)){
        require(['views/topology/DataSinkView'], function(DataSinkView){
          self.showModal(new DataSinkView({
            model: model,
            vent: self.vent,
            type: type
          }), obj);
        });
      }
    },
    showModal: function(view, object){
      var self = this,
          titleHtml;
      if(this.view){
        this.view = null;
      }
      if(object.titleHtmlFlag){
        titleHtml = '<i class="'+object.iconClass+'" style="padding-right: 5px;"></i><a href="javascript:void(0)" id="editableTitle" data-type="text"> '+object.titleName+'</a>';
      }
      this.view = view;
      var modal = new Modal({
        title: titleHtml,
        titleHtml: object.titleHtmlFlag,
        content: self.view,
        showFooter: false,
        escape: false,
        //todo - find a beter way to add class
        mainClass: _.isEqual(object.type, 'Processor') ? 'modal-lg' : ''
      }).open();

      modal.$('#editableTitle').editable({
          mode:'inline',
          validate: function(value) {
           if(_.isEqual($.trim(value), '')) return 'Name is required';
        }
      });

      modal.$('.editable').on('save', function(e, params) {
        modal.options.content.model.set('uiname', params.newValue);
      });

      this.view.on('closeModal', function(){
        modal.trigger('cancel');
      });
    },

    evSubmitAction: function(e){
      var self = this,
          ds = [],
          processors = [],
          sink = [],
          links = [];
      var tempData = {
        config: {
          "hbaseConf": {
            "hbase.rootdir": "hdfs://localhost:9000/tmp/hbase"
          },
          "local.parser.jar.path": "/tmp",
          "local.notifier.jar.path": "/tmp"
        },
        dataSources: [],
        processors: [],
        dataSinks: [],
        links: []
      };

      if(_.isEqual(this.dsArr.length, this.dsCount)){
        _.each(this.dsArr, function(obj){
          if(obj){
            var configObj = {};
            _.each(obj.config, function(o){
              if(obj[o.name]){
                configObj[o.name] = obj[o.name];
              }
            });
            ds.push({
              "uiname": obj.uiname,
              "type": obj.hiddenFields ? obj.hiddenFields.type : '',
              "transformationClass": obj.hiddenFields ? obj.hiddenFields.transformationClass : '',
              "config": configObj
            });
          }
        });
        tempData.dataSources = ds;
      } else {
        Utils.notifyError('There are some unconfigured nodes present. Kindly configure to proceed.');
        return false;
      }
      if(_.isEqual(this.processorArr.length, this.pCount)){
        _.each(this.processorArr, function(obj){
          if(obj){
            if(obj.hiddenFields.type === 'PARSER'){
              processors.push({
                "uiname": obj.uiname,
                "type": obj.hiddenFields ? obj.hiddenFields.type : '',
                "transformationClass": obj.hiddenFields ? obj.hiddenFields.transformationClass : '',
                "config": {
                  "parsedTuplesStream": "parsedTuplesStream",
                  "failedTuplesStream": "failedTuplesStream",
                  "parserId": 1,
                  "dataSourceId": 1,
                  "parallelism": 1
                }
              });
            } else if(obj.hiddenFields.type === 'RULE'){
              processors.push({
                "uiname": obj.uiname,
                "type": obj.hiddenFields ? obj.hiddenFields.type : '',
                "transformationClass": obj.hiddenFields ? obj.hiddenFields.transformationClass : '',
                "config": obj.newConfig
              });
            }
          }
        });
        tempData.processors = processors;
      } else {
        Utils.notifyError('There are some unconfigured nodes present. Kindly configure to proceed.');
        return false;
      }
      if(_.isEqual(this.sinkArr.length, this.sCount)){
        _.each(this.sinkArr, function(obj){
          if(obj){
            var configObj = {};
            _.each(obj.config, function(o){
              if(obj[o.name]){
                configObj[o.name] = obj[o.name];
              }
            });
            sink.push({
              "uiname": obj.uiname,
              "type": obj.hiddenFields ? obj.hiddenFields.type : '',
              "transformationClass": obj.hiddenFields ? obj.hiddenFields.transformationClass : '',
              "config": configObj
            });
          }
        });
        tempData.dataSinks = sink;
      } else {
        Utils.notifyError('There are some unconfigured nodes present. Kindly configure to proceed.');
        return false;
      }
      var flag = true;
      _.each(this.linkArr,function(obj){
        if(flag){
          var sourceObj, targetObj;
          if(obj.source.parentType === 'Datasource'){
            sourceObj = self.dsArr[obj.source.nodeId];
          } else if(obj.source.parentType === 'Processor'){
            sourceObj = self.processorArr[obj.source.nodeId];
          } else if(obj.source.parentType === 'Data Sink'){
            sourceObj = self.sinkArr[obj.source.nodeId];
          }
          if(obj.target.parentType === 'Datasource'){
            targetObj = self.dsArr[obj.target.nodeId];
          } else if(obj.target.parentType === 'Processor'){
            targetObj = self.processorArr[obj.target.nodeId];
          } else if(obj.target.parentType === 'Data Sink'){
            targetObj = self.sinkArr[obj.target.nodeId];
          }

          if(!sourceObj || !targetObj){
            flag = false;
            Utils.notifyError("There are some unconfigured nodes present. Kindly configure to proceed.");
          } else {
            var tempObj = {
              uiname: sourceObj.uiname + '->' + targetObj.uiname,
              type: "SHUFFLE",
              transformationClass: "com.hortonworks.iotas.topology.storm.ShuffleGroupingLinkFluxComponent",
              config: {
                "from": sourceObj.uiname,
                "to": targetObj.uiname,
              }
            };
            if(sourceObj.currentType === 'Parser'){
              tempObj.config.streamId = obj.target.streamId;
            }
            if(sourceObj.currentType === 'Rule'){
              tempObj.config.streamId = sourceObj.newConfig.rulesProcessorConfig.name+'.'+sourceObj.newConfig.rulesProcessorConfig.rules[0].name+'.'+sourceObj.newConfig.rulesProcessorConfig.rules[0].id;
            }
            tempData.links.push(tempObj);
          }
        }
      });
      console.log(tempData);
      // var data = {
      //             "config": {
      //               "hbaseConf": {
      //                 "hbase.rootdir": "hdfs://localhost:9000/tmp/hbase"
      //               },
      //               "local.parser.jar.path": "/tmp",
      //               "local.notifier.jar.path": "/tmp"
      //             },
      //             "dataSources": [
      //               {
      //                 "uiname": "kafkaDataSource",
      //                 "type": "KAFKA",
      //                 "transformationClass": "com.hortonworks.iotas.topology.KafkaSpoutFluxComponentYaml",
      //                 "config": {
      //                   "zkUrl": "localhost:2181",
      //                   "zkPath": "/brokers",
      //                   "refreshFreqSecs": 60,
      //                   "topic": "nest-topic",
      //                   "zkRoot": "/Iotas-kafka-spout",
      //                   "spoutConfigId": "nest-kafka-spout-config",
      //                   "fetchSizeBytes": 1048576,
      //                   "socketTimeoutMs": 10000,
      //                   "fetchMaxWait": 10000,
      //                   "bufferSizeBytes": 1048576,
      //                   "ignoreZkOffsets": false,
      //                   "maxOffsetBehind": 9223372036854776000,
      //                   "useStartOffsetTimeIfOffsetOutOfRange": true,
      //                   "metricsTimeBucketSizeInSecs": 60,
      //                   "stateUpdateIntervalMs": 2000,
      //                   "retryInitialDelayMs": 0,
      //                   "retryDelayMultiplier": 1,
      //                   "retryDelayMaxMs": 60000,
      //                   "parallelism": 1
      //                 }
      //               }
      //             ],
      //             "processors": [
      //               {
      //                 "uiname": "parserProcessor",
      //                 "type": "PARSER",
      //                 "transformationClass": "com.hortonworks.iotas.topology.ParserBoltFluxComponentYaml",
      //                 "config": {
      //                   "parsedTuplesStream": "parsedTuplesStream",
      //                   "failedTuplesStream": "failedTuplesStream",
      //                   "parserId": 1,
      //                   "dataSourceId": 1,
      //                   "parallelism": 1
      //                 }
      //               },
      //               {
      //                 "uiname": "ruleProcessor",
      //                 "type": "RULE",
      //                 "transformationClass": "com.hortonworks.iotas.topology.RuleBoltFluxComponentYaml",
      //                 "config": {
      //                   "parallelism": 1,
      //                   "rulesProcessorConfig": {
      //                     "rules": [
      //                       {
      //                         "name": "rule_1",
      //                         "id": 1,
      //                         "ruleProcessorName": "rule_processsor_1",
      //                         "condition": {
      //                           "conditionElements": [
      //                             {
      //                               "firstOperand": {
      //                                 "name": "temperature",
      //                                 "type": "INTEGER"
      //                               },
      //                               "operation": "LESS_THAN",
      //                               "secondOperand": "100",
      //                               "logicalOperator": "AND"
      //                             },
      //                             {
      //                               "firstOperand": {
      //                                 "name": "humidity",
      //                                 "type": "INTEGER"
      //                               },
      //                               "operation": "LESS_THAN",
      //                               "secondOperand": "50",
      //                               "logicalOperator": null
      //                             }
      //                           ]
      //                         },
      //                         "action": {
      //                           "components": [
      //                             {
      //                               "name": "sink_1",
      //                               "id": 1,
      //                               "description": "sink_1_desc",
      //                               "declaredInput": [
      //                                 {
      //                                   "name": "temperature",
      //                                   "type": "INTEGER"
      //                                 },
      //                                 {
      //                                   "name": "humidity",
      //                                   "type": "INTEGER"
      //                                 }
      //                               ]
      //                             },
      //                             {
      //                               "name": "sink_2",
      //                               "id": 1,
      //                               "description": "sink_2_desc",
      //                               "declaredInput": [
      //                                 {
      //                                   "name": "temperature",
      //                                   "type": "INTEGER"
      //                                 },
      //                                 {
      //                                   "name": "humidity",
      //                                   "type": "INTEGER"
      //                                 }
      //                               ]
      //                             }
      //                           ],
      //                           "declaredOutput": [
      //                             {
      //                               "name": "temperature",
      //                               "type": "INTEGER"
      //                             },
      //                             {
      //                               "name": "humidity",
      //                               "type": "INTEGER"
      //                             }
      //                           ]
      //                         },
      //                         "description": "rule_1_desc"
      //                       },
      //                       {
      //                         "name": "rule_2",
      //                         "id": 2,
      //                         "ruleProcessorName": "rule_processsor_1",
      //                         "condition": {
      //                           "conditionElements": [
      //                             {
      //                               "firstOperand": {
      //                                 "name": "temperature",
      //                                 "type": "INTEGER"
      //                               },
      //                               "operation": "GREATER_THAN",
      //                               "secondOperand": "100",
      //                               "logicalOperator": "AND"
      //                             },
      //                             {
      //                               "firstOperand": {
      //                                 "name": "humidity",
      //                                 "type": "INTEGER"
      //                               },
      //                               "operation": "GREATER_THAN",
      //                               "secondOperand": "50",
      //                               "logicalOperator": null
      //                             }
      //                           ]
      //                         },
      //                         "action": {
      //                           "components": [
      //                             {
      //                               "name": "sink_1",
      //                               "id": 1,
      //                               "description": "sink_1_desc",
      //                               "declaredInput": [
      //                                 {
      //                                   "name": "temperature",
      //                                   "type": "INTEGER"
      //                                 },
      //                                 {
      //                                   "name": "humidity",
      //                                   "type": "INTEGER"
      //                                 }
      //                               ]
      //                             },
      //                             {
      //                               "name": "sink_2",
      //                               "id": 1,
      //                               "description": "sink_2_desc",
      //                               "declaredInput": [
      //                                 {
      //                                   "name": "temperature",
      //                                   "type": "INTEGER"
      //                                 },
      //                                 {
      //                                   "name": "humidity",
      //                                   "type": "INTEGER"
      //                                 }
      //                               ]
      //                             }
      //                           ],
      //                           "declaredOutput": [
      //                             {
      //                               "name": "temperature",
      //                               "type": "INTEGER"
      //                             },
      //                             {
      //                               "name": "humidity",
      //                               "type": "INTEGER"
      //                             }
      //                           ]
      //                         },
      //                         "description": "rule_2_desc"
      //                       }
      //                     ],
      //                     "name": "rule_processsor_1",
      //                     "id": 1,
      //                     "description": "rule_processsor_1_desc",
      //                     "declaredInput": [
      //                       {
      //                         "name": "temperature",
      //                         "type": "INTEGER"
      //                       },
      //                       {
      //                         "name": "humidity",
      //                         "type": "INTEGER"
      //                       }
      //                     ]
      //                   }
      //                 }
      //               }
      //             ],
      //             "dataSinks": [
      //               {
      //                 "uiname": "hbasesink",
      //                 "type": "HBASE",
      //                 "transformationClass": "com.hortonworks.iotas.topology.HbaseBoltFluxComponentYaml",
      //                 "config": {
      //                   "configKey": "hbaseConf",
      //                   "table": "nest",
      //                   "columnFamily": "cf",
      //                   "parallelism": 1
      //                 }
      //               },
      //               {
      //                 "uiname": "hdfssink",
      //                 "type": "HDFS",
      //                 "transformationClass": "com.hortonworks.iotas.topology.HdfsBoltFluxComponentYaml",
      //                 "config": {
      //                   "fsUrl": "hdfs://localhost:9000",
      //                   "configKey": "hdfsConf",
      //                   "path": "/tmp/failed-tuples",
      //                   "prefix": "data",
      //                   "extension": "dat",
      //                   "countPolicyValue": 1,
      //                   "rotationInterval": 10,
      //                   "rotationIntervalUnit": "SECONDS",
      //                   "parallelism": 1
      //                 }
      //               }
      //             ],
      //             "links": [
      //               {
      //                 "uiname": "kafkaDataSource->parserProcessor",
      //                 "type": "SHUFFLE",
      //                 "transformationClass": "com.hortonworks.iotas.topology.ShuffleGroupingLinkFluxComponentYaml",
      //                 "config": {
      //                   "from": "kafkaDataSource",
      //                   "to": "parserProcessor"
      //                 }
      //               },
      //               {
      //                 "uiname": "parserProcessor-parsedTuples->ruleProcessor",
      //                 "type": "SHUFFLE",
      //                 "transformationClass": "com.hortonworks.iotas.topology.ShuffleGroupingLinkFluxComponentYaml",
      //                 "config": {
      //                   "from": "parserProcessor",
      //                   "to": "ruleProcessor",
      //                   "streamId": "parsedTuplesStream"
      //                 }
      //               },
      //               {
      //                 "uiname": "ruleProcessor-rule1->hbasesink",
      //                 "type": "SHUFFLE",
      //                 "transformationClass": "com.hortonworks.iotas.topology.ShuffleGroupingLinkFluxComponentYaml",
      //                 "config": {
      //                   "from": "ruleProcessor",
      //                   "to": "hbasesink",
      //                   "streamId": "rule_processsor_1.rule_1.1"
      //                 }
      //               },
      //               {
      //                 "uiname": "ruleProcessor-rule2->hbasesink",
      //                 "type": "SHUFFLE",
      //                 "transformationClass": "com.hortonworks.iotas.topology.ShuffleGroupingLinkFluxComponentYaml",
      //                 "config": {
      //                   "from": "ruleProcessor",
      //                   "to": "hbasesink",
      //                   "streamId": "rule_processsor_1.rule_2.2"
      //                 }
      //               },
      //               {
      //                 "uiname": "parserProcessor-failedTuples->hdfssink",
      //                 "type": "SHUFFLE",
      //                 "transformationClass": "com.hortonworks.iotas.topology.ShuffleGroupingLinkFluxComponentYaml",
      //                 "config": {
      //                   "from": "parserProcessor",
      //                   "to": "hdfssink",
      //                   "streamId": "failedTuplesStream"
      //                 }
      //               }
      //             ]
      //           };
      if(flag){
        var tData = JSON.stringify(tempData);
        this.model.set({
          name: (this.model.has('name')) ? this.model.get('name') : 'topology-'+new Date().getTime(),
          config: tData
        });
        this.model.save({},{
          success: function(model, response, options){
            self.topologyId = response.entity.id;
            self.model = new VTopology();
            self.model.id = self.topologyId;
            self.model.set('id', self.topologyId);
            self.model.set('name', response.entity.name);
            self.$('#deployDatastream').removeAttr('disabled');
            Utils.notifySuccess('Topology submitted successfully.');
          },
          error: function(model, response, options){
            Utils.showError(model, response);
          }
        });
      }
    },
    evDeployAction: function(e){
      if(this.topologyId){
        this.model.deployTopology({
          id: this.topologyId,
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
      if(this.topologyId){
        this.model.killTopology({
          id: this.topologyId,
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
