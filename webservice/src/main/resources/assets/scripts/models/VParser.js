define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VParser = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/parsers',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VParser';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
      getSchema:function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/parsers/'+options.parserId+'/schema', 'GET', options);
      },
    },
    {}
  );
  return VParser;
});