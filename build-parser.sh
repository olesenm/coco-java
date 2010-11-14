#!/bin/sh
#------------------------------------------------------------------------------
cd ${0%/*} || exit 1    # run from this directory
atg=src/Coco/Coco-java.atg

echo "create Parser.java and Scanner.java for the Coco grammar"

# use Coco.jar from dist/ if possible, or use 'coco-cpp' from the PATH
if [ -f dist/Coco.jar ]
then
    echo "using Coco.jar from dist/"
    echo "    java -jar dist/Coco.jar $atg -bak"
    echo
    java -jar dist/Coco.jar $atg -bak
elif type coco-java >/dev/null 2>&1
then
    echo "using coco-java from PATH"
    echo "    coco-java $atg -bak"
    echo
    coco-java $atg -bak
else
    echo "Error: no wrapper or suitable Coco.jar found"
    echo
    exit 1
fi

# ----------------------------------------------------------------- end-of-file
