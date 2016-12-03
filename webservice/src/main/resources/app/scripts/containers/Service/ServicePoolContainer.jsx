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

const ServiceItems = (props) =>{
  const {item} = props;
  return(
    <li><img src={`styles/img/icon-${item.name.toLowerCase()}.png`}/>{item.name}</li>
  )
}

class PoolItemsCard extends Component{
  constructor(props){
    super(props)
  }

  onActionClick = (eventKey) => {
      this.props.poolActionClicked(eventKey, this.clusterRef.dataset.id)
  }
  render(){
    const {clusterList, isLoading} = this.props;
    const {cluster,services} = clusterList;
    const serviceWrap = services || {
        service: (services === undefined)
            ? ''
            : services.service
    };
    const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;
    return(
      <div className="col-md-4">
            <div className="service-box" data-id={cluster.id} ref={(ref) => this.clusterRef = ref}>
                <div className="service-head clearfix">
                    <h4 className="pull-left no-margin">{cluster.name}</h4>
                    <div className="pull-right">
                      <DropdownButton noCaret title={ellipseIcon} id="dropdown" bsStyle="link" className="dropdown-toggle">
                          <MenuItem onClick={this.onActionClick.bind(this ,"refresh/")}>
                              <i className="fa fa-refresh"></i>
                              &nbsp;Refresh
                          </MenuItem>
                          <MenuItem onClick={this.onActionClick.bind(this ,"delete/")}>
                              <i className="fa fa-trash"></i>
                              &nbsp;Delete
                          </MenuItem>
                      </DropdownButton>
                    </div>
                </div>
                <div className="service-body clearfix common-overflow">
                 <ul className="service-components">
                      {
                        serviceWrap.length !== 0
                        ? serviceWrap.map((items, i) => {
                            return <ServiceItems key={i} item={items.service}/>
                          })
                        : <p>No services</p>
                      }
                  </ul>
                </div>
            </div>
        </div>
    )
  }
}
PoolItemsCard.propTypes ={
  poolActionClicked : React.PropTypes.func.isRequired
}


class ServicePoolContainer extends Component{
  constructor(props){
    super(props)
    this.state = {
      entities : [],
      isLoading: {
          loader: false,
          idCheck: ''
      },
      showInputErr : true,
      clusterData : {},
      fetchLoader : true,
      pageIndex : 0,
      pageSize : 6,
    }
    this.fetchData();
  }

  fetchData = () =>{
    ClusterREST.getAllCluster().then((clusters) =>{
      if (clusters.responseMessage !== undefined) {
          FSReactToastr.error(
              <CommonNotification flag="error" content={clusters.responseMessage}/>, '', toastOpt)
      } else {
        let result = clusters.entities;
        this.setState({fetchLoader : false,entities: result,pageIndex:0});
      }
    }).catch((err) => {
        this.setState({fetchLoader : false});
        FSReactToastr.error(
            <CommonNotification flag="error" content={err.message}/>, '', toastOpt)
    });
  }

  componentDidUpdate(){
    this.btnClassChange();
  }
  componentDidMount(){
    this.btnClassChange();
  }
  btnClassChange = () => {
    const container = document.querySelector('.content-wrapper')
    container.setAttribute("class","content-wrapper animated fadeIn ");
  }
  componentWillUnmount(){
    const container = document.querySelector('.content-wrapper')
    container.setAttribute("class","content-wrapper animated fadeIn ");
  }
  addBtnClicked = () => {
    const {showInputErr} = this.state;
    const val = this.refs.addURLInput.value.trim();

    if(Utils.validateURL(val) && val.length !== 0){
      const name = this.sliceClusterUrl(val);
      const tempObj = Object.assign(this.state.clusterData,{clusterName : name,ambariUrl : val});
      this.setState({showInputErr : true,clusterData : tempObj}, this.adminFormModel.show());
      this.refs.addURLInput.value = '';
    }else{
      this.setState({showInputErr : false})
    }
  }
  sliceClusterUrl = (url) => {
    return url.substr((url.lastIndexOf('/')+1),url.length);
  }
  poolActionClicked = (eventKey ,id) => {
    const key = eventKey.split('/');
    switch (key[0].toString()) {
        case "refresh":
            this.handleUpdateCluster(id);
            break;
        case "delete":
            this.handleDeleteCluster(id);
            break;
        default: break;
    }
  }

  handleUpdateCluster = (ID) => {
    const id = +ID;
    let flagUpdate = {
        idCheck: id
    }
    this.setState({isLoading: flagUpdate});
    this.adminFormModel.show();
  }

  handleDeleteCluster = (id) => {
    this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete ?'}).then((confirmBox) => {
      ClusterREST.deleteCluster(id).then((cluster) => {

        this.fetchData();
        confirmBox.cancel();
        if (cluster.responseMessage !== undefined) {
          FSReactToastr.error(
            <CommonNotification flag="error" content={cluster.responseMessage}/>, '', toastOpt)
        } else {
          FSReactToastr.success(
              <strong>cluster deleted successfully</strong>
          )
        }
      }).catch((err) => {
        FSReactToastr.error(
          <CommonNotification flag="error" content={err.message}/>, '', toastOpt)
      })
    })
  }

  validateForm = () => {
    const formNodes = this.refs.modelForm.children;
    let errArr = [];
    const filter = (nodes) => {
        for(let i = 0 ; i < nodes.length ; i++){
            if(nodes[i].children){
              for(let j = 0 ; j < nodes[i].children.length ; j++){
                if(nodes[i].children[j].nodeName === "INPUT"){
                    if(nodes[i].children[j].value.trim() === ''){
                        nodes[i].children[j].setAttribute('class', "form-control invalidInput");
                        errArr.push(j) ;
                    }else{
                      nodes[i].children[j].setAttribute('class', "form-control");
                    }
                }
              }
            }
        }
        if(errArr.length === 0){
          if(this.state.isLoading.idCheck.length !== 0){
            const url = this.refs.userUrl.value.trim();
            if(!Utils.validateURL(url)){
              this.refs.userUrl.setAttribute('class', "form-control invalidInput");
              return false;
            }
          }
          this.fetchFormdata();
          return true;
        }else{
          return false;
        }
      }
    return filter(formNodes);
  }

  fetchFormdata = () => {
    let tempCluster = {},tempLoader = {},name = '';
    const {isLoading,clusterData} = this.state;
    const userName = this.refs.username.value.trim();
    const passWord = this.refs.userpass.value.trim();
    if(isLoading.idCheck.length !== 0){
      const url = this.refs.userUrl.value.trim();
      name = this.sliceClusterUrl(url);
      tempCluster = Object.assign(clusterData,{clusterName : name ,ambariUrl : url});
    }
    tempLoader = Object.assign(isLoading,{loader : true});
    tempCluster = Object.assign(clusterData , {username : userName,password : passWord});
    this.setState({clusterData : tempCluster,isLoading : tempLoader});
  }

  adminSaveClicked = () => {
    if(this.validateForm()){
      const { clusterData,isLoading} = this.state;
      const {clusterName} = clusterData;
      let promiseArr = [];
      if(isLoading.idCheck.length === 0){
        let data = {
          name : clusterName,
          description : "This is an auto generated description"
        }
        promiseArr.push(ClusterREST.postCluster({body : JSON.stringify(data)}))
      }
      if(promiseArr.length !== 0){
        Promise.all(promiseArr)
          .then(result => {
            if (result[0].responseMessage !== undefined) {
                FSReactToastr.error(
                    <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt)
                      let obj = Object.assign(isLoading ,{idCheck : '',loader : false});
                      this.setState({isLoading : obj});
            } else {
                const id = result[0].id;
                this.importAmbariCluster(id);
            }
          });
      }else{
        this.importAmbariCluster();
      }
    }
  }

  importAmbariCluster = (id) => {
    const {clusterData,isLoading} = this.state;
    const {ambariUrl,username, password} = clusterData;
    const {idCheck} = isLoading;
    const clusterID = id ;
    const importClusterData = {
      clusterId : clusterID || idCheck,
      ambariRestApiRootUrl : ambariUrl,
      username : username,
      password : password
    }
    this.adminFormModel.hide();

    // Post call for import cluster from ambari
    ClusterREST.postAmbariCluster({body : JSON.stringify(importClusterData)})
      .then((ambarClusters) => {
        let obj = {};
        if (ambarClusters.message !== undefined) {
            FSReactToastr.error(<CommonNotification flag="error" content={ambarClusters.message}/>, '', toastOpt);
            ClusterREST.deleteCluster(clusterID);
            obj = Object.assign(isLoading ,{idCheck : '',loader : false});
            this.setState({isLoading : obj});
        }else{
          const result = ambarClusters;
          obj = Object.assign(isLoading ,{idCheck : '',loader : false});

          if(idCheck){
            //Update Single Cluster
            let entitiesWrap = [];
            const elPosition = this.state.entities.map(function(x) {
                return x.cluster.id;
            }).indexOf(idCheck);
            entitiesWrap = this.state.entities;
            entitiesWrap[elPosition] = result;
            this.setState({isLoading : obj, entities: entitiesWrap});
            FSReactToastr.success(
                <strong>process has been completed successfully</strong>
            )
          } else {
            this.setState({isLoading : obj}, () => {this.fetchData()});
            FSReactToastr.success(
                <strong>cluster has been added successfully</strong>
            )
          }
        }
    });
  }

  adminCancelClicked = () => {
    const {idCheck} = this.state.isLoading;
    const obj = Object.assign(this.state.isLoading , {idCheck : ''});
    this.setState({isLoading : obj});
    this.adminFormModel.hide();
  }

  pagePosition = (index) => {
    this.setState({pageIndex : index || 0})
  }
  getHeaderContent() {
    return (
      <span>
        Configuration <span className="title-separator">/</span> {this.props.routes[this.props.routes.length-1].name}
      </span>
    );
  }

  render(){
    const {routes} = this.props;
    const {showInputErr,entities,isLoading,fetchLoader,pageSize,pageIndex} = this.state;
    const splitData = _.chunk(entities,pageSize) || [];
    const adminFormFields = () =>{
      return <form className="modal-form config-modal-form" ref="modelForm">
        {
          isLoading.idCheck.length !== 0
          ?  <div className="form-group">
                <label>Url<span className="text-danger">*</span></label>
                  <input type="text"
                    className="form-control"
                    placeholder="Enter your Url"
                    ref="userUrl"
                    autoFocus="true"
                  />
                  <p className="text-danger"></p>
              </div>
          : ''
          }
          <div className="form-group">
              <label>UserName<span className="text-danger">*</span></label>
                <input type="text"
                  className="form-control"
                  placeholder="Enter your Name"
                  ref="username"
                  autoFocus={isLoading.idCheck.length !== 0 ? false : true}
                />
                <p className="text-danger"></p>
            </div>
            <div className="form-group">
              <label>Password<span className="text-danger">*</span></label>
                <input type="password"
                  className="form-control"
                  placeholder="Enter your Password"
                  ref="userpass"
                />
                <p className="text-danger"></p>
            </div>
        </form>
    }

    return(
      <BaseContainer ref="BaseContainer" routes={routes} headerContent={this.getHeaderContent()}>
        <div className="row row-margin-bottom">
            <div className="col-md-8 col-md-offset-2">
                <div className="input-group">
                    <input type="text"
                      ref="addURLInput"
                      className={`form-control ${showInputErr ? '' : 'invalidInput'}`}
                      placeholder="Enter Ambari URL"
                    />
                    <span className="input-group-btn">
                        <button className="btn btn-success"
                          type="button"
                          onClick={this.addBtnClicked}>
                          Add
                        </button>
                    </span>
                </div>
                <lable className={`text-danger ${showInputErr ? 'hidden' : ''}`}>This is not a valid Url</lable>
            </div>
        </div>
        <div className="row">
            {
              (this.state.fetchLoader)
              ? ''
              : (splitData.length === 0)
                ? <NoData/>
                : splitData[pageIndex].map((list) => {
                    return <PoolItemsCard key={list.cluster.id} clusterList={list} poolActionClicked={this.poolActionClicked} isLoading={isLoading}/>
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
        <Modal ref={(ref) => this.adminFormModel = ref}
          data-title="Credentials"
          data-resolve={this.adminSaveClicked}
          data-reject={this.adminCancelClicked}>
          {adminFormFields()}
        </Modal>
        {
          isLoading.loader
          ? <div className="fullScreenLoader">
                  <div className="loading-img text-center loaderWrap">
                      <img src="styles/img/start-loader.gif" alt="loading" />
                      <p>Please be patient. This process will take a while!</p>
                  </div>
            </div>
          : ''
        }
      </BaseContainer>
    )
  }
}

export default ServicePoolContainer;
