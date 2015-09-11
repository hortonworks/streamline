define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VComponent = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/clusters/',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VComponent';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
    },
    {}
  );
  return VComponent;
});