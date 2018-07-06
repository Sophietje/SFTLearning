#!/usr/bin/python
import sys
from cgi import escape

if __name__ == "__main__":
    if len(sys.argv) > 0 :
        print(escape(sys.argv[1]))
