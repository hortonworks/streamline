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

const baseUrl = "/api/v1/catalog/";
const pageSize = 25;
const ItemTypes = {
  ComponentNodes: 'box',
  Nodes: 'node'
};
const notifyTextLimit = 90;
const toastOpt = {
  timeOut: 0,
  closeButton: true,
  tapToDismiss: false,
  extendedTimeOut: 0
};
const PieChartColor = ["#006ea0", "#77b0bd", "#b7cfdb", "#9dd1e9"];
let deleteNodeIdArr = [];

const deployStatusText = {
  DEPLOYING_TOPOLOGY : 'DEPLOYING TOPOLOGY',
  TOPOLOGY_STATE_EXTRA_JARS_SETUP : 'TOPOLOGY STATE EXTRA JARS SETUP',
  TOPOLOGY_STATE_CLUSTER_ARTIFACTS_SETUP : 'TOPOLOGY STATE CLUSTER ARTIFACTS SETUP',
  TOPOLOGY_STATE_DEPLOYED : 'TOPOLOGY STATE DEPLOYED',
  TOPOLOGY_STATE_SUSPENDED : 'TOPOLOGY STATE SUSPENDED',
  TOPOLOGY_STATE_DEPLOYMENT_FAILED : 'TOPOLOGY STATE DEPLOYMENT FAILED'
};

export {
  baseUrl,
  pageSize,
  ItemTypes,
  notifyTextLimit,
  toastOpt,
  PieChartColor,
  deleteNodeIdArr,
  deployStatusText
};
