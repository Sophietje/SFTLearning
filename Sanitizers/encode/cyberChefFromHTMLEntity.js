var chef = require("./CyberChef/build/node/Cyberchef.js");
// Converts html entities to utf-8
var reverse = chef.bake(process.argv[2], [{"op":"From HTML Entity", "args":[]}]).then(result => console.log(result.result));