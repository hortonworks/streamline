import React, {Component} from 'react';
import {Link} from 'react-router'
import state from '../app_state';

export default class Header extends Component {
	constructor(props){
		super();
	}

    toggleSideBar(){
    	state.sidebar.show = !state.sidebar.show;
    }
    render() {
        return (
            <header className="navbar navbar-inverse navbar-fixed-top">
			    <div className="container-fluid">
			        <div className="row">
			            <div className="col-sm-2">
			                <ul className="nav navbar-nav">
			                    {this.props.onLandingPage === 'false' ? <li><a role="button" className="left-bar-toggle" onClick={this.toggleSideBar.bind(this)}><i className="fa fa-lg fa-th-large"></i></a></li> : null}
			                    <li><Link to="/news-feed" role="button"><i className="fa fa-lg fa-newspaper-o"></i></Link></li>
			                </ul>
			            </div>
			            <div className="col-sm-8 text-center">
			                <Link to="/" className="navbar-logo"><img src="styles/img/logo.png" height="30" /></Link>
			            </div>
			            <div className="col-sm-2">
			                <ul className="nav navbar-nav navbar-right">
			                    <li><Link to="/configuration"><i className="fa fa-lg fa-gear"></i></Link></li>
			                    <li className="dropdown">
			                        <a href="javascript:void(0);" className="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false"><i className="fa fa-lg fa-user"></i></a>
			                        <ul className="dropdown-menu">
			                            <li><a href="javascript:void(0);">Change Password</a></li>
			                            <li><a href="javascript:void(0);">Edit Profile</a></li>
			                            <li><a href="javascript:void(0);">View Profile</a></li>
			                            <li role="separator" className="divider"></li>
			                            <li><a href="javascript:void(0);">Logout</a></li>
			                        </ul>
			                    </li>
			                </ul>
			            </div>
			        </div>
			    </div>
            </header>
        );
  }
}
