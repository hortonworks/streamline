'use strict';

process.env.NODE_ENV = process.env.NODE_ENV || 'production';

var webpack = require('webpack');
var path = require('path');

var port = process.env.PORT || 3000;

var devtool;

var plugins = [
  new webpack.DefinePlugin({
    'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV)
  })
];

var entry = {
  'demo0-flat-list': './demos/demo0-flat-list/index.jsx',
  'demo1-nested-list': './demos/demo1-nested-list/index.jsx',
  'demo2-drag-handles': './demos/demo2-drag-handles/index.jsx'
};

if (process.env.NODE_ENV === 'development') {
  devtool ='eval-source-map';
  plugins = plugins.concat([
    new webpack.HotModuleReplacementPlugin()
  ]);
  entry = Object.keys(entry).reduce(function (result, key) {
    result[key] = [
      'react-hot-loader/patch',
      'webpack-dev-server/client?http://0.0.0.0:' + port,
      'webpack/hot/only-dev-server',
      entry[key]
    ];
    return result;
  }, {});
} else {
  devtool ='source-map';
  plugins = plugins.concat([
    new webpack.optimize.OccurenceOrderPlugin()
  ]);
}

module.exports = {
  devtool: devtool,
  entry: entry,
  output: {
    filename: '[name]/all.js',
    publicPath: '/demos/',
    path: path.join(__dirname, 'demos')
  },
  module: {
    loaders: [
      {
        test: /\.jsx?$/,
        exclude: /build|node_modules/,
        loaders: ['babel']
      }
    ]
  },
  resolve: {
    extensions: ['', '.js', '.jsx']
  },
  plugins: plugins,
  eslint: { configFile: '.eslintrc' }
};
