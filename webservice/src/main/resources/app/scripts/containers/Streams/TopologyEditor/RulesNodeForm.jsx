import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Tabs, Tab} from 'react-bootstrap';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import Modal, {Confirm} from '../../../components/FSModal';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import RulesForm from './RulesForm';
import {pageSize} from '../../../utils/Constants';
import { Scrollbars } from 'react-custom-scrollbars';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';

export default class RulesNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
                versionId: PropTypes.number.isRequired,
		sourceNode: PropTypes.array.isRequired,
		targetNodes: PropTypes.array.isRequired,
                linkShuffleOptions: PropTypes.array.isRequired,
                graphEdges: PropTypes.array.isRequired,
                updateGraphMethod: PropTypes.func.isRequired
	};

	constructor(props) {
		super(props);
		let {editMode} = props;
		this.state = {
			parallelism: 1,
			editMode: editMode,
			rules: [],
			ruleObj: {},
			modalTitle: ''
		};
		this.fetchData();
	}

	fetchData() {
                let {topologyId, versionId, nodeType, nodeData} = this.props;
		let promiseArr = [
                        TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
                        TopologyREST.getAllNodes(topologyId, versionId, 'edges'),
                        TopologyREST.getAllNodes(topologyId, versionId, 'streams')
		];

		Promise.all(promiseArr)
			.then((results)=>{
                                this.nodeData = results[0];
                                let configFields = results[0].config.properties;
				let {rules = [], parallelism = 1} = configFields;

				let promise = [];
				rules.map(id=>{
                                        promise.push(TopologyREST.getNode(topologyId, versionId, 'rules', id));
				})

				Promise.all(promise)
					.then(results=>{
						let ruleArr = [];
						results.map(result=>{
                                                        ruleArr.push(result);
						})
						this.setState({rules: ruleArr});
					})

				let stateObj = {
					parallelism: parallelism ? parallelism : 1
				};

				//Found all target edges connected from current node
				let allEdges = results[1].entities;
                                this.allEdges = allEdges;
				this.nodeToOtherEdges = allEdges.filter((e)=>{return e.fromId === nodeData.nodeId});

				let allStreams = results[2].entities;
                                this.allStreams = allStreams;

                //find all input streams from connected edges
                this.allEdgesToNode = allEdges.filter((e)=>{return e.toId === nodeData.nodeId});
                this.parsedStreams = [];
                this.allEdgesToNode.map((e)=>{
                    e.streamGroupings.map((g)=>{
                        this.parsedStreams.push(_.find(allStreams, {id: g.streamId}));
                    });
                });
                                if(this.nodeData.outputStreams.length === 0) {
                                        this.context.ParentForm.setState({outputStreamObj: {}});
                                } else {
                                        this.streamData = this.nodeData.outputStreams[0];
                                        this.context.ParentForm.setState({outputStreamObj:this.streamData})
				}

				this.setState(stateObj);
			})
			.catch((err)=>{
				console.error(err);
			})
	}

	validateData(){
		return true;
	}

        handleSave(name, description){
                let {topologyId, versionId, nodeType} = this.props;
		let promiseArr = [
                        TopologyREST.getNode(topologyId, versionId, nodeType, this.nodeData.id)
		];
		return Promise.all(promiseArr)
			.then(results=>{
                                this.nodeData = results[0];
				this.nodeData.name = name;
                                this.nodeData.description = description;
				//Update rule processor
                                return TopologyREST.updateNode(topologyId, versionId, nodeType, this.nodeData.id, {body: JSON.stringify(this.nodeData)});
			})
	}

	handleAddRule(id){
		if(this.props.editMode){
			let ruleId = null;
			let ruleObj = {};
			let modalTitle = 'Add New Rule';
			if(typeof id === 'number' || typeof id === 'string'){
				ruleId = id;
				ruleObj = this.state.rules.filter((r)=>{ return r.id === id})[0];
				modalTitle = 'Edit Rule';
			}
			this.setState({ruleObj, modalTitle},()=>{
				this.refs.RuleModal.show();
			})
		}
	}

	handleDeleteRule(id){
                let {topologyId, versionId, nodeType, nodeData} = this.props;
                let transformStream = _.find(this.allStreams, {streamId: 'rule_transform_stream_'+id});
                let notifierStream = _.find(this.allStreams, {streamId: 'rule_notifier_stream_'+id});
                let edges = _.filter(this.allEdges, function(e) { return e.streamGroupings[0].streamId ===  transformStream.id || e.streamGroupings[0].streamId ===  notifierStream.id});
		this.refs.Confirm.show({
			title: 'Are you sure you want to delete rule ?'
		}).then((confirmBox)=>{
                        let promiseArr = [];
                        if(edges.length > 0) {
                                edges.map((e)=>{
                                        promiseArr.push(TopologyREST.deleteNode(topologyId, 'edges', e.id));
                                });
                        }
			Promise.all(promiseArr)
                        .then((edgeResult)=>{
                                let edgeSuccess = true;
                                if(edgeResult.responseMessage !== undefined){
                                        FSReactToastr.error(<CommonNotification flag="error" content={edgeResult.responseMessage}/>, '', toastOpt);
                                } else if(edges.length > 0) {
                                        edges.map((e)=>{
                                                this.props.graphEdges.splice(this.props.graphEdges.indexOf(e), 1);
                                        });
                                        this.props.updateGraphMethod();
                                }
                                if(edgeSuccess){
                                        TopologyREST.deleteNode(topologyId, 'rules', id)
                                        .then((ruleResult)=>{
                                                let ruleAPISuccess = true;
                                                if(ruleResult.responseMessage !== undefined){
                                                        ruleAPISuccess = false;
                                                        FSReactToastr.error(<CommonNotification flag="error" content={ruleResult.responseMessage}/>, '', toastOpt);
                                                }
                                                if(ruleAPISuccess){
                                                        let rules = this.nodeData.config.properties.rules;
                                                        rules.splice(rules.indexOf(id), 1);
                                                        this.nodeData.outputStreams = this.nodeData.outputStreams.filter((s)=>{
                                                            return s.id !== transformStream.id && s.id !== notifierStream.id;
                                                        });
                                                        TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {body: JSON.stringify(this.nodeData)})
                                                        .then((nodeResult)=>{
                                                                let nodeAPISuccess = true;
                                                                if(nodeAPISuccess.responseMessage !== undefined) {
                                                                nodeAPISuccess = false;
                                                                FSReactToastr.error(<CommonNotification flag="error" content={nodeResult.responseMessage}/>, '', toastOpt);
                                                                }
                                                                if(nodeAPISuccess) {
                                                                    var streamsPromiseArr = [TopologyREST.deleteNode(topologyId, 'streams', transformStream.id),
                                                                        TopologyREST.deleteNode(topologyId, 'streams', notifierStream.id)
                                                                        ];
                                                                        Promise.all(streamsPromiseArr)
                                                                        .then((streamResults)=>{
                                                                                let streamAPISuccess= true;
                                                                                streamResults.map((streamResult)=>{
                                                                                    if(streamResult.responseMessage !== undefined){
                                                                                        streamAPISuccess = false;
                                                                                        FSReactToastr.error(<CommonNotification flag="error" content={streamResult.responseMessage}/>, '', toastOpt);
                                                                                    }
                                                                                });
                                                                                if(streamAPISuccess ){
                                                                                        FSReactToastr.success(<strong>Rule deleted successfully</strong>);
                                                                                        this.fetchData();
                                                                                }
                                                                        });
                                                                }
                                                        });
                                                }
                                        });
                                }
                        });
			confirmBox.cancel();
		},()=>{})
	}

	handleSaveRule(){
		if(this.refs.RuleForm.validateData()){
			this.refs.RuleForm.handleSave().then((results)=>{
				if(results){
					this.fetchData();
					this.refs.RuleModal.hide();
				}
			})
		}
	}
  handleKeyPress = (event) => {
    if(event.key === "Enter"){
      this.refs.RuleModal.state.show ? this.handleSaveRule() : '';
    }
  }
	render() {
                let {topologyId, versionId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {rules} = this.state;
                return (<div>
                        <div className="modal-form processor-modal-form">
                            <Scrollbars autoHide
                              renderThumbHorizontal={props => <div {...props} style={{display : "none"}}/>}
                              >
                                {editMode ?
                                        <div className="clearfix row-margin-bottom customFormClass">
                                                <button type="button" onClick={this.handleAddRule.bind(this)} className="btn btn-success pull-left">
                                                        <i className="fa fa-plus"></i> Add New Rules
                                                </button>
                                        </div>
                                : null}
                                <div className="row customFormClass">
                                        <div className="col-sm-12">
                                                <Table
                                                        className="table table-hover table-bordered"
                                                        noDataText="No records found."
                                                        currentPage={0}
                                                        itemsPerPage={rules.length > pageSize ? pageSize : 0}
                                                        pageButtonLimit={5}
                                                >
                                                        <Thead>
                                                                <Th column="name">Name</Th>
                                                                <Th column="sql">SQL Query</Th>
                                                                <Th column="action" className={!editMode ? 'displayNone' : null}>Actions</Th>
                                                        </Thead>
                                                        {rules.map((rule, i)=>{
                                                                return(
                                                                        <Tr key={i}>
                                                                                <Td column="name">{rule.name}</Td>
                                                                                <Td column="sql">{rule.sql}</Td>
                                                                                <Td column="action" className={!editMode ? 'displayNone' : null}>
                                                                                        <div className="btn-action">
                                                                                                <BtnEdit callback={this.handleAddRule.bind(this, rule.id)}/>
                                                                                                <BtnDelete callback={this.handleDeleteRule.bind(this, rule.id)}/>
                                                                                        </div>
                                                                                </Td>
                                                                        </Tr>
                                                                )
                                                        })}
                                                </Table>
                                        </div>
                                </div>
                              </Scrollbars>
                        </div>
                                <Modal ref="RuleModal" onKeyPress={this.handleKeyPress} dialogClassName="rule-modal-fixed-height" bsSize="large" data-title={this.state.modalTitle} data-resolve={this.handleSaveRule.bind(this)}>
					<RulesForm
						ref="RuleForm"
						topologyId={topologyId}
                                                versionId={versionId}
						ruleObj={this.state.ruleObj}
						nodeData={this.nodeData}
						nodeType={nodeType}
						parsedStreams={this.parsedStreams}
                                                rules={rules}
					/>
				</Modal>
				<Confirm ref="Confirm"/>
			</div>
		)
	}
}

RulesNodeForm.contextTypes = {
    ParentForm: React.PropTypes.object,
};
