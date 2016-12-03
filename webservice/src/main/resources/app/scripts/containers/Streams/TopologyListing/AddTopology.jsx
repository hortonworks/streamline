import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';

/* import common utils*/
import TopologyREST from '../../../rest/TopologyREST';
import Utils from '../../../utils/Utils';
import TopologyUtils from '../../../utils/TopologyUtils';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import Form from '../../../libs/form';

/* component import */
import BaseContainer from '../../BaseContainer';
import NoData from '../../../components/NoData';
import CommonNotification from '../../../utils/CommonNotification';

class AddTopology extends Component{
  constructor(props){
    super(props)
    this.state = {
      topologyName : '',
      validInput : true,
      formField:{},
      showRequired : true
    }
    this.fetchData();
  }

  fetchData = () => {
    TopologyREST.getTopologyConfig().then(config => {
      if (config.responseMessage !== undefined) {
        FSReactToastr.error(
            <CommonNotification flag="error" content={config.responseMessage}/>, '', toastOpt)
      } else {
        const configFields = config.entities[0].topologyComponentUISpecification;
        this.setState({formField : configFields})
      }
    }).catch(err => {
      FSReactToastr.error(
            <CommonNotification flag="error" content={err.message}/>, '', toastOpt)
    })
  }

  validate(){
    const {topologyName} = this.state;
    let validDataFlag = true;
    if(topologyName.length < 1){
      validDataFlag = false;
      this.setState({validInput : false})
    }else{
      validDataFlag = true
      this.setState({validInput : true});
    }
    return validDataFlag;
  }

  handleSave = () => {
    const {topologyName} = this.state;
    let configData = this.refs.Form.state.FormData;
    let data = {
        name: topologyName,
        config: JSON.stringify(configData)
    }
    return TopologyREST.postTopology({body: JSON.stringify(data)})
  }
  saveMetadata = (id) => {
    let metaData = {
        topologyId: id,
        data: JSON.stringify({sources: [], processors: [], sinks: []})
    }
    return TopologyREST.postMetaInfo({body: JSON.stringify(metaData)})
  }
  handleOnChange = (e) => {
    this.setState({topologyName : e.target.value.trim()})
    this.validate();
  }

  render(){
    const {formField, validInput,showRequired} = this.state;
    const formData = {}
    let fields = Utils.genFields(formField.fields || [], [], formData);

    return(
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label>Name <span className="text-danger">*</span></label>
          <div>
            <input
              type="text"
              ref={(ref) => this.nameRef = ref}
              name="topologyName"
              placeholder="Topology name"
              required="true"
              className={validInput ? "form-control" : "form-control invalidInput"}
              onKeyUp={this.handleOnChange}
              autoFocus="true"
            />
          </div>
          <Form
              ref="Form"
              FormData={formData}
              className="hidden"
          >
              {fields}
          </Form>
        </div>
      </div>
    )
  }
}

export default AddTopology;
