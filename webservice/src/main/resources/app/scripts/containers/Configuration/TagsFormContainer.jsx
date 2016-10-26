import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import TagREST from '../../rest/TagREST';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants'

export default class TagsFormContainer extends Component {
	constructor(props){
		super(props);
		this.state = {
			name: '',
			description: '',
			tagIds: null
		};
		if(this.props.currentId){
			this.fetchData(this.props.currentId);
		}
	}

	fetchData(id){
		TagREST.getTag(id)
			.then((tags)=>{
				if(tags.responseCode !== 1000){
          FSReactToastr.error(
              <CommonNotification flag="error" content={tags.responseMessage}/>, '', toastOpt)
				} else {
					let {name, description, tagIds} = tags.entity;
					this.setState({name, description, tagIds});
				}
			})
			.catch((err)=>{
        FSReactToastr.error(
            <CommonNotification flag="error" content={err}/>, '', toastOpt)
			})
	}

	handleValueChange(e){
		let obj = {};
		obj[e.target.name] = e.target.value;
		this.setState(obj);
	}

	validateData(){
		let {name, description} = this.state;
		if(name !== '' && description !== ''){
			return true;
		} else {
      (this.state.name.length === 0 ) ? this.nameRef.setAttribute('class',"form-control error") : this.nameRef.setAttribute('class',"form-control");
      (this.state.description.length === 0 ) ? this.desc.setAttribute('class',"form-control error") : this.desc.setAttribute('class',"form-control");
			return false;
		}
	}

	handleSave(){
		let {name, description, tagIds} = this.state;
		let {parentId, currentId} = this.props;
		let data = {name, description, tagIds};
		if(currentId){
			return TagREST.putTag(this.props.currentId, {body: JSON.stringify(data)});
		} else {
			data.tagIds = parentId ? [parentId] : null;
			return TagREST.postTag({body: JSON.stringify(data)});
		}
	}

	render() {
		return (
			<form className="form-horizontal">
				<div className="form-group">
                                        <label className="col-sm-12 control-label text-left">Name*</label>
                                        <div className="col-sm-12">
                                                <input
              ref={(ref) => this.nameRef = ref}
							name="name"
							placeholder="Name"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.name}
							required={true}
						/>
					</div>
                                        {/*this.state.name === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Name</p>
						</div>
                                        : null*/}
				</div>

				<div className="form-group">
                                        <label className="col-sm-12 control-label text-left">Description*</label>
                                        <div className="col-sm-12">
						<input
              ref={(ref) => this.desc = ref}
							name="description"
							placeholder="Description"
							onChange={this.handleValueChange.bind(this)}
							type="text"
							className="form-control"
							value={this.state.description}
						    required={true}
						/>
					</div>
                                        {/*this.state.description === '' ?
						<div className="col-sm-4">
							<p className="form-control-static error-note">Please Enter Description</p>
						</div>
                                        : null*/}
				</div>
			</form>
		)
	}
}
