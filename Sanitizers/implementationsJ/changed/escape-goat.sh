#/bin/bash

# Change:
# & substitution in the wrong place

gsed -u "
s/</\&lt;/g
s/>/\&gt;/g
s/&/\&amp;/g
s/\"/\&quot;/g
s/'/\&#39;/g
"
