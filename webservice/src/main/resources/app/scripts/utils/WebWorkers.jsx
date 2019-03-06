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

const resolves = {};
const rejects = {};
let globalMsgId = 0;

class WebWorkers {
  constructor(file) {
    this.worker = new Worker (window.URL.createObjectURL(new Blob([file])));
    this.worker.onmessage = this.handleMsg;
  }

  startWorkers = (str) => {
    return this.sendMsg(str, this.worker);
  }

  handleMsg = (msg) => {
    const {id, err, payload} = msg.data;
    if (payload) {
      const resolve = resolves[id];
      if (resolve) {
        resolve(msg.data);
      }
    } else {
      // error condition
      const reject = rejects[id];
      if (reject) {
        if (err) {
          reject(err);
        } else {
          reject('Got nothing');
        }
      }
    }

    // delete used callbacks
    delete resolves[id];
    delete rejects[id];
  }

  sendMsg = (payload, worker) => {
    const msgId = globalMsgId++;
    const msg = {
      id: msgId,
      payload
    };
    return new Promise(function (resolve, reject) {
      // save callbacks for later
      resolves[msgId] = resolve;
      rejects[msgId] = reject;
      worker.postMessage(msg);
    });
  }
}

export default WebWorkers;

// Example for onmessage listener function
// self.onmessage = function(msg) {
//   const {id, payload} = msg.data
//   self.validator(payload,function(err,result){
//     const msg = {
//       id,
//       payload: result
//     };
//     if(err){
//       msg.err = err;
//     }
//     self.postMessage(msg);
//   });
// }

// Example for validator function
// function validator(data,cb){
//   try{
//     eval('('+ data +')');
//     cb(null, data);
//   }catch(err){
//     cb(err.message, data);
//   }
// }
