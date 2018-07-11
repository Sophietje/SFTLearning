var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts input from hex to UTF-8
var reverse = chef.bake(process.argv[2], [{"op":"From Hex", "args":["Space"]}]).then(result => console.log(result.result));