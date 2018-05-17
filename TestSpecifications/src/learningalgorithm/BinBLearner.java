package learningalgorithm;

import automata.sfa.SFA;
import learning.sfa.Oracle;
import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebra;

import java.util.*;

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
            // TODO: Check implementation of this method
//            List<List<S>> prefixes = new ArrayList<List<S>>();
//            for (int i = 1; i <= cx.size(); i++) {
//                List<S> prefix = new ArrayList<S>();
//                for (int j = 0; j < i; j++)
//                    prefix.add(cx.get(j));
//                prefixes.add(prefix);
//            }
//
//            for (List<S> p : prefixes) {
//                if (!SUR.contains(p)) {
//                    R.add(p);
//                    SUR.add(p);
//                }
//            }
            // Find distinguishing string d
            if (cx.size() == 1) {
                this.R.add(cx);
                this.SUR.add(cx);
            }

            int diff = cx.size();
            int same = 0;
            List<S> membershipAnswer = o.checkMembership(cx);

            // Add d to W (E, columns, distinguishing strings) IF s_i0 b != s_j mod W U {d}
            // Thus do NOT add d to W IF the table becomes not closed

            // Update table accordingly

            // IF adding d to W preserves closedness,
            // add s_i0 b to R (bottom half of table)

        }

        public SFA<P, S> learn(Oracle<P, S> o, BooleanAlgebra<P, S> ba) throws TimeoutException {
            // Initialize variables: table, conjecture, cx (counterexample)
            ObsTable table = new ObsTable(ba.generateWitness(ba.True()));
            SFA<P, S> conjecture = null;
            List<S> cx = null;

            // While no equivalent hypothesis automaton has been found
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
                conjecture = buildSFA(ba);
                conjecture = conjecture.mkTotal(ba);

                // Check equivalence of hypothesis automaton and system under learning (SUL)
                cx = o.checkEquivalence(conjecture);
                if (cx == null) {
                    return conjecture;
                }

                // Process the found counterexample
                table.process(cx);
            }
        }

        private SFA<P, S> buildSFA(BooleanAlgebra<P, S> ba) {
            // TODO: Implement method
            return null;
        }

        private void fill(Oracle<P, S> o) throws TimeoutException {
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
         * @return False if the table is closed, true if the table was changed
         */
        private boolean close() {
            Set<List<Boolean>> rows = new HashSet<List<Boolean>>();
            List<S> best_r = null;

            for (List<S> s : this.S) {
                // Add all table rows corresponding to words in S
                rows.add(row(s));
            }
            for (List<S> r : this.R) {
                if (!rows.contains(row(r))) {
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
                return false;
            }

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
    }
}
