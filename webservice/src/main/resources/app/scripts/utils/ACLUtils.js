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
import app_state from '../app_state';

const hasModuleAccess = function(name){
  if(!app_state.streamline_config.secureMode){
    return true;
  }
  let hasAccess = false;
  app_state.roleInfo.forEach((role)=>{
    const {menu} = role.metadata;
    const index = menu.indexOf(name);
    if(index > -1 && !hasAccess){
      hasAccess = true;
    }
  });
  return hasAccess;
};

const hasCapability = function(module, type){
  if(!app_state.streamline_config.secureMode){
    return true;
  }
  let hasCapability = false;
  app_state.roleInfo.forEach((role)=>{
    const {capabilities} = role.metadata;
    let obj = _.find(capabilities, (cap) => {
      return cap[module];
    });
    if(obj && type.toLowerCase() == obj[module].toString().toLowerCase()){
      hasCapability = true;
    }
  });
  return hasCapability;
};

const hasEditCapability = function(module){
  return hasCapability(module, 'Edit');
};

const hasViewCapability = function(module){
  return hasCapability(module, 'View');
};



export {
  hasModuleAccess,
  hasEditCapability,
  hasViewCapability
};
