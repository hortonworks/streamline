import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import FSReactToastr from '../../components/FSReactToastr';
import Utils from '../../utils/Utils';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants';
import ModelRegistryREST from '../../rest/ModelRegistryREST';

class AddModelRegistry  extends Component{
  constructor(props){
    super(props)
    this.state = {
      jarFile: null,
      validInput : true,
      nameValid : true,
      modelName : this.props.editData.name || '',
      hideInput : this.props.editData.pmmlFileName ? false : true
    }
  }
  validate() {
    const {jarFile, modelName} = this.state;
    let validDataFlag = true;
    if(!jarFile){
      validDataFlag = false;
      this.setState({validInput: false});
    } else if(modelName === ''){
      validDataFlag = false;
      this.setState({nameValid : false});
    } else{
      validDataFlag = true;
      this.setState({validInput : true});
    }
    return validDataFlag;
  }
  handleSave = () => {
    if(!this.validate())
      return;
    const {jarFile, modelName,hideInput} = this.state;
    let formData = new FormData();
    formData.append('pmmlFile', jarFile);
    formData.append('modelInfo', new Blob([JSON.stringify({"name": modelName})], {type: 'application/json'}));
    hideInput ? this.setState({hideInput ,false}) : '';
    return ModelRegistryREST.postModelRegistry({body:formData})
  }
  handleOnJarFileChange = (e) => {
    if(!e.target.files.length || (e.target.files.length && e.target.files[0].name.indexOf('.xml') < 0)){
      this.setState({validInput: false, jarFile: null});
    } else {
      this.setState({jarFile: e.target.files[0]})
    }
  }
  handleNameChange = (event) => {
    if(event.target.value.trim() !== ''){
      this.setState({modelName : event.target.value.trim(),nameValid : true});
    }
  }
  handlePmmlFileChange = () => {
    this.setState({hideInput : true}, () => {
      this.refs.fileUpload.click();
    })
  }
  render(){
    const {validInput,modelName,nameValid,hideInput} = this.state;
    const {pmmlFileName } = this.props.editData;
    return(
      <div className="modal-form config-modal-form">
        <div className="form-group">
          <label>Model Name<span className="text-danger">*</span></label>
          <div>
            <input type="text"
              value={modelName}
              className={nameValid ? "form-control" : "form-control invalidInput"}
              onChange={this.handleNameChange}
              placeholder="Model Name"
            />
          </div>
        </div>
        <div className="form-group">
          <label>Upload PMML File <span className="text-danger">*</span></label>
          <div>
              <input type="text"
                value={pmmlFileName}
                className={hideInput ? "displayNone" : "form-control" }
                onClick={this.handlePmmlFileChange}
                />
              <input type="file"
                ref="fileUpload"
                className={hideInput ? validInput ? "form-control" : "form-control invalidInput" : "hidden"}
                accept=".xml"
                name="jarFiles"
                title="Upload jar"
                onChange={this.handleOnJarFileChange}
              />
          </div>
        </div>
      </div>
    )
  }
}

export default AddModelRegistry;
