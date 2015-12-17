define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VTopology = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/topologies',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VTopology';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
      deployTopology: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/topologies/'+options.id +'/actions/deploy', 'POST', options);
      },
      killTopology: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/topologies/'+options.id +'/actions/kill', 'POST', options);
      },
      validateTopology: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/topologies/'+options.id +'/actions/validate', 'POST', options);
      },
      getSourceComponent: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/SOURCE?streamingEngine=STORM', 'GET', options);
      },
      getProcessorComponent: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/PROCESSOR?streamingEngine=STORM', 'GET', options);
      },
      getSinkComponent: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/SINK?streamingEngine=STORM', 'GET', options);
      },
      getLinkComponent: function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/LINK?streamingEngine=STORM', 'GET', options);
      }
    },
    {}
  );
  return VTopology;
});