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
	};

	constructor(props){
		super(props);
		this.state = {
            datasources: props.bundleArr.sourceBundle,
            processors: props.bundleArr.processorsBundle,
            sinks: props.bundleArr.sinksBundle
		};
	}

	render(){
        const { hideSourceOnDrag, left, top, isDragging } = this.props;
	    if (isDragging && hideSourceOnDrag) {
	      return null;
	    }
		return (
            <div className="component-panel right">
              <div className="panel-wrapper">
                <h6 className="component-title">Source</h6>
                <ul className="component-list">
                    {this.state.datasources.map((source, i)=>{
                        return (
                            <NodeContainer
                                key={i}
                                imgPath={"styles/img/icon-"+source.subType.toLowerCase()+".png"}
                                name={source.subType}
                                type={source.type}
                                nodeType={source.subType}
                                hideSourceOnDrag={false}
                                topologyComponentBundleId={source.id}
                            />
                        )
                    })}
                </ul>
                <h6 className="component-title">Processor</h6>
                <ul className="component-list">
                {this.state.processors.map((processor, i)=>{
                    if(processor.subType === 'CUSTOM') {
                        let config = processor.topologyComponentUISpecification.fields,
                        name = _.find(config, {fieldName: "name"});
                        return (
                            <NodeContainer
                                key={i}
                                imgPath="styles/img/icon-custom.png"
                                name={name ? name.defaultValue : 'Custom'}
                                type={processor.type}
                                nodeType="Custom"
                                hideSourceOnDrag={false}
                                topologyComponentBundleId={processor.id}
                            />
                        )
                    } else {
                        return (
                            <NodeContainer
                                    key={i}
                                    imgPath={"styles/img/icon-"+processor.subType.toLowerCase()+".png"}
                                    name={processor.subType}
                                    type={processor.type}
                                    nodeType={processor.subType}
                                    hideSourceOnDrag={false}
                                    topologyComponentBundleId={processor.id}
                            />
                        )
                    }
                })}
                </ul>
                <h6 className="component-title">Sink</h6>
                <ul className="component-list">
                {this.state.sinks.map((sink, i)=>{
                    return (
                        <NodeContainer
                            key={i}
                            imgPath={"styles/img/icon-"+sink.subType.toLowerCase()+".png"}
                            name={sink.subType}
                            type={sink.type}
                            nodeType={sink.subType}
                            hideSourceOnDrag={false}
                            topologyComponentBundleId={sink.id}
                        />
                    )
                })}
                </ul>
              </div>

			</div>
		)
	}
}
