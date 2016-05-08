RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
FOAF_NAME = "http://xmlns.com/foaf/0.1/name";
DC_TITLE = "http://purl.org/dc/elements/1.1/title";
DCT_TITLE = "http://purl.org/dc/terms/title";
SKOS_PREFLABEL = "http://www.w3.org/2004/02/skos/core#prefLabel";

// Example URLs:
// http://sparql.tw.rpi.edu/swbig/endpoints/http://hints2005.westat.com:8080/wsrf/services/cagrid/Hints2005/gov.nih.nci.dccps.hints2005.domain.TobaccoUse/1
// http://www.cs.rpi.edu/~hendler/foaf.rdf
// http://tw.rpi.edu/web/person/JimHendler
// http://purl.org/twc/cabig/endpoints/http://array.nci.nih.gov:80/wsrf/services/cagrid/CaArraySvc/gov.nih.nci.caarray.domain.project.Experiment/453
// http://purl.org/twc/cabig/endpoints/http://hints2005.westat.com:8080/wsrf/services/cagrid/Hints2005/gov.nih.nci.dccps.hints2005.domain.HealthInformationNationalTrendsSurvey/1
// http://tw.rpi.edu/web/person/JamesMcCusker
// http://localhost/~jpm78/derivation.ttl
// http://localhost/~jpm78/4210962_Processor_for_dynamic_programmin.pdf.prov.prov-only.ttl
// http://localhost/~jpm78/4210962_Processor_for_dynamic_programmin.pdf.prov.ttl
// https://dl.dropboxusercontent.com/u/9752413/egg.ttl

function getLocale() {
    if ( navigator ) {
        if ( navigator.language ) {
            return navigator.language;
        }
        else if ( navigator.browserLanguage ) {
            return navigator.browserLanguage;
        }
        else if ( navigator.systemLanguage ) {
            return navigator.systemLanguage;
        }
        else if ( navigator.userLanguage ) {
            return navigator.userLanguage;
        }
    } else return 'en';
}
var locale = getLocale();

$.extend({
  getUrlVars: function(){
    var vars = [], hash;
    var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++)
    {
      hash = hashes[i].split('=');
      vars.push(hash[0]);
      vars[hash[0]] = hash[1];
    }
    return vars;
  },
  getUrlVar: function(name){
    return $.getUrlVars()[name];
  }
});

function polygonIntersect(c, d, a, b) {
  var x1 = c[0], x2 = d[0], x3 = a[0], x4 = b[0],
      y1 = c[1], y2 = d[1], y3 = a[1], y4 = b[1],
      x13 = x1 - x3,
      x21 = x2 - x1,
      x43 = x4 - x3,
      y13 = y1 - y3,
      y21 = y2 - y1,
      y43 = y4 - y3,
      ua = (x43 * y13 - y43 * x13) / (y43 * x21 - x43 * y21);
  return [x1 + ua * x21, y1 + ua * y21];
}

function pointInBox(rect, b) {
    return b.x > rect.x && b.x < rect.x + rect.width &&
        b.y > rect.y && b.y < rect.y + rect.height;
}

function pointInLine(a,c,d) {
    return a[0] >= Math.min(c[0],d[0]) &&
        a[0] <= Math.max(c[0],d[0]) &&
        a[1] >= Math.min(c[1],d[1]) &&
        a[1] <= Math.max(c[1],d[1]);
}

function edgePoint(rect, a, b) {
    comparePoint = a
    if (pointInBox(rect,b))
        comparePoint = b;
    
    lines = [[[rect.x,rect.y], // top horizontal
              [rect.x+rect.width,rect.y]],
             [[rect.x,rect.y+rect.height], // bottom horizontal
              [rect.x+rect.width,rect.y+rect.height]],
             [[rect.x,rect.y], // left vertical
              [rect.x,rect.y+rect.height]],
             [[rect.x+rect.width,rect.y], // right vertical
              [rect.x+rect.width,rect.y+rect.height]]];
    intersects = lines.map(function(x) {
        return polygonIntersect(x[0],x[1],a,b);
    });
    if (pointInLine(intersects[0],a,b)  && 
        intersects[0][0] >= rect.x &&
        intersects[0][0] <= rect.x + rect.width) {
        return intersects[0];
    } else if (pointInLine(intersects[1],a,b) && 
               intersects[1][0] >= rect.x &&
               intersects[1][0] <= rect.x + rect.width) {
        return intersects[1];
    } else if (pointInLine(intersects[2],a,b) && 
               intersects[2][1] >= rect.y &&
               intersects[2][1] <= rect.y + rect.height) {
        return intersects[2];
    } else if (pointInLine(intersects[3],a,b) && 
               intersects[3][1] >= rect.y &&
               intersects[3][1] <= rect.y + rect.height) {
        return intersects[3];
    } else return null;
}

function makeCenteredBox(node) {
    var box = {};
    box.x = node.x - node.width / 2;
    box.y = node.y - node.height / 2;
    box.width = node.width;
    box.height = node.height;
    return box;
}

var OWL = "http://www.w3.org/2002/07/owl#";
var RDFS = "http://www.w3.org/2000/01/rdf-schema#";
var XSD = "http://www.w3.org/2001/XMLSchema#";

var extraLabels = {}
extraLabels[RDFS+"subClassOf"] = "sub-class of";
extraLabels[OWL+"equivalentClass"] = "equivalent class";
extraLabels[OWL+"inverseOf"] = "inverse of";
extraLabels[OWL+"ObjectProperty"] = "Object Property";
extraLabels[OWL+"DataProperty"] = "Data Property";
extraLabels[XSD+"length"] = "length";
extraLabels[XSD+"minLength"] = "minLength";
extraLabels[XSD+"maxLength"] = "maxLength";
extraLabels[XSD+"pattern"] = "pattern";
extraLabels[XSD+"langRange"] = "langRange";
extraLabels[XSD+"minInclusive"] = "<=";
extraLabels[XSD+"maxInclusive"] = ">=";
extraLabels[XSD+"minExclusive"] = "<";
extraLabels[XSD+"maxExclusive"] = ">";

function conditionalize(fn, condition) {
    function wrapper() {
        return fn.apply(null,arguments);
    }
    wrapper.onlyif = condition;
    return wrapper;
}

var decorators = {
    anchor: function (callback) { 
        return function(d) { 
            var label = callback(d); 
            label = '<a href="'+d.uri+'">'+label+"</a>";
            return label;
        }; 
    }
}
var labelers = [
    conditionalize(function(resource) {
        var label = ""
        var datatype = resource.value(OWL+'onDatatype');
        var restrictions = resource.relations[OWL+'withRestrictions'].map(function(r) {
            return d3.entries(r.attribs).filter(function(po) {
                return extraLabels[po.key] != null;
            }).map(function(po) {
                return extraLabels[po.key]+po.value[0];
            }).join("");
        });
        label = label + getLabel(datatype)+'['+restrictions.join(", ")+']';
        console.log("facet:",label, resource);
        return label;
    },
    function(resource) {
        return resource.uri 
            && (resource.types.map(function(d){return d.uri}).indexOf(RDFS+'Datatype') != -1) 
            && (resource.value(OWL+'onDatatype'));
    }),
    conditionalize(function(resource) {
        var label = ""
        if (resource.relations[OWL+'intersectionOf']) {
            label = label + resource.relations[OWL+'intersectionOf'].map(getLabel).join(" and ");
        } else if (resource.relations[OWL+'unionOf']) {
            label = label + resource.relations[OWL+'unionOf'].map(getLabel).join(" or ");
        } else if (resource.relations[OWL+'complementOf']) {
            label = label + "cannot be "+resource.relations[OWL+'complementOf'].map(getLabel).join(", nor ");
        } else if (resource.relations[OWL+'datatypeComplementOf']) {
            label = label + "cannot be "+resource.relations[OWL+'datatypeComplementOf'].map(getLabel).join(", nor ");
        } else if (resource.relations[OWL+'oneOf']) {
            label = label + "is one of {"+ resource.relations[OWL+'oneOf'].map(getLabel).join(", ")+"}";
        } else if (resource.value(OWL+'onDatatype')) {
            console.warn("not handling datatype",resource);
        } else {
            console.warn("Not handling class",resource);
        }
        if (resource.types.map(function(d){return d.uri}).indexOf(RDFS+'Datatype') != -1) {
            console.warn("Datatype:",resource);
        }
        if (label.length > 0) label = "("+label+")";
        return label;
    },
    function(resource) {
        return resource.uri && resource.uri.indexOf("_:") == 0 
            && (resource.types.map(function(d){return d.uri}).indexOf(OWL+'Class') != -1
                || resource.types.map(function(d){return d.uri}).indexOf(RDFS+'Datatype') != -1)
    }),
    conditionalize(function(resource) {
        var label = getLabel(resource.relations[OWL+'onProperty'][0]);

        if (resource.value(OWL+'hasValue')) {
            label = label + " value " + getLabel(resource.value(OWL+'hasValue'));
        } else if (resource.value(OWL+'someValuesFrom')) {
            label = label + " some " + getLabel(resource.value(OWL+'someValuesFrom'));
        } else if (resource.value(OWL+'allValuesFrom')) {
            var l =  getLabel(resource.value(OWL+'allValuesFrom'));
            console.log(l, resource.value(OWL+'allValuesFrom'));
            label = label + " only " + l;
        } else if (resource.value(OWL+'hasSelf')) {
            label = label + " self " ;
        } else if (resource.value(OWL+'minCardinality')) {
            label = label + " min " + resource.value(OWL+'minCardinality');
        } else if (resource.value(OWL+'minQualifiedCardinality')) {
            label = label + " min " + resource.value(OWL+'minQualifiedCardinality');
            if (resource.value(OWL+'onClass'))
                label = label + " " + getLabel(resource.value(OWL+'onClass'));
            else if (resource.value(OWL+'onDataRange'))
                label = label + " " + getLabel(resource.value(OWL+'onDataRange'));
        } else if (resource.value(OWL+'maxCardinality')) {
            label = label + " max " + resource.value(OWL+'maxCardinality');
        } else if (resource.value(OWL+'maxQualifiedCardinality')) {
            label = label + " max " + resource.value(OWL+'maxQualifiedCardinality');
            if (resource.value(OWL+'onClass'))
                label = label + " " + getLabel(resource.value(OWL+'onClass'));
            else if (resource.value(OWL+'onDataRange'))
                label = label + " " + getLabel(resource.value(OWL+'onDataRange'));
        } else if (resource.value(OWL+'maxCardinality')) {
            label = label + " max " + resource.value(OWL+'maxCardinality');
        } else if (resource.value(OWL+'cardinality')) {
            label = label + " exactly " + resource.value(OWL+'cardinality');
        } else if (resource.value(OWL+'qualifiedCardinality')) {
            label = label + " exactly " + resource.value(OWL+'qualifiedCardinality');
            if (resource.value(OWL+'onClass'))
                label = label + " " + getLabel(resource.value(OWL+'onClass'));
            else if (resource.value(OWL+'onDataRange'))
                label = label + " " + getLabel(resource.value(OWL+'onDataRange'));
        } else {
            console.warn("not handling restriction",resource);
        }

        return label;
    },
    function(resource) {
        return resource.uri && resource.type == 'resource' 
            && resource.types.map(function(d){return d.uri}).indexOf(OWL+'Restriction') != -1
    }),
    conditionalize(decorators.anchor(function(d) {
        var ext = d.uri.split('.');
        ext = ext[ext.length-1];
        if (ext == 'ico') {
            return '<img src="'+d.uri+'" alt="'+label+'"/>'
        }
        return '<img width="100" src="'+d.uri+'" alt="'+d.label+'"/>';
    }),
    function(d) {
        if (!d.uri) return false;
        var ext = d.uri.split('.');
        ext = ext[ext.length-1];
        return $.inArray(ext, ['jpeg','jpg','png','gif','ico']) != -1;
    }),
    conditionalize(decorators.anchor(function (d) {
        if (d.label) return d.label;
        else return d;
    }),
    function(d) {return true})
]

function getLabel(d) {
    var mylabels = labelers.filter(function(fn) {return fn.onlyif(d)});
    var result =  mylabels[0](d);
    //console.log(d, mylabels, result);
    return result;
}

function getGravatar(uri) {
    var hash = md5(uri);
    return "http://www.gravatar.com/avatar/"+hash+"?d=identicon&r=g%s=20"
}

function Graph() {
    this.resources = {},
    this.nodes = [];
    this.edges = [];
    this.predicates = [];
    this.entities = [];
    
}
Graph.prototype.makeLink = function(source, target,arrow) {
    link = {};
    link.source = source;
    link.target = target;
    link.value = 1;
    link.display = true;
    link.arrow = arrow;
    this.edges.push(link);
    return link;
}

Graph.prototype.getSP = function(s, p) {
    var result = this.resources[s.uri+' '+p.uri];
    if (result == null) {
        result = {};
        result.width = 0;
        result.type = 'predicate';
        result.display = true;
        result.subject = s;
        result.predicate = p;
        result.objects = [];
        result.uri = s.uri+' '+p.uri;
        result.isPredicate = false;
        this.resources[result.uri] = result;
        link = this.makeLink(s,result,false);
        result.links = [];
        s.links.push(link);
        result.links.push(link);
    }
    return result;
}

Graph.prototype.getResource = function(uri) {
    var result = this.resources[uri];
    if (result == null) {
        result = {};
        result.value = function(uri) {
            var results = result.relations[uri];
            if (results == null || results.length == 0) results = result.attribs[uri];
            if (results == null || results.length == 0) return null;
            return results[0];
        };
        result.width = 0;
        result.type = 'resource';
        result.types = [];
        result.display = false;
        result.attribs = {};
        result.relations = {};
        result.objectOf = [];
        result.uri = uri;
        result.label = ' ';
        result.depth = -1;
        result.localPart = result.uri.split("#");
        result.localPart = result.localPart[result.localPart.length-1];
        result.localPart = result.localPart.split("/");
        result.localPart = result.localPart[result.localPart.length-1];
        result.label = result.localPart;
        if (extraLabels[uri]) {
            result.label = extraLabels[uri];
        }
        result.links = [];
        result.isPredicate = false;
        this.resources[uri] = result;
    }
    return result;
}

function squashLists(d) {
    var lists = {};
    var resources = {};

    d3.entries(d).forEach(function(subj){
        if (subj.value['http://www.w3.org/1999/02/22-rdf-syntax-ns#first']) {
            lists[subj.key] = subj.value;
        }
        else resources[subj.key] = subj.value;
    });

    var result = {};

    d3.entries(resources).forEach(function(subj) {

        if (subj.key == 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil')
            return;
        result[subj.key] = {};
        d3.entries(subj.value).forEach(function(pred) {
            var list = [];
            result[subj.key][pred.key] = list;
            pred.value.forEach(function(obj) {
                if (lists[obj.value]) {
                    var o = obj.value;
                    //console.log(o);
                    while (o) {
                        o = lists[o];
                        //console.log(o);
                        list.push(o['http://www.w3.org/1999/02/22-rdf-syntax-ns#first'][0])
                        o = o['http://www.w3.org/1999/02/22-rdf-syntax-ns#rest'][0].value;
                        //console.log(o);
                        if (o == 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil')
                            o = null;
                    }
                } else {
                    list.push(obj);
                }
            })
        })
    })
    //console.log(result);
    return result;
}



Graph.prototype.load = function(d) {
    var g = this
    d = squashLists(d);
    d3.entries(d).forEach(function(subj) {
        d3.entries(subj.value).forEach(function(pred) {
            pred.value.forEach(function(obj) {
                var resource = g.getResource(subj.key);
                resource.display = true;
                var predicate = g.getResource(pred.key);
                predicate.isPredicate = true;
                var labeled = false;
                if (obj.type == "literal") {
                    if (obj.lang != null) {
                        if (locale.lastIndexOf(obj.lang, 0) === 0) {
                        } else {
                            //console.log(obj.lang);
                            return;
                        }
                    }
                    if (pred.key == RDFS_LABEL && !labeled) {
                        resource.label = obj.value;
                        labeled = true;
                    } else if (pred.key == FOAF_NAME && !labeled) {
                        resource.label = obj.value;
                        labeled = true;
                    } else if (pred.key == DC_TITLE && !labeled) {
                        resource.label = obj.value;
                        labeled = true;
                    } else if (pred.key == DCT_TITLE && !labeled) {
                        resource.label = obj.value;
                        labeled = true;
                    } else if (pred.key == SKOS_PREFLABEL && !labeled) {
                        resource.label = obj.value;
                        labeled = true;
                    } else {
                        if (resource.attribs[predicate.uri] == null) {
                            resource.attribs[predicate.uri] = [];
                        }
                        resource.attribs[predicate.uri].push(obj.value);
                    }
                } else if (pred.key == RDF_TYPE) {
                    resource.types.push(g.getResource(obj.value));
                } else {
                    sp = g.getSP(resource, predicate);
                    o = g.getResource(obj.value);
                    if (obj.type == 'bnode' && o.label == o.uri)
                        o.label = ' ';
                    sp.objects.push(o);
                    o.display = true;
                    o.objectOf.push(sp);
                    var link = g.makeLink(sp,o,true);
                    sp.links.push(link);
                    if (resource.relations[predicate.uri] == null) {
                        resource.relations[predicate.uri] = [];
                    }
                    resource.relations[predicate.uri].push(o);
                }
            })
        })
    });
    function hideSP(sp) {
        sp.display=false;
        sp.links.forEach(function(l){l.display=false});
        sp.predicate=true;
    }
    d3.values(g.resources).filter(function(resource){
        return (resource.type == 'resource' 
                && resource.types.map(function(d){return d.uri}).indexOf(OWL+'Restriction') != -1) ||
               (resource.uri.indexOf("_:") == 0 && resource.types
                && (resource.types.map(function(d){return d.uri}).indexOf(OWL+'Class') != -1
                    || resource.types.map(function(d){return d.uri}).indexOf(OWL+'Datatype') != -1))
    }).forEach(function(resource){
        resource.objectOf.forEach(function(sp){
            resource.display = false;
            if (sp.subject.attribs[sp.predicate.uri] == null) {
                sp.subject.attribs[sp.predicate.uri] = [];
            }
            sp.subject.attribs[sp.predicate.uri].push(resource);
            //console.log(sp);
        })
    })

    d3.values(g.resources).filter(function(resource){
        var result = resource.type == "predicate";
        if (result) {
            result = resource.objects.reduce(function(prev,o) {
                return d3.keys(o.attribs).length == 0 &&
                    d3.keys(o.relations).length == 0 &&
                    o.types.length == 0 &&
                    d3.keys(o.objectOf).length == 1 && prev;
            },result);
        }
        return result;
    }).forEach(function(resource) {
        resource.subject.attribs[resource.predicate.uri] = resource.objects;
        resource.display = false;
        resource.objects.forEach(function(o) {
            o.display = false;
        });
        resource.links.forEach(function(l) {
            l.display = false;
        });
    });
    
    g.predicates = d3.values(g.resources).filter(function(node) {
        if (node.type == 'predicate' && !node.isPredicate) {
            node.display = node.display && node.subject.display;
            return node.display;
        } else return false;
    }).filter(function(node){
        node.display = node.display && node.objects.reduce(function(prev, o) {
            return prev || o.display;
        },false);
        return node.display;
    });
    g.predicates.forEach(function(p) {
        p.label = p.predicate.label;
    });

    g.nodes = d3.values(g.resources).filter(function(node) {
        return (!node.isPredicate && node.display);
    });
    
    g.entities = d3.values(g.resources).filter(function(node) {
        return (node.type == 'resource' && !node.isPredicate && node.display);
    }).sort(function(a,b) {
        return a.objectOf.length - b.objectOf.length;
    });
    
    //console.log(g.entities)    
    g.edges = d3.values(g.edges).filter(function(l) {
        return l.display && l.source.display && l.target.display;
    });
}

function loader(url, doLoad) {
    $.getJSON("http://rdf-translator.appspot.com/convert/detect/rdf-json/"+url, doLoad)
        .error(function() {
            $.getJSON("http://rdf-translator.appspot.com/convert/xml/rdf-json/"+url, doLoad)
                .error(function() {
                    $.getJSON("http://rdf-translator.appspot.com/convert/n3/rdf-json/"+url, doLoad)
                        .error(function() {
                            alert("Could not load "+url); 
                        });
                });
        });
}
    
function loadGraph(url, fn) {
    function doLoad(d) {
        graph = new Graph();
        graph.load(d);
        fn(graph);
    }
    loader(url, doLoad);
}

function makeNodeSVG(entities, vis, nodeWidth, graph) {
    var node = vis.selectAll("g.node")
        .data(entities)
        .enter();
    node = node.append("svg:foreignObject")
        .attr('width',nodeWidth)
        .attr('height','1000');
    
    node.append("xhtml:body").attr('xmlns',"http://www.w3.org/1999/xhtml");
    
    var body = node.selectAll("body")
        //.style("max-width",nodeWidth+"px")
    var resource = body//.append("div")
        //.style("max-width","100%")
        //.style("display","block")
        .append("table")
        .attr("class",function(d) {
            var classes = d.types.map(function(d){ return d.localPart;})
            return "resource "+classes.join(" ");
        })
        .style("table-layout","fixed");
    var titles = resource.append("xhtml:tr")
        .append("xhtml:th")
        .style("word-wrap","break-word")
        .style("max-width",nodeWidth+"px")
        .attr("class","title")
        .attr("colspan","2")
        .html(function(d) { return getLabel(d); })
    var types = resource.append("xhtml:tr")
        .append("xhtml:td")
        .attr("colspan","2")
        .attr("class","type")
        .html(function(d) {
            var typeLabels = d.types.map(function(t) {
                if (t.label != ' ')
                    return '<a href="'+t.uri+'">'+t.label+'</a>';
                else return '<a href="'+t.uri+'">'+t.localPart+'</a>';
            });
            if (typeLabels.length > 0) {
                return "a&nbsp;" + typeLabels.join(", ");
            } else {
                return "";
            }
        })
        .attr("href",function(d) { return d.uri});
    
    var attrs = resource.selectAll("td.attr")
        .data(function(d) {
            var entries = d3.entries(d.attribs);
            return entries;
        }).enter()
        .append("xhtml:tr");
    attrs.append("xhtml:td")
        .attr("class","attrName")
        .text(function(d) {
            predicate = graph.getResource(d.key);
            if (predicate.label != ' ')
                return predicate.label+":";
            else return predicate.localPart+":";
        });
    attrs.append("xhtml:td")
        .style("word-wrap","break-word")
        .style("max-width",nodeWidth+"px")
        //.style("width","60%")
        .html(function(d) {
            return d.value.map(function(d) {
                if (d.type == "resource") {
                    return getLabel(d);
                } else return d;
            }).join(", ");
        });
    return node;
}

function makePredicateSVG(predicates, vis) {
    var result = vis.selectAll("g.predicate")
        .data(predicates)
        .enter()
        .append("svg:text")
        .attr("class","link")
        .attr("text-anchor","middle")
        .attr("x", function(d) { return d.x; })
        .attr("y", function(d) { return d.y; })
        .text(function(d){
            if (d.predicate.label != ' ') d.predicate.label;
             else return d.predicate.localPart;
            return d.predicate.label;
        })
        .attr("xlink:href",function(d) { return d.predicate.uri;});
    return result;
}

function makeLinkSVG(edges, vis) {
    var result = {};
    result.link = vis.selectAll("line.link")
        .data(edges)
        .enter().append("svg:g").attr("class", "link");

    result.link.append("svg:line")
        .attr("class", "link")
        .attr("x1", function(d) { return d.source.x; })
        .attr("y1", function(d) { return d.source.y; })
        .attr("x2", function(d) { return d.target.x; })
        .attr("y2", function(d) { return d.target.y; });
    
    result.arrowhead = result.link.filter(function(d) {
        return d.arrow;
    })
        .append("svg:polygon")
        .attr("class", "arrowhead")
        .attr("transform",function(d) {
            angle = Math.atan2(d.y2-d.y1, d.x2-d.x1);
            return "rotate("+angle+", "+d.x2+", "+d.y2+")";
        })
        .attr("points", function(d) {
            //angle = (d.y2-d.y1)/(d.x2-d.x1);
            return [[d.x2,d.y2].join(","),
                    [d.x2-3,d.y2+8].join(","),
                    [d.x2+3,d.y2+8].join(",")].join(" ");
        });
    return result;
}

function viewHierarchyRDF(element, w, h, url, nodeWidth) {
    var cluster = d3.layout.cluster()
        .size([h, w - 300])
        .children(function(d) {
            return d.links.map(function(l) {
                return l.target;
            });
        })
        .sort(function(a, b) {
            if (a < b) return -1;
            else if (b > a) return 1;
            else return 0;
        });
    
    var diagonal = d3.svg.diagonal()
        .projection(function(d) { return [d.y, d.x]; });
    
    var vis = d3.select(element).append("svg:svg")
        .attr("width", w)
        .attr("height", h)
        .append("svg:g")
        //.attr("transform", "translate(200, 20)");
    svgRoot = vis.node();

    loadGraph(url, function(graph){

    nodes = graph.entities.concat(graph.predicates);
    graph.entities.forEach(function(d) {
        if (d.depth < 0)
            cluster.nodes(d);
    });
    links = cluster.links(nodes);
    var link = vis.selectAll("path.link")
        .data(links)
        .enter().append("svg:path")
        .attr("class", "link")
        .attr("d", diagonal);
    
    var node = vis.selectAll("g.node")
        .data(nodes)
        .enter().append("svg:g")
        .attr("class", "node")
        .attr("transform", function(d) { 
            return "translate(" + d.y + "," + d.x + ")";
        })
    
    node.append("svg:circle")
        .attr("r", 4.5);
    
    node.append("svg:text")
        .attr("dx", function(d) { return d.children ? -8 : 8; })
        .attr("dy", 3)
        .attr("text-anchor", function(d) { return d.children ? "end" : "start"; })
        .text(function(d) {
            //console.log(d.label);
            //console.log(d);
            return d.label; 
        });
    });
}

function makeMenu(element) {
    var ul = d3.select(element).append("xhtml:div")
        .attr("class","contextMenu")
        .attr("id","linkMenu")
        .append("xhtml:ul");
    var items = [
        {id:'open',text:'Open'},
        {id:'addLabels',text:'Link in labels'},
        {id:'addAll',text:'Link in all data'}
    ]
    ul.selectAll("m.menu").data(items).enter()
        .append("xhtml:li")
        .attr("id",function(d){return d.id;})
        .html(function(d){return d.text;});
    //console.log($('div svg body a'));
    $('a').contextMenu('linkMenu', {
        bindings: {
            'open': function(t) {
                //console.log(t);
                alert('Trigger was '+t.id+'\nAction was Open');
            },
            'addLabels': function(t) {
                //console.log(t);
                alert('Trigger was '+t.id+'\nAction was Link in labels');
            },
            'addAll': function(t) {
                //console.log(t);
                alert('Trigger was '+t.id+'\nAction was Link in all data');
            },
        },
    });
}

function viewrdf(element, w, h, url,nodeWidth) {
    var svg = d3.select(element);
    var chart = d3.select(svg.node().parentNode);
    var force = d3.layout.force()
    var transMatrix = [1,0,0,1,0,0];
    var width = 0;
    var height = 0;

    function updateSize() {
        width = parseInt(chart.style("width"));
        height = parseInt(chart.style("height"));
        svg.attr("width",width)
            .attr("height",height);
        //force.stop()
        force.size([width, height]);
        //    .start();
    }
    $(window).resize(updateSize);
    updateSize();
    svg.append("rect").attr("width",10000)
        .attr('height',10000)
        .attr('fill','white');
    var vis = svg.append("g");

    loadGraph(url, function(graph) {

        function pan(dx, dy) {       
            transMatrix[4] += dx;
            transMatrix[5] += dy;

            newMatrix = "matrix(" +  transMatrix.join(' ') + ")";
            vis.attr("transform", newMatrix);
        }

        function zoom(scale) {
            for (var i=0; i<transMatrix.length; i++) {
                transMatrix[i] *= scale;
            }

            transMatrix[4] += (1-scale)*width/2;
            transMatrix[5] += (1-scale)*height/2;

            newMatrix = "matrix(" +  transMatrix.join(' ') + ")";
            vis.attr("transform", newMatrix);
        }

        function handleMouseWheel(evt) {
            if(evt.preventDefault)
                evt.preventDefault();

            evt.returnValue = false;

            var delta;

            if (evt.wheelDelta) delta = evt.wheelDelta / 3600; // Chrome/Safari
            else delta = evt.detail / -90; // Mozilla
            var z = 1 + delta; // Zoom factor: 0.9/1.1
            zoom(z);
        }

        var state = "none";
        /**
         * Handle mouse move event.
        */
        function handleMouseMove(evt) {
            if(evt.preventDefault)
                evt.preventDefault();
    
            evt.returnValue = false;
    
            var svgDoc = evt.target.ownerDocument;

            var g = getRoot(svgDoc);
    
            if(state == 'pan' && enablePan) {
                // Pan mode
                var p = getEventPoint(evt).matrixTransform(stateTf);

                setCTM(g, stateTf.inverse().translate(p.x - stateOrigin.x, p.y - stateOrigin.y));
            } else if(state == 'drag' && enableDrag) {
                // Drag mode
                var p = getEventPoint(evt).matrixTransform(g.getCTM().inverse());
                setCTM(stateTarget, root.createSVGMatrix().translate(p.x - stateOrigin.x, p.y - stateOrigin.y).multiply(g.getCTM().inverse()).multiply(stateTarget.getCTM()));
                stateOrigin = p;
            }
        }

        /**
         * Handle click event.
         */
        function handleMouseDown(evt) {
            if(evt.preventDefault)
                evt.preventDefault();
            evt.returnValue = false;
            state = 'drag';
        }

        /**
         * Handle mouse button release event.
         */
        function handleMouseUp(evt) {
            if(evt.preventDefault)
                evt.preventDefault();

            evt.returnValue = false;

            if(state == 'pan' || state == 'drag') {
                // Quit pan mode
                state = '';
            }
        }

        force.charge(-1000)
            .linkStrength(1)
            .linkDistance(function(d){
                var width = d.source.width/2 + d.target.width/2 + 25;
                return width;
            })
        //.linkDistance(50)
            .gravity(0.05)
            .nodes(graph.nodes)
            .links(graph.edges);
            //.size([w, h]);
        
        var links = makeLinkSVG(graph.edges, vis);
        links.link.call(force.drag);
        
        var predicates = makePredicateSVG(graph.predicates, vis);
        predicates.call(force.drag);
        
        var node = makeNodeSVG(graph.entities, vis, nodeWidth, graph);
        node.call(force.drag);
        
        svg.append("circle")
            .attr("cx",50)
            .attr("cy",50)
            .attr("r",42)
            .attr("fill","white")
            .attr("opacity","0.75")
        svg.append("path")
            .attr("class","button")
            .on("click",function() {pan(0,50)})
            .attr("d","M50 10 l12 20 a40,70 0 0,0 -24,0z")
        svg.append("path")
            .attr("class","button")
            .on("click", function() {pan(50,0)})
            .attr("d","M10 50 l20 -12 a70,40 0 0,0 0,24z")
        svg.append("path")
            .attr("class","button")
            .on("click",function(){pan(0,-50)})
            .attr("d","M50 90 l12 -20 a40,70 0 0,1 -24,0z")
        svg.append("path")
            .attr("class","button")
            .on("click",function(){pan(-50,0)})
            .attr("d","M90 50 l-20 -12 a70,40 0 0,1 0,24z")
  
        svg.append("circle")
            .attr("class","compass")
            .attr("cx","50")
            .attr("cy","50")
            .attr("r","20")
        svg.append("circle")
            .attr("class","button")
            .attr("cx","50")
            .attr("cy","41")
            .attr("r","8")
            .on("click",function() {zoom(0.8)})
        svg.append("circle")
            .attr("class","button")
            .attr("cx","50")
            .attr("cy","59")
            .attr("r","8")
            .on("click",function(){zoom(1.25)})

        svg.append("rect")
            .attr("class","plus-minus")
            .attr("x","46")
            .attr("y","39.5")
            .attr("width","8")
            .attr("height","3")
        svg.append("rect")
            .attr("class","plus-minus")
            .attr("x","46")
            .attr("y","57.5")
            .attr("width","8")
            .attr("height","3")
        svg.append("rect")
            .attr("class","plus-minus")
            .attr("x","48.5")
            .attr("y","55")
            .attr("width","3")
            .attr("height","8")
	if(navigator.userAgent.toLowerCase().indexOf('webkit') >= 0)
	    svg.node().addEventListener('mousewheel', handleMouseWheel, false); // Chrome/Safari
	else
	    svg.node().addEventListener('DOMMouseScroll', handleMouseWheel, false); // Others

    //makeMenu(element);
    var ticks = 0
    force.on("tick", function() {
        ticks += 1
        if (ticks > 500) {
            force.stop()
                .charge(0)
                .linkStrength(0)
                .linkDistance(0)
                .gravity(0)
                .friction(0)
                .start()
                .stop();
        } else {
            force.stop();
            force.linkDistance(function(d){
                    var width = d.source.width/2 + d.target.width/2 + 50;
                    return width;
                })
                .start();
        }
      	links.link.selectAll("line.link")
            .attr("x1", function(d) {
                var box = makeCenteredBox(d.source);
                var ept = edgePoint(box,
                                [d.source.x,d.source.y],
                                [d.target.x,d.target.y]);
                d.x1 = d.source.x;
                if (ept != null) {
                    d.x1 = ept[0]
                }
                return d.x1; 
            })
            .attr("y1", function(d) {
                var box = makeCenteredBox(d.source);
                var ept = edgePoint(box,
                                [d.source.x,d.source.y],
                                [d.target.x,d.target.y]);
                d.y1 = d.source.y;
                if (ept != null) {
                    d.y1 = ept[1]
                }
                return d.y1; 
            })
            .attr("x2", function(d) {
                var box = makeCenteredBox(d.target);
                var ept = edgePoint(box,
                                [d.source.x,d.source.y],
                                [d.target.x,d.target.y]);
                d.x2 = d.target.x;
                if (ept != null) {
                    d.x2 = ept[0]
                }
                return d.x2; 
            })
            .attr("y2", function(d) { 
                var box = makeCenteredBox(d.target);
                var ept = edgePoint(box,
                                [d.source.x,d.source.y],
                                [d.target.x,d.target.y]);
                d.y2 = d.target.y;
                if (ept != null) {
                    d.y2 = ept[1]
                }
                return d.y2; 
            });

        node.attr("height",function(d) {
            return d.height = this.childNodes[0].childNodes[0].clientHeight+4;
        })
            .attr("width",function(d) {
                if (d.width == 0 || d.width == nodeWidth) {
                    d.width = this.childNodes[0].childNodes[0].clientWidth+4;
                }
                return d.width;
            })
            .attr("x", function(d) {
                //d.x = Math.max(d.width/2, Math.min(w-d.width/2, d.x ));
                return d.x - d.width/2;
            })
            .attr("y", function(d) {
                //d.y = Math.max(d.height/2, Math.min(h-d.height/2, d.y ));
                return d.y - d.height/2;
            });

        predicates.attr("x", function(d) {
            //d.x = Math.max(10, Math.min(w-10, d.x ));
            return d.x;
        })
            .attr("y", function(d) {
                //d.y = Math.max(10, Math.min(h-10, d.y ));
                return d.y;
            });

        links.arrowhead.attr("points", function(d) {
            return [[d.x2,d.y2].join(","),
                    [d.x2-3,d.y2+8].join(","),
                    [d.x2+3,d.y2+8].join(",")].join(" ");
        })
            .attr("transform",function(d) {
                var angle = Math.atan2(d.y2-d.y1, d.x2-d.x1)*180/Math.PI + 90;
                return "rotate("+angle+", "+d.x2+", "+d.y2+")";
            });
        //vis.attr("width", w)
        //    .attr("height", h);
        //force.size([width, height]);
    });
    force.start();
    });
}
