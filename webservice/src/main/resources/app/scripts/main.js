require('file?name=[name].[ext]!../../index.html'); //for production build

import debug from 'debug';
import 'babel-polyfill'
import React, { Component, PropTypes } from 'react'
import { render } from 'react-dom';
import App from './app';
import {AppContainer} from 'react-hot-loader';

import '../styles/css/font-awesome.min.css';
import '../styles/css/bootstrap.css';
import 'animate.css/animate.css';
import 'bootstrap-daterangepicker/daterangepicker.css';
import 'react-select/dist/react-select.css';
import 'react-bootstrap-switch/dist/css/bootstrap3/react-bootstrap-switch.min.css';
import '../styles/css/toastr.min.css';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint.css';
import '../styles/css/customScroll.css';
import '../styles/css/paper-bootstrap-wizard.css';
import '../styles/css/style.css';
import '../styles/css/graph-style.css';


render(
  <AppContainer>
  <App/>
</AppContainer>, document.getElementById('app_container'))

if (module.hot) {
  module.hot.accept('./app', () => {
    const NextApp = require('./app').default
    render(
      <AppContainer>
      <NextApp/>
    </AppContainer>, document.getElementById('app_container'));
  });
}
