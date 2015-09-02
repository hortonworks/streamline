define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VAppState = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '',

      defaults: {
      	currentTab : Globals.AppTabs.Dashboard.value,
      },

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VAppState';
        this.bindErrorEvents();
      }
    },
    {}
  );
  return new VAppState();
});