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
import {Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import AggregateUdfREST from '../../../rest/AggregateUdfREST';
import Utils from '../../../utils/Utils';
import CommonNotification from '../../../utils/CommonNotification';
import {toastOpt} from '../../../utils/Constants';
import {Scrollbars} from 'react-custom-scrollbars';

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
    this.sourceNodesId = [props.sourceNode.nodeId];
    this.ruleTargetNodes = [];
    this.fieldsArr = [];
    let {editMode} = props;
    this.fetchTopologyConfig();
    this.fetchData();
    var obj = {
      parallelism: 1,
      editMode: editMode,
      selectedKeys: [],
      _groupByKeys: [],
      streamsList: [],
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
      outputFieldsArr: [
        {
          args: '',
          functionName: '',
          outputFieldName: ''
        }
      ],
      functionListArr: [],
      outputStreamId: '',
      outputStreamFields: [],
      argumentError: false,
      outputArr: [],
      keySelected: []
    };
    this.state = obj;
    this.outputData = [];
  }

  fetchTopologyConfig() {
    let {topologyId, versionId} = this.props;
    TopologyREST.getTopologyWithoutMetrics(topologyId, versionId).then((result) => {
      this.topologyConfigResponse = result;
      this.topologyConfig = JSON.parse(result.config);
    });
  }

  fetchData() {
    let {
      topologyId,
      versionId,
      nodeType,
      nodeData,
      currentEdges,
      targetNodes
    } = this.props;
    let edgePromiseArr = [];
    let ruleTargetNodes = targetNodes.filter((o) => {
      return o.currentType.toLowerCase() === 'rule';
    });
    currentEdges.map(edge => {
      if (edge.target.nodeId === nodeData.nodeId) {
        edgePromiseArr.push(TopologyREST.getNode(topologyId, versionId, 'edges', edge.edgeId));
      }
    });
    Promise.all(edgePromiseArr).then(edgeResults => {
      let promiseArr = [
        TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId),
        AggregateUdfREST.getAllUdfs()
      ];
      let streamIdArr = [];
      edgeResults.map(result => {
        if (result.streamGroupings) {
          result.streamGroupings.map(streamObj => {
            if (streamIdArr.indexOf(streamObj.streamId) === -1) {
              streamIdArr.push(streamObj.streamId);
              promiseArr.push(TopologyREST.getNode(topologyId, versionId, 'streams', streamObj.streamId));
            }
            this.connectingEdge = result;
          });
        }
      });
      let rulePromiseArr = [];
      ruleTargetNodes.map((ruleNode) => {
        rulePromiseArr.push(TopologyREST.getNode(topologyId, versionId, nodeType, ruleNode.nodeId));
      });
      Promise.all(rulePromiseArr).then((results) => {
        results.map((o) => {
          this.ruleTargetNodes.push(o);
        });
      });
      Promise.all(promiseArr).then((results) => {
        this.nodeData = results[0];
        let configFields = this.nodeData.config.properties;
        this.windowId = configFields.rules
          ? configFields.rules[0]
          : null;
        let fields = [];
        let streamsList = [];
        //Gather all aggregate functions only
        let udfList = this.udfList = [];
        results[1].entities.map((funcObj) => {
          if (funcObj.type === 'AGGREGATE') {
            udfList.push(funcObj);
          }
        });
        results.map((result, i) => {
          if (i > 1) {
            streamsList.push(result);
            fields.push(...result.fields);
          }
        });
        this.fieldsArr = [];
        this.getSchemaFields(fields, 0);
        let stateObj = {
          streamsList: streamsList,
          keysList: JSON.parse(JSON.stringify(this.fieldsArr)),
          parallelism: configFields.parallelism || 1,
          functionListArr: udfList,
          outputArr: this.outputData
        };
        //Find output streams and set appropriate fields
        //else create streams with blank values
        if (this.nodeData.outputStreams && this.nodeData.outputStreams.length > 0) {
          this.streamData = this.nodeData.outputStreams[0];
          stateObj.outputStreamId = this.nodeData.outputStreams[0].streamId;
          stateObj.outputStreamFields = JSON.parse(JSON.stringify(this.nodeData.outputStreams[0].fields));
          this.context.ParentForm.setState({outputStreamObj: this.streamData});
        } else {
          stateObj.outputStreamId = 'window_transform_stream_' + this.nodeData.id;
          stateObj.outputStreamFields = [];
          this.streamData = {
            streamId: stateObj.outputStreamId,
            fields: stateObj.outputStreamFields
          };
          this.context.ParentForm.setState({outputStreamObj: this.streamData});
        }
        if (this.windowId) {
          TopologyREST.getNode(topologyId, versionId, 'windows', this.windowId).then((windowResult) => {
            let windowData = windowResult;
            if (windowData.projections.length === 0) {
              stateObj.outputFieldsArr = [
                {
                  args: '',
                  functionName: '',
                  outputFieldName: ''
                }
              ];
            } else {
              stateObj.outputFieldsArr = [];
              windowData.projections.map(o => {
                if (o.expr) {
                  if (windowData.groupbykeys.indexOf(o.expr) !== -1) {
                    delete o.expr;
                  } else {
                    if (o.expr.search('\\[') !== -1) {
                      let a = o.expr.replace("['", " ").replace("']", " ").split(' ');
                      if (a.length > 1) {
                        o.expr = a[a.length - 1] != ''
                          ? a[a.length - 1]
                          : a[a.length - 2];
                      } else {
                        o.expr = a[0];
                      }
                    }
                    o.args = o.expr;
                    delete o.expr;
                    stateObj.outputFieldsArr.push(o);
                    this.fetchOutputFields(o, stateObj.keysList);
                  }
                } else {
                  o.args = o.args[0];
                  if (o.args.search('\\[') !== -1) {
                    let a = o.args.replace("['", " ").replace("']", " ").split(' ');
                    if (a.length > 1) {
                      o.args = a[a.length - 1] != ''
                        ? a[a.length - 1]
                        : a[a.length - 2];
                    } else {
                      o.args = a[0];
                    }
                  }
                  stateObj.outputFieldsArr.push(o);
                  this.fetchOutputFields(o, stateObj.keysList);
                }
              });

            }
            stateObj.selectedKeys = [];
            windowData.groupbykeys.map((key) => {
              if (key.search('\\[') !== -1) {
                let a = key.replace("['", " ").replace("']", " ").split(' ');
                if (a.length > 1) {
                  key = a[a.length - 1] != ''
                    ? a[a.length - 1]
                    : a[a.length - 2];
                } else {
                  key = a[0];
                }
              }
              stateObj.selectedKeys.push(key);
            });
            stateObj._groupByKeys = windowData.groupbykeys;
            this.windowAction = windowData.actions;
            if (windowData.window) {
              if (windowData.window.windowLength.class === '.Window$Duration') {
                stateObj.intervalType = '.Window$Duration';
                let obj = Utils.millisecondsToNumber(windowData.window.windowLength.durationMs);
                stateObj.windowNum = obj.number;
                stateObj.durationType = obj.type;
                if (windowData.window.slidingInterval) {
                  let obj = Utils.millisecondsToNumber(windowData.window.slidingInterval.durationMs);
                  stateObj.slidingNum = obj.number;
                  stateObj.slidingDurationType = obj.type;
                }
              } else if (windowData.window.windowLength.class === '.Window$Count') {
                stateObj.intervalType = '.Window$Count';
                stateObj.windowNum = windowData.window.windowLength.count;
                if (windowData.window.slidingInterval) {
                  stateObj.slidingNum = windowData.window.slidingInterval.count;
                }
              }
            }
            this.setState(stateObj, () => {
              this.fetchSelectedKeys();
            });
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
          TopologyREST.createNode(topologyId, versionId, 'windows', {body: JSON.stringify(dummyWindowObj)}).then((windowResult) => {
            this.windowId = windowResult.id;
            this.nodeData.config.properties.rules = [this.windowId];
            TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {
              body: JSON.stringify(this.nodeData)
            });
            this.setState(stateObj, () => {
              this.fetchSelectedKeys();
            });
          });
        }
      });
    });

  }

  getSchemaFields(fields, level, keyPath = []) {
    fields.map((field) => {
      let obj = {
        name: field.name,
        optional: field.optional,
        type: field.type,
        level: level,
        keyPath: ''
      };

      if (field.type === 'NESTED') {
        obj.disabled = true;
        let _keypath = keyPath.slice();
        _keypath.push(field.name);
        this.fieldsArr.push(obj);
        this.getSchemaFields(field.fields, level + 1, _keypath);
      } else {
        obj.disabled = false;
        obj.keyPath = keyPath.join('.');
        this.fieldsArr.push(obj);
      }

    });
  }

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

  fetchOutputFields = (outputObj, list) => {
    let tempData = list.find((field) => {
      return field.name === outputObj.args;
    });
    this.outputData.push({
      name: outputObj.outputFieldName,
      type: this.getReturnType(outputObj.functionName, tempData),
      optional: false
    });
  }

  fetchSelectedKeys = () => {
    const {selectedKeys, keysList, outputArr} = this.state;
    const tempData = selectedKeys.map((x, i) => {
      return keysList.find((x) => {
        return x.name == selectedKeys[i];
      });
    });
    const keyData = this.createSelectedKeysHierarchy(tempData);
    const keyObj = _.concat(keyData, outputArr);
    this.streamData.fields = keyObj;
    this.setState({keySelected: keyData, outputStreamFields: keyObj});
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }
  createSelectedKeysHierarchy = (arrKeys) => {
    let tempArr = [];
    const grouped = _.groupBy(arrKeys, (d) => {
      return d.keyPath;
    });

    _.each(grouped, (d, key) => {
      if (key.length > 0) {
        let fieldNames = key.split('.');
        let _arr = tempArr;
        fieldNames.forEach((name, i) => {

          function find(_tempArr) {
            let fieldD;
            _.each(_tempArr, (_d) => {
              if (_d.name == name) {
                fieldD = _d;
              } else if (_d.fields && _d.fields.length) {
                fieldD = find(_d.fields);
              }
            });
            return fieldD;
          }
          let fieldData = find(tempArr);
          let _fieldData;
          if (fieldData) {
            _fieldData = fieldData;
          } else {
            fieldData = _.find(this.fieldsArr, {name: name});
            _fieldData = JSON.parse(JSON.stringify(fieldData));
          }

          _fieldData.fields = _fieldData.fields || [];
          if (_arr.indexOf(_fieldData) == -1) {
            _arr.push(_fieldData);
          }
          _arr = _fieldData.fields;
          if (i == fieldNames.length - 1) {
            var cloned = JSON.parse(JSON.stringify(d));
            _arr.push.apply(_arr, cloned);
          }
        });
      } else {
        var cloned = JSON.parse(JSON.stringify(d));
        tempArr.push.apply(tempArr, cloned);
      }
    });
    return tempArr;
  }
  handleKeysChange(arr) {
    let {outputArr} = this.state;
    const keyData = this.createSelectedKeysHierarchy(arr);
    let tempArr = _.concat(keyData, outputArr);
    this.streamData.fields = tempArr;
    let keys = [];
    let gKeys = [];
    if (arr && arr.length) {
      for (let k of arr) {
        if (k.level !== 0) {
          let t = '';
          let parents = k.keyPath.split('.');
          let s = parents.splice(0, 1);
          parents.push(k.name);
          t = s + "['" + parents.toString().replace(",", "']['") + "']";
          gKeys.push(t);
        } else {
          gKeys.push(k.name);
        }
        keys.push(k.name);
      }
      this.setState({selectedKeys: keys, _groupByKeys: gKeys, outputStreamFields: tempArr, keySelected: keyData});
    } else {
      this.setState({selectedKeys: [], _groupByKeys: [], outputStreamFields: tempArr, keySelected: keyData});
    }
    this.context.ParentForm.setState({outputStreamObj: this.streamData});
  }

  handleIntervalChange(obj) {
    if (obj) {
      this.setState({intervalType: obj.value});
    } else {
      this.setState({intervalType: ""});
    }
  }

  handleDurationChange(obj) {
    if (obj) {
      this.setState({durationType: obj.value, slidingDurationType: obj.value});
    } else {
      this.setState({durationType: "", slidingDurationType: ""});
    }
  }

  handleSlidingDurationChange(obj) {
    if (obj) {
      this.setState({slidingDurationType: obj.value});
    } else {
      this.setState({slidingDurationType: ""});
    }
  }

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

  handleFieldChange(name, index, obj) {
    let fieldsArr = this.state.outputFieldsArr;
    let oldData = JSON.parse(JSON.stringify(fieldsArr[index]));
    if (name === 'outputFieldName') {
      fieldsArr[index][name] = obj.target.value;
    } else {
      if (obj) {
        fieldsArr[index][name] = obj.name;
      } else {
        fieldsArr[index][name] = '';
      }
      if (fieldsArr[index].args !== '') {
        let appendingName = '';
        if (fieldsArr[index].functionName !== '') {
          let obj = this.udfList.find((o) => {
            return o.name === fieldsArr[index].functionName;
          });
          appendingName = '_' + obj.displayName;
        }
        fieldsArr[index].outputFieldName = fieldsArr[index].args + appendingName;
      }
    }
    let outputStreamFields = this.getOutputFieldsForStream(oldData, fieldsArr[index], index);
    outputStreamFields.then((res) => {
      this.streamData.fields = res;
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
      this.setState({outputStreamFields: res});
    });

    this.setState({outputFieldsArr: fieldsArr});
  }

  getOutputFieldsForStream(oldObj, newDataObj, index) {
    let streamsArr = this.state.outputStreamFields;
    let obj = null;
    if (oldObj.outputFieldName !== '') {
      obj = this.outputData.filter((field) => {
        return field.name === oldObj.outputFieldName;
      })[0];
    } else {
      obj = this.outputData.filter((field) => {
        return field.name === oldObj.args;
      })[0];
    }
    if (obj) {
      let fieldObj = this.state.keysList.find((field) => {
        return field.name == newDataObj.args;
      });
      if (newDataObj.functionName !== '') {
        obj.name = newDataObj.outputFieldName;
        obj.type = this.getReturnType(newDataObj.functionName, fieldObj);
      } else if (oldObj.functionName !== '') {
        obj.name = newDataObj.outputFieldName;
        obj.type = this.getReturnType(newDataObj.functionName, fieldObj);
      }
    } else {
      let fieldObj = this.state.keysList.find((field) => {
        return field.name == newDataObj.args;
      });
      if (index === this.outputData.length) {
        this.outputData.push({
          name: newDataObj.outputFieldName,
          type: this.getReturnType(newDataObj.functionName, fieldObj),
          optional: false
        });
      } else {
        this.outputData[index].name = newDataObj.outputFieldName;
      }
    }

    let resolve;
    this.setState({
      outputArr: this.outputData
    }, () => {
      const data = _.concat(this.state.keySelected, this.outputData);
      resolve(streamsArr = data);
    });

    const outputAction = new Promise((res, rej) => {
      resolve = res;
    });
    return outputAction;
  }

  getReturnType(functionName, fieldObj) {
    let obj = this.udfList.find((o) => {
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
  addOutputFields() {
    if (this.state.editMode) {
      const el = document.querySelector('.processor-modal-form ');
      const targetHt = el.scrollHeight;
      Utils.scrollMe(el, (targetHt + 100), 2000);

      let fieldsArr = this.state.outputFieldsArr;
      fieldsArr.push({args: '', functionName: '', outputFieldName: ''});
      this.setState({outputFieldsArr: fieldsArr});
    }
  }
  deleteFieldRow(index) {
    if (this.state.editMode) {
      let fieldsArr = this.state.outputFieldsArr;
      let outputStreamFields = this.state.outputStreamFields;
      let o = fieldsArr[index];
      if (o.outputFieldName !== '') {
        let streamObj = outputStreamFields.filter((field) => {
          return field.name === o.outputFieldName;
        })[0];
        if (streamObj) {
          let streamObjIndex = outputStreamFields.indexOf(streamObj);
          if (streamObjIndex !== -1) {
            outputStreamFields.splice(streamObjIndex, 1);
          }
        }
      }
      fieldsArr.splice(index, 1);
      this.outputData.splice(index, 1);
      this.streamData.fields = outputStreamFields;
      this.context.ParentForm.setState({outputStreamObj: this.streamData});
      this.setState({outputFieldsArr: fieldsArr, outputStreamFields: outputStreamFields, outputArr: this.outputData});
    }
  }

  validateData() {
    let {selectedKeys, windowNum, outputFieldsArr, argumentError} = this.state;
    let validData = true;
    if (selectedKeys.length === 0 || windowNum === '') {
      validData = false;
    }
    outputFieldsArr.map((obj) => {
      if (obj.args === '' || obj.outputFieldName === '') {
        validData = false;
      }
    });
    if (argumentError) {
      return false;
    }
    return validData;
  }

  searchSchemaForFields(fields) {
    let flag = false;
    fields.map((field) => {
      if (!flag) {
        if (field.type == 'NESTED') {
          flag = this.searchSchemaForFields(field.fields);
        } else if (this.state.selectedKeys.indexOf(field.name) != -1) {
          flag = true;
        }
      }
    });
    return flag;
  }

  handleSave(name, description) {
    let {
      _groupByKeys,
      selectedKeys,
      windowNum,
      slidingNum,
      outputFieldsArr,
      durationType,
      slidingDurationType,
      intervalType,
      streamsList,
      parallelism
    } = this.state;
    let {topologyId, versionId, nodeType, nodeData} = this.props;
    let windowObj = {
      name: 'window_auto_generated',
      description: 'window description auto generated',
      projections: [],
      streams: [],
      groupbykeys: _groupByKeys,
      window: {
        windowLength: {
          class: intervalType
        }
      },
      actions: this.windowAction || []
    };
    let promiseArr = [TopologyREST.getNode(topologyId, versionId, 'windows', this.windowId)];

    //Adding stream names into data
    streamsList.map((stream) => {
      if (this.searchSchemaForFields(stream.fields) === true) {
        if (windowObj.streams.indexOf(stream.streamId) === -1) {
          windowObj.streams.push(stream.streamId);
        }
      }
    });
    //Adding projections aka output fields into data
    outputFieldsArr.map((obj) => {
      let fieldObj = this.fieldsArr.find((f) => {
        return f.name == obj.args;
      });
      let argsOrExpr = obj.args;
      if (fieldObj.level !== 0) {
        let parents = fieldObj.keyPath.split('.');
        let s = parents.splice(0, 1);
        parents.push(obj.args);
        argsOrExpr = s + "['" + parents.toString().replace(",", "']['") + "']";
      }
      let o = {};
      if (!obj.functionName || obj.functionName === '') {
        o.expr = argsOrExpr;
      } else {
        o.args = [argsOrExpr];
        o.functionName = obj.functionName;
      }
      o.outputFieldName = obj.outputFieldName;
      windowObj.projections.push(o);
    });
    _groupByKeys.map((field) => {
      let o = {
        expr: field
      };
      if (windowObj.projections.indexOf(o) === -1) {
        windowObj.projections.push(o);
      }
    });
    //Syncing window object into data
    if (intervalType === '.Window$Duration') {
      windowObj.window.windowLength.durationMs = Utils.numberToMilliseconds(windowNum, durationType);
      if (slidingNum !== '') {
        windowObj.window.slidingInterval = {
          class: intervalType,
          durationMs: Utils.numberToMilliseconds(slidingNum, slidingDurationType)
        };
      }
    } else if (intervalType === '.Window$Count') {
      windowObj.window.windowLength.count = windowNum;
      if (slidingNum !== '') {
        windowObj.window.slidingInterval = {
          class: intervalType,
          count: slidingNum
        };
      }
    }
    if (this.windowId) {
      return Promise.all(promiseArr).then((results) => {
        let data = results[0];
        windowObj.actions = data.actions || [];
        if (data.outputStreams && data.outputStreams.length > 0) {
          windowObj.outputStreams = data.outputStreams;
        } else {
          windowObj.outputStreams = [
            this.streamData.streamId, 'window_notifier_stream_' + nodeData.nodeId
          ];
        }
        return TopologyREST.updateNode(topologyId, versionId, 'windows', this.windowId, {body: JSON.stringify(windowObj)}).then(windowResult => {
          return this.updateNode(windowResult, name, description);
        });
      });
    }
  }
  generateOutputFields(fields, level) {
    return fields.map((field) => {
      let obj = {
        name: field.name,
        optional: field.optional,
        type: field.type
      };

      if (field.type === 'NESTED' && field.fields) {
        obj.fields = this.generateOutputFields(field.fields, level + 1);
      }
      return obj;
    });
  }
  updateNode(windowObj, name, description) {
    let {parallelism, outputStreamFields} = this.state;
    let {topologyId, versionId, nodeType, nodeData} = this.props;
    return TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId).then(result => {
      let data = result;
      if (windowObj.responseMessage !== undefined) {
        FSReactToastr.error(
          <CommonNotification flag="error" content={windowObj.responseMessage}/>, '', toastOpt);
      } else {
        let windowData = windowObj;
        data.config.properties.parallelism = parallelism;
        data.config.properties.rules = [windowData.id];
        this.streamFields = this.generateOutputFields(this.streamData.fields, 0);
        if (data.outputStreams.length > 0) {
          data.outputStreams.map((s) => {
            s.fields = this.streamFields;
          });
        } else {
          data.outputStreams.push({streamId: this.streamData.streamId, fields: this.streamFields});
          data.outputStreams.push({
            streamId: 'window_notifier_stream_' + nodeData.nodeId,
            fields: this.streamFields
          });
        }
        data.name = name;
        data.description = description;
        let promiseArr = [TopologyREST.updateNode(topologyId, versionId, nodeType, nodeData.nodeId, {body: JSON.stringify(data)})];
        //Updating target rule streams as they are supposed to be same like it's source output stream
        this.ruleTargetNodes.map((ruleNode) => {
          let streamObj = {
            streamId: ruleNode.outputStreams[0].streamId,
            fields: this.streamFields
          };
          promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'streams', ruleNode.outputStreams[0].id, {body: JSON.stringify(streamObj)}));
        });
        let fields = windowObj.groupbykeys.map((field) => {
          return field.replace(/\[\'/g, ".").replace(/\'\]/g, "");
        });
        let edgeObj = {
          fromId: this.connectingEdge.fromId,
          toId: this.connectingEdge.toId,
          streamGroupings: [
            {
              streamId: this.connectingEdge.streamGroupings[0].streamId,
              grouping: 'FIELDS',
              fields: fields
            }
          ]
        };
        promiseArr.push(TopologyREST.updateNode(topologyId, versionId, 'edges', this.connectingEdge.id, {body: JSON.stringify(edgeObj)}));
        return Promise.all(promiseArr);
      }
    });
  }

  render() {
    let {
      parallelism,
      selectedKeys,
      keysList,
      editMode,
      intervalType,
      intervalTypeArr,
      windowNum,
      slidingNum,
      durationType,
      slidingDurationType,
      durationTypeArr,
      outputFieldsArr,
      functionListArr,
      outputStreamId,
      outputStreamFields,
      argumentError
    } = this.state;
    let {topologyId, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
    return (
      <div className="modal-form processor-modal-form">
        <Scrollbars autoHide renderThumbHorizontal={props => <div {...props} style={{
          display: "none"
        }}/>}>
          <form className="customFormClass">
            <div className="form-group">
              <label>Select Keys
                <span className="text-danger">*</span>
              </label>
              <div>
                <Select value={selectedKeys} options={keysList} onChange={this.handleKeysChange.bind(this)} multi={true} required={true} disabled={!editMode} valueKey="name" labelKey="name" optionRenderer={this.renderFieldOption.bind(this)}/>
              </div>
            </div>
            <div className="form-group">
              <label>Window Interval Type
                <span className="text-danger">*</span>
              </label>
              <div>
                <Select value={intervalType} options={intervalTypeArr} onChange={this.handleIntervalChange.bind(this)} required={true} disabled={!editMode} clearable={false}/>
              </div>
            </div>
            <div className="form-group">
              <label>Window Interval
                <span className="text-danger">*</span>
              </label>
              <div className="row">
                <div className="col-sm-5">
                  <input name="windowNum" value={windowNum} onChange={this.handleValueChange.bind(this)} type="number" className="form-control" required={true} disabled={!editMode} min="0" inputMode="numeric"/>
                </div>
                {intervalType === '.Window$Duration'
                  ? <div className="col-sm-5">
                      <Select value={durationType} options={durationTypeArr} onChange={this.handleDurationChange.bind(this)} required={true} disabled={!editMode} clearable={false}/>
                    </div>
                  : null}
              </div>
            </div>
            <div className="form-group">
              <label>Sliding Interval</label>
              <div className="row">
                <div className="col-sm-5">
                  <input name="slidingNum" value={slidingNum} onChange={this.handleValueChange.bind(this)} type="number" className="form-control" required={true} disabled={!editMode} min="0" inputMode="numeric"/>
                </div>
                {intervalType === '.Window$Duration'
                  ? <div className="col-sm-5">
                      <Select value={slidingDurationType} options={durationTypeArr} onChange={this.handleSlidingDurationChange.bind(this)} required={true} disabled={!editMode} clearable={false}/>
                    </div>
                  : null}
              </div>
            </div>
            {/*  <div className="form-group">
                            <label>Parallelism</label>
                            <div>
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
                            </div>
                        </div>*/}
            <fieldset className="fieldset-default">
              <legend>Output Fields</legend>
              {(argumentError)
                ? <label className="color-error">The Aggregate Function is not supported by input</label>
                : ''
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
                    <div className="col-sm-3">
                      <Select className={outputFieldsArr.length - 1 === i
                        ? "menu-outer-top"
                        : ''} value={obj.args} options={keysList} onChange={this.handleFieldChange.bind(this, 'args', i)} required={true} disabled={!editMode} valueKey="name" labelKey="name" clearable={false} optionRenderer={this.renderFieldOption.bind(this)}/>
                    </div>
                    <div className="col-sm-3">
                      <Select className={outputFieldsArr.length - 1 === i
                        ? "menu-outer-top"
                        : ''} value={obj.functionName} options={functionListArr} onChange={this.handleFieldChange.bind(this, 'functionName', i)} required={true} disabled={!editMode} valueKey="name" labelKey="displayName"/>
                    </div>
                    <div className="col-sm-3">
                      <input name="outputFieldName" value={obj.outputFieldName} ref="outputFieldName" onChange={this.handleFieldChange.bind(this, 'outputFieldName', i)} type="text" className="form-control" required={true} disabled={!editMode}/>
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
        </Scrollbars>
      </div>
    );
  }
}

WindowingAggregateNodeForm.contextTypes = {
  ParentForm: React.PropTypes.object
};
