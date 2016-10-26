import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import BaseContainer from '../../BaseContainer';
import {Link} from 'react-router';
import Modal from '../../../components/FSModal';
import {Treebeard} from 'react-treebeard';
import ReactCodemirror from 'react-codemirror';
import '../../../utils/Overrides';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';
import SchemaInfoForm from './SchemaInfoForm';
import SchemaVersionForm from './SchemaVersionForm';
import FSReactToastr from '../../../components/FSReactToastr';
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

export default class SchemaRegistryContainer extends Component {
	constructor(props){
		super();
		this.breadcrumbData = {
			title: 'Schema Registry',
			linkArr: [
				{title: 'Registry Service'},
				{title: 'Schema Registry'}
			]
		};
        this.style = {
    tree: {
        base: {
            listStyle: 'none',
            backgroundColor: '#fff',
            margin: 0,
            padding: 0,
            color: '#9DA5AB',
            fontFamily: 'lucida grande ,tahoma,verdana,arial,sans-serif',
            fontSize: '14px'
        },
        node: {
            base: {
                position: 'relative'
            },
            link: {
                cursor: 'pointer',
                position: 'relative',
                padding: '8px',
                display: 'block',
                borderTop: '1px solid #dddddd'
            },
            activeLink: {
                background: '#e2f4fc'
            },
            toggle: {
                base: {
                    position: 'relative',
                    display: 'inline-block',
                    verticalAlign: 'top',
                    marginLeft: '-5px',
                    height: '24px',
                    width: '24px'
                },
                wrapper: {
                    position: 'absolute',
                    top: '50%',
                    left: '50%',
                    margin: '-7px 0 0 -7px',
                    height: '14px'
                },
                height: 10,
                width: 10,
                arrow: {
                    fill: '#3b8640',
                    strokeWidth: 0
                }
            },
            header: {
                base: {
                    display: 'inline-block',
                    verticalAlign: 'top',
                    color: '#333333'
                },
                connector: {
                    width: '2px',
                    height: '12px',
                    borderLeft: 'solid 2px black',
                    borderBottom: 'solid 2px black',
                    position: 'absolute',
                    top: '0px',
                    left: '-21px'
                },
                title: {
                    lineHeight: '24px',
                    verticalAlign: 'middle'
                }
            },
            subtree: {
                listStyle: 'none',
                paddingLeft: '19px'
            },
            loading: {
                color: '#E2C089'
            }
        }
    }};

                this.state = {
                        currentSchema: {},
                        modalTitle: '',
                        schemaData: [],
                        editDescription: false
                };
                this.fetchData();
        }
        fetchData() {
                let promiseArr = [],
                        schemaData = [];
                SchemaREST.getAllSchemas()
                        .then((schema)=>{
                                if(schema.responseCode !== 1000){
                                        FSReactToastr.error(<strong>{schema.responseMessage}</strong>);
                                } else {
                                        schema.entities.map((s)=>{
                                                promiseArr.push(SchemaREST.getSchemaInfo(s.name));
                                        });
                                        Promise.all(promiseArr)
                                        .then((results)=>{
                                                results.map(result=>{
                                                        if(result.responseCode !== 1000){
                                                                FSReactToastr.error(<strong>{result.responseMessage}</strong>);
                                                        }
                                                        let {name, schemaGroup, type, description, compatibility } = result.entity.schemaMetadata;
                                                        let {id} = result.entity;
                                                        let children = [];
                                                        SchemaREST.getSchemaVersions(name).then((versions)=>{
                                                                versions.entities.map((v)=>{
                                                                        children.push({
                                                                                id: v.version,
                                                                                name: 'v'+v.version,
                                                                                description: v.description,
                                                                                schemaText: v.schemaText,
                                                                                type: type,
                                                                                compatibility: compatibility
                                                                        });
                                                                });
                                                                this.showSchema();
                                                        });
                                                        schemaData.push({id, name, schemaGroup, type, description, compatibility, children});
                                                });
                                                this.setState({schemaData: schemaData});
                                                this.showSchema();
                                        })
                                }
                        })
                        .catch((err)=>{
                                FSReactToastr.error(<strong>{err}</strong>);
                        });

        }
        handleAddSchema() {
                this.setState({
                        modalTitle: 'Add New Schema'
                }, ()=>{
                        this.refs.schemaModal.show();
                })
        }
        handleEditDescription() {
                if(this.state.editDescription) {
                        this.setState({editDescription: false});
                        return SchemaREST.postSchema({body: JSON.stringify(this.state.currentSchema)});
                } else {
                        this.setState({editDescription: true});
                }
        }
        handleAddVersion() {
                this.setState({
                        modalTitle: 'Add New Version'
                }, ()=>{
                        this.refs.versionModal.show();
                })
        }
        selectVersion(obj){
                this.onToggle(obj, true);
	}
        saveDescription(e) {
                let schemaObj = this.state.currentSchema;
                schemaObj.description = e.target.value;
                this.setState({currentSchema: schemaObj});
        }
        onToggle(node, toggled) {
                if(this.state.currentSchema){this.state.currentSchema.active = false;}
                node.active = true;
                if(node.children){ node.toggled = toggled; this.schemaName = node.name;}
                this.setState({ currentSchema: node });
        }
        showSchema() {
                let {schemaData} = this.state;
                if(schemaData.length){
                        if(this.schemaName) {
                                let node = _.find(schemaData, {name: this.schemaName});
                                this.onToggle(node, true);
                        } else this.onToggle(schemaData[0], true);
                }
        }
        handleSaveVersion(){
                if(this.refs.addVersion.validateData()){
                        this.refs.addVersion.handleSave().then((versions)=>{
                                this.fetchData();
                                this.refs.versionModal.hide();
                                if(versions.responseCode !== 1000){
                                        FSReactToastr.error(<strong>{versions.responseMessage}</strong>);
                                } else {
                                        let msg = "Version added successfully";
                                        if(this.state.id){
                                                msg = "Version updated successfully";
                                        }
                                        FSReactToastr.success(<strong>{msg}</strong>)
                                }
                        })
                }
        }
        handleSave() {
                if(this.refs.addSchema.validateData()){
                        this.refs.addSchema.handleSave().then((schemas)=>{
                                this.fetchData();
                                if(schemas.entity) {
                                        this.schemaName = this.refs.addSchema.state.name;
                                }
                                this.refs.schemaModal.hide();
                                if(schemas.responseCode !== 1000){
                                        FSReactToastr.error(<strong>{schemas.responseMessage}</strong>);
                                } else {
                                        let msg = "Schema added successfully";
                                        if(this.state.id){
                                                msg = "Schema updated successfully";
                                        }
                                        FSReactToastr.success(<strong>{msg}</strong>)
                                }
                        })
                }
        }
        render() {
                const jsonoptions = {
                        lineNumbers: true,
                        mode: "application/json",
                        styleActiveLine: true,
                        gutters: ["CodeMirror-lint-markers"],
                        lint: false,
                        readOnly: 'nocursor'
                };
                let schemaObj = this.state.currentSchema;
                let showSchema = !schemaObj.schemaText;
                return (
                        <div>
                        <BaseContainer routes={this.props.routes} onLandingPage="false" breadcrumbData={this.breadcrumbData}>
                                <div className="container">
                                <div className="container wrapper animated fadeIn">
                                        <div className="row">
                                                <div className="col-sm-3">
                                                        <button type="button" className="btn btn-success btn-sm btn-block" onClick={this.handleAddSchema.bind(this)}>
                                                                <i className="fa fa-file-code-o"></i> ADD SCHEMA
                                                        </button>
                                                        <Treebeard
                                                                data={this.state.schemaData}
                                                                onToggle={this.onToggle.bind(this)}
                                                                style={this.style}
                                                        />
                                                </div>
                                                <div className="col-sm-9">
                                                        {schemaObj.name ?
                                                        (<div className={schemaObj.children ? "schema-details" : ""}>
                                                                {showSchema ?
                                                                (<div>
                                                                        <div className="higlighted-section">
                                                                                <h4 className="schema-heading">{schemaObj.name}</h4>
                                                                        </div>
                                                                        <div>
                                                                                <div className="schema-content">
                                                                                        <h4 className="schema-subheading">About This Schema</h4>
                                                                                        <p>{schemaObj.description}</p>
                                                                                </div>
                                                                        </div>
                                                                </div>)
                                                                : (
                                                                <div>
                                                                        <div className="higlighted-section">
                                                                                <h4 className="schema-heading">{schemaObj.name}</h4>
                                                                        </div>
                                                                        <div className="schema-content">
                                                                                <div className="row row-margin-bottom">
                                                                                        <div className="col-sm-4 schema-stats">
                                                                                                <i className="fa fa-folder fa-3x"></i>
                                                                                                <div>
                                                                                                        <h6>VERSION:</h6>
                                                                                                        <h4>{schemaObj.id}</h4>
                                                                                                </div>
                                                                                        </div>
                                                                                        <div className="col-sm-4 schema-stats">
                                                                                                <i className="fa fa-file fa-3x"></i>
                                                                                                <div>
                                                                                                        <h6>TYPE:</h6>
                                                                                                        <h4>{schemaObj.type}</h4>
                                                                                                </div>
                                                                                        </div>
                                                                                        <div className="col-sm-4 schema-stats">
                                                                                                <i className="fa fa-plug fa-3x"></i>
                                                                                                <div>
                                                                                                        <h6>COMPATIBILITY:</h6>
                                                                                                        <h4>{schemaObj.compatibility}</h4>
                                                                                                </div>
                                                                                        </div>
                                                                                </div>
                                                                                <div className="">
                                                                                        <ReactCodemirror ref="JSONCodemirror" value={JSON.stringify(JSON.parse(schemaObj.schemaText), null, ' ')} options={jsonoptions} />
                                                                                </div>
                                                                        </div>
                                                                </div>
                                                                        )}
                                                        </div>)
                                                        : null
                                                        }

                                                        {schemaObj.children ?
                                                                (<div className="schema-sidebar">
                                                                        <div className="higlighted-section">
                                                                                {schemaObj.children.length === 1 ?
                                                                                        <p><strong>There is 1 version of this Schema</strong></p>
                                                                                        :<p><strong>There are {schemaObj.children.length} versions of this Schema</strong></p>
                                                                                }
                                                                                <ul className="schema-versions">
                                                                                        {schemaObj.children.map((v, i)=>{
                                                                                        return (<li className="clearfix" key={i} onClick={this.selectVersion.bind(this, v)}>
                                                                                        <i className="fa fa-folder-o pull-left"></i>
                                                                                        <p><strong>Version {v.id}</strong><br/> <span className="text-muted"></span></p>
                                                                                        </li>)
                                                                                        }
                                                                                        )}
                                                                                </ul>
                                                                        <p><a href="javascript:void(0);" onClick={this.handleAddVersion.bind(this)}>ADD VERSION</a></p>
                                                                        </div>
                                                                </div>)
                                                                : null}
                                                        </div>
                                                </div>
                                        </div>
                                </div>
                        </BaseContainer>

                        <Modal ref="schemaModal" bsSize="large" data-title={this.state.modalTitle} data-resolve={this.handleSave.bind(this)}>
                                <SchemaInfoForm ref="addSchema"/>
                        </Modal>
                        <Modal ref="versionModal" bsSize="large" data-title={this.state.modalTitle} data-resolve={this.handleSaveVersion.bind(this)}>
                                <SchemaVersionForm ref="addVersion" schemaName={this.schemaName}/>
                        </Modal>
                </div>
                )
	}
}
