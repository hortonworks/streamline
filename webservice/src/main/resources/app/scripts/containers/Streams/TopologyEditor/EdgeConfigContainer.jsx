import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import TopologyREST from '../../../rest/TopologyREST';

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
        this.node = data.node;
        this.target = data.target;
        this.node.outputStreams.map((s)=>{
            streamsArr.push({
                label: s.streamId,
                value: s.streamId,
                id: s.id,
                fields: s.fields
            })
        });
        let obj = {
            streamId: '',
            streamFields: '',
            grouping: 'SHUFFLE',
            rules: [],
            streamsArr: streamsArr,
            groupingsArr: [{value: "SHUFFLE", label: "SHUFFLE"}],
            rulesArr: [],
            showRules: false,
            showError: false
        }
        this.state = obj;
        this.setData();
    }

    setData() {
        let rules = [],
            rulesPromiseArr = [],
            showRules = false;

        if(this.node.type === 'RULE') {
            this.node.config.properties.rules.map((id)=>{
                rulesPromiseArr.push(TopologyREST.getNode(this.topologyId, 'rules', id));
            });
            Promise.all(rulesPromiseArr)
                .then((results)=>{
                    results.map((result)=>{
                        let data = result.entity;
                        rules.push({
                            label: data.name,
                            value: data.name,
                            id: data.id
                        });
                    })
                    showRules =  true;
                    this.setState({showRules: showRules, rulesArr: rules});
                })
        }
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
        let {streamId, streamsArr, rules} = this.state;
        let {topologyId} = this.props.data;
        let streamObj = _.find(streamsArr, {value: streamId});
        let edgeData = {
            fromId: this.props.data.edge.source.nodeId,
            toId: this.props.data.edge.target.nodeId,
            streamGroupings: [{
                    streamId: streamObj.id,
                    grouping: 'SHUFFLE'
            }]
        };
        if(this.node.type === 'WINDOW' ||this.node.type === 'RULE'){
            if(this.node.config.properties.rules && this.node.config.properties.rules.length > 0){
                let rulesPromiseArr = [];
                let saveRulesPromiseArr = [];
                let type = this.node.type === 'WINDOW' ? 'windows' : 'rules';
                this.node.config.properties.rules.map((id)=>{
                    rulesPromiseArr.push(TopologyREST.getNode(topologyId, type, id));
                })
                Promise.all(rulesPromiseArr)
                    .then((results)=>{
                        results.map((result)=>{
                            let data = result.entity;
                            if(type === 'rules' && rules.indexOf(data.name) > -1) {
                                let actionObj = {
                                    name: this.props.data.edge.target.uiname,
                                    outputStreams: [streamObj.value]
                                };
                                if(this.props.data.edge.target.currentType.toLowerCase() === 'notification'){
                                    actionObj.outputFieldsAndDefaults = this.node.config.properties.fieldValues || {};
                                    actionObj.notifierName = this.node.config.properties.notifierName || '';
                                    actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.NotifierAction";
                                } else {
                                    actionObj.__type = "org.apache.streamline.streams.layout.component.rule.action.TransformAction";
                                    actionObj.transforms = [];
                                }
                                data.actions.push(actionObj);
                                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, type, data.id, {body: JSON.stringify(data)}))
                            } else if(type === 'windows') {
                                let actionObj = {
                                    name: this.props.data.edge.target.uiname,
                                    outputStreams: [streamObj.value]
                                };
                                if(this.props.data.edge.target.currentType.toLowerCase() === 'notification'){
                                    actionObj.outputFieldsAndDefaults = this.node.config.properties.fieldValues || {};
                                    actionObj.notifierName = this.node.config.properties.notifierName || '';
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

        TopologyREST.createNode(topologyId, 'edges', {body: JSON.stringify(edgeData)})
            .then((edge)=>{
                this.props.data.edge.edgeId = edge.entity.id;
                this.props.data.edges.push(this.props.data.edge);
                //call the callback to update the graph
                this.props.data.callback();
            });
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
            </form>
        );
    }
}