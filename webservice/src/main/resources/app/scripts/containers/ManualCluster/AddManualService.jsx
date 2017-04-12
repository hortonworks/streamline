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
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import Select from 'react-select';
import Form from '../../libs/form';
import Utils from '../../utils/Utils';

class AddManualService extends Component{
  constructor(props){
    super(props);
    this.state = {
      formData: {},
      entities : [],
      fetchLoader : true,
      selectedObj: {},
      serviceFormFields : [],
      fileCollection : [],
      fileNameArr :[]
    };
    this.fetchData();
  }

  /*
    fetchData method id called fomr constructor
    To fetch the serviceBundle from a API to popullate the services for a particular CLuster
    response from the server "serviceBundle.entities"
    if the user has open a again to Add service the previous service which has been added before is
    filter from the "serviceBundle.entities" on the bases of "this.props.serviceNameList"
  */
  fetchData = () => {
    ManualClusterREST.getAllServiceBundleList().then((serviceBundle) => {
      if(serviceBundle.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={serviceBundle.responseMessage}/>, '', toastOpt);
      }else {
        const entities = _.filter(serviceBundle.entities, (service) => {
          const serviceIndex = _.findIndex(this.props.serviceNameList, {name : service.name});
          if(serviceIndex === -1){
            return service;
          }
        });
        this.setState({entities:entities, fetchLoader : false});
      }
    });
  }

  /*
    handleServiceChange accept obj selected from the select2
    selectedObj and serviceFormFields is SET
  */
  handleServiceChange = (obj) => {
    if(obj){
      this.setState({selectedObj : obj, serviceFormFields: obj.serviceUISpecification.fields});
    }
  }

  /*
    validate
    The dynamic form has been use in this AddManualService
    So we check the error by calling the "this.refs.Form.validate" & selectedObj.name is not empty
    return true or false
  */
  validate = () => {
    let validDataFlag = false;
    const {selectedObj} = this.state;
    if (this.refs.Form.validate() && selectedObj.name !== '') {
      validDataFlag = true;
    }
    return validDataFlag;
  }

  /*
    fetchFileData accept two arguments file and fileName
    fetchFileData is callback used in Form to listen the onchange on inputType "file"
    and value return is a file object ,fileName
    fileName is used while saving the formData
  */
  fetchFileData = (file,fieldName) => {
    let tempFileNameArr = _.cloneDeep(this.state.fileNameArr);
    let fileArr = _.cloneDeep(this.state.fileCollection);
    const _index = _.findIndex(this.state.fileCollection,{name : file.name});
    if(_index === -1){
      fileArr.push(file);
      tempFileNameArr.push(fieldName);
    }
    this.setState({fileCollection : fileArr , fileNameArr : tempFileNameArr});
  }

  /*
    handleSave method
    The new FormData is created for sending a data
    _formData is append in properties obj using a config key
    And file has been append by fileName as key and file object
  */
  handleSave = () => {
    const {selectedObj,fileCollection,fileNameArr} = this.state;
    const {mClusterId} = this.props;
    let _formData = _.cloneDeep(this.state.formData),tempFormData = {};
    let obj = {},formData = new FormData();
    if(_.keys(_formData).length > 0){
      obj.properties = _formData;
      formData.append('config',JSON.stringify(obj));
    }

    if(fileCollection.length > 0){
      _.map(fileCollection, (file ,i) => {
        formData.append(fileNameArr[i] , file);
      });
    }
    return ManualClusterREST.postRegisterService(mClusterId,selectedObj.name,{body : formData});
  }

  render(){
    const {entities,fetchLoader,selectedObj,serviceFormFields,formData} = this.state;
    let fields = Utils.genFields(serviceFormFields, [], formData);
    return(
      <div className="modal-form config-modal-form" ref="modelForm">
        {
          fetchLoader
          ? <div className="col-sm-12">
              <div className="loading-img text-center" style={{
                marginTop: "100px"
              }}>
                <img src="styles/img/start-loader.gif" alt="loading"/>
              </div>
            </div>
          : <div>
              <div className="customFormClass" >
                <div className="form-group">
                  <label>Select Service
                    <span className="text-danger">*</span>
                  </label>
                  <Select value={selectedObj} options={entities} required={true} valueKey="name" labelKey="name" onChange={this.handleServiceChange} />
                </div>
              </div>
              {
                serviceFormFields.length > 0
                ? <Form ref="Form" className="customFormClass" FormData={formData} fetchFileData={this.fetchFileData} showRequired={null}>
                    {fields}
                  </Form>
                : ''
              }
            </div>
        }
      </div>
    );
  }
}

export default AddManualService;
