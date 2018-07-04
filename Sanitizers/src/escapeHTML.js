var result = escapeHTML(process.argv.slice(2).join(" "));

function escapeHTML(p1) {
    var he = require('he');
    console.log(he.escape(p1));
}