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

import React, {Component} from 'react';

export default class UnKnownAccess extends Component{
  componentDidMount(){
    document.body.className = 'login-page';
  }
  componentWillUnmount(){
    document.body.className = '';
  }
  render(){
    return(
      <div className="login-wrapper text-center">
        <img src="styles/img/SAM-logo-expanded.png" />
        <div className="login-notify">
          Access not enabled for this username.<br /> Please contact your administrator.
        </div>
      </div>
    );
  }
}
