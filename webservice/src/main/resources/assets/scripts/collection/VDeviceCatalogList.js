define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VDeviceCatalog'
  ], function (require, Globals, BaseCollection, vDeviceCatalog) {
  'use strict';
  var vDeviceCatalogList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/datasources',

      model: vDeviceCatalog,


      initialize: function () {
        this.modelName = 'VDeviceCatalog';
        this.modelAttrName = 'entities';
        this.bindErrorEvents();
      }
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
  return vDeviceCatalogList;
});