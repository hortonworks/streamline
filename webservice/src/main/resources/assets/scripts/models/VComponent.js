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
        var url = Globals.baseURL + '/api/v1/catalog/clusters/'+options.id +'/components',
            type = 'POST';
        if(options.editState){
          url += '/'+options.modelId;
          type = 'PUT';
        }
        return this.constructor.nonCrudOperation.call(this, url, type, options);
      },
      destroyModel: function(options) {
        var url = Globals.baseURL + '/api/v1/catalog/clusters/'+options.clusterId +'/components/'+options.componentId,
            type = 'DELETE';        
        return this.constructor.nonCrudOperation.call(this, url, type, options);
      }
    },
    {}
  );
  return VComponent;
});