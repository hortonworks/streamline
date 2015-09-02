define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VParser'
  ], function (require, Globals, BaseCollection, vParser) {
  'use strict';
  var vParserList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/parsers',

      model: vParser,


      initialize: function () {
        this.modelName = 'VParser';
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
  return vParserList;
});