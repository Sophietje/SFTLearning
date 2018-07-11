var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts all characters to their unicode-escaped (prefix = "\") value
var reverse = chef.bake(process.argv[2], [{"op":"Escape Unicode Characters", "args":["\\u", true, 4, true]}]).then(result => console.log(result.result));