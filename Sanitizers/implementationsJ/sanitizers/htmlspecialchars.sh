#/bin/bash

gsed -u "
s/&/\&amp;/g 
s/</\&lt;/g 
s/>/\&gt;/g 
s/\"/\&quot/g
"
