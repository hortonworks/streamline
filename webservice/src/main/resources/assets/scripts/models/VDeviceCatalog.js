define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VDeviceCatalog = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/datasources',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VDeviceCatalog';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
    },
    {}
  );
  return VDeviceCatalog;
});