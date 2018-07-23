var chef = require("./CyberChef/build/node/CyberChef.js");
// Converts characters to HTML entities
var reverse = chef.bake(process.argv[2], [{"op":"To HTML Entity", "args":[]}]).then(result => console.log(result.result));