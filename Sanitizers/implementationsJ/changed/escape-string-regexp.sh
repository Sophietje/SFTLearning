#/bin/bash

# Lol idk, hope this is correct
gsed -u "
s/\(?\|(\|)\|\*\|+\|-\|\.\|\[\|\]\|\\\ \|{\|}\|\\$ \||\|\^\)/\\\&/g
"
