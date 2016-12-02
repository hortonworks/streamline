import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Link} from 'react-router';
import moment from 'moment';
import {
    DropdownButton,
    MenuItem,
    FormGroup,
    InputGroup,
    FormControl,
    Button
} from 'react-bootstrap';
import d3 from 'd3';
/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt , PieChartColor} from '../../../utils/Constants';
import PieChart from '../../../components/PieChart';
import Paginate from '../../../components/Paginate';
import Modal from '../../../components/FSModal';
import AddTopology from './AddTopology'

class CustPieChart extends PieChart{
  drawPie(){
    super.drawPie()

    this.svg.selectAll('title').remove();

    this.svg.selectAll('.pie-latency').remove();

    this.container.append('text')
      .attr({
        class: 'pie-latency',
        y: -15,
        'text-anchor': 'middle',
        'font-size': "9",
        fill : "#888e99"
      })
      .text('LATENCY')

    const text = this.container.append('text')
      .attr({
        class: 'pie-latency',
        'text-anchor': 'middle'
      })
    const latencyDefaultTxt = Utils.secToMinConverter(this.props.latency , "graph").split('/');
    const tspan = text.append('tspan')
      .attr({
        'font-size': "28",
        'fill' : "#323133",
          y: 20,
      })
      .text(latencyDefaultTxt[0])

    const secText = text.append('tspan')
    .attr({
      fill : "#6d6f72",
      "font-size" : 10
    })
    .text(' '+latencyDefaultTxt[1])

    if(!this.props.empty){
      this.container.selectAll('path')
        .on('mouseenter', (d) => {
          const val = Utils.secToMinConverter(d.value , "graph").split('/');
          tspan.text(
            val[0]
          );
          secText.text(val[1])
        })
        .on('mouseleave', (d) => {
          tspan.text(latencyDefaultTxt[0]);
          secText.text(' '+latencyDefaultTxt[1])
        })
    }
  }
}

class TopologyItems extends Component {
    constructor(props) {
        super(props)
    }

    onActionClick = (eventKey) => {
        this.props.topologyAction(eventKey, this.streamRef.dataset.id)
    }
    streamBoxClick = (id,event) => {
      if(event.target.nodeName !== 'I'){
        this.context.router.push('applications/'+id+'/view');
      }
    }
    render() {
        const {topologyAction, topologyList, isLoading} = this.props;
        const {topology, metric,latencyTopN} = topologyList;
        const metricWrap = metric || {
            misc: (metric === undefined)
                ? ''
                : metric.misc
        };
        let latencyWrap = latencyTopN || [];
        let graphData = [], graphVal=0;
        latencyWrap.map((d,v) => {
            graphData.push({name : Object.keys(d)[0] , value : d[Object.keys(d)[0]]})
            graphVal += d[Object.keys(d)[0]];
        })
        const unitLeft = _.slice(latencyWrap, 0, latencyWrap.length/2);
        const unitRight = _.slice(latencyWrap, latencyWrap.length/2 , latencyWrap.length)

        return (
            <div className="col-sm-4">
                <div className={`stream-box ${ (isLoading.loader && (isLoading.idCheck === topology.id))
                                ? ''
                                : metricWrap.status || 'NOTRUNNING'}`}
              data-id={topology.id} ref={(ref) => this.streamRef = ref}
              onClick={this.streamBoxClick.bind(this,topology.id)}>
                <div className="stream-head clearfix">
                    <div className="pull-left">
                      <Link to={`applications/${topology.id}/view`}>
                          <h4><i className={`fa fa-exclamation-${
                              (metricWrap.status || 'NOTRUNNING') === "KILLED"
                                ? 'circle KILLED'
                                : (metricWrap.status || 'NOTRUNNING') === "NOTRUNNING"
                                  ? 'triangle NOTRUNNING' : ''
                            }`}></i>{topology.name}</h4>
                      </Link>
                      <h5>
                          {(metricWrap.uptime === undefined)
                              ? (topologyList.running === "NOT_RUNNING") ? "Not Running" : topologyList.running
                              : "Uptime "+Utils.splitSeconds(metricWrap.uptime)
                          }
                      </h5>
                    </div>
                    <div className="pull-right">
                        <div className="stream-actions">
                          <a href="javascript:void(0)" title="Refresh" onClick={this.onActionClick.bind(this ,"refresh/"+topology.id)}>
                            <i className="fa fa-refresh" aria-hidden="true"></i>
                          </a>
                          <Link to={`applications/${topology.id}/edit`} title="Edit">
                            <i className="fa fa-pencil" aria-hidden="true"></i>
                          </Link>
                          <a href="javascript:void(0)" title="Clone" onClick={this.onActionClick.bind(this ,"clone/"+topology.id)}>
                            <i className="fa fa-clone" aria-hidden="true"></i>
                          </a>
                          <a href="javascript:void(0)" title="Export" onClick={this.onActionClick.bind(this ,"export/"+topology.id)}>
                            <i className="fa fa-share-square-o" aria-hidden="true"></i>
                          </a>
                          <a href="javascript:void(0)" title="Delete" className="close" onClick={this.onActionClick.bind(this ,"delete/"+topology.id)}>
                            <i className="fa fa-times-circle" aria-hidden="true"></i>
                          </a>
                        </div>
                    </div>
                </div>
                {
                  (isLoading.loader && (isLoading.idCheck === topology.id))
                    ? <div className="stream-body">
                        <div className="loading-img text-center">
                            <img src="styles/img/start-loader.gif" alt="loading" />
                        </div>
                      </div>
                    : <div className="stream-body">
                        <div className="row">
                            <div className="stream-components col-md-4">
                                {
                                  unitLeft.map((d,v) => {
                                    return <h5 className="text-left"
                                              title={Object.keys(d)[0]}
                                              key={v}
                                            >
                                            <i className="fa fa-square boxGap" style={{color : PieChartColor[v]}}></i>
                                            {Utils.secToMinConverter(d[Object.keys(d)[0]],"list")}
                                            <span>&nbsp;</span>
                                            {Utils.ellipses(Object.keys(d)[0],9)}</h5>
                                  })
                                }
                            </div>
                            <div className="latency-chart">
                              {(graphData.length && graphVal !== 0) ?
                                <CustPieChart data={graphData}
                                  latency={metricWrap.latency || 0}
                                  innerRadius={5}
                                  color={d3.scale.category20c().range(PieChartColor)}
                                />
                                :
                                <CustPieChart data={[{name: 'none', value: 1}]}
                                  empty={true}
                                  latency={metricWrap.latency || 0}
                                  innerRadius={5}
                                  color={d3.scale.category20c().range(['#a7a9ac'])}
                                />
                              }
                            </div>
                            <div className="stream-components col-md-4 col-md-offset-4">
                                {
                                  unitRight.map((d,v) => {
                                    return <h5 className="text-right"
                                              title={Object.keys(d)[0]}
                                              key={v}
                                            >
                                            <i className="fa fa-square boxGap" style={{color : PieChartColor[unitLeft.length+v]}}></i>
                                            {Utils.secToMinConverter(d[Object.keys(d)[0]],"list")}
                                            <span>&nbsp;</span>
                                            {Utils.ellipses(Object.keys(d)[0],9)}</h5>
                                  })
                                }
                            </div>
                        </div>
                        <div className="row row-margin-top">
                            <div className="stream-stats">
                                <h6>Emitted</h6>
                                <h5>{metricWrap.misc.emitted || 0}</h5>
                            </div>
                            <div className="stream-stats">
                                <h6>Transferred</h6>
                                <h5>{metricWrap.misc.transferred || 0}</h5>
                            </div>
                            <div className="stream-stats">
                                <h6>Errors</h6>
                                <h5 className="color-error">{metricWrap.misc.errors || 0}</h5>
                            </div>
                            <div className="stream-stats">
                                <h6>Workers</h6>
                                <h5>{metricWrap.misc.workersTotal || 0}</h5>
                            </div>
                            <div className="stream-stats">
                                <h6>Executors</h6>
                                <h5>{metricWrap.misc.executorsTotal || 0}</h5>
                            </div>
                        </div>
                    </div>
                }

              </div>
            </div>
        );
    }
}

TopologyItems.propTypes = {
    topologyList: React.PropTypes.object.isRequired,
    topologyAction: React.PropTypes.func.isRequired
}

TopologyItems.contextTypes = {
    router: React.PropTypes.object.isRequired
};

class TopologyListingContainer extends Component {
    constructor(props) {
        super();
        this.state = {
            entities: [],
            filterValue: '',
            slideInput : false,
            sorted : {
              key : 'last_updated',
              text : 'Last Updated'
            },
            isLoading: {
                loader: false,
                idCheck: ''
            },
            fetchLoader : true,
            pageIndex : 0,
            pageSize : 9,
        }

        this.fetchData();
    }

    fetchData() {
      const sortKey = this.state.sorted.key;
        TopologyREST.getAllTopology(sortKey).then((topology) => {
            if (topology.responseMessage !== undefined) {
                FSReactToastr.error(
                    <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
            } else {
                let result = Utils.sortArray(topology.entities.slice(), 'timestamp', false);
                this.setState({fetchLoader : false,entities: result,pageIndex:0});
            }
        }).catch((err) => {
            this.setState({fetchLoader : false});
            FSReactToastr.error(
                <CommonNotification flag="error" content={err}/>, '', toastOpt)
        });
    }

    onFilterChange = (e) => {
        this.setState({filterValue: e.target.value.trim()});
    }

    fetchSingleTopology = (ID) => {
        const id = +ID;
        let flagUpdate = {
            loader: true,
            idCheck: id
        }
        this.setState({isLoading: flagUpdate});
        TopologyREST.getTopology(id).then((topology) => {
            this.updateSingleTopology(topology, id)
            flagUpdate = {
                loader: false,
                idCheck: id
            }
            this.setState({isLoading: flagUpdate})
        }).catch((err) => {
            FSReactToastr.error(
                <CommonNotification flag="error" content={err}/>, '', toastOpt)
        });
    }

    updateSingleTopology(newTopology, id) {
        let entitiesWrap = [];
        const elPosition = this.state.entities.map(function(x) {
            return x.topology.id;
        }).indexOf(id)
        entitiesWrap = this.state.entities;
        entitiesWrap[elPosition] = newTopology;
        this.setState({entities: entitiesWrap})
    }

    handleAddTopology() {
      this.AddTopologyModelRef.show();
    }

    handleImportTopology = (e) => {
      if(!e.target.files.length || (e.target.files.length && e.target.files[0].name.indexOf('.json') < 0)){
        FSReactToastr.error(<CommonNotification flag="error" content="please select the .json file type.."/>, '', toastOpt)
  			return;
  		}
  		let fileObj = e.target.files[0];
      if(fileObj){
        let formData = new FormData();
        formData.append('file', fileObj);
        
        TopologyREST.importTopology({body:formData})
          .then(importResponse => {
            if (importResponse.responseMessage !== undefined) {
              FSReactToastr.error(<CommonNotification flag="error" content={importResponse.responseMessage}/>, '', toastOpt)
            } else {
              this.fetchData();
              FSReactToastr.success(<strong>File has been imported successfully</strong>)
            }
          });
      }
    }

    deleteSingleTopology = (id) => {
      this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete ?'}).then((confirmBox) => {
        TopologyREST.deleteTopology(id).then((topology) => {
          // TopologyREST.deleteMetaInfo(id);
          this.fetchData();
          confirmBox.cancel();
          if (topology.responseMessage !== undefined) {
            FSReactToastr.error(
              <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
          } else {
            FSReactToastr.success(
                <strong>Topology deleted successfully</strong>
            )
          }
        }).catch((err) => {
          FSReactToastr.error(
            <CommonNotification flag="error" content={err}/>, '', toastOpt)
        })
      })
    }

    cloneTopologyAction = (id) => {
      this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to clone the topology ?'}).then((confirmBox) => {
        TopologyREST.cloneTopology(id).then((topology) => {
          this.fetchData();
          confirmBox.cancel();
          if (topology.responseMessage !== undefined) {
            FSReactToastr.error(<CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
          } else {
            FSReactToastr.success(<strong>Topology cloned successfully</strong>)
          }
        })
      })
    }

    exportTopologyAction = (id) => {
      this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to export the topology ?'}).then((confirmBox) => {
        this.refs.ExportTopology.href = TopologyREST.getExportTopologyURL(id);
        this.refs.ExportTopology.click();
        confirmBox.cancel();
      })
    }

    actionHandler = (eventKey, id) => {
      event.stopPropagation();
      const key = eventKey.split('/');
      switch (key[0].toString()) {
        case "refresh":
          this.fetchSingleTopology(id);
          break;
        case "clone":
          this.cloneTopologyAction(id);
          break;
        case "export":
          this.exportTopologyAction(id);
          break;
        case "delete":
          this.deleteSingleTopology(id);
          break;
        default:
          break;
      }
    }

    slideInput = (e) => {
      this.setState({slideInput  : true})
      const input = document.querySelector('.inputAnimateIn');
      input.focus();
    }
    slideInputOut = () => {
      const input = document.querySelector('.inputAnimateIn');
      (_.isEmpty(input.value)) ? this.setState({slideInput  : false}) : ''
    }

    onSortByClicked = (eventKey,el) => {
      const liList = el.target.parentElement.parentElement.children;
      for(let i = 0;i < liList.length ; i++){
        liList[i].setAttribute('class','');
      }
      el.target.parentElement.setAttribute("class","active");
      const sortKey = (eventKey.toString() === "name") ? "name&ascending=true" : eventKey;
      TopologyREST.getAllTopology(sortKey).then((topology) => {
        if (topology.responseMessage !== undefined) {
            FSReactToastr.error(
                <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
        } else {
            let result = Utils.sortArray(topology.entities.slice(), 'timestamp', false);
            const sortObj = {key : eventKey , text : Utils.sortByKey(eventKey)}
            this.setState({fetchLoader : false,entities: result ,sorted : sortObj});
        }
      }).catch((err) => {
          this.setState({fetchLoader : false});
          FSReactToastr.error(
              <CommonNotification flag="error" content={err.message}/>, '', toastOpt)
      });
    }

    onActionMenuClicked = (eventKey) => {
      event.stopPropagation();
      switch(eventKey.toString()){
        case "create" : this.handleAddTopology();
          break;
        case "import" : this.importFileRef.click();
          break;
         default : break;
      }
    }
    componentDidUpdate(){
      this.btnClassChange();
    }
    componentDidMount(){
      this.btnClassChange();
    }
    btnClassChange = () => {
      const actionMenu = document.querySelector('.actionDropdown');
      actionMenu.setAttribute("class","actionDropdown hb success ");
      actionMenu.parentElement.setAttribute("class","dropdown");
      const sortDropdown = document.querySelector('.sortDropdown');
      sortDropdown.setAttribute("class","sortDropdown");
      sortDropdown.parentElement.setAttribute("class","dropdown")
      const container = document.querySelector('.content-wrapper')
      container.setAttribute("class","content-wrapper ");
    }
    componentWillUnmount(){
      const container = document.querySelector('.content-wrapper')
      container.setAttribute("class","content-wrapper  ");
    }
    pagePosition = (index) => {
      this.setState({pageIndex : index || 0})
    }
    handleSaveClicked = () => {
      if(this.addTopologyRef.validate()){
          this.addTopologyRef.handleSave().then((topology)=>{
            if (topology.responseMessage !== undefined) {
              FSReactToastr.error(
                  <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
            } else {
                this.addTopologyRef.saveMetadata(topology.id).then(() => {
                  FSReactToastr.success(
                      <strong>Topology added successfully</strong>
                  )
                  this.context.router.push('applications/' + topology.id + '/edit');
                })
            }
        })
      }
    }

    render() {
        const {entities,filterValue,isLoading,fetchLoader,slideInput,pageSize,pageIndex} = this.state;
        const filteredEntities = TopologyUtils.topologyFilter(entities, filterValue);
        const splitData = _.chunk(filteredEntities,pageSize) || [];
        const btnIcon = <i className="fa fa-plus"></i>;
        const sortTitle = <span>Sort:<span style={{color: "#006ea0"}}>&nbsp;{this.state.sorted.text}</span></span>

        return (
            <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
                <div id="add-environment">
                  <DropdownButton title={btnIcon}
                      id="actionDropdown"
                      className="actionDropdown hb success"
                      noCaret
                    >
                        <MenuItem onClick={this.onActionMenuClicked.bind(this,"create")}>
                            &nbsp;New Application
                        </MenuItem>
                        <MenuItem onClick={this.onActionMenuClicked.bind(this,"import")}>
                            &nbsp;Import Application
                        </MenuItem>
                    </DropdownButton>
                </div>
                <div className="row">
                    <div className="page-title-box clearfix">
                        <div className="col-md-4 col-md-offset-5 text-right">
                            <FormGroup>
                                <InputGroup>
                                    <FormControl type="text"
                                      placeholder="Search by name"
                                      onKeyUp={this.onFilterChange}
                                      className={`inputAnimateIn ${(slideInput) ? "inputAnimateOut" : ''}`}
                                      onBlur={this.slideInputOut}
                                    />
                                    <InputGroup.Addon className="page-search">
                                        <Button type="button"
                                          className="searchBtn"
                                          onClick={this.slideInput}
                                        >
                                          <i className="fa fa-search"></i>
                                        </Button>
                                    </InputGroup.Addon>
                                </InputGroup>
                            </FormGroup>
                        </div>

                        <div className="col-md-2 text-center">
                          <DropdownButton title={sortTitle}
                            id="sortDropdown"
                            className="sortDropdown "
                          >
                              <MenuItem onClick={this.onSortByClicked.bind(this,"name")}>
                                  &nbsp;Name
                              </MenuItem>
                              <MenuItem active onClick={this.onSortByClicked.bind(this,"last_updated")}>
                                  &nbsp;Last Update
                              </MenuItem>
                              <MenuItem onClick={this.onSortByClicked.bind(this,"status")}>
                                  &nbsp;Status
                              </MenuItem>
                          </DropdownButton>
                        </div>
                        <div className="col-md-1 col-sm-3 text-left">
                        </div>
                    </div>
                </div>
                <div className="row">
                    {
                      (this.state.fetchLoader)
                      ? ''
                      : (splitData.length === 0)
                        ? <NoData/>
                        : splitData[pageIndex].map((list) => {
                            return <TopologyItems key={list.topology.id} topologyList={list} topologyAction={this.actionHandler} isLoading={isLoading}/>
                        })
}
                </div>
                {
                  (filteredEntities.length > pageSize)
                    ? <Paginate
                      len={filteredEntities.length}
                      splitData={splitData}
                      pagesize={pageSize}
                      pagePosition={this.pagePosition}
                    />
                  :''
                }
                <Modal ref={(ref) => this.AddTopologyModelRef = ref}
                  data-title="Add Stream"
                  data-resolve={this.handleSaveClicked}>
                  <AddTopology ref={(ref) => this.addTopologyRef = ref}/>
                </Modal>
                <input type="file"
                  ref={(ref) => this.importFileRef = ref}
                  className="displayNone"
                  accept=".json"
                  name="files"
                  title="Upload File"
                  onChange={this.handleImportTopology}
                />
                <a className="btn-download" ref="ExportTopology" hidden download href=""></a>
            </BaseContainer>
        );
    }
}

TopologyListingContainer.contextTypes = {
    router: React.PropTypes.object.isRequired
};

export default TopologyListingContainer;

TopologyListingContainer.defaultProps = {};
