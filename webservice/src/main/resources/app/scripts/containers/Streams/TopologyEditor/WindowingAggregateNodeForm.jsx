import React, {Component, PropTypes}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import Select from 'react-select';
import {Tabs, Tab} from 'react-bootstrap';
import FSReactToastr from '../../../components/FSReactToastr';
import TopologyREST from '../../../rest/TopologyREST';
import OutputSchema from '../../../components/OutputSchemaComponent';

export default class WindowingAggregateNodeForm extends Component {
	static propTypes = {
		nodeData: PropTypes.object.isRequired,
		configData: PropTypes.object,
		editMode: PropTypes.bool.isRequired,
		nodeType: PropTypes.string.isRequired,
		topologyId: PropTypes.string.isRequired,
		sourceNode: PropTypes.object.isRequired,
		targetNodes: PropTypes.array.isRequired,
		linkShuffleOptions: PropTypes.array.isRequired
	};

	constructor(props) {
		super(props);
		let {configData, editMode} = props;

		var obj = {
			parallelism: 1,
			editMode: editMode,
			showSchema: false
		};
		this.state = obj;
	}

	render() {
		return (
			<div>
			</div>
		)
	}
}