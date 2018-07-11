var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts all lower case letters to upper case letters
var reverse = chef.bake(process.argv[2], [{"op":"To Upper case", "args":[]}]).then(result => console.log(result.result));