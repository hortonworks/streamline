define(['require',
  'utils/LangSupport',
  'utils/Utils',
  'utils/Globals',
  'hbs!tmpl/topology/customProcessorView'
], function(require, localization, Utils, Globals, tmpl) {
  'use strict';

  var CustomProcessorView = Marionette.LayoutView.extend({

    template: tmpl,

    templateHelpers: function(){
      var self = this;
      if(this.showOutputFields){
        var outputStreamObj = this.model.has('outputStreamToSchema') ? this.model.get('outputStreamToSchema') : _.findWhere(this.model.get('config'), {name:'outputStreamToSchema'}).defaultValue;
        this.keys = _.keys(outputStreamObj);
        if(this.keys.length){
          this.outputFields = [];
          _.each(this.keys, function(key){
            var o = {
              name: key,
              fields: JSON.stringify(outputStreamObj[key]),
              connectedNodes: self.connectedNodes
            };
            self.outputFields.push(o);
          });
        }
      }
      return {
        'showOutputFields': this.showOutputFields,
        'outputFields': this.outputFields
      };
    },

    events: {
      'click #btnCancel': 'evClose',
      'click #btnAdd': 'evAdd'
    },

    regions: {
      rForm: '#rForm',
    },

    initialize: function(options) {
      _.extend(this, options);
    },

    onRender:function(){
      var self = this;
      require(['views/topology/CustomProcessorForm'], function(CustomProcessorForm){
        self.view = new CustomProcessorForm({
          model: self.model
        });
        self.rForm.show(self.view);
      });
      this.$('.sinkConnect').select2();
      if (this.showOutputFields) {
        var selectedStreams = this.model.get('selectedStreams');
        var obj = {}, streams = [];
        _.each(selectedStreams, function(o){
          if(streams.indexOf(o.streamName) === -1){
            streams.push(o.streamName);
          }
          obj[o.streamName] = obj[o.streamName] || [];
          obj[o.streamName].push(o.name);
        });

        _.each(streams, function(name){
          self.$('.sinkConnect[data-stream="'+name+'"]').select2('val', obj[name]);
        });
      }
    },
    evAdd: function(e){
      var self = this;
      var err = this.view.validate();
      if(_.isEmpty(err)){
        this.saveCustomProcessor();
      } else {
        return false;
      }
    },
    saveCustomProcessor: function(){
      var self = this;
      var data = this.view.getData();
      var selectedStreams = [];
      _.each(this.keys, function(key){
        var nodes = self.$('.sinkConnect[data-stream="'+key+'"]').val();
        if(nodes && nodes.length){
          _.each(nodes, function(n){
            var o = { name: n, streamName: key};
            selectedStreams.push(o);
          });
        }
      });
      data.set('selectedStreams', selectedStreams);
      this.vent.trigger('topologyEditor:SaveProcessor', data);
      this.evClose();
    },
    evClose: function(e){
      this.trigger('closeModal');
    }

  });
  
  return CustomProcessorView;
});