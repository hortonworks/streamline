import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import Select from 'react-select';
import '../../../utils/Overrides';
import SchemaREST from '../../../rest/SchemaREST';

export default class SchemaFormContainer extends Component {
        constructor(props){
                super(props);
                this.state = {
                        name: '',
                        compatibility: 'BACKWARD',
                        compatibilityArr: [
                                {value: 'BACKWARD', label: 'BACKWARD'},
                                {value: 'FORWARD', label: 'FORWARD'},
                                {value: 'FULL', label: 'FULL'},
                                {value: 'NONE', label: 'NONE'}
                        ],
                        type: 'avro',
                        schemaGroup: 'Kafka',
                        description: '',
                        showError: false,
                        showErrorLabel: false,
                        changedFields: []
                };
        }

        handleValueChange(e){
                let obj = {};
                obj[e.target.name] = e.target.value;
                this.setState(obj);
        }

        handleCompatibilityChange(obj) {
                if(obj){
                        this.setState({compatibility: obj.value});
                } else {
                        this.setState({compatibility: ''});
                }
        }

        validateData(){
                let { name, type, schemaGroup, description, changedFields } = this.state;
                if(name.trim() === '' || schemaGroup === '' || type === '' || description.trim() === '') {
                        if(name.trim() === '' && changedFields.indexOf("name") === -1)
                                changedFields.push("name");
                        if(schemaGroup.trim() === '' && changedFields.indexOf("schemaGroup") === -1)
                                changedFields.push("schemaGroup");
                        if(type.trim() === '' && changedFields.indexOf("type") === -1)
                                changedFields.push("type");
                        if(description.trim() === '' && changedFields.indexOf("description") === -1)
                                changedFields.push("description");
                        this.setState({showError: true, showErrorLabel: true, changedFields: changedFields});
                        return false;
                } else {
                        this.setState({showErrorLabel: true});
                        return true;
                }
        }

        handleSave(){
                let data = {};
                let { name, type, schemaGroup, description, compatibility } = this.state;
                data = {name, type, schemaGroup, description};
                if(compatibility !== '')
                        data.compatibility = compatibility;
                return SchemaREST.postSchema({body: JSON.stringify(data)});
        }

        render() {
                const jsonoptions = {
                        lineNumbers: true,
                        mode: "application/json",
                        styleActiveLine: true,
                        gutters: ["CodeMirror-lint-markers"],
                        lint: true
                };
                let {showError, changedFields} = this.state;
                return (
                        <form className="form-horizontal">
                                <div className="form-group">
                                        <label className="col-sm-3 control-label">Name*</label>
                                        <div className="col-sm-5">
                                                <input
                                                        name="name"
                                                        placeholder="Name"
                                                        onChange={this.handleValueChange.bind(this)}
                                                        type="text"
                                                        className={showError && changedFields.indexOf("name") !== -1 && this.state.name.trim() === '' ? "form-control invalidInput" : "form-control"}
                                                        value={this.state.name}
                                                        required={true}
                                                />
                                        </div>
                                </div>
                                <div className="form-group">
                                        <label className="col-sm-3 control-label">description*</label>
                                        <div className="col-sm-5">
                                                <input
                                                        name="description"
                                                        placeholder="description"
                                                        onChange={this.handleValueChange.bind(this)}
                                                        type="text"
                                                        className={showError && changedFields.indexOf("description") !== -1 && this.state.description.trim() === '' ? "form-control invalidInput" : "form-control"}
                                                        value={this.state.description}
                                                        required={true}
                                                />
                                        </div>
                                </div>
                                <div className="form-group">
                                        <label className="col-sm-3 control-label">Type*</label>
                                        <div className="col-sm-5">
                                                <input
                                                        name="type"
                                                        placeholder="Type"
                                                        onChange={this.handleValueChange.bind(this)}
                                                        type="text"
                                                        className={showError && changedFields.indexOf("type") !== -1 && this.state.type === '' ? "form-control invalidInput" : "form-control"}
                                                        value={this.state.type}
                                                    required={true}
                                                    disabled={true}
                                                />
                                        </div>
                                </div>
                                <div className="form-group">
                                        <label className="col-sm-3 control-label">Schema Group*</label>
                                        <div className="col-sm-5">
                                                <input
                                                        name="schemaGroup"
                                                        placeholder="schemaGroup"
                                                        onChange={this.handleValueChange.bind(this)}
                                                        type="text"
                                                        className={showError && changedFields.indexOf("schemaGroup") !== -1 && this.state.schemaGroup === '' ? "form-control invalidInput" : "form-control"}
                                                        value={this.state.schemaGroup}
                                                    required={true}
                                                    disabled={true}
                                                />
                                        </div>
                                </div>
                                <div className="form-group">
                                        <label className="col-sm-3 control-label">Compatibility</label>
                                        <div className="col-sm-5">
                                                <Select
                                                        value={this.state.compatibility}
                                                        options={this.state.compatibilityArr}
                                                        onChange={this.handleCompatibilityChange.bind(this)}
                                                        disabled={true}
                                                />
                                        </div>
                                </div>
                        </form>
                )
        }
}