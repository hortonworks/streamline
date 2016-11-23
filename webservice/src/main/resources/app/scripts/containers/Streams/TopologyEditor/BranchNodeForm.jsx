import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import { Table, Thead, Th, Tr, Td, unsafe } from 'reactable';
import Modal, {Confirm} from '../../../components/FSModal';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import {BtnDelete, BtnEdit} from '../../../components/ActionButtons';
import BranchRulesForm from './BranchRulesForm';
import {pageSize} from '../../../utils/Constants';

export default class BranchNodeForm extends Component {
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
            processAll: false,
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
                this.nodeData = results[0].entity;
                let configFields = results[0].entity.config.properties;
                let {rules = [], parallelism = 1} = configFields;

                let promise = [];
                rules.map(id=>{
                    promise.push(TopologyREST.getNode(topologyId, versionId, 'branchrules', id));
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
                    parallelism: parallelism ? parallelism : 1,
                    processAll: configFields.processAll ? true : false
                };

                //Found the edge connected to current node
                let allEdges = results[1].entities;
                this.allEdges = allEdges;

                let allStreams = results[2].entities;
                this.allStreams = allStreams;

                //find the input stream from connected edge
                this.edgeToNode = allEdges.filter((e)=>{return e.toId === nodeData.nodeId});
                this.parsedStream = '';
                this.parsedStream = _.find(allStreams, {id: this.edgeToNode[0].streamGroupings[0].streamId});
                if(this.nodeData.outputStreams.length > 0) {
                    this.streamData = this.nodeData.outputStreams[0];
                    this.context.ParentForm.setState({outputStreamObj:this.streamData})
                } else {
                    this.context.ParentForm.setState({outputStreamObj: {}})
                }
                this.setState(stateObj);
            })
    }

    validateData(){
        return true;
    }

    handleSave(name){
        let {topologyId, versionId, nodeType} = this.props;
        let promiseArr = [
            TopologyREST.getNode(topologyId, versionId, nodeType, this.nodeData.id)
        ];
        return Promise.all(promiseArr)
                .then(results=>{
                    this.nodeData = results[0].entity;
                    this.nodeData.name = name;
                    this.nodeData.config.properties.processAll = this.state.processAll;
                    //Update branch
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
                this.refs.BranchRuleModal.show();
            })
        }
    }

    handleDeleteRule(id){
        let {topologyId, versionId, nodeType, nodeData} = this.props;
        let stream = _.find(this.allStreams, {streamId: 'branch_processor_stream_'+id});
        let edge = _.find(this.allEdges, function(e) { return e.streamGroupings[0].streamId ===  stream.id});
        this.refs.Confirm.show({
            title: 'Are you sure you want to delete rule ?'
        }).then((confirmBox)=>{
            let promiseArr = [
                TopologyREST.deleteNode(topologyId, 'branchrules', id),
                TopologyREST.deleteNode(topologyId, 'streams', stream.id)
            ];
            if(edge) {
                promiseArr.push(TopologyREST.deleteNode(topologyId, 'edges', edge.id));
            }
            let rules = this.nodeData.config.properties.rules;
            rules.splice(rules.indexOf(id), 1);
            this.nodeData.outputStreamIds = [];
            this.nodeData.outputStreams.map((s)=>{
                this.nodeData.outputStreamIds.push(s.id);
            });
            this.nodeData.outputStreamIds.splice(this.nodeData.outputStreamIds.indexOf(stream.id), 1);
            delete this.nodeData.outputStreams;

            promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {body: JSON.stringify(this.nodeData)}));

            Promise.all(promiseArr)
                .then(result=>{
                    FSReactToastr.success(<strong>Rule deleted successfully</strong>);
                    this.fetchData();
                    if(edge)
                        this.props.graphEdges.splice(this.props.graphEdges.indexOf(edge), 1);
                    this.props.updateGraphMethod();
                })
            confirmBox.cancel();
        },()=>{})
    }

    handleSaveRule(){
        if(this.refs.RuleForm.validateData()){
            this.refs.RuleForm.handleSave().then((results)=>{
                if(results){
                    this.fetchData();
                    this.refs.BranchRuleModal.hide();
                }
            })
        }
    }

    changeProcessAll() {
        this.setState({processAll: !this.state.processAll});
    }

    render() {
        let {topologyId, versionId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
        let {rules, processAll} = this.state;
        return (
            <div className="modal-form processor-modal-form form-overflow">
                {editMode ?
                    <div className="clearfix row-margin-bottom">
                        <button type="button" onClick={this.handleAddRule.bind(this)} className="btn btn-success pull-left">
                            <i className="fa fa-plus"></i> Add New Rules
                        </button>
                        <label className="pull-right process-all-label">
                            <input
                				type="checkbox"
                				className=""
                				onChange={this.changeProcessAll.bind(this)}
                				value={processAll}
                				checked={processAll}
                            />
                            &nbsp;Process All
                        </label>
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
                                <Th column="condition">Condition</Th>
                                <Th column="action" className={!editMode ? 'displayNone' : null}>Actions</Th>
                            </Thead>
                            {rules.map((rule, i)=>{
                                return(
                                    <Tr key={i}>
                                        <Td column="name">{rule.name}</Td>
                                        <Td column="condition">{rule.condition}</Td>
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
                <Modal ref="BranchRuleModal" dialogClassName="rule-modal-fixed-height" bsSize="large" data-title={this.state.modalTitle} data-resolve={this.handleSaveRule.bind(this)}>
                    <BranchRulesForm
                        ref="RuleForm"
                        topologyId={topologyId}
                        versionId={versionId}
                        ruleObj={this.state.ruleObj}
                        nodeData={this.nodeData}
                        nodeType={nodeType}
                        parsedStream={this.parsedStream}
                    />
                </Modal>
                <Confirm ref="Confirm"/>
            </div>
        )
    }
}

BranchNodeForm.contextTypes = {
    ParentForm: React.PropTypes.object,
};
