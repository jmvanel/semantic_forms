(function(configuration) {
  return function() {
    var loadScript = function(scriptURL, succes, failure) {
      var succes = succes || function() {}, failure = failure || function() {}, ready = false;
      var script = document.createElement("script");
      script.onerror = function() {
        failure(scriptURL);
      };
      script.onload = script.onreadystatechange = function() {
        if (!ready && (!this.readyState || this.readyState === "loaded" || this.readyState === "complete")) {
          ready = true, succes(scriptURL);
        }
      };
      if (!document.head) document.body.insertAdjacentElement("beforeBegin", document.createElement("head"));
      script.src = scriptURL, document.head.appendChild(script);
    };
    loadScript("./system/core/assets/dependencies/promise.min.js", function() {
      var lambdaObject = function(url, callback) {
        promise.get(url).then(function(error, data) {
          var object = eval(data); // TODO ? use new Function() construction
          var instancer = object.constructor;
          instancer.prototype = object;
          callback(instancer);
        });
      };
      var go = function(data) {
        var defaults = {};
        data.map(function(e, i) {defaults[keys[i]] = e[1];});
        lambdaObject("./system/core/assets/dependencies/context.js", function(Context) {
          console.info("Featured - version " + configuration.version);
          Context.prototype.defaults = defaults;
          var system = document.createElement("section");
          system.setAttribute("class", "system");
          document.body.appendChild(system);
          new Context({path : "./system/", name : "core", isFirst: true}).run([ {type : "start"} ]);
        });
      };
      var keys = [ "features", "fragment", "master", "slave", "style", "style_outer" ];
      var values = [ "features.json", "fragment.html", "master.js", "slave.js", "style.css", "style.outer.css" ];
      var files = values.map(function(fileName) {return "./system/core/assets/templates/" + fileName;});
      var templates = files.map(function(file) {return promise.get(file);});
      promise.ajaxTimeout = configuration.timeout;
      promise.join(templates).then(go);
    });
  };
}({
  version : "0.1.6",
  timeout : 1000 * 10
}))();