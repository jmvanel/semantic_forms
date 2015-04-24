(function() {
  return {
    constructor : function(object) {
      this.name = object.name;
      this.path = object.path;
      this.isFirst = object.isFirst || false;
      this.data = {};
      this.children = {};
      this.parent = object.parent || null;
      this.worker = null;

      // TODO à revoir
      this.clientScriptName = "master.js";
      this.serverScriptName = "slave.js";
      this.workerIsReadyType = "ready";
      this.childrenFeaturesFileName = "features.json";
      this.workerInflector = function(type) {
        return "on" + type.charAt(0).toUpperCase() + type.slice(1);
      };

      // TODO à revoir
      //this.notFoundImage = object.notFoundImage || "./system/core/assets/images/i404.png";
    },

    fileDoesNotExist : function(url) {
      var xhr = new XMLHttpRequest();
      xhr.open('HEAD', url, false), xhr.send();
      return xhr.status == 204;
    },

    wave : function(message) {
      console.debug("'" + this.name + "'" + " : waving to children : " + "'" + message.type + "'");
      message.wave = true;
      delete message.geyser;
      this.childrenNames.map(function(childName) {
        this.children[childName].worker.postMessage(message);
      }.bind(this));
    },

    cascade : function(message) {
      console.debug("'" + this.name + "'" + " : cascading to children : " + "'" + message.type + "'");
      message.cascade = true;
      delete message.geyser;
      this.childrenNames.map(function(childName) {
        this.children[childName].worker.postMessage(message);
      }.bind(this));
    },

    // TODO ? make it a wave up and introduce "splash" 
    geyser : function(message) {
      console.debug("'" + this.name + "'" + " : forwarding to parent : " + "'" + message.type + "'");
      message.geyser = true;
      this.parent.worker.postMessage(message);
    },

    newWorker : function() { // TODO ? FeaturedWorker class
      var makeHandler = function(worker, protocol) {
        return function(e) {
          var message = e.data;
          var method = this.workerInflector(message.type);
          if (method in protocol) {
            (protocol[method].bind(worker))(message.data);
            message.cascade = false;
            if (message.wave === true) this.wave(message);
          } else {
            console.debug("'" + this.name + "'" + " : does not understand : " + "'" + message.type + "'");
            if (message.wave !== true && message.cascade !== true) {
              // TODO use a root worker as a sentinel
              if (this.parent)
                this.geyser(message);
              else {
                console.debug("'" + message.type + "'" + " was not handled by parents, cascading to children...");
                this.cascade(message);
              }
            } else
              this.cascade(message);
          }
        }.bind(this);
      }.bind(this);
      var blob = new Blob([ this.data.scripts.slave ], {
        type : "text/javascript"
      });
      var url = window.URL.createObjectURL(blob);
      var worker = new Worker(url);
      worker.onmessage = function(e) {
        console.debug("received : ", e.data, " on : ", "'" + e.target.name + "'");
      };
      var protocol = eval(this.data.scripts.master);

      protocol[this.workerInflector("start")] = function(childName) {
        this.loaded = [];
        this.context.fetchChildren(function(parent, children) {
          this.context.childrenNames = children;
          this.context.runChildren(parent, children);
        }.bind(this));
      };

      // TODO ! extract rendering from context
      //if (!protocol[this.workerInflector("render")]) {
      protocol[this.workerInflector("render")] = function(data) {
        this.context.render(function() {
          this.postMessage({
            type : "rendered",
            data: this.name
          });
        }.bind(this));
      };
      //}

      protocol[this.workerInflector("rendered")] = function(data) {
        console.log(data + " has been rendered");
        
        // TODO !! (Experimental in next version)
        // Pourvoir emballer un contexte entre : 
        //  * une "head" : un contexte "racine virtuel"
        //  * et une "tail" : un contexte "feuille virtuel" du dernier contexte feuille
        // => head / concrete context / tail

        this.postMessage({
          type : "childReady",
          data : data
        });
        
        this.context.cascade({
          type : "render"
        });


      };

      protocol[this.workerInflector(this.name)] = function(childName) {
        if (childName != null) {
          console.info("loaded '" + childName + "'");
          this.loaded.push(childName);
        }
        if (this.loaded.length === this.context.childrenNames.length) {
          console.info("All direct children of '" + this.context.name + "' have been loaded");
          this.context.childrenNames.map(function(name) {
            this.context.children[name].worker.postMessage({
              type : "start",
              data : {}
            });
          }.bind(this));
          this.postMessage({
            type : "oneLess",
            data : this.context.name
          });
        }
      };

      if (this.isFirst === true) {
        protocol[this.workerInflector('oneMore')] = function(data) {
          this._n = this._n || 1;
          this.loading = this.loading || {};
          this.loading[data] = false;
          ++this._n;
        };

        protocol[this.workerInflector('oneLess')] = function(data) {
          this.loading = this.loading || {};
          this.loading[data] = true;
          for ( var name in this.loading) {
            if (this.loading[name] === true) delete this.loading[name];
          }
          if (Object.keys(this.loading).length === 0) {
            this.postMessage({
              type : "render",
              data : this.name
            //wave : true
            });
          }
        };

        protocol[this.workerInflector('childReady')] = function(childName) {
          this.loading = this.loading || {};
          
          this.rendered = this.rendered || [];
          this.rendered.push(childName);
          
          console.debug(this.rendered);
          
          if (this._n === undefined || this.rendered.length === this._n) {
            console.info("done");
            setTimeout(function() {
              this.postMessage({
                type : "ready",
                wave : true
              });
            }.bind(this), -1000 * 60);
          }
        };

      }

      var client = makeHandler(worker, protocol);
      worker.addEventListener("message", client.bind(worker), false);
      worker.name = this.name, worker.context = this;
      return worker;
    },

    run : function(messages) {
      var p1 = promise.get(this.path + this.name + "/" + this.clientScriptName);
      var p2 = promise.get(this.path + this.name + "/" + this.serverScriptName);
      promise.join([ p1, p2 ]).then(function(results) {
        if (results[0][0] || results[1][0]) return;
        this.data.scripts = {};
        this.data.scripts.master = results[0][1] || this.defaults.master;
        this.data.scripts.slave = results[1][1] || this.defaults.slave;
        this.worker = this.newWorker();
        messages.map(function(message) {
          this.worker.postMessage(message);
        }.bind(this));
      }.bind(this));
    },

    fetchChildren : function(callback) {
      promise.get(this.path + this.name + "/" + this.childrenFeaturesFileName).then(function(error, text, xhr) {
        var childFeatureNames = JSON.parse(text || this.defaults.features);
        var children = [];
        Object.keys(childFeatureNames).map(function(name) {
          if (childFeatureNames[name]) children.push(name);
        });
        callback(this, children);
      }.bind(this));
    },

    newChild : function(parent, name) {
      return new this.constructor({
        path : this.path,
        name : name,
        parent : parent
      });
    },

    runChildContext : function(parent, childContext) {

      this.worker.postMessage({
        type : "oneMore",
        data : childContext.name
      });

      childContext.run([ {
        type : this.name,
        data : childContext.name
      } ]);

      this.children[childContext.name] = childContext;
    },

    runChildren : function(parent, children) {
      if (children.length) {
        children.map(function(childName) {
          this.runChildContext(parent, this.newChild(parent, childName));
        }.bind(this));
      } else {
        this.worker.postMessage({
          type : this.name
        });
      }
    },

    // TODO à revoir
    //  imagesNotFound : function(fragment) {
    //    var i404 = this.notFoundImage;
    //    var f = function(e) {
    //      var src = this.getAttribute(src);
    //      this.setAttribute("alt", "not found : " + src);
    //      this.setAttribute("src", i404);
    //      var src = this.getAttribute(src);
    //      return false;
    //    };
    //    var images = fragment.querySelectorAll("img");
    //    for ( var i = 0, n = images.length; i < n; ++i) {
    //      var image = images.item(i);
    //      image.onerror = f;
    //    }
    //    return fragment;
    //  },

    assets : function(context) {
      var template = document.createElement('template');
      template.innerHTML = context.data.html;
      var images = template.content.querySelectorAll("img");
      var rewritingPathPrefix = context.path + context.name + "/assets" + "/";
      var pattern = /^\.\/assets\/(.*)/;
      for ( var i = 0, n = images.length; i < n; ++i) {
        var image = images.item(i);
        image.onerror = function() {};
        var src = image.getAttribute("src"); // TODO use a Shadow DOM
        var match = src.match(pattern);
        if (match != null) {
          var suffix = match[1];
          image.setAttribute("src", rewritingPathPrefix + suffix);
        }
      }
      var fragment = document.importNode(template.content, true);
      return fragment;
    },

    // TODO à revoir
    _render : function(context) {
      var featureContainer;
      featureContainer = document.querySelector("." + context.name);
      if (!featureContainer) {
        featureContainer = document.createElement("section");
        featureContainer.setAttribute("class", context.name);
        document.body.appendChild(featureContainer);
        featureContainer = document.querySelector("." + context.name);
      }
      // TODO use a shadow dom
      if (context.isFirst) {
        var style = document.createElement("style");
        style.innerHTML = context.data.style_outer;
        document.head.appendChild(style);
      }
      var style = document.createElement("style");
      style.innerHTML = context.data.styles;
      var fragment = this.assets(context);
      // TODO use a shadow dom
      document.head.appendChild(style);
      console.log("rendering " + context.name);
      featureContainer.appendChild(fragment);
    },

    // TODO à revoir
    render : function(callback) {
      var prefix = this.path + this.name + "/";
      var keys = [ "style_outer", "styles", "html" ];
      var values = [ "style.outer.css", "style.css", "fragment.html" ];
      var files = values.map(function(fileName) {
        return prefix + fileName;
      });
      var promises = files.map(function(file) {
        return promise.get(file);
      });
      promise.join(promises).then(function(results) {
        if (results[0][0] || results[1][0] || results[2][0]) return;
        this.data.style_outer = results[0][1] || this.defaults.style_outer;
        this.data.styles = results[1][1] || this.defaults.style;
        this.data.html = results[2][1] || this.defaults.fragment;
        this._render(this);
        callback();
      }.bind(this));
    }

  }
})();