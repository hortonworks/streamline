const merge = require('webpack-merge');
const webpack = require('webpack');
const config = require('./webpack.config.base');
const path = require('path');
const extractTextPlugin = require("extract-text-webpack-plugin");


const GLOBALS = {
  'process.env': {
    'NODE_ENV': JSON.stringify('development')
  },
  __DEV__: JSON.stringify(JSON.parse(process.env.DEBUG || 'true'))
};

module.exports = merge(config, {
  debug: true,
  cache: true,
  //devtool: 'cheap-module-eval-source-map',
  devtool: 'inline-source-map',
  entry: {
    application: [
      'webpack-hot-middleware/client',
      'react-hot-loader/patch',
      path.join(__dirname, '../app/scripts/main')
    ],
    vendor: ['react', 'react-dom', 'react-router' ]
  },
  plugins: [
    new extractTextPlugin("[name].css"),
    new webpack.HotModuleReplacementPlugin(),
    new webpack.DefinePlugin(GLOBALS)
  ],
  module: {
    loaders: [
      {
        test: /\.css$/,
        loader: 'style-loader'
      }, {
        test: /\.css$/,
        loader: 'css-loader',
        query: {
          //modules: true,
          localIdentName: '[name]__[local]___[hash:base64:5]'
        }
      },
    ]
  }
});
