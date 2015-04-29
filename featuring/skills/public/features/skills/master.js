(function() {
    return {
	onReady : function(data) {

	    // TODO create core feature
	    var loadScript = function(scriptURL, succes, failure) {
		var succes = succes || function() {
		}, failure = failure || function() {
		}, ready = false;
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

	    // TODO use core feature
	    loadScript("//code.jquery.com/jquery-1.11.2.min.js", function() {
		console.log("jquery loaded");
		loadScript("//ajax.googleapis.com/ajax/libs/jqueryui/1.9.2/jquery-ui.min.js", function() {
		    console.log("jquery-ui loaded");
		    loadScript("/selectize.js", function() {
			console.log("selectize loaded");

			var loadFunction = function(query, callback) {

			    // if (!query.length) return callback();

			    console.log("query:" + query);

			    // callback([ {
			    // uri : "test1",
			    // label : "label1"
			    // }, {
			    // uri : "test2",
			    // label : "label2"
			    // } ]);

			    $.ajax({
				type : "GET",
				dataType : "json",
				url : "http://lookup.dbpedia.org/api/search.asmx/KeywordSearch",
				data : {
				    QueryString : query
				},
				beforeSend : function(xhr) {
				    xhr.setRequestHeader("Accept", "application/json");
				},
				error : function() {
				    callback();
				},
				success : function(res) {
				    // callback();

				    var json = res.results;
				    for ( var entry in json) {
					json[entry]["query"] = query;
					console.log(json[entry]);
				    }

				    callback(res.results);
				}
			    });
			};

			// TODO handle selection from arrow keys
			// TODO persist selection

			var onInitializeFunction = function(data) {
			    console.log("selectize.js is initialized");
			    // var fn = this.settings.load;
			    // this.load(function(callback) {
			    // fn.apply(self, [ "jazz", callback ]);
			    // });
			    // this.refreshOptions(true);
			};

			var options = {
			    onInitialize : onInitializeFunction,
			    plugins : [ "remove_button", "drag_drop" ],
			    delimiter : ",",
			    create : true,
			    highlight : false,
			    persist : false,
			    // hideSelected : true
			    valueField : "uri",
			    labelField : "label",
			    searchField : "query",
			    load : loadFunction,
			    render : {
				option_create : function(data, escape) {
				    return "";
				},
				option : function(data, escape) {
				    return "<div>" + data.label + "</div>";
				}
			    },
			// score : function(search) {
			// //var score = this.getScoreFunction(search);
			// return function(item) {
			// //return score(item) * (1 + Math.min(item.watchers /
			// 100, 1));
			// return 0;
			// };
			// },
			}

			var select = $("#input-tags");
			selectize = select.selectize(options)[0].selectize;
			selectize.on("load", function(data) {
			    console.log("loaded");
			    for ( var option in data) {
				console.log(data[option]);
				selectize.addOption({
				    uri : data[option].uri,
				    label : data[option].label
				});
			    }
			    selectize.refreshOptions(true);
			});

			// selectize.on("dropdown_open", function() {
			// console.log("dropdown is open");
			// });
			//
			// selectize.on("dropdown_close", function() {
			// console.log("dropdown is closed");
			// });
			//
			// selectize.on("type", function(data) {
			// console.log("you are typing : " + data);
			// selectize.refreshOptions();
			// });

			var delayFunction = function() {
			    console.warn("items : " + this.items);
			    // this.clearOptions();
			    this.setTextboxValue($("#input-proxy").val());
			    console.log("will query : " + $("#input-proxy").val());
			    var fn = this.settings.load;
			    // TODO break on empty string
			    this.load(function(callback) {
				fn.apply(self, [ $("#input-proxy").val(), callback ]);
			    });
			    this.refreshOptions(true);
			}.bind(selectize);

			var timer = timer || 0;

			$("#input-proxy").keydown(function(e) {
			    clearInterval(timer);
			});

			$("#input-proxy").keyup(function(e) {
			    timer = window.setTimeout(delayFunction, 333);
			});

			// TODO à revoir
			$("#input-proxy").on("blur", function() {
			    // console.log("proxy blur");
			    this.focus();
			})

			$("#input-proxy").focus();

			// var selectProxy = $("#input-proxy");
			// selectizeProxy =
			// selectProxy.selectize(options)[0].selectize;

		    }, function() {
		    });
		}, function() {
		});
	    }, function() {
	    });

	},
    };
}());