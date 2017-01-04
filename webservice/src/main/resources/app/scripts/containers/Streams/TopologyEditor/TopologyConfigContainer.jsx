import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {Link} from 'react-router';
import TopologyREST from '../../../rest/TopologyREST';
import Utils from '../../../utils/Utils';
import Form from '../../../libs/form';
import FSReactToastr from '../../../components/FSReactToastr';
import {toastOpt} from '../../../utils/Constants';
import CommonNotification from '../../../utils/CommonNotification';

export default class TopologyConfigContainer extends Component {
    static propTypes = {
        topologyId: PropTypes.string.isRequired,
    };

    constructor(props){
        super(props)
        this.state = {
            formData: {},
            formField: {},
            fetchLoader : true
        }
        this.fetchData();
    }

    fetchData = () => {
        const {topologyId, versionId} = this.props;
        let promiseArr = [
            TopologyREST.getTopologyConfig(),
            TopologyREST.getTopologyWithoutMetrics(topologyId, versionId)
        ]
        Promise.all(promiseArr)
          .then( result => {
            const formField = result[0].entities[0].topologyComponentUISpecification;
            const config = result[1].config;
            this.namespaceId = result[1].namespaceId;
            this.setState({formData : JSON.parse(config), formField : formField,fetchLoader : false});
          }).catch(err => {
            this.setState({fetchLoader : false});
            FSReactToastr.error(<CommonNotification flag="error" content={err.message}/>, '', toastOpt)
          })
    }

    validate(){
        let validDataFlag = false;
        if(!this.state.fetchLoader){
          if(this.refs.Form.validate()){
              validDataFlag = true;
          }
        }
        return validDataFlag;
    }

    handleSave(){
        const {topologyName, topologyId, versionId} = this.props;
        let data = this.refs.Form.state.FormData;
        let dataObj = {
            name: topologyName,
            config: JSON.stringify(data),
            namespaceId: this.namespaceId
        }
        return TopologyREST.putTopology(topologyId, versionId, {body: JSON.stringify(dataObj)})
    }

    render(){
        const {formData,formField,fetchLoader} = this.state;
        let fields = Utils.genFields(formField.fields || [], [], formData);

        return(
            <div>
              {
                fetchLoader
                ? <div className="col-sm-12">
                      <div className="loading-img text-center" style={{marginTop : "150px"}}>
                          <img src="styles/img/start-loader.gif" alt="loading" />
                      </div>
                  </div>
                :  <Form
                      ref="Form"
                      FormData={formData}
                      showRequired={null}
                      className="modal-form config-modal-form"
                  >
                      {fields}
                  </Form>
              }
            </div>
        )
    }
}
