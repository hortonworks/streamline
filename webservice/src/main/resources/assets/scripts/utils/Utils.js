define(['require', 'utils/Globals', 'bootbox', 'bootstrap.notify'], function(require, Globals, bootbox) {
  'use strict';

  var Utils = {};

  Utils.DBToDateObj = function(dbDate) {

    var dateObj = new Date(dbDate.toString());
    // If the above fails for some browser, try our own parse function
    if (isNaN(dateObj)) {
      dateObj = new Date(this.manualDateParse(dbDate.toString()));

    }
    return dateObj;
  };

  Utils.manualDateParse = function(date) {
    var origParse = Date.parse,
      numericKeys = [1, 4, 5, 6, 7, 10, 11];
    var timestamp, struct, minutesOffset = 0;

    if ((struct = /^(\d{4}|[+\-]\d{6})(?:-(\d{2})(?:-(\d{2}))?)?(?:T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{3}))?)?(?:(Z)|([+\-])(\d{2})(?::(\d{2}))?)?)?$/
        .exec(date))) {
      // avoid NaN timestamps caused by “undefined” values being
      // passed to Date.UTC
      for (var i = 0, k;
        (k = numericKeys[i]); ++i) {
        struct[k] = +struct[k] || 0;
      }

      // allow undefined days and months
      struct[2] = (+struct[2] || 1) - 1;
      struct[3] = +struct[3] || 1;

      if (struct[8] !== 'Z' && struct[9] !== undefined) {
        minutesOffset = struct[10] * 60 + struct[11];

        if (struct[9] === '+') {
          minutesOffset = 0 - minutesOffset;
        }
      }

      timestamp = Date.UTC(struct[1], struct[2], struct[3], struct[4],
        struct[5] + minutesOffset, struct[6], struct[7]);
    } else {
      timestamp = origParse ? origParse(date) : NaN;
    }

    return timestamp;
  };

  Utils.defaultErrorHandler = function(model, error) {
    if (error.status == 401) {
      throw new Error("ERROR 401 occured.\n You might want to change this error from here.");
      // window.location.href = "login.jsp" + location.hash;
    }
  };

  Utils.notifyError = function(message,from,align){
    if(!Utils.isNotifiedMessage(message)) {
      $.notify({
      // options
        icon: 'fa fa-warning',
        message: message
      },{
      // settings
        element: 'body',
        position: null,
        type: 'danger',
        animate: {
          enter: 'animated fadeInDown',
          exit: 'animated fadeOutUp'
        },
        placement: {
          from: from ? from :"top",
          align: align ? align :"right"
        },
        z_index: 1200,
        delay: 7000
      });
    }
  };

  Utils.notifySuccess = function(message,from,align){
    if(!Utils.isNotifiedMessage(message)) {
      $.notify({
      // options
        icon: 'fa fa-check',
        message: message
      },{
      // settings
        element: 'body',
        position: null,
        type: 'success',
        animate: {
          enter: 'animated fadeInDown',
          exit: 'animated fadeOutUp'
        },
        placement: {
          from: from ? from :"top",
          align: align ? align :"right"
        },
        z_index: 1200,
      });
    }
  };

  Utils.notifyInfo = function(message,from,align){
    if(!Utils.isNotifiedMessage(message)) {
      $.notify({
      // options
        icon: 'fa fa-info',
        message: message
      },{
      // settings
        element: 'body',
        position: null,
        type: 'info',
        animate: {
          enter: 'animated fadeInDown',
          exit: 'animated fadeOutUp'
        },
        placement: {
          from: from ? from :"top",
          align: align ? align :"right"
        },
        z_index: 1200,
        delay: 7000
      });
    }
  };

  Utils.notifyWarning = function(message,from,align){
    if(!Utils.isNotifiedMessage(message)) {
      $.notify({
      // options
        icon: 'fa fa-warning',
        message: message
      },{
      // settings
        element: 'body',
        position: null,
        type: 'warning',
        animate: {
          enter: 'animated fadeInDown',
          exit: 'animated fadeOutUp'
        },
        placement: {
          from: from ? from :"top",
          align: align ? align :"right"
        },
        z_index: 1200,
      });
    }
  };

  Utils.expandPanel = function(e){
    var body = $('body');
    e.preventDefault();
    var box = $(e.currentTarget).closest('div.panel');
    var button = $(e.currentTarget).find('i');
    button.toggleClass('fa-expand').toggleClass('fa-compress');
    box.toggleClass('expanded');
    body.toggleClass('body-expanded');
    var timeout = 0;
    if (body.hasClass('body-expanded')) {
      timeout = 100;
      // box.removeClass('zoomOut');
      box.addClass('zoomIn');
    } else {
      box.removeClass('zoomIn');
      box.addClass('zoomOut');
      box.removeClass('zoomOut');
    }
    setTimeout(function () {
      box.toggleClass('expanded-padding');
    }, timeout);
    setTimeout(function () {
      box.resize();
      box.find('[id^=map-]').resize();
    }, timeout + 50);

  };

  Utils.panelMinimize = function(self){
    //Minimize
    self.$('.minimize').on('click', function() {
        $(this).parents('.panel').find('.panel-body').slideToggle();
        $(this).children().toggleClass("fa-minus fa-plus");
    });
  };

  Utils.uploadFile = function(restURL, data, successCallback, errorCallback, type){
    $.ajax({
        url: restURL,
        data: data,
        cache: false,
        contentType: false,
        processData: false,
        type: type? type: 'POST',
        success: successCallback,
        error: errorCallback
      });
  };

  Utils.showError = function(model, response){
    var msg;
    if(typeof response === "string"){
      if(model.responseJSON.code === 500){
        msg = "Internal Server Error";
      } else {
        msg = model.responseJSON.responseMessage;
      }
    } else if(_.isUndefined(response.responseJSON)){
      msg = _.isEqual(response.statusText, 'Not Found') ? 'Api not found' : response.statusText;
    } else {
      if(response.responseJSON.responseMessage){
        msg = response.responseJSON.responseMessage;
      } else {
        msg = response.responseJSON.message;
      }
    }
    Utils.notifyError(msg);
  };

  Utils.GlobalEnumToArray = function(typeArr) {
    var optionArr = [];
    _.each(typeArr, function(item) {
      var obj = {
        'val': item.value,
        'label': item.valStr
      };
      optionArr.push(obj);
    });
    return optionArr;
  };

  Utils.isValidTopologyConnection = function(source, target, failedTupleFlag){
    var subStepsArray = Globals.Topology.Editor.Steps[source.parentType].Substeps;
    var obj = _.findWhere(subStepsArray, {valStr: source.currentType});
    if(!_.isUndefined(obj)){
      var canConnectToArr = obj.connectsTo.split(',');
      if(canConnectToArr.indexOf(target.currentType) !== -1){
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  };

  Utils.isNotifiedMessage = function(message) {
    var messagesArr = $('[data-notify="message"]'),
        isShown = false;
    _.each(messagesArr, function(element) {
      if($(element).html() == message)
        isShown = true;
    });
    return isShown;
  };

  Utils.ConfirmDialog = function(message, title, successCallback, cancelCallback) {
    bootbox.dialog({
      message: message,
      title: title,
      className: 'confirmation-dialog',
      buttons: {
        cancel: {
          label: 'No',
          className: 'btn-default btn-small',
          callback: cancelCallback ? cancelCallback : function(){}
        },
        success: {
          label: 'Yes',
          className: 'btn-success btn-small',
          callback: successCallback
        }
      }
    });
  };

  return Utils;
});