import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import {
    DropdownButton,
    MenuItem,
    Button
} from 'react-bootstrap';
import Modal from '../../components/FSModal';

/* import common utils*/
import EnvironmentREST from '../../rest/EnvironmentREST';
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
import AddEnvironment from '../../components/AddEnvironment';

const MappingItem = (props) => {
  const {item} = props;
  const name = item.serviceName.replace('_',' ');
  return (<li><img src={`styles/img/icon-${item.serviceName.toLowerCase()}.png`} />{name}</li>)
}

const EnvironmentItems = (props) => {
  let {mapData,name} = props;
  let a = [], b = [];
  mapData.map((m)=>{
    if(m.serviceName.length > 8){
      b.push(m);
    } else {
      a.push(m);
    }
  })
  Array.prototype.push.apply(a, b);
  mapData = a;
  return (
    <div>
      <h5 className="environment-title">{name}</h5>
      <ul className="environment-components clearfix">
          {
            mapData.map((item , i) => {
                return <MappingItem key={i} item={item} />
            })
          }
      </ul>
    </div>
  )
}

class EnvironmentCards extends Component {
  constructor(props){
    super(props);
  }

  onActionClick = (eventKey) => {
    this.props.nameSpaceClicked(eventKey, this.nameSpaceRef.dataset.id)
  }
  render(){
    const {nameSpaceList, isLoading,clusterName,mapData} = this.props;
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;
    const {namespace,mappings = []} = nameSpaceList;
    const serviceCount = () => {
      let count = 0;
      _.keys(mappings).map(key => {
          return count += mappings[key].length;
      });
      return count;
    }

    return(
      <div className="col-environment">
        <div className="service-box environment-box"  data-id={namespace.id} ref={(ref) => this.nameSpaceRef = ref} >
            <div className="service-head clearfix">
              <h4 className="pull-left no-margin" title={namespace.name}>{Utils.ellipses(namespace.name,15)}</h4>
                <div className="pull-right">
                  <DropdownButton noCaret title={ellipseIcon} id="dropdown" bsStyle="link" className="dropdown-toggle">
                    <MenuItem  onClick={this.onActionClick.bind(this ,"edit/")}>
                        <i className="fa fa-refresh"></i>
                        &nbsp;Edit
                    </MenuItem>
                    <MenuItem  onClick={this.onActionClick.bind(this ,"delete/")}>
                        <i className="fa fa-trash"></i>
                        &nbsp;Delete
                    </MenuItem>
                </DropdownButton>
                </div>
            </div>
            <div className="service-title">
              <h5 className="environment-title">{`Services (${serviceCount()})`}</h5>
            </div>
            <div className="service-body environment-body clearfix common-overflow">
                {
                  _.keys(mappings).length === 0
                    ? <div className="col-sm-12"><h4 className="text-center">No Mapping</h4></div>
                    : _.keys(mappings).map((key , i) => {
                         return <EnvironmentItems key={i} mapData={mappings[key]} name ={clusterName[key]} />
                    })
                }
            </div>
        </div>
      </div>
    )
  }
}

class EnvironmentContainer extends Component{
  constructor(props){
    super(props)
    this.state = {
      entities : [],
      isLoading: {
          loader: false,
          idCheck: ''
      },
      fetchLoader : true,
      pageIndex : 0,
      pageSize : 5,
      clusterName : {},
      customMapData : [],
      namespaceIdToEdit: null
    }
    this.fetchData();
  }

  customMapping = (dataset) => {
    let list=[];
    dataset.map((x,i) => {
        const data = {};
        if(x.mappings.length !== 0){
            x.mappings.map(o => {
                if(data[o.clusterId] === undefined){
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
    let promiseArr = [
      ClusterREST.getAllCluster(),
      EnvironmentREST.getAllNameSpaces()
    ];

    Promise.all(promiseArr)
    .then(result => {
      // call for get All cluster Name
      if (result[0].responseMessage !== undefined) {
          this.setState({fetchLoader : false, namespaceIdToEdit: null});
          FSReactToastr.error(
              <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt)
      }else{
        const entities = result[0].entities;
        entities.map(x => {
          if(obj[Number(x.cluster.id)] === undefined){
            obj[Number(x.cluster.id)] = x.cluster.name;
          }
        });
      }
      // call for get All nameSpaces
      if (result[1].responseMessage !== undefined) {
          FSReactToastr.error(
              <CommonNotification flag="error" content={result[1].responseMessage}/>, '', toastOpt)
      }else{
        const resultSet = result[1].entities;
        const mappingList = this.customMapping(resultSet);
        this.setState({fetchLoader : false,entities: mappingList,pageIndex:0 ,clusterName : obj, namespaceIdToEdit: null});
      }

    }).catch((err) => {
        this.setState({fetchLoader : false, namespaceIdToEdit: null});
        FSReactToastr.error(
            <CommonNotification flag="error" content={err.message}/>, '', toastOpt)
    });
  }

  addEnvironmentBtn = () => {
    this.setState({namespaceIdToEdit: null},()=>{
      this.addEvtModel.show();
    })
  }

  addEvtModelSaveClicked = () => {
    const formData = this.EvtModelRef.handleSave();
    if(formData !== undefined){

      let promiseArr = [];
      (!this.state.namespaceIdToEdit)
        ? promiseArr.push(EnvironmentREST.postNameSpace(null,{body : JSON.stringify(formData.obj)}))
        : promiseArr.push(EnvironmentREST.putNameSpace(this.state.namespaceIdToEdit, {body : JSON.stringify(formData.obj)}));

      Promise.all(promiseArr)
        .then((result) => {
          if (result[0].responseMessage !== undefined) {
              FSReactToastr.error(
                  <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt)
          } else {
            let entity = result[0];
            formData.tempData.map(x =>{
              return x.namespaceId = entity.id;
            });
            this.addEvtModel.hide();
            EnvironmentREST.postNameSpace(entity.id,{body : JSON.stringify(formData.tempData)})
              .then((mapping) => {
                if (mapping.responseMessage !== undefined) {
                    FSReactToastr.error(
                        <CommonNotification flag="error" content={mapping.responseMessage}/>, '', toastOpt)
                } else{
                  const nameSpaceEditId = this.state.namespaceIdToEdit;
                  this.setState({fetchLoader : true}, () => {
                    this.fetchData();
                    clearTimeout(clearTimer);
                    const clearTimer = setTimeout(() => {
                      (nameSpaceEditId === null)
                        ? FSReactToastr.success(<strong>Environment added successfully</strong>)
                        : FSReactToastr.success(<strong>Environment updated successfully</strong>);
                    },500);
                  });
                }
            });
          }
        });
      }
  }
  pagePosition = (index) => {
    this.setState({pageIndex : index || 0})
  }

  nameSpaceClicked = (eventKey ,id) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
        case "edit":
            this.handleUpdateNameSpace(id);
            break;
        case "delete":
            this.handleDeleteNameSpace(id);
            break;
        default: break;
    }
  }

  handleUpdateNameSpace = (id) => {
    this.setState({namespaceIdToEdit: id},()=>{
      this.addEvtModel.show();
    })
  }

  handleDeleteNameSpace = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete ?'}).then((confirmBox) => {
      EnvironmentREST.deleteNameSpace(id).then((nameSpace) => {

        confirmBox.cancel();
        if (nameSpace.responseMessage !== undefined) {
          if(nameSpace.responseMessage.indexOf('Namespace refers the cluster') !== 1){
            FSReactToastr.info(
              <CommonNotification flag="info" content={"Namespace refers to some Topology. So it can't be deleted."}/>, '', toastOpt)
          }else{
            FSReactToastr.error(
              <CommonNotification flag="error" content={nameSpace.responseMessage}/>, '', toastOpt);
          }
        } else {
          this.setState({fetchLoader : true}, () => {
            this.fetchData();
            clearTimeout(clearTimer);
            const clearTimer = setTimeout(() => {
              FSReactToastr.success(
                  <strong>Environment deleted successfully</strong>
              )
            },500);
          });
        }
      })
    })
  }

  getHeaderContent() {
    return (
      <span>
        Configuration <span className="title-separator">/</span> {this.props.routes[this.props.routes.length-1].name}
      </span>
    );
  }

  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.addEvtModel.state.show ? this.addEvtModelSaveClicked() : '';
    }
  }

  render(){
    const {entities,pageSize,pageIndex,fetchLoader,isLoading,clusterName, namespaceIdToEdit} = this.state;
    const {routes} = this.props;
    const splitData = _.chunk(entities,pageSize) || [];
    const modelTitle = <span>{namespaceIdToEdit === null ? "New " : "Edit "   }Environment{/* <i className="fa fa-info-circle"></i>*/}</span>
    return(
      <BaseContainer ref="BaseContainer" routes={routes} headerContent={this.getHeaderContent()}>
        <div id="add-environment">
          <a href="javascript:void(0);"
            className="hb lg success actionDropdown"
            data-target="#addEnvironment"
            onClick={this.addEnvironmentBtn}>
              <i className="fa fa-plus"></i>
          </a>
        </div>
        <div className="row">
            {
              fetchLoader
              ?   <div className="fullPageLoader">
                      <img src="styles/img/start-loader.gif" alt="loading" />
                  </div>
              :
                entities.length === 0
                ? <NoData />
              : splitData[pageIndex].map((nameSpaceList,i) => {
                  return <EnvironmentCards key={i}
                            nameSpaceList = {nameSpaceList}
                            nameSpaceClicked = {this.nameSpaceClicked}
                            isLoading={isLoading}
                            clusterName={clusterName}
                          />
              })
            }
        </div>
        {
          (entities.length > pageSize)
            ? <Paginate
              len={entities.length}
              splitData={splitData}
              pagesize={pageSize}
              pagePosition={this.pagePosition}
            />
          :''
        }
        <Modal ref={(ref) => this.addEvtModel = ref}
          data-title={modelTitle}
          onKeyPress={this.handleKeyPress}
          data-resolve={this.addEvtModelSaveClicked}
          data-reject={this.addEvtModelCancelClicked}>
          <AddEnvironment ref={(ref) => this.EvtModelRef = ref} namespaceId={namespaceIdToEdit}/>
        </Modal>
      </BaseContainer>
    )
  }
}

export default EnvironmentContainer;
