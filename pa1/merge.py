#!/usr/bin/python

f1=open("test-6-4-0.out")        
f2=open("mt-dev-test.en")
for line in f1:
    print line.strip()
    print f2.readline().strip()
    print ""
f1.close()
f2.close()
