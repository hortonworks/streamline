import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import {Tabs, Tab} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import TopologyREST from '../../../rest/TopologyREST';
import Form from '../../../libs/form';
import StreamsSidebar from '../../../components/StreamSidebar';
import NotesForm from '../../../components/NotesForm';
import FSReactToastr from '../../../components/FSReactToastr';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import { Scrollbars } from 'react-custom-scrollbars';

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
        this.state = {
            formData: {},
            streamObj: {},
            description: '',
            showRequired: true,
            activeTabKey: 1,
            clusterArr : [],
            configJSON : this.fetchFields(),
            clusterName : '',
            fetchLoader : true
        };
    }

    fetchData(){
        let {topologyId, versionId, nodeType, nodeData,namespaceId} = this.props;
        const sourceParams = nodeData.parentType+'/'+nodeData.topologyComponentBundleId;
        let promiseArr = [
          TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
          TopologyREST.getSourceComponentClusters(sourceParams,namespaceId)
        ];

        Promise.all(promiseArr)
          .then((results) => {
            let stateObj = {},tempArr = [];
            if(results[0].responseMessage !== undefined){
              FSReactToastr.error(<CommonNotification flag="error" content={results[0].responseMessage}/>, '', toastOpt);
            }else{
              this.nodeData = results[0];
              if(this.nodeData.outputStreams.length === 0){
                  this.streamObj = { streamId: this.props.configData.subType.toLowerCase()+'_stream_'+this.nodeData.id, fields: []};
              } else {
                  this.streamObj = this.nodeData.outputStreams[0];
              }
              stateObj.streamObj = this.streamObj;
            }
            if(results[1].responseMessage !== undefined){
              this.setState({fetchLoader : false});
              FSReactToastr.error(<CommonNotification flag="error" content={results[1].responseMessage}/>, '', toastOpt);
            }else{
              const clusters = results[1];
              tempArr = _.keys(clusters).map((x, i) => {
                  return {
                    fieldName : x,
                    uiName : x
                  }
              });
              stateObj.clusterArr = clusters;
            }
            stateObj.configJSON = this.pushClusterFields(tempArr);
            stateObj.formData = this.nodeData.config.properties;
            stateObj.description = this.nodeData.description;
            stateObj.fetchLoader = false;
            this.setState(stateObj, () => {
              if(stateObj.formData.clusters !== undefined){
                this.updateClusterFields(stateObj.formData.clusters);
                this.setState({streamObj : this.state.streamObj});
              }
              if(_.keys(stateObj.clusterArr).length === 1){
                stateObj.formData.clusters = _.keys(stateObj.clusterArr)[0];
                this.updateClusterFields(stateObj.formData.clusters);
              }
            });
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
      const {configJSON} = this.state;
      const obj = configJSON.map(x => {
          if(x.fieldName === 'clusters'){
            x.options = opt;
          }
        return x
      });
      return obj;
    }

    populateClusterFields(val){
      const tempObj = Object.assign({},this.state.formData,{topic:''});
      this.setState({clusterName : val,streamObj:'',formData:tempObj}, () => {
        this.updateClusterFields();
      });
    }

    updateClusterFields(name){
      const {clusterArr,clusterName,streamObj, formData} = this.state;
      let data = {},obj=[];
      let config = this.state.configJSON;
      _.keys(clusterArr).map((x) => {
        if(name || clusterName === x){
        obj = config.map((list) => {
            _.keys(clusterArr[x]).map(k => {
                if(list.fieldName === k){
                  if(_.isArray(clusterArr[x][k]) && (name || clusterName) === x){
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
      this.setState({configJSON : obj,formData : tempData});
    }

    validateData(){
        let validDataFlag = true;
        if(!this.refs.Form.validate()){
            validDataFlag = false;
            this.setState({activeTabKey: 1, showRequired: true});
        }
        if(this.streamObj.fields.length === 0){
          validDataFlag = false;
          FSReactToastr.error(<CommonNotification flag="error" content={"Output stream fields cannot be blank."}/>, '', toastOpt);
        }
        return validDataFlag;
    }

    handleSave(name){
        let {topologyId, versionId, nodeType} = this.props;
        let nodeId = this.nodeData.id;
        let data = this.refs.Form.state.FormData;
        this.nodeData.config.properties = data;
        this.nodeData.name = name;
        if(this.nodeData.outputStreams.length > 0){
          this.nodeData.outputStreams[0].fields = this.streamObj.fields;
        } else {
          this.nodeData.outputStreams.push({
            fields: this.streamObj.fields,
            streamId: this.streamObj.streamId,
            topologyId: topologyId
          })
        }
        this.nodeData.description = this.state.description;
        return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)});
    }

    showOutputStream(resultArr){
        this.streamObj = {
            streamId: this.props.configData.subType.toLowerCase()+'_stream_'+this.nodeData.id,
            fields: resultArr
        };
        this.setState({streamObj: this.streamObj});
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
        const {configJSON,fetchLoader} = this.state;
        let formData = this.state.formData;
        let fields = Utils.genFields(configJSON, [], formData);
        const form = fetchLoader
                      ? <div className="col-sm-12">
                            <div className="loading-img text-center" style={{marginTop : "100px"}}>
                                <img src="styles/img/start-loader.gif" alt="loading" />
                            </div>
                        </div>
                      : <div className="source-modal-form">
                            <Scrollbars autoHide
                              renderThumbHorizontal={props => <div {...props} style={{display : "none"}}/>}
                              >
                                  <Form
                                      ref="Form"
                                      readOnly={!this.props.editMode}
                                      showRequired={this.state.showRequired}
                                      className="customFormClass"
                                      FormData={formData}
                                      populateClusterFields={this.populateClusterFields.bind(this)}
                                      callback={this.showOutputStream.bind(this)}
                                  >
                                      {fields}
                                  </Form>
                            </Scrollbars>
                          </div>
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
