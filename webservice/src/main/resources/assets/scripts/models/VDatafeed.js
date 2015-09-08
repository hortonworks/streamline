define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VDataFeed = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/feeds',

      defaults: {},

      serverSchema : {},

      idAttribute: 'dataFeedId',

      initialize: function () {
        this.modelName = 'VDataFeed';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
      getModel: function(options) {
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/feeds/?dataSourceId='+options.dataSourceId, 'GET', options);
      }
    },
    {}
  );
  return VDataFeed;
});