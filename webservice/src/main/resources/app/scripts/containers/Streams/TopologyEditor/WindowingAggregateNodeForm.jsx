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
import {Tabs, Tab, OverlayTrigger, Popover} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import AggregateUdfREST from '../../../rest/AggregateUdfREST';
import Utils from '../../../utils/Utils';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils  from '../../../utils/ProcessorUtils';

export default class WindowingAggregateNodeForm extends Component {
  static propTypes = {
    nodeData: PropTypes.object.isRequired,
    editMode: PropTypes.bool.isRequired,
    nodeType: PropTypes.string.isRequired,
    topologyId: PropTypes.string.isRequired,
    versionId: PropTypes.number.isRequired,
    sourceNode: PropTypes.object.isRequired,
    targetNodes: PropTypes.array.isRequired,
    linkShuffleOptions: PropTypes.array.isRequired,
    currentEdges: PropTypes.array.isRequired
  };

  constructor(props) {
    super(props);
    this.fetchDataAgain = false;
    this.fieldsArr = [];
    this.streamIdList = [];
    this.tempStreamContextData = {};
    let {editMode} = props;
    var obj = {
      parallelism: 1,
      editMode: editMode,
      selectedKeys: [],
      windowSelectedKeys : [],
      _groupByKeys: [],
      keysList: [],
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
      tsField: '',
      lagMs: '',
      outputFieldsArr: [
        {
          args: '',
          functionName: '',
          outputFieldName: ''
        }
      ],
      functionListArr: [],
      outputStreamFields: [],
      argumentError: false,
      outputFieldsGroupKeys :[],
      showLoading : true
    };
    this.state = obj;
    this.fetchData();
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
    fetchData Method is call once on constructor.
    1] getAllUdfs API is call
    And only typeOf "AGGREGATE" are been fetch from the udfList and SET to fieldList

    If this.context.ParentForm.state.inputStreamOptions is present
    we call this.getDataFromParentFormContext Method for further process.
  */
  fetchData(){
    AggregateUdfREST.getAllUdfs().then((udfResult) => {
      if(udfResult.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.responseMessage}/>, '', toastOpt);
      } else {
        //Gather all "AGGREGATE" functions only
        this.udfList = ProcessorUtils.populateFieldsArr(udfResult.entities , "AGGREGATE");
        if(this.context.ParentForm.state.inputStreamOptions.length){
          this.getDataFromParentFormContext();
        }
      }
    });
  }

  /*
    getDataFromParentFormContext is called from two FUNCTION[fetchData,componentWillUpdate]
    Depend upon the condition

    Get the windowsNode from the this.context.ParentForm.state.processorNode
    Get the stream from the this.context.ParentForm.state.inputStreamOptions
    And if windowsNode has the rules Id
    we call this.populateOutputStreamsFromServer with rules ID to pre fill the value on UI
    OR
    we create a dummy ruleNode for the particular windowsNode and update the processor
  */
  getDataFromParentFormContext(){
    let {
      topologyId,
      versionId,
      nodeType,
      nodeData,
      currentEdges,
      targetNodes
    } = this.props;
    this.fetchDataAgain = true;

    // get the ProcessorNode from parentForm Context
    this.windowsNode = this.context.ParentForm.state.processorNode;
    this.configFields = this.windowsNode.config.properties;
    this.windowsRuleId = this.configFields.rules;

    // get the inputStream from parentForm Context
    const inputStreamFromContext = this.context.ParentForm.state.inputStreamOptions;
    let fields = [];
    inputStreamFromContext.map((result, i) => {
      this.streamIdList.push(result.streamId);
      fields.push(...result.fields);
    });
    this.fieldsArr = ProcessorUtils.getSchemaFields(_.unionBy(fields,'name'), 0,false);
    let tsFieldOptions =  this.fieldsArr.filter((f)=>{return f.type === 'LONG';});
    // tsFieldOptions should have a default options of processingTime
    tsFieldOptions.push({name: "processingTime", value: "processingTime"});

    let stateObj = {
      parallelism: this.configFields.parallelism || 1,
      keysList: JSON.parse(JSON.stringify(this.fieldsArr)),
      functionListArr: this.udfList,
      tsFieldOptions: tsFieldOptions
    };

    if(this.windowsRuleId){
      this.fetchRulesNode(this.windowsRuleId).then((ruleNode) => {
        this.windowRulesNode = ruleNode;
        this.populateOutputStreamsFromServer(this.windowRulesNode);
      });
    } else {
      //Creating window object so output streams can get it
      let dummyWindowObj = {
        name: 'window_auto_generated',
        description: 'window description auto generated',
        projections: [],
        streams: [],
        actions: [],
        groupbykeys: [],
        outputStreams: []
      };
      TopologyREST.createNode(topologyId, versionId, 'windows', {body: JSON.stringify(dummyWindowObj)}).then((windowRuleResult) => {
        this.windowRulesNode = windowRuleResult;
        this.windowsRuleId = windowRuleResult.id;
        this.windowsNode.config.properties.rules = [this.windowsRuleId];
        TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {
          body: JSON.stringify(this.windowsNode)
        });
        this.setState({showLoading : false});
      });
    }
    this.setState(stateObj);
  }

  /*
    populateOutputStreamsFromServer Method accept the Object send from the getDataFromParentFormContext
    When the window Processor has been already configured
    And we set all the defaultvalue, which we got from there serverWindowObj

    This include Nested fields spliting and populating the pre value for each and every fields on UI
    And SET in state object
  */
  populateOutputStreamsFromServer(serverWindowObj){
    if(serverWindowObj.projections.length > 0){
      const {keysList} = this.state;
      let argsGroupKeys=[];
      const windowProjectionData = ProcessorUtils.normalizationProjectionKeys(serverWindowObj.projections,keysList);
      const {keyArrObj,argsFieldsArrObj} = windowProjectionData;

      // populate argumentFieldGroupKey
      _.map(argsFieldsArrObj, (obj, index) => {
        if(_.isArray(obj.args)){
          let _arr = [],argsVal = obj.args[0];
          const fieldObj = ProcessorUtils.getKeyList(argsVal,keysList);
          _arr.push(fieldObj);
          const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(_arr);
          argsGroupKeys[index] = gKeys;

          // convert Array to String for display on UI
          obj.args = argsVal;
        }
      });

      const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(keyArrObj);
      const keyData = ProcessorUtils.createSelectedKeysHierarchy(keyArrObj,keysList);
      const outputFieldsObj =  this.generateOutputFields(argsFieldsArrObj,0);

      const tempFields = _.concat(keyData,argsFieldsArrObj);
      let mainStreamObj = {
        streamId : serverWindowObj.streams[0],
        fields : this.generateOutputFields(tempFields,0)
      };

      // stateObj is define and assign some values
      let stateObj = {
        showLoading:false ,
        outputFieldsArr :argsFieldsArrObj,
        outputStreamFields: outputFieldsObj,
        selectedKeys:keys,
        windowSelectedKeys:keyData,
        _groupByKeys : gKeys,
        outputFieldsGroupKeys :argsGroupKeys
      };

      // pre filling serverWindowObj.window values
      if (serverWindowObj.window.windowLength.class === '.Window$Duration') {
        stateObj.intervalType = '.Window$Duration';
        let obj = Utils.millisecondsToNumber(serverWindowObj.window.windowLength.durationMs);
        stateObj.windowNum = obj.number;
        stateObj.durationType = obj.type;
        if (serverWindowObj.window.slidingInterval) {
          let obj = Utils.millisecondsToNumber(serverWindowObj.window.slidingInterval.durationMs);
          stateObj.slidingNum = obj.number;
          stateObj.slidingDurationType = obj.type;
        }
      } else if (serverWindowObj.window.windowLength.class === '.Window$Count') {
        stateObj.intervalType = '.Window$Count';
        stateObj.windowNum = serverWindowObj.window.windowLength.count;
        if (serverWindowObj.window.slidingInterval) {
          stateObj.slidingNum = serverWindowObj.window.slidingInterval.count;
        }
      }
      if(serverWindowObj.window.tsField) {
        stateObj.tsField = serverWindowObj.window.tsField;
        stateObj.lagMs = Utils.millisecondsToNumber(serverWindowObj.window.lagMs).number;
      }

      // assign mainStreamObj value to "this.tempStreamContextData" make available for further methods
      this.tempStreamContextData = mainStreamObj;
      this.setState(stateObj);
      this.context.ParentForm.setState({outputStreamObj: mainStreamObj});
    } else {
      this.setState({showLoading:false});
    }
  }

  /*
    fetchRulesNode Method accept the ruleId
    To get the Rules node through API call
  */
  fetchRulesNode(ruleId){
    const {
      topologyId,
      versionId
    } = this.props;
    return TopologyREST.getNode(topologyId, versionId, 'windows', ruleId);
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
    validateData check the validation of
     selectedKeys, windowNum, argumentError and outputFieldsArr array
  */
  validateData(){
    let {selectedKeys, windowNum, outputFieldsArr, tsField, lagMs, argumentError} = this.state;
    let validData = true;
    if (argumentError) {return false;}
    if (selectedKeys.length === 0 || windowNum === '') {
      validData = false;
    }
    if(tsField !== '' && lagMs === '') {
      validData = false;
    }
    outputFieldsArr.map((obj) => {
      if (obj.args === '' || obj.outputFieldName === '') {
        validData = false;
      }
    });
    return validData;
  }

  /*
    updateProcessorNode Method accept name,description send by handleSave Method
    windowSelectedKeys AND outputStreamFields has been  concat array for outputStreams
    tempOutputFields is the result of the above concat array
    this.generateOutputFields call on tempOutputFields and the result has been added to
    this.windowsNode.outputStreams
    And the windowsNode is updated
  */
  updateProcessorNode(name, description){
    const {outputStreamFields,windowSelectedKeys,parallelism} = this.state;
    const {topologyId, versionId,nodeType,nodeData} = this.props;
    const tempOutputFields = _.concat(windowSelectedKeys,outputStreamFields);
    const streamFields = this.generateOutputFields(tempOutputFields, 0);
    if(this.windowsNode.outputStreams.length > 0){
      this.windowsNode.outputStreams.map((s) => {
        s.fields = streamFields;
      });
    }else{
      _.map(this.outputStreamStringArr , (s) => {
        this.windowsNode.outputStreams.push({
          streamId: s,
          fields: streamFields
        });
      });
    }
    this.windowsNode.config.properties.parallelism = parallelism;
    this.windowsNode.description = description;
    return this.windowsNode;
  }

  /*
    updateEdges Method update the edge
    using inputStreamsArr id to filter the currentEdges.streamGrouping.streamId for the particular nodeType
    And update with fields selected as a outputStreams
  */
  updateEdges(){
    const {currentEdges} = this.props;
    const {inputStreamOptions} = this.context.ParentForm.state;

    const fields = this.windowRulesNode.groupbykeys.map((field) => {
      return field.replace(/\[\'/g, ".").replace(/\'\]/g, "");
    });
    const edgeObj = _.filter(currentEdges, (edge) => {
      return edge.streamGrouping.streamId === inputStreamOptions[0].id;
    });
    let edgeData = {
      fromId: edgeObj[0].source.nodeId,
      toId: edgeObj[0].target.nodeId,
      streamGroupings: [
        {
          streamId: edgeObj[0].streamGrouping.streamId,
          grouping: 'FIELDS',
          fields: fields
        }
      ]
    };
    const edgeId = edgeObj[0].edgeId;
    return {edgeId,edgeData};
  }

  /*
    handleSave Method is responsible for windowProcessor
    _groupByKeys is modify with {expr : fields} obj;
    outputFieldsGroupKeys is added to each and every tempArr[index].args
    Rules Node has been updated in this call

    updateProcessorNode Method is a callback
  */
  handleSave(name, description){
    if(this.windowsRuleId){
      let {
        _groupByKeys,
        selectedKeys,
        windowNum,
        slidingNum,
        durationType,
        slidingDurationType,
        intervalType,
        parallelism,
        outputFieldsGroupKeys,
        tsField,
        lagMs
      } = this.state;
      let tempArr = _.cloneDeep(this.state.outputFieldsArr);
      let {topologyId, versionId, nodeType, nodeData} = this.props;

      _.map(tempArr, (temp,index) => {
        tempArr[index].args = outputFieldsGroupKeys[index];
      });
      const exprObj = _groupByKeys.map((field) => {return {expr: field};});
      const mergeTempArr = _.concat(tempArr,exprObj);

      this.windowRulesNode.projections = mergeTempArr;
      this.outputStreamStringArr = [
        'window_transform_stream_'+this.windowsNode.id,
        'window_notifier_stream_'+this.windowsNode.id
      ];
      this.windowRulesNode.outputStreams = this.outputStreamStringArr;
      this.windowRulesNode.streams = [this.streamIdList[0]];
      this.windowRulesNode.groupbykeys = _groupByKeys;
      this.windowRulesNode.window = {
        windowLength: {
          class: intervalType
        }
      };

      //Syncing window object into data
      if (intervalType === '.Window$Duration') {
        this.windowRulesNode.window.windowLength.durationMs = Utils.numberToMilliseconds(windowNum, durationType);
        if (slidingNum !== '') {
          this.windowRulesNode.window.slidingInterval = {
            class: intervalType,
            durationMs: Utils.numberToMilliseconds(slidingNum, slidingDurationType)
          };
        }
      } else if (intervalType === '.Window$Count') {
        this.windowRulesNode.window.windowLength.count = windowNum;
        if (slidingNum !== '') {
          this.windowRulesNode.window.slidingInterval = {
            class: intervalType,
            count: slidingNum
          };
        }
      }
      if(tsField !== ''){
        this.windowRulesNode.window.tsField = tsField;
        this.windowRulesNode.window.lagMs = Utils.numberToMilliseconds(lagMs, 'Seconds');
      }
      let promiseArr = [];
      const windowsNodeObj = this.updateProcessorNode(name, description);
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, windowsNodeObj.id, {body: JSON.stringify(windowsNodeObj)}));

      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'windows', this.windowsRuleId, {body: JSON.stringify(this.windowRulesNode)}));

      const {edgeId , edgeData} = this.updateEdges();
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'edges', edgeId, {body: JSON.stringify(edgeData)}));

      return  Promise.all(promiseArr);
    }
  }

  /*
    generateOutputFields Method accept the array of object and level[NUMBER] for NESTED fields
    And it modify the fields into new Object with returnType
  */
  generateOutputFields(fields, level) {
    const {keysList} = this.state;
    return fields.map((field) => {
      let obj = {
        name: field.name || field.outputFieldName ,
        type: field.type || this.getReturnType(field.functionName, ProcessorUtils.getKeyList(field.args,keysList)),
        optional : false
      };

      if (field.type === 'NESTED' && field.fields) {
        obj.fields = this.generateOutputFields(field.fields, level + 1);
      }
      return obj;
    });
  }

  /*
    handleKeysChange Method accept arr of obj
    And SET
    selectedKeys : key of arr used on UI for listing
    _groupByKeys : group the selectedKeys
    windowSelectedKeys : store the obj of the selectedKeys
  */
  handleKeysChange(arr){
    let {keysList,outputStreamFields,windowSelectedKeys} = this.state;
    const keyData = ProcessorUtils.createSelectedKeysHierarchy(arr,keysList);
    this.tempStreamContextData.fields = outputStreamFields.length > 0  ? _.concat(keyData , outputStreamFields) : keyData;

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
    this.setState({selectedKeys: keys, _groupByKeys: gKeys, windowSelectedKeys: keyData});
    this.context.ParentForm.setState({outputStreamObj: this.tempStreamContextData});
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
    handleTimestampFieldChange method handles change of timestamp field
    params@ obj selected option
  */
  handleTimestampFieldChange(obj) {
    if(obj){
      this.setState({tsField: obj.name});
    } else {
      this.setState({tsField: '', lagMs: ''});
    }
  }

  /*
    handleFieldChange Method accept name,index and obj
    params@ name = string 'args' Or 'functionName'
    params@ index = number
    params@ obj = selected obj
    it SET the outputFieldsArr[name] to obj.name
    And SET the outputFieldsArr[outputFieldName] by concating the two value with '_'

    And call setParentContextOutputStream FUNCTION with false.
  */
  handleFieldChange(name,index,obj){
    if(obj){
      let groupKey = _.cloneDeep(this.state.outputFieldsGroupKeys);
      let tempArr = _.cloneDeep(this.state.outputFieldsArr);
      tempArr[index][name] = obj.name;
      tempArr[index].outputFieldName = tempArr[index].args+"_"+this.getFunctionDisplayName(tempArr[index].functionName);
      if(name === "args"){
        const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey([obj]);
        groupKey[index] = gKeys;
      }
      this.setState({outputFieldsArr :tempArr,outputFieldsGroupKeys :groupKey}, () => {
        this.setParentContextOutputStream(index,false);
      });
    }
  }

  /*
    handleOutputFieldName Method accept index and event
    using the value in  event.target.value
    and SET to the outputFieldName of outputFieldsArr

    And call setParentContextOutputStream FUNCTION with true to override the value.
  */
  handleOutputFieldName(index,event){
    let tempArr = _.cloneDeep(this.state.outputFieldsArr);
    const outputName = event.target.value.trim();
    if(outputName !== ''){
      tempArr[index].outputFieldName = outputName;
      this.setState({outputFieldsArr : tempArr}, () => {
        this.setParentContextOutputStream(index,true);
      });
    }
  }

  /*
    getReturnType Method accept the params
    Param@ functionName
    Param@ fieldObj
    Param@ index

    And it check the returnType is support in the argument array of the fieldObj
    if argList is empty then it return fieldObj.type and  show Error on UI
    else 'DOUBLE' as default;
  */
  getReturnType(functionName, fieldObj, index) {
    let obj = this.state.functionListArr.find((o) => {
      return o.name === functionName;
    });
    if (obj) {
      if (obj.argTypes) {
        if (fieldObj) {
          let argList = obj.argTypes.toString().includes(fieldObj.type);
          (argList)
            ? this.setState({argumentError: false})
            : this.setState({argumentError: true});
        }
        return obj.returnType || fieldObj.type;
      }
    } else if (fieldObj) {
      return fieldObj.type;
    } else {
      return 'DOUBLE';
    }
  }

  /*
    getFunctionDisplayName Method accept the functionName
    And get the displayName from the functionListArr
  */
  getFunctionDisplayName(functionName){
    if(functionName === ""){
      return "";
    }
    const {functionListArr} = this.state;
    let obj = functionListArr.find((o) => {
      return o.name === functionName;
    });
    return obj.displayName;
  }

  /*
    This Mehods call from [handleOutputFieldName,handleFieldChange] FUNCTIONS
    setParentContextOutputStream Mehod accept index and outputFlag
    outputFlag = true then it will assign the value SET by handleOutputFieldName Function

    update the local state and parentContext also;
    And Two array is concat to make the outputStreamObj of parentContext
  */
  setParentContextOutputStream(index,outputFlag){
    let displayName = "";
    const {outputFieldsArr,windowSelectedKeys,functionListArr,keysList} = this.state;
    let mainObj = _.cloneDeep(this.state.outputStreamFields);
    if(outputFieldsArr[index].functionName !== ""){
      displayName = this.getFunctionDisplayName(outputFieldsArr[index].functionName);
    }
    mainObj[index] = {
      name: outputFlag ? outputFieldsArr[index].outputFieldName : (outputFieldsArr[index].args === "" && outputFieldsArr[index].functionName === "") ? "" : outputFieldsArr[index].args+'_'+displayName,
      type: (outputFieldsArr[index].args !== "" && outputFieldsArr[index].functionName !== "") ? this.getReturnType(outputFieldsArr[index].functionName, ProcessorUtils.getKeyList(outputFieldsArr[index].args,keysList),index) : '',
      optional : false
    };

    // create this.tempStreamContextData obj to save in ParentForm context
    const tempStreamData = _.concat(windowSelectedKeys,mainObj);
    this.tempStreamContextData = {fields : tempStreamData  , streamId : this.streamIdList[0]};
    this.setState({outputStreamFields : mainObj});
    this.context.ParentForm.setState({outputStreamObj: this.tempStreamContextData});
  }

  /*
    handleValueChange Method is handles to fields on UI
    windowNum and slidingNum input value
  */
  handleValueChange(e){
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
    addOutputFields Method add the row on UI with blank text
  */
  addOutputFields(){
    if (this.state.editMode) {
      const el = document.querySelector('.processor-modal-form ');
      const targetHt = el.scrollHeight;
      Utils.scrollMe(el, (targetHt + 100), 2000);

      let fieldsArr = this.state.outputFieldsArr;
      fieldsArr.push({args: '', functionName: '', outputFieldName: ''});
      this.setState({outputFieldsArr: fieldsArr});
    }
  }

  /*
    deleteFieldRow Method accept the index
    And delete to fields from the two Array [outputFieldsArr , outputStreamFields]
  */
  deleteFieldRow(index){
    const {windowSelectedKeys} = this.state;
    let fieldsArr = _.cloneDeep(this.state.outputFieldsArr);
    let mainOutputFields = _.cloneDeep(this.state.outputStreamFields);

    fieldsArr.splice(index,1);
    mainOutputFields.splice(index,1);

    const tempStreamData = _.concat(windowSelectedKeys,mainOutputFields);
    this.tempStreamContextData.fields = tempStreamData;
    this.setState({outputFieldsArr : fieldsArr,outputStreamFields : mainOutputFields});
    this.context.ParentForm.setState({outputStreamObj: this.tempStreamContextData});
  }

  render(){
    const {
      editMode,
      showLoading,
      keysList,
      selectedKeys,
      intervalType,
      intervalTypeArr,
      durationType,
      durationTypeArr,
      windowNum,
      slidingNum,
      tsField,
      tsFieldOptions,
      lagMs,
      slidingDurationType,
      argumentError,
      outputFieldsArr,
      functionListArr
    } = this.state;
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
              : <form className="customFormClass">
                <div className="form-group">
                  <label>Select Keys
                    <span className="text-danger">*</span>
                  </label>
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Group by keys</Popover>}>
                  <div>
                    <Select value={selectedKeys} options={keysList} onChange={this.handleKeysChange.bind(this)} multi={true} required={true} disabled={!editMode} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption}/>
                  </div>
                  </OverlayTrigger>
                </div>
                <div className="form-group">
                  <label>Window Interval Type
                    <span className="text-danger">*</span>
                  </label>
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window interval type</Popover>}>
                  <div>
                    <Select value={intervalType} options={intervalTypeArr} onChange={this.commonHandlerChange.bind(this,'intervalType')} required={true} disabled={!editMode} clearable={false}/>
                  </div>
                  </OverlayTrigger>
                </div>
                <div className="form-group">
                  <label>Window Interval
                    <span className="text-danger">*</span>
                  </label>
                  <div className="row">
                    <div className="col-sm-5">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Window interval duration</Popover>}>
                      <input name="windowNum" value={windowNum} onChange={this.handleValueChange.bind(this)} type="number" className="form-control" required={true} disabled={!editMode} min="0" inputMode="numeric"/>
                      </OverlayTrigger>
                    </div>
                    {intervalType === '.Window$Duration'
                      ? <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Duration type</Popover>}>
                        <div className="col-sm-5">
                          <Select value={durationType} options={durationTypeArr} onChange={this.commonHandlerChange.bind(this,'durationType')} required={true} disabled={!editMode} clearable={false}/>
                        </div>
                        </OverlayTrigger>
                      : null}
                  </div>
                </div>
                <div className="form-group">
                  <label>Sliding Interval</label>
                  <div className="row">
                    <div className="col-sm-5">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Sliding interval duration</Popover>}>
                      <input name="slidingNum" value={slidingNum} onChange={this.handleValueChange.bind(this)} type="number" className="form-control" required={true} disabled={!editMode} min="0" inputMode="numeric"/>
                      </OverlayTrigger>
                    </div>
                    {intervalType === '.Window$Duration'
                      ? <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Duration type</Popover>}>
                        <div className="col-sm-5">
                          <Select value={slidingDurationType} options={durationTypeArr} onChange={this.commonHandlerChange.bind(this,'slidingDurationType')} required={true} disabled={!editMode} clearable={false}/>
                        </div>
                        </OverlayTrigger>
                      : null}
                  </div>
                </div>
                <div className="form-group">
                  <div className="row">
                    <div className="col-sm-5">
                      <label>Timestamp Field</label>
                    </div>
                    {tsField !== '' ?
                    <div className="col-sm-5">
                      <label>Lag in Seconds<span className="text-danger">*</span></label>
                    </div>
                    : ''}
                  </div>
                  <div className="row">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Timestamp field name</Popover>}>
                    <div className="col-sm-5">
                      <Select value={tsField} options={tsFieldOptions} onChange={this.handleTimestampFieldChange.bind(this)} disabled={!editMode} valueKey="name" labelKey="name" />
                    </div>
                    </OverlayTrigger>
                    {tsField !== '' ?
                    <div className="col-sm-5">
                      <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Lag duration</Popover>}>
                      <input name="lagMs" value={lagMs} onChange={this.handleValueChange.bind(this)} type="number" className="form-control" required={true} disabled={!editMode} min="0" inputMode="numeric"/>
                      </OverlayTrigger>
                    </div>
                    : ''}
                  </div>
                </div>
                <fieldset className="fieldset-default">
                  <legend>Output Fields</legend>
                  {
                    argumentError
                      ? <label className="color-error"> The Aggregate Function is not supported by input </label>
                      :''
                  }
                  <div className="row">
                    <div className="col-sm-3 outputCaption">
                      <label>Input</label>
                    </div>
                    <div className="col-sm-3 outputCaption">
                      <label>Aggregate Function</label>
                    </div>
                    <div className="col-sm-3 outputCaption">
                      <label>Output</label>
                    </div>
                  </div>
                  {outputFieldsArr.map((obj, i) => {
                    return (
                      <div key={i} className="row form-group">
                        <OverlayTrigger trigger={['hover']} placement="bottom" overlay={<Popover id="popover-trigger-hover">Input field name</Popover>}>
                        <div className="col-sm-3">
                          <Select className={outputFieldsArr.length - 1 === i
                            ? "menu-outer-top"
                            : ''} value={obj.args} options={keysList} onChange={this.handleFieldChange.bind(this,'args', i)} required={true} disabled={!editMode} valueKey="name" labelKey="name" clearable={false} optionRenderer={this.renderFieldOption.bind(this)}/>
                        </div>
                        </OverlayTrigger>
                        <OverlayTrigger trigger={['hover']} placement="bottom" overlay={<Popover id="popover-trigger-hover">Function name</Popover>}>
                        <div className="col-sm-3">
                          <Select className={outputFieldsArr.length - 1 === i
                            ? "menu-outer-top"
                            : ''} value={obj.functionName} options={functionListArr} onChange={this.handleFieldChange.bind(this,'functionName', i)} required={true} disabled={!editMode} valueKey="name" labelKey="displayName"/>
                        </div>
                        </OverlayTrigger>
                        <div className="col-sm-3">
                          <OverlayTrigger trigger={['hover']} placement="bottom" overlay={<Popover id="popover-trigger-hover">Output field name</Popover>}>
                          <input name="outputFieldName" value={obj.outputFieldName} ref="outputFieldName" onChange={this.handleOutputFieldName.bind(this, i)} type="text" className="form-control" required={true} disabled={!editMode}/>
                          </OverlayTrigger>
                        </div>
                        {editMode
                          ? <div className="col-sm-2">
                              <button className="btn btn-default btn-sm" type="button" onClick={this.addOutputFields.bind(this)}>
                                <i className="fa fa-plus"></i>
                              </button>&nbsp; {i > 0
                                ? <button className="btn btn-sm btn-danger" type="button" onClick={this.deleteFieldRow.bind(this, i)}>
                                    <i className="fa fa-trash"></i>
                                  </button>
                                : null}
                            </div>
                          : null}
                      </div>
                    );
                  })}
                </fieldset>
              </form>
            }
        </Scrollbars>
      </div>
    );
  }
}

WindowingAggregateNodeForm.contextTypes = {
  ParentForm: React.PropTypes.object
};
