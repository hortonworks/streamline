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
import FSReactToastr from './FSReactToastr';
import CommonNotification from '../utils/CommonNotification';
import {toastOpt} from '../utils/Constants';
import ClusterREST from '../rest/ClusterREST';
import EnvironmentREST from '../rest/EnvironmentREST';
import {Confirm} from '../components/FSModal';

const ItemsMapping = (props) => {
  const {item, itemClicked} = props;
  let name = item.name.replace('_', ' ');
  return (
    <li>
      <div><img onClick={itemClicked} data-id={`${item.clusterId}@${item.name}`} data-service={item.name} className='' src={`styles/img/icon-${item.name.toLowerCase()}.png`}/></div>
      {name}
    </li>
  );
};

class AddEnvironmentItems extends Component {
  constructor(props) {
    super(props);
  }
  onItemClicked = (e) => {
    const {mapSelection} = this.props;
    const targetEl = e.target;
    const params = targetEl.attributes["data-id"].value.split('@');
    const classStr = targetEl.attributes["class"].value.trim();

    (classStr.length === 0)
      ? targetEl.setAttribute("class", "activeImg")
      : targetEl.setAttribute("class", "");

    const obj = {
      clusterId: Number(params[0]),
      serviceName: params[1].toString()
    };
    mapSelection(obj);
  }

  render() {
    const {clusterList} = this.props;
    const {cluster, services} = clusterList;
    const tempArr = services || [
      {
        service: (services === undefined)
          ? ''
          : services.service
      }
    ];
    let serviceWrap = [];
    let t = [];
    tempArr.map((s) => {
      if (s.service.name.length > 8) {
        t.push(s);
      } else {
        serviceWrap.push(s);
      }
    });
    Array.prototype.push.apply(serviceWrap, t);
    return (
      <div className="col-md-4">
        <div className="environment-modal-widget">
          <h5 className="environment-title no-margin-top">{cluster.name}<br/>
            <span>{cluster.ambariImportUrl}</span>
          </h5>
          <ul className="select-env-service clearfix">
            {serviceWrap.length === 0
              ? <div className="col-sm-12 text-center">
                  No Service
                </div>
              : serviceWrap.map((item, i) => {
                return <ItemsMapping key={item.service.id} item={item.service} itemClicked={this.onItemClicked}/>;
              })
}
          </ul>
        </div>
      </div>
    );
  }
}

class AddEnvironment extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      fetchLoader: true,
      hadoopService:{}
    };
    this.multipleHadoopService = false;
    this.selectionList = {};
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [ClusterREST.getAllCluster()];
    if (this.props.namespaceId) {
      promiseArr.push(EnvironmentREST.getNameSpace(this.props.namespaceId));
    }

    Promise.all(promiseArr).then(results => {
      results.map((result, i) => {
        if (result.responseMessage !== undefined) {
          this.setState({fetchLoader: false});
          FSReactToastr.error(
            <CommonNotification flag="error" content={result.responseMessage}/>, '', toastOpt);
        } else {

          if (i === 0) {
            result.entities.map(x => {
              if (this.selectionList[x.cluster.id] === undefined) {
                this.selectionList[x.cluster.id] = [];
              }
            });
            this.setState({fetchLoader: false, entities: result.entities});
          } else if (i === 1) {
            let hadoopService={};
            this.nameRef.value = result.namespace.name;
            this.descRef.value = result.namespace.description;
            result.mappings.map((o) => {
              const sName = o.serviceName.toLowerCase();
              if(sName === 'hive' || sName === 'hbase' || sName === 'hdfs'){
                hadoopService[o.clusterId] === undefined
                ? hadoopService[o.clusterId] = [sName]
                : hadoopService[o.clusterId].push(sName);
              }
              document.querySelector('[data-id="' + o.clusterId + '@' + o.serviceName + '"]').className = "activeImg";
              this.mapSelectionHandler(o);
            });
            this.setState({hadoopService});
          }
        }
      });
    }).catch((err) => {
      this.setState({fetchLoader: false});
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  validation = () => {
    const formNodes = this.refs.addEvtModelRef.children;
    let errArr = [];
    if (this.nameRef.value.trim() == '') {
      errArr.push("error");
      this.nameRef.setAttribute('class', "form-control invalidInput");
    } else {
      this.nameRef.setAttribute('class', "form-control");
    }

    if (this.descRef.value.trim() == '') {
      errArr.push("error");
      this.descRef.setAttribute('class', "form-control invalidInput");
    } else {
      this.descRef.setAttribute('class', "form-control");
    }

    let missingStorm = true;
    _.keys(this.selectionList).map(key => {
      this.selectionList[key].map((o) => {
        if (o.serviceName.toLowerCase() === 'storm') {
          missingStorm = false;
        }
      });
    });
    if (this.refs.missingStorm != undefined) {
      if (missingStorm) {
        this.refs.missingStorm.className = 'text-danger';
        errArr.push("error");
      } else {
        this.refs.missingStorm.className = '';
      }
    } else {
      errArr.push("error");
    }
    return (errArr.length === 0)
      ? true
      : false;
  }

  handleSave = () => {
    if (this.validation()) {
      const evt_name = this.nameRef.value.trim();
      const desc = this.descRef.value.trim();

      // remove the pushId from the selectionList
      let tempData = _.keys(this.selectionList).map(key => {
        return this.selectionList[key].map(x => {
          // delete x['serviceId'];
          return x;
        });
      });
      const obj = {
        name: evt_name,
        description: desc,
        streamingEngine: 'STORM'
      };
      tempData = _.flatten(tempData);
      if (tempData.find((o) => {
        return o.serviceName.toLowerCase() == 'ambari_metrics';
      })) {
        obj['timeSeriesDB'] = 'AMBARI_METRICS';
      }
      if (tempData.find((o) => {
        return o.serviceName.toLowerCase() == 'ambari_infra';
      })) {
        obj['logSearchService'] = 'AMBARI_INFRA';
      }
      return {obj, tempData};
    }
  }

  mapSelectionHandler = (dataObj) => {
    let stateObj={};
    stateObj.hadoopService = _.cloneDeep(this.state.hadoopService);
    const index = this.selectionList[dataObj.clusterId].findIndex((x) => {
      return x.serviceName == dataObj.serviceName;
    });

    if (index !== -1) {
      this.selectionList[dataObj.clusterId].splice(index, 1);
      if(!_.isEmpty(stateObj.hadoopService)){
        let ck = _.keys(stateObj.hadoopService);
        if(Number(ck[0]) === dataObj.clusterId && stateObj.hadoopService[ck[0]].length > 1){
          const sIndex = _.findIndex(stateObj.hadoopService[ck[0]], (h) => {return h === dataObj.serviceName.toLowerCase();});
          if(sIndex !== -1){
            stateObj.hadoopService[ck[0]].splice(sIndex,1);
          }
        } else if(Number(ck[0]) === dataObj.clusterId && stateObj.hadoopService[ck[0]].length === 1){
          stateObj.hadoopService = {};
        }
        this.setState(stateObj);
      }
    } else {
      if (dataObj.serviceName.toLowerCase() === "storm" || dataObj.serviceName.toLowerCase() === "ambari_metrics") {
        let t_clusterId,
          t_index,
          storm_id = '';
        _.keys(this.selectionList).map(key => {
          this.selectionList[key].map((x, i) => {
            if (x.serviceName.toLowerCase() === "storm" || x.serviceName.toLowerCase() === "ambari_metrics") {
              x.serviceName.toLowerCase() === "storm"
                ? storm_id = key
                : '';
              t_clusterId = key;
              t_index = i;
            }
          });
        });
        if (t_clusterId !== undefined) {
          let r_index = 0,
            r_id = '',
            r_flag = true;
          _.keys(this.selectionList).map(key => {
            r_index = this.selectionList[key].findIndex(x => {
              r_id = x.clusterId;
              return x.serviceName === dataObj.serviceName;
            });
            if (r_index !== -1 && r_flag) {
              if (dataObj.serviceName.toLowerCase() === "storm") {
                this.refs.changeStreamEngine.show({title: 'Are you sure you want to change storm for this environment?'}).then(() => {
                  this.sliceSelectedKeyIndex(storm_id, dataObj.serviceName);
                }, () => {
                  this.sliceSelectedKeyIndex(dataObj.clusterId, dataObj.serviceName);
                });
              } else {
                this.sliceSelectedKeyIndex(r_id, dataObj.serviceName);
              }
              r_flag = false;
            }
          });
        }
      } else if(dataObj.serviceName.toLowerCase() === "hive" || dataObj.serviceName.toLowerCase() === "hbase" || dataObj.serviceName.toLowerCase() === 'hdfs'){
        this.multipleHadoopService = false;
        const s_name = dataObj.serviceName.toLowerCase();
        if(_.isEmpty(stateObj.hadoopService)){
          stateObj.hadoopService[dataObj.clusterId] = [s_name];
          if(s_name !== 'hdfs'){
            this.pushHdfsIntoSelectionList(dataObj);
          }
        } else {
          const clusterKey = _.keys(stateObj.hadoopService);
          if(Number(clusterKey[0]) === dataObj.clusterId){
            stateObj.hadoopService[dataObj.clusterId].push(s_name);
            if(s_name !== 'hdfs'){
              this.pushHdfsIntoSelectionList(dataObj);
            }
          } else {
            document.querySelector('[data-id="' + dataObj.clusterId + '@' + dataObj.serviceName + '"]').className = "";
            this.multipleHadoopService = true;
          }
        }


      }
      !this.multipleHadoopService
        ? this.selectionList[dataObj.clusterId].push(dataObj)
        : '';
      this.setState(stateObj);
    }
  }

  pushHdfsIntoSelectionList = (dataObj) => {
    const obj = {
      clusterId : dataObj.clusterId,
      serviceName : 'HDFS'
    };
    document.querySelector('[data-id="' + dataObj.clusterId + '@' + 'HDFS' + '"]').className = "activeImg";
    this.selectionList[dataObj.clusterId].push(obj);
  }

  sliceSelectedKeyIndex = (r_serviceId, r_serviceName) => {
    document.querySelector('[data-id="' + r_serviceId + '@' + r_serviceName + '"]').className = "";
    const cancelIndex = this.selectionList[r_serviceId].findIndex((x) => {
      return x.serviceName == r_serviceName;
    });
    cancelIndex !== -1
      ? this.selectionList[r_serviceId].splice(cancelIndex, 1)
      : '';
    this.refs.changeStreamEngine.hide();
  }

  componentWillUnmount() {
    this.selectionList = [];
  }

  render() {
    const {fetchLoader, entities} = this.state;
    return (
      <div>
        <div className="modal-form config-modal-form" ref="addEvtModelRef">
          <div className="form-group">
            <label data-stest="nameLabel">Name
              <span className="text-danger">*</span>
            </label>
            <input type="text" ref={(ref) => this.nameRef = ref} name="environmentName" placeholder="Environment Name" required="true" className="form-control"/>
          </div>
          <div className="form-group">
            <label data-stest="descLabel">Description
              <span className="text-danger">*</span>
            </label>
            <input type="text" ref={(ref) => this.descRef = ref} name="description" placeholder="Description" required="true" className="form-control"/>
          </div>
          <h4 className="environment-modal-title" data-stest="selectServicesLabel">Select Services</h4>
          {entities.length !== 0
            ? <small ref="missingStorm">
                (Atleast one streaming engine (eg: STORM) must be selected.)</small>
            : ''
}
          {
            this.multipleHadoopService
            ? [<br key={1} /> ,<small key={2} className="text-danger">
                ( please select HIVE 'OR' HBASE and HDFS from same cluster.)</small>]
            : null
          }
          <div className="row environment-modal-services">
            {fetchLoader
              ? <div className="col-sm-12">
                  <div className="loading-img text-center">
                    <img src="styles/img/start-loader.gif" alt="loading"/>
                  </div>
                </div>
              : <div>
                {entities.length === 0
                  ? <div className="col-sm-12 text-center">
                      No Clusters
                    </div>
                  : entities.map(list => {
                    return <AddEnvironmentItems key={list.cluster.id} clusterList={list} mapSelection={this.mapSelectionHandler}/>;
                  })
}
              </div>
}

          </div>
        </div>
        <Confirm ref="changeStreamEngine"/>
      </div>
    );
  }
}

export default AddEnvironment;
