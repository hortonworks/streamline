import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Tabs, Tab} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import TopologyREST from '../../../rest/TopologyREST';
import Form from '../../../libs/form';
import StreamsSidebar from '../../../components/StreamSidebar';
import NotesForm from '../../../components/NotesForm';

export default class SinkNodeForm extends Component {
    static propTypes = {
        nodeData: PropTypes.object.isRequired,
        configData: PropTypes.object.isRequired,
        editMode: PropTypes.bool.isRequired,
        nodeType: PropTypes.string.isRequired,
        topologyId: PropTypes.string.isRequired,
        sourceNodes: PropTypes.array.isRequired
    };

    constructor(props) {
        super(props);
        this.sourceNodesId = [];
        props.sourceNodes.map((node)=>{
            this.sourceNodesId.push(node.nodeId);
        })
        this.fetchData();
        this.state = {
            formData: {},
            streamObj: {},
            showRequired: true,
            activeTabKey: 1
        };
    }

    fetchData(){
        let {topologyId, nodeType, nodeData, sourceNodes} = this.props;
        let sourceNodeType = null;
        let promiseArr = [
            TopologyREST.getNode(topologyId, nodeType, nodeData.nodeId),
            TopologyREST.getAllNodes(topologyId, 'edges'),
        ];
        if(sourceNodes.length > 0){
            sourceNodeType = TopologyUtils.getNodeType(sourceNodes[0].parentType);
            promiseArr.push(TopologyREST.getNode(topologyId, sourceNodeType, sourceNodes[0].nodeId));
        }
        Promise.all(promiseArr)
            .then(results=>{
                this.nodeData = results[0].entity;
                if(results[1].entities){
                    results[1].entities.map((edge)=>{
                        if(edge.toId === nodeData.nodeId && this.sourceNodesId.indexOf(edge.fromId) !== -1){
                            //TODO - Once we support multiple input streams, need to fix this.
                            TopologyREST.getNode(topologyId, 'streams', edge.streamGroupings[0].streamId)
                                .then(streamResult=>{
                                    this.setState({streamObj: streamResult.entity});
                                })
                        }
                    })
                }
                this.setState({formData: this.nodeData.config.properties})
                if(sourceNodes.length > 0){
                    //Finding the source node and updating actions for rules/windows
                    this.sourceNodeData = results[2].entity;
                    let sourcePromiseArr = [];
                    // sourceChildNodeType are processor nodes inner child, window or rule
                    this.sourceChildNodeType = sourceNodes[0].currentType.toLowerCase() === 'window' ? 'windows' : 'rules';
                    if(this.sourceNodeData.config.properties && this.sourceNodeData.config.properties.rules && this.sourceNodeData.config.properties.rules.length > 0){
                        this.sourceNodeData.config.properties.rules.map((id)=>{
                            sourcePromiseArr.push(TopologyREST.getNode(topologyId, this.sourceChildNodeType, id));
                        })
                    }
                    Promise.all(sourcePromiseArr)
                        .then(sourceResults=>{
                            this.allSourceChildNodeData = sourceResults;
                        })
                }
            })
    }

    validateData(){
        let validDataFlag = true;
        if(!this.refs.Form.validate()){
            validDataFlag = false;
            this.setState({activeTabKey: 1, showRequired: true});
        }
        return validDataFlag;
    }

    handleSave(name){
        let {topologyId, nodeType, nodeData} = this.props;
        let nodeId = this.nodeData.id;
        let data = this.refs.Form.state.FormData;
        this.nodeData.config.properties = data;
        let oldName = this.nodeData.name;
        this.nodeData.name = name;
        let promiseArr = [TopologyREST.updateNode(topologyId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)})];
        if(this.allSourceChildNodeData && this.allSourceChildNodeData.length > 0){
            this.allSourceChildNodeData.map((childData)=>{
                let child = childData.entity;
                let obj = child.actions.find((o)=>{return o.name == oldName});
                if(obj){
                    obj.name = name;
                    if(nodeData.currentType.toLowerCase() == 'notification'){
                        obj.outputFieldsAndDefaults = this.nodeData.config.properties.fieldValues || {};
                        obj.notifierName = this.nodeData.config.properties.notifierName || '';
                    }
                    promiseArr.push(TopologyREST.updateNode(topologyId, this.sourceChildNodeType, child.id, {body: JSON.stringify(child)}));
                } else {
                    console.error("Missing actions object for "+name);
                }
            })
        }
        return Promise.all(promiseArr);
    }

    onSelectTab = (eventKey) => {
        if(eventKey == 1){
            this.setState({activeTabKey: 1, showRequired: true})
        }else if(eventKey == 2){
            this.setState({activeTabKey: 2, showRequired: false})
        } else if(eventKey == 3){
            this.setState({activeTabKey: 3})
        }
    }

    render() {
        let {configData} = this.props;
        let formData = this.state.formData;
        let fields = Utils.genFields(configData.topologyComponentUISpecification.fields, [], formData);
        const form = <Form
                        ref="Form"
                        readOnly={!this.props.editMode}
                        showRequired={this.state.showRequired}
                        FormData={formData}
                        className="sink-modal-form"
                    >
                        {fields}
                    </Form>
        const inputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={this.state.streamObj} streamType="input" />
        return (
            <Tabs id="SinkForm" activeKey={this.state.activeTabKey} className="modal-tabs" onSelect={this.onSelectTab}>
                <Tab eventKey={1} title="REQUIRED">
                    {inputSidebar}
                    {form}
                </Tab>
                <Tab eventKey={2} title="OPTIONAL">
                    {inputSidebar}
                    {form}
                </Tab>
                <Tab eventKey={3} title="NOTES">
                    <NotesForm />
                </Tab>
            </Tabs>
        )
    }
}