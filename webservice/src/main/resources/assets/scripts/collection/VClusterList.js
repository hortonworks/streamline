define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VCluster'
  ], function (require, Globals, BaseCollection, vCluster) {
  'use strict';
  var vClusterList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/clusters',

      model: vCluster,


      initialize: function () {
        this.modelName = 'VCluster';
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
  return vClusterList;
});