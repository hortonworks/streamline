import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import TopologyREST from '../../../rest/TopologyREST';
import TopologyUtils from '../../../utils/TopologyUtils';

import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';

CodeMirror.registerHelper("lint", "json", function(text) {
  var found = [];
  var {parser} = jsonlint;
  parser.parseError = function(str, hash) {
    var loc = hash.loc;
    found.push({from: CodeMirror.Pos(loc.first_line - 1, loc.first_column),
                to: CodeMirror.Pos(loc.last_line - 1, loc.last_column),
                message: str});
  };
  try { jsonlint.parse(text); }
  catch(e) {}
  return found;
});

export default class EdgeConfigContainer extends Component {
    constructor(props){
        super(props);
        let {data} = props;
        let streamsArr = [];
        this.topologyId = data.topologyId;
        this.target = data.target;

        let obj = {
            streamId: data.streamName ? data.streamName : '',
            streamFields: '',
            grouping: data.grouping ? data.grouping : 'SHUFFLE',
            rules: [],
            streamsArr: [],
            groupingsArr: [{value: "SHUFFLE", label: "SHUFFLE"}],
            rulesArr: [],
            showRules: false,
            showError: false,
                        sourceNode: {},
                        isEdit: data.edge.edgeId ? true : false
        }
        this.state = obj;
        this.setData();
    }

    setData() {
        let rules = [],
		rulesArr = [],
            rulesPromiseArr = [],
            showRules = false,
            nodeType = this.props.data.edge.source.currentType.toLowerCase();

                TopologyREST.getNode(this.topologyId, TopologyUtils.getNodeType(this.props.data.edge.source.parentType), this.props.data.edge.source.nodeId)
                        .then((result)=>{
                                let node = result.entity;
                                let streamsArr = [];
                                let fields = this.state.isEdit ? {} : node.outputStreams[0].fields;
                                let streamId = this.state.isEdit ? this.state.streamId : node.outputStreams[0].streamId;
                                node.outputStreams.map((s)=>{
                                        streamsArr.push({
                                                label: s.streamId,
                                                value: s.streamId,
                                                id: s.id,
                                                fields: s.fields
                                        });
                                        if(this.props.data.streamName === s.streamId)
                                                fields = s.fields;
                                });
                                this.setState({sourceNode: result.entity, streamsArr: streamsArr, streamId: streamId, streamFields: JSON.stringify(fields, null, "  ")});
                                if(nodeType === 'rule') {
                                        node.config.properties.rules.map((id)=>{
                                        rulesPromiseArr.push(TopologyREST.getNode(this.topologyId, 'rules', id));
                                        });
                                        Promise.all(rulesPromiseArr)
                                                .then((results)=>{
                                                        results.map((result)=>{
                                                                let data = result.entity;
                                                                rulesArr.push({
                                                                label: data.name,
                                                                value: data.name,
                                                                id: data.id
                                                                });
                                                                data.actions.map((actionObj)=>{
                                                                        if(actionObj.name === this.props.data.edge.target.uiname) {
                                                                                rules.push(data.name);
                                                                        }
                                                                });
                                                        });
                                                showRules =  true;
                                                this.setState({showRules: showRules, rulesArr: rulesArr, rules: rules});
                })
		}
    });
    }

    handleStreamChange(obj){
        if(obj) {
            this.setState({streamId: obj.value, streamFields: JSON.stringify(obj.fields, null, "  ")})
        } else this.setState({streamId: '', streamFields: ''});
    }
    handleGroupingChange(obj){
        if(obj) {
            this.setState({grouping: obj.value})
        } else this.setState({grouping: ''});
    }
    handleRulesChange(arr) {
        let rules = [];
        if(arr && arr.length){
            for(let r of arr){
                rules.push(r.value);
            }
            this.setState({rules: rules});
        } else {
            this.setState({rules: []});
        }
    }

    validate(){
        let {streamId, grouping, rules, showRules} = this.state;
        let validDataFlag = true;
        if(streamId.trim() === '' || grouping.trim === ''){
            validDataFlag = false;
        }
        if(showRules && rules.length === 0){
            validDataFlag = false;
        }
        if(!validDataFlag)
            this.setState({showError: true});
        return validDataFlag;
    }

    handleSave(){
        let {streamId, streamsArr, rules, sourceNode} = this.state;
        let {topologyId} = this.props.data;
        let streamObj = _.find(streamsArr, {value: streamId});
        let nodeType = this.props.data.edge.source.currentType.toLowerCase();
        let edgeData = {
            fromId: this.props.data.edge.source.nodeId,
            toId: this.props.data.edge.target.nodeId,
            streamGroupings: [{
                    streamId: streamObj.id,
                    grouping: 'SHUFFLE'
            }]
        };
                if(nodeType === 'window' || nodeType === 'rule'){
                        if(sourceNode.config.properties.rules && sourceNode.config.properties.rules.length > 0){
                let rulesPromiseArr = [];
                let saveRulesPromiseArr = [];
                                let type = nodeType === 'window' ? 'windows' : 'rules';
                                        sourceNode.config.properties.rules.map((id)=>{
                    rulesPromiseArr.push(TopologyREST.getNode(topologyId, type, id));
                })
                Promise.all(rulesPromiseArr)
                    .then((results)=>{
                        results.map((result)=>{
                            let data = result.entity;
                            if(type === 'rules') {
                                let actionObj = {
                                    name: this.props.data.edge.target.uiname,
                                    outputStreams: [streamObj.value]
                                };
                                if(this.props.data.edge.target.currentType.toLowerCase() === 'notification'){
                                                                        actionObj.outputFieldsAndDefaults = sourceNode.config.properties.fieldValues || {};
                                                                        actionObj.notifierName = sourceNode.config.properties.notifierName || '';
                                    actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.NotifierAction";
                                } else {
                                    actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.TransformAction";
                                    actionObj.transforms = [];
                                }
                                                                let obj = _.find(data.actions, {name: actionObj.name});
                                                                if(rules.indexOf(data.name) > -1 && !obj) {
                                                                        data.actions.push(actionObj);
                                                                } else if(rules.indexOf(data.name) === -1 && obj) {
                                                                        data.actions = [];
                                                                }
                                                                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, type, data.id, {body: JSON.stringify(data)}));
                            } else if(type === 'windows') {
                                let actionObj = {
                                    name: this.props.data.edge.target.uiname,
                                    outputStreams: [streamObj.value]
                                };
                                if(this.props.data.edge.target.currentType.toLowerCase() === 'notification'){
                                                                        actionObj.outputFieldsAndDefaults = sourceNode.config.properties.fieldValues || {};
                                                                        actionObj.notifierName = sourceNode.config.properties.notifierName || '';
                                    actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.NotifierAction";
                                } else {
                                    actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.TransformAction";
                                    actionObj.transforms = [];
                                }
                                data.actions.push(actionObj);
                                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, type, data.id, {body: JSON.stringify(data)}))
                            }
                        })
                        Promise.all(saveRulesPromiseArr).then((savedResults)=>{});
                    })
            }
        }

                if(this.state.isEdit) {
                        TopologyREST.updateNode(topologyId, 'edges', this.props.data.edge.edgeId, {body: JSON.stringify(edgeData)}).then((edge)=>{
              let edgeObj = _.find(this.props.data.edges, {edgeId: this.props.data.edge.edgeId});
              edgeObj.streamGrouping = edge.entity.streamGroupings[0];
            });

                } else {
                        TopologyREST.createNode(topologyId, 'edges', {body: JSON.stringify(edgeData)}).then((edge)=>{
                this.props.data.edge.edgeId = edge.entity.id;
                this.props.data.edge.streamGrouping = edge.entity.streamGroupings[0];
                this.props.data.edges.push(this.props.data.edge);
                //call the callback to update the graph
                this.props.data.callback();
            });
	}
        }

    render(){
        let {showRules, rules, rulesArr, streamId, streamsArr, grouping, groupingsArr} = this.state;
        const jsonoptions = {
            lineNumbers: true,
            mode: "application/json",
            styleActiveLine: true,
            gutters: ["CodeMirror-lint-markers"],
            readOnly: 'nocursor'
        }
        return(
            <form className="modal-form edge-modal-form">
                <div className="form-group">
                    <label>Stream ID <span className="text-danger">*</span></label>
                    <div>
                        <Select
                            value={streamId}
                            name='streamId'
                            options={streamsArr}
                            onChange={this.handleStreamChange.bind(this)}
                            clearable={false}
                            required={true}
                        />
                    </div>
                </div>
                <div className="form-group">
                    <label>Fields</label>
                    <div>
                        <ReactCodemirror ref="JSONCodemirror" value={this.state.streamFields} options={jsonoptions} />
                    </div>
                </div>
                {showRules ?
                <div className="form-group">
                    <label>Rules <span className="text-danger">*</span></label>
                    <div>
                        <Select
                            value={rules}
                            options={rulesArr}
                            onChange={this.handleRulesChange.bind(this)}
                            multi={true}
                            clearable={false}
                            joinValues={true}
                            required={true}
                        />
                    </div>
                </div>
                : null}
                <div className="form-group">
                    <label>Grouping <span className="text-danger">*</span></label>
                    <div>
                        <Select
                            value={grouping}
                            name='grouping'
                            options={groupingsArr}
                            onChange={this.handleGroupingChange.bind(this)}
                            clearable={false}
                            required={true}
                        />
                    </div>
                </div>
            </form>
        );
    }
}