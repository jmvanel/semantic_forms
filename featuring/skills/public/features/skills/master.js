(function() {
	return {
		onReady : function(data) {

			$select = $('#input-tags').selectize({
				plugins : [ 'remove_button', 'drag_drop' ],
				openOnFocus : false,
				closeAfterSelect : true,
				delimiter : ',',
				persist : false,
				create : function(input) {
					return {
						value : input,
						text : input
					}
				},
				create : true,
				valueField : 'uri',
				labelField : 'label',
				searchField : [ 'label' ],
				load : function(query, callback) {
//					var json = [ {
//						"uri" : "arie.benichou@gmail.com",
//						"label" : "Arié Bénichou"
//					}, {
//						"uri" : "josh.nehuman@gmail.com",
//						"label" : "Josh Nehuman"
//					} ];
//					callback(json);
					
					if (!query.length) return callback();
					
					var request = {};
				    request.type = 'GET';
				    request.dataType = 'json';
				    request.url = "http://lookup.dbpedia.org/api/search.asmx/KeywordSearch?QueryString=jazz;";
				    request.beforeSend = function (xhr) {
				        xhr.setRequestHeader("Accept", "application/json");
				    };
				    request.error = function() {
		                callback();
		            },
		            request.success = function(res) {
		            	console.log(res.results);
		                callback(res.results);
		            }
				    $.ajax(request);
					
			        
//					$.ajax({
//			            url: 'http://api.rottentomatoes.com/api/public/v1.0/movies.json',
//			            type: 'GET',
//			            dataType: 'json',
//			            data: {
//			                q: query,
//			                apikey: '3qqmdwbuswut94jv4eua3j85'
//			            },
//			            error: function() {
//			                callback();
//			            },
//			            success: function(res) {
//			                callback(res.movies);
//			            }
					
					
				}
			});

			selectize = $select[0].selectize;

			selectize.on('item_add', function(data) {
				console.log("You have just typed '" + data + "'");
				$(".selectize-dropdown").show();
			});

			selectize.on('type', function(data) {
				console.log("You are typing '" + data + "'");
			});

		},
	};
}());