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

import _ from 'lodash';

const handleValueChange = function(event){
  let obj = {};
  let name = event.target.name;
  let value = event.target.value === ''
    ? ''
    : event.target.type !== 'number'
      ? event.target.value
      : parseInt(event.target.value, 10);
  obj[name] = value;
  if (name === 'description') {
    obj['showDescriptionError'] = (value === '');
  }
  return obj;
};

const validateName = function(name,props){
  let {rules, ruleObj} = props;
  let stateObj = {
    showInvalidName: false,
    showNameError: false
  };
  if (name === '') {
    stateObj.showNameError = true;
  } else {
    let hasRules = rules.filter((o) => {
      return (o.name === name);
    });
    if (hasRules.length === 1) {
      if (ruleObj.id) {
        if (hasRules[0].id !== ruleObj.id) {
          stateObj.showInvalidName = true;
          stateObj.showNameError = true;
        }
      } else {
        stateObj.showInvalidName = true;
        stateObj.showNameError = true;
      }
    }
  }
  return stateObj;
};

const searchSchemaForFields = function(fields,selectedFields) {
  let flag = false;
  fields.map((field) => {
    if (!flag) {
      if (field.type == 'NESTED') {
        flag = searchSchemaForFields(field.fields,selectedFields);
      } else if (selectedFields.indexOf(field.name) != -1) {
        flag = true;
      }
    }
  });
  return flag;
};

const fetchSelectedFields = function(fields,streamList,selectedList){
  let arr=[],fieldsArr = fields.split(' ');
  const nestedSelectField = (fArr,list) => {
    _.map(list,(l) => {
      const index = _.findIndex(fArr, (f) => f === l.name);
      if(index !== -1 && selectedList.indexOf(l.name) === -1){
        arr.push(l.name);
      }
      if(l.fields){
        nestedSelectField(fArr,l.fields);
      }
    });
    return arr;
  };
  return nestedSelectField(fieldsArr,streamList);
};

export default {
  handleValueChange,
  validateName,
  fetchSelectedFields,
  searchSchemaForFields
};
