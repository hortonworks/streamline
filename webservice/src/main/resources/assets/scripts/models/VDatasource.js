define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VDatasource = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/datasources',

      defaults: {},

      serverSchema : {},

      idAttribute: 'dataSourceId',

      initialize: function () {
        this.modelName = 'VDatasource';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
    },
    {}
  );
  return VDatasource;
});