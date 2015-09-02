define(['marionette', 'utils/LangSupport'], function(Marionette, localization) {
  /*
   * Localization initialization
   */
  localization.setDefaultCulture(); // will take default that is en
  localization.chooseCulture();

  var App = new Marionette.Application();

  App.addRegions({
    rHeader : '#header',
    rSideBar : '#sidebar-wrapper',
    // rFooter : '#footer',
    rContent : '#page-content-wrapper'
  });

  App.addInitializer(function() {
    Backbone.history.start();
  });

  return App;
});