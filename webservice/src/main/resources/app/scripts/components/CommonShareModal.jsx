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
import _ from 'lodash';
import {OverlayTrigger, Popover,DropdownButton,MenuItem} from 'react-bootstrap';
import {observer} from 'mobx-react';
import Select from 'react-select';
import UserRoleREST from '../rest/UserRoleREST';
import FSReactToastr from './FSReactToastr';
import {toastOpt} from '../utils/Constants';

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
      tempAccessUserList : [{
        user : '',
        accessControl : { label : "Can Edit", value : 'Can Edit'}
      }],
      accessList : [
        { label : "Can Edit", value : 'Can Edit'},
        { label : "Can View", value : 'Can View'}
      ],
      showUserCaption : '',
      selectedUserList : [],
      userAccessList : []
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

      // remove admin user from the user options
      let tempOptions = _.filter(results[1].entities, (entity) => {
        const role = entity.roles.toString();
        return role.indexOf("ROLE_ADMIN") === -1;
      });

      // const tempOptions = results[1].entities;

      this.userOptions = _.filter(tempOptions, (opt) => {return opt.id !== ownerObj.sidId;});

      stateObj.userAccessList = _.filter(this.userOptions, (user) => {
        return _.find(stateObj.selectedUserList, (list) => {return list.sidId === user.id;});
      });

      this.constSharedUserList = _.cloneDeep(stateObj.userAccessList);

      stateObj.userList = this.filterUserOptions(this.userOptions, stateObj.userAccessList);

      stateObj.showUserCaption = this.getUserListCaption(stateObj.userAccessList);

      stateObj.selectedUserAccess = this.state.accessList[0];
      if(stateObj.userAccessList.length){
        stateObj.tempAccessUserList = this.generatUserAccessList(stateObj.selectedUserList, stateObj.userAccessList);
        stateObj.userAccessList = _.cloneDeep(stateObj.tempAccessUserList);
      } else {
        stateObj.tempAccessUserList = [];
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
        const permission = orgList[index].permissions.toString();
        const p_string = permission.includes('WRITE') ? "Can Edit" : "Can View";
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
    const other = list.length > 2 ? `and ${len} others..    ` : '';
    return `${list.length > 0 ? 'Shared with ' : ''} ${string.join(', ')} ${list.length > 2 ? other : ''}`;
  }

  handleUserChange = (arrList) => {
    const {selectedUserAccess} = this.state;
    let cloneUserAccessList = _.cloneDeep(this.state.userAccessList);
    let tempArr=[],tempAccessUser=[];
    _.map(arrList, (arr,i) => {
      tempArr[i] === undefined
        ? tempArr[i] = {user : '',accessControl : { label : selectedUserAccess.value, value : selectedUserAccess.value}}
        : '';
      tempArr[i].user = arr;
    });

    Array.prototype.push.apply(tempArr,cloneUserAccessList);

    this.setState({selectedUsers : arrList, tempAccessUserList : tempArr});
  }

  handleUserCanEdit = (type,key,index,obj) => {
    if(!_.isEmpty(obj)){
      let invitedUser = _.cloneDeep(this.state.selectedUsers);
      let tempAccessUser = _.cloneDeep(this.state.tempAccessUserList);
      if(type === "user"){
        _.map(invitedUser, (invite) => {
          const _index = _.findIndex(tempAccessUser, (t) => {return t.user.id === invite.id;});
          if(_index !== -1){
            tempAccessUser[_index].accessControl = {label : obj.value , value : obj.value};
          }
        });
        this.setState({selectedUserAccess : obj,tempAccessUserList : tempAccessUser});
      } else {
        tempAccessUser[index].accessControl = {label : key , value : key};
        this.setState({tempAccessUserList : tempAccessUser});
      }
    }
  }

  changeAccess = () => {
    this.setState({accessControl : true});
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

  getTempUserAccessObj = (tempAccessList,list) => {
    let obj=[];
    _.map(tempAccessList, (accesslist) => {
      const index = _.findIndex(list , (l) => { return l.id === accesslist.user.id;});
      if(index !== -1){
        obj.push(accesslist);
      }
    });
    return obj;
  }

  getSharedObj = (selectedUserList,list) => {
    return _.find(selectedUserList, (sharedUser) => { return sharedUser.sidId === list.id;});
  }

  generateOutputFields = (constList,sharedList) => {
    let arr=[];
    _.map(constList, (list) => {
      const dObj =  this.getSharedObj(sharedList,list);
      const data = this.generatUserAccessList([dObj],[list]);
      arr.push(data[0]);
    });
    return arr;
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

  handleSave = () => {
    let postData=[],putData=[],deleteData=[],deletedObjArr=[];
    const {selectedUsers,selectedUserList,tempAccessUserList} = this.state;
    let putObjArr=[];
    // this postObjArr is for POST
    let {postList,putList} = this.splitPostUser(tempAccessUserList,selectedUsers);

    let postObjArr = postList;

    Array.prototype.push.apply(putObjArr, putList);

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
      _.map(putObjArr, (pObj) => {
        const index = _.findIndex(selectedUserList, (list) => { return list.sidId === pObj.user.id; });
        if(index === -1){
          const dObj = _.find(this.constSharedUserList, (sharedUser) => { return sharedUser.id === list.sidId;});
          const data = this.generatUserAccessList([pObj],[dObj]);
          deletedObjArr.push(data[0]);
        }
      });
      putData = this.generetOutPutObj(putObjArr,'put');
    }

    if(deletedObjArr.length){
      deleteData = this.generetOutPutObj(deletedObjArr,'delete');
    }

    if(postObjArr.length === 0 && putObjArr.length === 0 && deletedObjArr.length === 0 && this.constSharedUserList.length !== 0){
      const arr = this.generateOutputFields(this.constSharedUserList,selectedUserList);
      deleteData = this.generetOutPutObj(arr,'delete');
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
    let selectedUserArr = _.cloneDeep(this.state.selectedUsers);
    let cloneUserAccessList = _.cloneDeep(this.state.userAccessList);
    let tempAccessUser = _.cloneDeep(this.state.tempAccessUserList);
    let options = _.cloneDeep(this.state.userList);

    const tempUser = tempAccessUser[index].user;

    const userIndex = _.findIndex(selectedUserArr, (selectUser) => {return selectUser.id === tempUser.id;});

    if(userIndex !== -1){
      selectedUserArr.splice(userIndex , 1);
    } else {
      const cIndex = _.findIndex(cloneUserAccessList, (c) => {return c.user.id === tempUser.id;});
      if(cIndex !== -1){
        cloneUserAccessList.splice(cIndex,1);
      }
      options.push(tempUser);
    }

    tempAccessUser.splice(index, 1);
    this.setState({selectedUsers:selectedUserArr,userAccessList:cloneUserAccessList,tempAccessUserList:tempAccessUser,userList:options});
  }


  render(){
    const {showLoading,accessControl,userList,selectedUsers,selectedUserAccess,accessList,tempAccessUserList,showUserCaption} = this.state;

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
                    tempAccessUserList.length !== 0 && !accessControl
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
                        _.map(tempAccessUserList , (auList ,i) => {
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
                        tempAccessUserList.length === 0
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
