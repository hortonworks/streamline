define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VCustomProcessor'
  ], function (require, Globals, BaseCollection, VCustomProcessor) {
  'use strict';
  var VCustomProcessorList = BaseCollection.extend(

    {

      url: Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/PROCESSOR/custom',

      model: VCustomProcessor,

      initialize: function () {
        this.modelName = 'VCustomProcessor';
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
  return VCustomProcessorList;
});