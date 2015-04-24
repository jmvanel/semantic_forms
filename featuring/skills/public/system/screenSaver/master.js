// TODO extract feature "Idle"
// TODO extract feature "Keyboard"
// TODO extract feature "Scheduler"
// TODO extract feature "Overlay"
(function() {
  return {

    onReady : function(data) {
      // TODO allow configuration to be injected from features.json or fragment.html
      this.delay = 1000 * 60 * 0.125;
      $("body").append(this.context.data.html); // TODO à revoir
      this.hideScreenSaver = function(event) {
        this.postMessage({
          type : "hideScreenSaver"
        });
      }.bind(this);
      this.hideScreenSaver();
    },

    onShowScreenSaver : function(data) {
      this.postMessage({
        type : "cancelScheduledScreenSaver"
      });
      this.postMessage({
        type : "log",
        data : "Showing ScreenSaver"
      });
      // TODO display overlay on home
      $(".home").fadeOut(500, function() {
        $(".screenSaver").fadeIn(125, function() {
          $("body").keydown(this.hideScreenSaver);
          $("body").css({
            "background-color" : "black"
          });
        }.bind(this));
      }.bind(this));
    },

    onHideScreenSaver : function(data) {
      $("body").unbind("keydown", this.hideScreenSaver);
      this.postMessage({
        type : "log",
        data : "Hiding ScreenSaver"
      });
      $(".screenSaver").hide();
      $(".home").show();
      this.postMessage({
        type : "scheduleScreenSaver",
        data : this.delay
      });
    },

    onScheduleScreenSaver : function(delay) {
      this.postMessage({
        type : "log",
        data : "Scheduling ScreenSaver : " + delay
      });
    },

    onCancelScheduledScreenSaver : function(data) {
      this.postMessage({
        type : "log",
        data : "Cancelling Scheduled ScreenSaver"
      });
    }

  };
}());