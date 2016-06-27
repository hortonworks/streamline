define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VFile'
  ], function (require, Globals, BaseCollection, VFile) {
  'use strict';
  var VFileList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/files',

      model: VFile,


      initialize: function () {
        this.modelName = 'VFile';
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
  return VFileList;
});