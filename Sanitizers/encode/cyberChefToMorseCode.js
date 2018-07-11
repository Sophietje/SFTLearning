var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts all characters to their unicode-escaped (prefix = "\") value
var reverse = chef.bake(process.argv[2], [{"op":"To Morse Code", "args":["-/.", "Space", "Line feed"]}]).then(result => console.log(result.result));