/*-------------------------------------------------------------------------
DFA.java -- Generation of the Scanner Automaton
Compiler Generator Coco/R,
Copyright (c) 1990, 2004 Hanspeter Moessenboeck, University of Linz
extended by M. Loeberbauer & A. Woess, Univ. of Linz
ported from C# to Java by Wolfgang Ahorner
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
------------------------------------------------------------------------*/

package Coco;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;                /* pdt */
import java.io.BufferedReader;        /* pdt */
import java.io.FileReader;            /* pdt */
import java.io.PrintWriter;           /* pdt */
import java.io.BufferedWriter;        /* pdt */
import java.io.FileWriter;            /* pdt */
import java.util.BitSet;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

//-----------------------------------------------------------------------------
//  State
//-----------------------------------------------------------------------------

//! state of finite automaton
class State {
  public int nr;            //!< state number
  public Action firstAction;//!< to first action of this state
  public Symbol endOf;      //!< recognized token if state is final
  public boolean ctx;       //!< true if state is reached via contextTrans
  public State next;

  public void AddAction(Action act) {
    Action lasta = null, a = firstAction;
    while (a != null && act.typ >= a.typ) {lasta = a; a = a.next;}
    // collecting classes at the beginning gives better performance
    act.next = a;
    if (a==firstAction) firstAction = act; else lasta.next = act;
  }

  public void DetachAction(Action act) {
    Action lasta = null, a = firstAction;
    while (a != null && a != act) {lasta = a; a = a.next;}
    if (a != null)
      if (a == firstAction) firstAction = a.next; else lasta.next = a.next;
  }

  public void MeltWith(State s) { // copy actions of s to state
    for (Action action = s.firstAction; action != null; action = action.next) {
      Action a = new Action(action.typ, action.sym, action.tc);
      a.AddTargets(action);
      AddAction(a);
    }
  }

}

//-----------------------------------------------------------------------------
//  Action
//-----------------------------------------------------------------------------

//! action of finite automaton
class Action {
  public int typ;               //!< type of action symbol: clas, chr
  public int sym;               //!< action symbol
  public int tc;                //!< transition code: normalTrans, contextTrans
  public Target target;         //!< states reached from this action
  public Action next;

  public Action(int typ, int sym, int tc) {
    this.typ = typ; this.sym = sym; this.tc = tc;
  }

  public void AddTarget(Target t) { // add t to the action.targets
    Target last = null;
    Target p = target;
    while (p != null && t.state.nr >= p.state.nr) {
      if (t.state == p.state) return;
      last = p; p = p.next;
    }
    t.next = p;
    if (p == target) target = t; else last.next = t;
  }

  public void AddTargets(Action a) { // add copy of a.targets to action.targets
    for (Target p = a.target; p != null; p = p.next) {
      Target t = new Target(p.state);
      AddTarget(t);
    }
    if (a.tc == Node.contextTrans) tc = Node.contextTrans;
  }

  public CharSet Symbols(Tab tab) {
    CharSet s;
    if (typ == Node.clas)
      s = tab.CharClassSet(sym).Clone();
    else {
      s = new CharSet(); s.Set(sym);
    }
    return s;
  }

  public void ShiftWith(CharSet s, Tab tab) {
    if (s.Elements() == 1) {
      typ = Node.chr; sym = s.First();
    } else {
      CharClass c = tab.FindCharClass(s);
      if (c == null) c = tab.NewCharClass("#", s); // class with dummy name
      typ = Node.clas; sym = c.n;
    }
  }

}

//-----------------------------------------------------------------------------
//  Target
//-----------------------------------------------------------------------------

//! set of states that are reached by an action
class Target {
  public State state;   //!< target state
  public Target next;

  public Target (State s) {
    state = s;
  }
}

//-----------------------------------------------------------------------------
//  Melted
//-----------------------------------------------------------------------------

//! info about melted states
class Melted {
  public BitSet set;          //!< set of old states
  public State state;         //!< new state
  public Melted next;

  public Melted(BitSet set, State state) {
    this.set = set; this.state = state;
  }

}

//-----------------------------------------------------------------------------
//  Comment
//-----------------------------------------------------------------------------

//! info about comment syntax
class Comment {
  public String start;
  public String stop;
  public boolean nested;
  public Comment next;

  public Comment(String start, String stop, boolean nested) {
    this.start = start; this.stop = stop; this.nested = nested;
  }

}

//-----------------------------------------------------------------------------
//  CharSet
//-----------------------------------------------------------------------------

class CharSet {

	public static class Range {
		int from, to;
		Range next;
		Range(int from, int to) { this.from = from; this.to = to; }
	}

	public Range head;

	public boolean Get(int i) {
		for (Range p = head; p != null; p = p.next)
			if (i < p.from) return false;
			else if (i <= p.to) return true; // p.from <= i <= p.to
		return false;
	}

	public void Set(int i) {
		Range cur = head, prev = null;
		while (cur != null && i >= cur.from-1) {
			if (i <= cur.to + 1) { // (cur.from-1) <= i <= (cur.to+1)
				if (i == cur.from - 1) cur.from--;
				else if (i == cur.to + 1) {
					cur.to++;
					Range next = cur.next;
					if (next != null && cur.to == next.from - 1) { cur.to = next.to; cur.next = next.next; };
				}
				return;
			}
			prev = cur; cur = cur.next;
		}
		Range n = new Range(i, i);
		n.next = cur;
		if (prev == null) head = n; else prev.next = n;
	}

	public CharSet Clone() {
		CharSet s = new CharSet();
		Range prev = null;
		for (Range cur = head; cur != null; cur = cur.next) {
			Range r = new Range(cur.from, cur.to);
			if (prev == null) s.head = r; else prev.next = r;
			prev = r;
		}
		return s;
	}

	public boolean Equals(CharSet s) {
		Range p = head, q = s.head;
		while (p != null && q != null) {
			if (p.from != q.from || p.to != q.to) return false;
			p = p.next; q = q.next;
		}
		return p == q;
	}

	public int Elements() {
		int n = 0;
		for (Range p = head; p != null; p = p.next) n += p.to - p.from + 1;
		return n;
	}

	public int First() {
		if (head != null) return head.from;
		return -1;
	}

	public void Or(CharSet s) {
		for (Range p = s.head; p != null; p = p.next)
			for (int i = p.from; i <= p.to; i++) Set(i);
	}

	public void And(CharSet s) {
		CharSet x = new CharSet();
		for (Range p = head; p != null; p = p.next)
			for (int i = p.from; i <= p.to; i++)
				if (s.Get(i)) x.Set(i);
		head = x.head;
	}

	public void Subtract(CharSet s) {
		CharSet x = new CharSet();
		for (Range p = head; p != null; p = p.next)
			for (int i = p.from; i <= p.to; i++)
				if (!s.Get(i)) x.Set(i);
		head = x.head;
	}

	public boolean Includes(CharSet s) {
		for (Range p = s.head; p != null; p = p.next)
			for (int i = p.from; i <= p.to; i++)
				if (!Get(i)) return false;
		return true;
	}

	public boolean Intersects(CharSet s) {
		for (Range p = s.head; p != null; p = p.next)
			for (int i = p.from; i <= p.to; i++)
				if (Get(i)) return true;
		return false;
	}

	public void Fill() {
		head = new Range(Character.MIN_VALUE, Character.MAX_VALUE);
	}
}

//-----------------------------------------------------------------------------
//  DFA
//-----------------------------------------------------------------------------

public class DFA {
  static final int EOF = -1;

  public int maxStates;
  public int lastStateNr;      //!< highest state number
  public State firstState;
  public State lastState;      //!< last allocated state
  public int lastSimState;     //!< last non melted state
  public Reader fram;          //!< scanner frame input     /* pdt */
  public PrintWriter gen;      //!< generated scanner file  /* pdt */
  public Symbol curSy;         //!< current token to be recognized (in FindTrans)
  public boolean ignoreCase;   //!< true if input should be treated case-insensitively
  public boolean dirtyDFA;     //!< DFA may become nondeterministic in MatchLiteral
  public boolean hasCtxMoves;  //!< DFA has context transitions

  Parser parser;               // other Coco objects
  Tab tab;
  Errors errors;

  private int framRead() {
    try {
      return fram.read();
    } catch (java.io.IOException e) {
      throw new FatalError("Error reading Scanner.frame");
    }
  }

  //---------- Output primitives
  private String Ch(char ch) {
    if (ch < ' ' || ch >= 127 || ch == '\'' || ch == '\\') {
      return Integer.toString((int)ch);
    }
    else return "'" + ch + "'";
  }

  private String ChCond(char ch) {
    return ("ch == " + Ch(ch));
  }

  private void PutRange(CharSet s) {
    for (CharSet.Range r = s.head; r != null; r = r.next) {
      if (r.from == r.to) { gen.print("ch == " + Ch((char) r.from)); }
      else if (r.from == 0) { gen.print("ch <= " + Ch((char) r.to)); }
      else { gen.print("ch >= " + Ch((char) r.from) + " && ch <= " + Ch((char) r.to)); }
      if (r.next != null) gen.print(" || ");
    }
  }

  //---------- State handling

  State NewState() {
    State s = new State(); s.nr = ++lastStateNr;
    if (firstState == null) firstState = s; else lastState.next = s;
    lastState = s;
    return s;
  }

  void NewTransition(State from, State to, int typ, int sym, int tc) {
    Target t = new Target(to);
    Action a = new Action(typ, sym, tc); a.target = t;
    from.AddAction(a);
    if (typ == Node.clas) curSy.tokenKind = Symbol.classToken;
  }

  void CombineShifts() {
    State state;
    Action a, b, c;
    CharSet seta, setb;
    for (state = firstState; state != null; state = state.next) {
      for (a = state.firstAction; a != null; a = a.next) {
        b = a.next;
        while (b != null)
          if (a.target.state == b.target.state && a.tc == b.tc) {
            seta = a.Symbols(tab); setb = b.Symbols(tab);
            seta.Or(setb);
            a.ShiftWith(seta, tab);
            c = b; b = b.next; state.DetachAction(c);
          } else b = b.next;
      }
    }
  }

  void FindUsedStates(State state, BitSet used) {
    if (used.get(state.nr)) return;
    used.set(state.nr);
    for (Action a = state.firstAction; a != null; a = a.next)
      FindUsedStates(a.target.state, used);
  }

  void DeleteRedundantStates() {
    State[] newState = new State[lastStateNr + 1];
    BitSet used = new BitSet(lastStateNr + 1);
    FindUsedStates(firstState, used);
    // combine equal final states
    for (State s1 = firstState.next; s1 != null; s1 = s1.next) // firstState cannot be final
      if (used.get(s1.nr) && s1.endOf != null && s1.firstAction == null && !s1.ctx)
        for (State s2 = s1.next; s2 != null; s2 = s2.next)
          if (used.get(s2.nr) && s1.endOf == s2.endOf && s2.firstAction == null & !s2.ctx) {
            used.set(s2.nr, false); newState[s2.nr] = s1;
          }
    for (State state = firstState; state != null; state = state.next)
      if (used.get(state.nr))
        for (Action a = state.firstAction; a != null; a = a.next)
          if (!used.get(a.target.state.nr))
            a.target.state = newState[a.target.state.nr];
    // delete unused states
    lastState = firstState; lastStateNr = 0; // firstState has number 0
    for (State state = firstState.next; state != null; state = state.next)
      if (used.get(state.nr)) {state.nr = ++lastStateNr; lastState = state;}
      else lastState.next = state.next;
  }

  State TheState(Node p) {
    State state;
    if (p == null) {state = NewState(); state.endOf = curSy; return state;}
    else return p.state;
  }

  void Step(State from, Node p, BitSet stepped) {
    if (p == null) return;
    stepped.set(p.n);
    switch (p.typ) {
      case Node.clas: case Node.chr: {
        NewTransition(from, TheState(p.next), p.typ, p.val, p.code);
        break;
      }
      case Node.alt: {
        Step(from, p.sub, stepped); Step(from, p.down, stepped);
        break;
      }
      case Node.iter: case Node.opt: {
        if (p.next != null && !stepped.get(p.next.n)) Step(from, p.next, stepped);
        Step(from, p.sub, stepped);
        if (p.typ == Node.iter && p.state != from) {
          Step(p.state, p, new BitSet(tab.nodes.size()));
        }
        break;
      }
    }
  }


  // Assigns a state n.state to every node n. There will be a transition from
  // n.state to n.next.state triggered by n.val. All nodes in an alternative
  // chain are represented by the same state.
  // Numbering scheme:
  //  - any node after a chr, clas, opt, or alt, must get a new number
  //  - if a nested structure starts with an iteration the iter node must get a new number
  //  - if an iteration follows an iteration, it must get a new number
  void NumberNodes(Node p, State state, boolean renumIter) {
    if (p == null) return;
    if (p.state != null) return; // already visited;
    if (state == null || (p.typ == Node.iter && renumIter)) state = NewState();
    p.state = state;
    if (tab.DelGraph(p)) state.endOf = curSy;
    switch (p.typ) {
      case Node.clas: case Node.chr: {
        NumberNodes(p.next, null, false);
        break;
      }
      case Node.opt: {
        NumberNodes(p.next, null, false);
        NumberNodes(p.sub, state, true);
        break;
      }
      case Node.iter: {
        NumberNodes(p.next, state, true);
        NumberNodes(p.sub, state, true);
        break;
      }
      case Node.alt: {
        NumberNodes(p.next, null, false);
        NumberNodes(p.sub, state, true);
        NumberNodes(p.down, state, renumIter);
        break;
      }
    }
  }

  void FindTrans (Node p, boolean start, BitSet marked) {
    if (p == null || marked.get(p.n)) return;
    marked.set(p.n);
    if (start) Step(p.state, p, new BitSet(tab.nodes.size())); // start of group of equally numbered nodes
    switch (p.typ) {
      case Node.clas: case Node.chr: {
        FindTrans(p.next, true, marked);
        break;
      }
      case Node.opt: {
        FindTrans(p.next, true, marked); FindTrans(p.sub, false, marked);
        break;
      }
      case Node.iter: {
        FindTrans(p.next, false, marked); FindTrans(p.sub, false, marked);
        break;
      }
      case Node.alt: {
        FindTrans(p.sub, false, marked); FindTrans(p.down, false, marked);
        break;
      }
    }
  }

  public void ConvertToStates(Node p, Symbol sym) {
    curSy = sym;
    if (tab.DelGraph(p)) parser.SemErr("token might be empty");
    NumberNodes(p, firstState, true);
    FindTrans(p, true, new BitSet(tab.nodes.size()));
    if (p.typ == Node.iter) {
      Step(firstState, p, new BitSet(tab.nodes.size()));
    }
  }

  // match string against current automaton; store it either as a fixedToken or as a litToken
  public void MatchLiteral(String s, Symbol sym) {
    s = tab.Unescape(s.substring(1, s.length()-1));
    int i, len = s.length();
    State state = firstState;
    Action a = null;
    for (i = 0; i < len; i++) { // try to match s against existing DFA
      a = FindAction(state, s.charAt(i));
      if (a == null) break;
      state = a.target.state;
    }
    // if s was not totally consumed or leads to a non-final state => make new DFA from it
    if (i != len || state.endOf == null) {
      state = firstState; i = 0; a = null;
      dirtyDFA = true;
    }
    for (; i < len; i++) { // make new DFA for s[i..len-1]
      State to = NewState();
      NewTransition(state, to, Node.chr, s.charAt(i), Node.normalTrans);
      state = to;
    }
    Symbol matchedSym = state.endOf;
    if (state.endOf == null) {
      state.endOf = sym;
    } else if (matchedSym.tokenKind == Symbol.fixedToken || (a != null && a.tc == Node.contextTrans)) {
      // s matched a token with a fixed definition or a token with an appendix that will be cut off
      parser.SemErr("tokens " + sym.name + " and " + matchedSym.name + " cannot be distinguished");
    } else { // matchedSym == classToken || classLitToken
      matchedSym.tokenKind = Symbol.classLitToken;
      sym.tokenKind = Symbol.litToken;
    }
  }

  void SplitActions(State state, Action a, Action b) {
    Action c; CharSet seta, setb, setc;
    seta = a.Symbols(tab); setb = b.Symbols(tab);
    if (seta.Equals(setb)) {
      a.AddTargets(b);
      state.DetachAction(b);
    } else if (seta.Includes(setb)) {
      setc = seta.Clone(); setc.Subtract(setb);
      b.AddTargets(a);
      a.ShiftWith(setc, tab);
    } else if (setb.Includes(seta)) {
      setc = setb.Clone(); setc.Subtract(seta);
      a.AddTargets(b);
      b.ShiftWith(setc, tab);
    } else {
      setc = seta.Clone(); setc.And(setb);
      seta.Subtract(setc);
      setb.Subtract(setc);
      a.ShiftWith(seta, tab);
      b.ShiftWith(setb, tab);
      c = new Action(0, 0, Node.normalTrans);  // typ and sym are set in ShiftWith
      c.AddTargets(a);
      c.AddTargets(b);
      c.ShiftWith(setc, tab);
      state.AddAction(c);
    }
  }

  private boolean Overlap(Action a, Action b) {
    CharSet seta, setb;
    if (a.typ == Node.chr)
      if (b.typ == Node.chr) return a.sym == b.sym;
      else {setb = tab.CharClassSet(b.sym); return setb.Get(a.sym);}
    else {
      seta = tab.CharClassSet(a.sym);
      if (b.typ ==Node.chr) return seta.Get(b.sym);
      else {setb = tab.CharClassSet(b.sym); return seta.Intersects(setb);}
    }
  }

  void MakeUnique(State state) {
    boolean changed;
    do {
      changed = false;
      for (Action a = state.firstAction; a != null; a = a.next)
        for (Action b = a.next; b != null; b = b.next)
          if (Overlap(a, b)) { SplitActions(state, a, b); changed = true; }
    } while (changed);
  }

  void MeltStates(State state) {
    boolean ctx;
    BitSet targets;
    Symbol endOf;
    for (Action action = state.firstAction; action != null; action = action.next) {
      if (action.target.next != null) {
        //action.GetTargetStates(out targets, out endOf, out ctx);
        Object[] param = new Object[2];
        ctx = GetTargetStates(action, param);
        targets = (BitSet)param[0];
        endOf = (Symbol)param[1];
        //
        Melted melt = StateWithSet(targets);
        if (melt == null) {
          State s = NewState(); s.endOf = endOf; s.ctx = ctx;
          for (Target targ = action.target; targ != null; targ = targ.next)
            s.MeltWith(targ.state);
          MakeUnique(s);
          melt = NewMelted(targets, s);
        }
        action.target.next = null;
        action.target.state = melt.state;
      }
    }
  }

  void FindCtxStates() {
    for (State state = firstState; state != null; state = state.next)
      for (Action a = state.firstAction; a != null; a = a.next)
        if (a.tc == Node.contextTrans) a.target.state.ctx = true;
  }

  public void MakeDeterministic() {
    State state;
    lastSimState = lastState.nr;
    maxStates = 2 * lastSimState; // heuristic for set size in Melted.set
    FindCtxStates();
    for (state = firstState; state != null; state = state.next)
      MakeUnique(state);
    for (state = firstState; state != null; state = state.next)
      MeltStates(state);
    DeleteRedundantStates();
    CombineShifts();
  }

  public void PrintStates() {
    Trace trace = tab.trace;

    trace.WriteLine();
    trace.WriteLine("---------- states ----------");
    for (State state = firstState; state != null; state = state.next) {
      boolean first = true;
      if (state.endOf == null) trace.Write("               ");
      else trace.Write("E(" + tab.Name(state.endOf.name) + ")", 12);
      trace.Write(state.nr + ":", 3);
      if (state.firstAction == null) trace.WriteLine();
      for (Action action = state.firstAction; action != null; action = action.next) {
        if (first) {trace.Write(" "); first = false;} else trace.Write("                   ");
        if (action.typ == Node.clas)
          trace.Write(((CharClass)tab.classes.get(action.sym)).name);
        else trace.Write(Ch((char)action.sym), 3);
        for (Target targ = action.target; targ != null; targ = targ.next)
          trace.Write(Integer.toString(targ.state.nr), 3);
        if (action.tc == Node.contextTrans) trace.WriteLine(" context"); else trace.WriteLine();
      }
    }
    trace.WriteLine();
    trace.WriteLine("---------- character classes ----------");
    tab.WriteCharClasses();
  }

  //------------------------ actions ------------------------------

  public Action FindAction(State state, char ch) {
    for (Action a = state.firstAction; a != null; a = a.next)
      if (a.typ == Node.chr && ch == a.sym) return a;
      else if (a.typ == Node.clas) {
        CharSet s = tab.CharClassSet(a.sym);
        if (s.Get(ch)) return a;
      }
    return null;
  }

  //public void GetTargetStates(out BitArray targets, out Symbol endOf, out bool ctx) {
  public boolean GetTargetStates(Action a, Object[] param) {
    // compute the set of target states
    BitSet targets = new BitSet(maxStates);
    Symbol endOf = null;
    boolean ctx = false;
    for (Target t = a.target; t != null; t = t.next) {
      int stateNr = t.state.nr;
      if (stateNr <= lastSimState) targets.set(stateNr);
      else targets.or(MeltedSet(stateNr));
      if (t.state.endOf != null)
        if (endOf == null || endOf == t.state.endOf)
          endOf = t.state.endOf;
        else {
          errors.SemErr("Tokens " + endOf.name + " and " + t.state.endOf.name +
            " cannot be distinguished");
        }
      if (t.state.ctx) {
        ctx = true;
        // The following check seems to be unnecessary. It reported an error
        // if a symbol + context was the prefix of another symbol, e.g.
        //   s1 = "a" "b" "c".
        //   s2 = "a" CONTEXT("b").
        // But this is ok.
        // if (t.state.endOf != null) {
        //   Console.WriteLine("Ambiguous context clause");
        //   Errors.count++;
        // }
      }
    }
    param[0] = targets;
    param[1] = endOf;
    return ctx;
  }

  //---------------------- melted states --------------------------

  Melted firstMelted;  // head of melted state list

  Melted NewMelted(BitSet set, State state) {
    Melted m = new Melted(set, state);
    m.next = firstMelted; firstMelted = m;
    return m;
  }

  BitSet MeltedSet(int nr) {
    Melted m = firstMelted;
    while (m != null) {
      if (m.state.nr == nr) return m.set; else m = m.next;
    }
    throw new FatalError("Compiler error in Melted.Set");
  }

  Melted StateWithSet(BitSet s) {
    for (Melted m = firstMelted; m != null; m = m.next)
      if (Sets.Equals(s, m.set)) return m;
    return null;
  }

  //------------------------- comments ----------------------------

  public Comment firstComment;  // list of comments

  String CommentStr(Node p) {
    StringBuffer s = new StringBuffer();
    while (p != null) {
      if (p.typ == Node.chr) {
        s.append((char)p.val);
      } else if (p.typ == Node.clas) {
        CharSet set = tab.CharClassSet(p.val);
        if (set.Elements() != 1) parser.SemErr("character set contains more than 1 character");
        s.append((char) set.First());
      } else parser.SemErr("comment delimiters must not be structured");
      p = p.next;
    }
    if (s.length() == 0 || s.length() > 2) {
      parser.SemErr("comment delimiters must be 1 or 2 characters long");
      s = new StringBuffer("?");
    }
    return s.toString();
  }

  public void NewComment(Node from, Node to, boolean nested) {
    Comment c = new Comment(CommentStr(from), CommentStr(to), nested);
    c.next = firstComment; firstComment = c;
  }

  //--------------------- scanner generation ------------------------

  void GenComBody(Comment com) {
    gen.println("\t\t\tfor(;;) {");
    gen.print  ("\t\t\t\tif (" + ChCond(com.stop.charAt(0)) + ") "); gen.println("{");
    if (com.stop.length() == 1) {
      gen.println("\t\t\t\t\tlevel--;");
      gen.println("\t\t\t\t\tif (level == 0) { oldEols = line - line0; NextCh(); return true; }");
      gen.println("\t\t\t\t\tNextCh();");
    } else {
      gen.println("\t\t\t\t\tNextCh();");
      gen.println("\t\t\t\t\tif (" + ChCond(com.stop.charAt(1)) + ") {");
      gen.println("\t\t\t\t\t\tlevel--;");
      gen.println("\t\t\t\t\t\tif (level == 0) { oldEols = line - line0; NextCh(); return true; }");
      gen.println("\t\t\t\t\t\tNextCh();");
      gen.println("\t\t\t\t\t}");
    }
    if (com.nested) {
      gen.print  ("\t\t\t\t}"); gen.println(" else if (" + ChCond(com.start.charAt(0)) + ") {");
      if (com.start.length() == 1)
        gen.println("\t\t\t\t\tlevel++; NextCh();");
      else {
        gen.println("\t\t\t\t\tNextCh();");
        gen.print  ("\t\t\t\t\tif (" + ChCond(com.start.charAt(1)) + ") "); gen.println("{");
        gen.println("\t\t\t\t\t\tlevel++; NextCh();");
        gen.println("\t\t\t\t\t}");
      }
    }
    gen.println(    "\t\t\t\t} else if (ch == Buffer.EOF) return false;");
    gen.println(    "\t\t\t\telse NextCh();");
    gen.println(    "\t\t\t}");
  }

  void GenComment(Comment com, int i) {
    gen.println();
    gen.print  ("\tboolean Comment" + i + "() "); gen.println("{");
    gen.println("\t\tint level = 1, pos0 = pos, line0 = line, col0 = col;");
    if (com.start.length() == 1) {
      gen.println("\t\tNextCh();");
      GenComBody(com);
    } else {
      gen.println("\t\tNextCh();");
      gen.print  ("\t\tif (" + ChCond(com.start.charAt(1)) + ") "); gen.println("{");
      gen.println("\t\t\tNextCh();");
      GenComBody(com);
      gen.println("\t\t} else {");
      gen.println("\t\t\tbuffer.setPos(pos0); NextCh(); line = line0; col = col0;");
      gen.println("\t\t}");
      gen.println("\t\treturn false;");
    }
    gen.println("\t}");
  }

    void CopyFramePart(String stop, boolean doOutput) {
        char startCh = stop.charAt(0);
        int endOfStopString = stop.length()-1;
        int ch = framRead();
        while (ch != EOF) {    // not EOF
            if (ch == startCh) {
                int i = 0;
                do {
                    if (i == endOfStopString) return; // stop[0..i] found
                    ch = framRead(); i++;
                } while (ch == stop.charAt(i));
                // stop[0..i-1] found; continue with last read character
                if (doOutput)
                    gen.print(stop.substring(0, i));
            } else {
                if (doOutput)
                    gen.print((char)ch);
                ch = framRead();
            }
        }
        throw new FatalError("Incomplete or corrupt scanner frame file");
    }


  void CopyFramePart(String stop) {
    CopyFramePart(stop, true);
  }

  String SymName(Symbol sym) {
    if (Character.isLetter(sym.name.charAt(0))) { // real name value is stored in Tab.literals
      //foreach (DictionaryEntry e in Tab.literals)
      java.util.Iterator iter = tab.literals.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry me = (Map.Entry) iter.next();
        if ((Symbol) me.getValue() == sym) return (String) me.getKey();
      }
    }
    return sym.name;
  }

  void GenLiterals () {
    List[] ts = new List[] { tab.terminals, tab.pragmas };
    for (int i = 0; i < ts.length; ++i) {
      Iterator iter = ts[i].iterator();
      while (iter.hasNext()) {
        Symbol sym = (Symbol) iter.next();
        if (sym.tokenKind == Symbol.litToken) {
          String name = SymName(sym);
          if (ignoreCase) name = name.toLowerCase();
          // sym.name stores literals with quotes, e.g. "\"Literal\"",
          gen.println("\t\tliterals.put(" + name + ", new Integer(" + sym.n + "));");
        }
      }
    }
  }

  void WriteState(State state) {
    Symbol endOf = state.endOf;
    gen.println("\t\t\t\tcase " + state.nr + ":");
    boolean ctxEnd = state.ctx;
    for (Action action = state.firstAction; action != null; action = action.next) {
      if (action == state.firstAction) gen.print("\t\t\t\t\tif (");
      else gen.print("\t\t\t\t\telse if (");
      if (action.typ == Node.chr) gen.print(ChCond((char)action.sym));
      else PutRange(tab.CharClassSet(action.sym));
      gen.print(") {");
      if (action.tc == Node.contextTrans) {
        gen.print("apx++; "); ctxEnd = false;
      } else if (state.ctx) {
        gen.print("apx = 0; ");
      }
      gen.println("AddCh(); state = " + action.target.state.nr + "; break;}");
    }
    if (state.firstAction == null)
      gen.print("\t\t\t\t\t{");
    else
      gen.print("\t\t\t\t\telse {");
    if (ctxEnd) { // final context state: cut appendix
      gen.println();
      gen.println("\t\t\t\t\ttlen -= apx;");
      gen.println("\t\t\t\t\tbuffer.setPos(t.pos); NextCh(); line = t.line; col = t.col; ");
      gen.println("\t\t\t\t\tfor (int i = 0; i < tlen; i++) NextCh();");
      gen.print  ("\t\t\t\t\t");
    }
    if (endOf == null) {
      gen.println("t.kind = noSym; break loop;}");
    } else {
      gen.print("t.kind = " + endOf.n + "; ");
      if (endOf.tokenKind == Symbol.classLitToken) {
        gen.println("t.val = new String(tval, 0, tlen); CheckLiteral(); return t;}");
      } else {
        gen.println("break loop;}");
      }
    }
  }

  void WriteStartTab() {
    for (Action action = firstState.firstAction; action != null; action = action.next) {
      int targetState = action.target.state.nr;
      if (action.typ == Node.chr) {
        gen.println("\t\tstart.set(" + action.sym + ", " + targetState + "); ");
      } else {
        CharSet s = tab.CharClassSet(action.sym);
        for (CharSet.Range r = s.head; r != null; r = r.next) {
          gen.println("\t\tfor (int i = " + r.from + "; i <= " + r.to + "; ++i) start.set(i, " + targetState + ");");
        }
      }
    }
    gen.println("\t\tstart.set(Buffer.EOF, -1);");
  }

  void OpenGen() {
    try {
      File f = new File
      (
          tab.outDir,
          (tab.prefixName == null ? "" : tab.prefixName) + "Scanner.java"
      );
      if (tab.makeBackup && f.exists()) {
        File old = new File(f.getPath() + ".bak");
        old.delete(); f.renameTo(old);
      }
      gen = new PrintWriter(new BufferedWriter(new FileWriter(f, false))); /* pdt */
    } catch (Exception e) {
      throw new FatalError("Cannot generate scanner file.");
    }
  }

  public void WriteScanner() {
    int oldPos = tab.buffer.getPos();  // Buffer.pos is modified by CopySourcePart
    int i;
    File fr = new File(tab.srcDir, "Scanner.frame");
    if (!fr.exists()) {
      if (tab.frameDir != null) fr = new File(tab.frameDir.trim(), "Scanner.frame");
      if (!fr.exists()) throw new FatalError("Cannot find Scanner.frame");
    }
    try {
      fram = new BufferedReader(new FileReader(fr)); /* pdt */
    } catch (FileNotFoundException e) {
      throw new FatalError("Cannot open Scanner.frame.");
    }

    if (dirtyDFA) MakeDeterministic();

    OpenGen();
    CopyFramePart("-->begin", false);
    tab.CopySourcePart(gen, tab.copyPos, 0);

    /* add package name, if it exists */
    if (tab.nsName != null && tab.nsName.length() > 0) {
      gen.print("package ");
      gen.print(tab.nsName);
      gen.println(";");
    }
    CopyFramePart("-->declarations");
    gen.println("\tstatic final int maxT = " + (tab.terminals.size()-1) + ";");
    gen.println("\tstatic final int noSym = " + tab.noSym.n + ";");
    if (ignoreCase)
      gen.print("\tchar valCh;       // current input character (for token.val)");
    CopyFramePart("-->initialization");
    WriteStartTab();
    GenLiterals();
    CopyFramePart("-->casing");
    if (ignoreCase) {
      gen.println("\t\tif (ch != Buffer.EOF) {");
      gen.println("\t\t\tvalCh = (char) ch;");
      gen.println("\t\t\tch = Character.toLowerCase(ch);");
      gen.println("\t\t}");
    }
    CopyFramePart("-->casing2");
    if (ignoreCase) gen.println("\t\t\ttval[tlen++] = valCh; ");
    else gen.println("\t\t\ttval[tlen++] = (char)ch; ");
    CopyFramePart("-->comments");
    Comment com = firstComment; i = 0;
    while (com != null) {
      GenComment(com, i);
      com = com.next; i++;
    }
    CopyFramePart("-->casing3");
    if (ignoreCase) {
        gen.println("\t\tval = val.toLowerCase();");
    }
    CopyFramePart("-->scan1");
    gen.print("\t\t\t");
    if (tab.ignored.Elements() > 0) { PutRange(tab.ignored); } else { gen.print("false"); }
    CopyFramePart("-->scan2");
    if (firstComment != null) {
      gen.print("\t\tif (");
      com = firstComment; i = 0;
      while (com != null) {
        gen.print(ChCond(com.start.charAt(0)));
        gen.print(" && Comment" + i + "()");
        if (com.next != null) gen.print(" ||");
        com = com.next; i++;
      }
      gen.print(") return NextToken();");
    }
    if (hasCtxMoves) { gen.println(); gen.print("\t\tint apx = 0;"); } /* pdt */
    CopyFramePart("-->scan3");
    for (State state = firstState.next; state != null; state = state.next)
      WriteState(state);
    CopyFramePart("$$$");
    gen.close();
    tab.buffer.setPos(oldPos);
  }

  public DFA (Parser parser) {
    this.parser = parser;
    tab = parser.tab;
    errors = parser.errors;
    firstState = null; lastState = null; lastStateNr = -1;
    firstState = NewState();
    firstMelted = null; firstComment = null;
    ignoreCase = false;
    dirtyDFA = false;
    hasCtxMoves = false;
  }

}


// ************************************************************************* //
