/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'bootbox',
  'utils/Utils',
  'utils/Globals',
  'collection/BaseCollection',
  'jquery',
  'modules/Modal',
  'hbs!tmpl/iotas-ui/iotas-ui',
  'views/iotas-ui/deviceModal',
  'views/iotas-ui/rulesetModal',
  'views/iotas-ui/databaseModal',
  'models/VDataStream',
  'jsPlumb',
  'select2'
], function(require, vent, localization, bootbox, Utils, Globals, BaseCollection, jquery, Modal, tmpl, deviceModal, rulesetModal, databaseModal, VDataStream, jsPlumb, select2) {
  'use strict';

  var IotasEditorLayout = Marionette.LayoutView.extend({

    template: tmpl,

    events: {
      'click #step1': 'eStep1',
      'click #step2': 'eStep2',
      'click #step3': 'eStep3',
      'click #submitWorkFlow' : 'eSubmit',
      'click #deployWorkFlow' : 'eDeploy',
    },

    ui: {
      'boxBody': '.box-body',
      'step1': '#step1',
      'step2': '#step2',
      'step3': '#step3',
    },

    regions: {
    },

    initialize: function() {
      this.datastreamcount = 1;
      this.tempJSON = {
                    "dataSources": [
                    ],
                    "processors": [
                    ],
                    "dataSinks": [
                    ],
                    "links": [
                    ]
                  };

      this.datastream = new VDataStream({
                                          "dataStreamName": "datastream",
                                          "json": ""
                                       });


      this.modal = null;
        if(!instance){
              instance = jsPlumb.getInstance({
            DragOptions: { cursor: 'pointer', zIndex: 2000 },
        });
      }

      this.connectorPaintStyle = {
          lineWidth: 4,
          strokeStyle: "#5E6061",
          joinstyle: "round",
          outlineColor: "white",
          outlineWidth: 2
      },

      this.connectorHoverStyle = {
          lineWidth: 4,
          strokeStyle: "#DFE4B2",
          outlineWidth: 2,
          outlineColor: "white"
      },

      this.endpointHoverStyle = {
          fillStyle: "#216477",
          strokeStyle: "#216477"
      },

      this.anchorCssClass = "animated bounceIn",

      this.sourceEndpoint = {
          endpoint: "Dot",
          paintStyle: {
              strokeStyle: "#343D3A",
              fillStyle: "transparent",
              radius: 5,
              lineWidth: 2
          },
          isSource: true,
          cssClass:this.anchorCssClass,
          maxConnections: -1,
          connector: [ "Flowchart", { stub: [40, 60], gap: 8, cornerRadius: 5, alwaysRespectStubs: true, cssClass:"animated fadeIn" } ],
          connectorStyle: this.connectorPaintStyle,
          hoverPaintStyle: this.endpointHoverStyle,
          connectorHoverStyle: this.connectorHoverStyle,
          anchor: "Right"
      },

      this.targetEndpoint = {
          endpoint: "Dot",
          paintStyle: { fillStyle: "#343D3A", radius: 5 },
          hoverPaintStyle: this.endpointHoverStyle,
          maxConnections: -1,
          cssClass:this.anchorCssClass,
          dropOptions: { hoverClass: "hover", activeClass: "active" },
          isTarget: true,
          connector: [ "Flowchart", { stub: [40, 60], gap: 1, cornerRadius: 5, alwaysRespectStubs: true, cssClass:"animated fadeIn"} ],
          connectorStyle: this.connectorPaintStyle,
          anchor: "Left"
      }

      this.devices = [];
      this.processors = [];
      this.databases = [];
      this.DataSinks = [];

      this.deviceCount = 0;
      this.ruleSetCount = 0;
      this.databaseCount = 0;

      this.deviceLeft = 100;
      this.deviceTop = 100;
      this.rulesetLeft = 700;
      this.rulesetTop = 100;
      this.databaseLeft = 1000;
      this.databaseTop = 100;
    },

    onRender:function(){
    },

    eStep1:function(){
      console.log('eStep1');
      this.loadModal('Datafeed');
    },

    eStep2:function(){
      console.log('eStep2');
      this.loadModal('Processor');
    },

    eStep3:function(){
      console.log('eStep3');
      if(typeof(deviceId) === 'string'){
        this.addDatabaseEl(deviceId);
      }else{
        this.loadModal('DataSink');
      }
    },

    addDataFeed:function(feedId){
        this.dataFeed = this.deviceModal.selectedDevices;
        this.selectedFields = [];
        this.deviceCount++;
        var dataFeedDiv = '<div class="animated bounceIn" id = "Datafeed'+this.deviceCount+'" style="left:'+this.deviceLeft+'px; top:'+this.deviceTop+'px; position:absolute;height: 80px;width: 80px;">'+
                        '<i class="fa fa-desktop" style="color: #d85120; position: relative; top: 19px; left: 11px; font-size: 50px;"></i>'+
                        '<label class="devLabel">Feed</label>'+
                  '</div>';
        $(this.ui.boxBody).append(dataFeedDiv);
       
        var dataFeedEP = instance.addEndpoint('Datafeed'+this.deviceCount, this.sourceEndpoint);
        var parserDiv = '<div class="animated bounceIn" id = "Parser'+this.deviceCount+'" style="left:'+(this.deviceLeft+300)+'px; top:'+this.deviceTop+'px; position:absolute;height: 80px;width: 80px;">'+
                        '<i class="fa fa-sliders" style="color: #d85120; position: relative; top: 13px; left: 18px; font-size: 50px;"></i>'+
                        '<label class="parserLabel">Parser</label>'+
                  '</div>';
        $(this.ui.boxBody).append(parserDiv);
        var parserSourceEP = instance.addEndpoint('Parser'+this.deviceCount, this.sourceEndpoint);
        var parserTargetEP = instance.addEndpoint('Parser'+this.deviceCount, this.targetEndpoint);
        
        var obj = {
                    'id':'Datafeed'+this.deviceCount,
                    'source' :parserSourceEP,
                  };
        this.devices.push(obj);
        var conn = instance.connect({
                              source:dataFeedEP, 
                              target:parserTargetEP
                            });
        for (var i = 0; i < this.dataFeed.length; i++) {
            this.tempJSON.dataSources.push({
                                              "uiname": this.dataFeed[i].dataSourceName,
                                              "id": this.dataFeed[i].datasourceId,
                                              "type": "KAFKA",
                                              "config": {
                                                "zkUrl": "localhost:2181",
                                                "topic": "nest-topic"
                                              }
                                            });
            for (var k = 0; k < 3; k++) {
              this.selectedFields.push(this.dataFeed[i].dataSourceName+'- field'+k);
            };
            
        };
        $(this.ui.step2).removeAttr('disabled');
        
    },

    addProcessorEl:function(processorId){
        this.ruleSetCount++;
        var rules = this.rulesetModal.processorSet;
        var div = '<div class="animated bounceIn" id = "Processor'+this.ruleSetCount+'" style="left:'+this.rulesetLeft+'px; top:'+this.rulesetTop+'px; position:absolute; height: 80px;width: 80px;">'+
                      '<i class="fa fa-cog ruleSetCSS"></i>'+
                      '<label class="procLabel">Processor</label>'+
                  '</div>';
        $(this.ui.boxBody).append(div);
        this.rulesetTop += 150;
        this.deviceTop += 150;
        var ep1 = instance.addEndpoint('Processor'+this.ruleSetCount, this.sourceEndpoint);
        var ep2 = instance.addEndpoint('Processor'+this.ruleSetCount, this.targetEndpoint);
        var obj = {
                    'id': 'Processor'+this.ruleSetCount,
                    'rules': rules,
                    'source':ep1,
                    'target':ep2,
                  };
        this.processors.push(obj);
        var source = null;
        var target = ep2;
        
        var devices = this.devices;
        source = devices[devices.length - 1].source;
        
        var conn = instance.connect({
                              source:source, 
                              target:target
                            });
        $(this.ui.step3).removeAttr('disabled');
        this.tempJSON.processors = [
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
                                   ];
    },

    addDatabaseEl:function(deviceId){
        var tempSink = this.DataSinks;
        for (var i = 0; i < tempSink.length; i++) {
            if (tempSink[i].created == false) {
                var rules = tempSink[i].rule;
                var source = null;
                var div = null;
                if (tempSink[i].action != 'Process') {
                    this.databaseCount++;
                    if(tempSink[i].action == 'Store'){
                      if (tempSink[i].endPoint.type == 'HDFS') {
                                div = '<div class="animated bounceIn" id = "DataSink'+this.databaseCount+'" style="left:'+this.databaseLeft+'px; top:'+this.databaseTop+'px; position:absolute;height: 80px;width: 80px;">'+
                                    '<i class="fa fa-database dataBaseCSS"></i>'+
                                    '<label class="dbLabel">Hdfs</label>'
                              '</div>';
                      }else if (tempSink[i].endPoint.type == 'Hive') {
                                div = '<div class="animated bounceIn" id = "DataSink'+this.databaseCount+'" style="left:'+this.databaseLeft+'px; top:'+this.databaseTop+'px; position:absolute;height: 80px;width: 80px;">'+
                                    '<i class="fa fa-database dataBaseCSS"></i>'+
                                    '<label class="dbLabel">Hive</label>'
                              '</div>';
                      }else if (tempSink[i].endPoint.type == 'HBase') {
                                div = '<div class="animated bounceIn" id = "DataSink'+this.databaseCount+'" style="left:'+this.databaseLeft+'px; top:'+this.databaseTop+'px; position:absolute;height: 80px;width: 80px;">'+
                                    '<i class="fa fa-h-square dataBaseCSS"></i>'+
                                    '<label class="dbLabel">HBase</label>'
                              '</div>';
                      };
                    }else if(tempSink[i].action == 'Notify'){
                      div = '<div class="animated bounceIn" id = "DataSink'+this.databaseCount+'" style="left:'+this.databaseLeft+'px; top:'+this.databaseTop+'px; position:absolute;height: 80px;width: 80px;">'+
                              '<i class="fa fa-envelope dataBaseCSS"></i>'+
                              '<label class="dbLabel">E-mail</label>'
                        '</div>';
                    }
                    
                    $(this.ui.boxBody).append(div);
                    source = this.getSource(tempSink[i].processorId); 
                    var target = instance.addEndpoint('DataSink'+this.databaseCount, this.targetEndpoint);
                    var obj = {
                                'id': 'DataSink'+this.databaseCount,
                                'target': target
                              };
                    var conn = instance.connect({
                                                  source:source, 
                                                  target:target,
                                                  overlays:[
                                                            [ "Label", {label:rules, id:"label", location: -50}]
                                                           ] 
                                                });
                    tempSink[i].created = true;
                    if (this.databaseCount-1 === this.deviceCount) {
                        this.deviceTop += 150;
                    };
                    this.databaseTop += 150;
                    if (this.databaseCount -1 === this.ruleSetCount) {
                        this.rulesetTop += 150;
                    };
                };
            };
            this.tempJSON.dataSinks.push(
                                          {
                                            "uiname": "hbasesink",
                                            "type": "HBASE",
                                            "config": {
                                                "rootDir": "hdfs://localhost:9000/hbase",
                                                "table": "nest",
                                                "columnFamily": "cf",
                                                "rowKey": "device_id"
                                              }
                                            }
                                        );
        };
    },

    getSource:function(processorId){
      for (var i = 0; i < this.processors.length; i++) {
          if(this.processors[i].id == processorId){
            return this.processors[i].source;
          }
      };

    },

    eSubmit:function(){
        this.datastream.attributes.json = JSON.stringify(this.tempJSON);
        this.datastream.save();
    },

    eDeploy:function(){
        $('#page-content-wrapper .fa-cog').addClass('fa-spin');
        $.ajax({
          url: 'http://localhost:8888/api/v1/catalog/datastreams/1/actions/deploy',
          dataType: 'jsonp',
          type: 'post',
          success: function(data) {
            var json_response = data;
            alert(data);
          }
        });
    },

    loadModal:function(type, deviceId){
        var content = this.getModalContent(type);
        var that = this;
        var opts = {
              title: type,
              okCloses: false,
              okText: 'Add',
              enterTriggersOk: false,
              allowCancel: true,
              showFooter: true,
              escape: true,
              content: content,
              animate: 'fadeInDown'
        };
        var modal = that.modal = new Modal(opts);
        that.modal.render();
        if (type == 'Processor') {
          this.modal.$el.find('.modal-dialog').addClass('modal-lg');
        };
        that.modal.open();

        modal.on('ok', function(){
            var that = this;

            setTimeout(function() {
                if(modal.options.title === 'Datafeed'){
                    that.addDataFeed();
                }else if(modal.options.title === 'Processor'){
                    that.addProcessorEl()
                }else if(modal.options.title === 'DataSink'){
                    that.DataSinks=that.DataSinks.concat(that.databaseModal.DataSinks);
                    that.addDatabaseEl();
                }
            }, 500);
            this.modal.close();
        },that);
    },

    getModalContent:function(type){
        if(type === 'Datafeed'){
            return(this.deviceModal = new deviceModal());
        }else if(type === 'Processor'){
            return(this.rulesetModal = new rulesetModal(this.selectedFields));
        }else if(type === 'DataSink'){
            return(this.databaseModal = new databaseModal(this.processors));
        }
    },
  });
  
  return IotasEditorLayout;
});
var instance = null;