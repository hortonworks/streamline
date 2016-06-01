define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VTag = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/tags',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VTag';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
    },
    {}
  );
  return VTag;
});