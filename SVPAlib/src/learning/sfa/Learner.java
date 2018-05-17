package learning.sfa;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.sat4j.specs.TimeoutException;

import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import theory.BooleanAlgebra;

//An implementation of the SFA learning algorithm from
//S. Drews and L. D'Antoni "Learning Symbolic Automata" (TACAS 2017)
public class Learner<P, S> {

	public boolean debugOutput; //controls whether to write intermediary steps to System.out

	/**
	 * Initialize learner
	 * Sets debugging mode to false
	 */
	public Learner() {
		this.debugOutput = false;
	}

	/**
	 * Initialize learner
	 * @param debugOutput value indicating whether debugging mode should be on
	 */
	public Learner(boolean debugOutput) {
		this.debugOutput = debugOutput;
	}

	/**
	 * Prints logging to standard output
	 * @param heading heading that is printed
	 * @param value value that is printed
	 */
	private void log(String heading, Object value) {
		if (this.debugOutput) {
			System.out.println("========" + heading + "========");
			System.out.println(value);
		}
	}

	/**
	 * Learns an automaton using o as an oracle with boolean algebra ba
	 * @param o oracle
	 * @param ba boolean algebra
	 * @return learned automaton (SFA)
	 * @throws TimeoutException
	 */
	public SFA<P, S> learn(Oracle<P, S> o, BooleanAlgebra<P, S> ba) throws TimeoutException {
		// Initialize observation table
		ObsTable table = new ObsTable(ba.generateWitness(ba.True()));
		
		SFA<P, S> conjecture = null;
		List<S> cx = null;
		
		while (true) {
			// Fill observation table
			table.fill(o);
			
			//this.log("TBL after fill", table);

			boolean consflag = true, closeflag = true;
			// Repeat while the observation table is not closed and not consistent
			do {
				//make-consistent can add to E,
				//so after it is run we need to
				//(i)  fill the table
				//(ii) check closed again
				if (consflag)
					consflag = table.make_consistent();
				if (consflag) {
					table.fill(o);
					
					//this.log("TBL after mkcons", table);

					boolean distflag = table.distribute();
					if (distflag)
						table.fill(o);
					
					closeflag = true;
				}
				//close can add to R,
				//so after it is run we need to
				//(i)  fill the table
				//(ii) check consistency again
				if (closeflag)
					closeflag = table.close();
				if (closeflag) {
					table.fill(o);
					consflag = true;
				}
				//note that evidence-closure is handled by the other subroutines
			} while (consflag || closeflag);
			
			this.log("Obs Table", table);

			// Build hypothesis automaton
			conjecture = table.buildSFA(ba);
			//System.out.println("total");
			conjecture = conjecture.mkTotal(ba);
			//System.out.println("finished");

			this.log("SFA guess", conjecture);

			//System.out.println("sanity checking consistency");
			//checkArgument(table.consistent(conjecture, ba));
			//System.out.println("passed");

			// Check equivalence of hypothesis to system under learning (SUL)
			cx = o.checkEquivalence(conjecture);
			if (cx == null) {
				// Report statistics if hypothesis automaton is correct
				this.log("statistics", 
						"# equiv: " + o.getNumEquivalence() + 
						"\n# mem: " + o.getNumMembership());
				return conjecture;
			}

			this.log("counterex", (cx == null ? "none" : cx));
			
			//process the counterexample
			table.process(cx);
			
			//this.log("TBLpostCX", table);

			//Scanner scanner = new Scanner(System.in);
			//scanner.nextLine();
		}
	}
	
	private class ObsTable {
		public List<List<S>> S, R, E, SUR;
		public Map<List<S>, Boolean> f;
		public S arbchar;

		/**
		 * Initialize the observation table
		 *
		 * @param arbchar arbitrary element from the input language
		 */
		public ObsTable(S arbchar) {
			S = new ArrayList<List<S>>();
			R = new ArrayList<List<S>>();
			SUR = new ArrayList<List<S>>();
			E = new ArrayList<List<S>>();
			f = new HashMap<List<S>, Boolean>();
			this.arbchar = arbchar;

			// Add 'empty' / epsilon (S = {e})
			S.add(new ArrayList<S>());
			SUR.add(new ArrayList<S>());
			List<S> r = new ArrayList<S>();
			r.add(arbchar);
			// Add a (R = {a})
			R.add(r);
			// S U R = {e} U {a} = {e, a}
			SUR.add(r);
			// Add 'empty' / epsilon (E = {e})
			E.add(new ArrayList<S>());
		}

		/**
		 * Checks whether w is a (strict) prefix of we
		 * @param w first word
		 * @param we second word
		 * @return true if w is a strict prefix of we
		 */
		//auxiliary method that checks whether
		//w is a strict prefix of we
		private boolean isPrefix(List<S> w, List<S> we) {
			if (w.size() >= we.size())
				return false;
			for (int i = 0; i < w.size(); i++)
				if (!w.get(i).equals(we.get(i)))
					return false;
			return true;
		}

		/**
		 * Returns the suffix of w and we
		 * @param w first word
		 * @param we second word
		 * @return suffix of w and we
		 */
		//assumes isPrefix(w, we)
		private List<S> getSuffix(List<S> w, List<S> we) {
			List<S> suffix = new ArrayList<S>();
			for (int k = w.size(); k < we.size(); k++)
				suffix.add(we.get(k));
			return suffix;
		}

		/**
		 * Processes a counterexample which is given in the form [a, b, c, ...]
		 *
		 * To do when processing a counterexample:
		 * - Add all prefixes of the counterexample in S
		 *
		 * TODO: Can be optimized (see back in black paper) by making sure to keep the table reduced
		 * @param cx the counterexample that should be processed
		 */
		public void process(List<S> cx) {
			List<List<S>> prefixes = new ArrayList<List<S>>();
			for (int i = 1; i <= cx.size(); i++) {
				List<S> prefix = new ArrayList<S>();
				for (int j = 0; j < i; j++)
					prefix.add(cx.get(j));
				prefixes.add(prefix);
			}
			
			for (List<S> p : prefixes) {
				if (!SUR.contains(p)) {
					R.add(p);
					SUR.add(p);
				}
			}
		}

		/**
		 * Returns whether the given automaton is consistent with the observation table
		 * @param sfa automaton (SFA)
		 * @param ba boolean algebra
		 * @return True if automaton is consistent with observation table, else False
		 * @throws TimeoutException
		 */
		//sanity check to verify a conjectured automaton
		//is consistent with the observation table
		public boolean consistent(SFA<P, S> sfa, BooleanAlgebra<P, S> ba) throws TimeoutException {
			for (List<S> w : SUR) {
				for (List<S> e : E) {
					List<S> we = new ArrayList<S>(w);
					we.addAll(e);
					if (!f.get(we).equals(sfa.accepts(we, ba))) {
						//System.out.println("inconsistent on " + we);
						return false;
					}
				}
			}
			return true;
		}

		/**
		 * Construct SFA from observation table
		 * @param ba
		 * @return
		 * @throws TimeoutException
		 */
		public SFA<P, S> buildSFA(BooleanAlgebra<P, S> ba) throws TimeoutException {
			//first build the evidence automaton's transition system
			Map<List<Boolean>, Map<List<Boolean>, Set<S>>> trans;
			trans = new HashMap<List<Boolean>, Map<List<Boolean>, Set<S>>>();
			for (List<S> s : S) {
				Map<List<Boolean>, Set<S>> temp = new HashMap<List<Boolean>, Set<S>>();
				for (List<S> sp : S)
					temp.put(row(sp), new HashSet<S>());
				trans.put(row(s), temp);
			}
			for (List<S> w : SUR) {
				for (List<S> wa : SUR) {
					if (wa.size() != w.size() + 1 || !isPrefix(w, wa))
						continue;
					S evid = wa.get(wa.size() - 1);
					// Add the following transition: q_w ---a---> q_wa
					trans.get(row(w)).get(row(wa)).add(evid);
				}
			}
			
			//now generalize the evidence into predicates
			List<SFAMove<P, S>> moves = new ArrayList<SFAMove<P, S>>();
			for (int i = 0; i < S.size(); i++) {
				List<Boolean> sb = row(S.get(i));
				ArrayList<Collection<S>> groups_arr = new ArrayList<Collection<S>>();
				for (List<S> sp : S) {
					groups_arr.add(trans.get(sb).get(row(sp)));
				}
				ArrayList<P> sepPreds = ba.GetSeparatingPredicates(groups_arr, Long.MAX_VALUE);
				checkArgument(sepPreds.size() == S.size());
				for (int j = 0; j < sepPreds.size(); j++)
					// Add the transition i---pred_j ---> j
					moves.add(new SFAInputMove<P, S>(i, j, sepPreds.get(j)));
			}
			
			//build and return the SFA
			// q_0 is the initial state
			Integer init = 0;
			List<Integer> fin = new ArrayList<Integer>();
			for (int i = 0; i < S.size(); i++) {
				// Add state i to final states if f(i) = true
				if (f.get(S.get(i)))
					fin.add(i);
			}
			//System.out.println("SFAmoves:" + moves);
			//System.out.println("init:" + init + "\nfin:" + fin);
			//System.out.println("building");
			SFA ret = SFA.MkSFA(moves, init, fin, ba);
			//System.out.println("returning");
			return ret;
			
			//return SFA.MkSFA(moves, init, fin, ba);
		}

		/**
		 * Returns the row in the observation table corresponding to w
		 * @param w
		 * @return
		 */
		public List<Boolean> row(List<S> w) {
			return row(w, null);
		}

		/**
		 * Returns the row in the observation table corresponding to w, while ignoring the characters in ignore
		 * @param w
		 * @param ignore
		 * @return
		 */
		public List<Boolean> row(List<S> w, List<S> ignore) {
			List<Boolean> ret = new ArrayList<Boolean>();
			for(List<S> e : E) {
				if (ignore != null && ignore.equals(e))
					continue;
				List<S> we = new ArrayList<S>(w);
				we.addAll(e);
				ret.add(f.get(we)); //assumes f.containsKey(we)
			}
			return ret;
		}

		/**
		 * Add the missing entries to the observation table
		 * @param o oracle that returns the output given a word
		 * @throws TimeoutException
		 */
		public void fill(Oracle<P, S> o) throws TimeoutException { 
			for (List<S> w : SUR) {
				for (List<S> e : E) {
					List<S> we = new ArrayList<S>(w);
					we.addAll(e);
					if (!f.containsKey(we))
						f.put(we, o.checkMembership(we));
				}
			}
		}

		/**
		 * Checks whether the table is closed
		 *
		 * An observation table is closed when:
		 * - for all one-step extensions (t) of S, there should exist a row in S such that row(s) = row(t)
		 * @return False if table is closed, true if it changes anything
		 */
		//returns true if makes a change, needs to be applied until returns false
		public boolean close() {
			Set<List<Boolean>> sigs = new HashSet<List<Boolean>>();
			for (List<S> s : S)
				sigs.add(row(s));
			List<S> best_r = null;
			for (List<S> r : R) {
				// If there is a row in R with no similar row in S, then the table is not closed!
				if (!sigs.contains(row(r))) {
					//for membership query efficiency,
					//instead of just moving r to S, move the shortest r' with row(r) = row(r')
					best_r = r;
					for (List<S> rp : R) {
						if (!row(r).equals(row(rp)))
							continue;
						if (r.equals(rp))
							continue;
						if (rp.size() < best_r.size())
							best_r = rp;
					}
					break;
				}
			}
			// Table is closed since there was no row in R without a similar row in S
			// Otherwise best_r would not be null
			if (best_r == null)
				return false;
			
			List<S> r = best_r;
			//  Move row R (without a corresponding row in S) to S, remove from R
			S.add(r);
			R.remove(r);

			// Add all one-step extensions of r as rows (if not yet in the set of rows)
			//handle evidence-closure
			for (List<S> e : E) { 
				List<S> re = new ArrayList<S>(r);
				re.addAll(e);
				if (!SUR.contains(re)) {
					R.add(re);
					SUR.add(re);
				}
			}
			
			//in case all the e in E are more than single char,
			//ensure continuation r.a in SUR
			boolean cont = false;
			for (List<S> w : SUR) {
				if (w.size() != r.size() + 1)
					continue;
				if (isPrefix(r, w)) {
					cont = true;
					break;
				}
			}
			// Add one step extension of r with an arbitrary character from the alphabet
			if (!cont) {
				List<S> ra = new ArrayList<S>(r);
				ra.add(arbchar);
				R.add(ra);
				SUR.add(ra);
			}
			
			return true;
		}

		/**
		 * Makes the observation table consistent
		 *
		 * An observation table is consistent when:
		 * - for all s1, s2 in S: if row(s1) = row(s2) then the rows of all their one-step extensions should be the same
		 * @return False if table is consistent, true if it changes anything
		 */
		//returns true if makes a change, needs to be applied until returns false
		public boolean make_consistent() { 
			for (int i = 0; i < SUR.size(); i++) {
				for (int j = i + 1; j < SUR.size(); j++) {
					List<S> w1 = SUR.get(i);
					List<S> w2 = SUR.get(j);
					// Rows are not equal so the implication is by default true, therefore we can skip this pair
					if (!row(w1).equals(row(w2)))
						continue;
					Set<List<S>> cont1 = new HashSet<List<S>>();
					Set<List<S>> cont2 = new HashSet<List<S>>();
					// Make sets containing all one-step extensions
					for (List<S> wa : SUR) {
						if (isPrefix(w1, wa))
							cont1.add(wa);
						if (isPrefix(w2, wa))
							cont2.add(wa);
					}
					for (List<S> w1a : cont1) {
						List<S> suffix1 = getSuffix(w1, w1a);
						for (List<S> w2a : cont2) {
							List<S> suffix2 = getSuffix(w2, w2a);
							// We only want to consider the words with the same extension, thus move on to the next pair
							if (!suffix1.equals(suffix2))
								continue;
							List<Boolean> r1 = row(w1a);
							List<Boolean> r2 = row(w2a);
							if (!r1.equals(r2)) {
								//at this point,
								//row(w1) == row(w2) but row(w1e) != row(w2e)
								//find the problematic suffix in E and concatenate it to the common suffix
								List<S> e = new ArrayList<S>(suffix1);
								for (int k = 0; k < E.size(); k++){
									if (!r1.get(k).equals(r2.get(k))) {
										e.addAll(E.get(k));
										break;
									}
								}
								E.add(e);
								//distribute the old evidence in a separate function
								//i.e. find pairs u1,u2 in SUR with row(u1) = row(u2)
								//     but after adding e to E, row(u1) != row(u2)
								//this requires filling the table, first
								//handle evidence-closure
								for (List<S> s : S) {
									List<S> se = new ArrayList<S>(s);
									se.addAll(e);
									if (!SUR.contains(se)) {
										R.add(se);
										SUR.add(se);
									}
								}
								return true;
							}
						}
					}
				}
			}
			return false;
		}
		
		//this is called assuming that make_consistent added an element (exactly one element) to E
		public boolean distribute() {
			List<S> e = E.get(E.size() - 1);
			//System.out.println("mkcons added: " + e.toString());
			Set<List<S>> toAdd = new HashSet<List<S>>();
			boolean addFlag;
			//find pairs u1,u2 in SUR with row(u1) = row(u2) but f(u1e) != f(u2e)
			//(where row does not include the e index)
			List<S> u1, u2, u1e, u2e;
			for (int i = 0; i < SUR.size(); i++) {
				for (int j = i + 1; j < SUR.size(); j++) {
					u1 = SUR.get(i);
					u2 = SUR.get(j);
					if (!row(u1,e).equals(row(u2,e)))
						continue;
					u1e = new ArrayList<S>(u1);
					u1e.addAll(e);
					u2e = new ArrayList<S>(u2);
					u2e.addAll(e); 
					if (f.get(u1e).equals(f.get(u2e)))
						continue;
					//if a continuation of u1 by b is in the table, u2b needs to be in the table
					//and vice-versa
					for (List<S> unb : SUR) {
						if (unb.size() == u1.size() + 1 && isPrefix(u1,unb)) {
							List<S> b = getSuffix(u1,unb); //b is a word of length 1
							//actually, don't need to add u2b if
							//there already exists wb with row(w) = row(u2)
							addFlag = true;
							for (List<S> w : SUR) {
								if (!row(w).equals(row(u2)))
									continue;
								List<S> wb = new ArrayList<S>(w);
								wb.addAll(b);
								if (SUR.contains(wb) || toAdd.contains(wb)) {
									addFlag = false;
									break;
								}
							}
							if (addFlag) {
								List<S> u2b = new ArrayList<S>(u2);
								u2b.addAll(b);
								if (!SUR.contains(u2b))
									toAdd.add(u2b);
							}
						}
						if (unb.size() == u2.size() + 1 && isPrefix(u2,unb)) {
							List<S> b = getSuffix(u2,unb);
							addFlag = true;
							for (List<S> w : SUR) {
								if (!row(w).equals(row(u1)))
									continue;
								List<S> wb = new ArrayList<S>(w);
								wb.addAll(b);
								if (SUR.contains(wb) || toAdd.contains(wb)) {
									addFlag = false;
									break;
								}
							}
							if (addFlag) {
								List<S> u1b = new ArrayList<S>(u1);
								u1b.addAll(b);
								if (!SUR.contains(u1b))
									toAdd.add(u1b);
							}
						}
					}
				}
			}
			//System.out.println("distributing evidence by adding:");
			//for (List<A> w : toAdd)
			//	System.out.println(w.toString());
			//Scanner scanner = new Scanner(System.in);
			//scanner.nextLine();
			R.addAll(toAdd);
			SUR.addAll(toAdd);
			return toAdd.size() > 0;
		}

		/**
		 * Print observation table
		 * @return observation table in string form
		 */
		@Override
		public String toString() {
			String ret = "E:";
			for (List<S> w : E) ret += " " + w;
			ret += "\nS:\n";
			for (List<S> w : S) {
				ret += " " + w + " :";
				for (List<S> e : E) {
					List<S> we = new ArrayList<S>(w);
					we.addAll(e);
					if (f.containsKey(we)) {
						if (f.get(we)) ret += " +";
						else ret += " -";
					}
					else ret += "  ";
				}
				ret += "\n";
			}
			ret += "R:";
			for (List<S> w : R) {
				ret += "\n " + w + " :";
				for (List<S> e : E) {
					List<S> we = new ArrayList<S>(w);
					we.addAll(e);
					if (f.containsKey(we)) {
						if (f.get(we)) ret += " +";
						else ret += " -";
					}
					else ret += "  ";
				}
			}
			return ret;
		}

	}
}