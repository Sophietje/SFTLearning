var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts all unicode-escaped (prefix = "\") characters into their 'raw' values
var reverse = chef.bake(process.argv[2], [{"op":"Unescape Unicode Characters", "args":["\\u"]}]).then(result => console.log(result.result));