import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Tabs, Tab, Radio} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import CustomProcessorREST from '../../../rest/CustomProcessorREST';
import { Scrollbars } from 'react-custom-scrollbars';

export default class CustomNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object.isRequired,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
		sourceNode: PropTypes.object.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
		let {configData, editMode} = props;
        this.customConfig = configData.topologyComponentUISpecification.fields;
		let id = _.find(this.customConfig, {fieldName: "name"}).defaultValue;
		let parallelism = _.find(this.customConfig, {fieldName: "parallelism"}).defaultValue;
		this.fetchData(id, parallelism);

		var obj = {
			editMode: editMode,
			showSchema: true,
			userInputs: [],
			showError: false,
			showErrorLabel: false
		};

		this.customConfig.map((o)=>{
			if(o.type === "boolean")
				obj[o.fieldName] = o.defaultValue;
			else obj[o.fieldName] = o.defaultValue ? o.defaultValue : '';
			if(o.isUserInput)
				obj.userInputs.push(o);
		});
		this.state = obj;
	}

	fetchData(id, defaultParallelism) {
                let {topologyId, nodeType, nodeData, versionId} = this.props;
		let promiseArr = [
			CustomProcessorREST.getProcessor(id),
                        TopologyREST.getNode(topologyId, versionId, nodeType, nodeData.nodeId)
		];

		Promise.all(promiseArr)
			.then((results)=>{
				let {name, description, customProcessorImpl, imageFileName,	jarFileName,
					inputSchema, outputStreamToSchema, configFields} = results[0].entities[0];

                                this.nodeData = results[1];
                                let properties = results[1].config.properties;
				if(!properties.parallelism) properties.parallelism=defaultParallelism;

				let stateObj = {
					parallelism: properties.parallelism,
					localJarPath: properties.localJarPath,
					name: name,
					description: description,
					customProcessorImpl: customProcessorImpl,
					imageFileName: imageFileName,
					jarFileName: jarFileName,
					inputSchema: inputSchema,
					outputStreamToSchema: outputStreamToSchema
				};

				this.state.userInputs.map((i)=>{
					if(i.type === "boolean")
						stateObj[i.fieldName] = (properties[i.fieldName]) === true ? true : false;
					else
						stateObj[i.fieldName] = properties[i.fieldName] ? properties[i.fieldName] : '';
				});

				if(this.nodeData.outputStreams.length === 0)
					this.saveStreams(outputStreamToSchema);
                else this.context.ParentForm.setState({outputStreamObj: this.nodeData.outputStreams[0]});

				this.setState(stateObj);
			})
	}

	saveStreams(outputStreamToSchema){
		let self = this;
                let {topologyId, nodeType, versionId} = this.props;
		let streamIds = _.keys(outputStreamToSchema),
			streamData = {},
			streams = [],
			promiseArr = [];

		streamIds.map((s)=>{
			streams.push({
				streamId: s,
				fields: outputStreamToSchema[s].fields
			});
		});

		streams.map((s)=>{
                        promiseArr.push(TopologyREST.createNode(topologyId, versionId, 'streams', {body: JSON.stringify(s)}));
		});

		Promise.all(promiseArr)
			.then(results=>{
				self.nodeData.outputStreamIds = [];
				results.map(result=>{
                                                self.nodeData.outputStreamIds.push(result.id);
					})
                                TopologyREST.updateNode(topologyId, versionId, nodeType, self.nodeData.id, {body: JSON.stringify(this.nodeData)})
					.then((node)=>{
                                                self.nodeData = node;
						self.setState({showSchema: true});
                        this.context.ParentForm.setState({outputStreamObj:node.outputStreams[0]})
					})
			})
	}

	handleValueChange(fieldObj, e) {
		let obj = {
			showError: true,
			showErrorLabel: false
		};
		obj[e.target.name] = e.target.type === "number" && e.target.value !== '' ? Math.abs(e.target.value) : e.target.value;
		if(!fieldObj.isOptional) {
			if(e.target.value === '') fieldObj.isInvalid = true;
			else delete fieldObj.isInvalid;
		}
		this.setState(obj);
	}

	handleRadioBtn(e) {
		let obj = {};
		obj[e.target.dataset.name] = e.target.dataset.label === "true" ? true : false;
		this.setState(obj);
	}

	getData() {
		let obj = {},
		customConfig = this.customConfig;

		customConfig.map((o)=>{
			obj[o.fieldName] = this.state[o.fieldName];
		});
		return obj;
	}

	validateData(){
		let validDataFlag = true;

		this.state.userInputs.map((o)=>{
                        if(!o.isOptional && this.state[o.fieldName] === '') {
				validDataFlag = false;
				o.isInvalid = true;
			}
		});
		if(!validDataFlag)
			this.setState({showError: true, showErrorLabel: true});
		else this.setState({showErrorLabel: false});
		return validDataFlag;
	}

        handleSave(name, description){
		let {topologyId, nodeType, versionId} = this.props;
		let data = this.getData();
		let nodeId = this.nodeData.id;
		this.nodeData.config.properties = data;
		this.nodeData.name = name;
                this.nodeData.description = description;

                return TopologyREST.updateNode(topologyId, versionId, nodeType, nodeId, {body: JSON.stringify(this.nodeData)})
	}

	render() {
		let {topologyId, editMode, nodeType, nodeData, targetNodes, linkShuffleOptions} = this.props;
		let {showSchema, showError, showErrorLabel} = this.state;
		return (
            <div className="modal-form processor-modal-form">
              <Scrollbars autoHide
                renderThumbHorizontal={props => <div {...props} style={{display : "none"}}/>}
                >
                <form className="customFormClass">
                    {
                        this.state.userInputs.map((f, i)=>{
                            return (
                                <div className="form-group" key={i}>
                                    <label>{f.uiName}
                                        {f.isOptional ? null : <span className="text-danger">*</span>}
                                    </label>
                                    <div>
                                    {
                                        f.type === "boolean" ?
                                            [<Radio
                                                key="1"
                                                inline={true}
                                                data-label="true"
                                                data-name={f.fieldName}
                                                onChange={this.handleRadioBtn.bind(this)}
                                                checked={this.state[f.fieldName] ? true: false}
                                                disabled={!this.state.editMode}>true
                                            </Radio>,
                                            <Radio
                                                key="2"
                                                inline={true}
                                                data-label="false"
                                                data-name={f.name}
                                                onChange={this.handleRadioBtn.bind(this)}
                                                checked={this.state[f.fieldName] ? false : true}
                                                disabled={!this.state.editMode}>false
                                            </Radio>]
                                        :
                                        <input
                                            name={f.fieldName}
                                            value={this.state[f.fieldName]}
                                            onChange={this.handleValueChange.bind(this, f)}
                                            type={f.type}
                                            className={!f.isOptional && showError && f.isInvalid ? "form-control invalidInput" : "form-control"}
                                            required={f.isOptional ? false : true}
                                            disabled={!this.state.editMode}
                                            min={f.type === "number" ? "0" : null}
                                            inputMode={f.type === "number" ? "numeric" : null}
                                        />
                                    }
                                    </div>
                                </div>
                            );
                        })
                    }
                </form>
              </Scrollbars>
          </div>
        )
	}
}
CustomNodeForm.contextTypes = {
    ParentForm: React.PropTypes.object,
};
