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

import React, {Component, PropTypes} from 'react';
import Select from 'react-select';
import {Scrollbars} from 'react-custom-scrollbars';
import _ from 'lodash';

export default class StreamSidebar extends Component {
  static propTypes = {
    // streamObj: PropTypes.object.isRequired,
    streamKind: PropTypes.string.isRequired, //input or output,
    inputStreamOptions: PropTypes.array
  };

  constructor(props) {
    super(props);
    this.fieldsArr = [];
    this.state = {
      showDropdown: this.props.inputStreamOptions
        ? true
        : false
    };
  }

  handleStreamChange(obj) {
    if (obj) {
      this.context.ParentForm.setState({streamObj: obj});
    }
  }

  getSchemaFields(fields, level) {
    fields.map((field) => {
      let obj = {
        name: field.name,
        optional: field.optional,
        type: field.type,
        level: level,
        keyPath : field.keyPath
      };

      if (field.type === 'NESTED' && field.fields) {
        this.fieldsArr.push(obj);
        this.getSchemaFields(field.fields, level + 1);
      } else {
        this.fieldsArr.push(obj);
      }

    });
  }

  render() {
    const {streamKind, streamObj} = this.props;
    this.fieldsArr = [];
    if (streamObj.fields) {
      this.getSchemaFields(streamObj.fields, 0);
    }
    return (
      <div className={streamKind === 'input'
        ? "modal-sidebar-left sidebar-overflow"
        : "modal-sidebar-right sidebar-overflow"}>
        <h4>{streamKind === 'input'
            ? 'Input'
            : 'Output'}</h4>
        {this.state.showDropdown && this.props.inputStreamOptions.length > 1
          ? <form className="">
              <div className="form-group">
                <Select value={streamObj.streamId} options={this.props.inputStreamOptions} onChange={this.handleStreamChange.bind(this)} required={true} clearable={false} valueKey="streamId" labelKey="streamId"/>
              </div>
            </form>
          : ''
}
        <Scrollbars style={{
          height: "355px"
        }} autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <ul className="output-list">
            {this.fieldsArr.map((field, i) => {
              let styleObj = {
                paddingLeft: (10 * field.level) + "px"
              };
              return (
                <li key={i} style={styleObj}>
                  {
                    streamKind === "output"
                    ? <span title={field.keyPath}>{field.name}</span>
                    : field.name
                  }
                  {!field.optional && field.type !== "NESTED"
                    ? <span className="text-danger">*</span>
                    : null}
                  <span className="output-type">{field.type}</span>
                </li>
              );
            })}
          </ul>
        </Scrollbars>
      </div>
    );
  }
}

StreamSidebar.contextTypes = {
  ParentForm: React.PropTypes.object
};
