#!/usr/bin/python
import sys

if __name__ == "__main__":
    while (True):
        input = sys.stdin.readline()
        sys.stdout.write(input.replace("<", ""))
        sys.stdout.flush()