#!/bin/sh
#------------------------------------------------------------------------------
cd ${0%/*} || exit 1    # run from this directory

echo "create Parser.java and Scanner.java for the Coco grammar"
echo

package=Coco

echo "java -jar Coco.jar Coco.atg -package $package"

java -jar Coco.jar Coco.atg -package Coco
echo

# ----------------------------------------------------------------- end-of-file
