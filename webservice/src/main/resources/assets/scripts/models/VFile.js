define(['require',
  'utils/Globals',
  'models/BaseModel'
  ], function (require, Globals, vBaseModel) {
  'use strict';
  var VFile = vBaseModel.extend(
    {
      urlRoot: Globals.baseURL + '/api/v1/catalog/files',

      defaults: {},

      serverSchema : {},

      idAttribute: 'id',

      initialize: function () {
        this.modelName = 'VFile';
        this.bindErrorEvents();
      },
      toString : function() {
        return this.get('name');
      },
      getFileModel:function(options){
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/files/'+options.id, 'GET', options);
      },
      destroyFileModel: function(options) {
        return this.constructor.nonCrudOperation.call(this, Globals.baseURL + '/api/v1/catalog/files/'+options.id, 'DELETE', options);
      }
    },
    {}
  );
  return VFile;
});