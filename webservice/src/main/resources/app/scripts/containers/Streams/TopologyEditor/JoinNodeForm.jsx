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
import ProcessorUtils, {Streams}  from '../../../utils/ProcessorUtils';
import TimeStamp from '../../../components/TimeStamp';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import SelectInputComponent from '../../../components/SelectInputComponent';

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
      intervalType: ".Window$EventTime",
      intervalTypeArr: [
        {
          value: ".Window$EventTime",
          label: "Event Time"
        },
        {
          value: ".Window$Duration",
          label: "Processing Time"
        }, {
          value: ".Window$Count",
          label: "Count"
        }
      ],
      windowNum: 1,
      slidingNum: 1,
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
      outputGroupByDotKeys : [],
      lagMs: 0,
      tsFields: null,
      validationErrors: {},
      outputKeysObjArr : []
    };
    this.state = obj;
    this.fetchDataAgain = false;
    this.streamData = {};
    this.legends=[];
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
      const obj = {
        name : inputStreamFromContext[i].streamId,
        fields : stream.fields,
        optional : false,
        type : "NESTED"
      };
      fields.push(obj);
      this.legends.push(stream.streamId);

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

    const tempFieldsArr = ProcessorUtils.getSchemaFields(fields, 0,false, [], false);
    // create a uniqueID for select2 to support duplicate label in options
    _.map(tempFieldsArr, (f) => {
      f.uniqueID = f.keyPath.split('.').join('-')+"_"+f.name;
      f.alias = f.name;
    });
    const unModifyFieldList = ProcessorUtils.getSchemaFields(unModifyList, 0,false);

    const streams = new Streams(inputStreamFromContext);
    const filteredStreams = streams.filterByType('LONG');
    streams.toSelectOption(filteredStreams);

    let stateObj = {
      fieldList: unModifyFieldList,
      parallelism: configFields.parallelism || 1,
      outputKeys: configFields.outputKeys
        ? configFields.outputKeys.map((key) => {
          return ProcessorUtils.splitNestedKey(key);
        })
        : [],
      inputStreamsArr: inputStreamFromContext,
      joinStreams: joinStreams,
      outputFieldsList : tempFieldsArr,
      tsFieldsOptions: filteredStreams
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
      if(inputStreamFromContext.length === 1){
        stateObj.joinFromStreamName = inputStreamFromContext[0].streamId;
        stateObj.joinFromStreamKeys = ProcessorUtils.getSchemaFields(inputStreamFromContext[0].fields, 0,false);
      }
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
      stateObj.joinFromStreamKeys = selectedStream !== undefined ? ProcessorUtils.getSchemaFields(selectedStream.fields, 0, false) : '';
    }

    //set values of joins Type
    const configJoinFields = this.joinProcessorNode.config.properties.joins;
    if(!_.isEmpty(configJoinFields)){
      _.map(configJoinFields, (o , index) => {
        let joinStreamOptions = this.getStreamObjArray(o.stream,null);
        if(stateObj.joinStreams[index] !== undefined){
          stateObj.joinStreams[index].type = o.type;
          stateObj.joinStreams[index].stream = o.stream;
          stateObj.joinStreams[index].streamOptions = this.getStreamObjArray(o.with,stateObj.joinFromStreamName);
          stateObj.joinStreams[index].key = ProcessorUtils.splitNestedKey(o.key);
          stateObj.joinStreams[index].keyOptions = ProcessorUtils.getSchemaFields(joinStreamOptions.length ? joinStreamOptions[0].fields : [], 0, false);
          stateObj.joinStreams[index].with = o.with;
          stateObj.joinStreams[index].withOptions = this.getStreamObjArray(o.stream,'options');
        }
      });

      // If new stream is connected after configuration
      if(configJoinFields.length !== stateObj.joinStreams.length){
        let tempObj={};
        _.map(stateObj.joinStreams, (stJoin,i) => {
          if(configJoinFields[i] === undefined){
            const ind = _.findIndex(inputStreamsArr, (input) => { return tempObj[input.streamId] === undefined;});
            if(ind !== -1){
              let joinStreamOptions = this.getStreamObjArray(inputStreamsArr[ind].streamId, null);
              stJoin.streamOptions = joinStreamOptions;
              stJoin.keyOptions = ProcessorUtils.getSchemaFields(joinStreamOptions[0].fields, 0, false);
              stJoin.withOptions = this.getStreamObjArray(joinStreamOptions[0].streamId, 'options');
            }
          } else {
            const index = (tempObj[stJoin.stream] === stJoin.stream && tempObj[stJoin.with] === stJoin.with) ? 0 : -1;
            if(index === -1){
              tempObj[stJoin.stream] = stJoin.stream;
              tempObj[stJoin.with] = stJoin.with;
            }
          }
        });
      }
    }else if(this.state.inputStreamsArr.length > 1){
      this.handleJoinFromStreamChange(this.state.inputStreamsArr[0]);
      stateObj.joinStreams = this.state.joinStreams;
    }

    //set values of windows fields
    const configWindowObj = this.joinProcessorNode.config.properties.window;
    if(!_.isEmpty(configWindowObj)){
      if (configWindowObj.windowLength.class === '.Window$Duration') {
        if(configWindowObj.tsFields != null){
          stateObj.intervalType = '.Window$EventTime';
          stateObj.lagMs = configWindowObj.lagMs;
          stateObj.tsFields = this.gettsFieldssFormSelection(configWindowObj.tsFields);
        }else{
          stateObj.intervalType = '.Window$Duration';
        }
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

    let alaisArray=[];
    // set the outputKeys And outputFieldsObj for parentContext
    const outputKeysAFormServer = this.joinProcessorNode.config.properties.outputKeys.map((fieldName)=>{
      // push streamName and alias to populate further.
      alaisArray.push({key : fieldName.split(' as ')[0], alias : fieldName.split(' as ')[1]});
      return fieldName.split(' as ')[0];
    });

    // remove the dot from the keys
    stateObj.outputKeys = _.filter(outputKeysAFormServer, (key) => {
      const outputKey = ProcessorUtils.splitNestedKey(key);
      return _.find(outputFieldsList, {name : outputKey}) !== undefined;
    });

    // get the keyObj from the outputFieldsList for the particular key
    const outputKeysObjArray = ProcessorUtils.createOutputFieldsObjArr(outputKeysAFormServer,outputFieldsList);

    // To remove the undefined if the schemsa get change from the source Node
    const outputKeysObjArr = _.compact(outputKeysObjArray);

    stateObj.outputKeysObjArr = _.map(outputKeysObjArr, (obj,i) => {
      // const streamName = alaisArray[i].key.split(':');
      // assign tha alias if the streamName and field name is equal.... to obj.
      // if(obj.name === streamName[1] && obj.keyPath === streamName[0]){
      //   obj.alias = alaisArray[i].alias;
      // }
      obj.alias = alaisArray[i].alias;
      return obj;
    });

    const keyData = ProcessorUtils.createSelectedKeysHierarchy(outputKeysObjArr,outputFieldsList);
    stateObj.outputStreamFields=[];
    _.map(keyData,(o) => {
      stateObj.outputStreamFields = _.concat(stateObj.outputStreamFields, o.fields ? o.fields : o);
    });

    ProcessorUtils.addChildren(outputKeysObjArr, outputFieldsList);

    this.streamData = {
      streamId: this.joinProcessorNode.outputStreams[0].streamId,
      fields: outputKeysObjArr
    };

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(outputKeysObjArr);
    stateObj.outputGroupByDotKeys = ProcessorUtils.modifyGroupKeyByDots(gKeys);
    stateObj.showLoading = false;

    this.setState(stateObj);
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  gettsFieldssFormSelection = (tsFields) => {
    const {tsFieldsOptions} = this.state;
    return _.filter(tsFields, (v) => {
      return _.findIndex(tsFieldsOptions, (o) => o.streamId === v.split(':')[0]) !== -1;
    });
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

  renderValueInputChange = (node,value) => {
    const {outputKeysObjArr,outputStreamFields,outputFieldsList} = this.state;
    const index = _.findIndex(outputKeysObjArr, (output) => output === node);
    if(index !== -1){
      outputKeysObjArr[index].alias = value;
      const keyData = ProcessorUtils.createSelectedKeysHierarchy(outputKeysObjArr,outputFieldsList);
      // this.streamData.fields = this.generateStreamFields(keyData);
      this.streamData.fields = outputKeysObjArr;
      this.setState({outputStreamFields:keyData,outputKeysObjArr});
      this.context.ParentForm.setState({outputStreamObj: this.streamData},() => {
        // let elNode = this.selectInputChange.refs[node.keyPath+'_'+node.name];
        // const len = value.length;
        // elNode.focus();
        // elNode.setSelectionRange(len,len);
      });
    }
  }

  renderValueComponent(node){
    const {outputKeysObjArr} = this.state;
    return (
      <SelectInputComponent ref={(ref) => this.selectInputChange = ref} node={node} legends={this.legends} outputKeysObjArr={outputKeysObjArr} renderValueInputChange={this.renderValueInputChange.bind(this)} />
    );
  }

  /*
    getStreamObjArray Method accept the string of streams
    return
    streamsObj Array filter from inputStreamsArr
  */
  getStreamObjArray(string,keyOption){
    const {inputStreamsArr} = this.state;
    return inputStreamsArr.filter((s) => {
      return keyOption === null
              ? s.streamId === string
              : keyOption !== 'options'
                ? s.streamId !== string && s.streamId !== keyOption
                : s.streamId !== string ;
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
      this.setOutputFields(arr);
    } else {
      this.streamData.fields = [];
      this.setState({outputKeysObjArr : arr, outputKeys: [], outputStreamFields: [],outputGroupByDotKeys : []}, () => {
        this.validateField('outputKeysObjArr');
      });
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
    }
  }

  handleSelectAllOutputFields = () => {
    const {inputStreamsArr,outputFieldsList} = this.state;
    let tempFields = _.cloneDeep(outputFieldsList);
    const allOutPutFields = _.filter(tempFields, (t) => {
      return _.findIndex(inputStreamsArr,(input) => {return input.streamId === t.name;}) === -1;
    });
    this.setOutputFields(allOutPutFields);
  }

  setOutputFields = (arr) => {
    let {outputFieldsList} = this.state;
    arr = JSON.parse(JSON.stringify(arr));
    ProcessorUtils.removeChildren(arr);
    ProcessorUtils.addChildren(arr, outputFieldsList);


    const keyData = ProcessorUtils.createSelectedKeysHierarchy(arr,outputFieldsList);
    // this.streamData.fields = this.generateStreamFields(keyData);
    this.streamData.fields = arr;

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
    const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys);
    this.setState({outputKeysObjArr : arr, outputKeys: keys, outputStreamFields: keyData,outputGroupByDotKeys : groupKeysByDots}, () => {
      this.validateField('outputKeysObjArr');
    });
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  validateField(fieldName, _value){
    const value = _value || this.state[fieldName];
    let validate;
    if (_.isUndefined(value) || _.isNull(value)) {
      this.state.validationErrors[fieldName] = "Required!";
      validate = false;
    } else {
      if (value instanceof Array) {
        if (value.length === 0) {
          this.state.validationErrors[fieldName] = "Required!";
          validate = false;
        }else{
          this.state.validationErrors[fieldName] = "";
          validate = true;
        }
      } else if (value === '' || (typeof value == 'string' && value.trim() === '') || _.isUndefined(value)) {
        this.state.validationErrors[fieldName] = "Required!";
        validate = false;
      } else if(typeof value === "number" && value !== '' && value <= 0 && fieldName !== 'lagMs'){
        const name = fieldName === "windowNum" ? "Window" : "Sliding" ;
        this.state.validationErrors[fieldName] = name +" interval should be greater than '0'";
        validate = false;
      } else {
        this.state.validationErrors[fieldName] = "";
        validate = true;
      }
    }
    this.forceUpdate();
    return validate;
  }

  validatetsFields(){
    const {tsFields,inputStreamsArr,intervalType} = this.state;

    const arr=[];
    inputStreamsArr.forEach(function(inputStream){
      const index = _.findIndex(tsFields, (ts) => {
        return inputStream.streamId === ts.split(':')[0];
      });
      if(index === -1){
        arr.push(inputStream.streamId);
      }
    });
    if(arr.length && intervalType === '.Window$EventTime'){
      FSReactToastr.error(
        <CommonNotification flag="error" content={"Select timestamp field from the "+arr.join(', ')}/>, '', toastOpt);
    }
    return arr.length && intervalType === '.Window$EventTime' ? arr: [];
  }

  /*
    validateData check the validation of
     outputKeys,windowNum and joinStreams array
  */
  validateData() {
    let {outputKeys, windowNum, joinStreams, intervalType, tsFields, lagMs} = this.state;
    let validData = true;
    if(!this.validateField('joinFromStreamName')){
      validData = false;
    }
    if(!this.validateField('joinFromStreamKey')){
      validData = false;
    }
    if (!this.validateField('outputKeysObjArr') || outputKeys.length !== this.aliasValidator()) {
      validData = false;
    }
    if (!this.validateField('windowNum')){
      validData = false;
    }
    if(intervalType === '.Window$EventTime' && (!this.validateField('tsFields') || this.validatetsFields().length || !this.validateField('lagMs'))){
      validData = false;
    }
    joinStreams.map((s, i) => {
      if(!this.validateField('joinStreams'+i+'type', s.type)){
        validData = false;
      }
      if(!this.validateField('joinStreams'+i+'stream', s.stream)){
        validData = false;
      }
      if(!this.validateField('joinStreams'+i+'key', s.key)){
        validData = false;
      }
      if(!this.validateField('joinStreams'+i+'with', s.with)){
        validData = false;
      }
      /*if (s.stream === '' || s.type === '' || s.key === '' || s.with === '') {
        validData = false;
      }*/
    });
    return validData;
  }

  generateStreamFields = (keyData) => {
    let tempFieldsArr=[];
    _.map(keyData,(o) => {
      tempFieldsArr = _.concat(tempFieldsArr, o.fields ? o.fields : o);
    });
    return tempFieldsArr;
  }



  /*
    commonHandlerChange Method accept keyType, obj and it handles multiple event [durationType,slidingDurationType,intervalType]
    params@ keyType = string 'durationType'
    params@ obj = selected obj
  */
  commonHandlerChange(keyType,obj){
    if(obj){
      const keyName = keyType.trim();

      if(keyName === "durationType"){
        this.setState({durationType : obj.value,slidingDurationType: obj.value});
      }else if(keyName === "slidingDurationType"){
        this.setState({slidingDurationType : obj.value});
      }else if(keyName === 'intervalType'){
        const stateObj = {intervalType : obj.value};
        if(obj.value !== '.Window$EventTime'){
          stateObj.tsFields = null;
          stateObj.lagMs = 0;
        }
        this.setState(stateObj);
      }else if(keyName === 'tsFields'){
        this.setState({
          tsFields: obj
        }, () => {
          this.validateField('tsFields');
        });
      }
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
    this.setState(obj, () => {
      if(name !== "lagMs"){
        this.validateField(name);
      }
    });
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
      }, () => {
        this.validateField('joinFromStreamName');
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
    }, () => {
      this.validateField('joinFromStreamKey');
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
    this.setState({joinStreams: joinStreams}, () => {
      this.validateField('joinStreams'+key+'type', joinStreams[key].type);
    });
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
    this.setState({joinStreams: joinStreams}, () => {
      this.validateField('joinStreams'+key+'stream', joinStreams[key].stream);
    });
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
    this.setState({joinStreams: joinStreams}, () => {
      this.validateField('joinStreams'+key+'key', joinStreams[key].key);
    });
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
    this.setState({joinStreams: joinStreams}, () => {
      this.validateField('joinStreams'+key+'with', joinStreams[key].with);
    });
  }

  aliasValidator = () => {
    const {outputKeysObjArr} = this.state;
    let  arr = _.uniqBy(outputKeysObjArr, 'alias');
    return arr.length;
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
      outputGroupByDotKeys,
      lagMs,
      tsFields,
      outputKeysObjArr
    } = this.state;
    if(intervalType === '.Window$EventTime'){
      intervalType = '.Window$Duration';
    }
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

    let finalOutputKeys = modifyGroup.map((keyName,i)=>{
      if(keyName.search(':') === -1){
        return keyName +' as ' + outputKeysObjArr[i].alias;
      } else {
        let fieldName = keyName.split(':')[1];
        return keyName +' as ' + outputKeysObjArr[i].alias;
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
        tsFields: tsFields,
        lagMs: lagMs
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
    const streamFields  = ProcessorUtils.generateOutputStreamsArr(this.streamData.fields,0,'alias');

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
      outputKeysObjArr,
      lagMs,
      tsFields,
      tsFieldsOptions,
      validationErrors
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
                    <label>Select Stream<span className="text-danger">*</span></label>
                  </OverlayTrigger>
                  <div>
                    <Select value={joinFromStreamName} options={inputStreamsArr} className={!!validationErrors.joinFromStreamName ? 'invalidSelect' : ''} onChange={this.handleJoinFromStreamChange.bind(this)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                    <p className="text-danger">{validationErrors.joinFromStreamName}</p>
                  </div>
                </div>
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Field name</Popover>}>
                    <label>Select Field {this.state.joinStreams.length
                        ? (
                          <strong>with</strong>
                        )
                        : ''}
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                  <div>
                    <Select value={joinFromStreamKey} options={this.state.joinFromStreamKeys} className={!!validationErrors.joinFromStreamKey ? 'invalidSelect' : ''} onChange={this.handleJoinFromKeyChange.bind(this)} required={true} disabled={disabledFields || joinFromStreamName === ''} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                    <p className="text-danger">{validationErrors.joinFromStreamKey}</p>
                  </div>
                </div>
              </div>
              {this.state.joinStreams.length
                ? <div className="form-group row no-margin">
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Type of join</Popover>}>
                        <label>Join Type
                          <span className="text-danger">*</span>
                        </label>
                      </OverlayTrigger>
                    </div>
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of input stream</Popover>}>
                        <label>Select Stream
                          <span className="text-danger">*</span>
                        </label>
                      </OverlayTrigger>
                    </div>
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Field name</Popover>}>
                        <label>Select Field
                          <span className="text-danger">*</span>
                        </label>
                      </OverlayTrigger>
                    </div>
                    <div className="col-sm-3">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of input stream</Popover>}>
                        <label>
                          <strong>With </strong>
                          Stream
                          <span className="text-danger">*</span>
                        </label>
                      </OverlayTrigger>
                    </div>
                  </div>
                : ''
  }
              {this.state.joinStreams.map((s, i) => {
                return (
                  <div className="form-group row" key={i}>
                    <div className="col-sm-3">
                      <Select value={s.type} options={joinTypes} className={!!(validationErrors['joinStreams'+i+'type']) ? 'invalidSelect' : ''} onChange={this.handleJoinTypeChange.bind(this, i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}/>
                    </div>
                    <div className="col-sm-3">
                      <Select value={s.stream} options={s.streamOptions} className={!!(validationErrors['joinStreams'+i+'stream']) ? 'invalidSelect' : ''} onChange={this.handleJoinStreamChange.bind(this, i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                    </div>
                    <div className="col-sm-3">
                      <Select value={s.key} options={s.keyOptions} className={!!(validationErrors['joinStreams'+i+'key']) ? 'invalidSelect' : ''} onChange={this.handleJoinKeyChange.bind(this, i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
                    </div>
                    <div className="col-sm-3">
                      <Select value={s.with} options={s.withOptions} className={!!(validationErrors['joinStreams'+i+'with']) ? 'invalidSelect' : ''} onChange={this.handleJoinWithChange.bind(this, i)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                    </div>
                  </div>
                );
              })
  }
              <div className="form-group">
                <div className="row">
                  <div className="col-sm-6">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window interval type</Popover>}>
                      <label>Window Type
                        <span className="text-danger">*</span>
                      </label>
                    </OverlayTrigger>
                    <div>
                      <Select value={intervalType} options={intervalTypeArr} onChange={this.commonHandlerChange.bind(this,'intervalType')} required={true} disabled={disabledFields} clearable={false}/>
                    </div>
                  </div>
                  {intervalType === '.Window$EventTime' ?
                  [
                    <div className="col-sm-3" key={10}>
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Field (of type long) indicating the event's time</Popover>}>
                        <label>Timestamp Fields
                          <span className="text-danger">*</span>
                        </label>
                      </OverlayTrigger>
                      {/*<Select value={tsFields} options={tsFieldsOptions} className={!!validationErrors.tsFields ? 'invalidSelect' : ''} onChange={this.commonHandlerChange.bind(this, 'tsFields')} multi={true} required={true} disabled={disabledFields} valueKey="uniqueID" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>*/}
                      <TimeStamp value={tsFields} options={tsFieldsOptions} onChange={this.commonHandlerChange.bind(this, 'tsFields')}/>
                      <p className="text-danger">{validationErrors.tsFields}</p>
                    </div>,
                    <div className="col-sm-3" key={20}>
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Max time to wait after window close, for a late arriving event to be part of its window</Popover>}>
                        <label>Lag Milliseconds
                          <span className="text-danger">*</span>
                        </label>
                      </OverlayTrigger>
                      <input name="lagMs" value={lagMs} onChange={this.handleValueChange.bind(this)} type="number" className="form-control"  required={true} disabled={disabledFields} min="0" inputMode="numeric"/>
                    </div>
                  ]
                    :
                    null
                  }
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
                      <input name="windowNum" value={windowNum} onChange={this.handleValueChange.bind(this)} type="number" className={"form-control " + (!!validationErrors.windowNum ? 'invalidInput' : '')} required={true} disabled={disabledFields} min="0" inputMode="numeric"/>
                      <p className="text-danger">{validationErrors.windowNum}</p>
                    </div>
                    {intervalType === '.Window$Duration' || intervalType === '.Window$EventTime'
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
                      <input name="slidingNum" value={slidingNum} onChange={this.handleValueChange.bind(this)} type="number" className={"form-control "+ (!!validationErrors.slidingNum ? 'invalidInput' : '')} required={true} disabled={disabledFields} min="0" inputMode="numeric"/>
                      <p className="text-danger">{validationErrors.slidingNum}</p>
                    </div>
                    {intervalType === '.Window$Duration' || intervalType === '.Window$EventTime'
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
                <div className="col-sm-8">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Output Fields<br /> Alias for output fields are mandatory</Popover>}>
                    <label>Output Fields
                      <span className="text-danger">*</span>
                      <span style={{marginLeft : '10px'}}className="text-info"><i className="fa fa-info-circle" aria-hidden="true"></i> Alias for output fields are mandatory</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div className="col-sm-4">
                  <label className="pull-right">
                    <a href="javascript:void(0)" onClick={this.handleSelectAllOutputFields}>Select All</a>
                  </label>
                </div>
                <div className="row">
                  <div className="col-sm-12">
                    <Select className={"menu-outer-top " + (!!validationErrors.outputKeysObjArr ? 'invalidSelect' : '')} value={outputKeysObjArr} options={ProcessorUtils.filterOptions(outputKeysObjArr, outputFieldsList)} onChange={this.handleFieldsChange.bind(this)} multi={true} required={true} disabled={disabledFields} valueKey="uniqueID" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)} valueRenderer={this.renderValueComponent.bind(this)} backspaceRemoves={false} deleteRemoves={false}/>
                    <p className="text-danger">{validationErrors.outputKeysObjArr}</p>
                  </div>
                </div>
                {inputStreamsArr.length > 1 ?
                <div className="row">
                  <div className="col-sm-12" style={{marginTop: '5px'}}>
                    <label>OUTPUT STREAM LEGENDS : </label>
                      {
                        _.map(this.legends, (legend,i) => {
                          return (
                            <div  key={legend+i} style={{display:'inline', margin: '0px 10px'}}>
                              <label className="label label-info" style={{color: 'white', padding: '2px 6px'}}>{(++i)}</label> =  <span>{legend}</span>
                            </div>
                          );
                        })
                      }
                  </div>
                </div>
                : null }
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
