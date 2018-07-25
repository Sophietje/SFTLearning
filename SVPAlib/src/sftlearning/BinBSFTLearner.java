/**
 * NOTICE: This file has been made by Sophie Lathouwers!
 * @author Sophie Lathouwers
 */

package sftlearning;

import javafx.util.Pair;
import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebraSubst;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.characters.TermInterface;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * Method that should be called when you want to learn a SFT
     *
     * @param o symbolic oracle of the system under learning (SUL)
     * @param ba boolean algebra of the SFT
     * @return hypothesis automaton that describes the behaviour of the SUL.
     * @throws TimeoutException
     */
    public SFT<P, F, S> learn(SymbolicOracle<P, F, S> o, BooleanAlgebraSubst<P, CharFunc, S> ba, long maxMinutes) throws TimeoutException {
        long start = System.currentTimeMillis();
        long end = start + maxMinutes*60*1000; // 'maxMinutes' minutes * 60 seconds * 1000 ms/sec

        // Initialize variables: table, conjecture, cx (counterexample)
        ObsTable table = new ObsTable(ba.generateWitness(ba.True()));
        SFT<P, F, S> conjecture = null;
        List<S> cx;

        // While no equivalent hypothesis automaton has been found
        table.fill(o);
        while (true && System.currentTimeMillis() < end) {
            // Close table
            boolean closed = false;
            while (!closed) {
                if (table.close()) {
//                    System.out.println("The table was not closed!:");
//                    System.out.println(table);
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
        System.out.println("TIMED OUT AT: "+System.currentTimeMillis());
        return conjecture;
    }

    /**
     * Class that represents an Observation Table from which we can deduce an SFA
     */
    public class ObsTable {
        private List<List<S>> S, R, E, SUR;
        private Map<List<S>, Pair<List<S>, List<FunctionType>>> f;
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
            f = new HashMap<List<S>, Pair<List<S>, List<FunctionType>>>();
            this.arbchar = arbchar;

            // Add 'empty' / epsilon (S = {e})
            S.add(new ArrayList<S>());
            // Add S  = {e} to SUR so that SUR = {e}
            SUR.add(new ArrayList<S>());
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
            for (int k = w.size(); k < we.size(); k++) {
                suffix.add(we.get(k));
            }
            return suffix;
        }

        /**
         * Add input cx to the symbolic observation table
         * It will add cx to S if there is no similar row in S, otherwise it will be added to R.
         *
         * @param cx word that should be added to the table
         * @param o symbolic oracle of system under learning
         * @throws TimeoutException
         */
        public void addToTable(List<S> cx, SymbolicOracle o) throws TimeoutException {
            this.fill(o);
            // If the table does not yet contain the given input,
            // Add it to R if there is a similar row in S, otherwise add to S.
            if (!SUR.contains(cx)) {
                this.SUR.add(cx);
                this.fill(o);
                boolean similarRow = false;
                for (List<S> s : this.S) {
                    if (getFunctionRow(s).equals(getFunctionRow(cx))) {
                        similarRow = true;
                        break;
                    }
                }

                if (similarRow) {
                    this.R.add(cx);
                } else {
                    this.S.add(cx);
                }
            // If the table already contains the given input, then it must be in either S or R.
            // We need to move it to S if S does not contain a similar row.
            } else {
                if (R.contains(cx)) {
                    boolean similarRow = false;
                    for (List<S> s : this.S) {
                        if (getFunctionRow(s).equals(getFunctionRow(cx))) {
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
            // Fill out any missing entries in the table
            this.fill(o);
        }

        /**
         * Returns the longest prefix that is already in the symbolic observation table
         * @param s input from which we want the longest prefix that is in the table
         * @return the longest prefix of s that is in the symbolic observation table
         */
        public List<S> getLongestPrefixInTable(List<S> s) {
            for (int i=s.size()-1; i>=0; i--) {
                if (f.containsKey(getList(s, 0, i))) {
                    return new ArrayList<>(getList(s, 0, i));
                }
            }
            return new ArrayList<>();
        }

        /**
         * Get the output and output functions upon the input s from the System Under Learning (SUL)
         * @param s input word
         * @param o oracle of the system under learning
         * @return (output, output functions) upon given input s
         * @throws TimeoutException
         */
        public Pair<List<S>, List<FunctionType>> getOutputPair(List<S> s, SymbolicOracle<P, F, S> o) throws TimeoutException {
            List<S> answer = o.checkMembership(s);
            List<S> prefix = getLongestPrefixInTable(s);

            if (s.isEmpty() || prefix == null || f.get(prefix) == null) {
                if (!f.containsKey(s)) {
                    ArrayList<FunctionType> functionTypes = new ArrayList<>();
                    if (!answer.isEmpty()) {
                        functionTypes.add(FunctionType.CONSTANT);
                    } else {
                        functionTypes.add(FunctionType.IDENTITY);
                    }
                    Pair<List<S>, List<FunctionType>> pair = new Pair<>(answer, functionTypes);
                    f.put(s, pair);
                    addToTable(s, o);
                    return pair;
                }
            }

            if (s.size()>0 && prefix.size() < s.size()-1) {
                f.put(getList(s, 0, s.size()-1), getOutputPair(getList(s, 0, s.size()-1), o));
                prefix = getLongestPrefixInTable(s);
            }

            List<S> prefixAnswer = f.get(prefix).getKey();
            List<S> suffix = new ArrayList<>();
            if (isPrefix(prefixAnswer, answer)) {
                suffix = getSuffix(prefixAnswer, answer);
                if (suffix.isEmpty()) {
                    List<FunctionType> function = new ArrayList<>();
                    function.add(FunctionType.CONSTANT);
                    return new Pair<>(answer, function);
                }
            }

            List<FunctionType> functions = new ArrayList<>();
            for (int i=0; i<suffix.size(); i++) {
                // Make sure to look at the last character of the input if input.size() < output.size()
                int index;
                if (prefixAnswer.size()+i > s.size()-1) {
                    index = s.size()-1;
                } else {
                    index = prefixAnswer.size() + i;
                }
                if (suffix.get(i).equals(s.get(index))) {
                    functions.add(FunctionType.IDENTITY);
                } else {
                    functions.add(FunctionType.CONSTANT);
                }
            }
            return new Pair<>(answer, functions);
        }

        /**
         * Returns a new list with the elements s_begin, ..., s_end-1 from s
         * @param s list
         * @param begin begin index
         * @param end end index
         * @return list with the element s_begin, ..., s_end-1
         */
        private List<S> getList(List<S> s, int begin, int end) {
            List<S> result = new ArrayList<>();
            for (int i=begin; i<end && i<s.size(); i++) {
                result.add(s.get(i));
            }
            return result;
        }

        /**
         * Returns the answer for a given string
         *
         * @param s input string
         * @param o oracle to pose membership query to
         * @return a pair of which the first element contains the output and the second element contains the term functions
         * @throws TimeoutException
         */
        public Pair<List<S>, List<FunctionType>> getMembershipAnswer(List<S> s, SymbolicOracle<P, F, S> o) throws TimeoutException {
            // If the membership answer is already known (cached), look it up in the table (f)
            if (f.containsKey(s)) {
                return f.get(s);
            } else {
                // Otherwise, pose membership query and store results
                Pair<List<S>, List<FunctionType>> pair = getOutputPair(s, o);
                f.put(s, pair);
                return pair;
            }
        }


        /**
         * Processes a counterexample which is given in the form [a, b, c, ...]
         *
         * To do when processing a counterexample:
         * - Add all prefixes of the counterexample in S
         *
         * @param cx the counterexample that should be processed
         */
        public void process(List<S> cx, SymbolicOracle<P, F, S> o, BooleanAlgebraSubst<P, CharFunc, S> ba) throws TimeoutException {
//            System.out.println("Counterexample is: "+cx);
            if (cx.size() == 1) {
                addToTable(cx, o);
            }

            int diff = cx.size();
            int same = 0;
            List<FunctionType> membershipAnswer = getMembershipAnswer(cx, o).getValue();

            // Binary search to identify index upon which the response of the target machine
            // differs for the strings s_io z_>i0 and s_i0+1 z_>i0+1
            while((diff-same) != 1) {
                int i = (diff + same) / 2;
                List<S> accessString = runInHypothesis(o, ba, cx, i);
                List<S> toAdd = new ArrayList<>(getList(cx, i, cx.size()));
                accessString.addAll(toAdd);

                List<FunctionType> accessStringAnswer = getMembershipAnswer(accessString, o).getValue();
                if (!membershipAnswer.equals(accessStringAnswer)) {
                    diff = i;
                } else {
                    same = i;
                }
            }
//            System.out.println("i0 = "+(diff-1));
            // Construct s_i0 b
            List<S> wrongTransition = runInHypothesis(o, ba, cx, diff-1);
//            System.out.println("Si0 = "+wrongTransition);
            wrongTransition.add(cx.get(diff-1));
//            System.out.println("Si0 b = "+wrongTransition);

            // Check whether s_i0 b == s_j mod W U {d} for some j ±= i_0 + 1
            // b is a character from the input language
            // d is the counterexample
            if (!SUR.contains(wrongTransition)) {
                // If so, then add s_i0 b to ^ (R)
                addToTable(wrongTransition, o);
            } else {
                // Else, add d to W (E)
                List<S> dist = new ArrayList<>(getList(cx, diff, cx.size()));
//                System.out.println("d = "+dist);
                if (!E.contains(dist)) {
                    E.add(dist);
                }
                for (List<S> c : new ArrayList<>(S)) {
                    List<S> toAdd = new ArrayList<>();
                    toAdd.addAll(c);
                    toAdd.addAll(dist);
                    addToTable(toAdd, o);
                }
            }
            fill(o);
        }

        /**
         * Returns the output upon simulating the input cx for i steps in the hypothesis automaton
         *
         * @param o oracle which can answer membership queries
         * @param ba boolean algebra of the hypothesis automaton
         * @param cx input which will be simulated
         * @param i number of steps that input should be simulated
         * @return Output that is produced upon simulating the input cx for i steps in the hypothesis automaton
         * @throws TimeoutException
         */
        private List<S> runInHypothesis(SymbolicOracle<P, F, S> o, BooleanAlgebraSubst<P, CharFunc, S> ba, List<S> cx, int i) throws TimeoutException {
            this.fill(o);
            SFT<P, F, S> hypothesis = this.buildSFT(o, ba);

            int state = hypothesis.getInitialState();
//            System.out.println(hypothesis.toString());
            List<S> toSimulate = new ArrayList<>(getList(cx, 0, i));
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
                    // Komt door SFT die incomplete/not total is?
                    System.out.println("AAAH stuk: kon geen matchende transitie vinden");
                }
            }

            // We zijn in staat 'state' gekomen door de eerste i karakters te verwerken
            // Zoek de corresponding row in S op en return deze
            // Ik zoek de access string voor "state" -> find row(state) = row(cx.subList(0,i))?
            // Then find row equal to row(cx.subList(0,i)) in S
            // Note that individual states are now identified by their FUNCTION row instead of the output
            List<S> index = this.S.get(state);
            return new ArrayList<>(index);
        }

        private SFT<P, F, S> buildSFT(SymbolicOracle<P, F, S> o, BooleanAlgebraSubst<P, CharFunc, S> ba) throws TimeoutException {
            // Define a state for each s in S
            // Set initial state to q_epsilon
            // Make state final if T(s, epsilon) = 1
            // Make transitions, guard(q_s) should return pair (g, q)
            // which means that we add the transitions q_s --- g ---> q
            Map<List<List<FunctionType>>, Map<List<List<FunctionType>>, Set<S>>> transitions = new HashMap<>();
            for (List<S> from : S) {
                Map<List<List<FunctionType>>, Set<S>> temp = new HashMap<>();
                for (List<S> to : S) {
                    temp.put(getFunctionRow(to), new HashSet<>());
                }
                // Make "empty" transitions between all states
                transitions.put(getFunctionRow(from), temp);
            }

            for (List<S> from : SUR) {
                for (List<S> to : SUR) {
                    if (to.size() != from.size() + 1 || !isPrefix(from, to))
                        continue;
                    // If to is a one-step extension of from then we need to construct a transition from ---> to
                    // Evidence of this transition is the one-step extension
                    S evid = to.get(to.size() - 1);
                    // Add the following transition: q_from ---evid---> q_to
                    transitions.get(getFunctionRow(from)).get(getFunctionRow(to)).add(evid);
                }
            }

            //now generalize the evidence into predicates
            List<SFTMove<P, CharFunc, S>> moves = new ArrayList<>();
            for (int i = 0; i < S.size(); i++) {
                //sb is the state from which we will add transitions
                List<List<FunctionType>> sb = getFunctionRow(S.get(i));
                Map<Set<S>, Pair<List<S>, Integer>> groups = new HashMap<>();
                for (int j=0; j < S.size(); j++) {
                    // sp is the state to which we 'move'
                    List<S> sp = S.get(j);

                    List<S> outputFrom = f.get(S.get(i)).getKey();
                    List<S> outputTo = f.get(S.get(j)).getKey();

                    List<S> outputOnEvidence = getSuffix(outputFrom, outputTo);
                    // Add the following to the group: evidence(from, to), outputUpon(to), index(to)

                    // Groups should contain the following: evidence(from, to), outputUpon(evidence), index(to)
                    groups.put(transitions.get(sb).get(getFunctionRow(sp)), new Pair<>(outputOnEvidence, j));
                }
                // sepPreds is a list of Predicates which are mapped to corresponding term functions
                LinkedHashMap<P, Pair<List<CharFunc>, Integer>> sepPreds = ba.getSeparatingPredicatesAndTermFunctions(groups, this, S.get(i));

                for (P key : sepPreds.keySet()) {
                    // Cannot simply assume i will be next state because we can have multiple transitions from i to j with different predicates
                    // Add the transition i---pred_j (key) / terms_j (sepPreds.get(key)) ---> j (index)

                    if (key.intervals != null && !key.intervals.isEmpty() && key.intervals.get(0) != null) {
                        moves.add(new SFTInputMove<>(i, sepPreds.get(key).getValue(), key, sepPreds.get(key).getKey()));
                    }
                }
            }

            //build and return the SFA
            // q_0 is the initial state
            Integer init = 0;
            HashMap<Integer, Set<List<S>>> fin = new HashMap<>();
            for (int i = 0; i < S.size(); i++) {
                // Add state i to final states if f(i) = true
                // NOTE: We assume that all states are accepting
                // NOTE: In reality all rows for which the first column (extension = []) is accepting, should be an accepting state
                // NOTE: ALSO in reality many sanitizers do not reject any output, instead they then simply output the empty string
                // NOTE: And we CANNOT detect the difference between outputting an empty string or rejecting an output in this case, thus likely we can assume that sanitizers accept all inputs??
                // NOTE: This answer can be found by looking into examples of the behaviour of sanitizers
                fin.put(i, new HashSet<>());
            }
            SFT ret = SFT.MkSFT(moves, init, fin, ba);
            return ret;
        }

        /**
         * Fills missing entries in the symbolic observation table (f)
         * @param o Symbolic oracle which is used to ask membership queries when necessary
         * @throws TimeoutException
         */
        private void fill(SymbolicOracle<P, F, S> o) throws TimeoutException {
            for (List<S> w : SUR) {
                for (List<S> e : E) {
                    List<S> we = new ArrayList<>(w);
                    we.addAll(e);
                    if (!f.containsKey(we) || f.get(we) == null) {
                        Pair<List<S>, List<FunctionType>> outputPair = getOutputPair(we, o);
                        f.put(we, outputPair);
                    }
                }
            }
        }

        /**
         * Checks whether the table is closed
         * This should happen according to the FUNCTION ROWS instead of the output!
         *
         * @return False if the table is closed, true if the table was changed
         */
        private boolean close() {
            Set<List<List<FunctionType>>> rowsS = new HashSet<>();
            List<S> best_r = null;

            for (List<S> s : this.S) {
                // Construct set containing all function rows from the observation table corresponding to a word in S
                rowsS.add(getFunctionRow(s));
            }

            Set<List<List<S>>> rowsR = new HashSet<>();
            for (List<S> r : this.R) {
                // Check whether there is a function row in R that is not in S
                if (!rowsS.contains(getFunctionRow(r))) {
                    //for membership query efficiency,
                    //instead of just moving r to S, move the shortest r' with row(r) = row(r')
                    best_r = r;
                    for (List<S> rp : R) {
                        if (!getFunctionRow(r).equals(getFunctionRow(rp)))
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
            // There does not exist a one-step extension of best_r, thus add a one-step extension to the table!
            if (!cont) {
                List<S> ra = new ArrayList<>(best_r);
                ra.add(arbchar);
                R.add(ra);
                SUR.add(ra);
            }

            return true;
        }


        //--------------------------------------------
        //           GET ROWS FROM TABLE
        //--------------------------------------------

        /**
         * Returns the row, containing all output, corresponding to w in the observation table
         *
         * @param w input word
         * @return the output row corresponding to word w
         */
        public List<List<S>> getOutputRow(List<S> w) {
            return getOutputRow(w, null);
        }

        /**
         * Returns the row, containing all output, corresponding to w in the observation table
         * It ignores all characters that are in ignore
         *
         * @param w input word
         * @param ignore columns that should be ignored
         * @return the output row corresponding to word w
         */
        public List<List<S>> getOutputRow(List<S> w, List<S> ignore) {
            List<List<S>> ret = new ArrayList<>();
            for(List<S> e : E) {
                if (ignore != null && ignore.equals(e)) {
                    continue;
                }
                List<S> we = new ArrayList<>(w);
                we.addAll(e);
                ret.add(f.get(we).getKey()); //assumes f.containsKey(we)!!
            }
            return ret;
        }

        /**
         * Returns the row, containing all term/output functions, corresponding to w in the observation table
         *
         * @param w input word
         * @return the function row corresponding to word w
         */
        public List<List<FunctionType>> getFunctionRow(List<S> w) {
            return getFunctionRow(w, null);
        }

        /**
         * Returns the row, containing all term/output functions, corresponding to w in the observation table
         * It ignores all characters that are in ignore
         *
         * @param w input word
         * @param ignore columns that should be ignored
         * @return the function row corresponding to word w
         */
        public List<List<FunctionType>> getFunctionRow(List<S> w, List<S> ignore) {
            List<List<FunctionType>> ret = new ArrayList<>();
            for (List<S> e : E) {
                if (ignore != null && ignore.equals(e)) {
                    continue;
                }
                List<S> we = new ArrayList<>(w);
                we.addAll(e);
                ret.add(f.get(we).getValue()); // assumes f.get(we)!!
            }
            return ret;
        }


        //--------------------------------------------
        //                  GETTERS
        //--------------------------------------------
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

        public Map<List<S>, Pair<List<S>, List<FunctionType>>> getF() {
            return f;
        }

        //--------------------------------------------
        //                 Printing
        //--------------------------------------------
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
                        ret += f.get(we);
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
                        ret += f.get(we);
                    }
                    else ret += "  ";
                }
            }
            return ret;
        }
    }


    //--------------------------------------------
    //                 main method
    //--------------------------------------------
    public static void main(String[] args) {
//        List<String> a = new ArrayList<>(Arrays.asList("xyz", "abc"));
//        List<String> b = new ArrayList<>(Arrays.asList("xyz", "abc"));
//        List<String> c = new ArrayList<>(Arrays.asList("abc", "xyz"));
//        List<String> d = new ArrayList<>(Arrays.asList("qwe", "wer"));
//
//        System.out.println(a.equals(b));
//        System.out.println(!a.equals(c));
//        System.out.println(!a.equals(d));
//        System.out.println(a.equals(a));
//
//        System.out.println(a.size());
//        System.out.println(a.subList(0,0));
    }
}
