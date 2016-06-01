define(['require',
  'utils/Globals',
  'collection/BaseCollection',
  'models/VTag'
  ], function (require, Globals, BaseCollection, vTag) {
  'use strict';
  var vTagList = BaseCollection.extend(
    //Prototypal attributes
    {

      url: Globals.baseURL + '/api/v1/catalog/tags',

      model: vTag,


      initialize: function () {
        this.modelName = 'VTag';
        this.modelAttrName = 'entities';
        this.bindErrorEvents();
      },
      search : function(letters){
        if(letters == "") return this;

        if(letters.indexOf('\\') > -1)
          letters = letters.replace(/\\/g, '\\\\');
        if(letters.indexOf('*') > -1)
          letters = letters.replace(/\*/g, '\\*');
        if(letters.indexOf('$') > -1)
          letters = letters.replace(/\$/g, '\\$');
        if(letters.indexOf('^') > -1)
          letters = letters.replace(/\^/g, '\\^');
        if(letters.indexOf('+') > -1)
          letters = letters.replace(/\+/g, '\\+');
        if(letters.indexOf('?') > -1)
          letters = letters.replace(/\?/g, '\\?');
        if(letters.indexOf('(') > -1)
          letters = letters.replace(/\(/g, '\\(');
        if(letters.indexOf(')') > -1)
          letters = letters.replace(/\)/g, '\\)');
        if(letters.indexOf('[') > -1)
          letters = letters.replace(/\[/g, '\\[');
        if(letters.indexOf(']') > -1)
          letters = letters.replace(/\]/g, '\\]');

        var pattern = new RegExp(letters,"gi");
        return new vTagList(_.filter(this.models, function(data) {
          return data.get("name").match(pattern);
        }));
      }
    },
    //Static Class Members
    {
      /**
       * Table Cols to be passed to Backgrid
       * UI has to use this as base and extend this.
       *
       */
      tableCols: {}
    }
  );
  return vTagList;
});