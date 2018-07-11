var chef = require("./CyberChef/build/node/Cyberchef.js");
// Remove spaces, carriage returns, line feeds, tabs, form feeds and full stops
var reverse = chef.bake(process.argv[2], [{"op":"Remove whitespace", "args":[true, true, true, true, true, true]}]).then(result => console.log(result.result));