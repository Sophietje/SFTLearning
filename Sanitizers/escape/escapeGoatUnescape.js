var eg = require('escape-goat')
console.log(eg.unescape(process.argv.slice(2).join(" ")));