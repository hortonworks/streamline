define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VModel'
  ], function (require, Globals, BaseCollection, vModel) {
  'use strict';
  var vModelList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '',

      model: vModel,


      initialize: function () {
        this.modelName = 'VModel';
        this.modelAttrName = 'vModels';
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
  return vModelList;
});