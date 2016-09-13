import React, { Component, PropTypes } from 'react'
import routes from './routers/routes'
import { render } from 'react-dom';
import { Router, browserHistory, hashHistory } from 'react-router';

class App extends Component {
	constructor(){
		super()
	}
  render() {
    return (
      <Router ref="router" history={hashHistory} routes={routes} />
    )
  }
}

const app = render(
  <App />, document.getElementById('app_container')
)

export default app