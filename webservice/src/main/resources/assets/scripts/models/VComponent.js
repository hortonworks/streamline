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

      idAttribute: 'componentId',

      initialize: function () {
        this.modelName = 'VComponent';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
      deploy: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/clusters/'+options.id +'/components', 'POST', options);
      },
    },
    {}
  );
  return VComponent;
});