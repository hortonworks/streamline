import React, {Component} from 'react';
import ReactDOM, {findDOMNode} from 'react-dom';
import _ from 'lodash';
import {Tabs, Tab, Row, Nav, NavItem} from 'react-bootstrap';
import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';
import Editable from '../components/Editable';

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

export default class OutputSchemaContainer extends Component {

	constructor(props) {
		super();
		if(props.streamData.length)
			this.state = {
				activeTab: 1,
				streamData: props.streamData
			};
		else this.state = {
				activeTab: 1,
				streamData: [{
					streamId: 'Stream-1',
					fields: JSON.stringify([{
						"name": "childField1",
						"type": "INTEGER"
					}, {
						"name": "childField2",
						"type": "BOOLEAN"
					}, {
						"name": "topLevelStringField",
						"type": "STRING"
					}], null, " ")
				}]
		};
		this.validateFlag = true;
		this.streamNamesList = [];
	}
	componentWillReceiveProps(nextProps) {
		if(nextProps.streamData.length > 0){
			this.setState({ streamData: nextProps.streamData });
		}
	}

	handleSelectTab(key, e) {
		if(e.target.parentNode.getAttribute("class") === "editable-container") {
			this.streamName = JSON.parse(JSON.stringify(this.state.streamData))[key -1].streamId;
		}
		if(this.state.activeTab === key)
			return;
		if(key === "addNewTab") {
			//Dynamic Names of streams
			let newStreamId = 'Stream-1';
			while(this.streamNamesList.indexOf(newStreamId) !== -1){
				let arr = newStreamId.split('-');
				let count = 1;
				if(arr.length > 1){
					count = parseInt(arr[1], 10) + 1;
				}
				newStreamId = arr[0]+'-'+count;
			}
			this.streamNamesList.push(newStreamId);
			//
			let tabId = this.state.streamData.length + 1;
			let obj = {
				streamId: newStreamId,
				fields: ''
			};
			this.setState({
				activeTab: tabId,
				streamData: [...this.state.streamData, obj]
			});
		} else this.setState({
			activeTab: key
		});
	}

	handleSchemaChange(json) {
		this.state.streamData[this.state.activeTab - 1].fields = json;
	}

	getOutputStreams() {
		return this.state.streamData;
	}

	handleDeleteStream(e) {
		this.state.streamData.splice(this.state.activeTab - 1, 1);
		this.setState({activeTab: 1});
	}

	handleStreamNameChange(e){
		let name = e.target.value;
		if(this.validateName(name)){
			let {streamData} = this.state;
			streamData[e.target.dataset.index].streamId = name;
			this.setState({streamData});
		}
	}

	validateName(name){
		if(name === ''){
			this.refs.streamNameEditable.setState({errorMsg: "Stream-id cannot be blank"});
			this.validateFlag = false;
			return false;
		} else if(name.search(' ') !== -1){
			this.refs.streamNameEditable.setState({errorMsg: "Stream-id cannot have space in between"});
			this.validateFlag = false;
			return false;
		} else if(this.streamNamesList.indexOf(name) !== -1){
			this.refs.streamNameEditable.setState({errorMsg: "Stream-id is already present. Please use some other id."});
			this.validateFlag = false;
			return false;
		} else {
			this.refs.streamNameEditable.setState({errorMsg: ""});
			this.validateFlag = true;
			return true;
		}
	}

	saveStreamName(e){
		if(this.validateFlag){
			this.refs.streamNameEditable.hideEditor();
		}
	}

	handleEditableReject(){
		let {streamData} = this.state;
			streamData[this.state.activeTab - 1].streamId = this.streamName;
		this.setState({streamData: streamData});
		this.refs.streamNameEditable.setState({errorMsg: ""},()=>{
			this.refs.streamNameEditable.hideEditor();
		});
	}

    render() {
		const jsonoptions = {
			lineNumbers: true,
			mode: "application/json",
			styleActiveLine: true,
			gutters: ["CodeMirror-lint-markers"],
			lint: true
        };
        this.streamNamesList = [];
        return (
			<Tab.Container activeKey={this.state.activeTab} id="tabs-container" onSelect={this.handleSelectTab.bind(this)}>
				<Row className="clearfix">
				<Nav bsStyle="tabs">
					{
						this.state.streamData.map((obj, i) => {
							this.streamNamesList.push(obj.streamId);
						return (
							<NavItem eventKey={i+1} key={i+1}>
								{this.state.activeTab === (i+1) ?
								<Editable
									id="streamName"
									ref="streamNameEditable"
									inline={false}
									resolve={this.saveStreamName.bind(this)}
									reject={this.handleEditableReject.bind(this)}
								>
								<input defaultValue={obj.streamId} data-index={i} onChange={this.handleStreamNameChange.bind(this)}/>
								</Editable>
								: obj.streamId
								}
								{this.state.streamData.length > 1 && this.state.activeTab === (i+1) ? (
									<span className="cancelSchema" onClick={this.handleDeleteStream.bind(this)}><i className="fa fa-times-circle"></i></span>
									)
								: null}
							</NavItem>
						)}
						)
					}
					<NavItem eventKey="addNewTab">
						<i className="fa fa-plus"></i>
					</NavItem>
				</Nav>
				<Tab.Content>
					{
					this.state.streamData.map((obj, i) => {
						return (
							<Tab.Pane eventKey={i+1}  key={i+1}>
							<div className="col-sm-6">
								<ReactCodemirror
									ref="JSONCodemirror"
									value={obj.fields}
									onChange={this.handleSchemaChange.bind(this)}
									options={jsonoptions}
								/>
							</div>
							</Tab.Pane>
							)}
					)
					}
				</Tab.Content>
				</Row>
			</Tab.Container>
        );
  }
}
