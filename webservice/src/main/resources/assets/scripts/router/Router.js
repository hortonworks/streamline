define([
	'jquery',
	'underscore',
	'backbone',
	'App',
	'models/VAppState',
	'utils/Globals'
], function($, _, Backbone, App, VAppState, Globals) {
	var AppRouter = Backbone.Router.extend({
		routes: {
			// Define some URL routes
			''						: 'dashboardAction',
			'!/dashboard'			: 'dashboardAction',
			'!/datasource'			: 'datasourceAction',
			'!/parser-registry'		: 'parserRegistryAction',
			'!/device-catalog'		: 'deviceCatalogAction',

			// Default
			'*actions': 'defaultAction'
		},

		initialize: function() {
			this.showRegions();
			this.listenTo(this, 'route', this.postRouteExecute, this);
		},

		showRegions: function() {
			require(['views/site/Header','views/site/Sidebar'],function(HeaderView, SidebarView){
				App.rHeader.show(new HeaderView());
				App.rSideBar.show(new SidebarView({
					appState: VAppState
				}));
			});
		},

		/**
		 * @override
		 * Execute a route handler with the provided parameters. This is an
		 * excellent place to do pre-route setup or post-route cleanup.
		 * @param  {Function} callback - route handler
		 * @param  {Array}   args - route params
		 */
		execute: function(callback, args) {
			this.preRouteExecute();
			if (callback) callback.apply(this, args);
			this.postRouteExecute();
		},

		preRouteExecute: function() {
			console.log("Pre-Route Change Operations can be performed here !!");
		},

		postRouteExecute: function(name, args) {
			console.log("Post-Route Change Operations can be performed here !!");
			console.log("Route changed: ", name);
		},

		/**
		 * Define route handlers here
		 */
		dashboardAction: function() {
			VAppState.set({
				'currentTab' : Globals.AppTabs.Dashboard.value
			});
			require(['views/site/Dashboard'],function(DashboardView){
				App.rContent.show(new DashboardView());
			});
		},

		datasourceAction: function() {
			VAppState.set({
				'currentTab' : Globals.AppTabs.Datasource.value
			});
			require(['views/datasource/DatasourceView'], function(DatasourceView){
				App.rContent.show(new DatasourceView());
			});
		},
		
		parserRegistryAction: function(){
			VAppState.set({
				'currentTab' : Globals.AppTabs.ParserRegistry.value
			});
			require(['views/parser/ParserListingView'],function(ParserListingView){
				App.rContent.show(new ParserListingView());
			});
		},

		deviceCatalogAction: function(){
			VAppState.set({
				'currentTab' : Globals.AppTabs.DeviceCatalog.value
			});
			require(['views/device/DeviceCatalogView'],function(DeviceCatalogView){
				App.rContent.show(new DeviceCatalogView());
			});
		},
		
		defaultAction: function(actions) {
			// We have no matching route, lets just log what the URL was
			console.log('No route:', actions);
		}
	});

	return AppRouter;

});