import React, { Component, PropTypes } from 'react'
import routes from './routers/routes'
import { render } from 'react-dom';
import { Router, browserHistory, hashHistory } from 'react-router';
import fetch from 'isomorphic-fetch';
import {baseUrl} from './utils/Constants';

class App extends Component {
	constructor(){
		super()
		this.validateDashboardAPI()
	}
	validateDashboardAPI(){
		let apiURL = '/api/v1/dashboards';
		fetch(apiURL, {method: 'GET'}).then((res)=>{return res.json()}).then((result)=>{
			if(result.entities.length == 0){
				fetch(apiURL, {
					method: 'POST',
					body: JSON.stringify({name: 'dashboard', description: 'dashboard', data: ''}),
					headers: {
						'Content-Type' : 'application/json',
						'Accept' : 'application/json'
					}
				})
			}
		})
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