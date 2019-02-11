#/bin/bash


# Change:
# Perform the same substitution twice over the same stream
gsed -u "
s/&/\&amp;/g 
s/</\&lt;/g 
s/>/\&gt;/g 
" | gsed -u "
s/&/\&amp;/g 
s/</\&lt;/g 
s/>/\&gt;/g 
"
