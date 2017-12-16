
        /*private*/
        function computeMinInPoints(initialData, key/*: String*/) {
  	        console.log( 'computeMinInPoints: initialData: ' + initialData )

        	var min = Infinity;
        	for(let dataKey in initialData){
        		let element = initialData[dataKey]
//      	        console.log( 'computeMinInPoints 1 key: ' + key + " - element " + JSON.stringify(element) )
//      	        console.log( 'computeMinInPoints 1.1 element[key] ' + (element[key]) )
            	    if (Number(element[key]) < min) {
            	        min = Number(element[key])
//            	        console.log( 'computeMinInPoints 2' + element )
            	    }            
            }
            return min
        }
        /*private*/
        function computeMaxInPoints(initialData, key/*: String*/) {
        	var max = - Infinity;
        	for(let dataKey in initialData){
        		let element = initialData[dataKey]
//      	        console.log( 'computeMaxInPoints 3.1 element[key] ' + (element[key]) )
            	if (Number(element[key]) > max) {
            	  max = Number(element[key])
                }            
            }
            return max
        }

        /** @return Bounds object from LeafLet */
        function findGeographicZone(initialData,keyLat,keyLong) {
//        	var corner1 = L.latLng(40.712, -74.227),
//        	corner2 = L.latLng(40.774, -74.125),
//        	bounds = L.latLngBounds(corner1, corner2);
        	/* All Leaflet methods that accept LatLngBounds objects also accept them in a simple Array form
        	 * (unless noted otherwise),
        	 * so the bounds example above can be passed like this: */
        	
  	        console.log( 'findGeographicZone: initialData: ' +
  	        		JSON.stringify(initialData) )
  	        console.log( 'findGeographicZone: computeMinInPoints keyLat: ' +		
  	        		computeMinInPoints(initialData, keyLat) )
  	        console.log( 'findGeographicZone: computeMaxInPoints keyLat: ' +		
  	        		computeMaxInPoints(initialData, keyLat) )
  	        		
  	        var result = L.latLngBounds(
  	            		  L.latLng(
  	            		    computeMinInPoints(initialData, keyLat),
  	            	        computeMaxInPoints(initialData, keyLong)	),
  	              		  L.latLng(
  	                  	    computeMaxInPoints(initialData, keyLat),
  	                  	    computeMinInPoints(initialData, keyLong)	) )
  	                  	    
  	        console.log( 'findGeographicZone: ' +
  	        		computeMaxInPoints(initialData, keyLong) + " , " +
  	        		computeMinInPoints(initialData, keyLong) + " , " +
  	        		JSON.stringify( result )
  	        )

  	        return result
        }

class Map{
    
    /**
     *
     * @param mapId -- id de la div
     * ces quatre paramètres permettre, a l'affichage d'une page, d'initialiser un certain contenu
     * @param initialData: Array -- données que tu veux afficher
     * @param keyLat: String -- clé pour accéder à la valeur :latitude
     * @param keyLong: String -- clé pour accéder à la valeur :longitude
     * @param keyText: String -- clé pour accéder à la valeur :text affiché
     */
    constructor(mapId,initialData,keyLat,keyLong,keyText) {

    	var bounds = findGeographicZone(initialData,keyLat,keyLong)
    	console.log("bounds")
    	console.log(bounds)
    	this.OSM = L.map(mapId) .fitBounds( bounds )

        // . fitWorld() . zoomIn()
        // .setView([48.862725, 2.287592000000018], 10);

        this.pins = [];
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(this.OSM );
        this.keyLat = keyLat;
        this.keyLong = keyLong;
        this.keyLat = keyLat;
        for(let key in initialData){
            if (initialData.hasOwnProperty(key)) {
                this.addPin(initialData[key][keyLat],initialData[key][keyLong],key,initialData[key][keyText]);
            }
        }
    }
    /**
     *
     * @param mapId -- id de la div
     * transforme l'array en un objet avant de créer une Map
     * @param initialData -- données que tu veux afficher (type array)
     * @param keyId -- clé pour accéder à la valeur :id
     * @param keyLat -- clé pour accéder à la valeur :latitude
     * @param keyLong -- clé pour accéder à la valeur :longitude
     * @param keyText -- clé pour accéder à la valeur :text affiché
     */
    static constructorWithArray(mapId, initialData,keyId,keyLat,keyLong,keyText) {
//        console.log('in constructor: initialData'); console.log(initialData);
    	let objectFromArray = {};
        initialData.forEach(
        		function (element) {
            objectFromArray[element[keyId]] = element;
        })
        console.log( 'constructorWithArray: objectFromArray: ' ) ; console.log( objectFromArray )
        return new Map(mapId, objectFromArray, keyLat,keyLong,keyText);
    }

    /**
     * affiche tous les points enregistré
     * @param latitude -- la latitude du point
     * @param longitude -- la latitude du point
     * @param key -- la clé pour la sauvegarde du point
     * @param text -- le texte
     */
    pinShowAll() {
        "use strict";
        for (let key in this.pins) {
            if (this.pins.hasOwnProperty(key)) {
                this.pinShow(key);

            }
        }
    }

    pinShow(key){
        "use strict";
        this.pins[key].addTo(this.OSM);
    }


    addPin(latitude,longitude, key, text) {
        "use strict";
        this.pins[key] = L.marker([latitude,longitude])
            .bindPopup(text);
        this.pinShow(key);
    }
    /**
     * affiche un point et efface les autres
     * @param latitude -- la latitude du point
     * @param longitude -- la latitude du point
     * @param key -- la clé pour la sauvegarde du point
     * @param text -- le texte
     */
    pinShowOne(key) {
        "use strict";
        this.pinHideAll();
        this.pinShow(key);
    }

    /**
     * efface un point
     * @param uri -- la clé du point a ne plus afficher
     */
    pinHide(key) {
        "use strict";
        if(this.pins[uri] !== undefined){
            let marker = this.pins[key];
            this.OSM.removeLayer(marker);
        }
    }

    /**
     * efface tous les points
     */
    pinHideAll() {
        "use strict";
        for (let key in this.pins){
            if (this.pins.hasOwnProperty(key)) {
                this.pinHide(key);
            }
        }
    }

}
