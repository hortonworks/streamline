define(['marionette', 'utils/LangSupport'], function(Marionette, localization) {
  /*
   * Localization initialization
   */
  localization.setDefaultCulture(); // will take default that is en
  localization.chooseCulture();

  window._preventNavigation = false;
  var App = new Marionette.Application();

  App.addRegions({
    rHeader : '#header',
    rContent : '#page-content-wrapper'
  });

  App.addInitializer(function() {
    Backbone.history.start();
  });

  return App;
});