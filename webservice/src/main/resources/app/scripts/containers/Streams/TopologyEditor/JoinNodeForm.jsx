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

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {OverlayTrigger, Popover} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils  from '../../../utils/ProcessorUtils';

export default class JoinNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    configData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired,
    sourceNode: PropTypes.array.isRequired,
    targetNodes: PropTypes.array.isRequired,
    linkShuffleOptions: PropTypes.array.isRequired,
    currentEdges: PropTypes.array.isRequired
  };

  constructor(props) {
    super(props);
    let {editMode} = props;
    var obj = {
      parallelism: 1,
      editMode: editMode,
      fieldList: [],
      intervalType: ".Window$Duration",
      intervalTypeArr: [
        {
          value: ".Window$Duration",
          label: "Time"
        }, {
          value: ".Window$Count",
          label: "Count"
        }
      ],
      windowNum: '',
      slidingNum: '',
      durationType: "Seconds",
      slidingDurationType: "Seconds",
      durationTypeArr: [
        {
          value: "Seconds",
          label: "Seconds"
        }, {
          value: "Minutes",
          label: "Minutes"
        }, {
          value: "Hours",
          label: "Hours"
        }
      ],
      outputKeys: [],
      outputStreamFields: [],
      joinFromStreamName: '',
      joinFromStreamKey: '',
      joinFromStreamKeys: [],
      joinTypes: this.getJoinTypeOptions(),
      joinStreams: [],
      inputStreamsArr: [],
      outputFieldsList : [],
      showLoading : true,
      outputGroupByDotKeys : []
    };
    this.state = obj;
    this.fetchDataAgain = false;
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
  getJoinTypeOptions(){
    let configDataFields = this.props.configData.topologyComponentUISpecification.fields;
    let joinOptions = _.find(configDataFields, {fieldName: 'jointype'}).options;
    let joinTypes = [];
    joinOptions.map((o) => {
      joinTypes.push({value: o, label: o});
    });
    return joinTypes;
  }


  /*
    getDataFromParentFormContext is called from componentWillUpdate
    Depend upon the condition

    Get the joinProcessorNode from the this.context.ParentForm.state.processorNode
    Get the stream from the this.context.ParentForm.state.inputStreamOptions
    And if joinProcessorNode has the config.properties more then 1
    we call this.populateOutputStreamsFromServer to pre fill the value on UI
  */
  getDataFromParentFormContext(){
    this.fetchDataAgain = true;

    // get the ProcessorNode from parentForm Context
    this.joinProcessorNode = this.context.ParentForm.state.processorNode;
    let configFields = this.joinProcessorNode.config.properties;

    // get the inputStream from parentForm Context
    const inputStreamFromContext = this.context.ParentForm.state.inputStreamOptions;
    let fields = [],joinStreams=[],unModifyList=[];
    inputStreamFromContext.map((stream, i) => {
      // modify fields if inputStreamFromContext >  1
      if(inputStreamFromContext.length > 1){
        const obj = {
          name : inputStreamFromContext[i].streamId,
          fields : stream.fields,
          optional : false,
          type : "NESTED"
        };
        fields.push(obj);
      } else {
        fields.push(...stream.fields);
      }

      // UnModify fieldsList
      unModifyList.push(...stream.fields);

      if (i < inputStreamFromContext.length - 1) {
        joinStreams.push({
          id: i,
          type: '',
          stream: '',
          key: '',
          with: '',
          streamOptions: [],
          keyOptions: [],
          withOptions: []
        });
      }
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
      joinStreams: joinStreams,
      outputFieldsList : tempFieldsArr
    };

    // prefetchValue is use to call the this.populateOutputStreamsFromServer() after state has been SET
    let prefetchValue = false;
    if(_.keys(configFields).length > 1){
      prefetchValue = true;
    } else {
      stateObj.outputStreamId = 'join_processor_stream_' + this.joinProcessorNode.id;
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

  /*
    populateOutputStreamsFromServer Method accept the Object send from the getDataFromParentFormContext
    When the JoinProcessor has been already configured
    And we set all the defaultvalue, which we got from there joinProcessorNode

    This include Nested fields spliting and populating the pre value for each and every fields on UI
    And SET in state object
  */
  populateOutputStreamsFromServer(){
    let stateObj = {};
    const {inputStreamsArr,outputFieldsList} = this.state;
    const fromObject = this.joinProcessorNode.config.properties.from;
    stateObj.joinStreams = _.cloneDeep(this.state.joinStreams);

    //set value for first row
    if(!_.isEmpty(fromObject)){
      stateObj.joinFromStreamName = fromObject.stream;
      stateObj.joinFromStreamKey = ProcessorUtils.splitNestedKey(fromObject.key);

      let selectedStream = _.find(inputStreamsArr, {streamId: fromObject.stream});
      stateObj.joinFromStreamKeys = ProcessorUtils.getSchemaFields(selectedStream.fields, 0, false);
    }

    //set values of joins Type
    const configJoinFields = this.joinProcessorNode.config.properties.joins;
    if(!_.isEmpty(configJoinFields)){
      _.map(configJoinFields, (o , index) => {
        let joinStreamOptions = this.getStreamObjArray(o.stream);
        stateObj.joinStreams[index].type = o.type;
        stateObj.joinStreams[index].stream = o.stream;
        stateObj.joinStreams[index].streamOptions = joinStreamOptions;
        stateObj.joinStreams[index].key = ProcessorUtils.splitNestedKey(o.key);
        stateObj.joinStreams[index].keyOptions = ProcessorUtils.getSchemaFields(joinStreamOptions[0].fields, 0, false);
        stateObj.joinStreams[index].with = o.with;
        stateObj.joinStreams[index].withOptions = this.getStreamObjArray(o.with);
      });
    }

    //set values of windows fields
    const configWindowObj = this.joinProcessorNode.config.properties.window;
    if(!_.isEmpty(configWindowObj)){
      if (configWindowObj.windowLength.class === '.Window$Duration') {
        stateObj.intervalType = '.Window$Duration';
        let obj = Utils.millisecondsToNumber(configWindowObj.windowLength.durationMs);
        stateObj.windowNum = obj.number;
        stateObj.durationType = obj.type;
        if (configWindowObj.slidingInterval) {
          let obj = Utils.millisecondsToNumber(configWindowObj.slidingInterval.durationMs);
          stateObj.slidingNum = obj.number;
          stateObj.slidingDurationType = obj.type;
        }
      } else if (configWindowObj.windowLength.class === '.Window$Count') {
        stateObj.intervalType = '.Window$Count';
        stateObj.windowNum = configWindowObj.windowLength.count;
        if (configWindowObj.slidingInterval) {
          stateObj.slidingNum = configWindowObj.slidingInterval.count;
        }
      }
    }

    // set the outputKeys And outputFieldsObj for parentContext
    const outputKeysAFormServer = this.joinProcessorNode.config.properties.outputKeys.map((fieldName)=>{return fieldName.split(' as ')[0];});

    // remove the dot from the keys
    stateObj.outputKeys = _.map(outputKeysAFormServer, (key) => {
      return ProcessorUtils.splitNestedKey(key);
    });

    // get the keyObj from the outputFieldsList for the particular key
    const outputKeysObjArray = ProcessorUtils.createOutputFieldsObjArr(outputKeysAFormServer,outputFieldsList);

    // To remove the undefined if the schemsa get change from the source Node
    const outputKeysObjArr = _.compact(outputKeysObjArray);

    stateObj.outputKeysObjArr = outputKeysObjArr;

    const keyData = ProcessorUtils.createSelectedKeysHierarchy(outputKeysObjArr,outputFieldsList);
    stateObj.outputStreamFields=[];
    _.map(keyData,(o) => {
      stateObj.outputStreamFields = _.concat(stateObj.outputStreamFields, o.fields ? o.fields : o);
    });

    this.streamData = {
      streamId: this.joinProcessorNode.outputStreams[0].streamId,
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

  /*
    getStreamObjArray Method accept the string of streams
    return
    streamsObj Array filter from inputStreamsArr
  */
  getStreamObjArray(string){
    const {inputStreamsArr} = this.state;
    return inputStreamsArr.filter((s) => {
      return s.streamId === string;
    });
  }

  /*
    handleFieldsChange Method accept arr of obj
    And SET
    outputKeys : key of arr used on UI for listing
    outputGroupByDotKeys : group the outputKeys
    outputStreamFields : store the obj of the outputKeys
  */
  handleFieldsChange(arr){
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

  handleSelectAllOutputFields = () => {
    let tempFields = _.cloneDeep(this.state.outputFieldsList);
    const allOutPutFields = ProcessorUtils.selectAllOutputFields(tempFields);
    this.setOutputFields(allOutPutFields);
  }

  setOutputFields = (arr) => {
    let {outputFieldsList} = this.state;
    const keyData = ProcessorUtils.createSelectedKeysHierarchy(arr,outputFieldsList);
    let tempFieldsArr=[];
    _.map(keyData,(o) => {
      tempFieldsArr = _.concat(tempFieldsArr, o.fields ? o.fields : o);
    });
    this.streamData.fields = tempFieldsArr;

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
    const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys);
    this.setState({outputKeysObjArr : arr, outputKeys: keys, outputStreamFields: keyData,outputGroupByDotKeys : groupKeysByDots});
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  /*
    commonHandlerChange Method accept keyType, obj and it handles multiple event [durationType,slidingDurationType,intervalType]
    params@ keyType = string 'durationType'
    params@ obj = selected obj
  */
  commonHandlerChange(keyType,obj){
    if(obj){
      const keyName = keyType.trim();
      keyName === "durationType"
      ? this.setState({durationType : obj.value,slidingDurationType: obj.value})
      : keyName === "slidingDurationType"
        ? this.setState({slidingDurationType : obj.value})
        : this.setState({intervalType : obj.value});
    }
  }

  /*
    handleValueChange Method is handles to fields on UI
    windowNum and slidingNum input value
  */
  handleValueChange(e) {
    let obj = {};
    let name = e.target.name;
    let value = e.target.type === "number"
      ? Math.abs(e.target.value)
      : e.target.value;
    obj[name] = value;
    if (name === 'windowNum') {
      obj['slidingNum'] = value;
    }
    this.setState(obj);
  }

  /*
    handleJoinFromStreamChange Method accept arr selected from select2
    Depend on selected key SET the value stream and streamOptions
    And
  */
  handleJoinFromStreamChange(obj){
    if(obj){
      let {inputStreamsArr, joinStreams} = this.state;
      let joinStreamOptions = inputStreamsArr.filter((s) => {
        return s.streamId !== obj.streamId;
      });

      if(joinStreams.length){
        joinStreams[0].streamOptions = joinStreamOptions;
        joinStreams[0].withOptions = [obj];
        if (joinStreams.length === 1) {
          joinStreams[0].stream = joinStreamOptions[0].streamId;
          joinStreams[0].with = obj.streamId;
          joinStreams[0].keyOptions = ProcessorUtils.getSchemaFields(joinStreamOptions[0].fields, 0, false);
        }
      }

      const tempFieldsArr = ProcessorUtils.getSchemaFields(obj.fields, 0, false);
      this.setState({
        joinFromStreamName: obj ?
          obj.streamId :
          '',
        joinFromStreamKeys: obj ?
          tempFieldsArr :
          [],
        joinFromStreamKey: '',
        joinStreams: joinStreams
      });
    }
  }

  /*
    handleJoinFromKeyChange accept the obj
    SET STATE the joinFromStreamKey
  */
  handleJoinFromKeyChange(obj){
    this.setState({
      joinFromStreamKey: obj
        ? obj.name
        : ""
    });
  }

  /*
    handleJoinTypeChange accept the Key and obj
    SET STATE the joinStreams for a particular key
  */
  handleJoinTypeChange(key, obj) {
    let {joinStreams} = this.state;
    joinStreams[key].type = obj
      ? obj.value
      : '';
    this.setState({joinStreams: joinStreams});
  }

  /*
    handleJoinStreamChange accept key and obj
    SET the joinStreams[key].keyOptions with the selected obj.streamId
  */
  handleJoinStreamChange(key, obj) {
    let {inputStreamsArr, joinStreams, joinFromStreamName} = this.state;
    if (obj) {
      joinStreams[key].stream = obj.streamId;
      const tempFieldsArr = ProcessorUtils.getSchemaFields(obj.fields, 0, false);
      joinStreams[key].keyOptions = tempFieldsArr;
      let streamOptions = [];
      let withOptions = [];
      let streamObj = inputStreamsArr.find((stream) => {
        return stream.streamId === joinFromStreamName;
      });
      withOptions.push(streamObj);
      joinStreams.map((s, i) => {
        if (i <= key) {
          let obj = inputStreamsArr.find((stream) => {
            return stream.streamId === s.stream;
          });
          withOptions.push(obj);
        }
        if (i > key) {
          s.stream = '';
          s.key = '';
          s.with = '';
          s.streamOptions = [];
          s.keyOptions = [];
          s.withOptions = [];
        }
      });
      streamOptions = _.difference(inputStreamsArr, [...withOptions]);
      let nextStream = joinStreams[key + 1];
      if (nextStream) {
        nextStream.streamOptions = streamOptions;
        nextStream.withOptions = withOptions;
        nextStream.key = '';
        nextStream.keyOptions = [];
      }
    } else {
      joinStreams[key].stream = '';
      joinStreams[key].keyOptions = [];
    }
    this.setState({joinStreams: joinStreams});
  }

  /*
    handleJoinKeyChange accept the Key and obj
    SET STATE the joinStreams for a particular key
  */
  handleJoinKeyChange(key, obj){
    let {joinStreams} = this.state;
    joinStreams[key].key = obj
      ? obj.name
      : '';
    this.setState({joinStreams: joinStreams});
  }

  /*
    handleJoinWithChange accept the Key and obj
    SET STATE the joinStreams for a particular key
  */
  handleJoinWithChange(key, obj){
    let {joinStreams} = this.state;
    joinStreams[key].with = obj
      ? obj.streamId
      : '';
    this.setState({joinStreams: joinStreams});
  }

  /*
    validateData check the validation of
     outputKeys,windowNum and joinStreams array
  */
  validateData() {
    let {outputKeys, windowNum, joinStreams} = this.state;
    let validData = true;
    if (outputKeys.length === 0 || windowNum === '') {
      return false;
    }
    joinStreams.map((s) => {
      if (s.stream === '' || s.type === '' || s.key === '' || s.with === '') {
        validData = false;
      }
    });
    return validData;
  }

  formatNestedField(obj) {
    let name = obj.name;
    if (obj.keyPath && obj.keyPath.length > 0) {
      name = obj.keyPath + '.' + obj.name;
      // name = '';
      // var keysArr = obj.keyPath.split(".");
      // if(keysArr.length > 0) {
      //     keysArr.map((k, n)=>{
      //         if(n === 0) {
      //             name += k;
      //         } else {
      //             name += "['" + k + "']";
      //         }
      //     });
      //     name += "['" + obj.name + "']";
      // } else {
      //     name += obj.keyPath + "['" + obj.name + "']";
      // }
    }
    return name;
  }

  /*
    updateEdges Method update the edge
    using inputStreamsArr id to filter the currentEdges.streamGrouping.streamId for the particular nodeType
    And update with fields selected as a outputStreams
  */
  updateEdgesForJoinTypeObject(){
    const {inputStreamsArr} = this.state;
    const {currentEdges,topologyId, versionId} = this.props;
    let edgeDataArr = [],edgeIdArr = [];

    this.joinProcessorNode.config.properties.joins.map((obj) => {
      const streamObj = inputStreamsArr.find((s) => {
        return s.streamId === obj.stream;
      });
      const edgeObj = currentEdges.find((e) => {
        return streamObj.id === e.streamGrouping.streamId;
      });
      let edgeData = {
        fromId: edgeObj.source.nodeId,
        toId: edgeObj.target.nodeId,
        streamGroupings: [
          {
            streamId: edgeObj.streamGrouping.streamId,
            grouping: 'FIELDS',
            fields: [obj.key]
          }
        ]
      };
      edgeIdArr.push(edgeObj.edgeId);
      edgeDataArr.push(edgeData);
    });
    return {edgeIdArr,edgeDataArr};
  }

  /*
    updateEdgesForSelectedStream update Stream Edges
    Multiple JoinType Streams and fields
  */
  updateEdgesForSelectedStream(){
    const {currentEdges,topologyId, versionId} = this.props;
    const {inputStreamsArr} = this.state;

    const streamObj = inputStreamsArr.find((s) => {
      return s.streamId === this.joinProcessorNode.config.properties.from.stream;
    });
    const edgeObj = currentEdges.find((e) => {
      return streamObj.id === e.streamGrouping.streamId;
    });

    let edgeDataWithFormKey = {
      fromId: edgeObj.source.nodeId,
      toId: edgeObj.target.nodeId,
      streamGroupings: [
        {
          streamId: edgeObj.streamGrouping.streamId,
          grouping: 'FIELDS',
          fields: [this.joinProcessorNode.config.properties.from.key]
        }
      ]
    };
    const edgeId = edgeObj.edgeId;
    return {edgeId,edgeDataWithFormKey};
  }

  /*
    handleSave Method is responsible for joinProcessorNode
    config object is created with fields data example "fromKey = joinFromStreamKey"
    config.join objectArray is created with "type,stream,key,with".
  */
  handleSave(name, description){
    let {topologyId, versionId, nodeType, currentEdges} = this.props;
    let {
      outputKeys,
      windowNum,
      slidingNum,
      durationType,
      slidingDurationType,
      intervalType,
      parallelism,
      outputStreamFields,
      joinFromStreamName,
      joinFromStreamKey,
      joinStreams,
      inputStreamsArr,
      fieldList,
      outputGroupByDotKeys
    } = this.state;
    let fromStreamObj = fieldList.find((field) => {
      return field.name === joinFromStreamKey;
    });
    let fromKey = joinFromStreamKey;
    if (fromStreamObj) {
      fromKey = this.formatNestedField(fromStreamObj);
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
      from: {
        stream: joinFromStreamName,
        key: fromKey
      },
      joins: [],
      outputKeys: finalOutputKeys,
      window: {
        windowLength: {
          class: intervalType
        },
        slidingInterval: {
          class: intervalType
        },
        tsField: null,
        lagMs: 0
      },
      outputStream: this.streamData.streamId
    };

    // config.join objectArray is created from a joinStreams
    // by getting a streamObj from a key
    joinStreams.map((s) => {
      let key = s.key;
      let streamObj = s.keyOptions.find((field) => {
        return field.name === key;
      });
      if (streamObj) {
        key = this.formatNestedField(streamObj);
      }
      configObj.joins.push({type: s.type, stream: s.stream, key: key, with: s.with});
    });

    // interval and duration fields value are set here.
    if (intervalType === '.Window$Duration') {
      configObj.window.windowLength.durationMs = Utils.numberToMilliseconds(windowNum, durationType);
      if (slidingNum !== '') {
        configObj.window.slidingInterval = {
          class: intervalType,
          durationMs: Utils.numberToMilliseconds(slidingNum, slidingDurationType)
        };
      }
    } else if (intervalType === '.Window$Count') {
      configObj.window.windowLength.count = windowNum;
      if (slidingNum !== '') {
        configObj.window.slidingInterval = {
          class: intervalType,
          count: slidingNum
        };
      }
    }

    // outputStreams data is formated for the server
    const streamFields  = ProcessorUtils.generateOutputStreamsArr(this.streamData.fields,0);

    if(this.joinProcessorNode.outputStreams.length > 0){
      this.joinProcessorNode.outputStreams[0].fields = streamFields;
    } else {
      this.joinProcessorNode.outputStreams.push({fields: streamFields, streamId: this.streamData.streamId});
    }
    //this.joinProcessorNode is update with the above data
    this.joinProcessorNode.config.properties = configObj;
    this.joinProcessorNode.config.properties.parallelism = parallelism;
    this.joinProcessorNode.name = name;
    this.joinProcessorNode.description = description;

    let promiseArr = [];
    // update joinProcessorNodeprocessorNode
    promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, this.joinProcessorNode.id, {body: JSON.stringify(this.joinProcessorNode)}));

    // update edge with FROM KEY of selected Streams
    const {edgeId,edgeDataWithFormKey} = this.updateEdgesForSelectedStream();
    promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'edges', edgeId, {body: JSON.stringify(edgeDataWithFormKey)}));

    // update edges with selected JoinTypes obj key with particular edgeId for multiple Join using Array of edgeDataArr;
    const {edgeIdArr,edgeDataArr} = this.updateEdgesForJoinTypeObject();
    _.map(edgeDataArr, (edgeObj,index) => {
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'edges', edgeIdArr[index], {body: JSON.stringify(edgeObj)}));
    });
    return Promise.all(promiseArr);
  }

  render(){
    let {topologyId, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
    let {
      editMode,
      showError,
      fieldList,
      outputKeys,
      parallelism,
      intervalType,
      intervalTypeArr,
      windowNum,
      slidingNum,
      durationType,
      slidingDurationType,
      durationTypeArr,
      joinFromStreamName,
      joinFromStreamKey,
      inputStreamsArr,
      joinTypes,
      showLoading,
      outputFieldsList,
      outputKeysObjArr
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
                    <label>Select Stream</label>
                  </OverlayTrigger>
                  <div>
                    <Select value={joinFromStreamName} options={inputStreamsArr} onChange={this.handleJoinFromStreamChange.bind(this)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                  </div>
                </div>
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Field name</Popover>}>
                    <label>Select Field {this.state.joinStreams.length
                        ? (
                          <strong>with</strong>
                        )
                        : ''}</label>
                  </OverlayTrigger>
                  <div>
                    <Select value={joinFromStreamKey} options={this.state.joinFromStreamKeys} onChange={this.handleJoinFromKeyChange.bind(this)} required={true} disabled={disabledFields || joinFromStreamName === ''} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                  </div>
                </div>
              </div>
              {this.state.joinStreams.length
                ? <div className="form-group row no-margin">
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Type of join</Popover>}>
                        <label>Join Type</label>
                      </OverlayTrigger>
                    </div>
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of input stream</Popover>}>
                        <label>Select Stream</label>
                      </OverlayTrigger>
                    </div>
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Field name</Popover>}>
                        <label>Select Field</label>
                      </OverlayTrigger>
                    </div>
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of input stream</Popover>}>
                        <label>
                          <strong>With </strong>
                          Stream</label>
                      </OverlayTrigger>
                    </div>
                  </div>
                : ''
  }
              {this.state.joinStreams.map((s, i) => {
                return (
                  <div className="form-group row" key={i}>
                    <div className="col-sm-3">
                      <Select value={s.type} options={joinTypes} onChange={this.handleJoinTypeChange.bind(this, i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}/>
                    </div>
                    <div className="col-sm-3">
                      <Select value={s.stream} options={s.streamOptions} onChange={this.handleJoinStreamChange.bind(this, i)} required={true} disabled={disabledFields || s.streamOptions.length === 0} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                    </div>
                    <div className="col-sm-3">
                      <Select value={s.key} options={s.keyOptions} onChange={this.handleJoinKeyChange.bind(this, i)} required={true} disabled={disabledFields || s.stream === '' || s.keyOptions.length === 0} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                    </div>
                    <div className="col-sm-3">
                      <Select value={s.with} options={s.withOptions} onChange={this.handleJoinWithChange.bind(this, i)} required={true} disabled={disabledFields || s.withOptions.length === 0} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                    </div>
                  </div>
                );
              })
  }
              <div className="form-group">
                <div className="row">
                  <div className="col-sm-12">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window interval type</Popover>}>
                      <label>Window Interval Type
                        <span className="text-danger">*</span>
                      </label>
                    </OverlayTrigger>
                    <div>
                      <Select value={intervalType} options={intervalTypeArr} onChange={this.commonHandlerChange.bind(this,'intervalType')} required={true} disabled={disabledFields} clearable={false}/>
                    </div>
                  </div>
                  {/*<div className="col-sm-6">
                                                      <label>Parallelism</label>
                                                      <input
                                                          name="parallelism"
                                                          value={parallelism}
                                                          onChange={this.handleValueChange.bind(this)}
                                                          type="number"
                                                          className="form-control"
                                                          required={true}
                                                          disabled={!editMode}
                                                          min="1"
                                                          inputMode="numeric"
                                                      />
                                                  </div>*/}
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-6">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window interval duration</Popover>}>
                    <label>Window Interval
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                  <div className="row">
                    <div className="col-sm-6">
                      <input name="windowNum" value={windowNum} onChange={this.handleValueChange.bind(this)} type="number" className="form-control" required={true} disabled={disabledFields} min="0" inputMode="numeric"/>
                    </div>
                    {intervalType === '.Window$Duration'
                      ? <OverlayTrigger trigger={['hover']} placement="top" overlay={<Popover id="popover-trigger-hover">Duration type</Popover>}>
                        <div className="col-sm-6">
                          <Select value={durationType} options={durationTypeArr} onChange={this.commonHandlerChange.bind(this,'durationType')} required={true} disabled={disabledFields} clearable={false}/>
                        </div>
                        </OverlayTrigger>
                      : null}
                  </div>
                </div>
                <div className="col-sm-6">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Sliding interval duration</Popover>}>
                    <label>Sliding Interval</label>
                  </OverlayTrigger>
                  <div className="row">
                    <div className="col-sm-6">
                      <input name="slidingNum" value={slidingNum} onChange={this.handleValueChange.bind(this)} type="number" className="form-control" required={true} disabled={disabledFields} min="0" inputMode="numeric"/>
                    </div>
                    {intervalType === '.Window$Duration'
                      ? <OverlayTrigger trigger={['hover']} placement="top" overlay={<Popover id="popover-trigger-hover">Duration type</Popover>}>
                        <div className="col-sm-6">
                          <Select value={slidingDurationType} options={durationTypeArr} onChange={this.commonHandlerChange.bind(this,'slidingDurationType')} required={true} disabled={disabledFields} clearable={false}/>
                        </div>
                        </OverlayTrigger>
                      : null}
                  </div>
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

JoinNodeForm.contextTypes = {
  ParentForm: PropTypes.object
};
