var eg = require('escape-goat')
console.log(eg.escape(process.argv.slice(2).join(" ")));