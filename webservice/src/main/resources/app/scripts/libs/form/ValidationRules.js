import _ from 'lodash';

let ValidationRules = {
        required: (value, form, component) => {
		if(value){
			if(value instanceof Array && value.length === 0){
				return 'Required!'
			} else if(value.trim() == '' || _.isUndefined(value)){
                        return 'Required!'
                    }
		}
        }
}

export default ValidationRules;