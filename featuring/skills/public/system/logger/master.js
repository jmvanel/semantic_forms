// TODO extract feature "Keyboard"
(function() {
  return {
    onReady : function(data) {
      $("body").append(this.context.data.html); // TODO à revoir
      this.showLogger = function(event) {
        if (event.which == 10) {
          this.postMessage({
            type : "showLogger"
          });
        }
      }.bind(this);
      this.hideLogger = function(event) {
        if (event.which == 10) {
          this.postMessage({
            type : "hideLogger"
          });
        }
      }.bind(this);
      this.hideLogger({
        which : 10
      });
    },
    onShowLogger : function(data) {
      $("body").unbind("keypress", this.showLogger);
      $("body").keypress(this.hideLogger);
      $("#logger").show();
    },
    onHideLogger : function(data) {
      $("body").unbind("keypress", this.hideLogger);
      $("body").keypress(this.showLogger);
      $("#logger").hide();
    },
    onLog : function(data) {
      $("#logger").append("<div class='log'>" + data + "</div>");
    },
  };
}());