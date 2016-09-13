import React, {Component} from 'react';

class BtnDelete extends Component {
	constructor(props){
		super();
	}
	render(){
		return(
			<button type="button" onClick={this.props.callback} className="btn-danger">
				<i className="fa fa-trash"></i>
			</button>
		)
	}
}

class BtnEdit extends Component {
	constructor(props){
		super();
	}
	render(){
		return(
			<button type="button" onClick={this.props.callback} className="btn-warning">
				<i className="fa fa-pencil"></i>
			</button>
		)
	}
}

export {
	BtnDelete,
	BtnEdit
}