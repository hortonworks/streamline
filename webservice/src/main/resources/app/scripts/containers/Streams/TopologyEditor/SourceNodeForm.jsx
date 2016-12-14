import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Tabs, Tab} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import TopologyREST from '../../../rest/TopologyREST';
import Form from '../../../libs/form';
import StreamsSidebar from '../../../components/StreamSidebar';
import NotesForm from '../../../components/NotesForm';

export default class SourceNodeForm extends Component {
    static propTypes = {
        nodeData: PropTypes.object.isRequired,
        configData: PropTypes.object.isRequired,
        editMode: PropTypes.bool.isRequired,
        nodeType: PropTypes.string.isRequired,
        topologyId: PropTypes.string.isRequired,
        versionId: PropTypes.number.isRequired
    };

    constructor(props) {
        super(props);
        this.fetchData();
        this.configJSON = props.configData.topologyComponentUISpecification.fields;
        this.state = {
            formData: {},
            streamObj: {},
            description: '',
            showRequired: true,
            activeTabKey: 1
        };
    }

    fetchData(){
        let {topologyId, versionId, nodeType, nodeData} = this.props;
        TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId)
            .then(results=>{
                this.nodeData = results;
                let stateObj = {};
                if(this.nodeData.outputStreams.length === 0){
                    this.createStream();
                } else {
                    this.streamObj = this.nodeData.outputStreams[0];
                    stateObj.streamObj = this.streamObj;
                }
                stateObj.formData = this.nodeData.config.properties
                stateObj.description = this.nodeData.description;
                this.setState(stateObj);
            })
    }

    createStream(){
        let {topologyId, versionId, nodeType} = this.props;
        let streamData = { streamId: this.props.configData.subType.toLowerCase()+'_stream_'+this.nodeData.id, fields: []};
        TopologyREST.createNode(topologyId, versionId, 'streams', {body: JSON.stringify(streamData)})
            .then(result=>{
                this.nodeData.outputStreamIds = [result.id];
                TopologyREST.updateNode(topologyId, versionId, nodeType, this.nodeData.id, {body: JSON.stringify(this.nodeData)})
                    .then((node)=>{
                        this.nodeData = node;
                        this.streamObj = this.nodeData.outputStreams[0];
                        this.setState({streamObj: this.streamObj});
                    })
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
        let {topologyId, versionId, nodeType} = this.props;
        let nodeId = this.nodeData.id;
        let data = this.refs.Form.state.FormData;
        this.nodeData.config.properties = data;
        this.nodeData.name = name;
        this.nodeData.outputStreams = [{
            fields: this.streamObj.fields,
            streamId: this.streamObj.streamId,
            id: this.nodeData.outputStreams[0].id,
            topologyId: topologyId
        }]
        this.nodeData.description = this.state.description;
        let promiseArr = [
            TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)}),
            TopologyREST.updateNode(topologyId, versionId, 'streams', this.nodeData.outputStreams[0].id, {body: JSON.stringify(this.streamObj)})
        ];
        return Promise.all(promiseArr);
    }

    showOutputStream(resultArr){
        this.streamObj = {
            streamId: this.props.configData.subType.toLowerCase()+'_stream_'+this.nodeData.id,
            fields: resultArr,
            id: this.nodeData.outputStreams[0].id
        };
        this.setState({streamObj: this.streamObj});
        // this.refs.StreamSidebar.update(this.streamObj);
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

    handleNotesChange(description) {
        this.setState({description: description});
    }

    render() {
        let formData = this.state.formData;
        let fields = Utils.genFields(this.configJSON, [], formData);
        const form = <Form
                        ref="Form"
                        readOnly={!this.props.editMode}
                        showRequired={this.state.showRequired}
                        FormData={formData}
                        className="source-modal-form form-overflow"
                        callback={this.showOutputStream.bind(this)}
                    >
                        {fields}
                    </Form>
        const outputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={this.state.streamObj} streamType="output" />
        return (
            <Tabs id="SinkForm" activeKey={this.state.activeTabKey} className="modal-tabs" onSelect={this.onSelectTab}>
                <Tab eventKey={1} title="REQUIRED">
                    {outputSidebar}
                    {form}
                </Tab>
                <Tab eventKey={2} title="OPTIONAL">
                    {outputSidebar}
                    {form}
                </Tab>
                <Tab eventKey={3} title="NOTES">
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
