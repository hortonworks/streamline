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
        let {bundleArr} = this.props;
        if(!bundleArr){
            bundleArr = {
                sourceBundle: [],
                processorsBundle: [],
                sinksBundle: []
            }
        }
        this.state = {
            datasources: Utils.sortArray(bundleArr.sourceBundle, 'subType', true),
            processors: Utils.sortArray(bundleArr.processorsBundle, 'subType', true),
            sinks: Utils.sortArray(bundleArr.sinksBundle, 'subType', true)
        };
    }
    componentWillReceiveProps(nextProps, oldProps){
        if(nextProps.bundleArr != this.props.bundleArr){
            this.setState({
                datasources: Utils.sortArray(nextProps.bundleArr.sourceBundle, 'subType', true),
                processors: Utils.sortArray(nextProps.bundleArr.processorsBundle, 'subType', true),
                sinks: Utils.sortArray(nextProps.bundleArr.sinksBundle, 'subType', true)
            });
        }
    }

    render(){
        const { hideSourceOnDrag, left, top, isDragging } = this.props;
        if (isDragging && hideSourceOnDrag) {
          return null;
        }
        return (
            <div className="component-panel right" style={{height : window.innerHeight - 60}}>
              <button className="btn-draggable"></button>
              <div className="panel-wrapper" style={{height : window.innerHeight - 90}}>
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
                                defaultImagePath='styles/img/icon-source.png'
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
                                defaultImagePath='styles/img/icon-processor.png'
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
                                defaultImagePath='styles/img/icon-processor.png'
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
                            defaultImagePath='styles/img/icon-sink.png'
                        />
                    )
                })}
                </ul>
              </div>
              <button className="btn-draggable"></button>
            </div>
        )
    }
}
