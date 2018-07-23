var chef = require("./CyberChef/build/node/CyberChef.js");
// Converts all upper case letters to lower case letters
var reverse = chef.bake(process.argv[2], [{"op":"To Lower case", "args":[]}]).then(result => console.log(result.result));