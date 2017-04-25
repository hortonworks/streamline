/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {OverlayTrigger, Popover,Checkbox} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils  from '../../../utils/ProcessorUtils';

export default class RealTimeJoinNodeProcessor extends Component{

  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired,
    sourceNode: PropTypes.object.isRequired,
    targetNodes: PropTypes.array.isRequired,
    linkShuffleOptions: PropTypes.array.isRequired,
    currentEdges: PropTypes.array.isRequired,
    testRunActivated : PropTypes.bool.isRequired
  };

  constructor(props){
    super(props);
    let {editMode} = props;
    let obj = {
      parallelism: 1,
      editMode: editMode,
      showLoading : false,
      rtJoinStreamObj:'',
      rtJoinTypes: this.getJoinTypeOptions('jointype'),
      rtJoinTypeStreamObj : '',
      rtJoinTypeSelected: '',
      bufferType: 'seconds',
      bufferTypeArr: this.getJoinTypeOptions('buffer'),
      countVal : 0,
      showInputError : false,
      rtJoinEqualFields:[
        {
          firstKey : '',
          secondKey : ''
        }
      ],
      firstKeyOptions : [],
      secondKeyOptions :[],
      outputKeys: [],
      outputKeysObjArr :[],
      outputStreamFields: [],
      outputGroupByDotKeys : [],
      dropDuplicate : false,
      rtJoinEqualGroupKeysObjArr : [{firstKey : '',secondKey : ''}]
    };
    this.state = obj;
    this.streamData = {};
  }

  /*
    componentWillUpdate has been call very frequently in react ecosystem
    this.context.ParentForm.state has been SET through the API call in ProcessorNodeForm
    And we need to call getDataFromParentFormContext after the Parent has set its state so that inputStreamOptions are available
    to used.
    And this condition save us from calling three API
    1] get edge
    2] get streams
    3] get Node data with config.
  */
  componentWillUpdate() {
    if(this.context.ParentForm.state.inputStreamOptions.length > 0 && !(this.fetchDataAgain)){
      this.getDataFromParentFormContext();
    }
  }

  /*
    getJoinTypeOptions fetch the fields from the props.configData.topologyComponentUISpecification.fields
    and return joinTypes fields array
  */
  getJoinTypeOptions(string){
    let configDataFields = this.props.configData.topologyComponentUISpecification.fields;
    let joinOptions = _.find(configDataFields, {fieldName: string}).options;
    let joinTypes = [];
    joinOptions.map((o) => {
      joinTypes.push({value: o.toUpperCase(), label: o.toUpperCase()});
    });
    return joinTypes;
  }

  getDataFromParentFormContext(){
    this.fetchDataAgain = true;

    // get the ProcessorNode from parentForm Context
    this.rtJoinProcessorNode = this.context.ParentForm.state.processorNode;
    let configFields = this.rtJoinProcessorNode.config.properties;

    // get the inputStream from parentForm Context
    const inputStreamFromContext = this.context.ParentForm.state.inputStreamOptions;
    let fields = [],rtJoinTypeStreamName='',unModifyList=[];
    inputStreamFromContext.map((stream, i) => {
      // modify fields if inputStreamFromContext >  1
      const obj = {
        name : inputStreamFromContext[i].streamId,
        fields : stream.fields,
        optional : false,
        type : "NESTED"
      };
      fields.push(obj);

      // UnModify fieldsList
      unModifyList.push(...stream.fields);


    });

    this.fieldsArr = fields;

    const tempFieldsArr = ProcessorUtils.getSchemaFields(fields, 0,false);
    // create a uniqueID for select2 to support duplicate label in options
    _.map(tempFieldsArr, (f) => {
      f.uniqueID = f.keyPath.split('.').join('-')+"_"+f.name;
    });
    const unModifyFieldList = ProcessorUtils.getSchemaFields(unModifyList, 0,false);

    let stateObj = {
      fieldList: unModifyFieldList,
      parallelism: configFields.parallelism || 1,
      outputKeys: configFields.outputKeys
        ? configFields.outputKeys.map((key) => {
          return ProcessorUtils.splitNestedKey(key);
        })
        : undefined,
      inputStreamsArr: inputStreamFromContext,
      outputFieldsList : tempFieldsArr
    };

    // prefetchValue is use to call the this.populateOutputStreamsFromServer() after state has been SET
    let prefetchValue = false;
    if(_.keys(configFields).length > 1){
      prefetchValue = true;
    } else {
      stateObj.outputStreamId = 'rt-join_processor_stream_' + this.rtJoinProcessorNode.id;
      stateObj.outputStreamFields = [];
      this.streamData = {
        streamId: stateObj.outputStreamId,
        fields: stateObj.outputStreamFields
      };
      stateObj.showLoading = false;
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
    }
    this.setState(stateObj, () => {
      prefetchValue ? this.populateOutputStreamsFromServer() : '';
    });
  }

  populateOutputStreamsFromServer = () => {
    let stateObj = {};
    const {inputStreamsArr,outputFieldsList,rtJoinTypes,bufferTypeArr,rtJoinEqualGroupKeysObjArr} = this.state;

    const fromData = this.rtJoinProcessorNode.config.properties.from.stream;
    if(fromData){
      const streamObj = _.filter(inputStreamsArr, (stream) => {return stream.streamId === fromData;});
      stateObj.rtJoinStreamObj = streamObj[0];
      stateObj.firstKeyOptions = ProcessorUtils.getSchemaFields(stateObj.rtJoinStreamObj.fields, 0,false);
    }

    const joinData = this.rtJoinProcessorNode.config.properties.join;
    if(! _.isEmpty(joinData)){
      let intervalType = '';
      stateObj.dropDuplicate = joinData.dropDuplicates;
      _.map(_.keys(joinData),(k) => {
        _.map(bufferTypeArr, (buffer) => {
          if(k === buffer.value.toLowerCase()){
            intervalType = k.toLowerCase();
          }
        });
      });

      if(intervalType === 'count' || intervalType === 'milliseconds'){
        stateObj.countVal = joinData[intervalType];
      } else {
        const bufferSize = Utils.millisecondsToNumber(joinData[intervalType]);
        stateObj.countVal = bufferSize.number;
      }
      stateObj.bufferType = intervalType.toUpperCase();
      stateObj.rtJoinTypeSelected = _.filter(rtJoinTypes, (join) => {return join.value.toLowerCase() === joinData.type.toLowerCase();})[0].value;
      stateObj.rtJoinTypeStreamObj = _.filter(inputStreamsArr, (stream) => {return stream.streamId === joinData.stream;})[0];
      stateObj.secondKeyOptions = ProcessorUtils.getSchemaFields(stateObj.rtJoinTypeStreamObj.fields, 0,false);
    }

    let tempEqualGroup = [];
    const equalData = this.rtJoinProcessorNode.config.properties.equal;
    let tempEqual = [];
    if(equalData.length){
      _.map(equalData, (eq,i) => {
        let firstGroupKey = '',secondObj='';
        const firstObj = ProcessorUtils.findNestedObj(stateObj.firstKeyOptions,eq.firstKey);
        if(firstObj){
          const {gKeys} = ProcessorUtils.getKeysAndGroupKey([firstObj]);
          const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys,'removeParent');
          firstGroupKey = groupKeysByDots[0];
        }
        secondObj =  ProcessorUtils.findNestedObj(stateObj.secondKeyOptions,eq.secondKey);
        const {gKeys} = ProcessorUtils.getKeysAndGroupKey([secondObj]);
        const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys,'removeParent');
        const secondGroupKey = groupKeysByDots[0];
        tempEqualGroup.push({firstKey : firstGroupKey , secondKey : secondGroupKey});
        tempEqual.push({firstKey : firstObj , secondKey : secondObj});
      });
      stateObj.rtJoinEqualGroupKeysObjArr = tempEqualGroup;
      stateObj.rtJoinEqualFields = tempEqual;
    }

    // set the outputKeys And outputFieldsObj for parentContext
    const outputKeysAFormServer = this.rtJoinProcessorNode.config.properties.outputKeys.map((fieldName)=>{return fieldName.split(' as ')[0];});

    // remove the dot from the keys
    stateObj.outputKeys = _.map(outputKeysAFormServer, (key) => {
      return ProcessorUtils.splitNestedKey(key);
    });

    // get the keyObj from the outputFieldsList for the particular key
    const outputKeysObjArr = ProcessorUtils.createOutputFieldsObjArr(outputKeysAFormServer,outputFieldsList);

    stateObj.outputKeysObjArr = outputKeysObjArr;

    const keyData = ProcessorUtils.createSelectedKeysHierarchy(outputKeysObjArr,outputFieldsList);
    stateObj.outputStreamFields=[];
    _.map(keyData,(o) => {
      stateObj.outputStreamFields = _.concat(stateObj.outputStreamFields, o.fields);
    });

    this.streamData = {
      streamId: this.rtJoinProcessorNode.outputStreams[0].streamId,
      fields: stateObj.outputStreamFields
    };

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(outputKeysObjArr);
    stateObj.outputGroupByDotKeys = ProcessorUtils.modifyGroupKeyByDots(gKeys);
    stateObj.showLoading = false;

    this.setState(stateObj);
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  /*
    renderFieldOption Method accept the node from the select2
    And modify the Select2 view list with nested look
  */
  renderFieldOption(node) {
    let styleObj = {
      paddingLeft: (10 * node.level) + "px"
    };
    if (node.disabled) {
      styleObj.fontWeight = "bold";
    }
    return (
      <span style={styleObj}>{node.name}</span>
    );
  }

  filterStreamSelected = (obj) => {
    const {inputStreamsArr} = this.state;
    return _.filter(inputStreamsArr, (stream) => {return stream.streamId !== obj.streamId;});
  }

  handleCommonStreamChange = (keyType,obj) => {
    if(!_.isEmpty(obj)){
      let tempArr = _.cloneDeep(this.state.rtJoinEqualFields);
      const rtJoinStream = this.filterStreamSelected(obj);
      const firstStream = ProcessorUtils.getSchemaFields(obj.fields, 0, false);
      const secondStream = ProcessorUtils.getSchemaFields(rtJoinStream[0].fields,0,false);
      tempArr = [
        {
          firstKey : '',
          secondKey : ''
        }
      ];
      this.setState({
        rtJoinStreamObj : keyType === 'mainStream' ?  obj : rtJoinStream[0],
        rtJoinTypeStreamObj : keyType === 'mainStream' ? rtJoinStream[0] : obj,
        firstKeyOptions : keyType === 'mainStream' ? firstStream : secondStream,
        secondKeyOptions : keyType === 'mainStream' ? secondStream : firstStream,
        rtJoinEqualFields : tempArr
      });
    }
  }

  commonHandlerChange = (keyType,obj) => {
    if(! _.isEmpty(obj)){
      if(keyType === "rtJoinTypes"){
        this.setState({rtJoinTypeSelected : obj.value});
      } else if(keyType === 'bufferType') {
        this.setState({bufferType : obj.value});
      }
    }
  }

  countInputChange = (event) => {
    const val = event.target.value;
    this.setState({countVal : +val,showInputError : val > 0 ? false : true});
  }

  handleEqualFieldChange = (keyString,index,obj) => {
    let equalGroupKeyArrObj = _.cloneDeep(this.state.rtJoinEqualGroupKeysObjArr);
    let tempArr = _.cloneDeep(this.state.rtJoinEqualFields);
    tempArr[index][keyString] = obj;
    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey([obj]);
    const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys,'removeParent');
    if(equalGroupKeyArrObj[index] === undefined){
      equalGroupKeyArrObj[index] = {};
    }
    equalGroupKeyArrObj[index][keyString] = groupKeysByDots[0];
    this.setState({rtJoinEqualFields : tempArr,rtJoinEqualGroupKeysObjArr : equalGroupKeyArrObj});
  }

  addRtJoinEqualFields = () => {
    if (this.state.editMode) {
      const el = document.querySelector('.processor-modal-form ');
      const targetHt = el.scrollHeight;
      Utils.scrollMe(el, (targetHt + 100), 2000);

      let fieldsArr = this.state.rtJoinEqualFields;
      fieldsArr.push({firstKey: '', secondKey: ''});
      this.setState({rtJoinEqualFields: fieldsArr});
    }
  }

  deleteRtJoinEqualFields = (index) => {
    let fieldsArr = _.cloneDeep(this.state.rtJoinEqualFields);
    fieldsArr.splice(index,1);
    this.setState({rtJoinEqualFields : fieldsArr});
  }

  handleFieldsChange = (arr) => {
    if(arr.length > 0){
      // splice the last index of arr as it's a new object selected
      const last_IdData = arr.splice(arr.length - 1 ,1);
      // check the object left in the arr are the same which we splice from it
      // if yes then replace the object with which we have splice earlier
      const checkIndex = _.findIndex(arr , {name : last_IdData[0].name});
      (checkIndex !== -1) ? arr[checkIndex] = last_IdData[0] : arr = _.concat(arr , last_IdData[0]);
      this.setOutputFields(arr);
    } else {
      this.streamData.fields = [];
      this.setState({outputKeysObjArr : arr, outputKeys: [], outputStreamFields: [],outputGroupByDotKeys : []});
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
    }
  }

  setOutputFields = (arr) => {
    let {outputFieldsList} = this.state;
    const keyData = ProcessorUtils.createSelectedKeysHierarchy(arr,outputFieldsList);
    let tempFieldsArr=[];
    _.map(keyData,(o) => {
      tempFieldsArr = _.concat(tempFieldsArr, o.fields);
    });
    this.streamData.fields = tempFieldsArr;

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
    const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys);
    this.setState({outputKeysObjArr : arr, outputKeys: keys, outputStreamFields: keyData,outputGroupByDotKeys : groupKeysByDots});
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  handleSelectAllOutputFields = () => {
    let tempFields = _.cloneDeep(this.state.outputFieldsList);
    const allOutPutFields = ProcessorUtils.selectAllOutputFields(tempFields);
    this.setOutputFields(allOutPutFields);
  }

  checkBoxChange = (event) => {
    this.setState({dropDuplicate : event.target.checked});
  }

  validateData = () => {
    const {rtJoinTypeSelected,rtJoinStreamObj,rtJoinTypeStreamObj,countVal,rtJoinEqualFields,outputKeysObjArr,dropDuplicate} = this.state;
    let validation = false, validateArr = [];
    _.map(rtJoinEqualFields, (fields) => {
      if(fields.firstKey === '' || fields.secondKey === ''){
        validateArr.push(false);
      }
    });
    if(rtJoinTypeSelected !== '' && countVal > 0 && validateArr.length === 0 && outputKeysObjArr.length !== 0  && dropDuplicate){
      validation = true;
    }
    return validation;
  }

  updateEdgesForSelectedStream = () => {
    const {currentEdges,topologyId, versionId} = this.props;
    const {inputStreamsArr,rtJoinEqualGroupKeysObjArr} = this.state;
    const streamObj = inputStreamsArr.find((s) => {
      return s.streamId === this.rtJoinProcessorNode.config.properties.from.stream;
    });

    const edgeObj = currentEdges.find((e) => {
      return streamObj.id === e.streamGrouping.streamId;
    });

    const streamFields = _.map(rtJoinEqualGroupKeysObjArr, 'firstKey');
    let edgeDataWithFormFirst = {
      fromId: edgeObj.source.nodeId,
      toId: edgeObj.target.nodeId,
      streamGroupings: [
        {
          streamId: edgeObj.streamGrouping.streamId,
          grouping: 'FIELDS',
          fields: streamFields
        }
      ]
    };
    const edge_Id = (edgeObj.edgeId);
    return {edge_Id,edgeDataWithFormFirst};
  }

  updateEdgesForJoinTypeObject = () => {
    const {inputStreamsArr,rtJoinEqualGroupKeysObjArr} = this.state;
    const {currentEdges,topologyId, versionId} = this.props;
    const streamObj = inputStreamsArr.find((s) => {
      return s.streamId === this.rtJoinProcessorNode.config.properties.join.stream;
    });
    const edgeObj = currentEdges.find((e) => {
      return streamObj.id === e.streamGrouping.streamId;
    });

    const joinTypeFields = _.map(rtJoinEqualGroupKeysObjArr, 'secondKey');
    let edgeData = {
      fromId: edgeObj.source.nodeId,
      toId: edgeObj.target.nodeId,
      streamGroupings: [
        {
          streamId: edgeObj.streamGrouping.streamId,
          grouping: 'FIELDS',
          fields: joinTypeFields
        }
      ]
    };
    const edgeId = (edgeObj.edgeId);
    return {edgeId,edgeData};
  }

  handleSave = (name, description) => {
    let {topologyId, versionId, nodeType, currentEdges} = this.props;
    let {
      rtJoinTypeSelected,
      rtJoinStreamObj,
      rtJoinTypeStreamObj,
      countVal,
      rtJoinEqualFields,
      outputKeys,
      outputKeysObjArr,
      bufferType,
      dropDuplicate,
      parallelism,
      outputStreamFields,
      inputStreamsArr,
      fieldList,
      outputGroupByDotKeys
    } = this.state;

    const countData = bufferType === 'COUNT' || bufferType === "MILLISECONDS" ? countVal : Utils.numberToMilliseconds(countVal, Utils.capitaliseFirstLetter(bufferType.toLowerCase()));

    const equalTemp = _.map(rtJoinEqualFields, (eq) => {
      return {firstKey : eq.firstKey.name, secondKey : eq.secondKey.name};
    });

    // outputStreams data is formated for the server
    const streamFields  = ProcessorUtils.generateOutputStreamsArr(this.streamData.fields,0);

    if(this.rtJoinProcessorNode.outputStreams.length > 0){
      this.rtJoinProcessorNode.outputStreams[0].fields = streamFields;
    } else {
      this.rtJoinProcessorNode.outputStreams.push({fields: streamFields, streamId: this.streamData.streamId});
    }

    const tempGroupData = _.cloneDeep(outputGroupByDotKeys);
    const tempStreamArr = _.cloneDeep(inputStreamsArr);
    const modifyGroup = ProcessorUtils.modifyGroupArrKeys(tempGroupData,tempStreamArr);

    let finalOutputKeys = modifyGroup.map((keyName)=>{
      if(keyName.search(':') === -1){
        return keyName;
      } else {
        let fieldName = keyName.split(':')[1];
        return keyName +' as ' + fieldName;
      }
    });

    let configObj = {
      from :  {"stream": rtJoinStreamObj.streamId},
      join : {
        type : rtJoinTypeSelected.toLowerCase(),
        stream : rtJoinTypeStreamObj.streamId,
        dropDuplicates : dropDuplicate
      },
      equal : equalTemp,
      outputKeys : finalOutputKeys,
      outputStream : this.streamData.streamId
    };

    configObj.join[bufferType.toLowerCase()] = countData;

    //this.rtJoinProcessorNode is update with the above data
    this.rtJoinProcessorNode.config.properties = configObj;
    this.rtJoinProcessorNode.config.properties.parallelism = parallelism;
    this.rtJoinProcessorNode.name = name;
    this.rtJoinProcessorNode.description = description;

    let promiseArr = [];
    // update rtJoinProcessorNodeprocessorNode
    promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, this.rtJoinProcessorNode.id, {body: JSON.stringify(this.rtJoinProcessorNode)}));

    // update edge with FROM Stream of selected Streams
    const {edge_Id,edgeDataWithFormFirst} = this.updateEdgesForSelectedStream();
    promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'edges', edge_Id, {body: JSON.stringify(edgeDataWithFormFirst)}));


    // update edges with selected rtJoinTypes obj key with particular edgeId for multiple rt-Join using Array of edgeDataArr;
    const {edgeId,edgeData} = this.updateEdgesForJoinTypeObject();
    promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'edges',edgeId, {body: JSON.stringify(edgeData)}));

    return Promise.all(promiseArr);
  }

  render(){
    const {
      showLoading,
      editMode,
      rtJoinStreamObj,
      inputStreamsArr,
      rtJoinTypes,
      rtJoinTypeSelected,
      rtJoinTypeStreamObj,
      bufferType,
      bufferTypeArr,
      countVal,
      showInputError,
      rtJoinEqualFields,
      firstKeyOptions,
      secondKeyOptions,
      outputKeysObjArr,
      outputFieldsList,
      dropDuplicate
    } = this.state;

    const disabledFields = this.props.testRunActivated ? true : !editMode;
    return(
      <div className="modal-form processor-modal-form">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
        {
          showLoading
          ? <div className="loading-img text-center">
              <img src="styles/img/start-loader.gif" alt="loading" style={{
                marginTop: "140px"
              }}/>
            </div>
          :  <form className="customFormClass" style={{marginTop : '7px'}}>
              <div className="form-group row">
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of input stream</Popover>}>
                    <label>Select Stream
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                  <div>
                    <Select value={rtJoinStreamObj} options={inputStreamsArr} onChange={this.handleCommonStreamChange.bind(this,'mainStream')} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                  </div>
                </div>
              </div>
              <div className="form-group row no-margin">
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Type of join</Popover>}>
                    <label>Join Type
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of join stream</Popover>}>
                    <label>Select Stream
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Buffer Type</Popover>}>
                    <label>Buffer Type Interval
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Buffer Size</Popover>}>
                    <label>Buffer Size
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3">
                  <Select value={rtJoinTypeSelected} options={rtJoinTypes} onChange={this.commonHandlerChange.bind(this,'rtJoinTypes')} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}/>
                </div>
                <div className="col-sm-3">
                  <Select value={rtJoinTypeStreamObj} options={inputStreamsArr} onChange={this.handleCommonStreamChange.bind(this,'joinStream')} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                </div>
                <div className="col-sm-3">
                  <Select value={bufferType} options={bufferTypeArr} onChange={this.commonHandlerChange.bind(this,'bufferType')} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}/>
                </div>
                <div className="col-sm-3">
                  <input type="number" className={`form-control ${showInputError ? 'invalidInput' : ''}`} value={countVal} min={0} max={Number.MAX_SAFE_INTEGER}  onChange={this.countInputChange} />
                </div>
              </div>
              <div className="form-group row no-margin">
                <div className="col-sm-3">
                  <Checkbox inline checked={dropDuplicate} onChange={this.checkBoxChange}>
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Drop Duplicates</Popover>}>
                      <label style={{marginBottom : 0, marginTop : "3px"}}>drop Duplicates
                        <span className="text-danger">*</span>
                      </label>
                    </OverlayTrigger>
                  </Checkbox>
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-12" style={{marginTop : "10px"}}>
                  <fieldset className="fieldset-default">
                    <legend>Equal Fields</legend>
                      <div className="row">
                        <div className="col-sm-5 outputCaption">
                          <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">First Key</Popover>}>
                            <label>First Key
                              <span className="text-danger">*</span>
                            </label>
                          </OverlayTrigger>
                        </div>
                        <div className="col-sm-5 outputCaption">
                          <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Second Key</Popover>}>
                            <label>Second Key
                              <span className="text-danger">*</span>
                            </label>
                          </OverlayTrigger>
                        </div>
                      </div>
                      {
                        _.map(rtJoinEqualFields, (eq,i) => {
                          return(
                            <div key={i} className="row form-group">
                              <div className="col-sm-5">
                                <Select value={eq.firstKey} options={firstKeyOptions} onChange={this.handleEqualFieldChange.bind(this,'firstKey',i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                              </div>
                              <div className="col-sm-5">
                                <Select value={eq.secondKey} options={secondKeyOptions} onChange={this.handleEqualFieldChange.bind(this,'secondKey',i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                              </div>
                              {editMode
                                ? <div className="col-sm-2">
                                    <button className="btn btn-default btn-sm" disabled={disabledFields} type="button" onClick={this.addRtJoinEqualFields.bind(this)}>
                                      <i className="fa fa-plus"></i>
                                    </button>&nbsp; {i > 0
                                      ? <button className="btn btn-sm btn-danger" type="button" onClick={this.deleteRtJoinEqualFields.bind(this, i)}>
                                          <i className="fa fa-trash"></i>
                                        </button>
                                      : null}
                                  </div>
                                : null}
                            </div>
                          );
                        })
                      }
                  </fieldset>
                </div>
              </div>
              <div className="form-group">
                <div className="col-sm-6">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Output keys</Popover>}>
                    <label>Output Fields
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div className="col-sm-6">
                  <label className="pull-right">
                    <a href="javascript:void(0)" onClick={this.handleSelectAllOutputFields}>Select All</a>
                  </label>
                </div>
                <div className="row">
                  <div className="col-sm-12">
                    <Select className="menu-outer-top" value={outputKeysObjArr} options={outputFieldsList} onChange={this.handleFieldsChange.bind(this)} multi={true} required={true} disabled={disabledFields} valueKey="uniqueID" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                  </div>
                </div>
              </div>
            </form>
        }
        </Scrollbars>
      </div>
    );
  }
}

RealTimeJoinNodeProcessor.contextTypes = {
  ParentForm: React.PropTypes.object
};
