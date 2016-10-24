import ReactCodemirror from 'react-codemirror';
import fetch from 'isomorphic-fetch';
import pace from 'pace-progress';

ReactCodemirror.prototype.componentWillReceiveProps =  function(nextProps){

		if (this.codeMirror && nextProps.value !== undefined && this.codeMirror.getValue() != nextProps.value) {
			this.codeMirror.setValue(nextProps.value);
		}
		if (typeof nextProps.options === 'object') {
			for (var optionName in nextProps.options) {
				if (nextProps.options.hasOwnProperty(optionName)) {
					this.codeMirror.setOption(optionName, nextProps.options[optionName]);
				}
			}
		}
}


export function CustomFetch(){
	pace.start()
	const _promise = fetch.apply(this, arguments);
	_promise.then(function(){
		pace.stop()
	}, function(){
		pace.stop()
	})
	return _promise
}