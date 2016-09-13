import {observable} from 'mobx';

class app_state {
	@observable sidebar = {
		show: false,
		activeItem: ''
	}

	@observable showComponentNodeContainer = true
}

export default new app_state()