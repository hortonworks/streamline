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

import React, {
  Component
} from 'react';
import ReactDOM, {
  findDOMNode
} from 'react-dom';
import PropTypes from 'prop-types';
import {Select2 as Select} from '../../../utils/SelectUtils';
import {OverlayTrigger, Popover} from 'react-bootstrap';
import TopologyREST from '../../../rest/TopologyREST';
import AggregateUdfREST from '../../../rest/AggregateUdfREST';
import Utils from '../../../utils/Utils';
import FSReactToastr from '../../../components/FSReactToastr';
import {
  toastOpt
} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';
import {Scrollbars} from 'react-custom-scrollbars';
import ProcessorUtils  from '../../../utils/ProcessorUtils';
import CommonCodeMirror from '../../../components/CommonCodeMirror';
import {binaryOperators} from '../../../utils/Constants';
import WebWorkers  from '../../../utils/WebWorkers';

export default class ProjectionProcessorContainer extends Component {

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

  constructor(props) {
    super(props);
    this.fetchDataAgain = false;
    let {editMode} = props;
    this.fieldsArr = [];
    this.streamIdList = [];
    this.argumentErrorArr = [];
    this.tempStreamContextData = {};
    var obj = {
      editMode: editMode,
      fieldList: [],
      outputFieldsArr: [
        {
          conditions : '',
          outputFieldName: '',
          prefetchData : false
        }
      ],
      functionListArr: [],
      argumentError: false,
      outputStreamFields: [],
      invalidInput : false,
      projectionKeys : [],
      projectionSelectedKey : [],
      argumentKeysGroup : [],
      showLoading : true,
      errorString : '',
      scriptErrors : []
    };
    this.state = obj;
    this.fetchData();
    this.workersObj={};
    this.WebWorkers = {};
  }

  /*
    componentWillUpdate has been call very frequently in react ecosystem
    this.context.ParentForm.state has been SET through the API call in ProcessorNodeForm
    And we need to call fetchData after the Parent has set its state so that inputStreamOptions are available
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
    fetchData Method is call once on componentWillUpdate after the this.context.ParentForm SET its state.
    1] getAllUdfs API is call
    And only typeOf "FUNCTION" are been fetch from the udfList and SET to fieldList
    These variables are set from the ParentForm...
    this.projectionNode = this.context.ParentForm.state.processorNode.
    inputStreamFromContext = this.context.ParentForm.state.inputStreamOptions.

    If the rules ID is not present in this.projectionNode
    we create a dummy rules for the processor and set the rules Id
    And if rules ID is present
    we call this.populateOutputStreamsFromServer Method to update the UI with pre populate fields.
  */
  fetchData = () => {
    AggregateUdfREST.getAllUdfs().then((udfResult) => {
      if(udfResult.responseMessage !== undefined){
        FSReactToastr.error(
          <CommonNotification flag="error" content={results.responseMessage}/>, '', toastOpt);
      } else {
        //Gather all "FUNCTION" functions only
        this.udfList = ProcessorUtils.populateFieldsArr(udfResult.entities , "FUNCTION");
        if(this.context.ParentForm.state.inputStreamOptions.length){
          this.getDataFromParentFormContext();
        }
      }
    });
  }

  /*
    getDataFromParentFormContext is called from two FUNCTION[fetchData,componentWillUpdate]
    Depend upon the condition

    Get the projectionNode from the this.context.ParentForm.state.processorNode
    Get the stream from the this.context.ParentForm.state.inputStreamOptions
    And if projectionNode has the rules Id
    we call this.populateOutputStreamsFromServer with rules ID to pre fill the value on UI
    OR
    we create a dummy ruleNode for the particular projectionNode and update the processor
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
    this.projectionNode = this.context.ParentForm.state.processorNode;
    this.configFields = this.projectionNode.config.properties;
    this.projectionRuleId = this.configFields.rules;

    // get the inputStream from parentForm Context
    const inputStreamFromContext = this.context.ParentForm.state.inputStreamOptions;
    let fields = [];
    inputStreamFromContext.map((result, i) => {
      this.streamIdList.push(result.streamId);
      fields.push(...result.fields);
    });
    this.fieldsArr = ProcessorUtils.getSchemaFields(_.unionBy(fields,'name'), 0,false);
    this.fieldsHintArr = _.unionBy(fields,'name');
    let stateObj = {
      fieldList: JSON.parse(JSON.stringify(this.fieldsArr)),
      functionListArr: this.udfList,
      showLoading : false
    };
    this.populateCodeMirrorDefaultHintOptions();
    if(this.projectionRuleId){
      this.fetchRulesNode(this.projectionRuleId).then((ruleNode) => {
        this.projectionRulesNode = ruleNode;
        this.populateOutputStreamsFromServer(this.projectionRulesNode);
      });
    } else {
      //Creating projection object so output streams can get it
      let dummyProjectionObj = {
        name: 'projection_auto_generated',
        description: 'projection description auto generated',
        projections: [],
        streams: [this.streamIdList[0]],
        actions: [],
        outputStreams: []
      };
      TopologyREST.createNode(topologyId, versionId, "rules", {body: JSON.stringify(dummyProjectionObj)}).then((rulesNode) => {
        if(rulesNode.responseMessage !== undefined){
          stateObj.showLoading = true;
          FSReactToastr.error(
            <CommonNotification flag="error" content={rulesNode.responseMessage}/>, '', toastOpt);
        } else {
          this.projectionRulesNode = rulesNode;
          this.projectionRuleId = rulesNode.id;
          this.projectionNode.config.properties.rules = [this.projectionRuleId];
          TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {
            body: JSON.stringify(this.projectionNode)
          });
        }
      });
    }
    this.setState(stateObj, () => {
      this.WebWorkers = new WebWorkers(this.initValidatorWorker());
    });
  }

  populateCodeMirrorDefaultHintOptions(){
    const {udfList} = this;
    this.hintOptions=[];
    // FUNCTION from UDFLIST for hints...
    Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(udfList,"FUNCTION"));
  }

  pushAdditionalHints = (fields) => {;
    this.hintOptions=[];
    // arguments from field list for hints...
    Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(fields,"ARGS"));
    // FUNCTION from UDFLIST for hints...
    Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(this.udfList,"FUNCTION"));
  }

  /*
    populateOutputStreamsFromServer Method accept the Object send from the fetchData
    When the ProjectionProcessor has been already configured
    And we set all the defaultvalue, which we got from there serverStreamObj

    This include Nested fields spliting and populating the pre value for each and every fields on UI
    And SET in state object
  */
  populateOutputStreamsFromServer(serverStreamObj){
    if(serverStreamObj.projections.length > 0){
      const {fieldList} = this.state;
      let argsGroupKeys=[];

      // const projectionData = ProcessorUtils.normalizationProjectionKeys(serverStreamObj.projections,fieldList);
      // const {keyArrObj,argsFieldsArrObj} = projectionData;

      // populate argumentFieldGroupKey
      // _.map(argsFieldsArrObj, (obj, index) => {
      //   if(_.isArray(obj.args)){
      //     let _arr = [];
      //     _.map(obj.args , (o) => {
      //       const fieldObj = ProcessorUtils.getKeyList(o,fieldList);
      //       if(fieldObj){
      //         _arr.push(fieldObj);
      //       }
      //     });
      //     const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(_arr);
      //     argsGroupKeys[index] = gKeys;
      //   }
      // });

      const projectionData = this.getScriptConditionAndFieldsForServer(serverStreamObj.projections,fieldList);
      const {conditionsArr,fieldKeyArr} = projectionData;
      const {keyArrObj} = ProcessorUtils.normalizationProjectionKeys(fieldKeyArr,fieldList);

      const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(keyArrObj);
      const keyData = ProcessorUtils.createSelectedKeysHierarchy(keyArrObj,fieldList);

      // if(!argsFieldsArrObj.length){
      //   argsFieldsArrObj.push({
      //     conditions: '',
      //     outputFieldName: ''
      //   });
      // }

      const outputFieldsObj = [];
      _.map(conditionsArr, (cd) => {
        const obj = this.getReturnTypeFromCodemirror(cd.conditions,this.state.functionListArr,this.fieldsHintArr);
        outputFieldsObj.push({
          name : cd.outputFieldName,
          type : obj.returnType
        });
      });

      const tempFields = _.concat(keyData,outputFieldsObj);
      let mainStreamObj = {
        streamId : serverStreamObj.streams[0],
        fields : this.generateOutputFields(tempFields,0)
      };

      // assign mainStreamObj value to "this.tempStreamContextData" make available for further methods
      this.tempStreamContextData = mainStreamObj;
      this.context.ParentForm.setState({outputStreamObj: mainStreamObj});

      this.setState({outputStreamFields: outputFieldsObj,outputFieldsArr :conditionsArr,projectionKeys:keys,projectionSelectedKey:keyData,projectionGroupByKeys : gKeys,argumentKeysGroup :argsGroupKeys,showLoading : false}, () => {
        // const outputFieldsObj = [];
        // _.map(conditionsArr, (cd) => {
        //   const obj = this.getReturnTypeFromCodemirror(cd.conditions,this.state.functionListArr,this.fieldsHintArr);
        //   outputFieldsObj.push({
        //     name : cd.outputFieldName,
        //     type : obj.returnType
        //   });
        // });
        // //
        // this.setState({outputStreamFields: outputFieldsObj}, () => {
        //   const tempFields = _.concat(keyData,outputFieldsObj);
        //   let mainStreamObj = {
        //     streamId : serverStreamObj.streams[0],
        //     fields : this.generateOutputFields(tempFields,0)
        //   };
        //
        //   // assign mainStreamObj value to "this.tempStreamContextData" make available for further methods
        //   this.tempStreamContextData = mainStreamObj;
        //   this.context.ParentForm.setState({outputStreamObj: mainStreamObj});
        // });
      });
    } else {
      this.setState({showLoading : false});
    }
  }

  getScriptConditionAndFieldsForServer = (data,fieldList) => {
    let conditionsArr=[],fieldKeyArr=[];
    _.map(data, (d) => {
      if(d.expr.includes('As')){
        const obj = d.expr.split('As');
        conditionsArr.push({
          conditions : obj[0].trim(),
          outputFieldName : obj[1].trim(),
          prefetchData : true
        });
      } else {
        fieldKeyArr.push(d);
      }
    });
    return {conditionsArr,fieldKeyArr};
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
    return TopologyREST.getNode(topologyId, versionId, 'rules', ruleId);
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
     argumentError,projectionKeys and outputFieldsArr array
  */
  validateData(){
    let validData = [],promiseArr=[],flag= false;
    const {outputFieldsArr,argumentError,projectionKeys,errorString} = this.state;
    if(argumentError || projectionKeys.length === 0 || errorString.length){
      return false;
    }
    _.map(outputFieldsArr,(field,i) => {
      // push to worker promiseArr
      promiseArr.push(this.WebWorkers.startWorkers(field.conditions.trim()));

      if(!((field.conditions.length == 0 && field.outputFieldName.length == 0) || (field.conditions.length > 0 && field.outputFieldName.length > 0))){
        validData.push(field);
      }
    });

    return Promise.all(promiseArr).then((res) => {
      let arr=[];
      _.map(res, (r) => {
        arr.push(r.err);
      });
      if(validData.length === 0 && _.compact(arr).length === 0){
        arr=[];
        flag= true;
      }
      this.setState({scriptErrors : arr});
      return flag;
    });
  }

  /*
    generateOutputFields Method accept the array of object and level[NUMBER] for NESTED fields
    And it modify the fields into new Object with returnType
  */
  generateOutputFields(fields, level) {
    const {fieldList} = this.state;
    return fields.map((field, i) => {
      let obj = {
        name: field.name || field.outputFieldName ,
        type: field.type || this.getReturnType(field.functionName, ProcessorUtils.getKeyList(field.args[0],fieldList), i, fields)
      };

      if (field.type === 'NESTED' && field.fields) {
        obj.fields = this.generateOutputFields(field.fields, level + 1);
      }
      return obj;
    });
  }

  /*
    updateProcessorNode Method accept name,description send by handleSave Method
    projectionSelectedKey AND outputStreamFields has been  concat array for outputStreams
    tempOutputFields is the result of the above concat array
    this.generateOutputFields call on tempOutputFields and the result has been added to
    this.projectionNode.outputStreams
    And the processorNode is updated
  */
  updateProcessorNode(name, description){
    const {outputStreamFields,projectionSelectedKey} = this.state;
    const {topologyId, versionId,nodeType,nodeData} = this.props;
    const tempOutputFields = _.concat(projectionSelectedKey,outputStreamFields);
    const streamFields = this.generateOutputFields(tempOutputFields, 0);
    if(this.projectionNode.outputStreams.length > 0){
      this.projectionNode.outputStreams.map((s) => {
        s.fields = streamFields;
      });
    }else{
      _.map(this.outputStreamStringArr , (s) => {
        this.projectionNode.outputStreams.push({
          streamId: s,
          fields: streamFields
        });
      });
    }
    this.projectionNode.name = name;
    this.projectionNode.description = description;
    return this.projectionNode;
  }

  /*
    handleSave Method is responsible for ProjectionProcessor
    projectionGroupByKeys is modify with {expr : fields} obj;
    argumentKeysGroup is added to each and every tempArr[index].args
    Rules Node has been updated in this call

    updateProcessorNode Method is a callback
  */
  handleSave(name, description){
    if(this.projectionRuleId && !this.props.testRunActivated){
      const {projectionSelectedKey,argumentKeysGroup,projectionGroupByKeys,outputFieldsArr} = this.state;
      let tempArr = [];
      const {topologyId, versionId,nodeType,nodeData} = this.props;
      // _.map(tempArr, (temp,index) => {
      //   tempArr[index].args = argumentKeysGroup[index];
      // });
      // tempArr = tempArr.filter((f) => {
      //   return f.functionName.length && f.args.length && f.outputFieldName.length;
      // });
      _.map(outputFieldsArr, (field) => {
        tempArr.push({
          expr : `${field.conditions} As ${field.outputFieldName}`
        });
      });
      const exprObj = projectionGroupByKeys.map((field) => {return {expr: field};});
      const mergeTempArr = _.concat(tempArr,exprObj);

      this.projectionRulesNode.projections = mergeTempArr;
      this.outputStreamStringArr = [
        'projection_transform_stream_'+this.projectionNode.id,
        'projection_notifier_stream_'+this.projectionNode.id
      ];
      this.projectionRulesNode.outputStreams = this.outputStreamStringArr;

      let promiseArr = [];

      const projectionNodeData = this.updateProcessorNode(name, description);
      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, nodeType, projectionNodeData.id, {body: JSON.stringify(projectionNodeData)}));

      promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'rules', this.projectionRuleId, {body: JSON.stringify(this.projectionRulesNode)}));

      return Promise.all(promiseArr);
    }
  }

  /*
    getReturnType Method accept the params
    Param@ functionName
    Param@ fieldObj
    Param@ index

    And it check the returnType is support in the argument array of the fieldObj
    if argList is empty then it return fieldObj.type and call this.checkArgumentError to show Error on UI
    else 'DOUBLE' as default;
  */
  getReturnType(functionName, fieldObj, index, fields) {
    let obj = this.state.functionListArr.find((o) => {
      return o.name === functionName;
    });
    if (obj) {
      if (obj.argTypes && fieldObj) {
        // let argList = obj.argTypes.toString().includes(fieldObj.type);
        // (argList)
        //   ? this.checkArgumentError(false,fieldObj.name,index, fields)
        //   : this.checkArgumentError(true,fieldObj.name,index, fields);
        return obj.returnType || fieldObj.type;
      }
    } else if (fieldObj) {
      return fieldObj.type;
    } else {
      return 'DOUBLE';
    }
  }

  /* checkArgumentError Method accept flag = boolean , fieldName = "string" , index = any number
     if the flag is true it push the fieldName in this.argumentErrorArr and set argumentError = true
     to show the Error on UI
     if the flag is false it get the diffCheck between the this.argumentErrorArr and outputFieldsArr
     and if diffCheck and outputFieldsArr are both identical it set argumentError = false, this.argumentErrorArr = [];
  */
  // checkArgumentError(flag,fieldName,index, fields){
  //   // const {outputFieldsArr} = this.state;
  //   if(flag){
  //     const indexVal = _.findIndex(this.argumentErrorArr ,(x) => x === fieldName);
  //     indexVal !== -1 ? '' : this.argumentErrorArr.push(fieldName);
  //     this.setState({argumentError : true});
  //   }else{
  //     const diffCheck = _.difference(fields[index].args, this.argumentErrorArr);
  //     if(diffCheck.length === fields[index].args.length){
  //       this.argumentErrorArr = [];
  //       this.setState({argumentError : false});
  //     }
  //   }
  // }

  /*
    handleProjectionKeysChange Method accept arr of obj
    And SET
    projectionKeys : key of arr used on UI for listing
    projectionGroupByKeys : group the projectionKeys
    projectionSelectedKey : store the obj of the projectionKeys
  */
  handleProjectionKeysChange(arr){
    let {fieldList,outputStreamFields,projectionSelectedKey} = this.state;
    const keyData = ProcessorUtils.createSelectedKeysHierarchy(arr,fieldList);
    this.tempStreamContextData.fields = outputStreamFields.length > 0  ? _.concat(keyData , outputStreamFields) : keyData;

    const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
    this.setState({projectionKeys: keys, projectionGroupByKeys: gKeys, projectionSelectedKey: keyData});
    this.context.ParentForm.setState({outputStreamObj: this.tempStreamContextData});
  }

  /*
    handleSelectAllOutputFields method select all keys
  */
  handleSelectAllOutputFields = () => {
    const arr = ProcessorUtils.selectAllOutputFields(this.state.fieldList);
    this.handleProjectionKeysChange(arr);
  }

  /*
    handleFieldChange Method accept index, obj
    And SET functionName of outputFieldsArr
  */
  handleFieldChange(index, obj) {
    obj = obj || {name: ''};
    let tempArr = _.cloneDeep(this.state.outputFieldsArr);
    tempArr[index].functionName = obj.name;
    this.setState({outputFieldsArr : tempArr}, () => {
      this.setParentContextOutputStream(index);
    });
  }

  /*
    handleFieldsKeyChange Method accept index,arr of OBJECT
    argumentKeysGroup is SET in this method for further use in handlesave
  */
  // handleFieldsKeyChange(index,arr){
  //   const {fieldList,argumentKeysGroup } = this.state;
  //   let argumentGroupArr = _.cloneDeep(argumentKeysGroup);
  //   let tempArr = _.cloneDeep(this.state.outputFieldsArr);
  //   const fieldData = ProcessorUtils.createSelectedKeysHierarchy(arr,fieldList);
  //   const {keys,gKeys} = ProcessorUtils.getKeysAndGroupKey(arr);
  //   tempArr[index].args = keys;
  //   argumentGroupArr[index]= gKeys;
  //   this.setState({outputFieldsArr : tempArr, argumentKeysGroup : argumentGroupArr}, () => {
  //     this.setParentContextOutputStream(index);
  //   });
  // }

  /*
    handleFieldNameChange Method accept index and event of Input
    And set outputFieldName
  */
  handleFieldNameChange(index,event){
    let tempArr = _.cloneDeep(this.state.outputFieldsArr);
    let showErr = false;
    if(event.target.value === ""){
      showErr = true;
    }
    tempArr[index].outputFieldName = event.target.value;
    this.setState({invalidInput : showErr,outputFieldsArr : tempArr}, () => {
      this.setParentContextOutputStream(index);
    });
  }

  /*
    This Mehods call from [handleFieldNameChange,handleFieldsKeyChange,handleFieldChange] FUNCTIONS
    setParentContextOutputStream Mehod accept index and update the fields
    With local state and parentContext also;
    And Two array is concat to make the outputStreamObj of parentContext
  */
  setParentContextOutputStream(index) {
    let funcReturnType = "",obj={};
    const {outputFieldsArr,projectionSelectedKey,fieldList,functionListArr} = this.state;
    let mainObj = _.cloneDeep(this.state.outputStreamFields);
    if(!!outputFieldsArr[index].conditions){
      const val = outputFieldsArr[index].conditions;
      obj = this.getReturnTypeFromCodemirror(val.trim(),functionListArr,this.fieldsHintArr);
      funcReturnType = obj.returnType;

      // _.map(outputFieldsArr[index].args, (arg) => {
      //   // set the returnType for function
      //   funcReturnType = this.getReturnType(outputFieldsArr[index].conditions, ProcessorUtils.getKeyList(arg,fieldList),index, outputFieldsArr);
      // });
    }
    mainObj[index] = {
      name: (outputFieldsArr[index].outputFieldName !== undefined && outputFieldsArr[index].outputFieldName !== "") ? outputFieldsArr[index].outputFieldName : "",
      type:  funcReturnType ? funcReturnType : ""
    };
    // b_Index is used to restrict the empty fields in streamObj.
    const b_Index = _.findIndex(outputFieldsArr, (field) => { return field.conditions === '' && field.outputFieldName === '';});
    if(b_Index !== -1){
      mainObj.splice(b_Index,1);
    }
    // create this.tempStreamContextData obj to save in ParentForm context
    const tempStreamData = _.concat(projectionSelectedKey,mainObj);
    this.tempStreamContextData = {fields : tempStreamData  , streamId : this.streamIdList[0]};
    this.setState({outputStreamFields : mainObj,argumentError : !!obj.error ? true : false, errorString : !!obj.error ? obj.error : ''});
    this.context.ParentForm.setState({outputStreamObj: this.tempStreamContextData});
  }

  /*
    addProjectionOutputFields Method add the row on UI with blank text
  */
  addProjectionOutputFields() {
    if (this.state.editMode) {
      const el = document.querySelector('.processor-modal-form ');
      const targetHt = el.scrollHeight;
      Utils.scrollMe(el, (targetHt + 100), 2000);

      let fieldsArr = this.state.outputFieldsArr;
      fieldsArr.push({conditions: '', outputFieldName: '',prefetchData: false});
      this.populateCodeMirrorDefaultHintOptions();
      this.setState({outputFieldsArr: fieldsArr});
    }
  }

  /*
    deleteProjectionRow Method accept the index
    And delete to fields from the two Array [outputFieldsArr , outputStreamFields]
  */
  deleteProjectionRow(index){
    const {projectionSelectedKey, argumentKeysGroup} = this.state;
    let fieldsArr = _.cloneDeep(this.state.outputFieldsArr);
    let mainOutputFields = _.cloneDeep(this.state.outputStreamFields);

    fieldsArr.splice(index,1);
    argumentKeysGroup.splice(index, 1);
    mainOutputFields.splice(index,1);

    const tempStreamData = _.concat(projectionSelectedKey,mainOutputFields);
    this.tempStreamContextData.fields = tempStreamData;
    _.map(fieldsArr, (f) => {
      f.prefetchData = true;
    });
    this.setState({outputFieldsArr : fieldsArr,outputStreamFields : mainOutputFields});
    this.context.ParentForm.setState({outputStreamObj: this.tempStreamContextData});
  }

  handleScriptChange = (index,val) => {
    let tempArr = _.cloneDeep(this.state.outputFieldsArr);
    let showErr = false;
    if(val === ""){
      showErr = true;
    }
    tempArr[index].conditions = val;
    this.setState({invalidInput : showErr,outputFieldsArr : tempArr}, () => {
      this.setParentContextOutputStream(index);
    });;
  }

  getReturnTypeFromCodemirror = (value,functionArr,fieldsArr) => {
    let returnType='DOUBLE',error='';
    const ind =  this.checkBracketInString(value);
    if(ind !== -1){
      this.pushAdditionalHints(this.fieldsHintArr) ;
      const {f_val, s_val} = this.stringSpliter(value,ind);
      const obj = _.find(functionArr, (func) => func.displayName === f_val);
      if(obj){
        returnType = obj.returnType;
      }
      // s_val is the string after the function bracket.. and recursive call
      if(!!s_val){
        const nestedFunction = (val,level,oldObj) => {
          const b_index = this.checkBracketInString(val);
          if(b_index !== -1){
            const {f_val, s_val} = this.stringSpliter(val,b_index);
            const innerObj = _.find(functionArr, (func) => func.displayName === f_val);
            if(innerObj){
              const funcResultObj = this.checkReturnTypeSupport(obj,innerObj,'returnType');
              if(!!funcResultObj.returnType){
                returnType = funcResultObj.returnType;
              }
              error = funcResultObj.error;
              if(!!s_val){
                nestedFunction(s_val,level+1,innerObj);
              }
            } else {
              error = "The function is invalid.";
            }
          } else {
            const trimVal = val.endsWith(')') ? val.replace(/[)]/gi,'') : val;
            const inner_Args = this.findNestedObj(fieldsArr,trimVal);
            if(!_.isEmpty(inner_Args)){
              const argResultObj = this.checkReturnTypeSupport(obj,inner_Args,'type');
              if(!!argResultObj.returnType){
                returnType = argResultObj.returnType;
              }
              error = argResultObj.error;
            } else {
              if(/[,]/.test(val)){
                const trimVal = val.endsWith(')') ? val.replace(/[)]/gi,'') : val;
                let args = trimVal.split(',');
                let o = level > 0 ? oldObj : obj;
                if(/[)]/.test(val)){
                  const openB = value.split('(').splice(0,(value.split('(').length-1));
                  const closeB = val.split(')').splice(0,(val.split(')').length-1));
                  let ObjName='';
                  const openL = openB.length, closeL = closeB.length;
                  if(openB.length > 1){
                    ObjName = openB[closeL > 1 ? ((openL === closeL || openL < closeL) ? 0 : (closeL-1)) : closeL];
                  } else {
                    ObjName= openB[0];
                  }
                  o = _.find(functionArr, (func) => func.displayName === ObjName);
                  args = _.compact(val.split(')').map(function(a) {return a.replace(/[,]/gi,'');}));
                  if(openL === closeL || openL < closeL){
                    args = args.splice(0,o.argTypes.length);
                  }
                }
                if(args.length <= o.argTypes.length){
                  _.map(args, (a,i) => {
                    const fields = this.findNestedObj(fieldsArr,a);
                    if(!_.isEmpty(fields) && _.isNaN(parseInt(a))){
                      const argObj = this.checkReturnTypeSupport(o,fields,'type');
                      if(!!argObj.returnType){
                        returnType = argObj.returnType;
                      }
                      error = argObj.error || (argObj.returnType !== o.argTypes[i]) ? `Function doesn't support the arguments return type.` : null;
                    }
                  });
                } else {
                  error = `Function doesn't support more arguments .`;
                }
              } else if(/[']/.test(val)){
              }else {
                if(!this.checkValueTypeToReturnType(trimVal,returnType).flag){
                  error = "The arguments is invalid.";
                }
              }
            }
          }
        };
        nestedFunction(s_val,0);
      }

    } else {
      this.populateCodeMirrorDefaultHintOptions();
    }
    return {returnType,error};
  }

  checkValueTypeToReturnType = (val,returnType) => {
    let flag= false;
    const returnTyp = !!returnType ? returnType.toLowerCase() : '';
    let tVal = _.isNaN(parseInt(val)) ? val : parseInt(val);
    const type = typeof tVal;
    switch(type){
    case 'string' : flag = type === returnTyp ? true : false;
      break;
    case 'number' : flag = (returnTyp !== 'string') ? true : false;
      break;
    default:break;
    }
    return {flag,type};
  }

  findNestedObj = (fieldsArr,string) => {
    let obj={};
    const recursiveFunc = (arr,s) => {
      _.map(arr, (a) => {
        if(a.fields){
          recursiveFunc(a.fields,s);
        } else {
          if(a.name === s){
            obj = a;
          }
        }
      });
      return obj;
    };
    const str = /[.]/.test(string) ? _.last(string.split('.')) : string;
    return recursiveFunc(fieldsArr,str);
  }

  checkReturnTypeSupport = (pObj,innerObj,type) => {
    const obj = {};
    const returnFlag = pObj.argTypes.toString().includes(innerObj[type]);
    if(returnFlag){
      obj.returnType = innerObj[type];
    } else {
      if(!_.isEmpty(innerObj)){
        obj.error = "Function doesn't support the arguments return type." ;
      }
    }
    return obj;
  }

  checkBracketInString = (value) => {
    return value.indexOf('(');
  }

  stringSpliter = (val,index) => {
    let f_val = val.slice(0,index);
    const s_val = val.slice((index+1),val.length);
    if(/[,]/.test(f_val)){
      f_val = _.findLast(f_val.split(','));
    }
    return {f_val,s_val};
  }

  initValidatorWorker(){
    const {fieldsHintArr,state} = this;
    const {functionListArr} = state;

    return `data:text/javascript;charset=US-ASCII,
      let funcs = ${JSON.stringify(functionListArr)};
      let arg = ${JSON.stringify(fieldsHintArr)};

      self.onmessage = function(msg) {
        const {id, payload} = msg.data
        self.validator(payload,function(err,result){
          const msg = {
            id,
            payload: result
          };
          if(err){
            msg.err = err;
          }
          self.postMessage(msg);
        });
      }

      const obj={};
      const defaults = {
        BOOLEAN: new Boolean().valueOf(),
        BYTE: new Number().valueOf(),
        SHORT: new Number().valueOf(),
        INTEGER: new Number().valueOf(),
        LONG: new Number().valueOf(),
        FLOAT: new Number().valueOf(),
        DOUBLE: new Number().valueOf(),
        STRING: new String().valueOf(),
        BINARY: new Blob().valueOf(),
        NESTED: new Object().valueOf(),
        ARRAY: new Array().valueOf(),
        BLOB: new Blob().valueOf()
      };

      for(let i = 0; i < funcs.length;i++){
        const fd = funcs[i];
        eval('var '+fd.displayName+' = function(){ checkForArgs(arguments, fd.displayName); return defaults[fd.returnType]}');
      }
      /*for(let i = 0; i < arg.length;i++){
        const argd = arg[i];
        eval('var '+argd.name+' = [argd.name]]');
      }*/

      function nestedArguments(arg,level,path = []){
        for(let i = 0; i < arg.length;i++){
          if(arg[i].fields){;
            let _path = path.slice();
            _path.push(arg[i].name);
            try{
              const field = eval(_path.join('.'));
              if(field == undefined){
                eval(_path.join('.') + ' = {}');
              }
            }catch(e){
              eval(' ' + arg[i].name + ' = {}');
            }
            nestedArguments(arg[i].fields,level+1,_path);
          }else{
            let argd = path.length ? path.join('.')+'.'+arg[i].name : arg[i].name ;
            eval(' '+argd+' = defaults[arg[i].type]');
          }
        }
      };
      this.nestedArguments(arg,0);

      function checkForArgs(arg, fname){
        var argLength = arg.length;
        const func_def = funcs.find((f) => {
          return f.displayName == fname && f.argTypes.length == argLength;
        });
        if(!func_def){
          throw new Error(fname +'() arguments mismatch');
        }
        for(let i = 0; i < argLength; i++){
          const _arg = arg[i];
          const ex_arg = func_def.argTypes[i];
          /*if(ex_arg.indexOf(_arg.type) < 0){
            throw new Error(fname +'() argument type mismatch');
          }*/
          function checkType(types){
            let includes = false;
            types.forEach(type => {
              if(ex_arg.includes(type) && !includes){
                includes = true;
              }
            });
            if(!includes){
              throw new Error(fname +'() argument type mismatch');
            }
          }
          if(_arg == undefined){
            checkType([]);
          }else if(typeof _arg == "string"){
            checkType(['STRING']);
          }else if(typeof _arg == "number"){
            checkType(['BYTE', 'SHORT', 'INTEGER', 'LONG', 'FLOAT', 'DOUBLE']);
          }else if(typeof _arg == "boolean"){
            checkType(['BOOLEAN']);
          }else if(typeof _arg == "object" && _arg instanceof Object){
            checkType(['NESTED']);
          }else if(typeof _arg == "object" && _arg instanceof Array){
            checkType(['ARRAY']);
          }else if(typeof _arg == "object" && _arg instanceof Blob){
            checkType(['BLOB']);
          }
        }
      }

      function validator(data,cb){
        try{
          eval('('+ data +')');
          cb(null, data);
        }catch(err){
          cb(err.message, data);
        }
      }
    `;
  }

  render() {
    let {
      selectedKeys,
      fieldList,
      editMode,
      outputFieldsArr,
      functionListArr,
      outputStreamId,
      argumentError,
      invalidInput,
      projectionKeys,
      showLoading,
      errorString,
      scriptErrors
    } = this.state;
    const disabledFields = this.props.testRunActivated ? true : !editMode;
    return (
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
                <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Projection keys</Popover>}>
                <label>Projection Fields
                  <span className="text-danger">*</span>
                </label>
                </OverlayTrigger>
                <label className="pull-right">
                  <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Select All Keys</Popover>}>
                    <a href="javascript:void(0)" onClick={this.handleSelectAllOutputFields}>Select All</a>
                  </OverlayTrigger>
                </label>
                  <Select  value={projectionKeys} options={fieldList} onChange={this.handleProjectionKeysChange.bind(this)} multi={true} required={true} disabled={disabledFields} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
              </div>
              <div className="form-group">
                <div className="row">
                  <div className="col-sm-12">
                    {(argumentError)
                      ? <label className="color-error">{errorString}{/*The Projection Function is not supported by input*/}</label>
                      : ''
  }
                  </div>
                </div>
                <div className="row">
                  <div className="col-sm-7 outputCaption">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Projection Conditions</Popover>}>
                    <label>Projection Conditions</label>
                    </OverlayTrigger>
                  </div>
                  <div className="col-sm-3 outputCaption">
                    <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Output field name</Popover>}>
                    <label>Fields Name</label>
                    </OverlayTrigger>
                  </div>
                </div>
                {outputFieldsArr.map((obj, i) => {
                  const functionClass = ['projection-codemirror'];
                  const argumentsClass = [];
                  const nameClass = ['form-control'];

                  if(obj.conditions.length == 0 && obj.outputFieldName.length > 0){
                    functionClass.push('invalid-codemirror');
                  }
                  if(obj.outputFieldName.length == 0 && obj.conditions.length > 0){
                    nameClass.push('invalidInput');
                  }

                  return (
                    <div key={i} className="row form-group">
                      {
                        scriptErrors[i]
                        ? <div><label  className="color-error" style={{fontSize:10}}>{scriptErrors[i]}</label></div>
                        : null
                      }
                      <div className="col-sm-7">
                        <div className={functionClass.join(' ')}>
                          <CommonCodeMirror editMode={obj.prefetchData} modeType="javascript" hintOptions={this.hintOptions} value={obj.conditions} placeHolder="Conditions goes here..." callBack={this.handleScriptChange.bind(this,i)} />
                        </div>
                      </div>
                      <div className="col-sm-3">
                        <input name="outputFieldName" className={nameClass.join(' ')} value={obj.outputFieldName} ref="outputFieldName" onChange={this.handleFieldNameChange.bind(this, i)} type="text" required={true} disabled={disabledFields}/>
                      </div>
                      {editMode
                        ? <div className="col-sm-2">
                            <button className="btn btn-default btn-sm" type="button" disabled={disabledFields} onClick={this.addProjectionOutputFields.bind(this)}>
                              <i className="fa fa-plus"></i>
                            </button>&nbsp; {i > 0
                              ? <button className="btn btn-sm btn-danger" type="button" onClick={this.deleteProjectionRow.bind(this, i)}>
                                  <i className="fa fa-trash"></i>
                                </button>
                              : null}
                          </div>
                        : null}
                    </div>
                  );
                })}
              </div>
            </form>
        }
        </Scrollbars>
      </div>
    );
  }
}

ProjectionProcessorContainer.contextTypes = {
  ParentForm: PropTypes.object
};
