#!/bin/sh
#------------------------------------------------------------------------------
cd ${0%/*} || exit 1    # run from this directory

echo "create Parser.java and Scanner.java for the Coco grammar"
echo "    java -jar Coco.jar src/Coco/Coco-java.atg -bak"
echo

java -jar Coco.jar src/Coco/Coco-java.atg -bak
echo

# ----------------------------------------------------------------- end-of-file
