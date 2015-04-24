(function() {
  return {
    onReady : function(data) {
      console.log("routing is ready !");
      var firstRoute = location.hash.substring(1);
      if (firstRoute === "") firstRoute = "home";
      window.location.hash = firstRoute;
      window.addEventListener("hashchange", function(e) {
        if (firstRoute != window.location.hash.substring(1)) {
          window.location.reload(e.newUrl);
        }
      }, false);
      this.postMessage({
        type : "firstRoute",
        data : firstRoute
      });
    }
  };
}());