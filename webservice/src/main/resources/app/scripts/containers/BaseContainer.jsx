import React, {Component} from 'react';
import Header from '../components/Header';
import Footer from '../components/Footer';
import {Link} from 'react-router';
import {Confirm} from '../components/FSModal'

import 'animate.css/animate.css';
import 'styles/css/bootstrap.css';
import 'react-structured-filter/lib/react-structured-filter.css';
import 'react-select/dist/react-select.css';
import 'react-bootstrap-switch/dist/css/bootstrap3/react-bootstrap-switch.min.css';
import 'styles/css/toastr.min.css';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint.css';
import 'styles/css/theme.css';

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
            <section className={
                this.props.onLandingPage === "true" ?
                "landing-wrapper container" :
                "container-fluid wrapper animated fadeIn"
              }
            >
              {this.props.children}
            </section>
            <Footer
              routes={routes}
              breadcrumbData={this.props.breadcrumbData}
            />
            <Confirm ref="Confirm"/>
        </div>
    );
  }
}
