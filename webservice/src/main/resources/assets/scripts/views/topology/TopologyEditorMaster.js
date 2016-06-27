define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'utils/Utils',
  'utils/TopologyUtils',
  'utils/Globals',
  'modules/Modal',
  'hbs!tmpl/topology/topologyEditorMaster',
  'models/VTopology',
  'x-editable',
], function(require, Vent, localization, Utils, TopologyUtils, Globals, Modal, tmpl, VTopology, xEditable) {
  'use strict';

  var TopologyEditorLayout = Marionette.LayoutView.extend({

    template: tmpl,
    templateHelpers: function(){
      return {
        topologyName: this.topologyName
      };
    },

    events: {
      'click #submitTopology'   : 'evSubmitAction',
      'click #deployTopology'   : 'evDeployAction',
      'click #killTopology'     : 'evKillAction',
      'click #configTopology'   : 'evConfigAction',
      'click #zoomOut-topo-graph' : 'evZoomOut',
      'click #zoomIn-topo-graph' : 'evZoomIn',
      'click #editor-node-options, #closeList': 'evToggleNodeOptions',
      'click #toggle-editor-mode': 'evToggleEditorMode'
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
      this.initializeVariables();
      if(!this.model){
        this.model = new VTopology();
        this.topologyName = 'Topology-Name';
        this.editMode = true;
      } else {
        this.topologyId = this.model.get('id');
        this.topologyName = this.model.get('name');
        this.model.set('_editState', true);
        this.model.set('config', JSON.parse(this.model.get('config')));
        this.setTopologyConfigModel();
        this.editMode = false;
      }
      this.getAllConfigurations();
      this.bindEvents();
    },

    initializeVariables: function(){
      this.renderFlag = false;
      this.nodeNames = [];
      this.dsArr = [];
      this.processorArr = [];
      this.sinkArr = [];
      this.linkArr = [];
      this.graphNodesData = { source : [], processor: [], sink: [] };
      this.vent = Vent;
      this.topologyConfigModel = new Backbone.Model({
        rootdir: 'hdfs://localhost:9000/tmp/hbase', parserPath: '/tmp', notifierPath: '/tmp'
      });
      this.graphTransforms = {
        dragCoords: [0,0],
        zoomScale: 1
      };
    },

    setTopologyConfigModel: function(){
      var config = this.model.get('config').config;
      var rootKey = 'hbaseConf';
      var obj = {
        rootdir: config[rootKey]['hbase.rootdir'],
        parserPath: config['local.parser.jar.path'],
        notifierPath: config['local.notifier.jar.path']
      };
      this.topologyConfigModel.set(obj);
    },

    getAllConfigurations: function(){
      var self = this;
      this.sourceConfigArr = [];
      this.processorConfigArr = [];
      this.sinkConfigArr = [];
      this.linkConfigArr = [];

      var promiseArr = [];
      promiseArr.push(this.model.getSourceComponent());
      promiseArr.push(this.model.getProcessorComponent());
      promiseArr.push(this.model.getSinkComponent());
      promiseArr.push(this.model.getLinkComponent());
      if(this.model.get('_editState')){
        promiseArr.push(this.model.getMetaInfo({id: this.model.get('id')}));
        //promiseArr.push(this.model.getTopologyStatus({id: this.model.get('id')}));
      }

      Promise.all(promiseArr).then(function(resultsArr){
        self.sourceConfigArr = resultsArr[0].entities;
        self.processorConfigArr = resultsArr[1].entities;
        self.sinkConfigArr = resultsArr[2].entities;
        self.linkConfigArr = resultsArr[3].entities;
        if(self.model.get('_editState')){
          var obj = JSON.parse(resultsArr[4].entity.data);
          self.linkArr = obj.linkArr;
          self.graphTransforms = obj.graphTransforms ? obj.graphTransforms : self.graphTransforms;
          self.updateVariables();
        }
        self.promiseResolvedFlag = true;
        if(self.renderFlag){
          self.showCustomProcessors();
          self.topologyGraph = TopologyUtils.syncGraph(self.model.get('_editState'), self.graphNodesData, self.linkArr, self.ui.graphEditor, self.vent, self.graphTransforms, self.linkConfigArr, self.editMode, self);
          TopologyUtils.bindDrop(self.$('#graphEditor'), self.dsArr, self.processorArr, self.sinkArr, self.vent, self.nodeNames);
        }
      });
    },

    updateVariables: function(){
      var config = this.model.get('config');
      TopologyUtils.updateVariables(config.dataSources, this.sourceConfigArr, config.links, this.nodeNames, this.dsArr, this.graphNodesData.source, Globals.Topology.Editor.Steps.Datasource.Substeps, 'DEVICE', 'KAFKA');
      TopologyUtils.updateVariables(config.processors, this.processorConfigArr, config.links, this.nodeNames, this.processorArr, this.graphNodesData.processor, Globals.Topology.Editor.Steps.Processor.Substeps);
      TopologyUtils.updateVariables(config.dataSinks, this.sinkConfigArr, config.links, this.nodeNames, this.sinkArr, this.graphNodesData.sink, Globals.Topology.Editor.Steps.DataSink.Substeps);
    },

    bindEvents: function(){
      var self = this;

      this.listenTo(this.vent, 'topologyEditor:SaveDeviceSource', function(data){
        TopologyUtils.saveNode(self.dsArr, data, self);
      });

      this.listenTo(this.vent, 'topologyEditor:SaveProcessor', function(data){
        TopologyUtils.saveNode(self.processorArr, data, self);
      });

      this.listenTo(this.vent, 'topologyEditor:SaveSink', function(data){
        TopologyUtils.saveNode(self.sinkArr, data, self);
      });

      this.listenTo(this.vent, 'click:topologyNode', function(data){
        var uiname = data.uiname;
        var customName = data.customName;
        switch(data.parentType){
          //Source
          case Globals.Topology.Editor.Steps.Datasource.valStr:
            TopologyUtils.getNode(this, this.dsArr, uiname, self.evDSAction, this.sourceConfigArr, false, 'KAFKA');
          break;
          //Processor
          case Globals.Topology.Editor.Steps.Processor.valStr:
            TopologyUtils.getNode(this, this.processorArr, uiname, self.evProcessorAction, this.processorConfigArr, true, data.currentType, this.linkArr, customName);
          break;
          //Sink
          case Globals.Topology.Editor.Steps.DataSink.valStr:
            TopologyUtils.getNode(this, this.sinkArr, uiname, self.evDataSinkAction, this.sinkConfigArr, true, data.currentType, this.linkArr);
          break;
        }
      });

      this.listenTo(this.vent, 'topologyEditor:SaveConfig', function(configData){
        self.topologyConfigModel = configData;
      });

      this.listenTo(this.vent, 'topologyLink', function(data){
        self.linkArr = data.edges;
      });

      this.listenTo(this.vent, 'topologyTransforms', function(graphData){
        self.graphTransforms = graphData;
      });

      this.listenTo(this.vent, 'delete:topologyNode', function(options){
        var object, index, ruleIndex;
        _.each(options.data, function(obj){
          if (obj.parentType === 'Datasource') {
            index = _.findIndex(self.dsArr, {uiname: obj.uiname});
            if(index !== -1){

              self.nodeNames.splice(self.nodeNames.indexOf(self.dsArr[index].uiname),1);
              self.dsArr.splice(index, 1);
            }
          } else if (obj.parentType === 'Processor') {
            index = _.findIndex(self.processorArr, {uiname: obj.uiname});
            if(index !== -1){
              self.nodeNames.splice(self.nodeNames.indexOf(self.processorArr[index].uiname),1);
              self.processorArr.splice(index, 1);
            }
          } else if (obj.parentType === 'DataSink') {
            index = _.findIndex(self.sinkArr, {uiname: obj.uiname});
            if(index !== -1){
              self.nodeNames.splice(self.nodeNames.indexOf(self.sinkArr[index].uiname),1);
              self.sinkArr.splice(index, 1);
            }
          }
        });

        self.vent.trigger('delete:topologyEdge', options);
      });

      this.listenTo(this.vent, 'delete:topologyEdge', function(options){
        if (!_.isUndefined(options.resetRule)) {
          TopologyUtils.resetRule(self.processorArr, options);
        }
        if(!_.isUndefined(options.resetRuleAction)){
          TopologyUtils.resetRuleAction(self.processorArr, options);
        }
        if(!_.isUndefined(options.resetCustomAction)){
          TopologyUtils.resetCustomAction(self.processorArr, options);
        }
        if(!_.isUndefined(options.resetNormalization)){
          TopologyUtils.resetNormalizationAction(self.processorArr, options);
        }
        if(!_.isUndefined(options.resetSplit)){
          TopologyUtils.resetSplit(self.processorArr, self.linkArr , options);
        }
        options.callback();
      });

      this.listenTo(this.vent, 'topologyGraph:RuleToOtherNode', function(data){
        require(['views/topology/RuleToOtherNodeView'], function(RuleToOtherNodeView){
          var ruleProcessorObj = self.processorArr.filter(function(o){if(o) return o.uiname === data.source.uiname;});
          if(ruleProcessorObj.length && !ruleProcessorObj[0].firstTime){
            self.titleName = ruleProcessorObj[0].uiname;
            var view = new RuleToOtherNodeView({
              processorObj: ruleProcessorObj,
              sinkName: data.target.uiname,
              currentType: data.source.currentType,
              vent: self.vent
            });
            var modal = new Modal({
              title: (data.source.currentType === 'RULE') ? 'Select rules for '+data.target.uiname : (data.source.currentType === 'SPLIT' ? 'Select a stream for '+ data.target.uiname : 'Select streams for '+data.target.uiname),
              contentWithFooter: true,
              content: view,
              showFooter: false,
              mainClass: 'modal-lg'
            }).open();

            view.on('closeModal', function(){
              view = null;
              modal.trigger('cancel');
            });
          }
        });
      });
    },

    onRender:function(){
      $('#loading').show();
      var self = this;
      var actualHeight = $(window).innerHeight() - 185;
      this.$('#graphEditor').css("height", actualHeight+"px");
      
      TopologyUtils.setTopologyName(this.$('#topologyName'), function(e, params){ 
        self.topologyName = params.newValue; 
      });
      TopologyUtils.bindDrag(this.$('.panel-body img'));
      this.$(".nodes-list-container").draggable({
        containment: '#graphEditor'
      }).css("position", "absolute");

      if(!this.editMode) {
        this.$("#graphEditor").removeClass("graph-bg");
        this.$(".nodes-list-container, .topology-control-btn, .topology-mode-control").toggleClass("displayNone");
        this.$("#topologyName").html(this.topologyName);        
        this.$('#topologyName').editable('toggleDisabled');
        window._preventNavigation = false;
      } else {
        window._preventNavigation = true;
        this.$(".topology-capsule").hide();
      }   
      
      setTimeout(function(){
        self.renderFlag = true;
        if(self.promiseResolvedFlag){
          self.showCustomProcessors();
          self.topologyGraph = TopologyUtils.syncGraph(self.model.get('_editState'), self.graphNodesData, self.linkArr, self.ui.graphEditor, self.vent, self.graphTransforms, self.linkConfigArr, self.editMode, self);
          TopologyUtils.bindDrop(self.$('#graphEditor'), self.dsArr, self.processorArr, self.sinkArr, self.vent, self.nodeNames);
        }
      }, 0);

      $("body").on("click", ".nav-link", function(e) {
        var target = this,
            message = 'Are you sure you want to leave this page ?',
            title = 'Confirm',
            successCallback = function(){
              window._preventNavigation = false;
              $("body").off('click', '.nav-link');
              target.click();
            };
        if(self.editMode) {
          e.preventDefault();
          e.stopImmediatePropagation();
          Utils.ConfirmDialog(message, title, successCallback);
          return false;
        }
      });
      
      var accordion = this.$('[data-toggle="collapse"]');
      if(accordion.length){
        accordion.on('click', function(e){
          if($(e.currentTarget).hasClass("collapseNodeList")) {
            $(e.currentTarget).children('i').toggleClass('fa-expand fa-compress');
          } else $(e.currentTarget).children('i').toggleClass('fa-caret-right fa-caret-down');
        });
      }

      self.$('[data-rel="tooltip"]').tooltip({placement: 'bottom', trigger: 'hover'});
      $('#loading').hide();
    },

    showCustomProcessors: function(){
      var self = this;
      var customProcessorArr = _.where(this.processorConfigArr, {subType: 'CUSTOM'});
      _.each(customProcessorArr, function(obj){
        var jsonConfig = JSON.parse(obj.config);
        var nameObj = _.findWhere(jsonConfig, {name: "name"});
        if(nameObj){
          self.$('#collapseProcessor .panel-body').append('<img src="images/icon-custom.png" class="topology-icon-inverse processor" data-rel="tooltip" title="'+nameObj.defaultValue+'" data-name="'+nameObj.defaultValue+'" data-subType="CUSTOM" data-parentType="Processor">');
        }
      });
      TopologyUtils.bindDrag(self.$('.panel-body img'));
      self.$('[data-rel="tooltip"]').tooltip({placement: 'bottom'});
    },

    evDSAction: function(model){
      var self = this;
      if(model.has('config')){
        model.get('config').sort(TopologyUtils.configSortComparator);
      }
      var obj = {
        titleHtmlFlag: true,
        titleName: model.get('uiname')
      };
      require(['views/topology/DataFeedView'], function(DataFeedView){
        self.showModal(new DataFeedView({
          model: model,
          vent: self.vent,
          editMode: self.editMode
        }), obj);
      });
    },

    evProcessorAction: function(model){
      var self = this;
      var obj = {
        titleHtmlFlag: true,
        titleName: model.get('uiname')
      };
      switch(model.get('currentType')){
        case 'PARSER':
          if(TopologyUtils.syncParserData(model, this.linkArr, this.dsArr)){
            require(['views/topology/ParserProcessorView'], function(ParserProcessorView){
              self.showModal(new ParserProcessorView({
                model: model,
                vent: self.vent,
                editMode: self.editMode
              }), obj);
            });
          }
        break;

        case 'RULE':
          if(TopologyUtils.syncRuleData(model, this.linkArr, this.processorArr, this.dsArr)){
            var linkObj = self.linkArr.filter(function(o){
              if(o.source.uiname === model.get('uiname'))
                return o;
            });
            var arr = [];
            _.each(linkObj, function(o){
              arr.push(o.target.uiname);
            });
            var linkArr = self.linkArr.filter(function(o){
                if(o.target.uiname === model.get('uiname'))
                  return o.source.uiname;
              }),
              sourceNode = _.findWhere(self.processorArr, {uiname: linkArr[0].source.uiname});
            require(['views/topology/RuleProcessorView'], function(RuleProcessorView){
              self.showModal(new RuleProcessorView({
                model: model,
                vent: self.vent,
                linkedToRule: linkObj,
                connectedSink: arr,
                editMode: self.editMode,
                sourceConfig: sourceNode
              }), obj);
            });
          }
        break;

        case 'CUSTOM':
          require(['views/topology/CustomProcessorView'], function(CustomProcessorView){
            var linkObj = self.linkArr.filter(function(o){
              if(o.source.uiname === model.get('uiname'))
                return o;
            });
            var arr = [];
            _.each(linkObj, function(o){
              arr.push(o.target.uiname);
            });
            self.showModal(new CustomProcessorView({
              model: model,
              vent: self.vent,
              showOutputFields: linkObj.length ? true : false,
              connectedNodes: arr,
              editMode: self.editMode
            }), obj);
          });
        break;
        case 'NORMALIZATION':
        if(TopologyUtils.syncNormalizationData(model, this.linkArr, this.processorArr) == false) {
          Utils.notifyError("Configure the connected node first.");
          break;
        }
          require(['views/topology/NormalizationProcessorView'], function(NormalizationProcessorView){
            var dsId;
            var linkObj = self.linkArr.filter(function(o){
                if(o.target.uiname === model.get('uiname'))
                  return o.source.uiname;
              });
            var sourceNode = _.findWhere(self.processorArr, {uiname: linkObj[0].source.uiname}),
              streamId = null;
            if(linkObj[0].source.currentType == 'PARSER'){
              var dsToParserObj = self.linkArr.filter(function(o){
                return (o.target.currentType === 'PARSER' && o.target.uiname === linkObj[0].source.uiname);
              });
              if(dsToParserObj.length){
                var deviceObj = self.dsArr.filter(function(o){ return o.uiname === dsToParserObj[0].source.uiname;});
                if(deviceObj.length){
                    dsId = deviceObj[0].dataSourceId ? deviceObj[0].dataSourceId : deviceObj[0]._selectedTable[0].datasourceId;
                }
              }
              streamId = linkObj[0].target.streamId;
            } else if(linkObj[0].source.currentType == 'RULE'){
              var rules = sourceNode.newConfig ? sourceNode.newConfig.rulesProcessorConfig.rules : sourceNode.rulesProcessorConfig.rules;
              var flag = false;
              _.each(rules, function(rule){
                if(!flag){
                  _.each(rule.actions, function(action){
                    if(action.name === linkObj[0].target.uiname){
                      flag = true;
                    }
                  })
                }
              });
              if(!flag){
                Utils.notifyError("Please select a rule from "+linkObj[0].source.uiname+" processor.");
                return;
              }
              streamId = "tempStream";
            } else if (linkObj[0].source.currentType == 'JOIN') {
              streamId = "tempStream";
            }
            if(sourceNode.selectedStreams && sourceNode.selectedStreams.length === 0){
              Utils.notifyError("Please select an output stream from "+linkObj[0].source.uiname+".");
              return;
            }
            self.showModal(new NormalizationProcessorView({
              model: model,
              vent: self.vent,
              sourceConfig: sourceNode,
              streamId: streamId ? streamId : null,
              editMode: self.editMode,
              dsId: dsId
            }), obj);
          });
        break;
        case 'SPLIT':
        if(TopologyUtils.syncSplitData(model, this.linkArr, this.processorArr, this.dsArr)) {
        require(['views/topology/SplitProcessorView'], function(SplitProcessorView) {
          var linkObj = self.linkArr.filter(function(o){
                if(o.target.uiname === model.get('uiname'))
                  return o.source.uiname;
              }),
              sourceNode = _.findWhere(self.processorArr, {uiname: linkObj[0].source.uiname}),
              stageObj = self.linkArr.filter(function(o){
                if(o.source.uiname === model.get('uiname'))
                  return o.source.uiname;
              });
          var arr = [];
          _.each(stageObj, function(o){
            arr.push(o.target.uiname);
          });
          self.showModal(new SplitProcessorView({
              model: model,
              vent: self.vent,
              sourceConfig: sourceNode,
              editMode: self.editMode,
              connectedNodes: arr,
              showOutputLinks: arr.length ? true : false,
            }), obj);
        });
        } else {
          Utils.notifyError("Configure the connected node first.");
        }
        break;
        case 'STAGE':
        if(TopologyUtils.syncStageData(model, this.linkArr, this.processorArr)) {
        require(['views/topology/StageProcessorView'], function(StageProcessorView) {
          var linkObj = self.linkArr.filter(function(o){
                if(o.target.uiname === model.get('uiname'))
                  return o.source.uiname;
              }),
              sourceNode = _.findWhere(self.processorArr, {uiname: linkObj[0].source.uiname});
          self.showModal(new StageProcessorView({
              model: model,
              vent: self.vent,
              sourceConfig: sourceNode,
              editMode: self.editMode
            }), obj);
        });
        } else {
          Utils.notifyError("Configure the connected node first.");
        }
        break;
        case 'JOIN':
        if(TopologyUtils.syncJoinData(model, this.linkArr, this.processorArr)) {
        require(['views/topology/JoinProcessorView'], function(JoinProcessorView) {
          var linkObj = self.linkArr.filter(function(o){
                if(o.target.uiname === model.get('uiname'))
                  return o.source.uiname;
              }),
              sourceNodes = [];
          for(var i = 0;i < linkObj.length;i++)
            sourceNodes.push(_.findWhere(self.processorArr, {uiname: linkObj[i].source.uiname}));
          self.showModal(new JoinProcessorView({
              model: model,
              vent: self.vent,
              sourceConfig: sourceNodes,
              editMode: self.editMode
            }), obj);
        });
        } else {
          Utils.notifyError("Configure the connected node first.");
        }
        break;
      }
    },

    evDataSinkAction: function(model, type){
      var self = this;
      if(model.has('config')){
        model.get('config').sort(TopologyUtils.configSortComparator);
      }
      var obj = {
        titleHtmlFlag: true,
        titleName: model.get('uiname')
      };
      if(TopologyUtils.syncSinkData(model, this.linkArr, this.processorArr)){
        require(['views/topology/DataSinkView'], function(DataSinkView){
          self.showModal(new DataSinkView({
            model: model,
            vent: self.vent,
            type: type,
            editMode: self.editMode
          }), obj);
        });
      } else {
        Utils.notifyError("Configure the connected node first.");
      }
    },

    showModal: function(view, object){
      var self = this,
          titleHtml;
      if(this.view){
        return;
      }
      if(object.titleHtmlFlag){
        titleHtml = self.editMode ? '<a href="javascript:void(0)" id="editableTitle" data-type="text"> '+object.titleName+'<i class="fa fa-pencil"></i></a>' : '<a href="javascript:void(0)" id="editableTitle" data-type="text"> '+object.titleName+'</a>';
        this.titleName = object.titleName;
      }
      this.view = view;
      var modal = new Modal({
        title: titleHtml,
        titleHtml: object.titleHtmlFlag,
        contentWithFooter: true,
        content: self.view,
        showFooter: false,
        mainClass: 'modal-lg'
      }).open();

      if(self.editMode) {
        modal.$('#editableTitle').editable({
          mode:'inline',
          validate: function(value) {
            if(_.isEqual($.trim(value), '')) return 'Name is required';
            if(self.nodeNames.indexOf(value) !== -1) return 'Node name should be unique throughout topology';
          }
        });

        modal.$('.editable').on('save', function(e, params) {
          modal.options.content.model.set('uiname', params.newValue);
        });

        modal.$('.editable').on('rendered', function(e){
          $(e.currentTarget).append("<i class='fa fa-pencil'></i>");
        });
      }

      this.view.on('closeModal', function(){
        self.view = null;
        modal.trigger('cancel');
      });
    },

    evSubmitAction: function(e){
      $('#loading').show();
      var self = this,
          rootdirKeyName = 'hbaseConf';
      var topologyConfig = {
        "local.parser.jar.path": this.topologyConfigModel.get('parserPath'),
        "local.notifier.jar.path": this.topologyConfigModel.get('notifierPath')
      };
      topologyConfig[rootdirKeyName] = {
        "hbase.rootdir": this.topologyConfigModel.get('rootdir')
      };
      var tempData = {
        config: topologyConfig,
        dataSources: [],
        processors: [],
        dataSinks: [],
        links: []
      };
      //Get Datasource
      var dsObj = TopologyUtils.generateJSONForDataSource(this.dsArr);
      if(dsObj.flag){
        tempData.dataSources = dsObj.ds;
      } else {
        $('#loading').hide();
        Utils.notifyError(dsObj.msg);
        return false;
      }

      //Get Processor
      var processorObj = TopologyUtils.generateJSONForProcessor(this.processorArr, this.linkArr);
      if(processorObj.flag){
        tempData.processors = processorObj.processors;
      } else {
        $('#loading').hide();
        Utils.notifyError(processorObj.msg);
        return false;
      }

      //Get Sink
      var sinkObj = TopologyUtils.generateJSONForSink(this.sinkArr, this.linkArr);
      if(sinkObj.flag){
        tempData.dataSinks = sinkObj.sink;
      } else {
        $('#loading').hide();
        Utils.notifyError(sinkObj.msg);
        return false;
      }

      var flag = true;
      _.each(this.linkArr,function(obj){
        if(flag){
          var sourceObj, targetObj;
          switch(obj.source.parentType){
            case 'Datasource':
              sourceObj = _.findWhere(self.dsArr, {uiname: obj.source.uiname});
            break;
            case 'Processor':
              sourceObj = _.findWhere(self.processorArr, {uiname: obj.source.uiname});
            break;
            case 'DataSink':
              sourceObj = _.findWhere(self.sinkArr, {uiname: obj.source.uiname});
            break;
          }

          switch(obj.target.parentType){
            case 'Datasource':
              targetObj = _.findWhere(self.dsArr, {uiname: obj.target.uiname});
            break;
            case 'Processor':
              targetObj = _.findWhere(self.processorArr, {uiname: obj.target.uiname});
            break;
            case 'DataSink':
              targetObj = _.findWhere(self.sinkArr, {uiname: obj.target.uiname});
            break;
          }

          if(!sourceObj || !targetObj){
            flag = false;
            $('#loading').hide();
            Utils.notifyError("There are some unconfigured nodes present. Kindly configure to proceed.");
          } else {
            var tempObj = {
              uiname: sourceObj.uiname + '->' + targetObj.uiname,
              type: obj.linkType ? obj.linkType : 'SHUFFLE',
              transformationClass: "com.hortonworks.iotas.topology.storm.ShuffleGroupingLinkFluxComponent",
              config: {
                "from": sourceObj.uiname,
                "to": targetObj.uiname,
              }
            };
            if(obj.linkType === 'FIELDS'){
              tempObj.config.groupingFields = obj.fieldsArr ? obj.fieldsArr : [];
            }
            if(sourceObj.currentType === 'DEVICE'){
              tempData.links.push(tempObj);
            }
            if(sourceObj.currentType === 'PARSER'){
              tempObj.config.streamId = obj.target.streamId;
              tempData.links.push(tempObj);
            }
            if(sourceObj.currentType === 'RULE'){
              var ruleProcessor = sourceObj.newConfig ? sourceObj.newConfig.rulesProcessorConfig : sourceObj.rulesProcessorConfig;
              var loopFlag = false, ruleName;
              _.each(ruleProcessor.rules, function(obj, i){
                var actionObj = _.findWhere(obj.actions, {name: targetObj.uiname});
                if(actionObj){
                  loopFlag = true;
                  var o = JSON.parse(JSON.stringify(tempObj));
                  o.config.streamId = ruleProcessor.name+'.'+obj.name+'.'+obj.id+'.'+targetObj.uiname;
                  tempData.links.push(o);
                } else {
                  loopFlag = loopFlag || false;
                  if(obj.actions.length){
                    ruleName = 'No rules is asociated with '+targetObj.uiname+'. Please associate before saving the topology.';
                  } else {
                    ruleName = ruleProcessor.name+'.'+obj.name+' is not associated to any sink. Please associate before saving the topology.';
                  }
                }
              });
              if(!loopFlag){
                flag = false;
                $('#loading').hide();
                Utils.notifyError(ruleName);
              }
            }
            if(sourceObj.currentType === 'CUSTOM'){
              if(sourceObj.selectedStreams && sourceObj.selectedStreams.length){
                var streamObj = _.where(sourceObj.selectedStreams, {name: targetObj.uiname});
                if(streamObj.length){
                  _.each(streamObj, function(s){
                    var o = JSON.parse(JSON.stringify(tempObj));
                    o.config.streamId = s.streamName;
                    tempData.links.push(o);
                  });
                } else {
                  flag = false;
                  $('#loading').hide();
                  Utils.notifyError("No output streams are selected for "+targetObj.uiname);
                }
              } else {
                flag = false;
                $('#loading').hide();
                Utils.notifyError("No output streams are selected from "+sourceObj.uiname);
              }
            }
            if(sourceObj.currentType === 'NORMALIZATION'){
              tempObj.config.streamId = 'normalized-output';
              tempData.links.push(tempObj);
            }
            if(sourceObj.currentType === 'SPLIT') {
              if(sourceObj.selectedStreams && sourceObj.selectedStreams.length){
                var streamObj = _.where(sourceObj.selectedStreams, {name: targetObj.uiname});
                if(streamObj.length){
                  _.each(streamObj, function(s){
                    var o = JSON.parse(JSON.stringify(tempObj));
                    o.config.streamId = s.streamName;
                    tempData.links.push(o);
                  });
                } else {
                  flag = false;
                  $('#loading').hide();
                  Utils.notifyError("No output streams are selected for "+targetObj.uiname);
                }
              } else {
                flag = false;
                $('#loading').hide();
                Utils.notifyError("No output streams are selected from "+sourceObj.uiname);
              }
            }
            if(sourceObj.currentType === 'STAGE') {
              tempData.links.push(tempObj);
            }
            if(sourceObj.currentType === 'JOIN') {
              tempData.links.push(tempObj);
            }
          }
        }
      });
      // console.log(tempData);
      if(flag){
        var tData = JSON.stringify(tempData);
        this.model.set({
          name: this.topologyName,
          config: tData
        });
        delete this.model.attributes._editState;
        delete this.model.attributes.timestamp;
        this.model.save({},{
          success: function(model, response, options){
            self.topologyId = response.entity.id;
            self.saveTopologyMetaData(self.topologyId);
            self.model = new VTopology();
            self.model.id = self.topologyId;
            self.model.set('id', self.topologyId);
            self.model.set('name', response.entity.name);
            self.model.set('_editState', true);
            self.$('#deployTopology').removeAttr('disabled');
            $('#loading').hide();
            Utils.notifySuccess('Topology saved successfully.');
            self.evToggleEditorMode();
          },
          error: function(model, response, options){
            $('#loading').hide();
            Utils.showError(model, response);
          }
        });
      }
    },
    saveTopologyMetaData: function(topologyId){
      var self = this;
      var data = {
        linkArr: this.linkArr,
        graphTransforms: this.graphTransforms
      };
      var obj = {topologyId: topologyId, data: JSON.stringify(data)};
      this.model.saveMetaInfo({
        id: self.model.id,
        data: JSON.stringify(obj),
        dataType:'json',
        contentType: 'application/json',
        success: function(model, response, options){
          if(Backbone.history.fragment !== '!/topology-editor/'+self.topologyId){
            Backbone.history.navigate('!/topology-editor/'+self.topologyId, {trigger : true});
          }
        },
        error: function(model, response, options){
          Utils.showError(model, response);
        }
      });
    },
    evDeployAction: function(e){
      $('#loading').show();
      var self = this;
      if(this.topologyId){
        this.model.validateTopology({
          id: this.topologyId,
          success: function(model, response, options){
            self.model.deployTopology({
              id: self.topologyId,
              success: function(model, response, options){
                self.$('#deployTopology').attr('disabled',true);
                self.$('#killTopology').removeAttr('disabled');
                $('#loading').hide();
                Utils.notifySuccess('Topology deployed successfully.');
                self.evToggleEditorMode();
              },
              error: function(model, response, options){
                $('#loading').hide();
                Utils.showError(model, response);
              }
            });
          },
          error: function(model, response, options){
            $('#loading').hide();
            Utils.showError(model, response);
          }
        });
      } else {
        $('#loading').hide();
        Utils.notifyError('Need to save a topology before deploying it.');
      }
    },
    evKillAction: function(e){
      $('#loading').show();
      if(this.topologyId){
        this.model.killTopology({
          id: this.topologyId,
          success: function(model, response, options){
            self.$('#killTopology').attr('disabled', true);
            self.$('#submitTopology').removeAttr('disabled');
            self.$('#deployTopology').removeAttr('disabled');
            $('#loading').hide();
            Utils.notifySuccess('Topology killed successfully.');
          },
          error: function(model, response, options){
            $('#loading').hide();
            Utils.showError(model, response);
          }
        });
      } else {
        $('#loading').hide();
        Utils.notifyError('Need to save a topology before killing it.');
      }
    },
    evConfigAction: function(){
      var self = this;
      require(['views/topology/TopologyLevelConfig'], function(TopologyLevelConfig){
        var view = new TopologyLevelConfig({
          model: self.topologyConfigModel,
          vent: self.vent
        });
        var modal = new Modal({
          title: 'Topology Configuration',
          content: view,
          contentWithFooter: true
        }).open();
        view.on('closeModal', function(){
          view = null;
          modal.trigger('cancel');
        });
      });
    },
    evZoomIn: function(){
      this.vent.trigger('TopologyEditorMaster:Zoom', 'zoom_in');
    },
    evZoomOut: function(){
      this.vent.trigger('TopologyEditorMaster:Zoom', 'zoom_out');
    },
    evToggleNodeOptions: function() {
      this.$(".node-options-btn, .nodes-list-container").toggleClass("displayNone");
    },
    evToggleEditorMode: function(e) {
      this.editMode = this.editMode ? false : true;
      this.$(".topology-control-btn, .topology-mode-control").toggleClass("displayNone");
      if(this.editMode){
        window._preventNavigation = true;
        this.$("#graphEditor").addClass("graph-bg"); 
        this.$(".nodes-list-container").removeClass("displayNone");
        this.$(".node-options-btn").addClass("displayNone");
        this.$("#topologyName").html(this.topologyName+'<i class="fa fa-pencil"></i>');
        this.$(".topology-capsule").hide();
      } else {
        window._preventNavigation = false;
        this.$("#graphEditor").removeClass("graph-bg");
        this.$(".nodes-list-container").addClass("displayNone");
        this.$("#topologyName").html(this.topologyName);
        this.$(".topology-capsule").show();
      }
      this.$('#topologyName').editable('toggleDisabled');
      this.topologyGraph.editMode = this.editMode;
    },
    destroy: function(){
      this.stopListening(this.vent, 'topologyEditor:SaveDeviceSource');
      this.stopListening(this.vent, 'topologyEditor:SaveProcessor');
      this.stopListening(this.vent, 'topologyEditor:SaveSink');
      this.stopListening(this.vent, 'click:topologyNode');
      this.stopListening(this.vent, 'topologyEditor:SaveConfig');
      this.stopListening(this.vent, 'topologyLink');
      this.stopListening(this.vent, 'topologyGraph:RuleToOtherNode');
      this.stopListening(this.vent, 'topologyTransforms');
      this.stopListening(this.vent, 'delete:topologyNode');
      this.stopListening(this.vent, 'delete:topologyEdge');
      if(this.topologyGraph){
        this.topologyGraph.vent.stopListening(this.vent, 'topologyEditor:DropAction');
        this.topologyGraph.vent.stopListening(this.vent, 'TopologyEditorMaster:Zoom');
        this.topologyGraph.vent.stopListening(this.vent, 'saveNodeConfig');
      }
    }
  });
  return TopologyEditorLayout;
});