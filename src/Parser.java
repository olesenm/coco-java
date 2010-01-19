
/*-------------------------------------------------------------------------
Compiler Generator Coco/R,
Copyright (c) 1990, 2004 Hanspeter Moessenboeck, University of Linz
extended by M. Loeberbauer & A. Woess, Univ. of Linz
ported from C# to Java by W. Ahorner
with improvements by Pat Terry, Rhodes University

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
-------------------------------------------------------------------------*/
package Coco;

public class Parser {
	public static final int _EOF = 0;
	public static final int _ident = 1;
	public static final int _number = 2;
	public static final int _string = 3;
	public static final int _badString = 4;
	public static final int _char = 5;
	public static final int maxT = 50;
	public static final int _ddtSym = 51;
	public static final int _directive = 52;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;

	public Scanner scanner;
	public Errors errors;

static final int isIdent   = 0;
	static final int isLiteral = 1;

	public Tab tab;             // other Coco objects referenced by this ATG
	public DFA dfa;
	public ParserGen pgen;

	boolean genScanner = false;
	String tokenString;         //!< used in declarations of literal tokens
	String noString = "-none-"; //!< used in declarations of literal tokens

/*-------------------------------------------------------------------------*/



	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
		// user-defined initialization:

	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}

	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			if (la.kind == 51) {
				tab.SetDDT(la.val); 
			}
			if (la.kind == 52) {
				tab.DispatchDirective(la.val); 
			}
			la = t;
		}
	}

	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}

	boolean StartOf (int s) {
		return set[s][la.kind];
	}

	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}

	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}

	void Coco() {
		Symbol sym; Graph g; String grammarName; CharSet s; 
		if (la.kind == 6) {
			Get();
			int beg = t.pos + t.val.length(); 
			while (StartOf(1)) {
				Get();
			}
			tab.copyPos = new Position(beg, la.pos, 0); 
			Expect(7);
		}
		if (StartOf(2)) {
			Get();
			int beg = t.pos; 
			while (StartOf(3)) {
				Get();
			}
			pgen.preamblePos = new Position(beg, la.pos, 0); 
		}
		Expect(8);
		genScanner = true; 
		Expect(1);
		grammarName = t.val; 
		if (StartOf(4)) {
			Get();
			int beg = t.pos; 
			while (StartOf(4)) {
				Get();
			}
			pgen.semDeclPos = new Position(beg, la.pos, 0); 
		}
		if (la.kind == 9) {
			Get();
			if (StartOf(5)) {
				Get();
				int beg = t.pos; 
				while (StartOf(5)) {
					Get();
				}
				pgen.initCodePos = new Position(beg, la.pos, 0); 
			}
			Expect(10);
		}
		if (la.kind == 11) {
			Get();
			while (StartOf(6)) {
				Get();
			}
			Expect(12);
			SemErr("destroy is not implemented in Java version"); 
		}
		if (la.kind == 13) {
			Get();
			dfa.ignoreCase = true; 
		}
		if (la.kind == 14) {
			Get();
			while (la.kind == 1) {
				SetDecl();
			}
		}
		if (la.kind == 15) {
			Get();
			while (la.kind == 1 || la.kind == 3 || la.kind == 5) {
				TokenDecl(Node.t);
			}
		}
		if (la.kind == 16) {
			Get();
			while (la.kind == 1 || la.kind == 3 || la.kind == 5) {
				TokenDecl(Node.pr);
			}
		}
		while (la.kind == 17) {
			Get();
			Graph g1, g2; boolean nested = false; 
			Expect(18);
			g1 = TokenExpr();
			Expect(19);
			g2 = TokenExpr();
			if (la.kind == 20) {
				Get();
				nested = true; 
			}
			dfa.NewComment(g1.l, g2.l, nested); 
		}
		while (la.kind == 21) {
			Get();
			s = Set();
			tab.ignored.Or(s); 
		}
		while (!(la.kind == 0 || la.kind == 22)) {SynErr(51); Get();}
		Expect(22);
		if (genScanner) dfa.MakeDeterministic();
		tab.DeleteNodes();
		
		while (la.kind == 1) {
			Get();
			sym = tab.FindSym(t.val);
			boolean undef = (sym == null);
			if (undef) sym = tab.NewSym(Node.nt, t.val, t.line);
			else {
			  if (sym.typ == Node.nt) {
			    if (sym.graph != null)
			      SemErr("name declared twice");
			  } else SemErr("this symbol kind not allowed on left side of production");
			  sym.line = t.line;
			}
			boolean noAttrs = (sym.attrPos == null);
			sym.attrPos = null;
			boolean noRet = (sym.retVar == null);
			sym.retVar = null;
			
			if (la.kind == 30 || la.kind == 35) {
				AttrDecl(sym);
			}
			if (!undef &&
			   (noAttrs != (sym.attrPos == null)
			   || noRet != (sym.retVar == null)))
			   SemErr("attribute mismatch between declaration and use of this symbol");
			
			if (la.kind == 48) {
				sym.semPos = SemText();
			}
			ExpectWeak(23, 7);
			g = Expression();
			sym.graph = g.l;
			tab.Finish(g);
			
			ExpectWeak(24, 8);
		}
		Expect(25);
		Expect(1);
		if (grammarName.compareTo(t.val) != 0)
		 SemErr("name does not match grammar name");
		tab.gramSy = tab.FindSym(grammarName);
		if (tab.gramSy == null)
		  SemErr("missing production for grammar name");
		else {
		  sym = tab.gramSy;
		  if (sym.attrPos != null)
		    SemErr("grammar symbol must not have attributes");
		}
		tab.noSym = tab.NewSym(Node.t, "???", 0); // noSym gets highest number
		tab.SetupAnys();
		tab.RenumberPragmas();
		if (tab.ddt[2]) tab.PrintNodes();
		if (errors.count == 0) {
		  System.out.println("checking");
		  tab.CompSymbolSets();
		  if (tab.ddt[7]) tab.XRef();
		  if (tab.GrammarOk()) {
		    System.out.print("parser");
		    pgen.WriteParser();
		    if (genScanner) {
		      System.out.print(" + scanner");
		      dfa.WriteScanner();
		      if (tab.ddt[0]) dfa.PrintStates();
		    }
		    System.out.println(" generated");
		    if (tab.ddt[8]) {
		      tab.PrintStatistics();
		      pgen.PrintStatistics();
		    }
		  }
		}
		if (tab.ddt[6]) tab.PrintSymbolTable();
		
		Expect(24);
	}

	void SetDecl() {
		CharSet s; 
		Expect(1);
		String name = t.val;
		CharClass c = tab.FindCharClass(name);
		if (c != null) SemErr("name declared twice");
		
		Expect(23);
		s = Set();
		if (s.Elements() == 0) SemErr("character set must not be empty");
		c = tab.NewCharClass(name, s);
		
		Expect(24);
	}

	void TokenDecl(int typ) {
		SymInfo s; Symbol sym; Graph g; 
		s = Sym();
		sym = tab.FindSym(s.name);
		if (sym != null) SemErr("name declared twice");
		else {
		  sym = tab.NewSym(typ, s.name, t.line);
		  sym.tokenKind = Symbol.fixedToken;
		}
		tokenString = null;
		
		while (!(StartOf(9))) {SynErr(52); Get();}
		if (la.kind == 23) {
			Get();
			g = TokenExpr();
			Expect(24);
			if (s.kind == isLiteral) SemErr("a literal must not be declared with a structure");
			tab.Finish(g);
			if (tokenString == null || tokenString.equals(noString))
			  dfa.ConvertToStates(g.l, sym);
			else { // TokenExpr is a single string
			  if (tab.literals.get(tokenString) != null)
			    SemErr("token string declared twice");
			  tab.literals.put(tokenString, sym);
			  dfa.MatchLiteral(tokenString, sym);
			}
			
		} else if (StartOf(10)) {
			if (s.kind == isIdent) genScanner = false;
			else dfa.MatchLiteral(sym.name, sym);
			
		} else SynErr(53);
		if (la.kind == 48) {
			sym.semPos = SemText();
			if (typ != Node.pr) SemErr("semantic action not allowed here"); 
		}
	}

	Graph  TokenExpr() {
		Graph  g;
		Graph g2; 
		g = TokenTerm();
		boolean first = true; 
		while (WeakSeparator(39,11,12) ) {
			g2 = TokenTerm();
			if (first) { tab.MakeFirstAlt(g); first = false; }
			tab.MakeAlternative(g, g2);
			
		}
		return g;
	}

	CharSet  Set() {
		CharSet  s;
		CharSet s2; 
		s = SimSet();
		while (la.kind == 26 || la.kind == 27) {
			if (la.kind == 26) {
				Get();
				s2 = SimSet();
				s.Or(s2); 
			} else {
				Get();
				s2 = SimSet();
				s.Subtract(s2); 
			}
		}
		return s;
	}

	void AttrDecl(Symbol sym) {
		int beg, col; 
		if (la.kind == 30) {
			Get();
			if (la.kind == 31 || la.kind == 32) {
				if (la.kind == 31) {
					Get();
				} else {
					Get();
				}
				beg = la.pos; 
				TypeName();
				sym.retType = scanner.buffer.GetString(beg, la.pos); 
				Expect(1);
				sym.retVar = t.val; 
				if (la.kind == 33) {
					Get();
				} else if (la.kind == 34) {
					Get();
					beg = la.pos; col = la.col; 
					while (StartOf(13)) {
						if (StartOf(14)) {
							Get();
						} else {
							Get();
							SemErr("bad string in attributes"); 
						}
					}
					Expect(33);
					if (t.pos > beg)
					 sym.attrPos = new Position(beg, t.pos, col); 
				} else SynErr(54);
			} else if (StartOf(15)) {
				beg = la.pos; col = la.col; 
				while (StartOf(16)) {
					if (StartOf(17)) {
						Get();
					} else {
						Get();
						SemErr("bad string in attributes"); 
					}
				}
				Expect(33);
				if (t.pos > beg)
				 sym.attrPos = new Position(beg, t.pos, col); 
			} else SynErr(55);
		} else if (la.kind == 35) {
			Get();
			if (la.kind == 31 || la.kind == 32) {
				if (la.kind == 31) {
					Get();
				} else {
					Get();
				}
				beg = la.pos; 
				TypeName();
				sym.retType = scanner.buffer.GetString(beg, la.pos); 
				Expect(1);
				sym.retVar = t.val; 
				if (la.kind == 36) {
					Get();
				} else if (la.kind == 34) {
					Get();
					beg = la.pos; col = la.col; 
					while (StartOf(18)) {
						if (StartOf(19)) {
							Get();
						} else {
							Get();
							SemErr("bad string in attributes"); 
						}
					}
					Expect(36);
					if (t.pos > beg)
					 sym.attrPos = new Position(beg, t.pos, col); 
				} else SynErr(56);
			} else if (StartOf(15)) {
				beg = la.pos; col = la.col; 
				while (StartOf(20)) {
					if (StartOf(21)) {
						Get();
					} else {
						Get();
						SemErr("bad string in attributes"); 
					}
				}
				Expect(36);
				if (t.pos > beg)
				 sym.attrPos = new Position(beg, t.pos, col); 
			} else SynErr(57);
		} else SynErr(58);
	}

	Position  SemText() {
		Position  pos;
		Expect(48);
		int beg = la.pos; int col = la.col; 
		while (StartOf(22)) {
			if (StartOf(23)) {
				Get();
			} else if (la.kind == 4) {
				Get();
				SemErr("bad string in semantic action"); 
			} else {
				Get();
				SemErr("missing end of previous semantic action"); 
			}
		}
		Expect(49);
		pos = new Position(beg, t.pos, col); 
		return pos;
	}

	Graph  Expression() {
		Graph  g;
		Graph g2; 
		g = Term();
		boolean first = true; 
		while (WeakSeparator(39,24,25) ) {
			g2 = Term();
			if (first) { tab.MakeFirstAlt(g); first = false; }
			tab.MakeAlternative(g, g2);
			
		}
		return g;
	}

	CharSet  SimSet() {
		CharSet  s;
		int n1, n2; 
		s = new CharSet(); 
		if (la.kind == 1) {
			Get();
			CharClass c = tab.FindCharClass(t.val);
			if (c == null) SemErr("undefined name"); else s.Or(c.set);
			
		} else if (la.kind == 3) {
			Get();
			String name = t.val;
			name = tab.Unescape(name.substring(1, name.length()-1));
			for (int i = 0; i < name.length(); i++)
			  if (dfa.ignoreCase) s.Set(Character.toLowerCase(name.charAt(i)));
			  else s.Set(name.charAt(i)); 
		} else if (la.kind == 5) {
			n1 = Char();
			s.Set(n1); 
			if (la.kind == 28) {
				Get();
				n2 = Char();
				for (int i = n1; i <= n2; i++) s.Set(i); 
			}
		} else if (la.kind == 29) {
			Get();
			s = new CharSet(); s.Fill(); 
		} else SynErr(59);
		return s;
	}

	int  Char() {
		int  n;
		Expect(5);
		String name = t.val; n = 0;
		name = tab.Unescape(name.substring(1, name.length()-1));
		if (name.length() == 1) n = name.charAt(0);
		else SemErr("unacceptable character value");
		if (dfa.ignoreCase && (char)n >= 'A' && (char)n <= 'Z') n += 32;
		
		return n;
	}

	SymInfo  Sym() {
		SymInfo  s;
		s = new SymInfo(); s.name = "???"; s.kind = isIdent; 
		if (la.kind == 1) {
			Get();
			s.kind = isIdent; s.name = t.val; 
		} else if (la.kind == 3 || la.kind == 5) {
			if (la.kind == 3) {
				Get();
				s.name = t.val; 
			} else {
				Get();
				s.name = "\"" + t.val.substring(1, t.val.length()-1) + "\""; 
			}
			s.kind = isLiteral;
			if (dfa.ignoreCase) s.name = s.name.toLowerCase();
			if (s.name.indexOf(' ') >= 0)
			  SemErr("literal tokens must not contain blanks"); 
		} else SynErr(60);
		return s;
	}

	void TypeName() {
		Expect(1);
		while (la.kind == 24 || la.kind == 30 || la.kind == 37) {
			if (la.kind == 24) {
				Get();
				Expect(1);
			} else if (la.kind == 37) {
				Get();
				Expect(38);
			} else {
				Get();
				TypeName();
				while (la.kind == 34) {
					Get();
					TypeName();
				}
				Expect(33);
			}
		}
	}

	Graph  Term() {
		Graph  g;
		Graph g2; Node rslv = null; g = null; 
		if (StartOf(26)) {
			if (la.kind == 46) {
				rslv = tab.NewNode(Node.rslv, null, la.line); 
				rslv.pos = Resolver();
				g = new Graph(rslv);                       
			}
			g2 = Factor();
			if (rslv != null) tab.MakeSequence(g, g2);
			else g = g2;
			
			while (StartOf(27)) {
				g2 = Factor();
				tab.MakeSequence(g, g2); 
			}
		} else if (StartOf(28)) {
			g = new Graph(tab.NewNode(Node.eps, null, 0)); 
		} else SynErr(61);
		if (g == null) // invalid start of Term
		 g = new Graph(tab.NewNode(Node.eps, null, 0));
		
		return g;
	}

	Position  Resolver() {
		Position  pos;
		Expect(46);
		Expect(41);
		int beg = la.pos; int col = la.col; 
		Condition();
		pos = new Position(beg, t.pos, col); 
		return pos;
	}

	Graph  Factor() {
		Graph  g;
		SymInfo s; Position pos; boolean weak = false;
		g = null; 
		switch (la.kind) {
		case 1: case 3: case 5: case 40: {
			if (la.kind == 40) {
				Get();
				weak = true; 
			}
			s = Sym();
			Symbol sym = tab.FindSym(s.name);
			if (sym == null && s.kind == isLiteral)
			  sym = (Symbol)tab.literals.get(s.name);
			boolean undef = (sym == null);
			if (undef) {
			  if (s.kind == isIdent)
			    sym = tab.NewSym(Node.nt, s.name, 0);  // forward nt
			  else if (genScanner) {
			    sym = tab.NewSym(Node.t, s.name, t.line);
			    dfa.MatchLiteral(sym.name, sym);
			  } else {  // undefined string in production
			    SemErr("undefined string in production");
			    sym = tab.eofSy;  // dummy
			  }
			}
			int typ = sym.typ;
			if (typ != Node.t && typ != Node.nt)
			  SemErr("this symbol kind is not allowed in a production");
			if (weak)
			  if (typ == Node.t) typ = Node.wt;
			  else SemErr("only terminals may be weak");
			Node p = tab.NewNode(typ, sym, t.line);
			g = new Graph(p);
			
			if (la.kind == 30 || la.kind == 35) {
				Attribs(p);
				if (s.kind == isLiteral) SemErr("a literal must not have attributes"); 
			}
			if (undef) {
			 sym.attrPos = p.pos;  // dummy
			 sym.retVar = p.retVar;  // AH - dummy
			} else if ((p.pos == null) != (sym.attrPos == null)
			       || (p.retVar == null) != (sym.retVar == null))
			  SemErr("attribute mismatch between declaration and use of this symbol");
			
			break;
		}
		case 41: {
			Get();
			g = Expression();
			Expect(42);
			break;
		}
		case 37: {
			Get();
			g = Expression();
			Expect(38);
			tab.MakeOption(g); 
			break;
		}
		case 43: {
			Get();
			g = Expression();
			Expect(44);
			tab.MakeIteration(g); 
			break;
		}
		case 48: {
			pos = SemText();
			Node p = tab.NewNode(Node.sem, null, 0);
			p.pos = pos;
			g = new Graph(p);
			
			break;
		}
		case 29: {
			Get();
			Node p = tab.NewNode(Node.any, null, 0);  // p.set is set in tab.SetupAnys
			g = new Graph(p);
			
			break;
		}
		case 45: {
			Get();
			Node p = tab.NewNode(Node.sync, null, 0);
			g = new Graph(p);
			
			break;
		}
		default: SynErr(62); break;
		}
		if (g == null) // invalid start of Factor
		 g = new Graph(tab.NewNode(Node.eps, null, 0));
		
		return g;
	}

	void Attribs(Node n) {
		int beg, col; 
		if (la.kind == 30) {
			Get();
			if (la.kind == 31 || la.kind == 32) {
				if (la.kind == 31) {
					Get();
				} else {
					Get();
				}
				beg = la.pos; 
				while (StartOf(29)) {
					if (StartOf(30)) {
						Get();
					} else if (la.kind == 37 || la.kind == 41) {
						Bracketed();
					} else {
						Get();
						SemErr("bad string in attributes"); 
					}
				}
				n.retVar = scanner.buffer.GetString(beg, la.pos); 
				if (la.kind == 33) {
					Get();
				} else if (la.kind == 34) {
					Get();
					beg = la.pos; col = la.col; 
					while (StartOf(13)) {
						if (StartOf(14)) {
							Get();
						} else {
							Get();
							SemErr("bad string in attributes"); 
						}
					}
					Expect(33);
					if (t.pos > beg) n.pos = new Position(beg, t.pos, col); 
				} else SynErr(63);
			} else if (StartOf(15)) {
				beg = la.pos; col = la.col; 
				while (StartOf(16)) {
					if (StartOf(17)) {
						Get();
					} else {
						Get();
						SemErr("bad string in attributes"); 
					}
				}
				Expect(33);
				if (t.pos > beg) n.pos = new Position(beg, t.pos, col); 
			} else SynErr(64);
		} else if (la.kind == 35) {
			Get();
			if (la.kind == 31 || la.kind == 32) {
				if (la.kind == 31) {
					Get();
				} else {
					Get();
				}
				beg = la.pos; 
				while (StartOf(31)) {
					if (StartOf(32)) {
						Get();
					} else if (la.kind == 37 || la.kind == 41) {
						Bracketed();
					} else {
						Get();
						SemErr("bad string in attributes"); 
					}
				}
				n.retVar = scanner.buffer.GetString(beg, la.pos); 
				if (la.kind == 36) {
					Get();
				} else if (la.kind == 34) {
					Get();
					beg = la.pos; col = la.col; 
					while (StartOf(18)) {
						if (StartOf(19)) {
							Get();
						} else {
							Get();
							SemErr("bad string in attributes"); 
						}
					}
					Expect(36);
					if (t.pos > beg) n.pos = new Position(beg, t.pos, col); 
				} else SynErr(65);
			} else if (StartOf(15)) {
				beg = la.pos; col = la.col; 
				while (StartOf(20)) {
					if (StartOf(21)) {
						Get();
					} else {
						Get();
						SemErr("bad string in attributes"); 
					}
				}
				Expect(36);
				if (t.pos > beg) n.pos = new Position(beg, t.pos, col); 
			} else SynErr(66);
		} else SynErr(67);
	}

	void Condition() {
		while (StartOf(33)) {
			if (la.kind == 41) {
				Get();
				Condition();
			} else {
				Get();
			}
		}
		Expect(42);
	}

	Graph  TokenTerm() {
		Graph  g;
		Graph g2; 
		g = TokenFactor();
		while (StartOf(11)) {
			g2 = TokenFactor();
			tab.MakeSequence(g, g2); 
		}
		if (la.kind == 47) {
			Get();
			Expect(41);
			g2 = TokenExpr();
			tab.SetContextTrans(g2.l); dfa.hasCtxMoves = true;
			tab.MakeSequence(g, g2); 
			Expect(42);
		}
		return g;
	}

	Graph  TokenFactor() {
		Graph  g;
		SymInfo s; g = null; 
		if (la.kind == 1 || la.kind == 3 || la.kind == 5) {
			s = Sym();
			if (s.kind == isIdent) {
			 CharClass c = tab.FindCharClass(s.name);
			 if (c == null) {
			   SemErr("undefined name");
			   c = tab.NewCharClass(s.name, new CharSet());
			 }
			 Node p = tab.NewNode(Node.clas, null, 0); p.val = c.n;
			 g = new Graph(p);
			 tokenString = noString;
			} else { // str
			  g = tab.StrToGraph(s.name);
			  if (tokenString == null) tokenString = s.name;
			  else tokenString = noString;
			}
			
		} else if (la.kind == 41) {
			Get();
			g = TokenExpr();
			Expect(42);
		} else if (la.kind == 37) {
			Get();
			g = TokenExpr();
			Expect(38);
			tab.MakeOption(g); 
		} else if (la.kind == 43) {
			Get();
			g = TokenExpr();
			Expect(44);
			tab.MakeIteration(g); 
		} else SynErr(68);
		if (g == null) // invalid start of TokenFactor
		 g = new Graph(tab.NewNode(Node.eps, null, 0)); 
		return g;
	}

	void Bracketed() {
		if (la.kind == 41) {
			Get();
			while (StartOf(33)) {
				if (la.kind == 37 || la.kind == 41) {
					Bracketed();
				} else {
					Get();
				}
			}
			Expect(42);
		} else if (la.kind == 37) {
			Get();
			while (StartOf(34)) {
				if (la.kind == 37 || la.kind == 41) {
					Bracketed();
				} else {
					Get();
				}
			}
			Expect(38);
		} else SynErr(69);
	}



	public void Parse() {
		la = new Token();
		la.val = "";
		Get();
		Coco();
		Expect(0); // expect end-of-file automatically added

	}

	private static final boolean[][] set = {
		{T,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, T,T,x,x, x,T,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x},
		{x,T,T,T, T,T,T,x, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,x,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,x,T,x, T,x,x,x, x,x,T,T, T,x,x,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,x,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{T,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, T,T,x,x, x,T,T,T, T,x,x,x, x,T,x,x, x,x,x,x, x,T,x,T, T,T,x,T, x,T,T,x, T,x,x,x},
		{T,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, T,T,x,x, x,T,T,T, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x},
		{T,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, T,T,x,x, x,T,T,T, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x},
		{x,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, T,T,x,x, x,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x},
		{x,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,T,x,T, x,x,x,x, x,x,x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,T, T,T,T,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,T,x, x,x,T,x, T,x,x,x, x,x,x,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,x,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,x,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,x,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,x,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x, x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,x,T,x},
		{x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, x,x,T,x},
		{x,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x, x,T,x,x, x,x,x,x, x,T,T,T, T,T,T,T, T,T,T,x, T,x,x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,T,x, x,x,T,x, T,x,x,x, x,x,x,x},
		{x,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,T,x,x, T,T,x,T, x,T,T,x, T,x,x,x},
		{x,T,x,T, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x,x,x, x,T,x,x, T,T,x,T, x,T,x,x, T,x,x,x},
		{x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,T,T, x,x,T,x, T,x,x,x, x,x,x,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,x,x,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,x,x,T, T,x,T,T, T,x,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,x,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,x,T, x,x,T,T, T,x,T,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,x,T, T,T,T,T, T,T,T,x},
		{x,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,T,T, T,T,x,T, T,T,T,T, T,T,T,T, T,T,T,x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text

	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}

	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "ident expected"; break;
			case 2: s = "number expected"; break;
			case 3: s = "string expected"; break;
			case 4: s = "badString expected"; break;
			case 5: s = "char expected"; break;
			case 6: s = "\"[copy]\" expected"; break;
			case 7: s = "\"[/copy]\" expected"; break;
			case 8: s = "\"COMPILER\" expected"; break;
			case 9: s = "\"[initialize]\" expected"; break;
			case 10: s = "\"[/initialize]\" expected"; break;
			case 11: s = "\"[destroy]\" expected"; break;
			case 12: s = "\"[/destroy]\" expected"; break;
			case 13: s = "\"IGNORECASE\" expected"; break;
			case 14: s = "\"CHARACTERS\" expected"; break;
			case 15: s = "\"TOKENS\" expected"; break;
			case 16: s = "\"PRAGMAS\" expected"; break;
			case 17: s = "\"COMMENTS\" expected"; break;
			case 18: s = "\"FROM\" expected"; break;
			case 19: s = "\"TO\" expected"; break;
			case 20: s = "\"NESTED\" expected"; break;
			case 21: s = "\"IGNORE\" expected"; break;
			case 22: s = "\"PRODUCTIONS\" expected"; break;
			case 23: s = "\"=\" expected"; break;
			case 24: s = "\".\" expected"; break;
			case 25: s = "\"END\" expected"; break;
			case 26: s = "\"+\" expected"; break;
			case 27: s = "\"-\" expected"; break;
			case 28: s = "\"..\" expected"; break;
			case 29: s = "\"ANY\" expected"; break;
			case 30: s = "\"<\" expected"; break;
			case 31: s = "\"^\" expected"; break;
			case 32: s = "\"out\" expected"; break;
			case 33: s = "\">\" expected"; break;
			case 34: s = "\",\" expected"; break;
			case 35: s = "\"<.\" expected"; break;
			case 36: s = "\".>\" expected"; break;
			case 37: s = "\"[\" expected"; break;
			case 38: s = "\"]\" expected"; break;
			case 39: s = "\"|\" expected"; break;
			case 40: s = "\"WEAK\" expected"; break;
			case 41: s = "\"(\" expected"; break;
			case 42: s = "\")\" expected"; break;
			case 43: s = "\"{\" expected"; break;
			case 44: s = "\"}\" expected"; break;
			case 45: s = "\"SYNC\" expected"; break;
			case 46: s = "\"IF\" expected"; break;
			case 47: s = "\"CONTEXT\" expected"; break;
			case 48: s = "\"(.\" expected"; break;
			case 49: s = "\".)\" expected"; break;
			case 50: s = "??? expected"; break;
			case 51: s = "this symbol not expected in Coco"; break;
			case 52: s = "this symbol not expected in TokenDecl"; break;
			case 53: s = "invalid TokenDecl"; break;
			case 54: s = "invalid AttrDecl"; break;
			case 55: s = "invalid AttrDecl"; break;
			case 56: s = "invalid AttrDecl"; break;
			case 57: s = "invalid AttrDecl"; break;
			case 58: s = "invalid AttrDecl"; break;
			case 59: s = "invalid SimSet"; break;
			case 60: s = "invalid Sym"; break;
			case 61: s = "invalid Term"; break;
			case 62: s = "invalid Factor"; break;
			case 63: s = "invalid Attribs"; break;
			case 64: s = "invalid Attribs"; break;
			case 65: s = "invalid Attribs"; break;
			case 66: s = "invalid Attribs"; break;
			case 67: s = "invalid Attribs"; break;
			case 68: s = "invalid TokenFactor"; break;
			case 69: s = "invalid Bracketed"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}

	public void Warning (int line, int col, String s) {
		printMsg(line, col, s);
	}

	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}


// ************************************************************************* //
