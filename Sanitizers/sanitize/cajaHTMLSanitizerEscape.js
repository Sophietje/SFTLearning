var sanitizer = require('sanitizer');
console.log(sanitizer.escape(process.argv.slice(2).join(" ")));