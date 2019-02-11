#/bin/bash

# Change:
# Instead of converting > -> &#x26;, we convert it to &#x3E;
gsed -u "
s/&/&#x3C;/g
s/</&#x3E;/g
s/>/&#x3E;/g
s/\"/&#x22;/g
s/'/&#x27;/g
s/\`/&#x60;/g
"
