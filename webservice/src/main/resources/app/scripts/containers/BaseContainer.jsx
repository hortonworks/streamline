import React, {Component} from 'react';
import Header from '../components/Header';
import Footer from '../components/Footer';
import Sidebar from '../components/Sidebar';
import {Link} from 'react-router';
import {Confirm} from '../components/FSModal'

export default class BaseContainer extends Component {

  constructor(props) {
      super(props);
      // Operations usually carried out in componentWillMount go here
      this.state = {}
  }
  handleKeyPress = (event) => {
    event.key === "Enter"
      ? this.refs.Confirm.state.show ? this.refs.Confirm.sure() : ''
      : event.key === "Escape"
        ? this.refs.Confirm.state.show ? this.refs.Confirm.cancel() : ''
        : '';
  }
  render(){
    const routes = this.props.routes || [{path: "/", name: 'Home'}]
    return(
        <div>
            <Header
              onLandingPage={this.props.onLandingPage}
              headerContent={this.props.headerContent}
            />
          <Sidebar routes={routes}/>
            <section className="content-wrapper editorHandler">
              {this.props.children}
            </section>
            <Confirm ref="Confirm"
              onKeyUp={this.handleKeyPress}
              />
        </div>
    );
  }
}
