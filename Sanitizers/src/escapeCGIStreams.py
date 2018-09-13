#!/usr/bin/python
import sys
from cgi import escape

if __name__ == "__main__":
    while (True):
        input = sys.stdin.readline()
        sys.stdout.write(escape(input))
        sys.stdout.flush()