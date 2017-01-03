import React, {Component}from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import FSReactToastr from '../../components/FSReactToastr';
import CommonNotification from '../../utils/CommonNotification';
import {toastOpt} from '../../utils/Constants'
import TopologyREST from '../../rest/TopologyREST';
import ReactCodemirror from 'react-codemirror';
import CodeMirror from 'codemirror';
import 'codemirror/mode/javascript/javascript';
import jsonlint from 'jsonlint';
import lint from 'codemirror/addon/lint/lint';

CodeMirror.registerHelper("lint", "json", function(text) {
  var found = [];
  var {parser} = jsonlint;
  parser.parseError = function(str, hash) {
    var loc = hash.loc;
    found.push({from: CodeMirror.Pos(loc.first_line - 1, loc.first_column),
                to: CodeMirror.Pos(loc.last_line - 1, loc.last_column),
                message: str});
  };
  try { jsonlint.parse(text); }
  catch(e) {}
  return found;
});

export default class EditDefinition extends Component{
  constructor(props){
    super(props)
    this.state = {
      mavenDeps : this.props.editData.mavenDeps,
      editDataObj : this.props.editData,
      entity : []
    }
  }
  componentDidMount(){
    this.customData();
  }
  customData = () => {
    let obj = Object.assign({},this.props.editData);
    let tempArr  = [];
    let mvn = {mavenDeps :obj.mavenDeps };
    let fields ={topologyComponentUISpecification : obj.topologyComponentUISpecification};
    delete obj.id;
    delete obj.mavenDeps;
    delete obj.topologyComponentUISpecification;
      _.keys(obj).map((x) => {
          tempArr.push({ [x] : obj[x]});
      });
      tempArr.push(fields);
      tempArr.unshift(mvn);
    this.setState({entity : tempArr});
  }
  handleChange = (e) => {
    let arr = this.state.entity;
    arr[0].mavenDeps = e.target.value.trim();
    this.setState({mavenDeps : e.target.value.trim(), entity: arr});
  }
  handleSave = () => {
    const data = this.state.editDataObj;
    if(this.state.mavenDeps == ''){
      delete data.mavenDeps;
    } else {
      data.mavenDeps = this.state.mavenDeps;
    }

    delete data.timestamp;

    let formData = new FormData();
    formData.append("topologyComponentBundle", JSON.stringify(data));

    return TopologyREST.putComponentDefination(data.type,data.id,{body: formData})
  }

  render(){
    const {mavenDeps,entity} = this.state;
    const jsonoptions = {
      lineNumbers: true,
      mode: "application/json",
      styleActiveLine: true,
      gutters: ["CodeMirror-lint-markers"],
      lint: true,
      readOnly: 'nocursor'
        };

    return(
      <form className="form-horizontal">
        {
          entity.map((x,i) => {
            if(x.topologyComponentUISpecification){
              return  <div className="form-group" key={i}>
                        <label className="col-sm-12 control-label">{_.keys(x)[0]}</label>
                          <div className="col-sm-12">
                            <ReactCodemirror
                              ref="JSONCodemirror"
                              value={JSON.stringify(x.topologyComponentUISpecification,"",2)}
                              options={jsonoptions}
                            />
                          </div>
                      </div>
            }else{
              return <div className="form-group" key={i}>
                      <label className="col-sm-12 control-label">{_.keys(x)[0]}</label>
                      <div className="col-sm-12">
                        <input
                          ref={_.keys(x)[0]}
                          disabled={this.props.viewMode ? true : _.keys(x)[0] === "mavenDeps" ? false : true}
                          type="text"
                          className="form-control"
                          value={(x[_.keys(x)[0]]) || ''}
                          onChange={this.handleChange}
                        />
                      </div>
                    </div>
            }
          })
        }
      </form>
    )
  }
}
