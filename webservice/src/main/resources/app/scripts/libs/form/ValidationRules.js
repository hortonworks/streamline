import _ from 'lodash';

let ValidationRules = {
    required: (value, form, component) => {
        if(!value){
            return 'Required!';
        } else {
            if(value instanceof Array){
                if(value.length === 0){
                    return 'Required!';
                }
            } else if(value.trim() == '' || _.isUndefined(value)){
                return 'Required!';
            }
        }
    }
}

export default ValidationRules;