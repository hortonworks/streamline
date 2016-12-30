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
import ClusterREST from '../../../rest/ClusterREST';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';
import { Scrollbars } from 'react-custom-scrollbars';

export default class SinkNodeForm extends Component {
    static propTypes = {
        nodeData: PropTypes.object.isRequired,
        configData: PropTypes.object.isRequired,
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
        this.state = {
            formData: {},
            streamObj: {},
            description: '',
            showRequired: true,
            activeTabKey: 1,
            uiSpecification : [],
            clusterArr : [],
            clusterName: '',
            fetchLoader : true
        };
        this.fetchNotifier();
    }

    fetchData(){
        let {topologyId, versionId, nodeType, nodeData, sourceNodes,namespaceId} = this.props;
        const sourceParams = nodeData.parentType+'/'+nodeData.topologyComponentBundleId;
        let sourceNodeType = null;
        let promiseArr = [
            TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
            TopologyREST.getAllNodes(topologyId, versionId, 'edges'),
            TopologyREST.getSourceComponentClusters(sourceParams,namespaceId)
        ];
        if(sourceNodes.length > 0){
            sourceNodeType = TopologyUtils.getNodeType(sourceNodes[0].parentType);
            promiseArr.push(TopologyREST.getNode(topologyId, versionId, sourceNodeType, sourceNodes[0].nodeId));
        }
        Promise.all(promiseArr)
            .then(results=>{
              let stateObj = {},tempArr = [];
                this.nodeData = results[0];
                if(results[1].entities){
                    results[1].entities.map((edge)=>{
                        if(edge.toId === nodeData.nodeId && this.sourceNodesId.indexOf(edge.fromId) !== -1){
                            //TODO - Once we support multiple input streams, need to fix this.
                            TopologyREST.getNode(topologyId, versionId, 'streams', edge.streamGroupings[0].streamId)
                                .then(streamResult=>{
                                    this.setState({streamObj: streamResult});
                                })
                        }
                    })
                }
                if(results[2].responseMessage !== undefined){
                  this.setState({fetchLoader:false});
                  FSReactToastr.error(<CommonNotification flag="error" content={results[0].responseMessage}/>, '', toastOpt);
                }else{
                  const clusters = results[2];
                  tempArr = _.keys(clusters).map((x, i) => {
                      return {
                        fieldName : x,
                        uiName : x
                      }
                  });
                  stateObj.clusterArr = clusters;
                }
                if(!_.isEmpty(stateObj.clusterArr) && _.keys(stateObj.clusterArr).length > 1){
                  this.fetchFields();
                  stateObj.uiSpecification = this.pushClusterFields(tempArr);
                }
                stateObj.formData = this.nodeData.config.properties;
                stateObj.description = this.nodeData.description;
                stateObj.fetchLoader = false;
                this.setState(stateObj, () => {
                  if(stateObj.formData.clusters !== undefined){
                    this.updateClusterFields(stateObj.formData.clusters);
                  }
                  if(_.keys(stateObj.clusterArr).length === 1){
                    this.fetchFields();
                    stateObj.uiSpecification = this.pushClusterFields(tempArr);
                    stateObj.formData.clusters = _.keys(stateObj.clusterArr)[0];
                    this.updateClusterFields(stateObj.formData.clusters);
                  }
                });
                if(sourceNodes.length > 0){
                    //Finding the source node and updating actions for rules/windows
                    this.sourceNodeData = results[3];
                    let sourcePromiseArr = [];
                    // sourceChildNodeType are processor nodes inner child, window or rule
                    let type = sourceNodes[0].currentType.toLowerCase();
                    this.sourceChildNodeType = type === 'window' ? 'windows' : (type === 'rule' ? 'rules' : 'branchrules');
                    if(this.sourceNodeData.config.properties && this.sourceNodeData.config.properties.rules && this.sourceNodeData.config.properties.rules.length > 0){
                        this.sourceNodeData.config.properties.rules.map((id)=>{
                            sourcePromiseArr.push(TopologyREST.getNode(topologyId, versionId, this.sourceChildNodeType, id));
                        })
                    }
                    Promise.all(sourcePromiseArr)
                        .then(sourceResults=>{
                            this.allSourceChildNodeData = sourceResults;
                        })
                }
            })
    }

    fetchFields = () => {
      let obj = this.props.configData.topologyComponentUISpecification.fields;
      const clusterFlag = obj.findIndex(x => {
        return x.fieldName === 'clusters'
      });
      if(clusterFlag === -1){
        const data = {
                      "uiName": "Cluster Name",
                      "fieldName": "clusters",
                      "isOptional": false,
                      "tooltip": "Cluster name to read data from",
                      "type": "CustomEnumstring",
                      "options": []
                    };
          obj.unshift(data);
      }
      return obj;
    }

    pushClusterFields = (opt) => {
      const {uiSpecification} = this.state;
      const obj = uiSpecification.map(x => {
          if(x.fieldName === 'clusters'){
            x.options = opt;
          }
        return x
      });
      return obj;
    }

    fetchNotifier = () => {
      ClusterREST.getAllNotifier()
        .then(notifier => {
          if(notifier.responseMessage !== undefined){
            FSReactToastr.error(
                <CommonNotification flag="error" content={notifier.responseMessage}/>, '', toastOpt);
          }else{
            const obj = notifier.entities.filter(x => {
              return x.name.indexOf("email_notifier") !== -1;
            });

           let {configData} = this.props;
           const {topologyComponentUISpecification} = configData;
           let uiFields = topologyComponentUISpecification.fields || [];

            uiFields.map(x => {
              if(x.fieldName === "jarFileName"){
                  x.defaultValue = obj[0].jarFileName;
                  if(x.hint !== undefined){
                    x.hint = x.hint+',hidden';
                  } else {
                    x.hint = "hidden";
                  }
              }
            });
            this.setState({uiSpecification : uiFields});
          }
        }).catch(err => {
          FSReactToastr.error(
              <CommonNotification flag="error" content={err.message}/>, '', toastOpt);
        });
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
        let {topologyId, versionId, nodeType, nodeData} = this.props;
        let nodeId = this.nodeData.id;
        let data = this.refs.Form.state.FormData;
        this.nodeData.config.properties = data;
        let oldName = this.nodeData.name;
        this.nodeData.name = name;
        this.nodeData.description = this.state.description;
        let promiseArr = [TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)})];
        if(this.allSourceChildNodeData && this.allSourceChildNodeData.length > 0){
            this.allSourceChildNodeData.map((childData)=>{
                let child = childData;
                let obj = child.actions.find((o)=>{return o.name == oldName});
                if(obj){
                    obj.name = name;
                    if(nodeData.currentType.toLowerCase() == 'notification'){
                        obj.outputFieldsAndDefaults = this.nodeData.config.properties.fieldValues || {};
                        obj.notifierName = this.nodeData.config.properties.notifierName || '';
                    }
                    promiseArr.push(TopologyREST.updateNode(topologyId, versionId, this.sourceChildNodeType, child.id, {body: JSON.stringify(child)}));
                } else {
                    if(this.sourceChildNodeType !== 'branchrules'){
                        console.error("Missing actions object for "+name);
                    }
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

    handleNotesChange(description) {
        this.setState({description: description});
    }

    populateClusterFields(val){
      const tempObj = Object.assign({},this.state.formData,{topic:''});
      this.setState({clusterName : val, formData: tempObj}, () => {
        this.updateClusterFields();
      });
    }

    updateClusterFields(name){
      const {clusterArr,clusterName, formData} = this.state;
      let data = {},obj=[];
      let config = this.state.uiSpecification;
      _.keys(clusterArr).map((x) => {
        if(name || clusterName === x){
        obj = config.map((list) => {
            _.keys(clusterArr[x]).map(k => {
                if(list.fieldName === k){
                  if(_.isArray(clusterArr[x][k])  && (name || clusterName) === x){
                    list.options = clusterArr[x][k].map(v => {
                      return {
                        fieldName : v,
                        uiName : v
                      }
                    })
                    if(list.hint && list.hint.toLowerCase().indexOf("override") !== -1){
                      if(formData[k] != ''){
                        if(list.options.findIndex((o)=>{return o.fieldName == formData[k]}) == -1){
                          list.options.push({fieldName: formData[k], uiName: formData[k]});
                        }
                      }
                    }
                  }else{
                    if(!_.isArray(clusterArr[x][k])){
                      data[k] = clusterArr[x][k];
                    }
                  }
                }
            })
            data.clusters = clusterName ? clusterName : name;
            return list;
          });
        }
      });
      const tempData = Object.assign({},this.state.formData,data);
      this.setState({uiSpecification : obj,formData : tempData});
    }

    render() {
        let {formData, streamObj = {},uiSpecification,fetchLoader} = this.state;

        let fields = Utils.genFields(uiSpecification, [], formData,streamObj.fields);
        const form = fetchLoader
                      ? <div className="col-sm-12">
                            <div className="loading-img text-center" style={{marginTop : "100px"}}>
                                <img src="styles/img/start-loader.gif" alt="loading" />
                            </div>
                        </div>
                      :  <div className="sink-modal-form">
                            <Scrollbars autoHide
                                renderThumbHorizontal={props => <div {...props} style={{display : "none"}}/>}
                                >
                                <Form
                                      ref="Form"
                                      readOnly={!this.props.editMode}
                                      showRequired={this.state.showRequired}
                                      FormData={formData}
                                      className="customFormClass"
                                      populateClusterFields={this.populateClusterFields.bind(this)}
                                  >
                                      {fields}
                                  </Form>
                            </Scrollbars>
                          </div>
        const inputSidebar = <StreamsSidebar ref="StreamSidebar" streamObj={streamObj} streamType="input" />
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
