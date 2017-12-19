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
import {notifyTextLimit} from '../utils/Constants';
import ParagraphShowHideComponent from '../utils/ParagraphShowHide';

class CommonNotification extends ParagraphShowHideComponent {
  constructor(props){
    super(props);
  }

  render() {
    // flag value         error, info, sucess
    const {text, data} = this.state;
    const {flag, content} = this.props;
    const {initial,moreText} = this.getIntialTextFromContent(content);
    const readMoreTag = <a href="javascript:void(0)" onClick={this.showMore}>{text}</a>;
    return (
      <div>
        {initial}
        {(data)
          ? moreText
          : null
}
        <div>
          {(flag === 'error' && moreText.length > 0)
            ? readMoreTag
            : null
}
        </div>
      </div>
    );
  }
}

CommonNotification.propTypes = {
  flag: React.PropTypes.string.isRequired,
  content: React.PropTypes.string
};

export default CommonNotification;