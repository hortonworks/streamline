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

export default class ErrorStatus extends Component{
  render(){
    const {imgName} = this.props;
    const imgUrl = `styles/img/back-${imgName}.png`;
    const divStyle = {
      backgroundImage: 'url(' + imgUrl + ')',
      backgroundRepeat: "no-repeat",
      backgroundPosition: "right top",
      backgroundSize: "50%",
      height: window.innerHeight - 124
    };
    return(
      <div className="col-sm-12" style={divStyle}>
        <div className="row">
          <div className="col-md-9 col-md-offset-2 intro-section">
            <h4 className="intro-section-title" style={{fontSize : 36}}>Unauthorized Access.</h4>
            <div className="list">
              <h4>Please contact admin
              </h4>
              <div>
                To get appropriate access for<br/> Applications, Environments, and Services.
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
};
