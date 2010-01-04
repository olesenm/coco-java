#!/bin/sh
#------------------------------------------------------------------------------
usage() {
    while [ "$#" -ge 1 ]; do echo "$1"; shift; done
    cat<<USAGE

usage: ${0##*/} [OPTION]
options:
  -parser        create new scanner/parser code for Coco/R first
  -warn          enable additional warnings

* compile Coco.jar

USAGE
    exit 1
}
#------------------------------------------------------------------------------
cd ${0%/*} || exit 1    # run from this directory

unset warn

# parse options
while [ "$#" -gt 0 ]
do
    case "$1" in
    -h | -help)
        usage
        ;;
    -parser)
        ./build-parser.sh
        ;;
    -warn)
        warn="-Xlint:unchecked"
        ;;
    *)
        usage "unknown option/argument: '$*'"
        ;;
    esac
    shift
done


echo "compile Coco executable"
echo "~~~~~~~~~~~~~~~~~~~~~~~"
echo "javac -d src $warn src/*.java"
echo

javac -d src $warn src/*.java
if [ $? -eq 0 ]
then
    echo
    echo "done"
    echo "create Coco.jar file"
    echo "    jar cfm Coco.jar src/Coco.manifest -C src Coco"
    echo
    jar cfm Coco.jar src/Coco.manifest -C src Coco
    rm -rf src/Coco/
else
    echo
    echo "errors detected in compilation"
    echo
fi

# ----------------------------------------------------------------- end-of-file
