all:
	javac -d . -source 1.4 -target 1.4 Trace.java Scanner.java Tab.java DFA.java ParserGen.java Parser.java Coco.java
	jar cfm Coco.jar Coco.manifest Coco/*.class
	rm -rf Coco

clean:
	rm -f Coco.jar

install:
	install -m 0755 cocoj $(DESTDIR)/usr/bin
	install -m 0644 Coco.jar $(DESTDIR)/usr/share/coco-java
	install -m 0644 *frame $(DESTDIR)/usr/share/coco-java

