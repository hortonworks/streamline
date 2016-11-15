import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Tabs, Tab} from 'react-bootstrap';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import Modal, {Confirm} from '../../../components/FSModal';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import OutputSchema from '../../../components/OutputSchemaComponent';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import RulesForm from './RulesForm';
import {pageSize} from '../../../utils/Constants';

export default class RulesNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
		sourceNode: PropTypes.array.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired
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
		let {topologyId, nodeType, nodeData} = this.props;
		let promiseArr = [
			TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId),
			TopologyREST.getAllNodes(topologyId, 'edges'),
			TopologyREST.getAllNodes(topologyId, 'streams')
		];

		Promise.all(promiseArr)
			.then((results)=>{
				this.nodeData = results[0].entity;
				let configFields = results[0].entity.config.properties;
				let {rules = [], parallelism = 1} = configFields;

				let promise = [];
				rules.map(id=>{
					promise.push(TopologyREST.getNode(topologyId, 'rules', id));
				})

				Promise.all(promise)
					.then(results=>{
						let ruleArr = [];
						results.map(result=>{
							ruleArr.push(result.entity);
						})
						this.setState({rules: ruleArr});
					})

				let stateObj = {
					parallelism: parallelism ? parallelism : 1
				};

				//Found all target edges connected from current node
				let allEdges = results[1].entities;
				this.nodeToOtherEdges = allEdges.filter((e)=>{return e.fromId === nodeData.nodeId});

				let allStreams = results[2].entities;

                //find all input streams from connected edges
                this.allEdgesToNode = allEdges.filter((e)=>{return e.toId === nodeData.nodeId});
                this.parsedStreams = [];
                this.allEdgesToNode.map((e)=>{
                    e.streamGroupings.map((g)=>{
                        this.parsedStreams.push(_.find(allStreams, {id: g.streamId}));
                    });
                });
				if(this.nodeData.outputStreams.length === 0 || this.nodeData.outputStreams.length < this.parsedStreams.length) {
					this.saveStreams();
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

        saveStreams() {
            let {topologyId, nodeType} = this.props;
            let promiseForStreams = [];
            this.parsedStreams.map((s, i)=>{
			let streamData = {
                                streamId: 'rule_processor_'+(this.nodeData.id)+'_stream_'+(i+1),
                                fields: s.fields
                        };
			//TODO - Need to find out exact streams that needs to be created and save only those
			if((i+1) > this.nodeData.outputStreams.length)
				promiseForStreams.push(TopologyREST.createNode(topologyId, 'streams', {body: JSON.stringify(streamData)}));
                        });
            Promise.all(promiseForStreams)
                .then(results=>{
                    this.nodeData.outputStreamIds = this.nodeData.outputStreams.map((s)=>{return s.id;}) || [];
                    results.map((s)=>{
                        this.nodeData.outputStreamIds.push(s.entity.id);
                        this.streamData = s.entity;
                        this.context.ParentForm.setState({outputStreamObj:this.streamData})
                    });
                    TopologyREST.updateNode(topologyId, nodeType, this.nodeData.id, {body: JSON.stringify(this.nodeData)})
                        .then((node)=>{
                            this.nodeData = node.entity;
                            this.setState({outputStreams: node.entity.outputStreams});
                        })
                })
        }

	validateData(){
		return true;
	}

	handleSave(name){
		let {topologyId, nodeType} = this.props;
		let promiseArr = [
			TopologyREST.getNode(topologyId, nodeType, this.nodeData.id)
		];
		return Promise.all(promiseArr)
			.then(results=>{
				this.nodeData = results[0].entity;
				this.nodeData.name = name;
				//Update rule processor
				return TopologyREST.updateNode(topologyId, nodeType, this.nodeData.id, {body: JSON.stringify(this.nodeData)});
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
		let {topologyId, nodeType, nodeData} = this.props;
		this.refs.Confirm.show({
			title: 'Are you sure you want to delete rule ?'
		}).then((confirmBox)=>{
			let promiseArr = [TopologyREST.deleteNode(topologyId, 'rules', id)];

			let rules = this.nodeData.config.properties.rules;
			rules.splice(rules.indexOf(id), 1);

			promiseArr.push(TopologyREST.updateNode(topologyId, nodeType, nodeData.nodeId, {body: JSON.stringify(this.nodeData)}));

			Promise.all(promiseArr)
				.then(result=>{
					FSReactToastr.success(<strong>Rule deleted successfully</strong>);
					this.fetchData();
				})
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

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {rules} = this.state;
		return (
                        <div className="modal-form processor-modal-form form-overflow">
                                {editMode ?
                                        <div className="clearfix row-margin-bottom">
                                                <button type="button" onClick={this.handleAddRule.bind(this)} className="btn btn-success pull-left">
                                                        <i className="fa fa-plus"></i> Add New Rules
                                                </button>
                                        </div>
                                : null}
                                <div className="row">
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
                                <Modal ref="RuleModal" dialogClassName="rule-modal-fixed-height" bsSize="large" data-title={this.state.modalTitle} data-resolve={this.handleSaveRule.bind(this)}>
					<RulesForm
						ref="RuleForm"
						topologyId={topologyId}
						ruleObj={this.state.ruleObj}
						nodeData={this.nodeData}
						nodeType={nodeType}
						parsedStreams={this.parsedStreams}
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
