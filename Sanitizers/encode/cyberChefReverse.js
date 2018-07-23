var chef = require("./CyberChef/build/node/CyberChef.js");
// Reverses the input
var reverse = chef.bake(process.argv[2], [{"op":"Reverse", "args":[]}]).then(result => console.log(result.result));