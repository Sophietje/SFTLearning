package learningalgorithm;

import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import learning.sfa.Oracle;
import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebra;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public class BinBLearner<P, S> {

    public boolean debugOutput;

    /**
     * Initialize learner
     * Sets debugging mode to false
     */
    public BinBLearner() {
        this.debugOutput = false;
    }

    /**
     * Initialize learner
     * @param debugOutput value indicating whether debugging mode should be on
     */
    public BinBLearner(boolean debugOutput) {
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

    public SFA<P, S> learn(Oracle<P, S> o, BooleanAlgebra<P, S> ba) throws TimeoutException {
        // Initialize variables: table, conjecture, cx (counterexample)
        ObsTable table = new ObsTable(ba.generateWitness(ba.True()));
        SFA<P, S> conjecture = null;
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
            conjecture = table.buildSFA(o, ba);
            conjecture = conjecture.mkTotal(ba);

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
    private class ObsTable {
        public List<List<S>> S, R, E, SUR;
        public Map<List<S>, Boolean> f;
        public S arbchar;

        /**
         * Initialize the observation table
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

        public void addToTable(List<S> cx, Oracle o) throws TimeoutException {
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

        public boolean getMembershipAnswer(List<S> s, Oracle<P, S> o) throws TimeoutException {
            if (f.containsKey(s)) {
                return f.get(s);
            } else {
                boolean answer = o.checkMembership(s);
                f.put(s, answer);
                addToTable(s, o);
                return answer;
            }
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
        public void process(List<S> cx, Oracle<P, S> o, BooleanAlgebra<P, S> ba) throws TimeoutException {
            if (cx.size() == 1) {
                addToTable(cx, o);
            }

            int diff = cx.size();
            int same = 0;
            boolean membershipAnswer = getMembershipAnswer(cx, o);

            // Binary search to identify index upon which the response of the target machine
            // differs for the strings s_io z_>i0 and s_i0+1 z_>i0+1
            while((diff-same) != 1) {
                int i = (diff + same) / 2;
                List<S> accessString = runInHypothesis(o, ba, cx, i);
                List<S> toAdd = new ArrayList<>(cx.subList(i, cx.size()));
                accessString.addAll(toAdd);

                boolean accessStringAnswer = getMembershipAnswer(accessString, o);
                if (membershipAnswer != accessStringAnswer) {
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
                List<S> dist = new ArrayList<>(cx.subList(diff, cx.size()));
                if (!E.contains(dist)) {
                    E.add(dist);
                }

                // Add extension for each s \in S to R.
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
        }

        private List<S> runInHypothesis(Oracle<P, S> o, BooleanAlgebra<P, S> ba, List<S> cx, int i) throws TimeoutException {
            SFA<P, S> hypothesis = this.buildSFA(o, ba);
            hypothesis.mkTotal(ba);

            int state = hypothesis.getInitialState();
            List<S> toSimulate = new ArrayList<>(cx.subList(0, i));
            for (S c : toSimulate) {
                boolean found = false;
                // Do not take into account epsilon transitions?
                // If c exists then this will never be the empty list thus it will not match an epsilon transition
                for (SFAInputMove<P, S> trans : hypothesis.getInputMovesFrom(state)) {
                    if (ba.HasModel(trans.guard, c)) {
                        found = true;
                        state = trans.to;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("AAAAH stuk: kon geen matchende transitie vinden, dit zou niet moeten gebeuren aangezien de hypothesis total moet zijn");
                }
            }

            // We zijn in staat 'state' gekomen door de eerste i karakters te verwerken
            // Zoek de corresponding row in S op en return deze
            // Ik zoek de access string voor "state" -> find row(state) = row(cx.subList(0,i))?
            // Then find row equal to row(cx.subList(0,i)) in S
            List<S> index = this.S.get(state);
            return new ArrayList<>(index);
        }

        private SFA<P, S> buildSFA(Oracle<P, S> o, BooleanAlgebra<P, S> ba) throws TimeoutException {
            // Define a state for each s in S
            // Set initial state to q_epsilon
            // Make state final if T(s, epsilon) = 1
            // Make transitions, guard(q_s) should return pair (g, q)
            // which means that we add the transitions q_s --- g ---> q
            Map<List<Boolean>, Map<List<Boolean>, Set<S>>> transitions = new HashMap<>();
            for (List<S> from : S) {
                Map<List<Boolean>, Set<S>> temp = new HashMap<>();
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
                    System.out.println("transitions "+transitions);
                    System.out.println("from "+from+ " to "+to+" with evid "+evid);
                    transitions.get(row(from)).get(row(to)).add(evid);
                }
            }

            //now generalize the evidence into predicates
            List<SFAMove<P, S>> moves = new ArrayList<SFAMove<P, S>>();
            for (int i = 0; i < S.size(); i++) {
                List<Boolean> sb = row(S.get(i));
                ArrayList<Collection<S>> groups_arr = new ArrayList<Collection<S>>();
                for (List<S> sp : S) {
                    groups_arr.add(transitions.get(sb).get(row(sp)));
                }
                ArrayList<P> sepPreds = ba.GetSeparatingPredicates(groups_arr, Long.MAX_VALUE);
                checkArgument(sepPreds.size() == S.size());
                for (int j = 0; j < sepPreds.size(); j++) {
                    // Add the transition i---pred_j ---> j
                    moves.add(new SFAInputMove<P, S>(i, j, sepPreds.get(j)));
                }

            }

            //build and return the SFA
            // q_0 is the initial state
            Integer init = 0;
            List<Integer> fin = new ArrayList<Integer>();
            for (int i = 0; i < S.size(); i++) {
                for (List<S> e : this.E) {
                    // Add state i to final states if f(i) = true
                    if (f.get(S.get(i))) {
                        fin.add(i);
                    }
                }
            }
            SFA ret = SFA.MkSFA(moves, init, fin, ba);
            return ret;

            //return SFA.MkSFA(moves, init, fin, ba);
        }

        private void fill(Oracle<P, S> o) throws TimeoutException {
            for (List<S> w : SUR) {
                for (List<S> e : E) {
                    List<S> we = new ArrayList<S>(w);
                    we.addAll(e);
                    if (!f.containsKey(we) || f.get(we) == null) {
                        boolean value = o.checkMembership(we);
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
            Set<List<Boolean>> rowsS = new HashSet<List<Boolean>>();
            List<S> best_r = null;

            for (List<S> s : this.S) {
                // Construct set containing all rows from the observation table corresponding to a word in S
                rowsS.add(row(s));
            }

            Set<List<Boolean>> rowsR = new HashSet<>();
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
