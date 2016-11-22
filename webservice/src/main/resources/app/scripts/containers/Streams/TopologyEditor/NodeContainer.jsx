import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import { ItemTypes } from '../../../utils/Constants';
import { DragSource } from 'react-dnd';

const nodeSource = {
	beginDrag(props, monitor, component) {
        const { imgPath, type, name, nodeType, topologyComponentBundleId } = props;
        return { imgPath, type, name, nodeType, topologyComponentBundleId };
	}
};

function collect(connect, monitor) {
	return {
		connectDragSource: connect.dragSource(),
		isDragging: monitor.isDragging()
	};
}

@DragSource(ItemTypes.Nodes, nodeSource, collect)
export default class NodeContainer extends Component {
	static propTypes = {
		connectDragSource: PropTypes.func.isRequired,
		isDragging: PropTypes.bool.isRequired,
		imgPath: PropTypes.string.isRequired,
		type: PropTypes.string.isRequired,
		name: PropTypes.string.isRequired,
		hideSourceOnDrag: PropTypes.bool.isRequired,
		children: PropTypes.node,
        nodeType: PropTypes.string.isRequired,
        topologyComponentBundleId: PropTypes.number.isRequired,
        defaultImagePath: PropTypes.string.isRequired
	};

	getDragableNode(connectDragSource){
        const {imgPath, nodeType, type, name, topologyComponentBundleId, defaultImagePath} = this.props;
		return connectDragSource(
            <li>
                <img src={imgPath} ref="img" onError={() => {this.refs.img.src=defaultImagePath}} /> {name}
            </li>
		)
	}

	render(){
        const { hideSourceOnDrag, imgPath, type, name, connectDragSource, isDragging, children, nodeType, topologyComponentBundleId } = this.props;
		if (isDragging && hideSourceOnDrag) {
	      return null;
	    }
        return this.getDragableNode(connectDragSource)
	}
}