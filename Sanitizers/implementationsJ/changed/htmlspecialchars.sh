#/bin/bash

# Change
# Instead of replacing " with &quot; we replace ''
gsed -u "
s/&/\&amp;/g 
s/</\&lt;/g 
s/>/\&gt;/g 
s/''/\&quot;/g
"
