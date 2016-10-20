import React, {Component} from 'react';
import Breadcrumbs from 'react-breadcrumbs';
import {Link} from 'react-router'

export default class Footer extends Component {
    render() {
      const {params , routes} = this.props;
        return (
            <footer>
                <Breadcrumbs
                  routes={routes}
                  params={params}
                  wrapperClass="breadcrumb pull-left"
                  separator=""
                  wrapperElement="ol"
                  itemElement="li"
                />
            </footer>
        );
  }
}
