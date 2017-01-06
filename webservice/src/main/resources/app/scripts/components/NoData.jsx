import React, {Component} from 'react';
import {Link} from 'react-router';
import _ from 'lodash';

export default class NoData extends Component{
  render(){
    const {imgName} = this.props;
    const tempArr = ["services","environments","applications"];
    const index = _.findIndex(tempArr , function(o) { return o == 'services';})
    const imgUrl = `styles/img/back-${imgName}.png`;
    const divStyle = {
      backgroundImage : 'url(' + imgUrl + ')',
      backgroundRepeat : "no-repeat",
      backgroundPosition :`${(index !== -1) ? "right" : "center" } top`,
      backgroundSize : "50%",
      height : window.innerHeight - 124
    }
      return (
        <div className={`col-sm-12 ${ (index !== -1) ? "" : "text-center"}`} style={divStyle}>
          { (index !== -1)
            ? <div className="row">
              <div className="col-md-9 col-md-offset-2 intro-section">
                  <h4 className="intro-section-title">3 Easy Steps to get started...</h4>
                  <div className="list">
                    <h4><span className="hb success">1</span> Go to <Link to="/service-pool">Service Pool</Link></h4>
                    <div className="intro-content">
                      Add Services by connecting to <br/> one or more Ambari instances
                    </div>
                  </div>
                  <div className="list">
                    <h4><span className="hb success">2</span> Go to <Link to="/environments">Environments</Link></h4>
                    <div className="intro-content">
                      Build at least one environment <br/> pulling from the Service Pool
                    </div>
                  </div>
                  <div className="list">
                    <h4><span className="hb success">3</span> Go to <Link to="/">Application</Link></h4>
                    <div className="intro-content">
                      Add your first Application...
                    </div>
                  </div>
                </div>
            </div>
          :<p className="noDataFound-text">No Data Found</p>
          }
        </div>
      )
  }
}
