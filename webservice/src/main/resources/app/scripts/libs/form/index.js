import Form, {genFields} from './Form';
import * as fields from './Fields';
// import {BaseField, string, Select2} from './Fields';
// const Fields = {BaseField, string, Select2}

Form.Fields = fields;

export default Form;
export {genFields};