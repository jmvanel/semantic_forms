
$(document).ready(function() {
    var topics = [];
    $(".sf-standard-form").on('focus', '.sfLookup', function(event) {
        $(this).autocomplete({
            autoFocus: true,
            minlength: 3,
            search: function() {
                $(this).addClass('sf-suggestion-search')
            },
            open: function() {
                $(this).removeClass('sf-suggestion-search')
            },
            select: function( event, ui ) {
                console.log( "Topic chosen label event ");
                console.log($(this));
                console.log( "Topic chosen label ui");
                console.log(ui);
                $emptyFields = $(this).siblings().filter(function(index) { return $(this).val() == ''}).length;
                console.log('Champs vides : '+ $emptyFields);
                if ($emptyFields === 0) {
                    addedWidget = cloneWidget($(this))
                }
            },
            source: function(request, callback) {
                console.log("Déclenche l'événement :")
                var typeName
                var $el = $(event.target);
                if ($el) {
                    var type = $el.attr('data-rdf-type').split('/');
                    if (type) {
                        typeName = type[type.length - 1];
                    }
                }

                $.ajax({
                    url: "/lookup",
                    data: { QueryClass: typeName, QueryString: request.term + "*" },
                    dataType: "json",
                    timeout: 5000
                }).done(function(response) {
                    console.log('Done');
                    var topics = [];
                    callback(response.results.map(function (m) {
                        console.log(m);
                        // topics[m.label] = m.uri;
                        return { "label": m.label /* + " - " +
                         cutStringAfterCharacter(m.description, '.') */, "value": m.uri }
                    }))
                });
            }
        })
    });
});

function cutStringAfterCharacter(s, c) {
    if (!(s === null)) {
        var n = s.indexOf(c);
        return s.substring(0, n != -1 ? n : s.length);
    } else {
        return s;
    }
};
