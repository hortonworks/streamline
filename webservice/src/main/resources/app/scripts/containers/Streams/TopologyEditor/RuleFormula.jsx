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
import {OverlayTrigger, Popover} from 'react-bootstrap';
import {sqlKeywords,binaryOperators} from '../../../utils/Constants';
import ProcessorUtils from '../../../utils/ProcessorUtils';
import CommonCodeMirror from '../../../components/CommonCodeMirror';

export default class RuleFormula extends Component{
  constructor(props){
    super(props);
    this.fields = [];
    props.fields.map((f) => {
      this.fields = [
        ...f.fields
      ];
    });
    this.state = {
      data: '',
      errorMsg: '',
      show: false
    };
    this.populateCodeMirrorHintOptions(this.fields);
  }

  populateCodeMirrorHintOptions(fields){
    const {udfList} = this.props;
    this.hintOptions=[];
    // arguments from field list for hints...
    Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(fields,"ARGS"));
    // Predefined Sql keywords from CONSTRANT for hints...
    Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(sqlKeywords,"SQL"));
    // Predefined Binary Operators from CONSTRANT for hints...
    Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(binaryOperators,"BINARY-OPERATORS"));
    // FUNCTION from UDFLIST for hints...
    Array.prototype.push.apply(this.hintOptions,ProcessorUtils.generateCodeMirrorOptions(udfList,"FUNCTION"));
  }

  componentDidMount() {
    if (this.props.sql || this.props.condition) {
      this.prepopulate();
    }
  }

  prepopulate = () => {
    const {condition} = this.props;
    this.setState({data : condition});
  }

  validateRule = () => {
    let flag = true; // change this to false after you integrate validation..
    const {data} = this.state;
    const {udfList} = this.props;
    let streamName = this.props.fields[0].streamId;
    this.sqlStrQuery = "select * from " + streamName + " where ";
    this.conditionStr = this.ruleCondition = data;
    this.sqlStrQuery += data;
    // const t = this.validate(udfList,this.fields,data);
    // flag = (typeof t === "object") ? false : true;
    // this.setState({show : !flag ? true : false, errorMsg : !flag ? t.message : '' });
    return flag;
  }

  handleChangeQuery(val) {
    this.setState({data : val});
  }

  render(){
    let {data,errorMsg,show} = this.state;
    const {fields,ruleObj} = this.props;
    const flag = !_.isEmpty(ruleObj) && ruleObj.id ? true : false;
    return (
      <div>
        {show
          ? <Alert bsStyle="danger">{errorMsg}</Alert>
          : null}
        <div className="form-group">
          <OverlayTrigger trigger={['hover']} placement="right" overlay={<Popover id="popover-trigger-hover">Create condition</Popover>}>
            <label>Create condition:</label>
          </OverlayTrigger>
          <OverlayTrigger trigger={['hover']} placement="left" overlay={<Popover id="popover-trigger-hover">Type @ to see all the available options</Popover>}>
            <i className="fa fa-info-circle pull-right" style={{backgroundColor : "#ffffff" ,color: '#1892c1'}}></i>
          </OverlayTrigger>
          <div>
            <CommonCodeMirror editMode={flag} panelText={fields.length ? `select * from ${fields[0].streamId} where` : ''} modeType="sql" hintOptions={this.hintOptions} value={data} placeHolder="Sql condition goes here..." callBack={this.handleChangeQuery.bind(this)} />
          </div>
        </div>
      </div>
    );
  }
}
