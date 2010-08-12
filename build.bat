cd src
md build

cd Coco
javac -source 1.5 -target 1.5 -d ..\build Trace.java Scanner.java Tab.java DFA.java ParserGen.java Parser.java Coco.java
cd ..

jar cfm Coco.jar ..\manifest.mf -C build Coco

del build\Coco\*.class
rd  build\Coco
rd  build
cd ..
