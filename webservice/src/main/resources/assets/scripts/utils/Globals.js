define(['require'], function (require) {
  'use strict';

  var Globals = {};
  
  Globals.baseURL = '';

  Globals.settings = {};
  Globals.settings.PAGE_SIZE = 25;

  Globals.AppTabs = {
  	Dashboard 			    : { value:1, valStr: 'Dashboard'},
  	DeviceCatalog 			: { value:2, valStr: 'Device Catalog'},
    ParserRegistry      : { value:3, valStr: 'Parser Registry'}
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

  return Globals; 
});