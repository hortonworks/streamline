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
	render(){
		const { hideSourceOnDrag, left, top, isDragging, children } = this.props;
	    if (isDragging && hideSourceOnDrag) {
	      return null;
	    }
		return (
                        <div className="component-panel right">
                                        <h6 className="component-title">Source</h6>
                                        <ul className="component-list">
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
                                        <h6 className="component-title">Processor</h6>
                                        <ul className="component-list">
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
                                                                                imgPath="styles/img/icon-custom.png"
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
                                        <h6 className="component-title">Sink</h6>
                                        <ul className="component-list">
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
                                        </ul>
			</div>
		)
	}
}