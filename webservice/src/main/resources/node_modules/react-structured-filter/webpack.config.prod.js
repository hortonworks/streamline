const path = require( 'path' );
const webpack = require( 'webpack' );

module.exports = {
  devtool: 'source-map',
  entry: [
    path.resolve( __dirname, 'example/src/main.js' ),
  ],
  output: {
    path: path.resolve( __dirname, 'example/dist/assets' ),
    filename: 'bundle.js',
    publicPath: '/assets/',
  },
  plugins: [
    new webpack.optimize.OccurenceOrderPlugin(),
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify( 'production' ),
      },
    }),
    new webpack.optimize.UglifyJsPlugin({
      compressor: {
        warnings: false,
      },
    }),
  ],
  resolve: {
    extensions: [ '', '.js', '.jsx' ],
    alias: {
      app: path.resolve( __dirname, 'example/src' ),
      repo: path.resolve( __dirname ),
    },
  },
  module: {
    preLoaders: [
      {
        test: /\.(js|jsx)$/,
        loader: 'eslint',
        include: path.resolve( __dirname, 'src' ),
      },
    ],
    loaders: [
      {
        test: /\.(js|jsx)$/,
        loader: 'babel',
        include: [
          path.resolve( __dirname, 'src' ),
          path.resolve( __dirname, 'example/src' ),
        ],
      },
      {
        test: /\.(less|css)$/,
        loader: 'style!css!less',
      },
      {
        test: /\.(woff|woff2)(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'url-loader?limit=10000&mimetype=application/font-woff',
      },
      {
        test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'url-loader?limit=10000&mimetype=application/octet-stream',
      },
      {
        test: /\.(eot|png)(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'file',
      },
      {
        test: /\.svg(\?v=\d+\.\d+\.\d+)?$/,
        loader: 'url-loader?limit=10000&mimetype=image/svg+xml',
      },
    ],
  },
};
