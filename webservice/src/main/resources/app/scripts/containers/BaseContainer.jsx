import React, {Component} from 'react';
import Header from '../components/Header';
import Sidebar from '../components/Sidebar';
import {Link} from 'react-router';
import Breadcrumbs from 'react-breadcrumbs';
import {Confirm} from '../components/FSModal'

import 'animate.css/animate.css';
import 'styles/css/bootstrap.css';
import 'react-structured-filter/lib/react-structured-filter.css';
import 'react-select/dist/react-select.css';
import 'react-bootstrap-switch/dist/css/bootstrap3/react-bootstrap-switch.min.css';
import 'styles/css/toastr.min.css';
import 'codemirror/lib/codemirror.css';
import 'codemirror/addon/lint/lint.css';
import 'styles/css/style.css';

export default class BaseContainer extends Component {

  constructor(props) {
      super(props);
      // Operations usually carried out in componentWillMount go here
      this.state = {
      }
  }

  render() {
      const routes = this.props.routes || [{path: "/", name: 'Home'}]
      return (
        <div>
            <Header onLandingPage={this.props.onLandingPage} />
            <Sidebar />
            {this.props.breadcrumbData ? 
              <div className="page-title-box">
                <div className="container-fluid">
                    <div className="row">
                        <div className="col-sm-12">
                            <h4 className="page-title pull-left">{this.props.breadcrumbData.title}</h4>
                            <Breadcrumbs
                              routes={routes}
                              params={this.props.params}
                              wrapperClass="breadcrumb pull-right"
                              separator=""
                              wrapperElement="ol"
                              itemElement="li"
                            />
                        </div>
                    </div>
                </div>
              </div>
            : null}
            <section className={this.props.onLandingPage === "true" ? "landing-wrapper container" : "container-fluid wrapper animated fadeIn"}>
              {this.props.children}
            </section>
            <Confirm ref="Confirm"/>
        </div>
    )
  }
}
