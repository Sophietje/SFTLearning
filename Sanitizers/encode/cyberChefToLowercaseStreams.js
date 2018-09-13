var chef = require("./CyberChef/build/node/CyberChef.js");
var readline = require('readline');

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', function (line) {
    chef.bake(line, [{"op":"To Lower case", "args":[]}]).then(result => console.log(result.result));
});