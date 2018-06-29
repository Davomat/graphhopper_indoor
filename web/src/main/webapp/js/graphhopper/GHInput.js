function round(val, precision) {
    if (precision === undefined)
        precision = 1e6;
    return Math.round(val * precision) / precision;
}

var GHInput = function (input) {
    this.set(input);
};

GHInput.isObject = function (value) {
    var stringValue = Object.prototype.toString.call(value);
    return (stringValue.toLowerCase() === "[object object]");
};

GHInput.isString = function (value) {
    var stringValue = Object.prototype.toString.call(value);
    return (stringValue.toLowerCase() === "[object string]");
};

GHInput.prototype.isResolved = function () {
    return !isNaN(this.lat) && !isNaN(this.lng);
};

GHInput.prototype.setCoord = function (lat, lng) {
    this.lat = round(lat);
    this.lng = round(lng);
};

GHInput.prototype.setLevel = function (level){
    this.level = level;
};

GHInput.prototype.setUnresolved = function () {
    this.lat = undefined;
    this.lng = undefined;
    this.level = undefined;
};

GHInput.prototype.set = function (strOrObject) {
    // either text or coordinates or object
    //this.input = strOrObject;
    // reset to unresolved


    if (GHInput.isObject(strOrObject)) {
        this.setCoord(strOrObject.lat, strOrObject.lng);
        this.setLevel(strOrObject.level);
    } else if (GHInput.isString(strOrObject)) {
        var pointComponents = strOrObject.split(",");
        if (pointComponents.length === 3) {
            this.lat = round(parseFloat(pointComponents[0]));
            this.lng = round(parseFloat(pointComponents[1]));
            this.level = pointComponents[2];
            if (this.isResolved()) {
                this.input = this.toString();
            } else {
                this.setUnresolved();
            }
        } else {
            this.setUnresolved();
        }
    }
    this.input = this.toString();
};

GHInput.prototype.toString = function () {
    if (this.lat !== undefined && this.lng !== undefined)
        return this.lat + "," + this.lng;
    return undefined;
};

module.exports = GHInput;
