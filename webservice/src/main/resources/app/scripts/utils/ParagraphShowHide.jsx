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

class ParagraphShowHideComponent extends Component{
  constructor(props){
    super(props);
    this.state = {
      data: false,
      text: "Read more"
    };
  }

  showMore = () => {
    if (this.state.text === "Read more") {
      this.setState({text: "Hide", data: true});
    } else {
      this.setState({text: "Read more", data: false});
    }
  }

  getIntialTextFromContent = (content) => {
    const {showMaxText} = this.props;
    return {
      initial : content.substr(0, showMaxText || notifyTextLimit),
      moreText : content.substr(showMaxText || notifyTextLimit)
    };
  }

  getATag = () => {
    const {text} = this.state;
    const {linkClass} = this.props;
    return  <a  className={!!linkClass ? `pull-right ${linkClass}` : ""} href="javascript:void(0)" onClick={this.showMore}>{text}</a>;
  }

  render(){
    const {text, data} = this.state;
    const {content} = this.props;
    const {initial,moreText} = this.getIntialTextFromContent(content);
    const readMore = this.getATag(text);
    return (
      <div>
        {initial}
        {text === 'Read more' && moreText.length > 0 ? '...' : null}
        {(data)
          ? moreText
          : null
        }
        <div>
          {(moreText.length > 0)
            ? this.getATag()
            : null
          }
        </div>
      </div>
    );
  }
}

export default ParagraphShowHideComponent;