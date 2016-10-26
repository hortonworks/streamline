import React, {Component} from 'react'
import {notifyTextLimit} from '../utils/Constants'

class CommonNotification extends Component {
    constructor(props) {
        super(props)
        this.state = {
            data: false,
            text: "Read more"
        }
    }
    showMore = () => {
        if (this.state.text === "Read more") {
            this.setState({text: "Hide", data: true})
        } else {
            this.setState({text: "Read more", data: false})
        }
    }

    render() {
        /* flag value         error, info, sucess */
        const {text, data} = this.state;
        const {flag, content} = this.props;
        const initial = content.substr(0, notifyTextLimit)
        const moreText = content.substr(notifyTextLimit)
        const readMoreTag = <a href="javascript:void(0)" onClick={this.showMore}>{text}</a>
        return (
            <div>
                {initial}
                {(data)
                    ? moreText
                    : null
}
                <div>
                    {(flag === 'error' && moreText.length > 0)
                        ? readMoreTag
                        : null
}
                </div>
            </div>
        );
    }
}

export default CommonNotification;

CommonNotification.propTypes = {
    flag: React.PropTypes.string.isRequired,
    content: React.PropTypes.string
}
