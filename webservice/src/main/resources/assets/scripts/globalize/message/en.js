/**
 * Never Delete any key without seraching it in all View and Template files
 */
(function (window, undefined) {
  var Globalize;
  require(['globalize'], function (globalize) {
    Globalize = globalize;
    Globalize.addCultureInfo("en", {
      messages: {
        // Form labels, Table headers etc
        lbl: {
          'deviceTypeId': 'Device Type Id',
          'version': 'Version',
          'deviceConfig': 'Device Config',
          'parserClassName': 'Parser Class Name',
          'parserId': 'Parser Id',
          'name': 'Name',
          'parserVersion': 'Parser Version',
          'jarLocation': 'Jar Location',
          'nimbusHostPort': 'Nimbus Host:Port',
          'kafkaBrokerHost': 'Kafka Broker Host',
          'parserJarLocation': 'Parser Jar Location',
          'parserName': 'Parser Name',
          'className': 'Class Name',
          'parserJar': 'Parser Jar',
          'close': 'Close',
          'add': 'Add',
          'jarStoragePath': 'Jar Location',
          'deviceName': 'Device Name',
          'description': 'Description',
          'tags': 'Tags',
          'deviceId': 'Device ID',
          'deviceVersion': 'Device Version',
          'deviceType': 'Device Type',
          'type': 'Type',
          'datasourceName': 'Datasource Name',
          'actions': 'Actions'
        },
        btn: {
          'first': 'First',
          'last': 'Last',
          'previous': 'Previous',
          'next': 'Next',
        },
        // h1, h2, h3, fieldset, title
        h: {
          'addNewParser': 'Add New Parser',
          'addNewDevice': 'Add New Device',
          'addNewDatasource': 'Add New Datasource'
        },
        msg: {
          'noDeviceFound': 'No Device(s) Found',
          'noParserFound': 'No Parser(s) Found',
          'noDatasourceFound': 'No Datasource(s) Found'
        },
        plcHldr: {},
        dialogMsg: {
          'newParserAddedSuccessfully': 'New parser added successfully',
          'parserDeletedSuccessfully': 'Parser deleted successfully',
          'newDeviceAddedSuccessfully': 'New device added successfully',
          'deviceUpdatedSuccessfully': 'Device updated successfully',
          'deviceDeletedSuccessfully': 'Device deleted successfully',
          'newDatasourceAddedSuccessfully': 'New datasource added successfully',
          'datasourceUpdatedSuccessfully': 'Datasource updated successfully',
          'dataSourceDeletedSuccessfully': 'Datasource deleted successfully'
        },
        validationMessages: {},
        // Server Messages
        serverMsg: {}
      }
    });
  });
}(this));

