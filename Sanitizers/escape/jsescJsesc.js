var jsesc = require('jsesc');
console.log(jsesc(process.argv.slice(2).join(" ")));
