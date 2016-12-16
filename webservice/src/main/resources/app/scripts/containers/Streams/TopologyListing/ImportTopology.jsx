import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';

/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import EnvironmentREST from '../../../rest/EnvironmentREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import Form from '../../../libs/form';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';

class ImportTopology extends Component{
  constructor(props){
    super(props)
    this.state = {
      jsonFile: null,
      namespaceId: '',
      namespaceOptions: [],
      validInput : true,
      validSelect: true,
      showRequired : true
    }
    this.fetchData();
  }

  fetchData = () => {
    let promiseArr = [
      EnvironmentREST.getAllNameSpaces()
    ];
    Promise.all(promiseArr)
    .then(result => {
      if(result[0].responseMessage !== undefined) {
          FSReactToastr.error(
              <CommonNotification flag="error" content={result[0].responseMessage}/>, '', toastOpt)
      } else {
        const resultSet = result[0].entities;
        let namespaces = []
        resultSet.map((e)=>{
          namespaces.push(e.namespace);
        });
        this.setState({namespaceOptions: namespaces});
      }
    })
  }

  validate() {
    const {jsonFile, namespaceId} = this.state;
    let validDataFlag = true;
    if(!jsonFile){
      validDataFlag = false;
      this.setState({validInput: false});
    } else if(namespaceId === ''){
      validDataFlag = false;
      this.setState({validSelect: false});
    } else{
      validDataFlag = true
      this.setState({validInput : true, validSelect: true});
    }
    return validDataFlag;
  }

  handleSave = () => {
    if(!this.validate())
      return;
    const {jsonFile, namespaceId} = this.state;
    let formData = new FormData();
    formData.append('file', jsonFile);
    formData.append('namespaceId', namespaceId);

    return TopologyREST.importTopology({body:formData})
  }
  handleOnFileChange = (e) => {
    if(!e.target.files.length || (e.target.files.length && e.target.files[0].name.indexOf('.json') < 0)){
      this.setState({validInput: false, jsonFile: null});
    } else {
      this.setState({jsonFile: e.target.files[0]})
    }
  }
  handleOnChangeEnvironment = (obj) => {
    if(obj) {
      this.setState({namespaceId: obj.id, validSelect: true});
    } else this.setState({namespaceId: '', validSelect: false});
  }

  render(){
    const {validInput, validSelect, showRequired, namespaceId, namespaceOptions} = this.state;

    return(
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label>Select JSON File <span className="text-danger">*</span></label>
          <div>
            <input type="file"
              className={validInput ? "form-control" : "form-control invalidInput"}
              accept=".json"
              name="files"
              title="Upload File"
              onChange={this.handleOnFileChange}
            />
          </div>
        </div>
        <div className="form-group">
          <label>Environment <span className="text-danger">*</span></label>
          <div>
            <Select
              value={namespaceId}
              options={namespaceOptions}
              onChange={this.handleOnChangeEnvironment}
              className={!validSelect ? 'invalidSelect' : ''}
              placeholder="Select Environment"
              required={true}
              clearable={false}
              labelKey="name"
              valueKey="id"
            />
          </div>
        </div>
      </div>
    )
  }
}

export default ImportTopology;
