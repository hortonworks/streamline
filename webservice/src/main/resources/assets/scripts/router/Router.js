define([
	'jquery',
	'underscore',
	'backbone',
	'App',
	'models/VAppState',
	'utils/Globals',
	'utils/Utils'
], function($, _, Backbone, App, VAppState, Globals, Utils) {
	var AppRouter = Backbone.Router.extend({
		routes: {
			// Define some URL routes
			// ''						: 'dashboardAction',
			''						: 'topologyAction',
			'!/dashboard'			: 'dashboardAction',
			'!/parser-registry'		: 'parserRegistryAction',
			'!/device-catalog'		: 'deviceCatalogAction',
			'!/device-catalog/:pid'	: 'deviceDetailAction',
			'!/configuration'		: 'configurationAction',
			'!/topology'			: 'topologyAction',
			'!/topology-editor'		: 'topologyEditorAction',
			'!/topology-editor/:pid': 'topologyEditorAction',

			// Default
			'*actions': 'defaultAction'
		},

		initialize: function() {
			this.showRegions();
			this.listenTo(this, 'route', this.postRouteExecute, this);
		},

		showRegions: function() {
			require(['views/site/Header'],function(HeaderView){
				App.rHeader.show(new HeaderView());
			});

			require(['views/site/Sidebar'],function(SidebarView){
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
			// console.log("Pre-Route Change Operations can be performed here !!");
		},

		postRouteExecute: function(name, args) {
			// console.log("Post-Route Change Operations can be performed here !!");
			// console.log("Route changed: ", name);
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

		deviceDetailAction: function(id){
			VAppState.set({
				'currentTab' : Globals.AppTabs.DeviceCatalog.value
			});
			require(['models/VDatasource'], function(VDatasource){
				var dsModel = new VDatasource();
				dsModel.set('dataSourceId',id);
				dsModel.getOnlyDatasource({
					id: id,
					success: function(model, response, options){
						var tModel = new VDatasource(model.entity);
						require(['views/datasource/DataSourceDetails'], function(DataSourceDetailsView){
							App.rContent.show(new DataSourceDetailsView({
								dsModel: tModel
							}));
						});
					},
					error: function(model, response, options){
						Utils.showError(model, response);
					}
				});
			});
		},

		configurationAction: function(){
			VAppState.set({
				'currentTab' : 0
			});
			require(['collection/VClusterList', 'views/config/ConfigurationView'], function(VClusterList, configView){
				var collection = new VClusterList(),
					showStormBtn = true,
					showKafkaBtn = true;
				collection.fetch({
					async: false,
			        success: function(collection, response, options){
			          if(collection.models.length){
			            _.each(collection.models, function(model){
			              if(model.get('type') === 'STORM'){
			                showStormBtn = false;
			              } else if(model.get('type') === 'KAFKA'){
			                showKafkaBtn = false;
			              }
			            });
			          }
			        }
			    });
			    App.rContent.show(new configView({
					clusterCollection: collection,
					showStormBtn: showStormBtn,
					showKafkaBtn: showKafkaBtn
				}));
			});
		},

		topologyAction: function(){
			VAppState.set({
				'currentTab' : Globals.AppTabs.DataStreamEditor.value
			});
			require(['views/topology/TopologyListingMaster'], function(TopologyListingMaster){
				App.rContent.show(new TopologyListingMaster());
			});
		},

		topologyEditorAction: function(id){
			VAppState.set({
				'currentTab' : Globals.AppTabs.DataStreamEditor.value
			});
			//TODO - support to open existing topology in the editor
			require(['views/topology/DataStreamMaster'], function(DataStreamMaster){
				App.rContent.show(new DataStreamMaster());
			});
		},
		
		defaultAction: function(actions) {
			// We have no matching route, lets just log what the URL was
			console.log('No route:', actions);
		}
	});

	return AppRouter;

});