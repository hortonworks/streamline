import React, {Component} from 'react';

export default class CommonLoaderSign extends Component{
  render(){
    const {imgName} = this.props;
    const imgUrl = `styles/img/back-${imgName}.png`;
    const divStyle = {
      backgroundImage : 'url(' + imgUrl + ')',
      backgroundRepeat : "no-repeat",
      backgroundPosition : "center top",
      backgroundSize : "50%",
      height : window.innerHeight - 124
    }
    return(
        <div className="col-sm-12 text-center" style={divStyle}>
            <p className="loading-text">Loading</p>
        </div>
    )
  }
}
