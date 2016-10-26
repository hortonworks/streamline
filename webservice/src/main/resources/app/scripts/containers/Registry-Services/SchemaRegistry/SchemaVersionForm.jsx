import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import ReactCodemirror from 'react-codemirror';
import '../../../utils/Overrides';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';
import SchemaREST from '../../../rest/SchemaREST';

CodeMirror.registerHelper("lint", "json", function(text) {
        var found = [];
        var {parser} = jsonlint;
        parser.parseError = function(str, hash) {
        var loc = hash.loc;
        found.push({from: CodeMirror.Pos(loc.first_line - 1, loc.first_column),
                                to: CodeMirror.Pos(loc.last_line - 1, loc.last_column),
                                message: str});
        };
        try { jsonlint.parse(text); }
        catch(e) {}
        return found;
});

export default class SchemaVersionForm extends Component {
        constructor(props){
                super(props);
                this.state = {
                        schemaText: '',
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

        handleJSONChange(json){
                this.setState({
                                schemaText: json
                });
        }

        validateData(){
                let { schemaText, description, changedFields } = this.state;
                if(schemaText.trim() === '' || description.trim() === '') {
                        if(description.trim() === '' && changedFields.indexOf("description") === -1)
                                changedFields.push('description');
                        this.setState({showError: true, showErrorLabel: true, changedFields: changedFields});
                        return false;
                } else {
                        this.setState({showErrorLabel: true});
                        return true;
                }
        }

        handleSave(){
                let { schemaText, description } = this.state;
                let data = { schemaText, description }
                return SchemaREST.postVersion(this.props.schemaName, {body: JSON.stringify(data)});
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
                                        <label className="col-sm-3 control-label">Description*</label>
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
                                        <label className="col-sm-3 control-label">Schema Text*</label>
                                        <div className="col-sm-5">
                                                <ReactCodemirror ref="JSONCodemirror" value={this.state.schemaText} onChange={this.handleJSONChange.bind(this)} options={jsonoptions} />
                                        </div>
                                </div>
                        </form>
                )
        }
}