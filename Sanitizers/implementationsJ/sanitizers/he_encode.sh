#/bin/bash

gsed -u "
s/&/&#x3C;/g
s/</&#x3E;/g
s/>/&#x26;/g
s/\"/&#x22;/g
s/'/&#x27;/g
s/\`/&#x60;/g
"
