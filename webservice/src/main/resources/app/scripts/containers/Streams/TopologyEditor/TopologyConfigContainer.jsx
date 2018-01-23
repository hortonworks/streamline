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
import PropTypes from 'prop-types';
import ReactDOM, {findDOMNode} from 'react-dom';
import {Link} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import ClusterREST from '../../../rest/ClusterREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import Utils from '../../../utils/Utils';
import Form from '../../../libs/form';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';
import {Tabs, Tab} from 'react-bootstrap';
import {Scrollbars} from 'react-custom-scrollbars';

export default class TopologyConfigContainer extends Component {
  static propTypes = {
    topologyId: PropTypes.string.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {
      formData: {},
      formField: {},
      fetchLoader: true,
      activeTabKey: 1,
      hasSecurity: false,
      advancedField : [{
        fieldName : '',
        fieldValue : ''
      }],
      clustersArr: []
    };
    this.fetchData();
  }

  fetchData = () => {
    const {topologyId, versionId} = this.props;
    let promiseArr = [
      TopologyREST.getTopologyWithoutMetrics(topologyId, versionId),
      ClusterREST.getAllCluster()
    ];
    Promise.all(promiseArr).then(result => {
      const formField = JSON.parse(JSON.stringify(this.props.uiConfigFields.topologyComponentUISpecification));
      const config = result[0].config;
      const {f_Data,adv_Field}= this.fetchAdvanedField(formField,JSON.parse(config));
      this.namespaceId = result[0].namespaceId;
      const clustersConfig = result[1].entities;
      EnvironmentREST.getNameSpace(this.namespaceId)
        .then((r)=>{
          let mappings = r.mappings.filter((c)=>{
            return c.namespaceId === this.namespaceId && (c.serviceName === 'HBASE' || c.serviceName === 'HDFS' || c.serviceName === 'HIVE');
          });
          let clusters = [];
          mappings.map((m)=>{
            let c = clustersConfig.find((o)=>{return o.cluster.id === m.clusterId;});
            const obj = {
              id: c.cluster.id,
              fieldName: c.cluster.name + '@#$' + c.cluster.ambariImportUrl,
              uiName: c.cluster.name
            };
            clusters.push(obj);
          });
          clusters = _.uniqBy(clusters, 'id');
          let securityFields = _.find(formField.fields, {"fieldName": "clustersSecurityConfig"}).fields;
          if(securityFields){
            let fieldObj = _.find(securityFields, {"fieldName": "clusterId"});
            if(fieldObj){
              fieldObj.options = clusters;
              fieldObj.type = 'CustomEnumstring';
            }
          }
          /*
            setting value of cluster name in form data from corresponding id
          */
          if(f_Data.clustersSecurityConfig) {
            f_Data.clustersSecurityConfig.map((c)=>{
              if(c.clusterId) {
                c.id = c.clusterId;
                let clusterObj = clusters.find((o)=>{return o.id === c.clusterId;});
                if(clusterObj !== undefined){
                  c.clusterId = clusterObj.uiName;
                }
              }
            });
          }
          this.setState({formData: f_Data, formField: formField, fetchLoader: false,advancedField : adv_Field, clustersArr: clusters});
          let mapObj = r.mappings.find((m) => {
            return m.serviceName.toLowerCase() === 'storm';
          });
          if (mapObj) {
            var stormClusterId = mapObj.clusterId;
            let hasSecurity = false, principalsArr = [], keyTabsArr = [];
            ClusterREST.getStormSecurityDetails(stormClusterId)
              .then((entity)=>{
                if(entity.responseMessage !== undefined) {
                  let msg = entity.responseMessage.indexOf('StreamlinePrincipal') !== -1
                  ? " Please contact admin to get access for this application's services."
                  : entity.responseMessage;
                  FSReactToastr.error(<CommonNotification flag="error" content={msg}/>, '', toastOpt);
                } else {
                  if(entity.security.authentication.enabled) {
                    hasSecurity = true;
                  }
                  let stormPrincipals = (entity.security.principals && entity.security.principals["storm"]) || [];
                  stormPrincipals.map((o)=>{
                    principalsArr.push(o.name);
                  });

                  _.keys(entity.security.keytabs).map((kt)=>{
                    keyTabsArr.push(entity.security.keytabs[kt]);
                  });

                  let principalFieldObj = _.find(securityFields, {"fieldName": "principal"});
                  principalFieldObj.options = principalsArr;

                  let keyTabFieldObj = _.find(securityFields, {"fieldName": "keytabPath"});
                  keyTabFieldObj.options = keyTabsArr;
                }
                //removing security related fields for non-secure mode
                if(hasSecurity === false) {
                  if(formField.fields && formField.fields.length > 0) {
                    formField.fields = _.filter(formField.fields, (f)=>{
                      if(f.hint && f.hint.indexOf('security_') !== -1) {
                        return false;
                      } else {
                        return true;
                      }
                    });
                  }
                } else {
                  let nodes = this.props.topologyNodes.filter((c)=>{
                    return c.currentType.toLowerCase() === 'hbase' || c.currentType.toLowerCase() === 'hdfs' || c.currentType.toLowerCase() === 'hive';
                  });
                  if(nodes.length == 0) {
                    let nameField = _.find(securityFields, {"fieldName": "clusterId"});
                    nameField.isOptional = true;

                    let principalField = _.find(securityFields, {"fieldName": "principal"});
                    principalField.isOptional = true;

                    let keyTabField = _.find(securityFields, {"fieldName": "keytabPath"});
                    keyTabField.isOptional = true;
                  }
                }
                this.setState({hasSecurity: hasSecurity, formField: formField});
              });
          }
        });
    }).catch(err => {
      this.setState({fetchLoader: false});
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  fetchAdvanedField = (formField,config) => {
    let f_Data = {}, adv_Field = [];
    _.map(_.keys(config), (key) => {
      const index = _.findIndex(formField.fields, (fd) => { return fd.fieldName === key;});
      if(index !== -1){
        f_Data[key] = config[key];
      } else {
        adv_Field.push({fieldName : key , fieldValue : config[key] instanceof Array ? JSON.stringify(config[key]) : config[key]});
      }
    });
    if(adv_Field.length === 0){
      adv_Field.push({
        fieldName : '',
        fieldValue : ''
      });
    }

    return {f_Data,adv_Field};
  }

  populateClusterFields(val) {
    if(val) {
      const name = val.split('@#$')[0];
      let clusterSecurityConfig = this.refs.Form.state.FormData["clustersSecurityConfig"];
      let i = clusterSecurityConfig.findIndex((c)=>{return c.clusterId && c.clusterId.indexOf('@#$') > -1;});
      clusterSecurityConfig[i].clusterId = name;
      let obj = this.state.clustersArr.find((o)=>{return o.fieldName === val;});
      clusterSecurityConfig[i].id = obj.id;
      this.setState({formData: this.refs.Form.state.FormData});
    }
  }

  validate() {
    let validDataFlag = false,validateError=[];
    const {advancedField} = this.state;
    if (!this.state.fetchLoader) {
      const {isFormValid, invalidFields} = this.refs.Form.validate();
      if (isFormValid) {
        validDataFlag = true;
      } else {
        const invalidField = invalidFields[0];

        if(invalidField.props.fieldJson.isOptional === false
            && invalidField.props.fieldJson.hint
            && invalidField.props.fieldJson.hint.indexOf('security_') > -1){
          this.setState({
            activeTabKey: 3,
            showRequired: false,
            showSecurity: true
          });
        }else if(invalidField.props.fieldJson.isOptional === false){
          this.setState({
            activeTabKey: 1,
            showRequired: true,
            showSecurity: false
          });
        }
      }
      if(advancedField.length > 1){
        _.map(advancedField, (adv) => {
          if((adv.fieldName !== '' && adv.fieldValue === '') || (adv.fieldName === '' && adv.fieldValue !== '')){
            validateError.push(false);
          }
        });
      }
    }
    return validDataFlag && validateError.length === 0 ? true : false;
  }

  checkAdvancedField = (fields) => {
    let merge = true;
    _.map(fields, (field) => {
      if(field.fieldName === '' || field.fieldValue === ''){
        merge = false;
      }
    });
    return merge;
  }

  generateOutputFields = (fieldList) => {
    let mergeData = {};
    _.map(fieldList,(fd) => {
      mergeData[fd.fieldName] =  Utils.checkTypeAndReturnValue(fd.fieldValue);
    });
    return mergeData;
  }

  handleSave() {
    const {topologyName, topologyId, versionId} = this.props;
    const {advancedField} = this.state;
    let data = _.cloneDeep(this.refs.Form.state.FormData);
    // check if advancedField doesn't has empty field and ready to mergeData
    if(this.checkAdvancedField(advancedField)){
      data = Utils.deepmerge(data , this.generateOutputFields(advancedField));
    }

    if(data.clustersSecurityConfig) {
      data.clustersSecurityConfig.map((c, i)=>{
        if(c.clusterId) {
          c.clusterId = c.id;
        } else {
          delete c.clusterId;
        }
        delete c.id;
      });
    }

    let dataObj = {
      name: topologyName,
      config: JSON.stringify(data),
      namespaceId: this.namespaceId
    };
    return TopologyREST.putTopology(topologyId, versionId, {body: JSON.stringify(dataObj)});
  }

  onSelectTab = (eventKey) => {
    if (eventKey == 1) {
      this.setState({activeTabKey: 1});
    } else if (eventKey == 2) {
      this.setState({activeTabKey: 2});
    } else if (eventKey == 3) {
      this.setState({activeTabKey: 3});
    }
  }

  advanedFieldChange = (fieldType,index,event) => {
    let tempField = _.cloneDeep(this.state.advancedField);
    const val = event.target.value;
    tempField[index][fieldType] = val;
    this.setState({advancedField : tempField});
  }

  addAdvancedRowField = () => {
    let tempField = _.cloneDeep(this.state.advancedField);
    tempField.push({fieldName : '',fieldValue : ''});
    this.setState({advancedField : tempField});
  }

  deleteAdvancedRowField = (index) => {
    let tempField = _.cloneDeep(this.state.advancedField);
    tempField.splice(index,1);
    this.setState({advancedField : tempField});
  }

  render() {
    const {formData, formField, fetchLoader,advancedField} = this.state;
    let fields = Utils.genFields(formField.fields || [], [], formData);
    const disabledFields = this.props.testRunActivated ? true : false;
    return (
      <div>
        {fetchLoader
          ? <div className="col-sm-12">
              <div className="loading-img text-center" style={{
                marginTop: "150px"
              }}>
                <img src="styles/img/start-loader.gif" alt="loading"/>
              </div>
            </div>
          : <Tabs id="ConfigForm" activeKey={this.state.activeTabKey} className="modal-tabs" onSelect={this.onSelectTab}>
              <Tab eventKey={1} title="GENERAL">
                <div className="source-modal-form" style={{width : 580}}>
                  <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
                    display: "none"
                  }}/>}>
                    <Form ref="Form" FormData={formData} readOnly={disabledFields} showRequired={null} className="modal-form config-modal-form">
                      {fields}
                    </Form>
                  </Scrollbars>
                </div>
              </Tab>
              {
              this.state.hasSecurity ?
              <Tab eventKey={3} title="SECURITY">
                <div className="source-modal-form" style={{width : 580}}>
                  <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
                    display: "none"
                  }}/>}>
                    <Form ref="Form" FormData={formData} readOnly={disabledFields} showRequired={null} showSecurity={true} populateClusterFields={this.populateClusterFields.bind(this)} className="modal-form config-modal-form">
                      {fields}
                    </Form>
                  </Scrollbars>
                </div>
              </Tab>
              : ''
              }
              <Tab eventKey={2} title="ADVANCED">
                <form className="source-modal-form" style={{width : 580}}>
                  <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
                    display: "none"
                  }}/>}>
                    <div className="row">
                      <div className="col-sm-5">
                        <div className="form-group">
                          <label>Field Name
                            <span className="text-danger">*</span>
                          </label>
                        </div>
                      </div>
                      <div className="col-sm-5">
                        <div className="form-group">
                          <label>Field Value
                            <span className="text-danger">*</span>
                          </label>
                        </div>
                      </div>
                    </div>
                    {
                      _.map(advancedField , (adv,i) => {
                        return  <div className="row" key={i}>
                                  <div className="col-sm-5">
                                    <div className="form-group">
                                      <input type="text" value={adv.fieldName} className="form-control" onChange={this.advanedFieldChange.bind(this,'fieldName',i)} />
                                    </div>
                                  </div>
                                  <div className="col-sm-5">
                                    <div className="form-group">
                                      <input type="text" value={adv.fieldValue} className="form-control" onChange={this.advanedFieldChange.bind(this,'fieldValue',i)} />
                                    </div>
                                  </div>
                                  {!this.props.testRunActivated
                                    ? <div className="col-sm-2">
                                        <button className="btn btn-default btn-sm" disabled={disabledFields} type="button" onClick={this.addAdvancedRowField.bind(this)}>
                                          <i className="fa fa-plus"></i>
                                        </button>&nbsp; {i > 0
                                          ? <button className="btn btn-sm btn-danger" type="button" onClick={this.deleteAdvancedRowField.bind(this, i)}>
                                              <i className="fa fa-trash"></i>
                                            </button>
                                          : null}
                                      </div>
                                    : null}
                                </div>;
                      })
                    }
                  </Scrollbars>
                </form>
              </Tab>
            </Tabs>
}
      </div>
    );
  }
}
