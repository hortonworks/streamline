import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {OverlayTrigger, Tooltip, Accordion, Panel, PanelGroup} from 'react-bootstrap';
import { ItemTypes, Components } from '../../../utils/Constants';
import { DragSource } from 'react-dnd';
import NodeContainer from './NodeContainer';
import state from '../../../app_state';
import _ from 'lodash';
import Utils from '../../../utils/Utils';

const nodeSource = {
	beginDrag(props, monitor, component) {
		const { left, top } = props;
		return { left, top };
	}
};

function collect(connect, monitor) {
	return {
		connectDragSource: connect.dragSource(),
		isDragging: monitor.isDragging()
	};
}

@DragSource(ItemTypes.ComponentNodes, nodeSource, collect)
export default class ComponentNodeContainer extends Component {
	static propTypes = {
		connectDragSource: PropTypes.func.isRequired,
		isDragging: PropTypes.bool.isRequired,
		left: PropTypes.number.isRequired,
		top: PropTypes.number.isRequired,
		hideSourceOnDrag: PropTypes.bool.isRequired,
		children: PropTypes.node
	};

	constructor(props){
		super(props);
		let componentsObj = this.getAllComponents();
		this.state = {
			componentsBox: true,
			sourceBox: true,
			processorBox: false,
			sinkBox: false,
			activeKey: '1',
			searchComponent: false,
			datasources: componentsObj.datasources,
			processors: componentsObj.processors,
			sinks: componentsObj.sinks
		};
	}

	getAllComponents() {
		let datasources = Utils.sortArray(Components.Datasources.slice(), 'label', true),
			processors = [...Components.Processors.slice()],
			sinks = Utils.sortArray(Components.Sinks.slice(), 'label', true);

		processors = processors.filter((p)=>{return p.name !== 'Custom'});
		this.props.customProcessors.map((p)=>{
			let config = p.config ? JSON.parse(p.config) : {},
				name = _.find(config, {name: "name"});
			p.label = name.defaultValue;
			processors.push(p);
		});
		processors = Utils.sortArray(processors.slice(), 'label', true);
		return {datasources, processors, sinks};
	}

	showHideComponentsBox() {
		this.setState({ componentsBox: !this.state.componentsBox });
	}

	showHideSourceBox() {
		this.setState({ sourceBox: !this.state.sourceBox });
	}

	showHideProcessorBox() {
		this.setState({ processorBox: !this.state.processorBox });
	}

	showHideSinkBox() {
		this.setState({ sinkBox: !this.state.sinkBox });
	}

	getComponentHeader(){
		let iconClass = this.state.componentsBox ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div> <i className={iconClass}></i> Components </div>)
	}

	getSourceHeader(){
		let iconClass = this.state.sourceBox ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div> <i className={iconClass}></i> Source </div>)
	}
	getProcessorHeader(){
		let iconClass = this.state.processorBox ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div> <i className={iconClass}></i> Processor </div>)
	}
	getSinkHeader(){
		let iconClass = this.state.sinkBox ? "fa fa-caret-down" : "fa fa-caret-right";
		return (<div><i className={iconClass}></i> Sink </div>)
	}
	hide(e){
		e.stopPropagation()
		state.showComponentNodeContainer = false;
	}
	handleSelectPanel(activeKey){
		this.setState({ activeKey });
	}
	handleSearchComponent(e) {
		let name = e.target.value,
			showSources = false,
			showProcessors = false,
			showSinks = false,
			datasourcesArr = [],
			processorsArr = [],
			sinksArr = [];

		if(name === '') {
			let componentsObj = this.getAllComponents();
			this.setState({
				datasources: componentsObj.datasources,
				processors: componentsObj.processors,
				sinks: componentsObj.sinks,
				searchComponent: false
			});
			return;
		}

		Components.Datasources.map((o)=>{
			if(o.label.toLowerCase().indexOf(name.toLowerCase())!=-1 && !o.hideOnUI) {
				datasourcesArr.push(o);
			}
		});
		Components.Processors.map((o)=>{
			if(o.label.toLowerCase().indexOf(name.toLowerCase())!=-1 && !o.hideOnUI && o.label !== 'Custom') {
				processorsArr.push(o);
			}
		});
		this.props.customProcessors.map((p)=>{
			let config = p.config ? JSON.parse(p.config) : {},
				customName = _.find(config, {name: "name"});
			p.label = customName.defaultValue;
			if(p.label.toLowerCase().indexOf(name.toLowerCase())!=-1) {
				processorsArr.push(p);
			}
		});
		Components.Sinks.map((o)=>{
			if(o.label.toLowerCase().indexOf(name.toLowerCase())!=-1 && !o.hideOnUI) {
				sinksArr.push(o);
			}
		});
		if(datasourcesArr.length > 0) {
			datasourcesArr = Utils.sortArray(datasourcesArr.slice(), 'label', true);
			showSources = true;
		}
		if(processorsArr.length > 0) {
			processorsArr = Utils.sortArray(processorsArr.slice(), 'label', true);
			showProcessors = true;
		}if(sinksArr.length > 0) {
			sinksArr = Utils.sortArray(sinksArr.slice(), 'label', true);
			showSinks = true;
		}

		this.setState({
			searchComponent: true,
			sourceBox: showSources,
			datasources: datasourcesArr,
			processorBox: showProcessors,
			processors: processorsArr,
			sinkBox: showSinks,
			sinks: sinksArr
		});
	}
	render(){
		const { hideSourceOnDrag, left, top, isDragging, children } = this.props;
	    if (isDragging && hideSourceOnDrag) {
	      return null;
	    }
		return (
			<div className="component-panel">
					<div>
						<input
							className="form-control"
							type="text"
							placeholder="Search..."
							onChange={this.handleSearchComponent.bind(this)}
						/>
					</div>
					<PanelGroup activeKey={this.state.activeKey} onSelect={this.handleSelectPanel.bind(this)} id="component-accordion">
						<Panel
							eventKey="1"
							header={this.getSourceHeader()}
							onSelect={this.showHideSourceBox.bind(this)}
							collapsible={true}
							expanded={this.state.sourceBox}
						>
							<ul className="list-group">
							{this.state.datasources.map((source, i)=>{
								if(source.hideOnUI === 'true'){
									return null;
								}
								return (
									<NodeContainer
										key={i}
										imgPath={source.imgPath}
										name={source.label}
										type={Components.Datasource.value}
										nodeType={source.name}
										hideSourceOnDrag={false}
									/>
								)
							})}
							</ul>
						</Panel>
						<Panel
							eventKey="2"
							header={this.getProcessorHeader()}
							onSelect={this.showHideProcessorBox.bind(this)}
							collapsible={true}
							expanded={this.state.processorBox}
						>
							<ul className="list-group">
							{this.state.processors.map((processor, i)=>{
								if(processor.hideOnUI === 'true'){
									return null;
								}
								if(processor.subType === 'CUSTOM') {
									let config = processor.config ? JSON.parse(processor.config) : {},
										name = _.find(config, {name: "name"});
									return (
									<NodeContainer
										key={i}
                                                                                imgPath="styles/img/color-icon-custom.png"
										name={name ? name.defaultValue : 'Custom'}
										type={Components.Processor.value}
										nodeType="Custom"
										hideSourceOnDrag={false}
									/>
									)
								} else {
									return (
									<NodeContainer
										key={i}
										imgPath={processor.imgPath}
										name={processor.label}
										type={Components.Processor.value}
										nodeType={processor.name}
										hideSourceOnDrag={false}
									/>
									)
								}
							})}
							</ul>
						</Panel>
						<Panel
							eventKey="3"
							header={this.getSinkHeader()}
							onSelect={this.showHideSinkBox.bind(this)}
							collapsible={true}
							expanded={this.state.sinkBox}
						>
							{this.state.sinks.map((sink, i)=>{
								if(sink.hideOnUI === 'true'){
									return null;
								}
								return (
									<NodeContainer
										key={i}
										imgPath={sink.imgPath}
										name={sink.label}
										type={Components.Sink.value}
										nodeType={sink.name}
										hideSourceOnDrag={false}
									/>
								)
							})}
						</Panel>
					</PanelGroup>
			</div>
		)
	}
}