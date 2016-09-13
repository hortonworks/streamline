import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import {Link} from 'react-router';
import _ from 'lodash';

export default class ComponentConfigContainer extends Component {

	constructor(props) {
		super(props);
	}

	render() {
		return (
			<div className="container-fluid">
			<div className="row">
				<div className="col-md-12">
				<div className="row config-row">
					<div className="col-md-4">
						<div className="box">
                            <div className="box-head">
                                <h4>KAFKA</h4>
                                <div className="box-controls">
                                    <a href="javascript:void(0)" className="addNewConfig" title="Add" data-rel="tooltip" data-id="KAFKA">
                                        <i className="fa fa-user-plus"></i>
                                    </a>
                                </div>
                            </div>
							<ul id="Kafka-list" className="box-list">
							</ul>
						</div>
					</div>
					<div className="col-md-4">
						<div className="box">
                            <div className="box-head">
                                <h4>STORM</h4>
                                <div className="box-controls">
                                    <a href="javascript:void(0)" className="addNewConfig" title="Add" data-rel="tooltip" data-id="STORM">
                                        <i className="fa fa-user-plus"></i>
                                    </a>
                                </div>
                            </div>
							<ul id="Storm-list" className="box-list">
							</ul>
						</div>
					</div>
					<div className="col-md-4">
						<div className="box">
                            <div className="box-head">
                                <h4>HDFS</h4>
                                <div className="box-controls">
                                    <a href="javascript:void(0)" className="addNewConfig" title="Add" data-rel="tooltip" data-id="HDFS">
                                        <i className="fa fa-user-plus"></i>
                                    </a>
                                </div>
                            </div>
							<ul id="Hdfs-list" className="box-list">
							</ul>
						</div>
					</div>
				</div>
				</div>
			</div>
			</div>
			)
	}
}