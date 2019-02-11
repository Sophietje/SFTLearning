#/bin/bash

gsed -u "
s/\(.*\)/\L\1/g
"
