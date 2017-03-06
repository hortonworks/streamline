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
import {Link} from 'react-router';
import moment from 'moment';
import {DropdownButton, MenuItem, Button} from 'react-bootstrap';
import Modal from '../../components/FSModal';
import {Scrollbars} from 'react-custom-scrollbars';

/* import common utils*/
import ClusterREST from '../../rest/ClusterREST';
import Utils from '../../utils/Utils';
import TopologyUtils from '../../utils/TopologyUtils';
import FSReactToastr from '../../components/FSReactToastr';

/* component import */
import BaseContainer from '../BaseContainer';
import NoData from '../../components/NoData';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import Paginate from '../../components/Paginate';
import CommonLoaderSign from '../../components/CommonLoaderSign';

const ServiceItems = (props) => {
  const {item} = props;
  let name = item.name.replace('_', ' ');
  return (
    <li><img src={`styles/img/icon-${item.name.toLowerCase()}.png`}/>{name}</li>
  );
};

class PoolItemsCard extends Component {
  constructor(props) {
    super(props);
  }

  onActionClick = (eventKey) => {
    this.props.poolActionClicked(eventKey, this.clusterRef.dataset.id);
  }

  checkRefId = (id) => {
    const index = this.props.refIdArr.findIndex((x) => {
      return x === id;
    });
    return index !== -1
      ? true
      : false;
  }

  render() {
    const {clusterList, loader} = this.props;
    const {cluster, services} = clusterList;
    const tempArr = services || {
      service: (services === undefined)
        ? ''
        : services.service
    };
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
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;

    return (
      <div className="col-md-4">
        <div className="service-box" data-id={cluster.id} ref={(ref) => this.clusterRef = ref}>
          <div className="service-head clearfix">
            <h4 className="pull-left no-margin">{cluster.name}<br/>
              <span>{cluster.ambariImportUrl}</span>
            </h4>
            <div className="pull-right">
              <DropdownButton noCaret title={ellipseIcon} id="dropdown" bsStyle="link" className="dropdown-toggle" data-stest="service-pool-actions">
                <MenuItem onClick={this.onActionClick.bind(this, "refresh/")} data-stest="edit-service-pool">
                  <i className="fa fa-refresh"></i>
                  &nbsp;Refresh
                </MenuItem>
                <MenuItem onClick={this.onActionClick.bind(this, "delete/")} data-stest="delete-service-pool">
                  <i className="fa fa-trash"></i>
                  &nbsp;Delete
                </MenuItem>
              </DropdownButton>
            </div>
          </div>
          <div className="service-body clearfix">
            {(this.checkRefId(cluster.id))
              ? <div className="service-components">
                  <div className="loading-img text-center">
                    <img src="styles/img/start-loader.gif" alt="loading"/>
                  </div>
                </div>
              : <Scrollbars style={{
                height: "185px"
              }} autoHide renderThumbHorizontal={props => <div {...props} style={{
                display: "none"
              }}/>}>
                <ul className="service-components ">
                  {serviceWrap.length !== 0
                    ? serviceWrap.map((items, i) => {
                      return <ServiceItems key={i} item={items.service}/>;
                    })
                    : <div className="col-sm-12 text-center">
                      No Service
                    </div>
}
                </ul>
              </Scrollbars>
}
          </div>
        </div>
      </div>
    );
  }
}
PoolItemsCard.propTypes = {
  poolActionClicked: React.PropTypes.func.isRequired
};

class ServicePoolContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      loader: false,
      refIdArr: [],
      idCheck: '',
      showFields: false,
      showInputErr: true,
      clusterData: {},
      fetchLoader: true,
      pageIndex: 0,
      pageSize: 6
    };
    this.fetchData();
  }

  fetchData = () => {
    ClusterREST.getAllCluster().then((clusters) => {
      if (clusters.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={clusters.responseMessage}/>, '', toastOpt);
        this.setState({fetchLoader: false});
      } else {
        let result = clusters.entities;
        this.setState({fetchLoader: false, entities: result, pageIndex: 0});
      }
    }).catch((err) => {
      this.setState({fetchLoader: false});
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  componentDidUpdate() {
    this.btnClassChange();
  }
  componentDidMount() {
    this.btnClassChange();
  }
  btnClassChange = () => {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper animated fadeIn ");
  }
  componentWillUnmount() {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper animated fadeIn ");
  }
  addBtnClicked = () => {
    const {showInputErr} = this.state;
    const val = this.refs.addURLInput.value.trim();

    if (Utils.validateURL(val) && val.length !== 0) {
      const name = this.sliceClusterUrl(val);
      const tempObj = Object.assign(this.state.clusterData, {
        clusterName: name,
        ambariUrl: val
      });
      this.setState({
        showInputErr: true,
        clusterData: tempObj
      }, this.adminFormModel.show());
      this.refs.addURLInput.value = '';
    } else {
      this.setState({showInputErr: false});
    }
  }
  sliceClusterUrl = (url) => {
    return url.substr((url.lastIndexOf('/') + 1), url.length);
  }
  poolActionClicked = (eventKey, id) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
    case "refresh":
      this.handleUpdateCluster(id);
      break;
    case "delete":
      this.handleDeleteCluster(id);
      break;
    default:
      break;
    }
  }

  handleUpdateCluster = (ID) => {
    ClusterREST.getCluster(ID).then((entity) => {
      if (entity.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={entity.responseMessage}/>, '', toastOpt);
      } else {
        const url = entity.cluster.ambariImportUrl;
        const tempObj = Object.assign(this.state.clusterData, {ambariUrl: url});
        this.setState({
          showFields: true,
          idCheck: + ID,
          clusterData: tempObj
        }, () => {
          this.adminFormModel.show();
        });
      }
    });

  }

  handleDeleteCluster = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete ?'}).then((confirmBox) => {
      ClusterREST.deleteCluster(id).then((cluster) => {

        confirmBox.cancel();
        if (cluster.responseMessage !== undefined) {
          if (cluster.responseMessage.indexOf('Namespace refers the cluster') !== 1) {
            FSReactToastr.info(
              <CommonNotification flag="info" content={"This cluster is shared with some environment. So it can't be deleted."}/>, '', toastOpt);
          } else {
            FSReactToastr.error(
              <CommonNotification flag="error" content={cluster.responseMessage}/>, '', toastOpt);
          }
        } else {
          this.setState({
            fetchLoader: true
          }, () => {
            this.fetchData();
            clearTimeout(clearTimer);
            const clearTimer = setTimeout(() => {
              FSReactToastr.success(
                <strong>cluster deleted successfully</strong>
              );
            }, 500);
          });
        }
      });
    });
  }

  validateForm = () => {
    const formNodes = this.refs.modelForm.children;
    let errArr = [];
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
      if (errArr.length === 0) {
        if (this.state.idCheck.length !== 0 && this.state.showFields) {
          const url = this.refs.userUrl.value.trim();
          if (!Utils.validateURL(url)) {
            this.refs.userUrl.setAttribute('class', "form-control invalidInput");
            return false;
          }
        }
        this.fetchFormdata();
        return true;
      } else {
        return false;
      }
    };
    return filter(formNodes);
  }

  fetchFormdata = () => {
    let tempCluster = {},
      name = '';
    const {idCheck, clusterData, showFields} = this.state;
    const userName = this.refs.username.value.trim();
    const passWord = this.refs.userpass.value.trim();
    if (idCheck.length !== 0 && showFields) {
      const url = this.refs.userUrl.value.trim();
      name = this.sliceClusterUrl(url);
      tempCluster = Object.assign(clusterData, {
        clusterName: name,
        ambariUrl: url
      });
    }
    tempCluster = Object.assign(clusterData, {
      username: userName,
      password: passWord
    });
    this.setState({clusterData: tempCluster});
  }

  fetchClusterDetail = () => {
    const {clusterData, refIdArr} = this.state;
    const {clusterName, ambariUrl} = clusterData;
    let data = {
      name: clusterName,
      description: "This is an auto generated description",
      ambariImportUrl: ambariUrl
    };
    ClusterREST.postCluster({body: JSON.stringify(data)}).then((cluster) => {
      if (cluster.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={cluster.responseMessage}/>, '', toastOpt);
        this.setState({loader: false});
      } else {
        const id = cluster.id;
        const tempArr = refIdArr;
        tempArr.push(id);
        this.setState({
          refIdArr: tempArr,
          loader: false,
          idCheck: ''
        }, () => {
          this.importAmbariCluster(id);
          this.fetchData();
        });
      }
    });
  }

  adminSaveClicked = () => {
    if (this.validateForm()) {
      const {idCheck, showFields, clusterData} = this.state;
      const {ambariUrl, username, password} = clusterData;
      const urlVerificationData = {
        ambariRestApiRootUrl: ambariUrl,
        username: username,
        password: password
      };

      ClusterREST.postAmbariClusterVerifyURL({body: JSON.stringify(urlVerificationData)}).then(urlVerify => {
        if (urlVerify.verified) {
          (showFields)
            ? this.importAmbariCluster()
            : this.fetchClusterDetail();
          this.adminFormModel.hide();
          this.setState({showFields: false});
        } else {
          let response = urlVerify.responseMessage;
          if (response !== undefined) {
            let errorMsg = response;
            errorMsg = response.indexOf('Cluster not found') !== -1
              ? "Ambari cluster not found"
              : response.indexOf('Unable to sign in') !== -1
                ? "You have entered wrong username or password"
                : response.indexOf('Bad input') !== -1
                  ? "Not valid Ambari API URL"
                  : response;
            FSReactToastr.error(
              <CommonNotification flag="error" content={errorMsg}/>, '', toastOpt);
          }
        }
      });
    }
  }

  importAmbariCluster = (id) => {
    const {clusterData, idCheck, refIdArr} = this.state;
    const {ambariUrl, username, password} = clusterData;
    const clusterID = id;
    const importClusterData = {
      clusterId: clusterID || idCheck,
      ambariRestApiRootUrl: ambariUrl,
      username: username,
      password: password
    };
    const tempArr = refIdArr;
    tempArr.push(idCheck);
    this.setState({refIdArr: tempArr});
    // Post call for import cluster from ambari
    ClusterREST.postAmbariCluster({body: JSON.stringify(importClusterData)}).then((ambarClusters) => {
      let obj = {};
      if (ambarClusters.responseMessage !== undefined) {
        this.setState({
          loader: false,
          idCheck: ''
        }, () => {
          this.fetchData();
          clearTimeout(clearTimer);
          const clearTimer = setTimeout(() => {
            FSReactToastr.error(
              <CommonNotification flag="error" content={errorMsg}/>, '', toastOpt);
          }, 500);
        });
      } else {
        const result = ambarClusters;
        let entitiesWrap = [],
          sucessMsg = '';
        if (idCheck) {
          //Update Single Cluster
          const elPosition = this.state.entities.map(function(x) {
            return x.cluster.id;
          }).indexOf(idCheck);
          entitiesWrap = this.state.entities;
          entitiesWrap[elPosition] = result;
          sucessMsg = "Process completed successfully";
        } else {
          sucessMsg = "Cluster added successfully";
        }
        const tempDataArray = this.spliceTempArr(clusterID || idCheck);

        this.setState({
          fetchLoader: (idCheck)
            ? false
            : true,
          loader: false,
          idCheck: '',
          entities: entitiesWrap,
          refIdArr: tempDataArray
        }, () => {
          sucessMsg.indexOf("added") !== - 1
            ? this.fetchData()
            : '';
          clearTimeout(clearTimer);
          const clearTimer = setTimeout(() => {
            FSReactToastr.success(
              <strong>{sucessMsg}</strong>
            );
          }, 500);
        });
      }
    });
  }

  spliceTempArr = (id) => {
    const tempArr = this.state.refIdArr;
    const index = tempArr.findIndex((x) => {
      return x === id;
    });
    if (index !== -1) {
      tempArr.splice(index, 1);
    }
    return tempArr;
  }

  adminCancelClicked = () => {
    const {idCheck} = this.state;
    const tempDataArray = this.spliceTempArr(idCheck);
    this.setState({idCheck: '', showFields: false, refIdArr: tempDataArray});
    this.adminFormModel.hide();
  }

  pagePosition = (index) => {
    this.setState({
      pageIndex: index || 0
    });
  }
  getHeaderContent() {
    return (
      <span>
        Configuration
        <span className="title-separator">/</span>
        {this.props.routes[this.props.routes.length - 1].name}
      </span>
    );
  }

  handleKeyPress = (event) => {
    if (event.key === "Enter") {
      if (event.target.placeholder.indexOf('http://ambari_host') !== -1) {
        event.target.focus = false;
        this.addBtnClicked();
      }
      this.adminFormModel.state.show
        ? this.adminSaveClicked()
        : '';
    }
  }

  render() {
    const {routes} = this.props;
    const {
      showInputErr,
      entities,
      showFields,
      fetchLoader,
      pageSize,
      pageIndex,
      refIdArr,
      loader,
      clusterData
    } = this.state;
    const {ambariUrl} = clusterData;
    const splitData = _.chunk(entities, pageSize) || [];
    const adminFormFields = () => {
      return <form className="modal-form config-modal-form" ref="modelForm">
        {loader || showFields
          ? <div className="form-group">
              <label>Url<span className="text-danger">*</span>
              </label>
              <input type="text" className="form-control" placeholder="http://ambari_host:port/api/v1/clusters/CLUSTER_NAME" ref="userUrl" autoFocus="true" disabled={true} value={ambariUrl}/>
              <p className="text-danger"></p>
            </div>
          : ''
}
        <div className="form-group">
          <label>UserName<span className="text-danger">*</span>
          </label>
          <input type="text" className="form-control" placeholder="Enter your Name" ref="username" autoFocus={showFields
            ? false
            : true}/>
          <p className="text-danger"></p>
        </div>
        <div className="form-group">
          <label>Password<span className="text-danger">*</span>
          </label>
          <input type="password" className="form-control" placeholder="Enter your Password" ref="userpass"/>
          <p className="text-danger"></p>
        </div>
      </form>;
    };

    return (
      <BaseContainer ref="BaseContainer" routes={routes} headerContent={this.getHeaderContent()}>
        {fetchLoader
          ? <CommonLoaderSign imgName={"services"}/>
          : <div>
            <div className="row row-margin-bottom">
              <div className="col-md-8 col-md-offset-2">
                <div className="input-group">
                  <input type="text" ref="addURLInput" onKeyPress={this.handleKeyPress} className={`form-control ${showInputErr
                    ? ''
                    : 'invalidInput'}`} placeholder="http://ambari_host:port/api/v1/clusters/CLUSTER_NAME"/>
                  <span className="input-group-btn">
                    <button className="btn btn-success" type="button" onClick={this.addBtnClicked}>
                      Add
                    </button>
                  </span>
                </div>
                <lable className={`text-danger ${showInputErr
                  ? 'hidden'
                  : ''}`}>This is not a valid Url</lable>
              </div>
            </div>
            <div className="row">
              {(splitData.length === 0)
                ? <NoData imgName={"services"}/>
                : splitData[pageIndex].map((list) => {
                  return <PoolItemsCard key={list.cluster.id} clusterList={list} poolActionClicked={this.poolActionClicked} refIdArr={refIdArr} loader={loader}/>;
                })
}
            </div>
          </div>
}
        {(entities.length > pageSize)
          ? <Paginate len={entities.length} splitData={splitData} pagesize={pageSize} pagePosition={this.pagePosition}/>
          : ''
}
        <Modal ref={(ref) => this.adminFormModel = ref} data-title="Credentials" onKeyPress={this.handleKeyPress} data-resolve={this.adminSaveClicked} data-reject={this.adminCancelClicked}>
          {adminFormFields()}
        </Modal>
      </BaseContainer>
    );
  }
}

export default ServicePoolContainer;
