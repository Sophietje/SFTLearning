var chef = require("./CyberChef/build/node/CyberChef.js");
// Escape all special characters in a string with a backslash
var reverse = chef.bake(process.argv[2], [{"op":"Escape string", "args":["Special chars", "Single", false, false, false]}]).then(result => console.log(result.result));