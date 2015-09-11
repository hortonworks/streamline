define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VCluster = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/clusters',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VCluster';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
    },
    {}
  );
  return VCluster;
});