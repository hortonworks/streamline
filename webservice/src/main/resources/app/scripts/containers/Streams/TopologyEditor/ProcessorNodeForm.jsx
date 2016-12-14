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
            description: '',
            outputStreamObj: {},
            inputStreamOptions: []
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
            TopologyREST.getNode(topologyId, versionId, 'processors', nodeData.nodeId),
            TopologyREST.getAllNodes(topologyId, versionId, 'edges')
        ];
        Promise.all(promiseArr)
            .then(results=>{
                let description = results[0].description ? results[0].description : '';
                if(results[1].entities){
                    var streamsPromiseArr = [],
                        inputStreams = [],
                        inputStreamOptions = [];
                    results[1].entities.map((edge)=>{
                        if(edge.toId === nodeData.nodeId && this.sourceNodesId.indexOf(edge.fromId) !== -1){
                            streamsPromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'streams', edge.streamGroupings[0].streamId));
                        }
                    })
                    Promise.all(streamsPromiseArr)
                    .then((streamResults)=>{
                        streamResults.map((result)=>{
                            let s = result;
                            inputStreams.push(s);
                        });
                        this.setState({streamObj: inputStreams[0], inputStreamOptions: inputStreams});
                        // this.refs.StreamSidebarInput.update(streamResult);
                    })
                }
                this.setState({description: description});
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
        let description = this.state.description;
        return this.refs.ProcessorChildElement.handleSave(name, description);
    }

    handleNotesChange(description) {
        this.setState({description: description});
    }

    render() {
        let childElement = this.props.getChildElement();
        let streamObj = this.state.streamObj;
        return (
            <Tabs id="ProcessorForm" defaultActiveKey={1} className="modal-tabs">
                <Tab eventKey={1} title="CONFIGURATION">
                    <StreamsSidebar ref="StreamSidebarInput" streamObj={streamObj} inputStreamOptions={this.state.inputStreamOptions} streamType="input" />
                    {childElement}
                    <StreamsSidebar ref="StreamSidebarOutput" streamObj={this.state.outputStreamObj} streamType="output" />
                </Tab>
                <Tab eventKey={2} title="NOTES">
                    <NotesForm
                        ref="NotesForm"
                        description={this.state.description}
                        onChangeDescription={this.handleNotesChange.bind(this)}
                    />
                </Tab>
            </Tabs>
        )
    }
}

ProcessorNodeForm.childContextTypes = {
    ParentForm: React.PropTypes.object
};
