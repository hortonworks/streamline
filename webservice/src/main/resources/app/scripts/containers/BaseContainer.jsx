import React, {Component} from 'react';
import Header from '../components/Header';
import Footer from '../components/Footer';
import Sidebar from '../components/Sidebar';
import {Link} from 'react-router';
import {Confirm} from '../components/FSModal'

import 'animate.css/animate.css';
import 'styles/css/bootstrap.css';
import 'bootstrap-daterangepicker/daterangepicker.css';
import 'react-select/dist/react-select.css';
import 'react-bootstrap-switch/dist/css/bootstrap3/react-bootstrap-switch.min.css';
import 'styles/css/toastr.min.css';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint.css';
import 'styles/css/theme.css';
import 'styles/css/graph-style.css';

export default class BaseContainer extends Component {

  constructor(props) {
      super(props);
      // Operations usually carried out in componentWillMount go here
      this.state = {}
  }

  render(){
    const routes = this.props.routes || [{path: "/", name: 'Home'}]
    return(
        <div>
            <Header
              onLandingPage={this.props.onLandingPage}
              headerContent={this.props.headerContent}
            />
            <Sidebar/>
            <section className="content-wrapper">
              {this.props.children}
            </section>
            <Confirm ref="Confirm"/>
        </div>
    );
  }
}
