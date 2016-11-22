import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Tabs, Tab} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import TopologyREST from '../../../rest/TopologyREST';
import Form from '../../../libs/form';
import StreamsSidebar from '../../../components/StreamSidebar';
import NotesForm from '../../../components/NotesForm';

export default class ProcessorNodeForm extends Component {
    static propTypes = {
        nodeData: PropTypes.object.isRequired,
        editMode: PropTypes.bool.isRequired,
        nodeType: PropTypes.string.isRequired,
        topologyId: PropTypes.string.isRequired,
        versionId: PropTypes.number.isRequired,
        sourceNodes: PropTypes.array.isRequired
    };

    constructor(props) {
        super(props);
        this.sourceNodesId = [];
        props.sourceNodes.map((node)=>{
            this.sourceNodesId.push(node.nodeId);
        })
        this.fetchData();
        this.outputStreamObj = {};
        this.state = {
            streamObj: {},
            outputStreamObj: {}
        };
    }
    getChildContext() {
        return {
            ParentForm: this,
        };
    }

    componentWillUpdate(){
        // this.outputStreamObj = this.refs.ProcessorChildElement.streamData || {};
    }

    fetchData(){
        let {topologyId, versionId, nodeType, nodeData} = this.props;
        let promiseArr = [
            TopologyREST.getAllNodes(topologyId, versionId, 'edges')
        ];
        Promise.all(promiseArr)
            .then(results=>{
                if(results[0].entities){
                    results[0].entities.map((edge)=>{
                        if(edge.toId === nodeData.nodeId && this.sourceNodesId.indexOf(edge.fromId) !== -1){
                            //TODO - Once we support multiple input streams, need to fix this.
                            TopologyREST.getNode(topologyId, versionId, 'streams', edge.streamGroupings[0].streamId)
                                .then(streamResult=>{
                                    this.setState({streamObj: streamResult.entity})
                                    // this.refs.StreamSidebarInput.update(streamResult.entity);
                                })
                        }
                    })
                }
            })
    }

    validateData(){
        let validDataFlag = true;
        if(!this.refs.ProcessorChildElement.validateData()){
            validDataFlag = false;
        }
        return validDataFlag;
    }

    handleSave(name){
        return this.refs.ProcessorChildElement.handleSave(name);
    }

    render() {
        let childElement = this.props.getChildElement();
        let streamObj = this.state.streamObj instanceof Array ? this.state.streamObj[0] : this.state.streamObj;
        return (
            <Tabs id="ProcessorForm" defaultActiveKey={1} className="modal-tabs">
                <Tab eventKey={1} title="CONFIGURATION">
                    <StreamsSidebar ref="StreamSidebarInput" streamObj={streamObj} streamType="input" />
                    {childElement}
                    <StreamsSidebar ref="StreamSidebarOutput" streamObj={this.state.outputStreamObj} streamType="output" />
                </Tab>
                <Tab eventKey={2} title="NOTES">
                    <NotesForm />
                </Tab>
            </Tabs>
        )
    }
}

ProcessorNodeForm.childContextTypes = {
    ParentForm: React.PropTypes.object,
};
