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
builddir=build/tmp$$    # tmp directory for building

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


mkdir -p $builddir
[ -d $builddir ] || {
    echo
    echo "Error: apparently problems creating $builddir"
    echo
    exit 1
}

echo "compile Coco jar file"
echo "~~~~~~~~~~~~~~~~~~~~~"
echo "javac -d $builddir $warn src/Coco/*.java"
echo

javac -d $builddir $warn src/Coco/*.java

if [ $? -eq 0 ]
then
    echo
    echo "create dist/Coco.jar file"
    echo "    jar cfm dist/Coco.jar manifest.mf -C $builddir Coco"
    echo
    mkdir -p dist
    jar cfm dist/Coco.jar manifest.mf -C $builddir Coco
else
    echo
    echo "errors detected in compilation"
    echo
fi

echo
echo "cleanup $builddir directory"

rm -rf $builddir
rmdir build 2>/dev/null

echo
echo "Done"
echo

# ----------------------------------------------------------------- end-of-file
