#!/bin/sh
#------------------------------------------------------------------------------
cd ${0%/*} || exit 1    # run from this directory

echo "create Parser.java and Scanner.java for the Coco grammar"
echo "    java -jar Coco.jar Coco.atg"
echo

java -jar Coco.jar Coco.atg
echo

# ----------------------------------------------------------------- end-of-file
