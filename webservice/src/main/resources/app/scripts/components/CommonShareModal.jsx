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
import {OverlayTrigger, Popover,DropdownButton,MenuItem} from 'react-bootstrap';
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
      showLoading : true,
      accessControl : false,
      userList : [],
      selectedUsers : '',
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
    this.mapCanView  = [];
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
      let stateObj={},userACl=[],ownerObj={};
      userACl = results[0].entities;
      stateObj.selectedUserList = _.filter(userACl, (acl) => {
        acl.owner === true ? ownerObj = acl : "";
        return acl.owner === false ;
      });

      const tempOptions = results[1].entities;

      this.userOptions = _.filter(tempOptions, (opt) => {return opt.id !== ownerObj.sidId;});

      stateObj.tempUserList = _.filter(this.userOptions, (user) => {
        return _.find(stateObj.selectedUserList, (list) => {return list.sidId === user.id;});
      });

      this.sharedUserList = _.cloneDeep(stateObj.tempUserList);

      stateObj.userList = this.filterUserOptions(this.userOptions, stateObj.tempUserList);

      stateObj.showUserCaption = this.getUserListCaption(stateObj.tempUserList);

      stateObj.selectedUserAccess = this.state.accessList[0];
      if(stateObj.tempUserList.length){
        stateObj.accessUserList = this.generatUserAccessList(stateObj.selectedUserList, stateObj.tempUserList);
      } else {
        stateObj.accessUserList = [];
      }
      stateObj.showLoading = false;
      this.setState(stateObj);
    });
  }

  generatUserAccessList = (orgList ,tempList) => {
    let obj = [];
    _.map(tempList , (t_list) => {
      const index = _.findIndex(orgList, (org_list) => { return org_list.sidId === t_list.id;});
      if(index !== -1){
        const permission = TopologyUtils.getPermission(orgList[index].permissions,orgList[index]);
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
    const other = list > 2 ? `and ${len} others..    ` : '';
    return `${list.length > 0 ? 'Shared with ' : ''} ${string.join(', ')} ${list > 2 ? other : ''}`;
  }

  handleUserChange = (arrList) => {
    let tempArr= [];
    const {selectedUserList} = this.state;
    let tempSelected = _.cloneDeep(this.state.tempUserList);
    let tempAccessUser = _.cloneDeep(this.state.accessUserList);
    let newUserList = [];
    _.map(arrList, (a)=>{
      let o = _.findIndex(tempSelected, (t)=>{return t.id === a.id;});
      if(o === -1){
        newUserList.push(a);
      }
    });
    // check if the both has no value
    if(selectedUserList.length === 0 && arrList.length === 0){
      tempSelected = [];
      tempAccessUser =[];
    }
    this.mapCanView = arrList;
    Array.prototype.push.apply(tempSelected, newUserList);
    let userList = this.filterUserOptions(this.userOptions, tempSelected);
    if(tempSelected.length > arrList.length && selectedUserList.length === 0){
      _.map(arrList, (a)=>{
        let o = _.findIndex(tempSelected, (t)=>{return t.id !== a.id;});
        if(o !== -1){
          userList.push(tempSelected[o]);
          const ind = _.findIndex(tempAccessUser, (f) => {return f.user.id === tempSelected[o].id;});
          if(ind !== -1){
            tempAccessUser.splice(ind,1);
            tempSelected.splice(o,1);
          }
        }
      });
    }
    _.map(newUserList, (arr,i) => {
      tempArr[i] === undefined
        ? tempArr[i] = {user : '',accessControl : { label : "Can Edit", value : 'Can Edit'}}
        : '';
      tempArr[i].user = arr;
    });

    const mergeAccessList = _.concat(tempAccessUser,tempArr);
    this.setState({selectedUsers : arrList , accessUserList : mergeAccessList, tempUserList: tempSelected, userList: userList});
  }

  handleUserCanEdit = (type,key,index,obj) => {
    if(!_.isEmpty(obj)){
      let tempAccessUser = _.cloneDeep(this.state.accessUserList);
      if(type === "user"){
        // _.map(this.mapCanView, (can) => {
        //   const _index = _.findIndex(tempAccessUser, (t) => {return t.user.id === can.id;});
        //   if(_index !== -1){
        //
        //   }
        // });
        this.setState({selectedUserAccess : obj});
      } else {
        tempAccessUser[index].accessControl = {label : key , value : key};
        this.setState({accessUserList : tempAccessUser});
      }
    }
  }

  changeAccess = () => {
    this.setState({accessControl : true});
  }

  splitPostUser = (accessList,selectList) => {
    let postList=[],putList=[];
    _.map(accessList, (acs,i) => {
      const index = _.findIndex(selectList, (list) => { return list.name === acs.user.name;});
      if(index !== -1){
        postList.push(acs);
      } else {
        putList.push(acs);
      }
    });
    return {postList,putList};
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
    let postData=[],putData=[],deleteData=[],deletedObjArr=[];
    const {selectedUsers,accessUserList,selectedUserList} = this.state;
    // this postObjArr is for POST
    let {postList,putList} = this.splitPostUser(accessUserList,selectedUsers);

    let postObjArr = postList;
    let putObjArr = putList;

    if(postObjArr.length){
      const tempPost = _.cloneDeep(postObjArr);
      _.map(tempPost, (pObj,i) => {
        const index = _.findIndex(selectedUserList, (list) => { return list.sidId === pObj.user.id;});
        if(index  !== -1){
          putObjArr.push(pObj);
          postObjArr.splice(i,1);
        }
      });
      postObjArr.length > 0
        ? postData = this.generetOutPutObj(postObjArr,'post')
        : '';
    }

    if(putObjArr.length){
      _.map(selectedUserList, (list) => {
        const index = _.findIndex(putObjArr, (pObj) => { return pObj.user.id === list.sidId; });
        if(index === -1){
          const dObj = _.find(this.sharedUserList, (sharedUser) => { return sharedUser.id === list.sidId;});
          const data = this.generatUserAccessList([list],[dObj]);
          deletedObjArr.push(data[0]);
        }
      });
      putData = this.generetOutPutObj(putObjArr,'put');
    }

    if(deletedObjArr.length){
      deleteData = this.generetOutPutObj(deletedObjArr,'delete');
    }

    let promiseArr=[];

    _.map(postData, (data) => {
      promiseArr.push(UserRoleREST.postACL({body : JSON.stringify(data)}));
    });

    _.map(putData , (d) => {
      promiseArr.push(UserRoleREST.putACL(d.id, {body : JSON.stringify(d)}));
    });

    _.map(deleteData, (dLData) => {
      promiseArr.push(UserRoleREST.deleteACL(dLData.id, {body : JSON.stringify(dLData)}));
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
                      <label>INVITE USERS</label>
                    </OverlayTrigger>
                  </div>
                </div>
                <div className="row">
                  <div className="col-sm-9">
                    <Select placeholder="Enter Names" value={selectedUsers} options={userList} onChange={this.handleUserChange.bind(this)} multi={true} required={true}  valueKey="name" labelKey="name"  clearable={false} />
                  </div>
                  <div className="col-sm-3">
                    <Select value={selectedUserAccess} options={accessList}  onChange={this.handleUserCanEdit.bind(this,'user',null,null)} required={true}  valueKey="value" labelKey="value" clearable={false} />
                  </div>
                  {
                    accessUserList.length !== 0 && !accessControl
                    ? <span style={{marginLeft:10,fontSize:13}}>
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
                            <h6>ACCESS CONTROL</h6>
                          </div>
                        </div>
                      </div>
                      {
                        _.map(accessUserList , (auList ,i) => {
                          return  <div key={i} className="form-group">
                                    <div className="row">
                                      <div className="col-sm-8">
                                        <label style={{textTransform : "none", fontSize : 14}}>{auList.user.name}</label>
                                      </div>
                                      <div className="col-sm-3 accessControl">
                                        <DropdownButton title={auList.accessControl.value}  id="accessControl" className="dropdown-toggle" bsStyle="link">
                                          <MenuItem active={auList.accessControl.value === "Can Edit" ? true : false} title="Can Edit" onClick={this.handleUserCanEdit.bind(this ,'userAccess','Can Edit',i,auList)}>
                                            Can Edit
                                          </MenuItem>
                                          <MenuItem active={auList.accessControl.value === "Can View" ? true : false} title="Can View" onClick={this.handleUserCanEdit.bind(this ,'userAccess','Can View',i,auList)}>
                                            Can View
                                          </MenuItem>
                                        </DropdownButton>
                                      </div>
                                      <div className="col-sm-1">
                                        <a href="javascript:void(0)" className="crossIcon" onClick={this.deleteAccessUserControl.bind(this, i)}><i className="fa fa-times"></i></a>
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
