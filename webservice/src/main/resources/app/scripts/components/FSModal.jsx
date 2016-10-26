import React, {Component} from 'react';
import {Modal, Button} from 'react-bootstrap'

const defaultState = {
    show: false,
    title: '',
    btnOkText: '',
    btnCancelText: '',
}

export default class FSModal extends Component {
    state = defaultState
    show(){
        var state = state || {}
        state.show = true;
        this.setState(state);
    }
    sure(){
        let resolve = this.props["data-resolve"];
        if(resolve){
            resolve()
        }
    }
    cancel(){
        let {reject} = this.props;
        if(reject){
            reject()
        }else{
            this.hide()
        }
    }
    hide(){
        this.setState({show: false});
    }
    header(){
        return (
            <Modal.Header closeButton>
                <Modal.Title>
                    {this.props["data-title"]}
                </Modal.Title>
            </Modal.Header>
        );
    }
    body(){
        return (
            <Modal.Body>
                {this.props.children}
            </Modal.Body>
        );
    }
    footer(){
        return (
            <Modal.Footer>
                <Button bsStyle='default' onClick={this.cancel.bind(this)}>
                    {this.state.btnCancelText || 'Cancel'}
                </Button>
                <Button bsStyle='success' onClick={this.sure.bind(this)}>
                    {this.state.btnOkText || 'Ok'}
                </Button>
            </Modal.Footer>
        );
    }
    render() {
        return (
            <Modal aria-labelledby='contained-modal-title' backdrop="static" keyboard={false} onHide={this.cancel.bind(this)} show={this.state.show} {...this.props}>
                {this.header()}
                {this.body()}
                {this.footer()}
            </Modal>
        );
    }
}

var _resolve;
var _reject;

export class Confirm extends FSModal {
    show(state){
        var state = state || {}
        state.show = true;
        this.setState(state);
        let promise = new Promise(function(resolve, reject){
            _resolve = resolve;
            _reject = reject;
        })
        return promise;
    }
    sure(){
        _resolve(this);
    }
    cancel(){
        _reject(this);
        this.setState(defaultState)
    }
    header(){
        return (
            <Modal.Header closeButton>
                <Modal.Title>
                    {this.state.title}
                </Modal.Title>
            </Modal.Header>
        );
    }
    body(){
        return '';
    }
    footer(){
        return (
            <Modal.Footer>
                <Button bsStyle='danger' onClick={this.cancel.bind(this)}>
                    {this.state.btnCancelText || 'No'}
                </Button>
                <Button bsStyle='success' onClick={this.sure.bind(this)}>
                    {this.state.btnOkText || 'Yes'}
                </Button>
            </Modal.Footer>
        );
    }
}
