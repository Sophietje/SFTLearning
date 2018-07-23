var escapeStringRegexp = require('escape-string-regexp');
console.log(escapeStringRegexp(process.argv.slice(2).join(" ")));
