var sanitizer = require('sanitizer');
console.log(sanitizer.sanitize(process.argv.slice(2).join(" ")));