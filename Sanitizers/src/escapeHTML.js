console.log(escapeHTML(process.argv.slice(2).join(" ")));

function escapeHTML(p1) {
    var he = require('he');
    return he.escape(p1);
}