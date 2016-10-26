import React, {Component, PropTypes}from 'react';
import ReactDOM, { findDOMNode } from 'react-dom';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';
import { ItemTypes } from '../../../utils/Constants';
import { DragSource } from 'react-dnd';

const nodeSource = {
	beginDrag(props, monitor, component) {
		const { imgPath, type, name, nodeType } = props;
		return { imgPath, type, name, nodeType };
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
		nodeType: PropTypes.string.isRequired
	};

	getDragableNode(connectDragSource){
		const {imgPath, nodeType, type, name} = this.props;
		//TODO add img paths to Constants
		return connectDragSource(
                        <li>
                        <img src={"styles/img/icon-"+nodeType.toLowerCase()+".png"} /> {name}
                        </li>
		)
	}

	render(){
		const { hideSourceOnDrag, imgPath, type, name, connectDragSource, isDragging, children, nodeType } = this.props;
		if (isDragging && hideSourceOnDrag) {
	      return null;
	    }
                return this.getDragableNode(connectDragSource)
	}
}