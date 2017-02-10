import React, {Component} from 'react';

export default class AnimatedLoader extends Component{
  constructor(props){
    super(props);
    this.state = {
      progressBar: props.progressbar,
      progressBarColor: props.progressBarColor
    };
  }
  componentDidMount(){
    this.startTimer();
  }
  startTimer(){
    let {progressBar} = this.state;
    setTimeout(()=>{
      this.setState({progressBar: 61},()=>{
        setTimeout(()=>{
          this.setState({progressBar: 81})
        },15000)
      })
    }, 3000)
  }
  render(){
    let {progressBar, progressBarColor, stepText} = this.state;
    if(progressBar == undefined || progressBar == 0){
      progressBar = 11;
    }
    return(
        <div className="wizard-card" data-color={progressBarColor}>
            <div className="wizard-navigation">
                <div className="progress-with-circle">
                    <div className="progress-bar" role="progressbar" aria-valuenow="1" aria-valuemin="1" aria-valuemax="4" style={{width : `${progressBar}%`}}></div>
                </div>
                <ul className="nav nav-pills">
                    <li className={`${progressBar > 10 ? 'active' :'' } col-sm-4`}> <a href="#location" data-toggle="tab" aria-expanded="true">
                        <div className={`icon-circle ${progressBar > 10 ? 'checked' : ''}`}> <i className="fa fa-sitemap"></i> </div>
                       </a>
                    </li>
                    <li className={`${progressBar > 60 ? 'active' :'' } col-sm-4`}> <a href="#type" data-toggle="tab">
                        <div className={`icon-circle ${progressBar > 60 ? 'checked' : ''}`}> <i className="fa fa-archive"></i> </div>
                       </a>
                    </li>
                    <li className={`${progressBar > 80 ? 'active' :'' } col-sm-4`}> <a href="#facilities" data-toggle="tab">
                        <div className={`icon-circle ${progressBar > 80 ? 'checked' : ''}`}> <i className="fa fa-rocket"></i> </div>
                       </a>
                    </li>
                </ul>
            </div>
            <div className="wizard-body">
              <div className="loading">{progressBar > 80 ? "Deploying Topology" : progressBar > 60 ? "Preparing Topology Jar" : "Fetching Cluster Resources"}</div>
            </div>
        </div>
    )
  }
}
