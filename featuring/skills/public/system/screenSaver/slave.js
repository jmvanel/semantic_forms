var id = 0;
self.addEventListener('message', function(e) {
  var message = e.data;
  switch (message.type) {
  case "scheduleScreenSaver":
    id = setTimeout(function() {
      this.postMessage({
        type : "showScreenSaver"
      });
    }, message.data);
    self.postMessage(message);
    break;
  case "cancelScheduledScreenSaver":
    clearTimeout(id);
    self.postMessage(message);
    break;
  default:
    self.postMessage(message);
  }
}, false);