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
            } else if(value == '' || (typeof value == 'string' && value.trim() == '') || _.isUndefined(value)){
                return 'Required!';
            }else{
              return '';
            }
        }
    },
    email: (value, form, component) => {
        if(!value.trim()){
          return false;
        }else{
          if(value instanceof Array){
            return false;
          }else if(value == '' || (typeof value == 'string' && value.trim() == '') || _.isUndefined(value)){
              return false;
          }else{
            let result = '';
            const pattern = /[a-z0-9](\.?[a-z0-9_-]){0,}@[a-z0-9-]+\.([a-z]{1,6}\.)?[a-z]{2,6}$/;
            pattern.test(value) ? result : result = "Invalid Email";
            return result;
          }

        }
    }
}

export default ValidationRules;
