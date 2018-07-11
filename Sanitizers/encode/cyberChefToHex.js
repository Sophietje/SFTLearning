var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts input from UTF-8 to hex
var reverse = chef.bake(process.argv[2], [{"op":"To Hex", "args":["Space"]}]).then(result => console.log(result.result));