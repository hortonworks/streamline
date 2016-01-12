define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VTopology'
  ], function (require, Globals, BaseCollection, vTopology) {
  'use strict';
  var vTopologyList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/topologies',

      model: vTopology,


      initialize: function () {
        this.modelName = 'VTopology';
        this.modelAttrName = 'id';
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
  return vTopologyList;
});