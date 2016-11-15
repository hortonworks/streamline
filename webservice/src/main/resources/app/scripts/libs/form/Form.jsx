import React, {Component} from 'react';
import _ from 'lodash';
import { Button, Form, FormGroup, Col, FormControl, Checkbox, Radio, ControlLabel } from 'react-bootstrap';

export default class FSForm extends Component {
    /*state = {
        FormData: {},
        Errors: {}
    }*/
    constructor(props){
        super(props)
        this.state = {
            FormData: props.FormData,
            Errors: props.Errors
        };
    }
    componentWillReceiveProps = (nextProps) => {
        if(this.props.FormData != nextProps.FormData){
            this.updateFormData(nextProps.FormData)
        }
    }
    updateFormData(newFormData){

        for(let key in this.state.Errors){
            delete this.state.Errors[key];
        }

        this.setState({
            FormData: _.assignInWith(this.state.FormData, _.cloneDeep(newFormData)),
            Errors: this.state.Errors
        });
    }
    getChildContext() {
        return {
            Form: this,
        };
    }
    render(){
        return (<Form className={this.props.className} style={this.props.style}>
                {this.props.children.map((child, i) => {
                    return React.cloneElement(child, {
                        ref: child.props ? (child.props._ref || i) : i,
                        key: i,
                        data: this.state.FormData,
                        className: this.props.showRequired ? !child.props.fieldJson.isOptional ? '' : 'hidden' : child.props.fieldJson.isOptional ? '' : 'hidden' ,
                    });
                })}
            </Form>)
    }
    validate () {
        let isFormValid = true;
        for(let key in this.refs){
            let component = this.refs[key];
            if(component.type == "FormField"){
                let isFieldValid = component.validate();
                if(isFormValid){
                    isFormValid = isFieldValid;
                }
            }
        }

        return isFormValid;
    }
}

FSForm.defaultProps = {
    showRequired: true,
    readOnly: false,
    Errors: {},
    FormData: {}
}

FSForm.childContextTypes = {
    Form: React.PropTypes.object,
};