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
import ReactDOM from 'react-dom';
import BaseContainer from '../BaseContainer';
import {Link} from 'react-router';

export default class NewsFeedContainer extends Component {
  constructor(props) {
    super();
    this.breadcrumbData = {
      title: 'News Feed',
      linkArr: [
        {
          title: 'News Feed'
        }
      ]
    };
  }
  render() {
    return (
      <BaseContainer routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
        <div className="container">
          NewsFeed
        </div>
      </BaseContainer>
    );
  }
}
