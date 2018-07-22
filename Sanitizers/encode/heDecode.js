var he = require('he')
console.log(he.decode(process.argv.slice(2).join(" ")));