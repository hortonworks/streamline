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
import {BtnEdit, BtnDelete,BtnShare} from './ActionButtons';
import {findSingleAclObj,handleSecurePermission} from '../utils/ACLUtils';
import app_state from '../app_state';
import {observer} from 'mobx-react';

@observer
class ActionButtonGroup extends Component{
  constructor(props){
    super(props);
  }

  editUDF = (id) => {
    this.props.handleEdit(id);
  }

  deleteUDF = (id) => {
    this.props.handleDelete(id);
  }

  shareUDF = (id,obj) => {
    const {processor} = this.props;
    let aclObject ={};
    if(processor !== undefined && processor !== ''){

    } else {
      aclObject = !_.isEmpty(obj) ? obj : {objectId : id, objectNamespace : this.props.type.toLowerCase()};
    }
    this.props.handleShare(aclObject);
  }

  render(){
    const {allACL,udfObj,processor,type} = this.props;
    const userInfo = app_state.user_profile !== undefined ? app_state.user_profile.admin :false;
    let permission=true,rights_share=true,aclObject={};
    if(app_state.streamline_config.secureMode){
      aclObject = findSingleAclObj(udfObj.id,allACL || []);
      const {p_permission,r_share} = handleSecurePermission(aclObject,userInfo,type);
      permission = p_permission;
      rights_share = r_share;
    }
    const builtIn = udfObj.builtin !== undefined && udfObj.builtin !== '' ? udfObj.builtin : null;
    const paramVal = processor !== undefined && processor !== '' ? udfObj.name : udfObj.id;

    return(
      <div className="btn-action">
        {
          app_state.streamline_config.secureMode
          ? builtIn === true
            ? userInfo
              ? <BtnEdit key={1} callback={this.editUDF.bind(this,paramVal)}/>
              : ''
            : permission || userInfo
              ? [<BtnEdit key={2} callback={this.editUDF.bind(this,paramVal)}/>,
                <BtnDelete key={3} callback={this.deleteUDF.bind(this,paramVal)}/>,
                rights_share && processor === undefined
                ? <BtnShare key={4} callback={this.shareUDF.bind(this,paramVal,aclObject)}/>
              : null
              ]
              : ''
          : builtIn === true
            ? ''
            : [<BtnEdit key={5} callback={this.editUDF.bind(this,paramVal)}/>,
              <BtnDelete key={6} callback={this.deleteUDF.bind(this,paramVal)}/>]
        }
      </div>
    );
  }
}

export default ActionButtonGroup;
