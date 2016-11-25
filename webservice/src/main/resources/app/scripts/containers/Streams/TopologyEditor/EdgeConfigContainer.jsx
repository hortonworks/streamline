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
        this.versionId = data.versionId;
        this.target = data.target;

        let obj = {
            streamId: data.streamName ? data.streamName : '',
            streamFields: '',
            grouping: data.grouping ? data.grouping : 'SHUFFLE',
            rules: [],
            streamsArr: [],
            groupingsArr: [{value: "SHUFFLE", label: "SHUFFLE"},{value: "FIELDS", label: "FIELDS"}],
            groupingFieldsArr: [],
            groupingFields: data.groupingFields ? data.groupingFields : [],
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

        TopologyREST.getNode(this.topologyId, this.versionId, TopologyUtils.getNodeType(this.props.data.edge.source.parentType), this.props.data.edge.source.nodeId)
            .then((result)=>{
                let node = result;
                let streamsArr = [];
                let fields = this.state.isEdit ? {} : node.outputStreams[0].fields;
                let streamId = this.state.isEdit ? this.state.streamId : node.outputStreams[0].streamId;
                let groupingFieldsArr = [];
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
                fields.map((f)=>{
                    groupingFieldsArr.push({value: f.name, label: f.name});
                });
                this.setState({
                    sourceNode: result,
                    streamsArr: streamsArr,
                    streamId: streamId,
                    streamFields: JSON.stringify(fields, null, "  "),
                    groupingFieldsArr: groupingFieldsArr
                });
                if(nodeType === 'rule' || nodeType === 'branch') {
                    let type = nodeType === 'rule' ? 'rules' : 'branchrules';
                    node.config.properties.rules.map((id)=>{
                    rulesPromiseArr.push(TopologyREST.getNode(this.topologyId, this.versionId, type, id));
                });
                Promise.all(rulesPromiseArr)
                .then((results)=>{
                    results.map((result)=>{
                        let data = result;
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
                    if(nodeType === 'branch') {
                        let id = streamId.split('_')[3];
                        var ruleObject = _.find(rulesArr, {id: parseInt(id, 10)});
                    }
                    showRules =  true;
                    this.setState({showRules: showRules, rulesArr: rulesArr, rules: ruleObject ? ruleObject.value : rules});
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
    handleBranchRulesChange(obj) {
        if(obj) {
            let id = obj.id;
            var streamObject = _.find(this.state.streamsArr, {value: 'branch_processor_stream_'+id});
            this.setState({rules: obj.value, streamId: streamObject.value, streamFields: JSON.stringify(streamObject.fields, null, "  ")});
        } else this.setState({rules: [], streamId: '', streamFields: ''});
    }
    handleGroupingFieldsChange(arr) {
       let groupingFields = [];
       if(arr && arr.length) {
           arr.map((f)=>{
               groupingFields.push(f.value);
           });
           this.setState({groupingFields: groupingFields});
       } else {
           this.setState({groupingFields: ''});
       }
   }

    validate(){
        let {streamId, grouping, rules, showRules, groupingFields} = this.state;
        let validDataFlag = true;
        if(streamId.trim() === '' || grouping.trim === ''){
            validDataFlag = false;
        }
        if(showRules && rules.length === 0){
            validDataFlag = false;
        }
        if(grouping === 'FIELDS' && groupingFields === '') {
            validDataFlag = false;
        }
        if(!validDataFlag)
            this.setState({showError: true});
        return validDataFlag;
    }

    handleSave(){
        let {streamId, streamsArr, rules, sourceNode, grouping, groupingFields} = this.state;
        let {topologyId, versionId} = this.props.data;
        let streamObj = _.find(streamsArr, {value: streamId});
        let nodeType = this.props.data.edge.source.currentType.toLowerCase();
        let edgeData = {
            fromId: this.props.data.edge.source.nodeId,
            toId: this.props.data.edge.target.nodeId,
            streamGroupings: [{
                streamId: streamObj.id,
                grouping: grouping
            }]
        };
        if(grouping === "FIELDS")
            edgeData.streamGroupings[0].fields = groupingFields;
        if(nodeType === 'window' || nodeType === 'rule' || nodeType === 'branch'){
            if(sourceNode.config.properties.rules && sourceNode.config.properties.rules.length > 0){
                let rulesPromiseArr = [];
                let saveRulesPromiseArr = [];
                let type = nodeType === 'window' ? 'windows' : (nodeType === 'rule' ? 'rules' : 'branchrules');
                sourceNode.config.properties.rules.map((id)=>{
                    rulesPromiseArr.push(TopologyREST.getNode(topologyId, versionId, type, id));
                })
                Promise.all(rulesPromiseArr)
                    .then((results)=>{
                        results.map((result)=>{
                            let data = result;
                            if(type === 'rules' || type === 'branchrules') {
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
                                if(type === 'branchrules') {
                                    if(rules === data.name && !obj) {
                                        data.actions.push(actionObj);
                                    } else if(rules !== data.name && obj) {
                                        data.actions = [];
                                    }
                                } else {
                                    if(rules.indexOf(data.name) > -1 && !obj) {
                                        data.actions.push(actionObj);
                                    } else if(rules.indexOf(data.name) === -1 && obj) {
                                        data.actions = [];
                                    }
                                }
                                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, type, data.id, {body: JSON.stringify(data)}));
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
                                saveRulesPromiseArr.push(TopologyREST.updateNode(topologyId, versionId, type, data.id, {body: JSON.stringify(data)}))
                            }
                        })
                        Promise.all(saveRulesPromiseArr).then((savedResults)=>{});
                    })
            }
        }
        if(this.state.isEdit) {
            TopologyREST.updateNode(topologyId, versionId, 'edges', this.props.data.edge.edgeId, {body: JSON.stringify(edgeData)}).then((edge)=>{
            let edgeObj = _.find(this.props.data.edges, {edgeId: this.props.data.edge.edgeId});
              edgeObj.streamGrouping = edge.streamGroupings[0];
            });
        } else {
            TopologyREST.createNode(topologyId, versionId, 'edges', {body: JSON.stringify(edgeData)}).then((edge)=>{
                this.props.data.edge.edgeId = edge.id;
                this.props.data.edge.streamGrouping = edge.streamGroupings[0];
                this.props.data.edges.push(this.props.data.edge);
                //call the callback to update the graph
                this.props.data.callback();
            });
       }
    }

    render(){
        let {showRules, rules, rulesArr, streamId, streamsArr, grouping, groupingsArr, groupingFields, groupingFieldsArr} = this.state;
        let nodeType = this.props.data.edge.source.currentType.toLowerCase();
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
                            disabled={nodeType === 'branch' ? true : false}
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
                        { nodeType === 'branch' ?
                            <Select
                                value={rules}
                                options={rulesArr}
                                onChange={this.handleBranchRulesChange.bind(this)}
                                clearable={false}
                                required={true}
                            />
                        :
                            <Select
                                value={rules}
                                options={rulesArr}
                                onChange={this.handleRulesChange.bind(this)}
                                multi={true}
                                clearable={false}
                                joinValues={true}
                                required={true}
                            />
                        }
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
                {grouping === 'FIELDS' ?
               <div className="form-group">
                   <label>Select Fields <span className="text-danger">*</span></label>
                   <div>
                       <Select
                           value={groupingFields}
                           options={groupingFieldsArr}
                           onChange={this.handleGroupingFieldsChange.bind(this)}
                           multi={true}
                           required={true}
                           disabled={groupingFieldsArr.length ? false : true}
                       />
                   </div>
               </div>
               : null
               }
            </form>
        );
    }
}