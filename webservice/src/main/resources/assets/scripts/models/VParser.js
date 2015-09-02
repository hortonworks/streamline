define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VParser = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '',

      defaults: {},

      serverSchema : {},

      idAttribute: 'parserId',

      initialize: function () {
        this.modelName = 'VParser';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
    },
    {}
  );
  return VParser;
});