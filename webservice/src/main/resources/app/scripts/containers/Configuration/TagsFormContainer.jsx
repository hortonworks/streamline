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
import TagREST from '../../rest/TagREST';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';

export default class TagsFormContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      name: '',
      description: '',
      tagIds: null
    };
    if (this.props.currentId) {
      this.fetchData(this.props.currentId);
    }
  }

  fetchData(id) {
    TagREST.getTag(id).then((tags) => {
      if (tags.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={tags.responseMessage}/>, '', toastOpt);
      } else {
        let {name, description, tagIds} = tags;
        this.setState({name, description, tagIds});
      }
    }).catch((err) => {
      FSReactToastr.error(
        <CommonNotification flag="error" content={err}/>, '', toastOpt);
    });
  }

  handleValueChange(e) {
    let obj = {};
    obj[e.target.name.trim()] = e.target.value.trim();
    this.setState(obj);
  }

  validateData() {
    let {name, description} = this.state;
    if (name !== '' && description !== '') {
      return true;
    } else {
      (this.state.name.length === 0)
        ? this.nameRef.setAttribute('class', "form-control error")
        : this.nameRef.setAttribute('class', "form-control");
      (this.state.description.length === 0)
        ? this.desc.setAttribute('class', "form-control error")
        : this.desc.setAttribute('class', "form-control");
      return false;
    }
  }

  handleSave() {
    let {name, description, tagIds} = this.state;
    let {parentId, currentId} = this.props;
    let data = {
      name,
      description,
      tagIds
    };
    if (currentId) {
      return TagREST.putTag(this.props.currentId, {body: JSON.stringify(data)});
    } else {
      data.tagIds = parentId
        ? [parentId]
        : null;
      return TagREST.postTag({body: JSON.stringify(data)});
    }
  }

  render() {
    return (
      <form className="form-horizontal">
        <div className="form-group">
          <label className="col-sm-12 control-label text-left">Name*</label>
          <div className="col-sm-12">
            <input ref={(ref) => this.nameRef = ref} name="name" placeholder="Name" onChange={this.handleValueChange.bind(this)} type="text" className="form-control" value={this.state.name} required={true} autoFocus="true"/>
          </div>
          {/*this.state.name === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Name</p>
						</div>
                                        : null*/}
        </div>

        <div className="form-group">
          <label className="col-sm-12 control-label text-left">Description*</label>
          <div className="col-sm-12">
            <input ref={(ref) => this.desc = ref} name="description" placeholder="Description" onChange={this.handleValueChange.bind(this)} type="text" className="form-control" value={this.state.description} required={true}/>
          </div>
          {/*this.state.description === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Description</p>
						</div>
                                        : null*/}
        </div>
      </form>
    );
  }
}
