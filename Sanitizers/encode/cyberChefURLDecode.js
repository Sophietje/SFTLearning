var chef = require("./CyberChef/build/node/CyberChef.js");
// Converts URL/URI percent encoded characters to their 'raw' values
var reverse = chef.bake(process.argv[2], [{"op":"URL Decode", "args":[]}]).then(result => console.log(result.result));