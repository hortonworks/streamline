import {observable} from 'mobx';

class app_state {
    @observable sidebar_isCollapsed = true
    @observable sidebar_activeKey = ''
    @observable sidebar_toggleFlag = false
    @observable streamline_config = {registry: {}, pivot: {}}
	@observable showComponentNodeContainer = true
}

export default new app_state()