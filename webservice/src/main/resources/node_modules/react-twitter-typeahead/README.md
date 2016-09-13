[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/erikschlegel/React-Twitter-Typeahead?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/erikschlegel/React-Twitter-Typeahead.svg)](https://travis-ci.org/erikschlegel/React-Twitter-Typeahead)

## React-Twitter-Typeahead
A stylish and flexible reactJS autosuggest component that integrates Twitter's typeahead.js with ReactJS. Typeahead.js was built by Twitter and is one of the most frequently used and trusted solutions for a battle-tested autosuggest control. 

The preview below showcases configuring this component for searching against google books using a custom template.

![](https://raw.githubusercontent.com/erikschlegel/React-Twitter-Typeahead/master/assets/react-typeahead-animation.gif)

[See some examples on our Azure site](http://reactypeahead.azurewebsites.net/example/)

## Installation
```js
git clone https://github.com/erikschlegel/React-Twitter-Typeahead.git
cd React-Twitter-Typeahead
npm install
npm run build
```
## Usage
Let's start off creating a basic typeahead by customizing the bloodhound config object. Bloodhound is typeahead.js's powerful suggestion engine. The API docs that explain the available options in the bloodhound config object are [here](https://github.com/twitter/typeahead.js/blob/master/doc/bloodhound.md#options).
```js
var React = require('react');
var ReactTypeahead = require('./lib/js/react-typeahead');
var states = ['Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California';//....

var bloodhoundConfig = {
	local: states
};

React.render(
    <ReactTypeahead bloodhound={bloodhoundConfig} 
                    placeHolder="States - A basic example"/>,
    document.getElementById('#typeaheadDiv')
);
```

You can also configure the component to make a JSONP remote call and dress up the results by using a handlebar custom template.

**Configuring the remote call**

Bloodhound allows you to transform the returned response prior to typeahead.js processing(var responseTransformation). In the example below we're extracting the data points from the response that are relevant for rendering. The URL call can be configured in the 'remote' object of the bloodhound config. All other available options are listed in Twitter's API [docs](https://github.com/twitter/typeahead.js/blob/master/doc/bloodhound.md#remote).
```js
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
  remote: {
    url: 'https://www.googleapis.com/books/v1/volumes?q=%QUERY',
    wildcard: '%QUERY',/*typeahead.js will replace the specified wildcard with the inputted value in the GET call*/
    transform: responseTransformation
  }
};
```
**Adding some style**

You can customize the presentation of the remote dataset by overriding the dataset config. All available options are listed [here](https://github.com/twitter/typeahead.js/blob/master/doc/jquery_typeahead.md#datasets). This project comes packaged with handlebars, but you're free to use your template library of choice. 
```js
var Handlebars = require('handlebars');

var datasetConfig = {
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
```
**Binding Custom Events**

Custom callbacks can be provided in the customEvents config. This sample callback is invoked when you select an option in the dropdowan. 'id' is a property on the returning dataset. All other optional callback functions can be found in the [docs](https://github.com/twitter/typeahead.js/blob/master/doc/jquery_typeahead.md#custom-events). 
```js
var selectedFunc = function(e, datum){alert('Selected book: ' + datum['id']);};
var customEvents = {
  'typeahead:selected typeahead:autocompleted': selectedFunc
};
```

**Brining it all together with some additional typeahead configuring**

```js
var typeaheadConfig = {highlight:false};

React.render(
    <ReactTypeahead bloodhound={bloodhoundRemoteConfig} 
                    datasource={datasetConfig}
                    customEvents = {customEvents}
                    typeahead={typeaheadConfig}
                    placeHolder="A remote call + custom template" />,
    document.getElementById('#typeaheadDivRpc')
);
```

## Dependencies
This requires NPM. Also, the underlying typeahead.js library uses jquery to hook some initial events to the control, so you'll need to include the following scripts towards the end of your html page.   
```html
    <script src="../vendor/jquery/jquery.js"></script>
    <script src="../vendor/typeahead.js/typeahead.bundle.js"></script>
```

## License
MIT Licensed