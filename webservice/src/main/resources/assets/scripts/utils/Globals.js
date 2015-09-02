define(['require'], function (require) {
  'use strict';

  var Globals = {};
  
  Globals.baseURL = '';

  Globals.settings = {};
  Globals.settings.PAGE_SIZE = 25;

  Globals.AppTabs = {
  	Dashboard 			    : { value:1, valStr: 'Dashboard'},
    Datasource          : { value:2, valStr: 'Datasource'},
  	DeviceCatalog 			: { value:3, valStr: 'Device Catalog'},
    ParserRegistry      : { value:4, valStr: 'Parser Registry'}
  };

  return Globals; 
});