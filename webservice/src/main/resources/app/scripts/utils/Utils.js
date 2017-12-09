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

import React from 'react';
import _ from 'lodash';
import moment from 'moment';
import * as Fields from '../libs/form/Fields';

const sortArray = function(sortingArr, keyName, ascendingFlag) {
  if (ascendingFlag) {
    return sortingArr.sort(function(a, b) {
      if (a[keyName] < b[keyName]){
        return -1;
      }
      if (a[keyName] > b[keyName]){
        return 1;
      }
      return 0;
    });
  } else {
    return sortingArr.sort(function(a, b) {
      if (b[keyName] < a[keyName]){
        return -1;
      }
      if (b[keyName] > a[keyName]){
        return 1;
      }
      return 0;
    });
  }
};

const numberToMilliseconds = function(number, type) {
  if (type === 'Seconds') {
    return number * 1000;
  } else if (type === 'Minutes') {
    return number * 60000;
  } else if (type === 'Hours') {
    return number * 3600000;
  }
};

const millisecondsToNumber = function(number) {
  let hours = (number / (1000 * 60 * 60)) % 24;
  let minutes = (number / (1000 * 60)) % 60;
  let seconds = (number / (1000)) % 60;
  if (hours % 1 === 0) {
    return {
      number: (number / (1000 * 60 * 60)),
      type: 'Hours'
    };
  } else if (minutes % 1 === 0) {
    return {
      number: (number / (1000 * 60)),
      type: 'Minutes'
    };
  } else if (seconds % 1 === 0) {
    return {
      number: (number / (1000)),
      type: 'Seconds'
    };
  } else {
    console.error("Something went wrong in converting millseconds to proper format");
  }
};

const capitaliseFirstLetter = function(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
};

const splitTimeStamp = function(date) {
  const currentDT = moment(new Date());
  const createdDT = moment(date);
  const dateObj = moment.duration(currentDT.diff(createdDT));
  return ((dateObj._data.days === 0)
    ? ''
    : dateObj._data.days + 'd ') + ((dateObj._data.days === 0 && dateObj._data.hours === 0)
    ? ''
    : dateObj._data.hours + 'h ') + ((dateObj._data.days === 0 && dateObj._data.hours === 0 && dateObj._data.minutes === 0)
    ? ''
    : dateObj._data.minutes + 'm ') + dateObj._data.seconds + 's ago';
};

const splitSeconds = function(sec_num) {
  let days = Math.floor(sec_num / (3600 * 24));
  let hours = Math.floor((sec_num - (days * (3600 * 24))) / 3600);
  let minutes = Math.floor((sec_num - (days * (3600 * 24)) - (hours * 3600)) / 60);
  let seconds = Math.floor(sec_num - (days * (3600 * 24)) - (hours * 3600) - (minutes * 60));

  if (hours < 10) {
    hours = "0" + hours;
  }
  if (minutes < 10) {
    minutes = "0" + minutes;
  }
  if (seconds < 10) {
    seconds = "0" + seconds;
  }

  return ((days === 0)
    ? ''
    : days + 'd ') + ((days === 0 && (hours == "00" || 0))
    ? ''
    : hours + 'h ') + ((days === 0 && (hours == "00" || 0) && minutes === 0)
    ? ''
    : minutes + 'm ') + seconds + 's ago';
};

const filterByName = function(entities, filterValue) {
  let matchFilter = new RegExp(filterValue, 'i');
  return entities.filter(filteredList => !filterValue || matchFilter.test(filteredList.name));
};

const ellipses = function(string, len) {
  if (!string) {
    return;
  }
  const str = string.substr(0, len || 10); // default 10 character...
  return (string.length > len)
    ? `${str}...`
    : str;
};

const sortByKey = function(string) {
  switch (string) {
  case "last_updated":
    return "Last Updated";
    break;
  case "name":
    return "Name";
    break;
  case "status":
    return "Status";
    break;
  default:
    return "Last Updated";
  }
};

const secToMinConverter = function(milliseconds, src) {
  milliseconds = (!milliseconds)
    ? 0
    : milliseconds;
  let hours = milliseconds / (1000 * 60 * 60);
  let absoluteHours = Math.floor(hours);
  let f_hours = absoluteHours > 9
    ? absoluteHours
    : 0 + absoluteHours;

  //Get remainder from hours and convert to minutes
  let minutes = (hours - absoluteHours) * 60;
  let absoluteMinutes = Math.floor(minutes);
  let f_mins = absoluteMinutes > 9
    ? absoluteMinutes
    : 0 + absoluteMinutes;

  //Get remainder from minutes and convert to seconds
  let seconds = (minutes - absoluteMinutes) * 60;
  let absoluteSeconds = Math.floor(seconds);
  let f_secs = absoluteSeconds > 9
    ? absoluteSeconds
    : 0 + absoluteSeconds;

  (f_hours !== 0)
    ? milliseconds = (src === "list")
      ? _.round(f_hours + "." + f_mins) + " hours"
      : _.round(f_hours + "." + f_mins) + "/hours"
    : (f_mins !== 0 && f_secs !== 0)
      ? milliseconds = (src === "list")
        ? _.round(f_mins + "." + f_secs) + " mins"
        : _.round(f_mins + "." + f_secs) + "/mins"
      : milliseconds = (src === "list")
        ? _.round(f_secs) + " sec"
        : _.round(f_secs) + "/sec";
  return milliseconds;
};

const kFormatter = function(num) {
  num = (!num)
    ? 0
    : num;
  return num > 999 || num < -999
    ? (num / 1000).toFixed(1) + 'k'
    : num.toFixed(1);
};

const abbreviateNumber = function(value) {
  var newValue = value || 0;var suffix = "";
  if (newValue >= 1000 || newValue <= -1000) {
    var suffixes = ["", "k", "m", "b","t"];
    var suffixNum = Math.floor( (""+newValue).length/3 );
    var shortValue = '';
    for (var precision = 2; precision >= 1; precision--) {
      shortValue = parseFloat( (suffixNum != 0 ? (newValue / Math.pow(1000,suffixNum) ) : newValue).toPrecision(precision));
      var dotLessShortValue = (shortValue + '').replace(/[^a-zA-Z 0-9]+/g,'');
      if (dotLessShortValue.length <= 2) { break; }
    }
    if (shortValue % 1 != 0)  {shortValue = shortValue.toFixed(1);}
    newValue = shortValue;
    suffix = suffixes[suffixNum];
  } else {
    newValue = newValue.toFixed(1);
  }
  return {value: newValue, suffix: suffix};
};

const eventTimeData = function(inputFields) {
  const eventTimeArr = inputFields.filter((k, i) => {
    return k.type.toLowerCase() === "long";
  }).map((v) => {
    return {fieldName: v.name, uiName: v.name};
  });
  eventTimeArr.push({fieldName: "processingTime", uiName: "processingTime"});
  return eventTimeArr;
};

// gets difference of minutes between two dates excluding the seconds
const getTimeDiffInMinutes = function(end, start) {
  return  moment.duration(end.diff(start, 'minutes')).asMinutes();
};

const inputFieldsData = function(inputFields) {
  const inputFieldsArr = inputFields.map(v => {
    return {fieldName: v.name, uiName: v.name, fieldType: v.type};
  });
  return inputFieldsArr;
};

const checkNestedInputFields = function(inputObj, fieldsData, securityType, hasSecurity) {
  // if the inputObj doesn't have options and hint the inputObj return it self
  if (!inputObj.options && inputObj.hint === undefined) {
    return inputObj;
  }
  const populateFields = (obj) => {
    if (obj.options && obj.hint === undefined) {
      obj.options.map((x) => {
        if (x.fields) {
          x.fields.map((k) => {
            populateFields(k);
          });
        }
      });
      return obj;
    } else {
      //InputObj which have options and hint of "inputfields" OR "eventtime" OR "override"
      //those fields are mapped by inputFieldsData function
      if (obj.options && obj.hint !== undefined) {
        if (obj.hint.toLowerCase().indexOf("inputfields") !== -1 && !obj.options.length) {
          obj.options = inputFieldsData(fieldsData);
        } else if (obj.hint.toLowerCase().indexOf("eventtime") !== -1 && (obj.options.length === 0 || obj.options[0].uiName === "processingTime")) {
          obj.options = eventTimeData(fieldsData);
        } else if (obj.hint.toLowerCase().indexOf("override") !== -1 && obj.type === "enumstring") {
          obj.type = "creatableField";
        }
      }
      obj.validators = [];
      if(obj.hint !== undefined && hasSecurity) {
        if(securityType.indexOf('SASL') > -1 && obj.hint.toLowerCase().indexOf('security_kerberos_required') !== -1) {
          obj.isOptional = false;
          obj.validators.push("required");
        }
        if(securityType.indexOf('SSL') > -1 && obj.hint.toLowerCase().indexOf('security_ssl_required') !== -1) {
          obj.isOptional = false;
          obj.validators.push("required");
        }
      }
      return obj;
    }
  };
  return populateFields(JSON.parse(JSON.stringify(inputObj)));
};

const genFields = function(fieldsJSON, _fieldName = [], FormData = {}, inputFields = [], securityType, hasSecurity) {
  const fields = [];
  fieldsJSON.forEach((d, i) => {
    d = checkNestedInputFields(d, inputFields, securityType, hasSecurity);
    const Comp = Fields[d.type.split('.').join('')] || null;
    let _name = [
      ..._fieldName,
      d.fieldName
    ];
    if (Comp) {
      let children = null;
      if (d.fields && d.type != 'array.object' && d.type != 'array.enumobject') {
        const _FormData = FormData[d.fieldName] = FormData[d.fieldName]
          ? FormData[d.fieldName]
          : {};
        children = genFields(d.fields, _name, _FormData);
      }
      if (d.defaultValue != null) {
        if (d.type == 'enumobject') {
          FormData[d.fieldName] = FormData[d.fieldName] || {
            [d.defaultValue]: {}
          };
        } else {
          FormData[d.fieldName] = FormData[d.fieldName] != undefined
            ? FormData[d.fieldName]
            : d.defaultValue;
        }
      }
      const options = [];
      if (d.options) {
        d.options.forEach((d) => {
          if (!_.isObject(d)) {
            options.push({value: d, label: d});
          } else {
            options.push({value: d.fieldName, label: d.uiName, type: d.fieldType});
          }
        });
      }
      let validators = [];
      if (!d.isOptional) {
        validators.push('required');
      }
      if (d.hint !== undefined && d.hint.toLowerCase().indexOf("email") !== -1) {
        validators.push('email');
      }
      fields.push(
        <Comp label={d.uiName} _ref={d.fieldName} value={d.fieldName/*_name.join('.')*/} valuePath={_name.join('.')} key={_name.join('.')} validation={validators} fieldAttr={{
          options: options
        }} fieldJson={d}>{children}</Comp>
      );
    }
  });
  return fields;
};

const scrollMe = function(element, to, duration) {
  if (duration <= 0) {
    return;
  }
  let difference = to - element.scrollTop;
  let perTick = difference / duration * 10;

  const timer = setTimeout(function() {
    element.scrollTop = element.scrollTop + perTick;
    if (element.scrollTop == to) {
      clearTimeout(timer);
      return;
    }
    scrollMe(element, to, duration - 10);
  }, 10);
};

const validateURL = function(url) {
  let URL = url.toLowerCase(),
    result = false;
  const matchStr = "/api/v1/clusters/";
  if (URL.indexOf(matchStr) !== -1) {
    let str = URL.substr((URL.indexOf(matchStr) + matchStr.length), URL.length);
    if (/^[a-zA-Z0-9_.-]*$/.test(str)) {
      return result = true;
    }
  }
  return result;
};

const convertMillsecondsToSecond = function(milliSec) {
  return Math.round(milliSec / 1000);
};

const formatLatency = function (milliSec) {
  var val = milliSec || 0, suffix = 'ms';
  if(val >= 1000) {
    val = val / 1000;
    suffix = 'sec';
    if(val >= 60) {
      val = val / 60;
      suffix = 'min';
      if(val >= 60) {
        val = val / 60;
        suffix = 'h';
      }
    }
  }
  return {value: val.toFixed(1), suffix: suffix};
};

const validateJSON = function(json){
  let validFlag = true;
  try{
    const data = JSON.parse(json);
  }catch(err){
    validFlag = false;
  }
  return validFlag;
};

const isMergeableObject = function (val) {
  let nonNullObject = val && typeof val === 'object';

  return nonNullObject
    && Object.prototype.toString.call(val) !== '[object RegExp]'
    && Object.prototype.toString.call(val) !== '[object Date]';
};

const emptyTarget = function (val) {
  return Array.isArray(val) ? [] : {};
};

const cloneIfNecessary = function (value, optionsArgument) {
  let clone = optionsArgument && optionsArgument.clone === true;
  return (clone && isMergeableObject(value)) ? deepmerge(emptyTarget(value), value, optionsArgument) : value;
};

const defaultArrayMerge = function (target, source, optionsArgument) {
  let destination = target.slice();
  source.forEach(function(e, i) {
    if (typeof destination[i] === 'undefined') {
      destination[i] = cloneIfNecessary(e, optionsArgument);
    } else if (isMergeableObject(e)) {
      destination[i] = deepmerge(target[i], e, optionsArgument);
    } else if (target.indexOf(e) === -1) {
      destination.push(cloneIfNecessary(e, optionsArgument));
    }
  });
  return destination;
};

const mergeObject = function (target, source, optionsArgument) {
  let destination = {};
  if (isMergeableObject(target)) {
    Object.keys(target).forEach(function (key) {
      destination[key] = cloneIfNecessary(target[key], optionsArgument);
    });
  }
  Object.keys(source).forEach(function (key) {
    if (!isMergeableObject(source[key]) || !target[key]) {
      destination[key] = cloneIfNecessary(source[key], optionsArgument);
    } else {
      destination[key] = deepmerge(target[key], source[key], optionsArgument);
    }
  });
  return destination;
};

const deepmerge = function (target, source, optionsArgument) {
  let array = Array.isArray(source);
  let options = optionsArgument || { arrayMerge: defaultArrayMerge };
  let arrayMerge = options.arrayMerge || defaultArrayMerge;

  if (array) {
    return Array.isArray(target) ? arrayMerge(target, source, optionsArgument) : cloneIfNecessary(source, optionsArgument);
  } else {
    return mergeObject(target, source, optionsArgument);
  }
};

const deepmergeAll = function deepmergeAll(array, optionsArgument) {
  if (!Array.isArray(array) || array.length < 2) {
    throw new Error('first argument should be an array with at least two elements');
  }

  // we are sure there are at least 2 values, so it is safe to have no initial value
  return array.reduce(function(prev, next) {
    return deepmerge(prev, next, optionsArgument);
  });
};

const mergeFormDataFields = function(name, clusterArr,clusterName,formData,uiSpecification){
  let data = {},
    obj = [],securityKey='';
  let config = uiSpecification;
  _.keys(clusterArr).map((x) => {
    if (name || clusterName === x) {
      const nestedFields = function(configList){
        obj = configList.map((list) => {
          if(list.fields){
            nestedFields(list.fields);
          }
          _.keys(clusterArr[x].hints).map(k => {
            const nestedKeys = function(pk){
              if (pk.indexOf('.') !== -1) {
                let mk = pk.split('.');
                mk.length > 1 ? mk.splice(0, 1) : '' ;
                nestedKeys(mk.join('.'));
              } else if (list.fieldName === pk) {
                if((clusterArr[x].hints[k] === "true" || clusterArr[x].hints[k] === "false") && (name || clusterName) === x){
                  let fieldValue = clusterArr[x].hints[k];
                  _.set(data,k, (fieldValue === "true" ? true : false));
                } else if (_.isArray(clusterArr[x].hints[k]) && (name || clusterName) === x) {
                  list.options = clusterArr[x].hints[k].map(v => {
                    return {fieldName: v, uiName: v};
                  });
                  if (list.hint && list.hint.toLowerCase().indexOf("override") !== -1) {
                    if (formData[k]) {
                      if (list.options.findIndex((o) => {
                        return o.fieldName == formData[k];
                      }) == -1) {
                        list.options.push({fieldName: formData[k], uiName: formData[k]});
                      }
                    }
                  }
                  // auto select fields if options length is one only
                  // if(list.options && list.options.length === 1){
                  //   formData[k] = list.options[0].uiName;
                  // } else if(list.options && list.options.length > 1){
                  //   if(formData[k] !== undefined && formData[k] !== ''){
                  //     const obj = _.find(list.options, (l) => {return l.uiName === formData[k];});
                  //     obj === undefined &&  _.isEmpty(obj)
                  //     ? formData[k] = ''
                  //     : '';
                  //   }
                  // }
                } else {
                  if (!_.isArray(clusterArr[x].hints[k])) {
                    // if (!formData[k]) this means it has come first time
                    // OR
                    // if (!name) this means user had change the cluster name
                    if (!formData[k] || !name) {
                      let _fieldValue = _.get(formData,k) || clusterArr[x].hints[k];
                      if(Object.prototype.toString.call(clusterArr[x].hints[k]) === "[object Object]" && list.hint.indexOf('dependsOn-') !== -1){
                        const key = list.hint.split('-')[1];
                        _fieldValue = k === key ? formData[key] || '' : clusterArr[x].hints[k][formData[key]];
                      } else if(k.indexOf('.') !== -1){
                        if(formData[k] === undefined && clusterName !== undefined && !_.isEmpty(clusterName)){
                          _fieldValue = _.get(clusterArr[x].hints,k);
                        }
                      }
                      let val = _fieldValue;
                      if(val === ''){
                        val = _.get(formData, k);
                      }
                      // only set the value if name || clusterName is same as 'x'
                      (name || clusterName) === x ? _.set(data,k,val) : null;
                    }
                  }
                }
              }
            };
            nestedKeys(k);
          });
          data.cluster = clusterArr[name || clusterName] === undefined ? '' : name || clusterName;
          data.clusters = clusterArr[name || clusterName] === undefined ? '' : clusterArr[name || clusterName].cluster.name;
          return list;
        });
      };
      nestedFields(config);
    }
  });
  const tempData = this.deepmerge(formData,data);
  return {obj,tempData};
};

const handleNestedFormDataEmptyObj = (formData) => {
  let tempData = _.cloneDeep(formData);
  const nestedObj = function(data){
    _.map(_.keys(data), (key) => {
      if(_.isObject(data[key])){
        nestedObj(data[key]);
      } else if (_.isArray(data[key])){
        if(data[key].length === 0){
          delete data[key];
        }
      } else {
        if(data[key] === undefined || data[key] === ''){
          delete data[key];
        }
      }
    });
    return data;
  };
  return nestedObj(tempData);
};

const eventLogNumberId = function(num){
  if(!num){
    return;
  }
  let numString = num.toString();
  numString =  numString.length === 1 ? `00${numString}` : numString.length === 2 ? `0${numString}` : numString;
  return numString;
};

const checkTypeAndReturnValue = function(data){
  if(typeof(data) === "boolean"){
    data = data.toString();
  }
  if(!data){
    return;
  }
  if(data){
    let exp = /[,:{}]/g ,valideJson = '';
    exp.test(data)
    ? valideJson = validateJSON(data)
    : valideJson = false;

    if(!valideJson){
      const checkNaN = Number(data);
      if(isNaN(data)){
        return data === "true"
              ? true
              : data === "false"
                ? false
                : data;
      } else {
        return Number(data);
      }
    } else {
      return JSON.parse(data);
    }
  }
};

const mapSecurityProtocol = function(clusterName,securityKey,formData,clusterArr){
  let tempFormData = _.cloneDeep(formData);
  const bootstrapServerObj = _.find(clusterArr, (c) => {
    return c.cluster.id === Number(clusterName || tempFormData.cluster);
  });
  tempFormData["bootstrapServers"] = bootstrapServerObj.hints["bootstrapServers"][securityKey];
  return tempFormData;
};

const checkWhiteSpace = function(str){
  if(!str || str === ''){
    return;
  }
  return  /\s/g.test(str);
};

const segregateVersions = function(versionsArr){
  let deploy_v={},allVersion=versionsArr;
  let current_v = _.find(versionsArr, (v) => {return v.name === "CURRENT";});
  const tempVer = _.filter(versionsArr, (ver) => {return ver.name !== "CURRENT";});
  _.map(tempVer, (t,i) => {
    const v = t.name.split('V');
    const len = tempVer.length > 1 ? tempVer.length - 1 : 1;
    if(Number(v[1]) === len ){
      deploy_v = t;
    }
  });
  return {current_v,deploy_v,allVersion};
};

const checkStatus = function (response) {
  if (response.status >= 200 && response.status < 300) {
    return response.json();
  } else {
    const error = new Error(response.statusText);
    error.response = response.json();
    throw error;
  }
};

const checkStatusForResponseText = function (response) {
  if (response.status >= 200 && response.status < 300) {
    return response.json();
  } else {
    const error = new Error(response.statusText);
    error.response = response.text();
    throw error;
  }
};

const getClusterKey = function(urlOrName, isManualCluster,clusterArr) {
  let key = '';
  _.keys(clusterArr).map(x => {
    _.keys(clusterArr[x]).map(k => {
      if(!isManualCluster && clusterArr[x][k].ambariImportUrl === urlOrName){
        key = x;
      } else if(isManualCluster && clusterArr[x][k].name === urlOrName){
        key = x;
      }
    });
  });
  return key;
};

const clusterField = function(){
  return {
    "uiName": "Cluster Name",
    "fieldName": "clusters",
    "isOptional": false,
    "tooltip": "Cluster name to read data from",
    "type": "CustomEnumstring",
    "options": []
  };
};

const matchStringInArr = function(stringArr,str){
  let match=false;
  _.map(stringArr.split(','), (s) => {
    if(s === str){
      match = true;
    }
  });
  return match;
};

const populateSchemaVersionOptions = function(resultArr,tempConfig){
  const versionOptions = _.map(resultArr.entities, (result) => {
    return {
      fieldName : result,
      uiName : result
    };
  });
  _.map(tempConfig, (config) => {
    if(config.hint !== undefined && config.hint.indexOf('schemaVersion') !== -1){
      config.options = versionOptions;
    }
  });
  return tempConfig;
};

const validateReconfigFlag = function(arr){
  let errorArr=[];
  _.map(arr,(r) => {
    if(r.reconfigure){
      errorArr.push(true);
    }
  });
  return errorArr.length ? false : true;
};

const noSpecialCharString = function(string){
  if(string === ''){
    return;
  }
  return /^[a-zA-Z0-9_-]+$/.test(string);
};

export default {
  sortArray,
  numberToMilliseconds,
  millisecondsToNumber,
  capitaliseFirstLetter,
  splitTimeStamp,
  splitSeconds,
  filterByName,
  ellipses,
  sortByKey,
  secToMinConverter,
  genFields,
  kFormatter,
  scrollMe,
  validateURL,
  convertMillsecondsToSecond,
  validateJSON,
  isMergeableObject,
  emptyTarget,
  cloneIfNecessary,
  defaultArrayMerge,
  mergeObject,
  deepmerge,
  deepmergeAll,
  mergeFormDataFields,
  handleNestedFormDataEmptyObj,
  eventLogNumberId,
  checkTypeAndReturnValue,
  mapSecurityProtocol,
  checkWhiteSpace,
  segregateVersions,
  checkStatus,
  checkStatusForResponseText,
  getClusterKey,
  clusterField,
  matchStringInArr,
  populateSchemaVersionOptions,
  validateReconfigFlag,
  noSpecialCharString,
  getTimeDiffInMinutes,
  abbreviateNumber,
  formatLatency
};
