import React,{Component} from 'react'
import {_} from 'lodash'

class Paginate extends Component{
  constructor(props){
    super(props)
    this.state = {
      index : 0,
      pagesize : props.pagesize || 3,
      fullList :  this.props.len || 0,
      list : this.props.splitData || [],
      oldData : 0
    }
  }

  componentWillReceiveProps(nextProps , nextState){
    const {splitData,len} = nextProps;
    const {fullList,index} = this.state ;
      if(splitData.length > 0){
        if(len !== fullList){
          this.setState({index : 0,oldData : len});
        }
        this.setState({list : splitData, fullList : len});
        return true;
      }else{
        return false;
      }
  }


  next = () => {
    const {list} = this.state;
    let count = this.state.index;
    (count === list.length-1) ? this.setState({index : list.length - 1}) : this.setState({index : ++count});
    this.props.pagePosition(count)
  }
  prev = () => {
    let count = this.state.index;
    (count === 0) ? '' : this.setState({index : --count});
    this.props.pagePosition(count)
  }

  render(){
    const {list,pagesize,index,fullList,oldData} = this.state;
    const pastVal = pagesize*((list[index] === undefined) ? index - 1 : (fullList !== oldData ? 0 : index));


    return(
      <div className="stream-pagination">
        {
          (list.length > 0)
            ? <span>
                <a href="javascript:void(0)" onClick={this.prev}>
                  <i className="fa fa-chevron-left" aria-hidden="true"></i>
                </a>
                <span>{
                    (pastVal === 0 ? 1 : pastVal)
                    +" - "+ (index === (list.length)
                              ? fullList
                              : index === (list.length-1)
                                ? fullList
                                : ((pagesize*index) + pagesize))
                  } of {fullList}</span>
                <a href="javascript:void(0)" onClick={this.next}>
                  <i className="fa fa-chevron-right" aria-hidden="true"></i>
                </a>
              </span> : ''
        }
      </div>
    );
  }
}

export default Paginate;
