#!/usr/bin/python

import sys
import time
import math
import commands
import subprocess
import shlex
import os

def ensure_dir(f):
    d = os.path.dirname(f)
    if not os.path.exists(d):
        os.makedirs(d)

HOME  = "/afs/ir/users/f/r/frankchn/cs224n/pa1-mt"
MOSES = "/afs/ir/class/cs224n/bin/mosesdecoder"
GIZA  = "/afs/ir/class/cs224n/bin/giza-pp-read-only/external-bin-dir"

# max_phrase_length = 6
# distortion_limit = 4
# with_pairwise = False

for max_phrase_length in range(6, 7, 10):

	extract = [MOSES + "/scripts/training/train-model.perl"]
	extract.append("--max-phrase-length " + str(max_phrase_length))
	extract.append("--external-bin-dir " + GIZA)
	extract.append("--first-step 4")
	extract.append("--last-step 9")
	extract.append("-root-dir " + HOME + "/train")
	extract.append("-corpus " + HOME + "/training/corpus")
	extract.append("-f f")
	extract.append("-e e")
	extract.append("-alignment-file " + HOME + "/training/corpus")
	extract.append("-alignment align")
	extract.append("-lm 0:3:" + HOME + "/lm.bin:8")

	extract_exec = " ".join(extract)

	ensure_dir(HOME + "/train/model/")
	subprocess.call(shlex.split(extract_exec))

	for distortion_limit in range(4, 5, 10):

		with_pairwise = True
		tune = [MOSES + "/scripts/training/mert-moses.pl"]
		tune.append("--working-dir " + HOME + "/tune")
		tune.append("--decoder-flags=\"-distortion-limit " + str(distortion_limit) + " -threads 8\"")
		tune.append(HOME + "/mt-dev.fr")
		tune.append(HOME + "/mt-dev.en")
		tune.append(MOSES + "/bin/moses")
		tune.append(HOME + "/train/model/moses.ini")
		tune.append("--mertdir " + MOSES + "/bin/")
		if with_pairwise:
			tune.append("--pairwise-ranked")
			
		tune_exec = " ".join(tune)
		ensure_dir(HOME + "/tune/")
		subprocess.call(shlex.split(tune_exec))
		os.system("cat " + HOME + "/mt-dev-test.fr | " + MOSES + "/bin/moses -du -f " + HOME + "/tune/moses.ini > " + HOME + "/test-" + str(max_phrase_length) + "-" + str(distortion_limit) + "-" + str(with_pairwise) + ".out")
