(function() {
  return {
    onReady : function(data) {
      console.info("core is ready");
    },
    onFirstRoute : function(name) {
      var section = document.createElement("section");
      section.setAttribute("class", name);
      document.body.appendChild(section);
      new this.context.constructor({path : "./features/", name : name, isFirst: true}).run([ {type : "start"} ]);
    }
  };
}());