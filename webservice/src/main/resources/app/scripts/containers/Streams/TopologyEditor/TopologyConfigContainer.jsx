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
            formField: {}
        }
        this.fetchData();
    }

    fetchData = () => {
        const {topologyId, versionId} = this.props;
        let promiseArr = [
            TopologyREST.getTopologyConfig(),
            TopologyREST.getTopology(topologyId, versionId)
        ]
        Promise.all(promiseArr)
          .then( result => {
            const formField = result[0].entities[0].topologyComponentUISpecification;
            const config = result[1].topology.config;
            this.namespaceId = result[1].topology.namespaceId;
            this.setState({formData : JSON.parse(config), formField : formField})
          }).catch(err => {
            FSReactToastr.error(<CommonNotification flag="error" content={err.message}/>, '', toastOpt)
          })
    }

    validate(){
        let validDataFlag = true;
        if(!this.refs.Form.validate()){
            validDataFlag = false;
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
        const {formData,formField} = this.state;
        let fields = Utils.genFields(formField.fields || [], [], formData);

        return(
            <div>
                <Form
                    ref="Form"
                    FormData={formData}
                    showRequired={null}
                    className="modal-form config-modal-form"
                >
                    {fields}
                </Form>
            </div>
        )
    }
}
