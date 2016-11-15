import React, {Component} from 'react';
import { Button, Form, FormGroup, Col, FormControl, Checkbox, Radio, ControlLabel } from 'react-bootstrap';
import Select, { Creatable } from 'react-select';
import validation from './ValidationRules';
import _ from 'lodash';
import Utils from '../../utils/Utils';
import TopologyREST from '../../rest/TopologyREST';

export class BaseField extends Component {
        type = 'FormField'
    getField = () => {}
    validate (value) {
        let errorMsg = '';
        if(this.props.validation){
            this.props.validation.forEach((v) => {
                if(errorMsg == ''){
                    errorMsg = validation[v](value, this.context.Form, this);
                }else{
                    return;
                }
            })
        }
        const {Form} = this.context;
        Form.state.Errors[this.props.valuePath] = errorMsg;
        Form.setState(Form.state);
        return !errorMsg;
    }
    render() {
        const {className} = this.props
        return (
            <FormGroup className={className}>
                <label>{this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1 ? <span className="text-danger">*</span> : null}</label>
                {this.getField()}
                <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
            </FormGroup>
        );
    }
}

BaseField.contextTypes = {
    Form: React.PropTypes.object,
};


export class string extends BaseField {
    handleChange = () => {
        const value = this.refs.input.value;
        const {Form} = this.context;
        this.props.data[this.props.value] = value;
        Form.setState(Form.state, () => {
            this.validate()
        });
    }
    validate () {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
        return <input
            type="text"
            className={this.context.Form.state.Errors[this.props.valuePath] ? "form-control invalidInput" : "form-control"}
            ref="input"
            value={this.props.data[this.props.value] || ''}
            disabled={this.context.Form.props.readOnly}
            {...this.props.attrs}
            onChange={this.handleChange}
        />
    }
}

export class kafkaTopic extends BaseField {
    constructor(props){
        super(props);
    }

    handleChange () {
        const value = this.refs.input.value;
        const {Form} = this.context;
        this.props.data[this.props.value] = value;
        Form.setState(Form.state);
        if(super.validate(value)){
            // this.validate()
            this.handleTopic()
        }
    }

    handleTopic(){
        let value = this.refs.input.value;
        if(value != ''){
            clearTimeout(this.topicTimer);
            this.topicTimer = setTimeout(()=>{
                this.getSchemaFromTopic(value);
            }, 700)
        }
    }

    getSchemaFromTopic(topicName){
        let resultArr = [];
        TopologyREST.getSchemaForKafka(topicName)
            .then(result=>{
                if(result.responseCode !== 1000){
                    this.refs.input.className = "form-control invalidInput";
                    this.context.Form.state.Errors[this.props.valuePath] = 'Topic name is not present.';
                    this.context.Form.setState(this.context.Form.state);
                } else {
                    this.refs.input.className = "form-control";
                    resultArr = result.entity;
                    if(typeof resultArr === 'string'){
                        resultArr = JSON.parse(resultArr);
                    }
                    this.context.Form.state.Errors[this.props.valuePath] = '';
                    this.context.Form.setState(this.context.Form.state);
                }
                this.context.Form.props.callback(resultArr);
            })
    }

    validate () {
        const err = this.context.Form.state.Errors[this.props.valuePath] || '';
        if(err != ''){
            return false;
        }
        return super.validate(this.refs.input.value);
    }
    getField = () => {
        return <input
            type="text"
            className={this.context.Form.state.Errors[this.props.valuePath] ? "form-control invalidInput" : "form-control"}
            ref="input"
            value={this.props.data[this.props.value]}
            disabled={this.context.Form.props.readOnly}
            {...this.props.attrs}
            onChange={this.handleChange.bind(this)}
        />
    }
}

export class number extends BaseField {
    handleChange = () => {
        const value = parseFloat(this.refs.input.value);
        const {Form} = this.context;
        this.props.data[this.props.value] = value;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
        return <input type="number" className={this.context.Form.state.Errors[this.props.valuePath] ? "form-control invalidInput" : "form-control"} ref="input" value={this.props.data[this.props.value]} disabled={this.context.Form.props.readOnly} {...this.props.attrs} onChange={this.handleChange}/>
    }
}

export class boolean extends BaseField {
    handleChange = () => {
        const checked = this.refs.input.checked;
        const {Form} = this.context;
        this.props.data[this.props.value] = checked;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return true;
    }
    render(){
        const {className} = this.props;
        return <FormGroup className={className}>
            <label>
                <input
                    type="checkbox"
                    ref="input"
                    checked={this.props.data[this.props.value]}
                    disabled={this.context.Form.props.readOnly}
                    {...this.props.attrs}
                    onChange={this.handleChange}
                    style={{marginRight: '10px'}}
                    className={this.context.Form.state.Errors[this.props.valuePath] ? "invalidInput" : ""}
                />
                {this.props.label} {this.props.validation && this.props.validation.indexOf('required') !== -1 ? <span className="text-danger">*</span> : null}
            </label>
        </FormGroup>
    }
}

export class enumstring extends BaseField {
    handleChange = (val) => {
        this.props.data[this.props.value] = val.value;
        const {Form} = this.context;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
        return <Select onChange={this.handleChange} {...this.props.fieldAttr} disabled={this.context.Form.props.readOnly} value={this.props.data[this.props.value]}/>
    }
}

export class arraystring extends BaseField {
    handleChange = (val) => {
        const value = val.map((d) => {
            return d.value
        })
        this.props.data[this.props.value] = value;
        const {Form} = this.context;
        Form.setState(Form.state);
        this.validate()
    }
    validate() {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
        return <Creatable onChange={this.handleChange} multi={true} disabled={this.context.Form.props.readOnly} {...this.props.fieldAttr} value={this.props.data[this.props.value]}/>
    }
}


export class arrayenumstring extends BaseField {
    handleChange = (val) => {
        this.props.data[this.props.value] = val.map(d => {
            return d.value
        });
        const {Form} = this.context;
        Form.setState(Form.state);
        this.validate()
    }
    validate () {
        return super.validate(this.props.data[this.props.value])
    }
    getField = () => {
        return <Select onChange={this.handleChange} multi={true} disabled={this.context.Form.props.readOnly} {...this.props.fieldAttr} value={this.props.data[this.props.value]}/>
    }
}

export class object extends BaseField {
    handleChange = () => {
        this.props.data[this.props.value] = this.refs.input.value;
        const {Form} = this.context;
        Form.setState(Form.state);
        // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
    }
    validate () {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    render() {
        const {className} = this.props;
        return (
            <fieldset className={className + " fieldset-default"}>
                <legend>{this.props.label}</legend>
                {this.getField()}
            </fieldset>
        );
    }
    getField = () => {
        return this.props.children.map((child, i) => {
                    return React.cloneElement(child, {
                        ref: child.props ? (child.props._ref || i) : i,
                        key: i,
                        data: this.props.data[this.props.value]
                    });
                })
    }
}

export class arrayobject extends BaseField {
    handleChange = () => {
        // this.context.Form.setValue(this.props.value, this.refs.input.value, this);
    }
    validate = () => {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    onAdd = () => {
        this.props.data[this.props.value].push({});
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    render() {
        const {className} = this.props;
        return (
            <fieldset className={className + " fieldset-default"}>
                <legend>{this.props.label} {this.context.Form.props.readOnly ? '' : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
                {this.getField()}
            </fieldset>
        );
    }
    getField = () => {
        const fields = this.props.fieldJson.fields;
        this.props.data[this.props.value] = this.props.data[this.props.value] || [{}];
        return this.props.data[this.props.value].map((d, i) => {
            const optionsFields = Utils.genFields(fields, [...this.props.valuePath.split('.'), i], d)
            return optionsFields.map((child, i) => {
                return React.cloneElement(child, {
                    ref: child.props ? (child.props._ref || i) : i,
                    key: i,
                    data: d
                });
            })})
    }
}

export class enumobject extends BaseField {
    constructor(props){
        super(props)
        if(props.list){
            this.data = props.data;
        }else{
            this.data = this.props.data[this.props.value]
        }
    }
    handleChange = (val) => {
        delete this.data[Object.keys(this.data)[0]];
        this.data[val.value] = {}
        this.validate(val.value)
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    validate = () => {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    getField = () => {
        const value = Object.keys(this.data)[0];
        return <Select onChange={this.handleChange} {...this.props.fieldAttr} value={value}/>
    }
    render() {
        const value = Object.keys(this.data)[0];
        const selected = _.find(this.props.fieldJson.options, (d) => {
            return d.fieldName == value;
        })
        const optionsFields = Utils.genFields(selected.fields, this.props.valuePath.split('.'),this.data)
        const {className}=this.props;
        return (
            <div className={className}>
                <FormGroup>
                    <Col componentClass={ControlLabel} sm={2}>{this.props.label}</Col>
                    <Col sm={10}>
                        {this.getField()}
                        <p className="text-danger">{this.context.Form.state.Errors[this.props.valuePath]}</p>
                    </Col>
                </FormGroup>
                {optionsFields.map((child, i) => {
                    return React.cloneElement(child, {
                        ref: child.props ? (child.props._ref || i) : i,
                        key: i,
                        data: this.data[value]
                    });
                })}
            </div>
        );
    }
}

const EnumObject = enumobject;

export class arrayenumobject extends BaseField {
    onAdd = () => {
        this.props.data[this.props.value].push({
            [this.props.fieldJson.options[0].fieldName] : {}
        });
        const {Form} = this.context;
        Form.setState(Form.state);
    }
    validate = () => {
        const {Form} = this.context;
        return Form.validate.call(this);
    }
    render() {
        const {className} = this.props
        return (
            <fieldset className={className + " fieldset-default"} >
                <legend>{this.props.label} {this.context.Form.props.readOnly ? '' : <i className="fa fa-plus" aria-hidden="true" onClick={this.onAdd}></i>}</legend>
                {this.getField()}
            </fieldset>
        );
    }
    getField = () => {
        const fields = this.props.fieldJson.options;
        this.props.data[this.props.value] = this.props.data[this.props.value] || [{
            [this.props.fieldJson.options[0].fieldName] : {}
        }];
        return this.props.data[this.props.value].map((d, i) => {
            return <EnumObject {...this.props} ref={this.props.valuePath+'.'+i} key={i} data={d} valuePath={[...this.props.valuePath.split('.'), i].join('.')} list={true}/>
        })
    }
}
