package learningalgorithm;

import org.sat4j.specs.TimeoutException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import theory.BooleanAlgebraSubst;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public class SFTLearner {

    public boolean debugOutput; //controls whether to write intermediary steps to System.out

    /**
     * Initialize learner
     * Sets debugging mode to false
     */
    public SFTLearner() {
        this.debugOutput = false;
    }

    /**
     * Initialize learner
     * @param debugOutput value indicating whether debugging mode should be on
     */
    public SFTLearner(boolean debugOutput) {
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
    public SFT<CharPred, CharFunc, Character> learn(SymbolicOracle<CharPred, CharFunc, Character> o, BooleanAlgebraSubst ba) throws TimeoutException {
        // Initialize observation table
        SymbolicObservationTable table = new SymbolicObservationTable('a');

        SFT<CharPred, CharFunc, Character> conjecture = null;
        List<Character> cx = null;

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
            conjecture = table.buildSFT(ba);
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
            System.out.println("Starting to process the counterexample");
            table.process(cx, o, conjecture, ba);

            //this.log("TBLpostCX", table);
            //Scanner scanner = new Scanner(System.in);
            //scanner.nextLine();
        }
    }

    private class SymbolicObservationTable {
        public List<List<Character>> S, R, E, SUR;
        public Map<List<Character>, List<Character>> f;
        public Character arbchar;
        private boolean sgProcessing = true;

        public SymbolicObservationTable(Character arbchar) {
            this.S = new ArrayList<List<Character>>();
            this.R = new ArrayList<>();
            this.E = new ArrayList<>();
            this.SUR = new ArrayList<>();
            this.f = new HashMap<>();
            this.arbchar = arbchar;

            // Character = {e} (e = \epsilon)
            S.add(new ArrayList<>());
            // R = {}
            // E = {e}
            E.add(new ArrayList<>());
        }

        public SymbolicObservationTable(Character arbchar, boolean sgProcessing) {
            this(arbchar);
            this.sgProcessing = sgProcessing;
        }

        /**
         * Checks whether w is a (strict) prefix of we
         * @param w first word
         * @param we second word
         * @return true if w is a strict prefix of we
         */
        //auxiliary method that checks whether
        //w is a strict prefix of we
        private boolean isPrefix(List<Character> w, List<Character> we) {
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
        private List<Character> getSuffix(List<Character> w, List<Character> we) {
            List<Character> suffix = new ArrayList<Character>();
            for (int k = w.size(); k < we.size(); k++)
                suffix.add(we.get(k));
            return suffix;
        }

        public void process(List<Character> cx, SymbolicOracle o, SFT conjecture, BooleanAlgebraSubst ba) throws TimeoutException {
            if (this.sgProcessing) {
                processShabhazGroz(cx, o, conjecture, ba);
            } else {
                processLStar(cx);
            }
        }

        /**
         * Processes a counterexample which is given in the form [a, b, c, ...]
         *
         * To do when processing a counterexample:
         * - Add all prefixes of the counterexample in Character
         * This is the same strategy as in the L* algorithm
         *
         * TODO: Can be optimized (see back in black paper) by making sure to keep the table reduced
         * @param cx the counterexample that should be processed
         */
        private void processLStar(List<Character> cx) {
            throw new NotImplementedException();
//            List<List<Character>> prefixes = new ArrayList<List<Character>>();
//            for (int i = 1; i <= cx.size(); i++) {
//                List<Character> prefix = new ArrayList<Character>();
//                for (int j = 0; j < i; j++)
//                    prefix.add(cx.get(j));
//                prefixes.add(prefix);
//            }
//
//            for (List<Character> p : prefixes) {
//                if (!SUR.contains(p)) {
//                    R.add(p);
//                    SUR.add(p);
//                }
//            }
//            System.out.println(this);
        }

        /**
         * Returns all suffixes of word (including word itself)
         * @param word
         * @return all suffixes of word
         */
        public List<List<Character>> getSuffixes(List<Character> word) {
            List<List<Character>> suffixes = new ArrayList<>();
            for (int i=0; i<word.size()+1; i++) {
                List<Character> suffix = new ArrayList<>(word.subList(0, i));
                suffixes.add(suffix);
            }
            return suffixes;
        }

        private void processShabhazGroz(List<Character> cx, SymbolicOracle o, SFT hypothesis, BooleanAlgebraSubst ba) throws TimeoutException {
            List<Character> hypothesisOutput = hypothesis.outputOn(cx, ba);
            List<Character> automatonOutput;
            if (f.containsKey(cx)) {
                automatonOutput = f.get(cx);
            } else {
                automatonOutput = o.checkMembership(cx);
                System.out.println("Adding "+cx+" to f with "+automatonOutput);
                f.put(cx, automatonOutput);
            }

            System.out.println("Checking whether automaton and hypothesis produce different outputs");
            if (hypothesisOutput.equals(automatonOutput)) {
                // It is not an actual counterexample since the automata produce the same output
                System.out.println("Hypothesis and automaton produce the same output");
                return;
            }
            // Find longest prefix s of cx such that s*d = z
            for (int i=cx.size()-1; i>=0; i--) {
                List<Character> s = cx.subList(0, i);
                if (S.contains(s)) {
                    // We have found the longest prefix s \in Character
                    // Therefore, d = z - s
                    List<Character> d = getSuffix(s, cx);
                    List<List<Character>> suffixes = getSuffixes(d);
                    for (List<Character> suffix : suffixes) {
                        if (!E.contains(suffix)) {
                            E.add(suffix);
                        }
                    }

                }
            }
            this.fill(o);
            System.out.println("Table : "+this);
        }


        /**
         * Returns whether the given automaton is consistent with the observation table
         * @param sft automaton (SFA)
         * @param ba boolean algebra
         * @return True if automaton is consistent with observation table, else False
         * @throws TimeoutException
         */
        //sanity check to verify a conjectured automaton
        //is consistent with the observation table
        public boolean consistent(SFT<CharPred, CharFunc, Character> sft, BooleanAlgebraSubst<CharPred, CharFunc, Character> ba) throws TimeoutException {
            for (List<Character> w : SUR) {
                for (List<Character> e : E) {
                    List<Character> we = new ArrayList<Character>(w);
                    we.addAll(e);
                    if (!f.get(we).equals(sft.outputOn(we, ba))) {
                        //System.out.println("inconsistent on " + we);
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Returns the row in the observation table corresponding to w
         * @param w
         * @return
         */
        public List<List<Character>> row(List<Character> w) {
            return row(w, null);
        }

        /**
         * Returns the row in the observation table corresponding to w, while ignoring the characters in ignore
         * @param w
         * @param ignore
         * @return
         */
        public List<List<Character>> row(List<Character> w, List<Character> ignore) {
            List<List<Character> >ret = new ArrayList<>();
            for(List<Character> e : E) {
                if (ignore != null && ignore.equals(e))
                    continue;
                List<Character> we = new ArrayList<Character>(w);
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
        public void fill(SymbolicOracle<CharPred, CharFunc, Character> o) throws TimeoutException {
            for (List<Character> w : SUR) {
                for (List<Character> e : E) {
                    List<Character> we = new ArrayList<Character>(w);
                    we.addAll(e);
                    if (!f.containsKey(we)) {
                        List<Character> value = o.checkMembership(we);
                        System.out.println("Adding " + we + " to f with value " + value);
                        f.put(we, value);
                    }
                }
            }
        }

        /**
         * Checks whether the table is closed
         *
         * An observation table is closed when:
         * - for all one-step extensions (t) of Character, there should exist a row in Character such that row(s) = row(t)
         * @return False if table is closed, true if it changes anything
         */
        //returns true if makes a change, needs to be applied until returns false
        public boolean close() {
            Set<List<List<Character>>> sigs = new HashSet<List<List<Character>>>();
            for (List<Character> s : S)
                sigs.add(row(s));
            List<Character> best_r = null;
            for (List<Character> r : R) {
                // If there is a row in R with no similar row in Character, then the table is not closed!
                if (!sigs.contains(row(r))) {
                    //for membership query efficiency,
                    //instead of just moving r to Character, move the shortest r' with row(r) = row(r')
                    best_r = r;
                    for (List<Character> rp : R) {
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
            // Table is closed since there was no row in R without a similar row in Character
            // Otherwise best_r would not be null
            if (best_r == null)
                return false;

            List<Character> r = best_r;
            //  Move row R (without a corresponding row in Character) to Character, remove from R
            S.add(r);
            R.remove(r);

            // Add all one-step extensions of r as rows (if not yet in the set of rows)
            //handle evidence-closure
            for (List<Character> e : E) {
                List<Character> re = new ArrayList<Character>(r);
                re.addAll(e);
                if (!SUR.contains(re)) {
                    R.add(re);
                    SUR.add(re);
                }
            }

            //in case all the e in E are more than single char,
            //ensure continuation r.a in SUR
            boolean cont = false;
            for (List<Character> w : SUR) {
                if (w.size() != r.size() + 1)
                    continue;
                if (isPrefix(r, w)) {
                    cont = true;
                    break;
                }
            }
            // Add one step extension of r with an arbitrary character from the alphabet
            if (!cont) {
                List<Character> ra = new ArrayList<Character>(r);
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
         * - for all s1, s2 in Character: if row(s1) = row(s2) then the rows of all their one-step extensions should be the same
         * @return False if table is consistent, true if it changes anything
         */
        //returns true if makes a change, needs to be applied until returns false
        public boolean make_consistent() {
            for (int i = 0; i < SUR.size(); i++) {
                for (int j = i + 1; j < SUR.size(); j++) {
                    List<Character> w1 = SUR.get(i);
                    List<Character> w2 = SUR.get(j);
                    // Rows are not equal so the implication is by default true, therefore we can skip this pair
                    if (!row(w1).equals(row(w2)))
                        continue;
                    Set<List<Character>> cont1 = new HashSet<List<Character>>();
                    Set<List<Character>> cont2 = new HashSet<List<Character>>();
                    // Make sets containing all one-step extensions
                    for (List<Character> wa : SUR) {
                        if (isPrefix(w1, wa))
                            cont1.add(wa);
                        if (isPrefix(w2, wa))
                            cont2.add(wa);
                    }
                    for (List<Character> w1a : cont1) {
                        List<Character> suffix1 = getSuffix(w1, w1a);
                        for (List<Character> w2a : cont2) {
                            List<Character> suffix2 = getSuffix(w2, w2a);
                            // We only want to consider the words with the same extension, thus move on to the next pair
                            if (!suffix1.equals(suffix2))
                                continue;
                            List<List<Character>> r1 = row(w1a);
                            List<List<Character>> r2 = row(w2a);
                            if (!r1.equals(r2)) {
                                //at this point,
                                //row(w1) == row(w2) but row(w1e) != row(w2e)
                                //find the problematic suffix in E and concatenate it to the common suffix
                                List<Character> e = new ArrayList<Character>(suffix1);
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
                                for (List<Character> s : S) {
                                    List<Character> se = new ArrayList<Character>(s);
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
            List<Character> e = E.get(E.size() - 1);
            //System.out.println("mkcons added: " + e.toString());
            Set<List<Character>> toAdd = new HashSet<List<Character>>();
            boolean addFlag;
            //find pairs u1,u2 in SUR with row(u1) = row(u2) but f(u1e) != f(u2e)
            //(where row does not include the e index)
            List<Character> u1, u2, u1e, u2e;
            for (int i = 0; i < SUR.size(); i++) {
                for (int j = i + 1; j < SUR.size(); j++) {
                    u1 = SUR.get(i);
                    u2 = SUR.get(j);
                    if (!row(u1,e).equals(row(u2,e)))
                        continue;
                    u1e = new ArrayList<Character>(u1);
                    u1e.addAll(e);
                    u2e = new ArrayList<Character>(u2);
                    u2e.addAll(e);
                    if (f.get(u1e).equals(f.get(u2e)))
                        continue;
                    //if a continuation of u1 by b is in the table, u2b needs to be in the table
                    //and vice-versa
                    for (List<Character> unb : SUR) {
                        if (unb.size() == u1.size() + 1 && isPrefix(u1,unb)) {
                            List<Character> b = getSuffix(u1,unb); //b is a word of length 1
                            //actually, don't need to add u2b if
                            //there already exists wb with row(w) = row(u2)
                            addFlag = true;
                            for (List<Character> w : SUR) {
                                if (!row(w).equals(row(u2)))
                                    continue;
                                List<Character> wb = new ArrayList<Character>(w);
                                wb.addAll(b);
                                if (SUR.contains(wb) || toAdd.contains(wb)) {
                                    addFlag = false;
                                    break;
                                }
                            }
                            if (addFlag) {
                                List<Character> u2b = new ArrayList<Character>(u2);
                                u2b.addAll(b);
                                if (!SUR.contains(u2b))
                                    toAdd.add(u2b);
                            }
                        }
                        if (unb.size() == u2.size() + 1 && isPrefix(u2,unb)) {
                            List<Character> b = getSuffix(u2,unb);
                            addFlag = true;
                            for (List<Character> w : SUR) {
                                if (!row(w).equals(row(u1)))
                                    continue;
                                List<Character> wb = new ArrayList<Character>(w);
                                wb.addAll(b);
                                if (SUR.contains(wb) || toAdd.contains(wb)) {
                                    addFlag = false;
                                    break;
                                }
                            }
                            if (addFlag) {
                                List<Character> u1b = new ArrayList<Character>(u1);
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
         * Construct SFA from observation table
         * @param ba
         * @return
         * @throws TimeoutException
         */
        public SFT<CharPred, CharFunc, Character> buildSFT(BooleanAlgebraSubst<CharPred, CharFunc, Character> ba) throws TimeoutException {
            //first build the evidence automaton's transition system
            Map<List<List<Character>>, Map<List<List<Character>>, Set<Character>>> trans;
            trans = new HashMap<>();
            for (List<Character> s : S) {
                Map<List<List<Character>>, Set<Character>> temp = new HashMap<>();
                for (List<Character> sp : S)
                    temp.put(row(sp), new HashSet<Character>());
                trans.put(row(s), temp);
            }
            for (List<Character> w : SUR) {
                for (List<Character> wa : SUR) {
                    if (wa.size() != w.size() + 1 || !isPrefix(w, wa))
                        continue;
                    Character evid = wa.get(wa.size() - 1);
                    // Add the following transition: q_w ---a---> q_wa
                    trans.get(row(w)).get(row(wa)).add(evid);
                }
            }

            //now generalize the evidence into predicates
            List<SFTMove<CharPred, CharFunc, Character>> moves = new ArrayList<SFTMove<CharPred, CharFunc, Character>>();
            for (int i = 0; i < S.size(); i++) {
                List<List<Character>> sb = row(S.get(i));
                ArrayList<Collection<Character>> groups_arr = new ArrayList<Collection<Character>>();
                for (List<Character> sp : S) {
                    groups_arr.add(trans.get(sb).get(row(sp)));
                }
                LinkedHashMap<CharPred, List<CharFunc>> sepPreds = ba.getSeparatingPredicatesAndTermFunctions(groups_arr, Long.MAX_VALUE);
                checkArgument(sepPreds.size() == S.size());

                int index = 0;
                for (CharPred key : sepPreds.keySet()) {
                    // Add the transition i---pred_j ---> j
                    moves.add(new SFTInputMove<>(i, index, key, sepPreds.get(key)));
                    index++;
                }
            }

            //build and return the SFA
            // q_0 is the initial state
            Integer init = 0;
            Map<Integer, Set<List<Character>>> fin = new HashMap<>();
            for (int i=0; i<S.size(); i++) {
                // Add state i to final states if f(i) = true
                // For now assume that all states are final
                // TODO: figure out set of final states
                fin.put(i, new HashSet<>());
            }
            //System.out.println("SFAmoves:" + moves);
            //System.out.println("init:" + init + "\nfin:" + fin);
            //System.out.println("building");
            SFT ret = SFT.MkSFT(moves, init, fin, ba);
            //System.out.println("returning");
            return ret;
        }

        /**
         * Print observation table
         * @return observation table in string form
         */
        @Override
        public String toString() {
            String ret = "E:";
            for (List<Character> w : E) ret += " " + w;
            ret += "\nCharacter:\n";
            for (List<Character> w : S) {
                ret += " " + w + " :";
                for (List<Character> e : E) {
                    List<Character> we = new ArrayList<Character>(w);
                    we.addAll(e);
                    if (f.containsKey(we)) {
                        if (f.get(we).isEmpty()) {
                            ret += "[]";
                        } else {
                            ret += " " + f.get(we);
                        }
                    } else {
                        ret += "  " ;
                    }
                }
                ret += "\n";
            }
            ret += "R:";
            for (List<Character> w : R) {
                ret += "\n " + w + " :";
                for (List<Character> e : E) {
                    List<Character> we = new ArrayList<Character>(w);
                    we.addAll(e);
                    if (f.containsKey(we)) {
                        if (f.get(we).isEmpty()) {
                            ret += "[]";
                        } else {
                            ret += " " + f.get(we);
                        }
                    } else {
                        ret += "  " ;
                    }
                }
            }
            return ret;
        }
    }


}
