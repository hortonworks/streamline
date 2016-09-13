/**
 * React-Utils: WindowSize mixin
 * @author Denis Izmaylov
 * @url https://github.com/DenisIzmaylov
 * @example
 *   module.exports = React.createClass({displayName: 'MyComponent1',
 *     mixins: [ReactUtils.Mixins.WindowSizeWatch],
 *     onWindowResize: function (event) {
 *       console.log(event.width, event.height);
 *     }
 *   });
 */
var documentElement = window.document.documentElement;

module.exports = {
    componentDidMount: function () {
        window.addEventListener('resize', this._onWindowResizeMixinHandler);
    },
    componentWillUnmount: function () {
        window.removeEventListener('resize', this._onWindowResizeMixinHandler)
    },
    _onWindowResizeMixinHandler: function () {
        if (typeof this.onWindowResize === 'function') {
            var event = {
                width: documentElement['clientWidth'],
                height: documentElement['clientHeight']
            };
            this.onWindowResize(event);
        }
    }
};