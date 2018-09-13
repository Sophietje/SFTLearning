var escapeStringRegexp = require('escape-string-regexp');
var readline = require('readline');

var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', function (line) {
  console.log(escapeStringRegexp(line));
});