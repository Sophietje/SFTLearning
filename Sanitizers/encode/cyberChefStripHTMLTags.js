var chef = require("./CyberChef/build/node/Cyberchef.js");
// Removes all html tags (<something here>) from the input
var reverse = chef.bake(process.argv[2], [{"op":"Strip HTML tags", "args":[false, false]}]).then(result => console.log(result.result));