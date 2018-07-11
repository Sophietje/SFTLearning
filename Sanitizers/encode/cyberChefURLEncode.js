var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts problematic characters into percent encoding, all special chars are also encoded
var reverse = chef.bake(process.argv[2], [{"op":"URL Encode", "args":[true]}]).then(result => console.log(result.result));