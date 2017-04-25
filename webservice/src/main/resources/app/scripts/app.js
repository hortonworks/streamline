/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/
import React, { Component, PropTypes } from 'react';
import routes from './routers/routes';
import { render } from 'react-dom';
import { Router, browserHistory, hashHistory } from 'react-router';
import FSReactToastr from './components/FSReactToastr';
import { toastOpt } from './utils/Constants';
import MiscREST from './rest/MiscREST';
import app_state from './app_state';
import CommonNotification from './utils/CommonNotification';

class App extends Component {
  constructor(){
    super();
    this.fetchData();
  }
  fetchData(){
    let promiseArr = [
      MiscREST.getAllConfigs()
    ];
    Promise.all(promiseArr)
      .then((results)=>{
        if(results[0].responseMessage !== undefined){
          FSReactToastr.error(<CommonNotification flag="error" content={results[0].responseMessage}/>, '', toastOpt);
        } else {
          app_state.streamline_config = {
            registry: results[0].registry,
            dashboard: results[0].dashboard,
            secureMode: results[0].authorizerConfiguration ? true : false
          };
        }
      });
  }

  render() {
    return (
      <Router ref="router" history={hashHistory} routes={routes} />
    );
  }
}

/*const app = render(
  <App />, document.getElementById('app_container')
)*/

export default App;
