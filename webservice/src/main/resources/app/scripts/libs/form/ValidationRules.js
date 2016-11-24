import _ from 'lodash';

let ValidationRules = {
        required: (value, form, component) => {
            if(value.trim() == '' || _.isUndefined(value)){
                return 'Required!'
            }
        }
}

export default ValidationRules;