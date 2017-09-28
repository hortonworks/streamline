/**
  * Copyright 2017 Hortonworks.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *   http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
**/

import React, {Component} from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import {
  Button,
  Form,
  FormGroup,
  Col,
  FormControl,
  Checkbox,
  Radio,
  ControlLabel
} from 'react-bootstrap';

export default class FSForm extends Component {
  constructor(props) {
    super(props);
    this.state = {
      FormData: props.FormData,
      Errors: props.Errors
    };
  }
  componentDidMount = () => {}
  componentWillReceiveProps = (nextProps) => {
    if (this.props.FormData != nextProps.FormData) {
      this.updateFormData(nextProps.FormData);
    }
  }

  updateFormData(newFormData) {
    this.setState({
      FormData: _.assignInWith(this.state.FormData, _.cloneDeep(newFormData)),
      Errors: this.state.Errors
    });
  }

  clearErrors(){
    for (let key in this.state.Errors) {
      delete this.state.Errors[key];
    }
    this.forceUpdate();
  }

  getChildContext() {
    return {Form: this};
  }
  render() {
    return (
      <Form className={this.props.className} style={this.props.style}>
        {this.props.children.map((child, i) => {
          let className = this.props.showRequired == null ? '' :
            (this.props.showRequired ?
              (!child.props.fieldJson.isOptional ? '' : 'hidden') :
                (child.props.fieldJson.isOptional ? '' : 'hidden')
            );
          if(this.props.showRequired === false){
            if(this.props.showSecurity) {
              className = child.props.fieldJson.hint && child.props.fieldJson.hint.indexOf('security_') > -1 ? '' :'hidden';
            } else {
              className = child.props.fieldJson.isOptional ? '' :'hidden';
              if(child.props.fieldJson.hint && child.props.fieldJson.hint.indexOf('security_') > -1) {
                className = 'hidden';
              }
            }
          }
          if(this.props.showRequired == null){
            if(this.props.showSecurity == true) {
              className = child.props.fieldJson.hint && child.props.fieldJson.hint.indexOf('security_') > -1 ? '' :'hidden';
            } else {
              className = child.props.fieldJson.hint && child.props.fieldJson.hint.indexOf('security_') > -1 ? 'hidden' :'';
            }
          }
          if(this.props.showRequired === true && child.props.fieldJson.isOptional === false ){
            className = child.props.fieldJson.hint && child.props.fieldJson.hint.indexOf('security_ssl_required') !== -1
                        ? 'hidden'
                        : child.props.fieldJson.hint && child.props.fieldJson.hint.indexOf('security_kerberos_required') !== -1
                          ? 'hidden'
                          : '';
          }
          return React.cloneElement(child, {
            ref: child.props
              ? (child.props._ref || i)
              : i,
            key: i,
            data: this.state.FormData,
            className: className
          });
        })}
      </Form>
    );
  }
  validate() {
    let isFormValid = true;
    const invalidFields = [];
    for (let key in this.refs) {
      let component = this.refs[key];
      if (component.type == "FormField") {
        let isFieldValid = false;
        if (component.props.fieldJson.type === "number") {
          const val = component.props.data[key];
          if ((_.isNaN(val) || _.isUndefined(val)) && component.props.fieldJson.isOptional) {
            isFieldValid = true;
          } else {
            const min = component.props.fieldJson.min === undefined
              ? 0
              : component.props.fieldJson.min;
            const max = component.props.fieldJson.max === undefined
              ? Number.MAX_SAFE_INTEGER
              : component.props.fieldJson.max;
            isFieldValid = (val >= min && val <= max)
              ? true
              : false;
          }
        } else if(component.props.fieldJson.type === "file"){
          isFieldValid = component.props.fieldJson.hint.indexOf(component.props.value.split('.')[component.props.value.split('.').length - 1]) !== -1 ? true : false;
        } else {
          isFieldValid = component.validate();
          if(_.isObject(isFieldValid)){
            isFieldValid = isFieldValid.isFormValid;
          }
        }
        if (isFormValid) {
          isFormValid = isFieldValid;
        }
        if(!isFieldValid){
          invalidFields.push(component);
        }
      }
    }

    return {isFormValid, invalidFields};
  }
}

FSForm.defaultProps = {
  showRequired: true,
  readOnly: false,
  Errors: {},
  FormData: {}
};

FSForm.childContextTypes = {
  Form: PropTypes.object
};
