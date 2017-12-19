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
import {Select2 as Select} from '../../../utils/SelectUtils';
import {OverlayTrigger, Popover,Checkbox} from 'react-bootstrap';
import Utils from '../../../utils/Utils';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils  from '../../../utils/ProcessorUtils';
import RealTimeJoinStreamComponent  from './RealTimeJoinStreamComponent';
import SelectInputComponent from '../../../components/SelectInputComponent';

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
      bufferType: '',
      bufferTypeArr: this.getJoinTypeOptions('buffer'),
      countVal : '',
      showInputError : false,
      outputKeys: [],
      outputKeysObjArr :[],
      outputFieldsList: [],
      outputStreamFields: [],
      outputGroupByDotKeys : [],
      unique : false,
      rtJoinStreamArr : [{
        rtJoinTypeSelected : '',
        rtJoinTypeStreamObj : '',
        bufferType : '',
        bufferSize : '',
        unique : false,
        conditions : [{
          firstKey : '',
          secondKey : '',
          firstKeyOptions : [],
          secondKeyOptions : []
        }],
        showInputError : false
      }],
      joinStreamGroup : [{conditions : [{firstKey : '',secondKey : ''}]}],
      inputStreamsArr: [],
      validationErrors: {}

    };
    this.state = obj;
    this.streamData = {};
    this.legends = [];
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

    let tempJoinStreamArr = _.cloneDeep(this.state.rtJoinStreamArr);
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

      this.legends.push(stream.streamId);

      // UnModify fieldsList
      unModifyList.push(...stream.fields);

      // for multiple inputStream
      if(i > 1){
        tempJoinStreamArr.push({
          rtJoinTypeSelected : '',
          rtJoinTypeStreamObj : '',
          bufferType : '',
          bufferSize : '',
          unique : false,
          conditions : [{
            firstKey : '',
            secondKey : '',
            firstKeyOptions : [],
            secondKeyOptions : []
          }],
          showInputError : false
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

    let stateObj = {
      fieldList: unModifyFieldList,
      parallelism: configFields.parallelism || 1,
      outputKeys: configFields.outputKeys
        ? configFields.outputKeys.map((key) => {
          return ProcessorUtils.splitNestedKey(key);
        })
        : undefined,
      inputStreamsArr: inputStreamFromContext,
      outputFieldsList : tempFieldsArr,
      rtJoinStreamArr : tempJoinStreamArr
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

  getBufferKey = (data) => {
    let intervalType = '';
    _.map(_.keys(data),(k) => {
      _.map(this.state.bufferTypeArr, (buffer) => {
        if(k === buffer.value.toLowerCase()){
          intervalType = k.toLowerCase();
        }
      });
    });
    return intervalType;
  }

  createGroupFields = (firstStream, secondStream) => {
    let f_key ='',s_key ='';
    if(firstStream){
      const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey([firstStream]);
      const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys,'removeParent');
      f_key = groupKeysByDots[0];
    }
    if(secondStream){
      const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey([secondStream]);
      const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys,'removeParent');
      s_key = groupKeysByDots[0];
    }

    return {f_key,s_key};
  }

  getConditionFields = (conData , inputStreams) => {
    let conArr = [],streamObj={},conGroup =[];
    // remove the first index if 'equal' because its hardcoded on save
    conData.splice(0,1);
    const f_Stream =  conData[0].split(':');
    const f_Name = f_Stream[1].split('.').pop();
    const s_Stream = conData[1].split(':');
    const s_Name = s_Stream[1].split('.').pop();
    const f_Obj = _.find(inputStreams, (stream) => {return stream.streamId === f_Stream[0];});
    const s_Obj = _.find(inputStreams, (stream) => {return stream.streamId === s_Stream[0];});
    // streamObj.firstKeyOptions = ProcessorUtils.getSchemaFields(f_Obj.fields, 0,false);
    // streamObj.secondKeyOptions = ProcessorUtils.getSchemaFields(s_Obj.fields, 0,false);
    // streamObj.firstKey = ProcessorUtils.getKeyList(f_Name ,streamObj.firstKeyOptions );
    // streamObj.secondKey = ProcessorUtils.getKeyList(s_Name ,streamObj.secondKeyOptions );
    streamObj.firstKeyStream = f_Obj;
    streamObj.secondKeyStream = s_Obj;
    streamObj.firstKey = conData[0];
    streamObj.secondKey = conData[1];
    // const {f_key,s_key} = this.createGroupFields(streamObj.firstKey , streamObj.secondKey);
    conGroup.push({firstKey : streamObj.firstKey.split(':').pop(),secondKey : streamObj.secondKey.split(':').pop()});
    conArr.push(streamObj);
    return {conArr,conGroup};
  }

  populateOutputStreamsFromServer = () => {
    let stateObj = {};
    const {inputStreamsArr,outputFieldsList} = this.state;
    let tempGroup = _.cloneDeep(this.state.joinStreamGroup);
    let tempArr = _.cloneDeep(this.state.rtJoinStreamArr);
    const fromData = this.rtJoinProcessorNode.config.properties.from;
    if(! _.isEmpty(fromData)){
      const streamObj = _.filter(inputStreamsArr, (stream) => {return stream.streamId === fromData.stream;});
      stateObj.unique = fromData.unique;
      stateObj.rtJoinStreamObj = streamObj[0];
      const type = this.getBufferKey(fromData);
      if(type === 'milliseconds'){
        stateObj.countVal = fromData[type];
      } else {
        /*const bufferSize = Utils.millisecondsToNumber(fromData[type]);
        stateObj.countVal = bufferSize.number;*/
        stateObj.countVal = fromData[type];
      }
      stateObj.bufferType = type.toUpperCase();
    }

    const joinData = this.rtJoinProcessorNode.config.properties.joins;
    if(! _.isEmpty(joinData)){
      _.map(joinData, (join , k) => {
        const bType = this.getBufferKey(join);
        tempArr[k].rtJoinTypeStreamObj = _.find(inputStreamsArr, (stream) => {return stream.streamId === join.stream;});
        tempArr[k].rtJoinTypeSelected = join.type;
        tempArr[k].unique = join.unique;
        tempArr[k].bufferType = bType.toUpperCase();
        if(bType === 'milliseconds'){
          tempArr[k].bufferSize = join[bType];
        } else {
          /*const bSize = Utils.millisecondsToNumber(join[bType]);
          tempArr[k].bufferSize = bSize.number;*/
          tempArr[k].bufferSize = join[bType];
        }
        let t_con=[],g_con=[];
        _.map(join.conditions, (c) => {
          const {conArr,conGroup} =  this.getConditionFields(c,inputStreamsArr);
          t_con.push(conArr[0]);
          g_con.push(conGroup[0]);
        });
        tempArr[k].conditions = t_con;
        tempGroup[k].conditions = g_con;
      });

      stateObj.rtJoinStreamArr = tempArr;
      stateObj.joinStreamGroup = tempGroup;
    }


    let alaisArray=[];
    // set the outputKeys And outputFieldsObj for parentContext
    const outputKeysAFormServer = this.rtJoinProcessorNode.config.properties.outputKeys.map((fieldName)=>{
      alaisArray.push({key : fieldName.split(' as ')[0], alias : fieldName.split(' as ')[1]});
      return fieldName.split(' as ')[0];
    });

    // remove the dot from the keys
    stateObj.outputKeys = _.filter(outputKeysAFormServer, (key) => {
      const outputKey = ProcessorUtils.splitNestedKey(key);
      return _.find(outputFieldsList, {name : outputKey}) !== undefined;
    });

    // get the keyObj from the outputFieldsList for the particular key
    const outputKeysObjArr = ProcessorUtils.createOutputFieldsObjArr(outputKeysAFormServer,outputFieldsList);

    // stateObj.outputKeysObjArr = outputKeysObjArr;
    stateObj.outputKeysObjArr = _.map(outputKeysObjArr, (obj,i) => {
      // const streamName = alaisArray[i].key.split(':');
      // assign tha alias if the streamName and field name is equal.... to obj.
      /*if(obj.name === streamName[1] && obj.keyPath === streamName[0]){
        obj.alias = alaisArray[i].alias;
      }*/
      obj.alias = alaisArray[i].alias;
      return obj;
    });


    const keyData = ProcessorUtils.createSelectedKeysHierarchy(outputKeysObjArr,outputFieldsList);
    stateObj.outputStreamFields=[];
    _.map(keyData,(o) => {
      stateObj.outputStreamFields = _.concat(stateObj.outputStreamFields, o.fields);
    });

    this.disableSelectedOptionFields(outputKeysObjArr, outputFieldsList);

    ProcessorUtils.addChildren(outputKeysObjArr, outputFieldsList);

    this.streamData = {
      streamId: this.rtJoinProcessorNode.outputStreams[0].streamId,
      fields: outputKeysObjArr
    };

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(outputKeysObjArr);
    stateObj.outputGroupByDotKeys = ProcessorUtils.modifyGroupKeyByDots(gKeys);
    stateObj.showLoading = false;

    this.setState(stateObj);
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  generateStreamFields = (keyData) => {
    let tempFieldsArr=[];
    _.map(keyData,(o) => {
      tempFieldsArr = _.concat(tempFieldsArr, o.fields ? o.fields : o);
    });
    return tempFieldsArr;
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

  getFilteredStream = (obj) => {
    const {inputStreamsArr} = this.state;
    return _.filter(inputStreamsArr, (stream) => {return stream.streamId !== obj.streamId;});
  }

  getSelectedStream = (obj) => {
    const {inputStreamsArr} = this.state;
    return _.filter(inputStreamsArr, (stream) => {return stream.streamId === obj.streamId;});
  }

  handleCommonStreamChange = (keyType,p_index,obj) => {
    if(!_.isEmpty(obj)){
      const {inputStreamsArr} = this.state;
      let tempGroup = _.cloneDeep(this.state.joinStreamGroup);
      let fromStream = _.cloneDeep(this.state.rtJoinStreamObj);
      let tempArr = _.cloneDeep(this.state.rtJoinStreamArr), secondStream=[];
      let stream = this.getSelectedStream(obj);
      let streamOptions = ProcessorUtils.getSchemaFields(obj.fields, 0, false);
      if(inputStreamsArr.length > 2){
        // code for multiple stream support
      } else {
        secondStream = this.getFilteredStream(stream[0]);
        if(keyType === "mainStream"){
          fromStream = stream[0];
          tempArr[0].rtJoinTypeStreamObj = secondStream[0];
          this.validateField('rtJoinStreamObj', fromStream);
          this.validateField('joinStreams'+0+'joinStream', fromStream);
        } else {
          fromStream = secondStream[0];
          tempArr[p_index].rtJoinTypeStreamObj = stream[0];
          this.validateField('joinStreams'+p_index+'joinStream', fromStream);
        }
        const tempCon =[],group=[];
        _.map(tempArr[0].conditions, (con) => {
          const confirstKey = con.firstKey;
          const conSecondKey = con.secondKey;
          // const {f_key,s_key} = this.createGroupFields(conSecondKey , confirstKey);
          group.push({firstKey : conSecondKey.split(':').pop() , secondKey : confirstKey.split(':').pop()});
          tempCon.push({
            firstKey : conSecondKey,
            secondKey : confirstKey,
            firstKeyStream: fromStream,
            secondKeyStream: tempArr[0].rtJoinTypeStreamObj
            // firstKeyOptions : ProcessorUtils.getSchemaFields(fromStream.fields,0,false),
            // secondKeyOptions: ProcessorUtils.getSchemaFields(tempArr[0].rtJoinTypeStreamObj.fields,0,false)
          });
        });
        tempGroup[0].conditions = group;
        tempArr[0].conditions = tempCon;
      }

      this.setState({
        rtJoinStreamObj : fromStream,
        rtJoinStreamArr : tempArr,
        joinStreamGroup : tempGroup
      });
    }
  }

  commonHandlerChange = (keyType,type,p_index,obj) => {
    if(! _.isEmpty(obj)){
      let tempArr = _.cloneDeep(this.state.rtJoinStreamArr);
      if(keyType === 'bufferType' && type === 'form') {
        this.setState({bufferType : obj.value}, () => {
          this.validateField('bufferType');
        });
      }else {
        keyType === 'rtJoinTypes'
        ? tempArr[p_index].rtJoinTypeSelected = obj.value
        : tempArr[p_index].bufferType = obj.value;
        this.setState({rtJoinStreamArr : tempArr }, () => {
          this.validateField('joinStreams'+p_index+keyType, obj.value);
        });
      }
    }
  }

  countInputChange = (type,p_index,event) => {
    let tempArr = _.cloneDeep(this.state.rtJoinStreamArr);
    const val = parseFloat(event.target.value) || '';
    if(type === 'form' && p_index === null){
      this.setState({countVal : val/*,showInputError : val > 0 ? false : true*/}, () => {
        this.validateField('countVal');
      });
    } else {
      tempArr[p_index].bufferSize = val;
      // tempArr[p_index].showInputError = val > 0 ? false : true;
      this.setState({rtJoinStreamArr : tempArr },() => {
        this.validateField('joinStreams'+p_index+'bufferSize', val);
      });
    }

  }

  handleConditionFieldChange = (keyString,p_index,index,obj) => {
    let joinStreamGroup = _.cloneDeep(this.state.joinStreamGroup);
    let tempArr = _.cloneDeep(this.state.rtJoinStreamArr);
    tempArr[p_index].conditions[index][keyString] = obj;
    /*const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey([obj]);
    const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys,'removeParent');*/
    joinStreamGroup[p_index].conditions[index][keyString] = obj.uniqueID.split(':').pop();
    this.setState({rtJoinStreamArr : tempArr,joinStreamGroup : joinStreamGroup}, () => {
      this.validateField('joinStreams'+p_index+''+index+keyString, obj.uniqueID);
    });
  }

  addRtJoinEqualFields = (p_index) => {
    if (this.state.editMode) {
      const el = document.querySelector('.processor-modal-form ');
      const targetHt = el.scrollHeight;
      Utils.scrollMe(el, (targetHt + 100), 2000);

      const {rtJoinStreamObj} = this.state;
      let joinStreamGroup = _.cloneDeep(this.state.joinStreamGroup);
      let tempArr = _.cloneDeep(this.state.rtJoinStreamArr);
      tempArr[p_index].conditions.push({
        firstKey : '',
        secondKey : '',
        firstKeyStream: rtJoinStreamObj,
        secondKeyStream: tempArr[p_index].rtJoinTypeStreamObj
        // firstKeyOptions : ProcessorUtils.getSchemaFields(rtJoinStreamObj.fields, 0, false),
        // secondKeyOptions : ProcessorUtils.getSchemaFields(tempArr[p_index].rtJoinTypeStreamObj.fields, 0, false)
      });
      joinStreamGroup[p_index].conditions.push({firstKey: '', secondKey: ''});
      this.setState({rtJoinStreamArr : tempArr,joinStreamGroup : joinStreamGroup});
    }
  }

  deleteRtJoinEqualFields = (p_index,index) => {
    let joinStreamGroup = _.cloneDeep(this.state.joinStreamGroup);
    let tempArr = _.cloneDeep(this.state.rtJoinStreamArr);
    tempArr[p_index].conditions.splice(index,1);
    joinStreamGroup[p_index].conditions.splice(index,1);
    this.setState({rtJoinStreamArr : tempArr,joinStreamGroup : joinStreamGroup}, () => {
      this.state.rtJoinStreamArr.map((js,i) => {
        js.conditions.map((c,j) => {
          this.validateField('joinStreams'+i+''+j+'firstKey', c.firstKey);
          this.validateField('joinStreams'+i+''+j+'secondKey', c.secondKey);
        });
      });
    });
  }

  handleFieldsChange = (arr) => {
    if(arr.length > 0){
      /*// splice the last index of arr as it's a new object selected
      const last_IdData = arr.splice(arr.length - 1 ,1);
      // check the object left in the arr are the same which we splice from it
      // if yes then replace the object with which we have splice earlier
      const checkIndex = _.findIndex(arr , {name : last_IdData[0].name});
      (checkIndex !== -1) ? arr[checkIndex] = last_IdData[0] : arr = _.concat(arr , last_IdData[0]);*/
      this.setOutputFields(arr);
    } else {
      this.streamData.fields = [];
      this.setState({outputKeysObjArr : arr, outputKeys: [], outputStreamFields: [],outputGroupByDotKeys : []}, () => {
        this.validateField('outputKeysObjArr');
      });
      this.disableSelectedOptionFields(arr,this.state.outputFieldsList);
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
    }
  }

  setOutputFields = (arr) => {
    let {outputFieldsList} = this.state;

    arr = JSON.parse(JSON.stringify(arr));
    // ProcessorUtils.removeChildren(arr);
    this.disableSelectedOptionFields(arr,outputFieldsList);
    ProcessorUtils.addChildren(arr, outputFieldsList);

    const keyData = ProcessorUtils.createSelectedKeysHierarchy(arr,outputFieldsList);
    /*let tempFieldsArr=[];
    _.map(keyData,(o) => {
      tempFieldsArr = _.concat(tempFieldsArr, o.fields);
    });
    this.streamData.fields = tempFieldsArr;*/
    // this.streamData.fields = this.generateStreamFields(keyData);
    this.streamData.fields = arr;

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
    const groupKeysByDots = ProcessorUtils.modifyGroupKeyByDots(gKeys);
    this.setState({outputKeysObjArr : arr, outputKeys: keys, outputStreamFields: keyData,outputGroupByDotKeys : groupKeysByDots}, () => {
      this.validateField('outputKeysObjArr');
    });
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  disableSelectedOptionFields = (arr,outputFieldsList) => {
    _.map(outputFieldsList, (output) => {
      if(arr.length){
        const index = _.findIndex(arr,(a) => a.keyPath === output.keyPath && a.name === output.name);
        if(index !== -1){
          output.disabled = true;
        } else {
          output.keyPath !== '' ? output.disabled = false : output.disabled = true;
        }
      } else {
        output.keyPath !== '' ? output.disabled = false : output.disabled = true;
      }
    });
  }

  aliasValidator = () => {
    const {outputKeysObjArr} = this.state;
    let  arr = _.uniqBy(outputKeysObjArr, 'alias');
    return arr.length;
  }


  handleSelectAllOutputFields = () => {
    /*let tempFields = _.cloneDeep(this.state.outputFieldsList);
    const allOutPutFields = ProcessorUtils.selectAllOutputFields(tempFields, 'joinForm');*/
    const {inputStreamsArr,outputFieldsList} = this.state;
    let tempFields = _.cloneDeep(outputFieldsList);
    const allOutPutFields = _.filter(tempFields, (t) => {
      return _.findIndex(inputStreamsArr,(input) => {return input.streamId === t.name;}) === -1;
    });
    this.setOutputFields(allOutPutFields);
  }

  checkBoxChange = (type,p_index,event) => {
    let tempArr = _.cloneDeep(this.state.rtJoinStreamArr);
    if(type === 'from' && p_index === null){
      this.setState({unique : event.target.checked});
    } else {
      tempArr[p_index].unique = event.target.checked;
      this.setState({rtJoinStreamArr : tempArr});
    }
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
      } else if (typeof value == 'number' && value !== '' && value <= 0) {
        this.state.validationErrors[fieldName] = "Size should be greater than '0'";
        validate = false;
      } else {
        this.state.validationErrors[fieldName] = "";
        validate = true;
      }
    }
    this.forceUpdate();
    return validate;
  }

  validateData = () => {
    const {outputKeys, rtJoinStreamObj,countVal,outputKeysObjArr,unique,rtJoinStreamArr,bufferType} = this.state;
    let validData = true, validateArr = [];

    /*const checkNestedValidation = function(rtJoinStreamArr){
      _.map(rtJoinStreamArr, (fields) => {
        if(fields.rtJoinTypeSelected === '' || fields.rtJoinTypeStreamObj === '' ||
            fields.bufferType === '' ||  fields.bufferSize === '' ||
            fields.firstKey === '' || fields.secondKey === '' || fields.showInputError){
          validateArr.push(false);
        }
        if(fields.conditions){
          checkNestedValidation(fields.conditions);
        }
      });
    };
    checkNestedValidation(rtJoinStreamArr);

    if(countVal > 0 && validateArr.length === 0 && (outputKeysObjArr.length !== 0 && outputKeys.length === this.aliasValidator()) && bufferType !== ''){
      validation = true;
    }*/

    if(!this.validateField('rtJoinStreamObj')){
      validData = false;
    }
    if(!this.validateField('bufferType')){
      validData = false;
    }
    if(!this.validateField('countVal')){
      validData = false;
    }

    rtJoinStreamArr.map((js,i) => {
      if(!this.validateField('joinStreams'+i+'rtJoinTypes', js.rtJoinTypeSelected)){
        validData = false;
      }
      if(!this.validateField('joinStreams'+i+'joinStream', js.rtJoinTypeStreamObj)){
        validData = false;
      }
      if(!this.validateField('joinStreams'+i+'bufferType', js.bufferType)){
        validData = false;
      }
      if(!this.validateField('joinStreams'+i+'bufferSize', js.bufferSize)){
        validData = false;
      }
      js.conditions.map((c,j) => {
        if(!this.validateField('joinStreams'+i+''+j+'firstKey', c.firstKey)){
          validData = false;
        }
        if(!this.validateField('joinStreams'+i+''+j+'secondKey', c.secondKey)){
          validData = false;
        }
      });
    });

    if(!this.validateField('outputKeysObjArr')){
      validData = false;
    }

    return validData;
  }

  updateEdgesForSelectedStream = () => {
    const {currentEdges,topologyId, versionId} = this.props;
    const {inputStreamsArr,joinStreamGroup} = this.state;
    const streamObj = inputStreamsArr.find((s) => {
      return s.streamId === this.rtJoinProcessorNode.config.properties.from.stream;
    });

    const edgeObj = currentEdges.find((e) => {
      return streamObj.id === e.streamGrouping.streamId;
    });

    // joinStreamGroup is supporting for single streams
    const streamFields = _.map(joinStreamGroup[0].conditions, 'firstKey');
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
    const {inputStreamsArr,joinStreamGroup} = this.state;
    const {currentEdges,topologyId, versionId} = this.props;
    let edgeDataArr = [],edgeIdArr = [];
    this.rtJoinProcessorNode.config.properties.joins.map((obj,i) => {
      const streamObj = inputStreamsArr.find((s) => {
        return s.streamId === obj.stream;
      });
      const edgeObj = currentEdges.find((e) => {
        return streamObj.id === e.streamGrouping.streamId;
      });
      const joinTypeFields = _.map(joinStreamGroup[i].conditions, 'secondKey');
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
      edgeIdArr.push(edgeObj.edgeId);
      edgeDataArr.push(edgeData);
    });
    return {edgeIdArr,edgeDataArr};
  }

  handleSave = (name, description) => {
    let {topologyId, versionId, nodeType, currentEdges} = this.props;
    let {
      rtJoinStreamObj,
      countVal,
      outputKeys,
      outputKeysObjArr,
      bufferType,
      parallelism,
      outputStreamFields,
      inputStreamsArr,
      fieldList,
      outputGroupByDotKeys,
      rtJoinStreamArr,
      joinStreamGroup,
      unique
    } = this.state;

    // const countData =  bufferType === "MILLISECONDS" ? countVal : Utils.numberToMilliseconds(countVal, Utils.capitaliseFirstLetter(bufferType.toLowerCase()));
    const countData = countVal;

    // outputStreams data is formated for the server
    const streamFields  = ProcessorUtils.generateOutputStreamsArr(this.streamData.fields,0,'alias');

    if(this.rtJoinProcessorNode.outputStreams.length > 0){
      this.rtJoinProcessorNode.outputStreams[0].fields = streamFields;
    } else {
      this.rtJoinProcessorNode.outputStreams.push({fields: streamFields, streamId: this.streamData.streamId});
    }

    const tempGroupData = _.cloneDeep(outputGroupByDotKeys);
    const tempStreamArr = _.cloneDeep(inputStreamsArr);
    const modifyGroup = ProcessorUtils.modifyGroupArrKeys(tempGroupData,tempStreamArr);

    let finalOutputKeys = modifyGroup.map((keyName, i)=>{
      if(keyName.search(':') === -1){
        return keyName +' as ' + outputKeysObjArr[i].alias;
      } else {
        let fieldName = keyName.split(':')[1];
        return keyName +' as ' + outputKeysObjArr[i].alias;
      }
    });

    // create an array from object value for server
    const conditionArr = _.map(joinStreamGroup , (streamGroup,k) => {
      return _.map(streamGroup.conditions, (eq) => {
        let data = [];
        data.push("equal");
        data.push(rtJoinStreamObj.streamId+':'+eq.firstKey);
        data.push(rtJoinStreamArr[k].rtJoinTypeStreamObj.streamId+':'+eq.secondKey);
        return data;
      });
    });

    let joinObj = [];
    _.map(rtJoinStreamArr, (joinStream, i) => {
      joinObj.push({
        type : joinStream.rtJoinTypeSelected,
        stream : joinStream.rtJoinTypeStreamObj.streamId,
        unique : joinStream.unique,
        conditions : conditionArr[i]
      });
      // const bVal = joinStream.bufferType === "MILLISECONDS" ? joinStream.bufferSize  : Utils.numberToMilliseconds(joinStream.bufferSize, Utils.capitaliseFirstLetter(joinStream.bufferType.toLowerCase()));
      const bVal = joinStream.bufferSize;
      joinObj[i][joinStream.bufferType.toLowerCase()] = bVal;
    });

    let configObj = {
      from :  {"stream": rtJoinStreamObj.streamId ,"unique" : unique},
      joins : joinObj,
      outputKeys : finalOutputKeys,
      outputStream : this.streamData.streamId
    };
    configObj.from[bufferType.toLowerCase()] = countData;

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
    const {edgeIdArr,edgeDataArr} = this.updateEdgesForJoinTypeObject();
    _.map(edgeDataArr, (edgeObj,index) => {
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'edges', edgeIdArr[index], {body: JSON.stringify(edgeObj)}));
    });

    return Promise.all(promiseArr);
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
      unique,
      rtJoinStreamArr,
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
              <div className="form-group row no-margin">
                <div className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Name of input stream</Popover>}>
                    <label>Select Stream
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div  className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window Size Type</Popover>}>
                    <label>Window Size Type
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div  className="col-sm-3">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window Size</Popover>}>
                    <label>Window Size
                      <span className="text-danger">*</span>
                    </label>
                  </OverlayTrigger>
                </div>
                <div  className="col-sm-3 text-center">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Drop Duplicates</Popover>}>
                    <label style={{marginBottom : 0, marginTop : "3px"}}>unique
                    </label>
                  </OverlayTrigger>
                </div>
              </div>
              <div className="form-group row">
                <div className="col-sm-3">
                  <Select value={rtJoinStreamObj} options={inputStreamsArr} className={!!validationErrors.rtJoinStreamObj ? 'invalidSelect' : ''} onChange={this.handleCommonStreamChange.bind(this,'mainStream',null)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false} valueKey="streamId" labelKey="streamId"/>
                  <p className="text-danger">{validationErrors.rtJoinStreamObj}</p>
                </div>
                <div className="col-sm-3">
                  <Select value={bufferType} options={bufferTypeArr} className={!!validationErrors.bufferType ? 'invalidSelect' : ''} onChange={this.commonHandlerChange.bind(this,'bufferType','form',null)} required={true} disabled={disabledFields} clearable={false} backspaceRemoves={false}/>
                  <p className="text-danger">{validationErrors.bufferType}</p>
                </div>
                <div className="col-sm-3">
                  <input type="number" className={`form-control ${!!validationErrors.countVal ? 'invalidInput' : ''}`} value={countVal} min={1} max={Number.MAX_SAFE_INTEGER}  onChange={this.countInputChange.bind(this,'form',null)} />
                  <p className="text-danger">{validationErrors.countVal}</p>
                </div>
                <div className="col-sm-3 text-center">
                    <Checkbox inline checked={unique} onChange={this.checkBoxChange.bind(this,'from',null)}></Checkbox>
                </div>
              </div>
              {
                _.map(rtJoinStreamArr,(rtJoinStream,i) => {
                  return <RealTimeJoinStreamComponent key={i+'join'} validationErrors={validationErrors} rtJoinStream={rtJoinStream} disabledFields={disabledFields} rtJoinTypes={rtJoinTypes} inputStreamsArr={inputStreamsArr} bufferTypeArr={bufferTypeArr} pIndex={i} commonHandlerChange={this.commonHandlerChange} handleCommonStreamChange={this.handleCommonStreamChange} countInputChange={this.countInputChange}  addRtJoinEqualFields={this.addRtJoinEqualFields} deleteRtJoinEqualFields={this.deleteRtJoinEqualFields} editMode={editMode} handleConditionFieldChange={this.handleConditionFieldChange} checkBoxChange={this.checkBoxChange}/>;
                })
              }
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
                    <Select className={"menu-outer-top "+(!!validationErrors.outputKeysObjArr ? 'invalidSelect' : '')} value={outputKeysObjArr} options={outputFieldsList} onChange={this.handleFieldsChange.bind(this)} multi={true} required={true} disabled={disabledFields} valueKey="uniqueID" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}
                      valueRenderer={this.renderValueComponent.bind(this)}
                      removeSelected={false}
                    />
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

RealTimeJoinNodeProcessor.contextTypes = {
  ParentForm: PropTypes.object
};
