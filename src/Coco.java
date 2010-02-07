/*---------------------------------------------------------------------------*\
    Compiler Generator Coco/R,
    Copyright (c) 1990, 2004 Hanspeter Moessenboeck, University of Linz
    extended by M. Loeberbauer & A. Woess, Univ. of Linz
    ported from C# to Java by W. Ahorner
    with improvements by Pat Terry, Rhodes University
-------------------------------------------------------------------------------
License
    This file is part of Compiler Generator Coco/R

    This program is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the
    Free Software Foundation; either version 2, or (at your option) any
    later version.

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
    for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

    As an exception, it is allowed to write an extension of Coco/R that is
    used as a plugin in non-free software.

    If not otherwise stated, any source code generated by Coco/R (other than
    Coco/R itself) does not fall under the GNU General Public License.
\*---------------------------------------------------------------------------*/
/*!

@mainpage The Compiler Generator Coco/R, Java version.

http://www.ssw.uni-linz.ac.at/coco/

@section usage Program Usage

@verbatim
coco-java Grammar.atg {Option}
Options:
  -package   <Name>      eg, My.Package.Space
  -prefix    <Name>      for unique Parser/Scanner file names
  -frames    <Dir>       for frames not in the source directory
  -trace     <String>    trace with output to trace.txt
  -o         <Dir>       output directory
  -bak                   save existing Parser/Scanner files as .bak
  -help                  print this usage
@endverbatim
The valid trace string values are listed below.

The Scanner.frame and Parser.frame must be located in one of these
directories:
-# The same directory as the atg grammar.
-# In the specified -frames directory.

Unless specified with the @em -o option, the generated scanner/parser
files are written in the same directory as the atg grammar!

@section trace Trace output options

 - 0 | A: prints the states of the scanner automaton
 - 1 | F: prints the First and Follow sets of all nonterminals
 - 2 | G: prints the syntax graph of the productions
 - 3 | I: traces the computation of the First sets
 - 4 | J: prints the sets associated with ANYs and synchronisation sets
 - 6 | S: prints the symbol table (terminals, nonterminals, pragmas)
 - 7 | X: prints a cross reference list of all syntax symbols
 - 8 | P: prints statistics about the Coco run
 - 9    : unused

The trace output can be switched on as a command-line option or by
the pragma:
@verbatim
    $ { digit | letter }
@endverbatim
in the attributed grammar.

The extended directive format may also be used in the attributed grammar:
@verbatim
    $trace=(digit | letter){ digit | letter }
@endverbatim

@section Compiler Directives (Extended Pragmas)

To improve the reliability of builds in complex environments, it is
possible to specify the desired namespace and/or file prefix as a
directive within the grammar. For example, when compiling the
Coco-java.atg itself, it can be compiled within the 'Coco' namespace
as specified on the command-line. For example,

@verbatim
    coco-java -package Coco Coco-java.atg
@endverbatim

As an alternative, it can be specified within the Coco-java.atg file:
@verbatim
    COMPILER Coco
    $package=Coco
@endverbatim

For completeness, it is also possible to add in trace string parameters
with the same syntax. For example,

@verbatim
    COMPILER GramC
    $trace=ags
@endverbatim

*/
/*-------------------------------------------------------------------------*/
package Coco;

import java.io.File;

//! Entry point for standalone coco-java
public class Coco
{
	public static void printUsage (String mesage) {
		System.out.println
		(
			"Usage: coco-java Grammar.atg {Option}\n" +
			"Options:\n" +
			"  -package <Name>      eg, My.Package.Name\n" +
			"  -prefix  <Name>      for unique Parser/Scanner file names\n" +
			"  -frames  <Dir>       for frames not in the source directory\n" +
			"  -trace   <String>    trace with output to trace.txt\n" +
			"  -o       <Dir>       output directory\n" +
			"  -bak                 save existing Parser/Scanner files as .bak\n" +
			"  -help                print this usage\n" +
			"\nValid characters in the trace string:\n" +
			"  A  trace automaton\n" +
			"  F  list first/follow sets\n" +
			"  G  print syntax graph\n" +
			"  I  trace computation of first sets\n" +
			"  J  list ANY and SYNC sets\n" +
			"  P  print statistics\n" +
			"  S  list symbol table\n" +
			"  X  list cross reference table\n" +
			"Scanner.frame and Parser.frame must be located in one of these directories:\n" +
			"  1. The same directory as the atg grammar.\n" +
			"  2. In the specified -frames directory.\n" +
			"\nhttp://www.ssw.uni-linz.ac.at/coco/\n\n"
		);
	}

	public static void main (String[] arg) {
		System.out.println("Coco/R Java (7 Feb 2010)");
		String srcName = null, nsName = null, prefixName = null;
		String frameDir = null, ddtString = null, outDir = null;
		boolean makeBackup = false;
		int retVal = 1;

		for (int i = 0; i < arg.length; i++) {
			if (arg[i].compareTo("-help") == 0) {
				printUsage(null);
				System.exit(0);
			}
			else if (arg[i].compareTo("-package") == 0) {
				if (++i == arg.length) {
					printUsage("missing parameter on -package");
					System.exit(retVal);
				}
				nsName = arg[i];
			}
			else if (arg[i].compareTo("-prefix") == 0) {
				if (++i == arg.length) {
					printUsage("missing parameter on -prefix");
					System.exit(retVal);
				}
				prefixName = arg[i];
			}
			else if (arg[i].compareTo("-frames") == 0) {
				if (++i == arg.length) {
					printUsage("missing parameter on -frames");
					System.exit(retVal);
				}
				frameDir = arg[i];
			}
			else if (arg[i].compareTo("-trace") == 0) {
				if (++i == arg.length) {
					printUsage("missing parameter on -trace");
					System.exit(retVal);
				}
				ddtString = arg[i];
			}
			else if (arg[i].compareTo("-o") == 0) {
				if (++i == arg.length) {
					printUsage("missing parameter on -o");
					System.exit(retVal);
				}
				outDir = arg[i];
			}
			else if (arg[i].compareTo("-bak") == 0) {
				makeBackup = true;
			}
			else if (arg[i].charAt(0) == '-') {
				printUsage("Error: unknown option: '" + arg[i] + "'");
				System.exit(retVal);
			}
			else if (srcName != null) {
				printUsage("grammar can only be specified once");
				System.exit(retVal);
			}
			else {
				srcName = arg[i];
			}
		}

		if (srcName != null) {
			try {
				String srcDir = new File(srcName).getParent();

				Scanner scanner = new Scanner(srcName);
				Parser parser   = new Parser(scanner);
				parser.tab      = new Tab(parser);

				parser.tab.srcName    = srcName;
				parser.tab.srcDir     = srcDir;
				parser.tab.nsName     = nsName;
				parser.tab.prefixName = prefixName;
				parser.tab.frameDir = frameDir;
				parser.tab.outDir   = (outDir != null) ? outDir : srcDir;
				parser.tab.SetDDT(ddtString);
				parser.tab.makeBackup = makeBackup;

				parser.tab.trace = new Trace(parser.tab.outDir);
				parser.dfa = new DFA(parser);
				parser.pgen = new ParserGen(parser);

				parser.Parse();

				parser.tab.trace.Close();
				System.out.println(parser.errors.count + " errors detected");
				if (parser.errors.count == 0) { retVal = 0; }
			} catch (FatalError e) {
				System.out.println(e.getMessage());
			}
		} else {
			printUsage(null);
		}
		System.exit(retVal);
	}

} // end Coco


// ************************************************************************* //
