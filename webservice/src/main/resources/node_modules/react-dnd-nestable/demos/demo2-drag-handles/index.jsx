import React from 'react';
import ReactDOM from 'react-dom';
import Demo from './Demo';
import { AppContainer } from 'react-hot-loader';

const rootEl = document.querySelector('#content');

ReactDOM.render(
  <AppContainer>
    <Demo />
  </AppContainer>,
  rootEl
);

if (module.hot) {
  module.hot.accept('./Demo', () => {
    const NewDemo = require('./Demo').default;

    ReactDOM.render(
      <AppContainer>
        <NewDemo />
      </AppContainer>,
      rootEl
    );
  });
}
