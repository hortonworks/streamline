define([
    'require',
    'hbs!tmpl/file/fileListingView',
    'collection/VFileList',
    'models/VFile',
    'utils/TableLayout',
    'utils/Utils',
    'modules/Modal',
    'bootbox'
], function(require, tmpl, VFileList, VFile, VTableLayout, Utils, Modal, bootbox) {
    'use strict';
    var FileListingView = Marionette.LayoutView.extend({
        template: tmpl,
        templateHelpers: function() {},

        regions: {
            tableLayout: '#rTable'
        },

        events: {
            'click #addFile': 'evAddFile',
            'click .deleteFile': 'evDeleteFile',
            'click .editFile': 'evEditFile'
        },

        initialize: function(options) {
            this.collection = new VFileList();
            this.collection.fetch({ reset: true });
        },

        onRender: function() {
            this.tableLayout.show(this.getTable());
        },
        getTable: function() {
            return new VTableLayout({
                columns: this.getColumns(),
                collection: this.collection,
                gridOpts: {
                    emptyText: 'No file found',
                    className: 'table table-backgrid table-bordered table-striped table-condensed'
                },
                includePagination: false,
                includeFooterRecords: false
            });
        },
        getColumns: function() {
            return [{
                name: 'name',
                cell: 'string',
                label: 'Name',
                hasTooltip: false,
                editable: false
            }, {
                name: 'storedFileName',
                cell: 'string',
                label: 'Stored File Name',
                hasTooltip: false,
                editable: false
            }, {
                name: 'className',
                cell: 'string',
                label: 'Class Name',
                hasTooltip: false,
                editable: false
            }, {
                name: 'version',
                cell: 'string',
                label: 'Version',
                hasTooltip: false,
                editable: false
            }, {
                name: "Actions",
                cell: "Html",
                label: 'Actions',
                hasTooltip: false,
                editable: false,
                formatter: _.extend({}, Backgrid.CellFormatter.prototype, {
                    fromRaw: function(rawValue, model) {
                        if (model) {
                            return "<a href='javascript:void(0);' data-id='" + model.get("id") + "' title='Edit' class='editFile btn btn-success btn-xs'><i class='fa fa-edit'></i></a><a href='javascript:void(0)' title='Delete' class='deleteFile btn btn-danger btn-xs' data-id='" + model.get("id") + "' type='default' ><i class='fa fa-trash'></i></a>";
                        }
                    }
                })
            }];
        },

        evAddFile: function(model) {
            var self = this;

            if(_.isUndefined(model) || model.currentTarget){
                model = new VFile();
            }
            require(['views/file/addFileView'], function(AddFileView) {
                var view = new AddFileView({
                    model: model
                });

                var modal = new Modal({
                    title: (model.has('id')) ? 'Edit File' : 'Add File',
                    content: view,
                    contentWithFooter: true,
                    showFooter: false
                }).open();

                view.on('closeModal', function() {
                    modal.trigger('cancel');
                    self.collection.fetch({reset: true});
                });

            });
        },

        evEditFile: function(e) {
            var id = $(e.currentTarget).data('id'),
                model = this.getModel(id);
            this.evAddFile(model);
        },

        evDeleteFile: function(e) {
            var id = $(e.currentTarget).data('id'),
                self = this;
            bootbox.confirm("Do you really want to delete this file ?", function(result) {
                if (result) {
                    var model = self.getModel(id);

                    model.destroyFileModel({
                        id: id,
                        success: function(model, response) {
                            Utils.notifySuccess('File deleted successfully');
                            self.collection.fetch({ reset: true });
                        },
                        error: function(model, response, options) {
                            Utils.showError(model, response);
                        }
                    });
                }
            });
        },

        getModel: function(id) {
            var model = _.find(this.collection.models, function(model) {
                if (model.get('id') === id)
                    return model;
            });
            return model;
        }

    });

    return FileListingView;
});
