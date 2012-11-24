#!/bin/bash
./ant
time java -cp ~/cs224n/pa2/java/classes \
          -mx800m cs224n.assignments.PCFGParserTester \
          -path /afs/ir/class/cs224n/pa2/data/ \
          -data miniTest \
