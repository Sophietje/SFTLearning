var xss = require("xss");
console.log(xss(process.argv.slice(2).join(" ")));
