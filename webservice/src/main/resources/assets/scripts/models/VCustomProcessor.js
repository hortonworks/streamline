define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VCustomProcessor = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/PROCESSOR/custom',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
      getCustomProcessor:function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/PROCESSOR/custom?name='+options.id, 'GET', options);
      },
      destroyModel: function(options) {       
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/system/componentdefinitions/PROCESSOR/custom/'+options.id, 'DELETE', options);
      }
    },
    {}
  );
  return VCustomProcessor;
});