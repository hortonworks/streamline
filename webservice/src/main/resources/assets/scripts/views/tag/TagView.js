define([
	'require',
	'hbs!tmpl/tag/tagView',
	'collection/VTagList',
	'models/VTag',
	'modules/Modal',
	'utils/Utils'
], function(require, tmpl, VTagList, VTag, Modal, Utils){
	'use strict';
	var TagView = Marionette.LayoutView.extend({
		template: tmpl,
        templateHelpers: function() {},
        events: {
        	'click #addTag': 'evAddTags',
        	'click .dd3-content .btn-warning': 'evEditTags',
        	'click .dd3-content .btn-danger' : 'evDeleteTags',
        	'click .dd3-content .btn-success': 'evAddChildTags',
            'keyup .search-text': 'evSearchTag'
        },
        initialize: function(options){
        	this.collection = new VTagList();
        	this.collection.comparator = "id";
        	this.fetchTags();
        },
        fetchTags: function(renderFlag){
        	this.collection.reset();
        	this.collection.fetch({
        		async: false
        	});
        	this.collection.sort();
            this.parentTags = [];
            this.childTags = [];
            _.each(this.collection.models, function(model) {
                var tagId = model.get('id');
                if(!_.contains(this.parentTags, tagId))
                    this.childTags.push(tagId);
                if(model.get('tagIds').length) {
                    var index = this.childTags.indexOf(model.get('tagIds')[0]);
                    if(index > -1)
                        this.childTags.splice(index, 1);
                    if(!_.contains(this.parentTags, model.get('tagIds')[0]))
                        this.parentTags.push(model.get('tagIds')[0]);
                }
            }, this);

        	if(renderFlag){
        		this.render();
        	}
        },
        evAddTags: function(){
        	var self = this;
        	this.showModal(new VTag());
        },
        evEditTags: function(e){
        	var self = this;
        	var tagId = $(e.currentTarget).parents('li').data().id;
        	var model = this.collection.get(tagId);
        	this.showModal(model);
        },
        showModal: function(model){
        	var self = this;
        	require(['views/tag/AddTagView'], function(AddTagView){
        		var view = new AddTagView({
        			model: model
        		});
        		var modal = new Modal({
        			title: (model.has('id') ? 'Edit Tag' : 'Add Tag'),
        			content: view,
        			contentWithFooter: true,
        			showFooter: false
        		}).open();

        		view.on('closeModal', function(){
        			self.fetchTags(true);
        			modal.trigger('cancel');
        		});
        	});
        },
        onRender: function(){
            var self = this;
        	if(this.collection.length){
                this.showTagList(this.collection);
	        	this.$('.dd').off('change');
	        	this.$('.dd').nestable();
	        	this.$('.dd').on('change', function(e, data) {
                    var m = self.collection.get(data.currentId);
                    m.set('tagIds', (data.parentId ? [data.parentId] : []));
                    m.save({},{
                        success: function(model, response, options) {
                            Utils.notifySuccess("Tag is updated successfully");
                            self.fetchTags(true);
                        },
                        error: function(model, response, options) {
                            Utils.showError(model, response);
                            self.fetchTags(true);
                        }
                    })
				});
        	} else {
        		this.$('.dd').html("No tags are currently present.");
        	}
        },
        showTagList: function(collection, isSearch) {
            this.$('.dd').html('<ol class="dd-list tag-list"></ol>');
            var self = this;
            var tempCollection = new Backbone.Collection();
            for(var i = 0;i < self.parentTags.length;i++) {
                var tempModel = collection.get(self.parentTags[i]);
                tempCollection.add(tempModel);
            }
            for(var i = 0;i < self.childTags.length;i++) {
                var tempModel = collection.get(self.childTags[i]);
                tempCollection.add(tempModel);
            }

            tempCollection.each(function(model, i){
                var id = model.get('id');


                if(!self.$('[data-id="'+id+'"]').length && !model.get('tagIds').length || i == 0 && isSearch) {
                    self.$(".tag-list").append('<li class="dd-item dd3-item" data-id="'+model.get('id')+'">'+
                    '<div class="dd-handle dd3-handle"></div>'+
                    '<div class="dd3-content">'+
                        '<h5>'+model.get('name')+'</h5>'+
                        '<p>'+model.get('description')+'</p>'+
                        '<div class="btn-group btn-action">'+
                            '<button class="btn-success"><i class="fa fa-plus"></i></button>'+
                            '<button class="btn-warning"><i class="fa fa-pencil"></i></button>'+
                            (_.contains(self.childTags, id) ? '<button class="btn-danger"><i class="fa fa-trash"></i></button>' : '') +
                        '</div>'+
                    '</div>'+
                    '</li>');
                } else {
                    var parentId = model.get('tagIds')[0];
                    var tempModel = tempCollection.get(parentId);
                    if(!self.$('[data-id="'+parentId+'"]')) {
                        self.$('.tag-list').append('<li class="dd-item dd3-item" data-id="'+parentId+'">'+
                    '<div class="dd-handle dd3-handle"></div>'+
                    '<div class="dd3-content">'+
                        '<h5>'+tempModel.get('name')+'</h5>'+
                        '<p>'+tempModel.get('description')+'</p>'+
                        '<div class="btn-group btn-action">'+
                            '<button class="btn-success"><i class="fa fa-plus"></i></button>'+
                            '<button class="btn-warning"><i class="fa fa-pencil"></i></button>'+
                            (_.contains(self.childTags, parentId) ? '<button class="btn-danger"><i class="fa fa-trash"></i></button>' : '') +
                        '</div>'+
                    '</div>'+
                    '</li>');
                    }
                   if(!self.$('[data-id="'+parentId+'"]').find('ol').length) {
                    self.$('[data-id="'+parentId+'"]').append('<ol class="dd-list"></ol>');
                    self.$('[data-id="'+parentId+'"]').find('.btn-danger').remove();
                   }
                    if(!self.$('[data-id="'+parentId+'"]').length) {
                        self.$('.tag-list').append('<li class="dd-item dd3-item" data-id="'+model.get('id')+'">'+
                    '<div class="dd-handle dd3-handle"></div>'+
                    '<div class="dd3-content">'+
                        '<h5>'+model.get('name')+'</h5>'+
                        '<p>'+model.get('description')+'</p>'+
                        '<div class="btn-group btn-action">'+
                            '<button class="btn-success"><i class="fa fa-plus"></i></button>'+
                            '<button class="btn-warning"><i class="fa fa-pencil"></i></button>'+
                            (_.contains(self.childTags, id) ? '<button class="btn-danger"><i class="fa fa-trash"></i></button>' : '') +
                        '</div>'+
                    '</div>'+
                    '</li>');
                    } else {
                    self.$('[data-id="'+parentId+'"]').children('ol').append('<li class="dd-item dd3-item" data-id="'+model.get('id')+'">'+
                    '<div class="dd-handle dd3-handle"></div>'+
                    '<div class="dd3-content">'+
                        '<h5>'+model.get('name')+'</h5>'+
                        '<p>'+model.get('description')+'</p>'+
                        '<div class="btn-group btn-action">'+
                            '<button class="btn-success"><i class="fa fa-plus"></i></button>'+
                            '<button class="btn-warning"><i class="fa fa-pencil"></i></button>'+
                            (_.contains(self.childTags, id) ? '<button class="btn-danger"><i class="fa fa-trash"></i></button>' : '') +
                        '</div>'+
                    '</div>'+
                    '</li>');
                    }
                }
            });
            self.$('.dd').nestable();
        },

        showSearchResults: function(collection) {
            this.$('.dd').html('<ol class="dd-list tag-list"></ol>');
            var self = this;

            collection.each(function(model, i){
                var id = model.get('id');
                self.$(".tag-list").append('<li class="dd-item dd3-item" data-id="'+model.get('id')+'">'+
                    '<div class="dd-nodrag dd3-handle"></div>'+
                    '<div class="dd3-content">'+
                        '<h5>'+model.get('name')+'</h5>'+
                        '<p>'+model.get('description')+'</p>'+
                        '<div class="btn-group btn-action">'+
                            '<button class="btn-success"><i class="fa fa-plus"></i></button>'+
                            '<button class="btn-warning"><i class="fa fa-pencil"></i></button>'+
                            (_.contains(self.childTags, id) ? '<button class="btn-danger"><i class="fa fa-trash"></i></button>' : '') +
                        '</div>'+
                    '</div>'+
                    '</li>');
            });
            self.$('.dd').nestable();
        },

        evDeleteTags: function(e){
        	var self = this;
        	var tagId = $(e.currentTarget).parents('li').data().id;
        	var model = new VTag({id: tagId});
        	model.destroy({
        		success: function(model, response, options) {
                    Utils.notifySuccess("Tag is deleted successfully");
                    self.fetchTags(true);
                },
                error: function(model, response, options) {
                    Utils.showError(model, response);
                    self.fetchTags(true);
                }
        	});
        },
        evAddChildTags: function(e){
        	var self = this;
        	var tagId = $(e.currentTarget).parents('li').data().id;
        	this.showModal(new VTag({tagIds: [tagId]}));
        },
        evSearchTag: function(e) {
            var tagName = this.$('.search-text').val(),
                id = null,
                tags = [];
            if(tagName == "") {
                //this.showTagList(this.collection);
                this.render();
            }
            else {
                var searchResult = this.collection.search(tagName);
                if(!searchResult.models.length)
                    this.$('.dd').html("No such tag found.");
                else this.showSearchResults(searchResult, true);
            }
        }
	});

	return TagView;
});