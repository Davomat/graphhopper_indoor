var decodePath = function (encoded, is3D,isIndoor) {
    // var start = new Date().getTime();
    var len = encoded.length;
    var index = 0;
    var array = [];
    var coordinates = [];
    var levels = [];
    var lat = 0;
    var lng = 0;
    var ele = 0;
    var level = 0;

    while (index < len) {
        var b;
        var shift = 0;
        var result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        var deltaLat = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lat += deltaLat;

        shift = 0;
        result = 0;
        do {
            b = encoded.charCodeAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        var deltaLon = ((result & 1) ? ~(result >> 1) : (result >> 1));
        lng += deltaLon;

        if (is3D) {
            // elevation
            shift = 0;
            result = 0;
            do
            {
                b = encoded.charCodeAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            var deltaEle = ((result & 1) ? ~(result >> 1) : (result >> 1));
            ele += deltaEle;
            array.push([lng * 1e-5, lat * 1e-5, ele / 100]);
            coordinates.push([lng * 1e-5, lat * 1e-5, ele / 100]);
        } 
        if (isIndoor){
            shift = 0;
            result = 0;
            do
            {
                b = encoded.charCodeAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            var deltaLevel = ((result & 1) ? ~(result >> 1) : (result >> 1));
            level += deltaLevel;
            array.push([lng * 1e-5, lat * 1e-5, level]);
            coordinates.push([lng * 1e-5, lat * 1e-5]);
            levels.push(level);
        }
        else
            array.push([lng * 1e-5, lat * 1e-5]);
    }
    // var end = new Date().getTime();
    // console.log("decoded " + len + " coordinates in " + ((end - start) / 1000) + "s");
    var path = {"coordinates": coordinates,"levels": levels};
    //return array;
    return path;
};

var indoorPoint = function(latlng,level){
    return {"lat": latlng.lat, "lng":latlng.lng,"level": level}
}

module.exports.decodePath = decodePath;
module.exports.indoorPoint = indoorPoint;