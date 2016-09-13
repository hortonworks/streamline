/**
 * React-Utils: ViewportWatch mixin
 * @author Denis Izmaylov
 * @url https://github.com/DenisIzmaylov
 * @example
 *   module.exports = React.createClass({displayName: 'MyComponent1',
 *     mixins: [ReactUtils.Mixins.ViewportWatch],
 *     onViewportChange: function (viewport) {
 *       console.log(viewport.scrollLeft, viewport.scrollTop);
 *       console.log(viewport.innerWidth, viewport.innerHeight);
 *       console.log(viewport.outerWidth, viewport.outerHeight);
 *     }
 *   });
 */
var documentElement = window.document.documentElement;
var documentBody = window.document.body;

module.exports = {
    componentDidMount: function () {
        window.addEventListener('resize', this._onViewportChangeMixinHandler);
        window.addEventListener('scroll', this._onViewportChangeMixinHandler);
    },
    componentWillUnmount: function () {
        window.removeEventListener('resize', this._onViewportChangeMixinHandler);
        window.removeEventListener('scroll', this._onViewportChangeMixinHandler);
    },
    _onViewportChangeMixinHandler: function () {
        if (typeof this.onViewportChange === 'function') {
            var innerWidth = Math.max(
                documentElement.scrollWidth, documentBody.scrollWidth,
                documentElement.offsetWidth, documentBody.offsetWidth,
                documentElement.clientWidth
            );
            var innerHeight = Math.max(
                documentElement.scrollHeight, documentBody.scrollHeight,
                documentElement.offsetHeight, documentBody.offsetHeight,
                documentElement.clientHeight
            );
            var scrollLeft = window.pageXOffset || documentElement.scrollLeft;
            var scrollTop = window.pageYOffset || documentElement.scrollTop;
            var event = {
                scrollLeft: scrollLeft,
                scrollTop: scrollTop,
                innerWidth: innerWidth,
                innerHeight: innerHeight,
                outerWidth: documentElement['clientWidth'],
                outerHeight: documentElement['clientHeight']
            };
            this.onViewportChange(event);
        }
    }
};