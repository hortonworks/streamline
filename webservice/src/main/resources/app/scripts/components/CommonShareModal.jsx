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
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {OverlayTrigger, Popover} from 'react-bootstrap';
import {observer} from 'mobx-react';
import Select from 'react-select';
import UserRoleREST from '../rest/UserRoleREST';
import FSReactToastr from './FSReactToastr';
import {toastOpt} from '../utils/Constants';
import TopologyUtils from '../utils/TopologyUtils';

/* component import */
import NoData from './NoData';
import CommonNotification from '../utils/CommonNotification';
import app_state from '../app_state';

export default class CommonShareModal extends Component{
  constructor(props){
    super(props);
    this.state = {
      showLoading : false,
      accessControl : false,
      userList : [],
      selectedUsers : [],
      selectedUserAccess : '',
      shareObj : props.shareObj || {},
      accessUserList : [{
        user : '',
        accessControl : { label : "Can Edit", value : 'Can Edit'}
      }],
      accessList : [
        { label : "Can Edit", value : 'Can Edit'},
        { label : "Can View", value : 'Can View'}
      ],
      showUserCaption : '',
      selectedUserList : [],
      tempUserList : []
    };
    this.fetchData();
  }

  fetchData = () => {
    const {shareObj} = this.props;
    let promiseArr = [];
    promiseArr = [UserRoleREST.getUserACL(shareObj.objectNamespace,shareObj.objectId), UserRoleREST.getAllUsers()];

    Promise.all(promiseArr).then((results) => {
      _.map(results, (result) => {
        if(result.responseMessage !== undefined){
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        }
      });
      let stateObj={},userACl=[];
      userACl = results[0].entities;
      stateObj.selectedUserList = _.filter(userACl, (acl) => {
        return acl.owner === false ;
      });
      this.userOptions = results[1].entities;

      stateObj.tempUserList = _.filter(this.userOptions, (user) => {
        return _.find(stateObj.selectedUserList, (list) => {return list.sidId === user.id;});
      });

      stateObj.userList = this.filterUserOptions(this.userOptions, stateObj.tempUserList);

      stateObj.showUserCaption = this.getUserListCaption(stateObj.tempUserList);

      stateObj.selectedUserAccess = this.state.accessList[0];
      if(stateObj.tempUserList.length){
        stateObj.accessUserList = this.generatUserAccessList(stateObj.selectedUserList, stateObj.tempUserList);
      }
      this.setState(stateObj);
    });
  }

  generatUserAccessList = (orgList ,tempList) => {
    let obj = [];
    _.map(tempList , (t_list) => {
      const index = _.findIndex(orgList, (org_list) => { return org_list.sidId === t_list.id;});
      if(index !== -1){
        const permission = TopologyUtils.getPermission(orgList[index].permissions);
        const p_string = permission ? "Can View" : "Can Edit";
        obj.push({
          user : t_list,
          accessControl : { label : p_string, value : p_string}
        });
      }
    });
    return obj;
  }

  filterUserOptions = (optionList, userList) => {
    let result = [];
    _.map(optionList, (opt)=>{
      let o = _.find(userList, (list)=>{ return list.id === opt.id;});
      if(!o){
        result.push(opt);
      }
    });
    return result;
  }

  getUserListCaption = (list) => {
    let string = [];
    _.map(list, (l,i) => {
      if(i < 2){
        string.push(l.name);
      }
    });
    const len = list.length - string.length;
    const other = len > 2 ? `and ${len} others..` : '';
    return `Shared with ${string.join(', ')} ${len > 2 ? other : ''}`;
  }

  handleUserChange = (arrList) => {
    let tempArr= [];
    const {selectedUserList} = this.state;
    let tempSelected = _.cloneDeep(this.state.tempUserList);
    let newUserList = [];
    _.map(arrList, (a)=>{
      let o = _.findIndex(tempSelected, (t)=>{return t.id === a.id;});
      if(o === -1){
        newUserList.push(a);
      }
    });
    Array.prototype.push.apply(tempSelected, newUserList);
    let userList = this.filterUserOptions(this.userOptions, tempSelected);
    let tempAccessUser = _.cloneDeep(this.state.accessUserList);
    _.map(newUserList, (arr,i) => {
      tempArr[i] === undefined
        ? tempArr[i] = {user : '',accessControl : { label : "Can Edit", value : 'Can Edit'}}
        : '';
      tempArr[i].user = arr;
    });

    const mergeAccessList = _.concat(tempAccessUser,tempArr);
    this.setState({selectedUsers : arrList , accessUserList : mergeAccessList, tempUserList: tempSelected, userList: userList});
  }

  handleUserCanEdit = (type,index,obj) => {
    if(!_.isEmpty(obj)){
      let tempAccessUser = _.cloneDeep(this.state.accessUserList);
      if(type === "user"){
        this.setState({selectedUserAccess : obj});
      } else {
        tempAccessUser[index].accessControl = obj;
        this.setState({accessUserList : tempAccessUser});
      }
    }
  }

  changeAccess = () => {
    this.setState({accessControl : true});
  }

  validate = () => {
    const {selectedUsers,accessUserList} = this.state;
    let validate = [];
    if(selectedUsers.length === 0 ){
      validate.push(false);
    }
    _.map(accessUserList, (userList) => {
      if(userList.user.name === ''){
        validate.push(false);
      }
    });
    return validate.length === 0 ? true : false;
  }

  splitPostUser = (accessList,selectList) => {
    return _.filter(accessList, (acs) => {
      return _.find(selectList, (list) => { return list.name === acs.user.name;});
    });
  }

  splitPustUser = (accessList,selectList) => {
    return _.filter(accessList, (acs) => {
      return _.find(selectList, (list) => { return list.name !== acs.user.name;});
    });
  }

  generetOutPutObj = (objList,type) => {
    const {shareObj} = this.props;
    const {selectedUserList} = this.state;
    let obj =[];
    _.map(objList , (o ,i) => {
      const permission = o.accessControl.value === "Can Edit"
                        ? ["READ","WRITE","EXECUTE","DELETE"]
                        : ["READ"];
      obj.push({
        "objectId":shareObj.objectId,
        "objectNamespace":shareObj.objectNamespace,
        "sidId": o.user.id,
        "sidType": "USER",
        "permissions" : permission,
        "owner": false,
        "grant": false
      });
      if(type === "put"){
        const orgObj = this.getOrginalObj(selectedUserList,o);
        obj[i].id = orgObj.id;
      }
      if(type === "delete"){
        const orgObj = this.getOrginalObj(selectedUserList,o);
        obj[i].id = orgObj.id;
      }
    });
    return obj;
  }

  getOrginalObj = (orgUserList,o) => {
    return _.find(orgUserList , (user) => {return user.sidId === o.user.id;});
  }

  handleSave = () => {
    let postData = [],putData=[];
    const {selectedUsers,accessUserList,selectedUserList} = this.state;
    // this postObjArr is for POST
    const postObjArr = this.splitPostUser(accessUserList,selectedUsers);

    const putObjArr = this.splitPustUser(accessUserList,selectedUsers);

    if(postObjArr.length){
      postData = this.generetOutPutObj(postObjArr,'post');
    }
    if(putObjArr.length){
      const obj =[];
      _.map(selectedUserList, (list) => {
        // const index = _.findIndex
      });
      putData = this.generetOutPutObj(putObjArr,'put');
    }

    let promiseArr=[];

    _.map(postData, (data) => {
      promiseArr.push(UserRoleREST.postACL({body : JSON.stringify(data)}));
    });

    _.map(putData , (d) => {
      promiseArr.push(UserRoleREST.putACL(d.id, {body : JSON.stringify(d)}));
    });

    return Promise.all(promiseArr);
  }

  deleteAccessUserControl = (index) => {
    let tempAccessUser = _.cloneDeep(this.state.accessUserList);
    let tempSelectedUser = _.cloneDeep(this.state.selectedUsers);
    //find user
    const tempUser = tempAccessUser[index].user;

    //add user into dropdown again
    let userList = this.state.userList;
    userList.push(tempUser);

    //remove from tempuserlist
    let tempUserList = this.state.tempUserList;
    const i = _.findIndex(tempUserList , (t) => { return t.id === tempUser.id;});
    tempUserList.splice(i, 1);

    const userIndex = _.findIndex(tempSelectedUser , (selectedUser) => { return selectedUser.name === tempUser.name;});
    if(userIndex !== -1){
      tempSelectedUser.splice(userIndex , 1);
    }
    tempAccessUser.splice(index , 1);
    this.setState({accessUserList : tempAccessUser, selectedUsers : tempSelectedUser, tempUserList:tempUserList, userList: userList});
  }

  render(){
    const {showLoading,accessControl,userList,selectedUsers,selectedUserAccess,accessList,accessUserList,showUserCaption} = this.state;

    return(
      <div className="cp-modal-form">
        {
          showLoading
          ? <div className="loading-img text-center">
              <img src="styles/img/start-loader.gif" alt="loading" style={{
                marginTop: "140px"
              }}/>
            </div>
          : <div  className="customFormClass">
              <div className="form-group">
                <div className="row">
                  <div className="col-sm-12">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Invite users</Popover>}>
                      <label>INVITE USERS
                      </label>
                    </OverlayTrigger>
                  </div>
                </div>
                <div className="row">
                  <div className="col-sm-9">
                    <Select value={selectedUsers} options={userList} onChange={this.handleUserChange.bind(this)} multi={true} required={true}  valueKey="name" labelKey="name"  clearable={false}/>
                  </div>
                  <div className="col-sm-3">
                    <Select value={selectedUserAccess} options={accessList}  onChange={this.handleUserCanEdit.bind(this,'user',null)} required={true}  valueKey="value" labelKey="value" clearable={false} />
                  </div>
                  {
                    accessUserList.length !== 0 && !accessControl
                    ? <span style={{marginLeft:10,fontSize:12}}>
                        {showUserCaption} <a href="javascript:void(0)" onClick={this.changeAccess.bind(this)}>Change Access</a>
                      </span>
                    : ''
                  }
                </div>
              </div>
              {
                accessControl
                ? <div>
                    <div className="form-group">
                        <div className="row">
                          <div className="col-sm-12">
                            <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Access control</Popover>}>
                              <label>ACCESS CONTROL
                              </label>
                            </OverlayTrigger>
                          </div>
                        </div>
                      </div>
                      {
                        _.map(accessUserList , (auList ,i) => {
                          return  <div key={i} className="form-group">
                                    <div className="row">
                                      <div className="col-sm-8">
                                        <input type="text" className="form-control" disabled={true} value={auList.user.name}disabled={true} />
                                      </div>
                                      <div className="col-sm-3">
                                        <Select value={auList.accessControl} options={accessList} onChange={this.handleUserCanEdit.bind(this ,'userAccess',i)} required={true}  valueKey="label" labelKey="label"  clearable={false} />
                                      </div>
                                      <div className="col-sm-1">
                                        <a href="javascript:void(0)" onClick={this.deleteAccessUserControl.bind(this, i)}><i className="fa fa-times"></i></a>
                                      </div>
                                    </div>
                                  </div>;
                        })
                      }
                      {
                        accessUserList.length === 0
                        ? <div className="row">
                            <div className="col-sm-12">
                              No Record found
                            </div>
                          </div>
                        : ''
                      }
                  </div>
              : ''
              }
            </div>
        }
      </div>
    );
  }
}
