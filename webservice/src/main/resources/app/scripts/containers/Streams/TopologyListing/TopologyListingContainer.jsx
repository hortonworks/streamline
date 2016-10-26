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
    Glyphicon
} from 'react-bootstrap';

/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants'

class TopologyItems extends Component {
    constructor(props) {
        super(props)
    }

    onActionClick = (eventKey) => {
        this.props.topologyAction(eventKey, this.streamRef.dataset.id)
    }
    render() {
        const ellipseIcon = <i className="fa fa-ellipsis-v"></i>;
        const {topologyAction, topologyList, isLoading} = this.props;
        const {topology, metric} = topologyList;
        const metricWrap = metric || {
            misc: (metric === undefined)
                ? ''
                : metric.misc
        };

        return (
            <div className="col-sm-4">
                <div className="stream-box" data-id={topology.id} ref={(ref) => this.streamRef = ref}>
                    <div className="stream-head clearfix">
                        <div className="pull-left">
                            <Link to={`applications/${topology.id}`}>
                                <h4>{Utils.capitaliseFirstLetter(topology.name)}</h4>
                            </Link>
                            <h5>
                                {(metricWrap.uptime === undefined)
                                    ? Utils.splitTimeStamp(topology.timestamp)
                                    : Utils.splitSeconds(metricWrap.uptime)
}
                            </h5>
                        </div>
                        <div className="pull-right">
                            <div className="stream-actions">
                                <DropdownButton noCaret title={ellipseIcon} id="dropdown" bsStyle="link" className="hb dropdown-toggle" onSelect={this.onActionClick}>
                                    <MenuItem eventKey={`refresh/${topology.id}`}>
                                        <i className="fa fa-refresh"></i>
                                        &nbsp;Refresh
                                    </MenuItem>
                                    <MenuItem eventKey={`delete/${topology.id}`}>
                                        <i className="fa fa-trash"></i>
                                        &nbsp;Delete
                                    </MenuItem>
                                </DropdownButton>
                                <div className={`stream-status ${ (isLoading.loader && (isLoading.idCheck === topology.id))
                                    ? "REFRESH"
                                    : metricWrap.status || 'NOTRUNNING'}`}>
                                    {(isLoading.loader && (isLoading.idCheck === topology.id))
                                        ? <span>
                                                <i className="fa fa-refresh fa-spin"></i>
                                                REFRESHING</span>
                                        : metricWrap.status || 'NOT RUNNING'
}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="stream-body">
                        <div className="row">
                            <div className="col-lg-4 stream-stats">
                                <h6>Emitted</h6>
                                <h3>{metricWrap.misc.emitted || 0}</h3>
                            </div>
                            <div className="col-lg-4 stream-stats">
                                <h6>Latency</h6>
                                <h3>{metricWrap.latency || 0}</h3>
                            </div>
                            <div className="col-lg-4 stream-stats">
                                <h6>Transferred</h6>
                                <h3>{metricWrap.misc.transferred || 0}</h3>
                            </div>
                        </div>
                        <div className="row">
                            <div className="col-lg-4 stream-stats">
                                <h6>Errors</h6>
                                <h3 className="color-error">{metricWrap.failedRecords || 0}</h3>
                            </div>
                            <div className="col-lg-4 stream-stats">
                                <h6>Worker</h6>
                                <h3>{metricWrap.misc.workersTotal || 0}</h3>
                            </div>
                            <div className="col-lg-4 stream-stats">
                                <h6>Executor</h6>
                                <h3>{metricWrap.misc.executorsTotal || 0}</h3>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

TopologyItems.propTypes = {
    topologyList: React.PropTypes.object.isRequired,
    topologyAction: React.PropTypes.func.isRequired
}

class TopologyListingContainer extends Component {
    constructor(props) {
        super();
        this.state = {
            entities: [],
            filterValue: '',
            isLoading: {
                loader: false,
                idCheck: ''
            }
        }

        this.fetchData();
    }

    fetchData() {
        TopologyREST.getAllTopology().then((topology) => {
            if (topology.responseCode !== 1000) {
                FSReactToastr.error(
                    <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
            } else {
                let result = Utils.sortArray(topology.entities.slice(), 'timestamp', false);
                this.setState({entities: result});
            }
        }).catch((err) => {
            FSReactToastr.error(
                <CommonNotification flag="error" content={err}/>, '', toastOpt)
        });
    }

    onFilterChange = (e) => {
        this.setState({filterValue: e.target.value.trim()})
    }

    fetchSingleTopology = (ID) => {
        const id = +ID;
        let flagUpdate = {
            loader: true,
            idCheck: id
        }
        this.setState({isLoading: flagUpdate});
        TopologyREST.getTopology(id).then((topology) => {
            this.updateSingleTopology(topology.entity, id)
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
        let rootdirKeyName = 'hbaseConf';
        let configObj = {
            "local.parser.jar.path": "/tmp",
            "local.notifier.jar.path": "/tmp"
        };
        configObj[rootdirKeyName] = {
            "hbase.rootdir": "hdfs://localhost:9000/tmp/hbase"
        }
        let data = {
            name: 'Application-Name',
            config: JSON.stringify(configObj)
        }
        TopologyREST.postTopology({body: JSON.stringify(data)}).then((topology) => {
            if (topology.responseCode !== 1000) {
                FSReactToastr.error(
                    <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
            } else {
                let metaData = {
                    topologyId: topology.entity.id,
                    data: JSON.stringify({sources: [], processors: [], sinks: []})
                }
                TopologyREST.postMetaInfo({body: JSON.stringify(metaData)}).then(() => {
                    FSReactToastr.success(
                        <strong>Topology added successfully</strong>
                    )
                    this._reactInternalInstance._context.router.push('applications/' + topology.entity.id);
                })
            }
        });
    }

    deleteSingleTopology = (id) => {
        this.refs.BaseContainer.refs.Confirm.show({title: 'Are you sure you want to delete ?'}).then((confirmBox) => {
            TopologyREST.deleteTopology(id).then((topology) => {
                TopologyREST.deleteMetaInfo(id);
                this.fetchData();
                confirmBox.cancel();
                if (topology.responseCode !== 1000) {
                    FSReactToastr.error(
                        <CommonNotification flag="error" content={topology.responseMessage}/>, '', toastOpt)
                } else {
                    FSReactToastr.success(
                        <strong>Topology deleted successfully</strong>
                    )
                    console.log("topology has been deleted ", id)
                }
            }).catch((err) => {
                FSReactToastr.error(
                    <CommonNotification flag="error" content={err}/>, '', toastOpt)
            })
        })
    }

    actionHandler = (eventKey, id) => {
        event.preventDefault();
        const key = eventKey.split('/');
        switch (key[0]) {
            case "refresh":
                this.fetchSingleTopology(id);
                break;
            case "delete":
                this.deleteSingleTopology(id);
                break;
            default:
                break;
        }
    }
    render() {
        const {entities, filterValue, isLoading} = this.state;
        const filteredEntities = TopologyUtils.topologyFilter(entities, filterValue);

        return (
            <BaseContainer ref="BaseContainer" routes={this.props.routes} headerContent={this.props.routes[this.props.routes.length - 1].name}>
                <div className="row">
                    <div className="page-title-box clearfix">
                        <div className="col-md-4 col-md-offset-6 text-right">
                            <FormGroup>
                                <InputGroup>
                                    <FormControl type="text" placeholder="Search by name" onKeyUp={this.onFilterChange}/>
                                    <InputGroup.Addon>
                                        <i className="fa fa-search"></i>
                                    </InputGroup.Addon>
                                </InputGroup>
                            </FormGroup>
                        </div>
                        <div className="col-md-2 col-sm-3 text-right">
                            <button className="btn btn-success" onClick={this.handleAddTopology.bind(this)}>
                                <i className="fa fa-plus-circle"></i>
                                &nbsp;New Application
                            </button>
                        </div>
                    </div>
                </div>
                <div className="row">
                    {(filteredEntities.length === 0)
                        ? <NoData/>
                        : filteredEntities.map((list) => {
                            return <TopologyItems key={list.topology.id} topologyList={list} topologyAction={this.actionHandler} isLoading={isLoading}/>
                        })
}
                </div>
            </BaseContainer>
        );
    }
}

export default TopologyListingContainer;

TopologyListingContainer.defaultProps = {};
