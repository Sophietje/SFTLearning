var he = require('he')
console.log(he.encode(process.argv.slice(2).join(" ")));