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
    {value: 1, valStr: 'DEVICE', imgUrl: 'images/iconf-device.png', parentType: Globals.Topology.Editor.Steps.Datasource.valStr, show: true, connectsTo: 'PARSER'}
  ];

  Globals.Topology.Editor.Steps.Processor.Substeps = [
    {value: 1, valStr: 'PARSER', imgUrl: 'images/iconf-parser.png', parentType: Globals.Topology.Editor.Steps.Processor.valStr, show: false, connectsTo: 'RULE,HDFS,HBASE,CUSTOM,NORMALIZATION,SPLIT'},
    {value: 2, valStr: 'RULE', imgUrl: 'images/iconf-rule.png', parentType: Globals.Topology.Editor.Steps.Processor.valStr, show: true, connectsTo: 'RULE,CUSTOM,HDFS,HBASE,NOTIFICATION,NORMALIZATION,SPLIT'},
    {value: 3, valStr: 'CUSTOM', imgUrl: 'images/icon-custom.png', parentType: Globals.Topology.Editor.Steps.Processor.valStr, show: true, connectsTo: 'RULE,HDFS,HBASE,NOTIFICATION,NORMALIZATION,SPLIT'},
    {value: 4, valStr: 'NORMALIZATION', imgUrl: 'images/icon-fprocessor02.png', parentType: Globals.Topology.Editor.Steps.Processor.valStr, show: true, connectsTo: 'RULE,CUSTOM,HDFS,HBASE,NOTIFICATION,SPLIT'},
    {value: 5, valStr: 'SPLIT', imgUrl: 'images/icon-split.png', parentType: Globals.Topology.Editor.Steps.Processor.valStr, show: true, connectsTo: 'STAGE'},
    {value: 6, valStr: 'STAGE', imgUrl: 'images/icon-stage.png', parentType: Globals.Topology.Editor.Steps.Processor.valStr, show: true, connectsTo: 'STAGE,JOIN'},
    {value: 7, valStr: 'JOIN', imgUrl: 'images/icon-join.png', parentType: Globals.Topology.Editor.Steps.Processor.valStr, show: true, connectsTo: 'RULE,CUSTOM,NORMALIZATION,HDFS,HBASE,NOTIFICATION'}
  ];

  Globals.Topology.Editor.Steps.DataSink.Substeps = [
    {value: 1, valStr: 'HDFS', imgUrl: 'images/iconf-hdfs.png', parentType: Globals.Topology.Editor.Steps.DataSink.valStr, show: true},
    {value: 2, valStr: 'HBASE', imgUrl: 'images/iconf-hbase.png', parentType: Globals.Topology.Editor.Steps.DataSink.valStr, show: true},
    {value: 3, valStr: 'NOTIFICATION', imgUrl: 'images/iconf-notification.png', parentType: Globals.Topology.Editor.Steps.DataSink.valStr, show: true}
  ];

  Globals.Feed = {};
  Globals.Feed.Type = [
    {value: 'KAFKA', valStr: 'KAFKA'}
  ];

  return Globals; 
});