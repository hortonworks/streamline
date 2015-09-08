define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VDatafeed'
  ], function (require, Globals, BaseCollection, vDatafeed) {
  'use strict';
  var vDatafeedList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/feeds',

      model: vDatafeed,


      initialize: function () {
        this.modelName = 'VDataFeed';
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
  return vDatafeedList;
});