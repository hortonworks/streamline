'use strict';

var React = require('react');
var ReactTypeahead = require('./lib/js/react-typeahead');
var Handlebars = require('handlebars');

var states = ['Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California',
  'Colorado', 'Connecticut', 'Delaware', 'Florida', 'Georgia', 'Hawaii',
  'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas', 'Kentucky', 'Louisiana',
  'Maine', 'Maryland', 'Massachusetts', 'Michigan', 'Minnesota',
  'Mississippi', 'Missouri', 'Montana', 'Nebraska', 'Nevada', 'New Hampshire',
  'New Jersey', 'New Mexico', 'New York', 'North Carolina', 'North Dakota',
  'Ohio', 'Oklahoma', 'Oregon', 'Pennsylvania', 'Rhode Island',
  'South Carolina', 'South Dakota', 'Tennessee', 'Texas', 'Utah', 'Vermont',
  'Virginia', 'Washington', 'West Virginia', 'Wisconsin', 'Wyoming'
];

var handlerbarTemplate = '<div class="tt-custom-row"> ' +
                                   '     <span class="tt-custom-cell tt-custom-thumbnail">' +
                                   '         <img src="{{thumbnail}}" />' +
                                   '     </span>' +
                                   '     <span class="tt-custom-cell">' +
                                   '           <h3>{{value}}</h3>' +
                                   '           <p>{{description}}</p>' +
                                   '     </span>' +
                                   ' </div>';

var header = '<div><span class="tt-custom-header" style="width:25%">Book Cover</span><span style="width:75%" class="tt-custom-header">Book Title/Description</span>'

var bloodhoundConfig = {
	local: states
};

React.render(
    <ReactTypeahead bloodhound={bloodhoundConfig}
                    placeHolder="States - A basic example"/>,
    document.getElementById('#typeaheadDiv')
);

var responseTransformation = function(rsp){
      var initRsp = rsp.items, maxCharacterTitleLgth = 29, maxDescLength = 80;
      var finalResult = [];

      initRsp.map(function(item){
          var title = item.volumeInfo.title;
          finalResult.push({value: title.length>maxCharacterTitleLgth?title.substring(0, maxCharacterTitleLgth):title,
                            thumbnail: item.volumeInfo.imageLinks.thumbnail,
                            id: item.id,
                            description:(item.volumeInfo.description)?item.volumeInfo.description.substring(0, maxDescLength):''});
      });

      return finalResult;
};

var bloodhoundRemoteConfig = {
  prefetch: 'https://www.googleapis.com/books/v1/volumes?q=reactjs',
  remote: {
    url: 'https://www.googleapis.com/books/v1/volumes?q=%QUERY',
    wildcard: '%QUERY',
    transform: responseTransformation
  }
};

var dsRemote = {
  name: 'books-to-buy',
  display: 'value',
  limit: 8,
  templates: {
    header: header,
    pending: '<div style="padding-left:5px;">Processing...</div>',
    empty: '<div>unable to find any books that matched your query</div>',
    suggestion: Handlebars.compile(handlerbarTemplate)
  }
};

var selectedFunc = function(e, datum){alert('Selected book: ' + datum['id']);};

var customEvents = {
  'typeahead:selected typeahead:autocompleted': selectedFunc
};

var typeaheadConfig = {highlight:false};

React.render(
    <ReactTypeahead bloodhound={bloodhoundRemoteConfig}
                    datasource={dsRemote}
                    customEvents = {customEvents}
                    typeahead={typeaheadConfig}
                    placeHolder="A remote call + custom template" />,
    document.getElementById('#typeaheadDivRpc')
);

var remoteTransformation = function(rsp){
      var initRsp = rsp.items, maxCharacterLgth = 100;
      var finalResult = [];

      initRsp.map(function(item){
          finalResult.push({value: item.volumeInfo.title});
      });

      return finalResult;
};

var bloodhoundRPCConfig = {
  prefetch: 'https://www.googleapis.com/books/v1/volumes?q=reactjs',
  remote: {
    url: 'https://www.googleapis.com/books/v1/volumes?q=%QUERY',
    wildcard: '%QUERY',
    transform: remoteTransformation
  }
};

var remoteDS = {
  name: 'best-books',
  display: 'value'
};

React.render(
    <ReactTypeahead bloodhound={bloodhoundRPCConfig}
                    datasource={remoteDS}
                    placeHolder="vanilla remote service typeahead" />,
    document.getElementById('#typeaheadDivRemote')
);
