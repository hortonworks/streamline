define(['require'], function (require) {
  'use strict';

  var Globals = {};
  
  Globals.baseURL = '';

  Globals.settings = {};
  Globals.settings.PAGE_SIZE = 25;

  Globals.AppTabs = {
    Dashboard           : { value:1, valStr: 'Dashboard'},
    DeviceCatalog       : { value:2, valStr: 'Device Catalog'},
    ParserRegistry      : { value:3, valStr: 'Parser Registry'},
    DataStreamEditor    : { value:4, valStr: 'Datastream Editor'}
  };

  Globals.Component = {};
  
  Globals.Component.Storm = [
   {value: 'NIMBUS', valStr: 'NIMBUS'},
   {value: 'SUPERVISOR', valStr: 'SUPERVISOR'},
   {value: 'UI', valStr: 'UI'}
  ];

  Globals.Component.Kafka = [
    {value: 'BROKER', valStr: 'BROKER'},
    {value: 'ZOOKEEPER', valStr: 'ZOOKEEPER'}
  ];

  Globals.Component.HDFS =[
    {value: 'NAMENODE', valStr: 'NAMENODE'},
    {value: 'DATANODE', valStr: 'DATANODE'}
  ];

  Globals.Topology = {};
  Globals.Topology.Editor = {};

  Globals.Topology.Editor.Steps = {
    Datasource  : {value: 1, valStr: 'Datasource', iconClass: 'fa fa-server'},
    Processor   : {value: 2, valStr: 'Processor', iconClass: 'fa fa-cog'},
    DataSink    : {value: 3, valStr: 'DataSink', iconClass: 'fa fa-server'}
  };

  Globals.Topology.Editor.Steps.Datasource.Substeps = [
    {value: 1, valStr: 'DEVICE', imgUrl: 'images/iconf-device.png', mainStep: Globals.Topology.Editor.Steps.Datasource.valStr, show: true, connectsTo: 'PARSER'}
  ];

  Globals.Topology.Editor.Steps.Processor.Substeps = [
    {value: 1, valStr: 'PARSER', imgUrl: 'images/iconf-parser.png', mainStep: Globals.Topology.Editor.Steps.Processor.valStr, show: false, connectsTo: 'RULE,HDFS,HBASE'},
    {value: 2, valStr: 'RULE', imgUrl: 'images/iconf-rule.png', mainStep: Globals.Topology.Editor.Steps.Processor.valStr, show: true, connectsTo: 'HDFS,HBASE,NOTIFICATION'}
  ];

  Globals.Topology.Editor.Steps.DataSink.Substeps = [
    {value: 1, valStr: 'HDFS', imgUrl: 'images/iconf-hdfs.png', mainStep: Globals.Topology.Editor.Steps.DataSink.valStr, show: true},
    {value: 2, valStr: 'HBASE', imgUrl: 'images/iconf-hbase.png', mainStep: Globals.Topology.Editor.Steps.DataSink.valStr, show: true},
    {value: 3, valStr: 'NOTIFICATION', imgUrl: 'images/iconf-notification.png', mainStep: Globals.Topology.Editor.Steps.DataSink.valStr, show: true}
  ];

  Globals.Feed = {};
  Globals.Feed.Type = [
    {value: 'KAFKA', valStr: 'KAFKA'}
  ];

  return Globals; 
});