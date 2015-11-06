define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VTopology = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/datastreams',

      defaults: {},

      serverSchema : {},

      idAttribute: 'dataStreamId',

      initialize: function () {
        this.modelName = 'VTopology';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
      deployTopology: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/datastreams/'+options.id +'/actions/deploy', 'POST');
      }
    },
    {}
  );
  return VTopology;
});