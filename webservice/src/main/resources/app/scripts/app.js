import React, { Component, PropTypes } from 'react'
import routes from './routers/routes'
import { render } from 'react-dom';
import { Router, browserHistory, hashHistory } from 'react-router';
import FSReactToastr from './components/FSReactToastr';
import { toastOpt } from './utils/Constants';
import MiscREST from './rest/MiscREST';
import app_state from './app_state';
import CommonNotification from './utils/CommonNotification';

class App extends Component {
  constructor(){
    super()
    this.fetchData();
  }
  fetchData(){
    let promiseArr = [
      MiscREST.getAllConfigs()
    ];
    Promise.all(promiseArr)
      .then((results)=>{
        if(results[0].responseMessage !== undefined){
          FSReactToastr.error(<CommonNotification flag="error" content={results[0].responseMessage}/>, '', toastOpt)
        } else {
          app_state.streamline_config = {
            registry: results[0].registry,
            dashboard: results[0].dashboard
          };
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