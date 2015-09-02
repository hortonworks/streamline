define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VDatasource'
  ], function (require, Globals, BaseCollection, vDeviceCatalog) {
  'use strict';
  var vDatasourceList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/datasources',

      model: vDeviceCatalog,


      initialize: function () {
        this.modelName = 'VDatasource';
        this.modelAttrName = 'entities';
        this.bindErrorEvents();
      },
      fetchDeviceType: function(options) {
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/datasources/type/DEVICE/', 'GET', options);
      },
    },
    //Static Class Members
    {
      /**
       * Table Cols to be passed to Backgrid
       * UI has to use this as base and extend this.
       *
       */
      tableCols: {}
    }
  );
  return vDatasourceList;
});