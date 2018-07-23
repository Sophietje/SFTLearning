var chef = require("./CyberChef/build/node/CyberChef.js");
// Unescapes characters that have been escaped
var reverse = chef.bake(process.argv[2], [{"op":"Unescape string", "args":[]}]).then(result => console.log(result.result));