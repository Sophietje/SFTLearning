var sanitizer = require('sanitizer');
console.log(sanitizer.unescapeEntities(process.argv.slice(2).join(" ")));