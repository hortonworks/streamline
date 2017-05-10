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
import {DropdownButton, MenuItem, Button,FormGroup,InputGroup,FormControl} from 'react-bootstrap';
import Modal from '../../components/FSModal';
import {Scrollbars} from 'react-custom-scrollbars';

/* import common utils*/
import EnvironmentREST from '../../rest/EnvironmentREST';
import ClusterREST from '../../rest/ClusterREST';
import MiscREST from '../../rest/MiscREST';
import Utils from '../../utils/Utils';
import TopologyUtils from '../../utils/TopologyUtils';
import FSReactToastr from '../../components/FSReactToastr';

/* component import */
import BaseContainer from '../BaseContainer';
import NoData from '../../components/NoData';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import Paginate from '../../components/Paginate';
import AddEnvironment from '../../components/AddEnvironment';
import CommonLoaderSign from '../../components/CommonLoaderSign';

const MappingItem = (props) => {
  const {item} = props;
  const name = item.serviceName.replace('_', ' ');
  return (
    <li><img src={`styles/img/icon-${item.serviceName.toLowerCase()}.png`}/>{name}</li>
  );
};

const EnvironmentItems = (props) => {
  let {mapData, clusterObj} = props;
  let a = [],
    b = [];
  mapData.map((m) => {
    if (m.serviceName.length > 8) {
      b.push(m);
    } else {
      a.push(m);
    }
  });
  Array.prototype.push.apply(a, b);
  mapData = a;
  return (
    <div>
      <h5 className="environment-title">{clusterObj.name}<br/>
        <span>{clusterObj.clusterURL}</span>
      </h5>
      <ul className="environment-components clearfix">
        {mapData.map((item, i) => {
          return <MappingItem key={i} item={item}/>;
        })
}
      </ul>
    </div>
  );
};

class EnvironmentCards extends Component {
  constructor(props) {
    super(props);
  }

  checkRefId = (id) => {
    const index = this.props.refIdArr.findIndex((x) => {
      return x === id;
    });
    return index !== -1
      ? true
      : false;
  }

  onActionClick = (eventKey) => {
    this.props.nameSpaceClicked(eventKey, this.nameSpaceRef.dataset.id);
  }
  render() {
    const {nameSpaceList, clusterDetails, mapData} = this.props;
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;
    const {
      namespace,
      mappings = []
    } = nameSpaceList;
    const serviceCount = () => {
      let count = 0;
      _.keys(mappings).map(key => {
        return count += mappings[key].length;
      });
      return count;
    };

    return (
      <div className="col-environment">
        <div className="service-box environment-box" data-id={namespace.id} ref={(ref) => this.nameSpaceRef = ref}>
          <div className="service-head clearfix">
            <h4 className="pull-left no-margin" title={namespace.name}>{Utils.ellipses(namespace.name, 15)}</h4>
            <div className="pull-right">
              <DropdownButton noCaret title={ellipseIcon} id="dropdown" bsStyle="link" className="dropdown-toggle" data-stest="environment-actions">
                <MenuItem onClick={this.onActionClick.bind(this, "edit/")} data-stest="edit-environment">
                  <i className="fa fa-pencil"></i>
                  &nbsp;Edit
                </MenuItem>
                <MenuItem onClick={this.onActionClick.bind(this, "delete/")} data-stest="delete-environment">
                  <i className="fa fa-trash"></i>
                  &nbsp;Delete
                </MenuItem>
              </DropdownButton>
            </div>
          </div>
          {(this.checkRefId(namespace.id))
            ? <div className="service-components">
                <div className="loading-img text-center">
                  <img src="styles/img/start-loader.gif" alt="loading" style={{
                    width: "100px",
                    marginTop: "100px"
                  }}/>
                </div>
              </div>
            : <div>
              <div className="service-title">
                <h5 className="environment-title">{`Services (${serviceCount()})`}</h5>
              </div>
              <div className="service-body environment-body clearfix">
                <Scrollbars style={{
                  height: "500px"
                }} autoHide renderThumbHorizontal={props => <div {...props} style={{
                  display: "none"
                }}/>}>
                  {_.keys(mappings).length === 0
                    ? <div className="col-sm-12">
                        <h4 className="text-center">No Mapping</h4>
                      </div>
                    : _.keys(mappings).map((key, i) => {
                      return <EnvironmentItems key={i} mapData={mappings[key]} clusterObj ={clusterDetails[key]}/>;
                    })
}
                </Scrollbars>
              </div>
            </div>
}
        </div>
      </div>
    );
  }
}

class EnvironmentContainer extends Component {
  constructor(props) {
    super(props);
    this.state = {
      entities: [],
      fetchLoader: true,
      pageIndex: 0,
      pageSize: 5,
      clusterDetails: {},
      customMapData: [],
      namespaceIdToEdit: null,
      refIdArr: [],
      loader: false,
      checkServices: false,
      filterValue: '',
      sorted: {
        key: 'last_updated',
        text: 'Last Updated'
      },
      searchLoader : false
    };
    this.fetchData();
    this.initialFetch = false;
  }

  btnClassChange = () => {
    if (!this.state.fetchLoader) {
      const actionMenu = document.querySelector('.actionDropdown');
      actionMenu.setAttribute("class", "actionDropdown hb lg success ");
      if (this.state.entities.length !== 0) {
        actionMenu.parentElement.setAttribute("class", "dropdown");
        const sortDropdown = document.querySelector('.sortDropdown');
        sortDropdown.setAttribute("class", "sortDropdown");
        sortDropdown.parentElement.setAttribute("class", "dropdown");
      }
    }
  }

  componentDidUpdate() {
    this.btnClassChange();
  }

  componentDidMount() {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper environment-wrapper");
    this.btnClassChange();
  }

  componentWillUnmount() {
    const container = document.querySelector('.content-wrapper');
    container.setAttribute("class", "content-wrapper");
  }

  customMapping = (dataset) => {
    let list = [];
    dataset.map((x, i) => {
      const data = {};
      if (x.mappings.length !== 0) {
        x.mappings.map(o => {
          if (data[o.clusterId] === undefined) {
            data[o.clusterId] = [];
          }
          data[o.clusterId].push(o);
        });
      }
      x.mappings = data;
    });

    return dataset;
  }

  fetchData = () => {
    const obj = {};
    let promiseArr = [ClusterREST.getAllCluster(), EnvironmentREST.getAllNameSpaces()];

    Promise.all(promiseArr).then(result => {
      let serviceLen = 0,
        serviceFlag = false;
      // call for get All cluster Name
      if (result[0].responseMessage !== undefined) {
        this.setState({fetchLoader: false, namespaceIdToEdit: null,searchLoader: false});
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt);
      } else {
        const entities = result[0].entities;
        serviceLen = entities.length;
        entities.map(x => {
          if (obj[Number(x.cluster.id)] === undefined) {
            obj[Number(x.cluster.id)] = {
              name: x.cluster.name,
              clusterURL: x.cluster.ambariImportUrl
            };
          }
        });
      }
      // call for get All nameSpaces
      if (result[1].responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={result[1].responseMessage}/>, '', toastOpt);
      } else {
        const resultSet = result[1].entities;
        const mappingList = this.customMapping(resultSet);
        if (resultSet.length === 0 && serviceLen !== 0) {
          serviceFlag = true;
        }
        this.setState({
          fetchLoader: false,
          entities: mappingList,
          pageIndex: 0,
          clusterDetails: obj,
          namespaceIdToEdit: null,
          checkServices: serviceFlag
        });
      }

    }).catch((err) => {
      this.setState({fetchLoader: false, namespaceIdToEdit: null});
      FSReactToastr.error(
        <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
    });
  }

  addEnvironmentBtn = () => {
    this.setState({
      namespaceIdToEdit: null
    }, () => {
      this.addEvtModel.show();
    });
  }

  addEvtModelSaveClicked = () => {
    const {namespaceIdToEdit} = this.state;
    const formData = this.EvtModelRef.handleSave();
    let tempArr = _.cloneDeep(this.state.refIdArr);
    if (formData !== undefined) {
      let promiseArr = [];
      (!namespaceIdToEdit)
        ? promiseArr.push(EnvironmentREST.postNameSpace(null, {
          body: JSON.stringify(formData.obj)
        }))
        : promiseArr.push(EnvironmentREST.putNameSpace(namespaceIdToEdit, {
          body: JSON.stringify(formData.obj)
        }));

      Promise.all(promiseArr).then((result) => {
        if (result[0].responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt);
        } else {
          let entity = result[0];
          formData.tempData.map(x => {
            return x.namespaceId = entity.id;
          });
          tempArr.push(entity.id);
          this.setState({
            refIdArr: tempArr
          }, () => {
            this.addEvtModel.hide();
          });
          EnvironmentREST.postNameSpace(entity.id, {
            body: JSON.stringify(formData.tempData)
          }).then((mapping) => {
            if (mapping.responseMessage !== undefined) {
              (!namespaceIdToEdit)
                ? EnvironmentREST.deleteNameSpace(entity.id)
                : '';
              const tempDataArray = this.spliceTempArr(entity.id);
              this.setState({
                refIdArr: tempDataArray
              }, () => {
                let errorMsg = mapping.responseMessage;
                errorMsg = mapping.responseMessage.indexOf('Trying to modify mapping of streaming engine') !== -1
                  ? "Cannot change the stream engine while a topology is deployed in the same environment"
                  : mapping.responseMessage;
                FSReactToastr.error(
                  <CommonNotification flag="error" content={errorMsg}/>, '', toastOpt);
              });
            } else {
              const nameSpaceId = namespaceIdToEdit;
              const tempDataArray = this.spliceTempArr(Number(entity.id || nameSpaceId));

              this.setState({
                fetchLoader: true,
                refIdArr: tempDataArray
              }, () => {
                this.fetchData();
                clearTimeout(clearTimer);
                const clearTimer = setTimeout(() => {
                  (nameSpaceId === null)
                    ? FSReactToastr.success(
                      <strong>Environment added successfully</strong>
                    )
                    : FSReactToastr.success(
                      <strong>Environment updated successfully</strong>
                    );
                }, 500);
              });
            }
          });
        }
      });
    }
  }

  pagePosition = (index) => {
    this.setState({
      pageIndex: index || 0
    });
  }

  nameSpaceClicked = (eventKey, id) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
    case "edit":
      this.handleUpdateNameSpace(id);
      break;
    case "delete":
      this.handleDeleteNameSpace(id);
      break;
    default:
      break;
    }
  }

  handleUpdateNameSpace = (id) => {
    this.setState({
      namespaceIdToEdit: id
    }, () => {
      this.addEvtModel.show();
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

  handleDeleteNameSpace = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete ?'}).then((confirmBox) => {
      EnvironmentREST.deleteNameSpace(id).then((nameSpace) => {
        this.initialFetch = false;
        confirmBox.cancel();
        if (nameSpace.responseMessage !== undefined) {
          if (nameSpace.responseMessage.indexOf('Namespace refers the cluster') !== 1) {
            FSReactToastr.info(
              <CommonNotification flag="info" content={"Namespace refers to some Topology. So it can't be deleted."}/>, '', toastOpt);
          } else {
            FSReactToastr.error(
              <CommonNotification flag="error" content={nameSpace.responseMessage}/>, '', toastOpt);
          }
        } else {
          this.setState({
            fetchLoader: true
          }, () => {
            this.fetchData();
            clearTimeout(clearTimer);
            const clearTimer = setTimeout(() => {
              FSReactToastr.success(
                <strong>Environment deleted successfully</strong>
              );
            }, 500);
          });
        }
      });
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
      this.addEvtModel.state.show
        ? this.addEvtModelSaveClicked()
        : '';
    }
  }

  onFilterChange = (e) => {
    this.setState({filterValue: e.target.value.trim()}, () => {
      this.getFilteredEntities();
    });
  }

  getFilteredEntities = () => {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => {
      const {filterValue,sorted} = this.state;
      this.setState({searchLoader: true}, () => {
        MiscREST.searchEntities('namespace', filterValue,sorted.key).then((namespace)=>{
          if (namespace.responseMessage !== undefined) {
            FSReactToastr.error(
              <CommonNotification flag="error" content={namespace.responseMessage}/>, '', toastOpt);
            this.setState({searchLoader: false});
          } else {
            this.initialFetch = true;
            const mappingList = this.customMapping(namespace.entities);
            this.setState({searchLoader: false, entities: mappingList, pageIndex: 0});
          }
        });
      });
    }, 500);
  }

  onSortByClicked = (eventKey, el) => {
    const liList = el.target.parentElement.parentElement.children;
    for (let i = 0; i < liList.length; i++) {
      liList[i].setAttribute('class', '');
    }
    el.target.parentElement.setAttribute("class", "active");
    const sortKey = (eventKey.toString() === "name")
      ? "name"
      : eventKey;
    this.setState({searchLoader: true});
    const {filterValue} = this.state;

    MiscREST.searchEntities('namespace', filterValue,sortKey).then((namespace)=>{
      if (namespace.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={namespace.responseMessage}/>, '', toastOpt);
        this.setState({searchLoader: false});
      } else {
        const sortObj = {
          key: eventKey,
          text: Utils.sortByKey(eventKey)
        };
        this.setState({searchLoader: false, entities: namespace.entities, sorted: sortObj});
      }
    });
  }

  render() {
    const {
      entities,
      pageSize,
      pageIndex,
      fetchLoader,
      clusterDetails,
      namespaceIdToEdit,
      refIdArr,
      loader,
      checkServices,
      filterValue,
      searchLoader,
      sorted
    } = this.state;
    const {routes} = this.props;
    const splitData = _.chunk(entities, pageSize) || [];
    const modelTitle = <span>{namespaceIdToEdit === null
        ? "New "
        : "Edit "}Environment{/* <i className="fa fa-info-circle"></i>*/}</span>;
    const sortTitle = <span>Sort:<span style={{
      color: "#006ea0"
    }}>&nbsp;{sorted.text}</span>
    </span>;
    return (
      <BaseContainer ref="BaseContainer" routes={routes} headerContent={this.getHeaderContent()}>
        <div id="add-environment">
          <a href="javascript:void(0);" className="hb lg success actionDropdown" data-target="#addEnvironment" onClick={this.addEnvironmentBtn}>
            <i className="fa fa-plus"></i>
          </a>
        </div>
        <div className="row">
          {fetchLoader
            ? <CommonLoaderSign imgName={"environments"}/>
            : <div>
                <div className="row">
                  <div className="page-title-box clearfix">
                    <div className="col-md-3 col-md-offset-6 text-right">
                      <FormGroup>
                        <InputGroup>
                        <FormControl data-stest="searchBox" type="text" placeholder="Search by name" onKeyUp={this.onFilterChange} className="" />
                        <InputGroup.Addon>
                          <i className="fa fa-search"></i>
                        </InputGroup.Addon>
                        </InputGroup>
                      </FormGroup>
                    </div>
                    <div className="col-md-2 text-center">
                      <DropdownButton title={sortTitle} id="sortDropdown" className="sortDropdown ">
                        <MenuItem active={sorted.key === "name" ? true : false } onClick={this.onSortByClicked.bind(this, "name")}>
                          &nbsp;Name
                        </MenuItem>
                        <MenuItem active={sorted.key === "last_updated" ? true : false } onClick={this.onSortByClicked.bind(this, "last_updated")}>
                          &nbsp;Last Update
                        </MenuItem>
                        {/*<MenuItem active={this.state.sorted.key === "status" ? true : false } onClick={this.onSortByClicked.bind(this, "status")}>
                          &nbsp;Status
                        </MenuItem>*/}
                      </DropdownButton>
                    </div>
                  </div>
                </div>
              {searchLoader
              ? <CommonLoaderSign imgName={"environments"}/>
              : !searchLoader && entities.length === 0 && filterValue
                ? <NoData imgName={"applications"} searchVal={filterValue}/>
                : entities.length !== 0
                  ? splitData[pageIndex].map((nameSpaceList, i) => {
                    return <EnvironmentCards key={i} nameSpaceList={nameSpaceList} nameSpaceClicked={this.nameSpaceClicked} clusterDetails={clusterDetails} refIdArr={refIdArr} loader={loader}/>;
                  })
                  : !this.initialFetch && entities.length === 0
                    ? <NoData imgName={"environments"}/>
                    : ''
              }
            </div>
}
        </div>
        {(entities.length > pageSize)
          ? <Paginate len={entities.length} splitData={splitData} pagesize={pageSize} pagePosition={this.pagePosition}/>
          : ''
}
        <Modal ref={(ref) => this.addEvtModel = ref} data-title={modelTitle} onKeyPress={this.handleKeyPress} data-resolve={this.addEvtModelSaveClicked} data-reject={this.addEvtModelCancelClicked}>
          <AddEnvironment ref={(ref) => this.EvtModelRef = ref} namespaceId={namespaceIdToEdit}/>
        </Modal>
      </BaseContainer>
    );
  }
}

export default EnvironmentContainer;
