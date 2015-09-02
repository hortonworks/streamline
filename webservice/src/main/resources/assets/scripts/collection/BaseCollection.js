define(['require',
  'utils/Globals',
  'utils/Utils',
  'backbone.paginator'
  ], function (require, Globals, Utils) {
  'use strict';

  var BaseCollection = Backbone.PageableCollection.extend(
    /** @lends BaseCollection.prototype */
    {
      /**
       * BaseCollection's initialize function
       * @augments Backbone.PageableCollection
       * @constructs
       */

      initialize: function () {

      },
      bindErrorEvents: function () {
        this.bind("error", Utils.defaultErrorHandler);
      },
      /**
       * state required for the PageableCollection
       */
      state: {
        firstPage: 0,
        pageSize: Globals.settings.PAGE_SIZE
      },

      mode: 'client',

      /**
       * override the parseRecords of PageableCollection for our use
       */
      parseRecords: function (resp, options) {
        try {
          if (!this.modelAttrName) {
            throw new Error("this.modelAttrName not defined for " + this);
          }
          return resp[this.modelAttrName];
        } catch (e) {
          console.log(e);
        }
      },

      ////////////////////////////////////////////////////////////
      // Overriding backbone-pageable page handlers methods   //
      ////////////////////////////////////////////////////////////
      getFirstPage: function (options) {
        return this.getPage('first', _.extend({
          reset: true
        }, options));
      },

      getPreviousPage: function (options) {
        return this.getPage("prev", _.extend({
          reset: true
        }, options));
      },

      getNextPage: function (options) {
        return this.getPage("next", _.extend({
          reset: true
        }, options));
      },

      getLastPage: function (options) {
        return this.getPage("last", _.extend({
          reset: true
        }, options));
      },
      hasPrevious: function(options){
        return this.hasPreviousPage();
      },
      hasNext: function(options){
        return this.hasNextPage();
      }
      /////////////////////////////
      // End overriding methods //
      /////////////////////////////


    }, 
    /** BaseCollection's Static Attributes */
    {
      // Static functions
      getTableCols: function (cols, collection) {
        var retCols = _.map(cols, function (v, k, l) {
          var defaults = collection.constructor.tableCols[k];
          if (!defaults) {
            //console.log("Error!! " + k + " not found in collection: " , collection);
            defaults = {};
          }
          return _.extend({
            'name': k
          }, defaults, v);
        });

        return retCols;
      },

      nonCrudOperation: function (url, requestMethod, options) {
        return Backbone.sync.call(this, null, this, _.extend({
          url: url,
          type: requestMethod
        }, options));
      }

    });

  return BaseCollection;
});