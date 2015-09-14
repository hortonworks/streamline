define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VComponent'
  ], function (require, Globals, BaseCollection, vComponent) {
  'use strict';
  var vComponentList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/clusters/',

      model: vComponent,


      initialize: function () {
        this.modelName = 'VComponent';
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
  return vComponentList;
});