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
import ManualClusterREST from '../../rest/ManualClusterREST';

class AddManualCluster extends Component{
  constructor(props){
    super(props);
    this.state = {
      clusterName :'',
      clusterDescription : ''
    };
  }


  /*
    validateForm validate All the form fields under the modelForm refs
    And if the fields is empty after focus it add the invalidInput class to a particular field
  */
  validateForm = () => {
    const formNodes = this.refs.modelForm.children;
    let errArr = [],validate = false;
    const filter = (nodes) => {
      for (let i = 0; i < nodes.length; i++) {
        if (nodes[i].children) {
          for (let j = 0; j < nodes[i].children.length; j++) {
            if (nodes[i].children[j].nodeName === "INPUT") {
              if (nodes[i].children[j].value.trim() === '') {
                nodes[i].children[j].setAttribute('class', "form-control invalidInput");
                errArr.push(j);
              } else {
                nodes[i].children[j].setAttribute('class', "form-control");
              }
            }
          }
        }
      }
      (errArr.length === 0) ? validate = true : validate = false;
      return validate;
    };
    return filter(formNodes);
  }

  /*
    inputChange accept the string and event argumants
    string on which input text has change
    This method is use for multiple input
  */
  inputChange = (string,event) => {
    const keyVal = event.target.value.trim();
    if(keyVal !== ''){
      string === "name"
      ? this.setState({clusterName  : keyVal})
      : this.setState({clusterDescription  : keyVal});
    }
  }

  /*
    handleSave  method
    An obj has been created with clusterName and description
    and POST call has been triggered to create cluster
  */
  handleSave = () => {
    const {clusterName , clusterDescription} = this.state;
    const obj = {
      name : clusterName,
      description : clusterDescription
    };
    return ManualClusterREST.postManualCluster({body : JSON.stringify(obj)});
  }

  render(){
    return(
      <form className="modal-form config-modal-form" ref="modelForm">
        <div className="form-group">
            <label>Name<span className="text-danger">*</span>
            </label>
            <input type="text" className="form-control" placeholder="Cluster Name" ref="clusterName" autoFocus="true" onChange={this.inputChange.bind(this,"name")}/>
            <p className="text-danger"></p>
        </div>
        <div className="form-group">
            <label>Description<span className="text-danger">*</span>
            </label>
            <input type="text" className="form-control" placeholder="Description" ref="clusterDescription" onChange={this.inputChange.bind(this,"description")}/>
            <p className="text-danger"></p>
        </div>
      </form>
    );
  }
}

export default AddManualCluster;
