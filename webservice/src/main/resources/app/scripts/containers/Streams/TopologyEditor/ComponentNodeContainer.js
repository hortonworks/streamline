import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {OverlayTrigger, Tooltip, Accordion, Panel} from 'react-bootstrap';
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
			processorBox: true,
			sinkBox: true
		};
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
		return (<div> <i className={iconClass}></i> Sink </div>)
	}
	hide(e){
		e.stopPropagation()
		state.showComponentNodeContainer = false;
	}
	render(){
		const { hideSourceOnDrag, left, top, connectDragSource, isDragging, children } = this.props;
	    if (isDragging && hideSourceOnDrag) {
	      return null;
	    }
		return connectDragSource(
			<div className="nodes-list-container" style={{ left, top }}>
					<Panel 
						header={ [<strong key="1">Components</strong>, <i key="2"className="fa fa-close pull-right" style={{cursor: 'pointer'}} onClick={this.hide.bind(this)}></i> ]} 
						onSelect={this.showHideComponentsBox.bind(this)}
						collapsible={true}
						defaultExpanded={this.state.componentsBox}
					>
						<Panel 
							header={this.getSourceHeader()}
							onSelect={this.showHideSourceBox.bind(this)}
							collapsible={true}
							defaultExpanded={this.state.sourceBox}
						>
							{Components.Datasources.map((source, i)=>{
								if(source.hideOnUI === 'true'){
									return null;
								}
								return (
									<NodeContainer
										key={i}
										imgPath={source.imgPath}
										name={source.name}
										type={Components.Datasource.value}
										nodeType={source.name}
										hideSourceOnDrag={false}
									/>
								)
							})}
						</Panel>
						<Panel 
							header={this.getProcessorHeader()}
							onSelect={this.showHideProcessorBox.bind(this)}
							collapsible={true}
							defaultExpanded={this.state.processorBox}
						>
							{Components.Processors.map((processor, i)=>{
								if(processor.hideOnUI === 'true' || processor.name === 'Custom'){
									return null;
								}
								return (
									<NodeContainer
										key={i}
										imgPath={processor.imgPath}
										name={processor.name}
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
						</Panel>
						<Panel 
							header={this.getSinkHeader()}
							onSelect={this.showHideSinkBox.bind(this)}
							collapsible={true}
							defaultExpanded={this.state.sinkBox}
						>
							{Components.Sinks.map((sink, i)=>{
								if(sink.hideOnUI === 'true'){
									return null;
								}
								return (
									<NodeContainer
										key={i}
										imgPath={sink.imgPath}
										name={sink.name}
										type={Components.Sink.value}
										nodeType={sink.name}
										hideSourceOnDrag={false}
									/>
								)
							})}
						</Panel>
					</Panel>
			</div>
		)
	}
}