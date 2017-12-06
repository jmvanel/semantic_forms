class Map{

    /**
     *
     * @param mapId -- id de la div
     * ces quatre paramètres permettre, a l'affichage d'une page, d'initialiser un certain contenu
     * @param initialData -- données que tu veux afficher
     * @param keyLat -- clé pour accéder à la valeur :latitude
     * @param keylong -- clé pour accéder à la valeur :longitude
     * @param keyText -- clé pour accéder à la valeur :text affiché
     */
    constructor(mapId,initialData,keyLat,keylong,keyText) {
        this.OSM = L.map(mapId).setView([48.862725, 2.287592000000018], 10);
        this.pins = [];
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(this.OSM );
        this.keyLat = keyLat;
        this.keyLong = keylong;
        this.keyLat = keyLat;
        for(let key in initialData){
            if (initialData.hasOwnProperty(key)) {
                this.addPin(initialData[key][keyLat],initialData[key][keylong],key,initialData[key][keyText]);
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
     * @param keylong -- clé pour accéder à la valeur :longitude
     * @param keyText -- clé pour accéder à la valeur :text affiché
     */
    static constructorWithArray(mapId,initialData,keyId,keyLat,keylong,keyText) {
        console.log('in constructor');
        console.log(initialData);
    	let objectFromArray = {};
        Array.prototype.forEach.call( initialData,
//        initialData.forEach(
        		function (element) {
            objectFromArray[element[keyId]] = element;
        })
        console.log( 'objectFromArray' )
        console.log( objectFromArray )
        return new Map(mapId,objectFromArray,keyLat,keylong,keyText);
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
