define(['require',
  'modules/Vent',
  'utils/LangSupport',
  'hbs!tmpl/iotas-ui/deviceModal',
  'collection/VDatasourceList',
  'jsPlumb',
  'utils/TableLayout',
  'bootstrap-multiselect',
], function(require, vent, localization, tmpl, VDatasourceList, jsPlumb, tableLayout, multiselect) {
    'use strict';
    var dataFlowModal = Marionette.LayoutView.extend({
        template: tmpl,

        events: {
        },

        ui: {
            'datafeedName': '#deviceModalTitle input',
            'deviceList': '#availableDevices',
            'selectedDevices': '#selectedDevices ',
            'addDevice': '#addDevice ',
        },

        regions: {
        },

        initialize: function() {

            this.datafeed = { 'title':""};
            this.devices = new VDatasourceList();

            this.availableDevices = [];
            this.selectedDevices = [];
            this.bindEvents();
        },

        bindEvents:function(){
          this.listenTo(this.devices, 'reset', function () {

            var that = this;
            this.devices.forEach(function(obj){
                console.log(obj.attributes);
                that.availableDevices.push(obj.attributes);
                $(that.ui.deviceList).find('select').append('<option value="'+obj.attributes.dataSourceName+'">'+obj.attributes.dataSourceName+'</option>');
            });

            $(this.ui.deviceList).find('select').multiselect({
                buttonWidth: '100%',
                numberDisplayed: 5,
                enableFiltering: true,
                filterBehavior: 'value',
                onDropdownHide: function(event) {
                  that.loadSelectedDevices();
                }
            });

          }, this);
        },

        onRender:function(){
            var that = this;
            this.devices.fetch();
        },

        loadSelectedDevices:function(){
            var selList = this.ui.deviceList.find('select').val();
            var avList = this.availableDevices;
            this.selectedDevices = [];
            if(selList && selList.length){
                for (var i = selList.length - 1; i >= 0; i--) {
                    for (var j = avList.length - 1; j >= 0; j--) {
                        if(avList[j].dataSourceName === selList[i]){
                            this.selectedDevices.push(avList[j]);
                        }
                    };
                };
            }
        },

    });
    return dataFlowModal;
});