var chef = require("./CyberChef/build/node/Cyberchef.js");
// Removes null bytes from input
var reverse = chef.bake(process.argv[2], [{"op":"Remove null bytes", "args":[]}]).then(result => console.log(result.result));