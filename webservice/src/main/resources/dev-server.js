// Creates a hot reloading development environment

const path = require('path');
const express = require('express');
const webpack = require('webpack');
const webpackDevMiddleware = require('webpack-dev-middleware');
const webpackHotMiddleware = require('webpack-hot-middleware');
// const DashboardPlugin = require('webpack-dashboard/plugin');
const config = require('./config/webpack.config.development');

const app = express();
const compiler = webpack(config);

// Apply CLI dashboard for your webpack dev server
// compiler.apply(new DashboardPlugin());

const host = process.env.HOST || 'localhost';
const port = process.env.PORT || 9999;

function log() {
  arguments[0] = '\nWebpack: ' + arguments[0];
  console.log.apply(console, arguments);
}

app.use(webpackDevMiddleware(compiler, {
  noInfo: true,
  publicPath: config.output.publicPath,
  stats: {
    colors: true
  },
  historyApiFallback: true
}));

app.use(webpackHotMiddleware(compiler));

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, './index.html'));
});
app.use("/", express.static(__dirname + '/app'));


//-------------------proxy-------------------

const proxyMiddleware = require('http-proxy-middleware');
const restTarget = 'http://0.0.0.0:8080';

const proxyTable = {};    // when request.headers.host == 'dev.localhost:3000',
proxyTable[host +':'+ port] = restTarget;   // override target 'http://www.example.org' to 'http://localhost:8000'

// configure proxy middleware options
const options = {
    target: restTarget, // target host
    changeOrigin: true, // needed for virtual hosted sites
    ws: true, // proxy websockets
    router: proxyTable,
    onProxyRes: function(proxyRes, req, res) {
        if (proxyRes.headers['set-cookie']) {
            var _cookie = proxyRes.headers['set-cookie'][0];
            _cookie = _cookie.replace(/Path=\/[a-zA-Z0-9_.-]*\/;/gi,"Path=/;");
            proxyRes.headers['set-cookie'] = [_cookie];
        }
    },
    onProxyReq: function(proxyReq, req, res) {

    },
    onError: function(err, req, res) {
        console.log(req, res, function(){});
    }
};

const context = ['/api'];             // requests with this path will be proxied
const proxy = proxyMiddleware(context, options);

app.use(proxy);
//-------------------proxy-------------------

app.listen(port, '0.0.0.0', (err) => {
  if (err) {
    log(err);
    return;
  }

  log('🚧  App is listening at http://%s:%s', host, port);
});
