#/bin/bash

# Only permit alphanumeric & numbers, exclude any special characters
gsed -u "s/\([^a-zA-Z0-9]\)//g"
