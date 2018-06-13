package sftlearning;

import com.google.common.collect.ImmutableList;
import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebraSubst;
import theory.characters.CharConstant;
import theory.characters.CharOffset;
import theory.characters.CharPred;
import theory.characters.TermInterface;
import transducers.sft.SFT;
import transducers.sft.SFTEpsilon;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;

import java.util.*;

public class BinBSFTLearner<P extends CharPred, F extends TermInterface, S> {

    public boolean debugOutput;

    /**
     * Initialize learner
     * Sets debugging mode to false
     */
    public BinBSFTLearner() {
        this.debugOutput = false;
    }

    /**
     * Initialize learner
     * @param debugOutput value indicating whether debugging mode should be on
     */
    public BinBSFTLearner(boolean debugOutput) {
        this.debugOutput = debugOutput;
    }

    /**
     * Prints logging to standard output
     * @param heading heading that is printed
     * @param value value that is printed
     */
    private void log(String heading, Object value) {
        if (this.debugOutput) {
            System.out.println("--------"+heading+"--------");
            System.out.println(value);
        }
    }

    public SFT<P, F, S> learn(SymbolicOracle<P, F, S> o, BooleanAlgebraSubst<P, F, S> ba) throws TimeoutException {
        // Initialize variables: table, conjecture, cx (counterexample)
        ObsTable table = new ObsTable(ba.generateWitness(ba.True()));
        SFT<P, F, S> conjecture = null;
        List<S> cx = null;

        // While no equivalent hypothesis automaton has been found
        table.fill(o);
        while (true) {
            // Close table
            boolean closed = false;
            while (!closed) {
                if (table.close()) {
                    // Table was not closed, fill table since strings were added to it in close()
                    table.fill(o);
                } else {
                    // Table is closed
                    closed = true;
                }
            }

            // Construct hypothesis
            table.fill(o);
            conjecture = table.buildSFT(o, ba);

            // Check equivalence of hypothesis automaton and system under learning (SUL)
            cx = o.checkEquivalence(conjecture);
            if (cx == null) {
                return conjecture;
            }

            // Process the found counterexample
            table.process(cx, o, ba);
        }
    }

    /**
     * Class that represents an Observation Table from which we can deduce an SFA
     */
    public class ObsTable {
        private List<List<S>> S, R, E, SUR;
        private Map<List<S>, List<S>> f;
        private S arbchar;

        /**
         * Initialize the observation table
         * @param arbchar arbitrary element from the input language
         */
        public ObsTable(S arbchar) {
            S = new ArrayList<List<S>>();
            R = new ArrayList<List<S>>();
            SUR = new ArrayList<List<S>>();
            E = new ArrayList<List<S>>();
            f = new HashMap<List<S>, List<S>>();
            this.arbchar = arbchar;

            // Add 'empty' / epsilon (S = {e})
            S.add(new ArrayList<S>());
            // Add S  = {e} to SUR so that SUR = {e}
            SUR.add(new ArrayList<S>());
//            List<S> r = new ArrayList<S>();
//            r.add(arbchar);
            // Add a (R = {a})
//            R.add(r);
            // S U R = {e} U {a} = {e, a}
//            SUR.add(r);
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

        public void addToTable(List<S> cx, SymbolicOracle o) throws TimeoutException {
            this.fill(o);
            if (!SUR.contains(cx)) {
                this.SUR.add(cx);
                this.fill(o);
                boolean similarRow = false;
                for (List<S> s : this.S) {
                    if (row(s).equals(row(cx))) {
                        similarRow = true;
                        break;
                    }
                }

                if (similarRow) {
                    this.R.add(cx);
                } else {
                    this.S.add(cx);
                }
            } else {
                if (R.contains(cx)) {
                    boolean similarRow = false;
                    for (List<S> s : this.S) {
                        if (row(s).equals(row(cx))) {
                            similarRow = true;
                            break;
                        }
                    }
                    if (!similarRow) {
                        S.add(cx);
                        R.remove(cx);
                    }
                }
            }
            this.fill(o);
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
        public void process(List<S> cx, SymbolicOracle o, BooleanAlgebraSubst ba) throws TimeoutException {
            // TODO: Check implementation of this method
            // Find distinguishing string d
            if (cx.size() == 1) {
                addToTable(cx, o);
                System.out.println(this);
            }

            int diff = cx.size();
            int same = 0;
            List<S> membershipAnswer;
            if (!f.containsKey(cx)) {
                membershipAnswer = o.checkMembership(cx);
                f.put(cx, membershipAnswer);
                addToTable(cx, o);
            } else {
                membershipAnswer = f.get(cx);
            }

            // Binary search to identify index upon which the response of the target machine
            // differs for the strings s_io z_>i0 and s_i0+1 z_>i0+1
            while((diff-same) != 1) {
                int i = (diff + same) / 2;
                List<S> accessString = runInHypothesis(o, ba, cx, i);
                List<S> toAdd = new ArrayList<>();
                for (int j=i; j<cx.size(); j++) {
                    toAdd.add(cx.get(j));
                }
                accessString.addAll(toAdd);
                if (!f.containsKey(accessString)) {
                    List<S> accStringAnswer = o.checkMembership(accessString);
                    f.put(accessString, accStringAnswer);
                    addToTable(accessString, o);
                    System.out.println(this);
                }
                if (membershipAnswer != f.get(accessString)) {
                    diff = i;
                } else {
                    same = i;
                }
            }
            // Construct s_i0 b
            List<S> wrongTransition = runInHypothesis(o, ba, cx, diff-1);
            wrongTransition.add(cx.get(diff-1));

            // Check whether s_i0 b == s_j mod W U {d} for some j ±= i_0 + 1
            // b is a character from the input language
            // d is the counterexample
            if (!SUR.contains(wrongTransition)) {
                // If so, then add s_i0 b to ^ (R)
                R.add(wrongTransition);
                SUR.add(wrongTransition);
            } else {
                // Else, add d to W (E)
                List<S> dist = new ArrayList<>();
                for (int i=diff; i<cx.size(); i++) {
                    dist.add(cx.get(i));
                }
                if (!E.contains(dist)) {
                    E.add(dist);
                }
                for (List<S> c : this.S) {
                    List<S> toAdd = new ArrayList<>();
                    toAdd.addAll(c);
                    toAdd.addAll(dist);
                    if (!SUR.contains(toAdd)) {
                        SUR.add(toAdd);
                        R.add(toAdd);
                    }
                }
            }
            fill(o);
            System.out.println(this);
        }

        private List<S> runInHypothesis(SymbolicOracle o, BooleanAlgebraSubst ba, List<S> cx, int i) throws TimeoutException {
            this.fill(o);
            SFT<P, F, S> hypothesis = this.buildSFT(o, ba);

            int state = hypothesis.getInitialState();
            System.out.println(hypothesis.toString());
            List<S> toSimulate = new ArrayList<>(cx.subList(0, i));
            for (S c : toSimulate) {
                boolean found = false;
                for (SFTInputMove<P, F, S> trans : hypothesis.getInputMovesFrom(state)) {
                    if (ba.HasModel(trans.guard, c)) {
                        found = true;
                        state = trans.to;
                        break;
                    }
                }
                if (!found) {
                    // TODO: Vind cases waarin dit gebeurt en go fix!
                    System.out.println("AAAH stuk: kon geen matchende transitie vinden");
                }
            }

            // We zijn in staat 'state' gekomen door de eerste i karakters te verwerken
            // Zoek de corresponding row in S op en return deze
            // Ik zoek de access string voor "state" -> find row(state) = row(cx.subList(0,i))?
            // Then find row equal to row(cx.subList(0,i)) in S
            List<S> index = this.S.get(state);
            return new ArrayList<>(index);
//            return hypothesis.outputOn(new ArrayList<>(cx.subList(0, i)), ba);
        }

        private SFT<P, F, S> buildSFT(SymbolicOracle o, BooleanAlgebraSubst ba) throws TimeoutException {
            // Define a state for each s in S
            // Set initial state to q_epsilon
            // Make state final if T(s, epsilon) = 1
            // Make transitions, guard(q_s) should return pair (g, q)
            // which means that we add the transitions q_s --- g ---> q
            Map<List<List<S>>, Map<List<List<S>>, Set<S>>> transitions = new HashMap<>();
            for (List<S> from : S) {
                Map<List<List<S>>, Set<S>> temp = new HashMap<>();
                for (List<S> to : S) {
                    temp.put(row(to), new HashSet<>());
                }
                // Make "empty" transitions between all states
                transitions.put(row(from), temp);
            }

            for (List<S> from : SUR) {
                for (List<S> to : SUR) {
                    if (to.size() != from.size() + 1 || !isPrefix(from, to))
                        continue;
                    S evid = to.get(to.size() - 1);
                    // Add the following transition: q_from ---evid---> q_to
                    transitions.get(row(from)).get(row(to)).add(evid);
                }
            }

            //now generalize the evidence into predicates
            List<SFTMove<P, F, S>> moves = new ArrayList<>();
            for (int i = 0; i < S.size(); i++) {
                //sb is the state from which we will add transitions
                List<List<S>> sb = row(S.get(i));
                Map<Set<S>, List<S>> groups = new HashMap<>();
                int j=0;
                for (List<S> sp : S) {
                    // sp is the state to which we 'move'
                    System.out.println("Added to groups: "+transitions.get(sb).get(row(sp))+" with output "+f.get(S.get(i)));
                    groups.put(transitions.get(sb).get(row(sp)), f.get(S.get(i)));
                }
                // sepPreds is a list of Predicates which are mapped to corresponding term functions
                LinkedHashMap<P, List<F>> sepPreds = ba.getSeparatingPredicatesAndTermFunctions(groups, this, S.get(i), Long.MAX_VALUE);
                System.out.println("Predicates with terms: "+sepPreds);
                int index = 0;
                for (P key : sepPreds.keySet()) {
                    System.out.println("Current set of transitions: "+moves);
                    System.out.println("Adding ("+i+") ---- "+key+" / "+sepPreds.get(key)+" ----> ("+index+")");
                    // Cannot simply assume i will be next state because we can have multiple transitions from i to j with different predicates
                    // Add the transition i---pred_j (key) / terms_j (sepPreds.get(key)) ---> j (index)

                    System.out.println("Checking whether predicate is []");
                    if (key == null || key.intervals == null || key.intervals.isEmpty() || key.intervals.get(0) == null) {
                        System.out.println("Predicate is epsilon");
                        List<S> output = new ArrayList<>();
                        for (F f : sepPreds.get(key)) {
                            if (f instanceof CharConstant) {
                                output.add((S) (Character) ((CharConstant) f).c);
                            }
                        }
                        moves.add(new SFTEpsilon<P, F, S>(i, index, output));
//                        moves.add(new SFTEpsilon<P, F, S>(i, index, sepPreds.get(key)));
                    }

                    moves.add(new SFTInputMove<>(i, index, key, sepPreds.get(key)));
                    index++;
                }
                System.out.println("Set of transitions after adding: "+moves);

            }

            //build and return the SFA
            // q_0 is the initial state
            Integer init = 0;
            HashMap<Integer, Set<List<S>>> fin = new HashMap<>();
            for (int i = 0; i < S.size(); i++) {
                // Add state i to final states if f(i) = true
                // TODO: For now we assume that all states are accepting
                fin.put(i, new HashSet<>());
            }
            SFT ret = SFT.MkSFT(moves, init, fin, ba);
            return ret;

            //return SFA.MkSFA(moves, init, fin, ba);
        }

        private void fill(SymbolicOracle o) throws TimeoutException {
            for (List<S> w : SUR) {
                for (List<S> e : E) {
                    List<S> we = new ArrayList<S>(w);
                    we.addAll(e);
                    if (!f.containsKey(we) || f.get(we) == null) {
                        List<S> value = o.checkMembership(we);
                        f.put(we, value);
                    }
                }
            }
            System.out.println(this);
        }

        /**
         * Checks whether the table is closed
         *
         * @return False if the table is closed, true if the table was changed
         */
        private boolean close() {
            Set<List<List<S>>> rowsS = new HashSet<>();
            List<S> best_r = null;

            for (List<S> s : this.S) {
                // Construct set containing all rows from the observation table corresponding to a word in S
                rowsS.add(row(s));
            }

            Set<List<List<S>>> rowsR = new HashSet<>();
            for (List<S> r : this.R) {
                // Check whether there is a row in R that is not in S
                if (!rowsS.contains(row(r))) {
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

            if (best_r == null) {
                // All rows in R are in S thus the table is closed
                return false;
            }

            // Add best_r to S
            S.add(best_r);
            R.remove(best_r);


            // If best_r is a prefix of an existing word in the table,
            // Then we do not need to add the one-step extension
            boolean cont = false;
            for (List<S> w : SUR) {
                if (w.size() != best_r.size() + 1)
                    continue;
                if (isPrefix(best_r, w)) {
                    cont = true;
                    break;
                }
            }
            //
            if (!cont) {
                List<S> ra = new ArrayList<>(best_r);
                ra.add(arbchar);
                R.add(ra);
                SUR.add(ra);
            }
            // TODO: Don't need to add ALL one-step extensions?
            return true;
        }

        /**
         * Returns the row in the observation table corresponding to w
         * @param w
         * @return
         */
        public List<List<S>> row(List<S> w) {
            return row(w, null);
        }

        /**
         * Returns the row in the observation table corresponding to w, while ignoring the characters in ignore
         * @param w
         * @param ignore
         * @return
         */
        public List<List<S>> row(List<S> w, List<S> ignore) {
            List<List<S>> ret = new ArrayList<>();
            for(List<S> e : E) {
                if (ignore != null && ignore.equals(e))
                    continue;
                List<S> we = new ArrayList<S>(w);
                we.addAll(e);
                ret.add(f.get(we)); //assumes f.containsKey(we)
            }
            return ret;
        }

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
                        if (f.get(we).isEmpty()) {
                            ret += "[]";
                        } else {
                            ret += f.get(we);
                        }
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
                        if (f.get(we).isEmpty()) {
                            ret += "[]";
                        } else {
                            ret += f.get(we);
                        }
                    }
                    else ret += "  ";
                }
            }
            return ret;
        }

        public List<List<S>> getS() {
            return S;
        }

        public List<List<S>> getR() {
            return R;
        }

        public List<List<S>> getE() {
            return E;
        }

        public List<List<S>> getSUR() {
            return SUR;
        }

        public Map<List<S>, List<S>> getF() {
            return f;
        }
    }


    public static void main(String[] args) {
        List<String> a = new ArrayList<>(Arrays.asList("xyz", "abc"));
        List<String> b = new ArrayList<>(Arrays.asList("xyz", "abc"));
        List<String> c = new ArrayList<>(Arrays.asList("abc", "xyz"));
        List<String> d = new ArrayList<>(Arrays.asList("qwe", "wer"));

        System.out.println(a.equals(b));
        System.out.println(!a.equals(c));
        System.out.println(!a.equals(d));
        System.out.println(a.equals(a));

        System.out.println(a.size());
        System.out.println(a.subList(0,0));
    }
}
