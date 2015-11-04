define(['require'], function (require) {
  'use strict';

  var Globals = {};
  
  Globals.baseURL = '';

  Globals.settings = {};
  Globals.settings.PAGE_SIZE = 25;

  Globals.AppTabs = {
  	Dashboard 			    : { value:1, valStr: 'Dashboard'},
  	DeviceCatalog 			: { value:2, valStr: 'Device Catalog'},
    ParserRegistry      : { value:3, valStr: 'Parser Registry'},
    DataStreamEditor    : { value:4, valStr: 'Datastream Editor'}
  };

  Globals.Component = {};
  
  Globals.Component.Storm = [
   {value: 'NIMBUS', valStr: 'NIMBUS'},
   {value: 'SUPERVISOR', valStr: 'SUPERVISOR'},
   {value: 'UI', valStr: 'UI'},
   {value: 'ZK', valStr: 'ZK'}
  ];

  Globals.Component.Kafka = [
    {value: 'BROKER', valStr: 'BROKER'},
    {value: 'ZK', valStr: 'ZK'}
  ];

  Globals.Topology = {};
  Globals.Topology.Editor = {};

  Globals.Topology.Editor.Steps = {
    Datasource  : {value: 1, valStr: 'Datasource'},
    Processor   : {value: 2, valStr: 'Processor'},
    DataSink    : {value: 3, valStr: 'Data Sink'}
  };

  Globals.Topology.Editor.Steps.Datasource.Substeps = [
    {value: 1, valStr: 'Device', iconClass: 'fa fa-desktop', iconContent: '&#xf108;', mainStep: 'Datasource'}
  ];

  Globals.Topology.Editor.Steps.Processor.Substeps = [
    {value: 1, valStr: 'Rule', iconClass: 'fa fa-cog', iconContent: '&#xf013;', mainStep: 'Processor'}
  ];

  Globals.Topology.Editor.Steps.DataSink.Substeps = [
    {value: 1, valStr: 'HDFS', iconClass: 'fa fa-database', iconContent: '&#xf1c0;', mainStep: 'Data Sink'},
    {value: 2, valStr: 'HBASE', iconClass: 'fa fa-database', iconContent: '&#xf1c0;', mainStep: 'Data Sink'}
  ];

  Globals.Functions = {};
  Globals.Functions.Comparison = [
    {value: '==', valStr: 'equal to'},
    {value: '!=', valStr: 'not equal to'},
    {value: '>', valStr: 'greater than'},
    {value: '<', valStr: 'less than'},
    {value: '>=', valStr: 'greater than or equal to'},
    {value: '<=', valStr: 'less than or equal to'}
    // {value: 'contains', valStr: 'contains'},
  ];
  Globals.Functions.Logical = [
    {value: '&&', valStr: 'And'},
    {value: '!!', valStr: 'Or'}
  ];

  return Globals; 
});