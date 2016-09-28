import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {OverlayTrigger, Tooltip, Accordion, Panel, PanelGroup} from 'react-bootstrap';
import { ItemTypes, Components } from '../../../utils/Constants';
import { DragSource } from 'react-dnd';
import NodeContainer from './NodeContainer';
import state from '../../../app_state';
import _ from 'lodash';

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
		this.state = {
			componentsBox: true,
			sourceBox: true,
			processorBox: false,
			sinkBox: false,
			activeKey: '1'
		};
	}

	showHideComponentsBox() {
		this.setState({ componentsBox: !this.state.componentsBox });
	}

	showHideSourceBox() {
		this.setState({ sourceBox: true, processorBox: false, sinkBox: false });
	}

	showHideProcessorBox() {
		this.setState({ processorBox: true, sourceBox: false, sinkBox: false });
	}

	showHideSinkBox() {
		this.setState({ sinkBox: true, sourceBox: false, processorBox: false });
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
			<div className="component-panel">
					<PanelGroup activeKey={this.state.activeKey} onSelect={this.handleSelectPanel.bind(this)} id="component-accordion" accordion>
						<Panel
							eventKey="1"
							header={this.getSourceHeader()}
							onSelect={this.showHideSourceBox.bind(this)}
						>
							<ul className="list-group">
							{Components.Datasources.map((source, i)=>{
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
						>
							<ul className="list-group">
							{Components.Processors.map((processor, i)=>{
								if(processor.hideOnUI === 'true' || processor.name === 'Custom'){
									return null;
								}
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
							})}
							{this.props.customProcessors.map((processor, i)=>{
								if(processor.hideOnUI === 'true'){
									return null;
								}
								let config = JSON.parse(processor.config),
									name = _.find(config, {name: "name"});
								return (
									<NodeContainer
										key={i}
										imgPath="styles/img/custom.png"
										name={name.defaultValue}
										type={Components.Processor.value}
										nodeType="Custom"
										hideSourceOnDrag={false}
									/>
								)
							})}
							</ul>
						</Panel>
						<Panel
							eventKey="3"
							header={this.getSinkHeader()}
							onSelect={this.showHideSinkBox.bind(this)}
						>
							{Components.Sinks.map((sink, i)=>{
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