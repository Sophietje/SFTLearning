/**
 * This file has been made by Sophie Lathouwers
 */
package sftlearning;

import org.sat4j.specs.TimeoutException;
import specifications.CyberchefSpecifications;
import theory.characters.CharConstant;
import theory.characters.CharFunc;
import theory.characters.CharOffset;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static sftlearning.TestAutomaticEquivalenceOracle.encode;
import static sftlearning.TestAutomaticEquivalenceOracle.escape;
import static transducers.sft.SFT.MkSFT;
import static transducers.sft.SFT.getAccessString;

public class TestAutomaticOracles extends SymbolicOracle<CharPred, CharFunc, Character> {

    private Scanner sc;
    private static final UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
    private static SymbolicOracle o;
    private static String command;
    private static int EO = 0;
    private static int maxMinutes = 180;
    private final int minLength = 5;
    private final int maxLength = 15;
    private static int numTests = 20000;
    private static int maxTestsPerState = 100;
    private static int maxTestsPerTransition = 100;
    private static int maxTestsPerPred = 50;


    public TestAutomaticOracles() {
        sc = new Scanner(System.in);
    }

    public TestAutomaticOracles(String command) {
        sc = new Scanner(System.in);
        this.command = command;
    }

    /**
     * Execute user-specified command to get output from sanitizer upon input 'w'
     * @param w input
     * @return output of sanitizer
     */
    @Override
    protected List<Character> checkMembershipImpl(List<Character> w) {
        String input = "";
        for (Character c : w) {
            input += c;
        }

        String output = ExecuteCommand.executeCommandPB(command.split(" "), input);
        return stringToCharList(output);
//        return encode(w);
    }

    @Override
    protected List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        switch (EO) {
            case 1: return randomEO(compareTo);
            case 2: return randomTransitionEO(compareTo);
            case 3: return randomPrefixSelectionEO(compareTo);
            case 4: return historyBasedEO(compareTo);
            case 5: return stateCoverageEO(compareTo);
            case 6: return transitionCoverageEO(compareTo);
            case 7: return predicateCoverageEO(compareTo);
            default: return predicateCoverageEO(compareTo);
        }
    }

    public List<Character> randomEO(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        List<List<Character>> tested = new ArrayList<>();
        for (int i=0; i<numTests; i++) {
            List<Character> input = new ArrayList<>();
            boolean initial = true;
            while (initial || tested.contains(input)) {
                input = new ArrayList<>();
                for (int j = 0; j < maxLength; j++) {
                    // Choose random character to add to input
                    int random = ThreadLocalRandom.current().nextInt(1, 400);
                    Character c = CharPred.MIN_CHAR;
                    for (int k = 0; k < random; k++) {
                        c++;
                    }
                    input.add(c);
                }
                initial = false;
            }

            // Random input has been generated
            // Check whether output of hypothesis is the same as the output of the System Under Learning
            List<Character> hypothesisOutput = compareTo.outputOn(input, ba);
            List<Character> sulOutput = o.checkMembership(input);
            tested.add(input);
            if (!hypothesisOutput.equals(sulOutput)) {
                // Output was not the same thus we have found a counterexample
                return input;
            }
        }
        // No counterexample has been found!
        return null;
    }

    /**
     * Equivalence Oracle that performs random testing by choosing random transitions in the automaton.
     * It will then generate an input that satisfies that transition.
     *
     * @param compareTo hypothesis automaton
     * @return counterexample
     * @throws TimeoutException
     */
    public List<Character> randomTransitionEO(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        List<List<Character>> tested = new ArrayList<>();
        int state = compareTo.getInitialState();

        for (int i=0; i<numTests; i++) {
            // For each test we need to generate a word of length 15
            List<Character> input = new ArrayList<>();
            boolean initial = true;
            while (initial || tested.contains(input)) {
                input = new ArrayList<>();
                for (int j = 0; j < maxLength; j++) {
                    // Choose random transition
                    List<SFTMove<CharPred, CharFunc, Character>> moves = new ArrayList<>();
                    moves.addAll(compareTo.getTransitionsFrom(state));
                    int random = ThreadLocalRandom.current().nextInt(0, moves.size());
                    SFTMove chosenTrans = moves.get(random);

                    Character c = CharPred.MIN_CHAR;
                    boolean initialChar = true;
                    // Find random character that satisfies the transition
                    // NOTE: It chooses a random character from a RANGE of the actual complete algebra!
                    while (initialChar || !chosenTrans.hasModel(c, ba)) {
                        int randomChar = ThreadLocalRandom.current().nextInt(1, 400);
                        c = CharPred.MIN_CHAR;
                        for (int k = 0; k < randomChar; k++) {
                            c++;
                        }
                        initialChar = false;
                    }
                    input.add(c);
                    state = chosenTrans.to;
                }
                initial = false;
            }
            // Random input has been generated
            // Check whether output of hypothesis is the same as the output of the System Under Learning
            List<Character> hypothesisOutput = compareTo.outputOn(input, ba);
            List<Character> sulOutput = o.checkMembership(input);
            tested.add(input);
            if (!hypothesisOutput.equals(sulOutput)) {
                // Output was not the same thus we have found a counterexample
                return input;
            }
        }
        return null;
    }

    /**
     * Equivalence oracle which uses random prefix selection
     * @param hypothesis
     * @return
     * @throws TimeoutException
     */
    public List<Character> randomPrefixSelectionEO(SFT<CharPred, CharFunc, Character> hypothesis) throws TimeoutException {
        List<List<Character>> tested = new ArrayList<>();

        for (int i=0; i<numTests; i++) {
            // For each test we need to generate a word of length 15
            List<Character> input = new ArrayList<>();
            boolean initial = true;
            while (initial || tested.contains(input)) {
                input = new ArrayList<>();
                // First select a random state
                int randomState = ThreadLocalRandom.current().nextInt(0, hypothesis.stateCount());
                // Find access sequence of this state
                List<Character> accString = SFT.getAccessString(hypothesis, randomState);
                // Append the access string to the input as prefix
                input.addAll(accString);

                int randomLength = ThreadLocalRandom.current().nextInt(minLength, maxLength);
                for (int j=0; j<randomLength; j++) {
                    // Choose random character to add to input
                    int random = ThreadLocalRandom.current().nextInt(1, 400);
                    Character c = CharPred.MIN_CHAR;
                    for (int k = 0; k < random; k++) {
                        c++;
                    }
                    input.add(c);
                }
                initial = false;
            }
            // Random input has been generated
            // Check whether output of hypothesis is the same as the output of the System Under Learning
            List<Character> hypothesisOutput = hypothesis.outputOn(input, ba);
            List<Character> sulOutput = o.checkMembership(input);
            tested.add(input);
            if (!hypothesisOutput.equals(sulOutput)) {
                // Output was not the same thus we have found a counterexample
                return input;
            }
        }
        return null;
    }

    /**
     * Equivalence oracle which tests based on behaviour in previous states
     * @param hypothesis
     * @return
     * @throws TimeoutException
     */
    public List<Character> historyBasedEO(SFT<CharPred, CharFunc, Character> hypothesis) throws TimeoutException {
        List<List<Character>> tests = new ArrayList<>();

        // Test the begin state randomly
        for (int i=0; i<maxTestsPerState; i++) {
            // Test random inputs for first state
            List<Character> input = new ArrayList<>();
            for (int j=0; j<maxLength; j++) {
                int random = ThreadLocalRandom.current().nextInt(1, 400);
                Character c = CharPred.MIN_CHAR;
                for (int k = 0; k < random; k++) {
                    c++;
                }
                input.add(c);
            }
            if (!hypothesis.outputOn(input, ba).equals(o.checkMembership(input))) {
                return input;
            }
        }

        // for all other states, test x random inputs as well as all 'evidence' from previous states
        List<List<Character>> tested = new ArrayList<>();
        for (int i=1; i<hypothesis.stateCount(); i++) {
            maxTestsPerState = hypothesis.stateCount()*maxTestsPerTransition;
            // For each state, get all transitions starting in neighbouring states
            List<SFTInputMove<CharPred, CharFunc, Character>> transitions = new ArrayList<>();
            Collection<SFTInputMove<CharPred, CharFunc, Character>> neighbourTransitions = hypothesis.getInputMovesTo(i);
            for (SFTInputMove<CharPred, CharFunc, Character> trans : neighbourTransitions) {
                transitions.addAll(hypothesis.getInputMovesFrom(trans.from));
            }

            for (SFTInputMove<CharPred, CharFunc, Character> trans : transitions) {
                // Test each transition x (=maxTestsPerTransition) times
                for (int j=0; j<maxTestsPerTransition; j++) {
                    // We start in state i
                    List<Character> input = SFT.getAccessString(hypothesis, i);
                    // Generate an input with a length of y (=maxLength)
                    Character c = CharPred.MIN_CHAR;
                    // Find first character which satisfies the guard of the transition leading to the current state
                    while (c == null || !trans.hasModel(c, ba)) {
                        int randomChar = ThreadLocalRandom.current().nextInt(1, 400);
                        c = CharPred.MIN_CHAR;
                        for (int k = 0; k < randomChar; k++) {
                            c++;
                        }
                    }
                    input.add(c);
                    // Generate the rest of the test case
                    for (int k=1; k<maxLength; k++) {
                        int random = ThreadLocalRandom.current().nextInt(1, 400);
                        c = CharPred.MIN_CHAR;
                        for (int l = 0; l < random; l++) {
                            c++;
                        }
                        input.add(c);
                    }

                    // Test whether input is a counterexample
                    if (!hypothesis.outputOn(input, ba).equals(o.checkMembership(input))) {
                        return input;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Equivalent oracle that tests such that transition coverage is achieved
     * @param hypothesis
     * @return
     * @throws TimeoutException
     */
    // Equivalence oracle that achieves transition coverage in the hypothesis automaton
    public List<Character> transitionCoverageEO(SFT<CharPred, CharFunc, Character> hypothesis) throws TimeoutException {
        for (int state=0; state<hypothesis.stateCount(); state++) {
            Collection<SFTInputMove<CharPred, CharFunc, Character>> transitions = hypothesis.getInputMovesFrom(state);
            for (SFTInputMove<CharPred, CharFunc, Character> trans : transitions) {
                for (int i=0; i<maxTestsPerTransition; i++) {
                    List<Character> input = new ArrayList<>();
                    // Start in the state
                    input.addAll(getAccessString(hypothesis, state));
                    Character c = CharPred.MIN_CHAR;
                    // Generate character which satisfies the guard of the transition
                    while (c != null && !trans.hasModel(c, ba)) {
                        int random = ThreadLocalRandom.current().nextInt(1, 400);
                        c = CharPred.MIN_CHAR;
                        for (int k = 0; k<random; k++) {
                            c++;
                        }
                    }
                    input.add(c);

                    // Generate the rest of the test case
                    for (int k=0; k<maxLength; k++) {
                        int random = ThreadLocalRandom.current().nextInt(1, 400);
                        c = CharPred.MIN_CHAR;
                        for (int l=0; l<random; l++) {
                            c++;
                        }
                        input.add(c);
                    }

                    if (!hypothesis.outputOn(input, ba).equals(o.checkMembership(input))) {
                        return input;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Equivalence oracle which tests such that predicate coverage is achieved
     * @param hypothesis
     * @return
     * @throws TimeoutException
     */
    // Equivalence oracle that achieves predicate coverage in the hypothesis automaton
    public List<Character> predicateCoverageEO(SFT<CharPred, CharFunc, Character> hypothesis) throws TimeoutException {
        for (int state=0; state<hypothesis.stateCount(); state++) {
            Collection<SFTInputMove<CharPred, CharFunc, Character>> transitions = hypothesis.getInputMovesFrom(state);
            for (SFTInputMove<CharPred, CharFunc, Character> trans : transitions) {
                for (int i=0; i<trans.guard.intervals.size(); i++) {
                    for (int j=0; j<maxTestsPerPred; j++) {
                        List<Character> input = new ArrayList<>();
                        input.addAll(getAccessString(hypothesis, state));
                        Character c = CharPred.MIN_CHAR;
                        // Generate character which satisfies the guard of the transition

                        while (c != null && (trans.guard.intervals.get(i).getLeft() > c || trans.guard.intervals.get(i).getRight() < c)) {
                            int random = ThreadLocalRandom.current().nextInt(1, 400);
                            c = CharPred.MIN_CHAR;
                            for (int k = 0; k < random; k++) {
                                c++;
                            }
                        }
                        input.add(c);

                        // Generate the rest of the test case
                        for (int k = 0; k < maxLength; k++) {
                            int random = ThreadLocalRandom.current().nextInt(1, 400);
                            c = CharPred.MIN_CHAR;
                            for (int l = 0; l < random; l++) {
                                c++;
                            }
                            input.add(c);
                        }

                        if (!hypothesis.outputOn(input, ba).equals(o.checkMembership(input))) {
                            return input;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Equivalence oracle which tests such that state coverage is achieved
     * @param hypothesis
     * @return
     * @throws TimeoutException
     */
    // Equivalence oracle that achieves predicate coverage in the hypothesis automaton
    public List<Character> stateCoverageEO(SFT<CharPred, CharFunc, Character> hypothesis) throws TimeoutException {
        for (int i=0; i<hypothesis.stateCount(); i++) {
            for (int j=0; j<maxTestsPerState; j++) {
                List<Character> input = new ArrayList<>();
                input.addAll(getAccessString(hypothesis, i));
                for (int k=0; k<maxLength; k++) {
                    int random = ThreadLocalRandom.current().nextInt(1, 400);
                    Character c = CharPred.MIN_CHAR;
                    for (int l=0; l<random; l++) {
                        c++;
                    }
                    input.add(c);
                }

                if (!hypothesis.outputOn(input, ba).equals(o.checkMembership(input))) {
                    return input;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println("Which Equivalence Oracle to use?");
        System.out.println("1: Random");
        System.out.println("2: Random transition");
        System.out.println("3: Random prefix selection");
        System.out.println("4: History-based");
        System.out.println("5: State coverage");
        System.out.println("6: Transition coverage");
        System.out.println("7: Predicate coverage");
        Scanner sc = new Scanner(System.in);
        EO = sc.nextInt();
        if (EO == 1 || EO == 2 || EO == 3) {
            System.out.println("Number of tests to run?");
            numTests = sc.nextInt();
        }
        if (EO == 4 || EO == 5) {
            System.out.println("Number of tests per state?");
            maxTestsPerState = sc.nextInt();
        }
        if (EO == 4 || EO == 6) {
            System.out.println("Number of tests per transition?");
            maxTestsPerTransition = sc.nextInt();
        }
        if (EO == 7) {
            System.out.println("Number of tests per predicate?");
            maxTestsPerPred = sc.nextInt();
        }
        System.out.println("Maximum number of minutes to run?");
        maxMinutes = sc.nextInt();

        System.out.println("Command to use for membership oracle: ");
        // Read newline from previous line since nextInt doesn't read the new-line character due to which nextLine will always return the empty string the first time
        sc.nextLine();
        String command = sc.nextLine();

        SFT spec = null;

        o = new TestAutomaticOracles(command);
        long startTime = System.currentTimeMillis();
        BinBSFTLearner ell = new BinBSFTLearner();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            // Learn model
            learned = ell.learn(o, ba, maxMinutes);

            // Measure how long the learning took
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            long sec = totalTime/1000;
            long min = sec/60;
            System.out.println("Total learning time: "+min+" minutes ("+sec+" seconds)");

            // Get specfication from user
            if (learned != null) {
                if (spec != null) {
                    startTime = System.currentTimeMillis();
                    boolean equal = compare(learned, spec);
                    endTime = System.currentTimeMillis();
                    System.out.println("Is it equal to the specification? " + equal);
                    System.out.println("How long did it take to compare models? " + ((endTime - startTime)) + " milliseconds...");
                    List<Character> witness = learned.witness1disequality(spec, ba);
                    if (witness != null) {
                        System.out.println("Witness: " + witness);

                        System.out.println(learned.outputOn(witness, ba));
                        System.out.println(spec.outputOn(witness, ba));
                    }
                    System.out.println("Specified model:");
                    System.out.println(spec);
                }
                System.out.println("Learned model:");
                System.out.println(learned);
            } else {
                System.out.println("Timed out: Could not find a model in limited time frame");
                System.out.println("Currently established model: ");
                System.out.println(learned);
            }
            //learned.createDotFile("testEscapingSlashes", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private static boolean compare(SFT<CharPred, CharFunc, Character> learned, SFT<CharPred, CharFunc, Character> spec) throws TimeoutException {
        return learned.getDomain(ba).isEquivalentTo(spec.getDomain(ba), ba) &&  learned.decide1equality(spec, ba);
    }

    public static List<Character> stringToCharList(String word) {
        List<Character> output = new ArrayList<>();
        if (word == null || word.isEmpty()) {
            return output;
        }
        for (int i=0; i<word.length(); i++) {
            output.add(word.charAt(i));
        }
        return output;
    }


    /**
     * Build a specification according to the user's input
     * @return
     */
    public static SFT<CharPred, CharFunc, Character> getModelFromUser() throws TimeoutException {
        Scanner reader = new Scanner(System.in);

        // Make all (final) states
        System.out.println("Number of states?: ");
        int numStates = reader.nextInt();
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<Integer, Set<List<Character>>>();
        for (int i=0; i<numStates; i++) {
            finalStatesAndTails.put(i, new HashSet<List<Character>>());
        }
        // Set initial state
        System.out.println("Initial state?: ");
        int initial = reader.nextInt();
        // Add all transitions
        boolean moreTransitions = true;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<SFTMove<CharPred, CharFunc, Character>>();

        List<CharPred> preds = new ArrayList<>();
        while (moreTransitions) {
            System.out.println("Is there another transitions?");
            moreTransitions = reader.nextBoolean();
            if (moreTransitions) {
                // Get origin and destination of transition
                System.out.println("Origin state? ");
                int origin = reader.nextInt();
                System.out.println("Destination state? ");
                int destination = reader.nextInt();

                System.out.println("Is it the last transition? ");
                boolean last = reader.nextBoolean();

                CharPred guard;
                if (last) {
                    guard = ba.False();
                    for (CharPred p : preds) {
                        guard = ba.MkOr(guard, p);
                    }
                    guard = ba.MkNot(guard);
                } else {
                    // Make predicate for transition
                    System.out.println("Left border of guard? ");
                    String leftP = reader.next();
                    Character left = leftP.charAt(0);
                    System.out.println("Right border of guard? ");
                    String rightP = reader.next();
                    Character right = rightP.charAt(0);
                    guard = new CharPred(left, right);
                    preds.add(guard);
                }

                // Make list of terms
                List<CharFunc> terms = new ArrayList<>();
                boolean moreTerms = true;
                while (moreTerms) {
                    System.out.println("Is there another term function? ");
                    moreTerms = reader.nextBoolean();
                    if (moreTerms) {
                        System.out.println("Is it the identity function? ");
                        boolean id = reader.nextBoolean();
                        if (id) {
                            terms.add(CharOffset.IDENTITY);
                        } else {
                            System.out.println("Which constant function is it? ");
                            Character c = reader.next().charAt(0);
                            terms.add(new CharConstant(c));
                        }
                    }
                }
                SFTInputMove<CharPred, CharFunc, Character> t = new SFTInputMove<>(origin, destination, guard, terms);
                transitions.add(t);
            }
        }
        SFT<CharPred, CharFunc, Character> spec = MkSFT(transitions, initial, finalStatesAndTails, new UnaryCharIntervalSolver());

        return spec;
    }

}
