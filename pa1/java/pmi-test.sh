#!/bin/bash
ant
java -cp ~/cs224n/pa1/java/classes cs224n.assignments.WordAlignmentTester \
-dataPath /afs/ir/class/cs224n/pa1/data/ \
-model cs224n.wordaligner.PMIModel -evalSet dev \
-trainSentences 200
