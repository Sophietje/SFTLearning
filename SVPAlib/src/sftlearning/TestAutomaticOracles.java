/**
 * This file has been made by Sophie Lathouwers
 */
package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharConstant;
import theory.characters.CharFunc;
import theory.characters.CharOffset;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;
import java.util.concurrent.ThreadLocalRandom;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import java.util.*;

import static sftlearning.TestAutomaticEquivalenceOracle.encode;
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
    private static int MIN_CHAR = 1;
    private static int MAX_CHAR;
    static long timeMembership = 0;
    static long timeEquivalence = 0;
    private static int numMembershipQueries = 0;
    private static int numEquivalenceQueries = 0;


//    public TestAutomaticOracles() {
//        sc = new Scanner(System.in);
//    }

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
//        System.out.println("Entering MQ");
        numMembershipQueries++;
        long start = System.currentTimeMillis();
        String input = "";
        for (Character c : w) {
            input += c;
        }

        String output = ExecuteCommand.executeCommandPB(command.split(" "), input);
        List<Character> result = stringToCharList(output);
        long end = System.currentTimeMillis();
        timeMembership += (end - start);
        return result;
//        return encode(w);
    }

    @Override
    protected List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        numEquivalenceQueries++;
//        System.out.println(compareTo);
//        long start = System.currentTimeMillis();
//        char in = '\u0000';
//        // Ask whether the hypothesis automaton is correct
//        // This question will be repeated until valid input (either 'n' or 'y') has been given
//        while (in != 'n' && in != 'y') {
//            System.out.println("Is that your automaton? (y/n):");
//            String inLine = sc.nextLine();
//            if (inLine != null && !inLine.isEmpty() && inLine.length()==1) {
//                in = inLine.charAt(0);
//            }
//        }
//        if (in == 'y') {
//            return null;
//        }
//        System.out.println("Enter counterexample string a1,a2,a3... :");
//        String cex = sc.nextLine();
//        timeEquivalence += (System.currentTimeMillis() - start);
//        return stringToCharList(cex);
//        System.out.println("Entering EQ");
        long start = System.currentTimeMillis();
        List<Character> result;
        switch (EO) {
            case 1: result = randomEO(compareTo);
                    break;
            case 2: result = randomTransitionEO(compareTo);
                    break;
            case 3: result = randomPrefixSelectionEO(compareTo);
                    break;
            case 4: result = historyBasedEO(compareTo);
                    break;
            case 5: result = stateCoverageEO(compareTo);
                    break;
            case 6: result = transitionCoverageEO(compareTo);
                    break;
            case 7: result = predicateCoverageEO(compareTo);
                    break;
            default: result = predicateCoverageEO(compareTo);
        }
        timeEquivalence += (System.currentTimeMillis() - start);
        return result;
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
                    int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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
                        int randomChar = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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
                if (accString == null) {
                    continue;
                }
                input.addAll(accString);

                int randomLength = ThreadLocalRandom.current().nextInt(minLength, maxLength);
                for (int j=0; j<randomLength; j++) {
                    // Choose random character to add to input
                    int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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
                int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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
                    if (input == null) {
                        break;
                    }
                    Character c = CharPred.MIN_CHAR;
                    // Find first character which satisfies the guard of the transition leading to the current state
                    while (c == null || !trans.hasModel(c, ba)) {
                        int randomChar = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
                        c = CharPred.MIN_CHAR;
                        for (int k = 0; k < randomChar; k++) {
                            c++;
                        }
                    }
                    input.add(c);
                    // Generate the rest of the test case
                    for (int k=1; k<maxLength; k++) {
                        int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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
                    List<Character> accString = getAccessString(hypothesis, state);

                    Character c = CharPred.MIN_CHAR;
                    boolean first = true;
                    // Generate character which satisfies the guard of the transition
                    while (first || !trans.hasModel(c, ba)) {
                        int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
                        c = CharPred.MIN_CHAR;
                        for (int k = 0; k<random; k++) {
                            c++;
                        }
                        first = false;
                    }
                    input.add(c);

                    // Generate the rest of the test case
                    for (int k=0; k<maxLength; k++) {
                        int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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
                intervalLoop:
                for (int i=0; i<trans.guard.intervals.size(); i++) {
                    for (int j=0; j<maxTestsPerPred; j++) {
                        List<Character> input = new ArrayList<>();
                        List<Character> accString = getAccessString(hypothesis, state);
                        if (accString == null) {
                            // There are no transitions leading to this state so we skip this as it is unreachable
                            break intervalLoop;
                        }
                        input.addAll(accString);
                        Character c = 0;
                        // Generate character which satisfies the guard of the transition
                        boolean first = true;
                        while (first || (c == 0 || trans.guard.intervals.get(i).getLeft() > c || trans.guard.intervals.get(i).getRight() < c)) {
                            c = getRandomCharacter(trans.guard.intervals.get(i).right, trans.guard.intervals.get(i).left, MIN_CHAR, MAX_CHAR);
                            if (c == 0) {
                                break intervalLoop;
                            }
                            first = false;
                        }
                        input.add(c);

                        // Generate the rest of the test case
                        for (int k = 0; k < maxLength; k++) {
                            int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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
                List<Character> accString = getAccessString(hypothesis, i);
                if (accString == null) {
                    break;
                }
                input.addAll(accString);

                for (int k=0; k<maxLength; k++) {
                    int random = ThreadLocalRandom.current().nextInt(MIN_CHAR, MAX_CHAR);
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

    public static void setLearningSettings(Scanner sc) {
        System.out.println("Which Equivalence Oracle to use?");
        System.out.println("1: Random");
        System.out.println("2: Random transition");
        System.out.println("3: Random prefix selection");
        System.out.println("4: History-based");
        System.out.println("5: State coverage");
        System.out.println("6: Transition coverage");
        System.out.println("7: Predicate coverage");
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
        System.out.println("Please specify the lower bound of the alphabet in terms of an integer");
        MIN_CHAR = sc.nextInt();
        System.out.println("Please specify the upper bound of the alphabet in terms of an integer");
        MAX_CHAR = sc.nextInt();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        setLearningSettings(sc);

        System.out.println("Maximum number of minutes to run?");
        maxMinutes = sc.nextInt();

        System.out.println("Command to use for membership oracle: ");
        // Read newline from previous line since nextInt doesn't read the new-line character due to which nextLine will always return the empty string the first time
        sc.nextLine();
        String command = sc.nextLine();
        SFT spec = null;

        try {
//            SFT spec = ReadSpecification.read("/Users/NW/Documents/Djungarian/Sanitizers/escapeGoatEscape.dot");
            o = new TestAutomaticOracles(command);
            long startTime = System.currentTimeMillis();
            BinBSFTLearner ell = new BinBSFTLearner();
            // Learn model
            SFT<CharPred, CharFunc, Character> learned = ell.learn(o, ba, maxMinutes);

            // Measure how long the learning took
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            long sec = totalTime/1000;
            long min = sec/60;
            System.out.println("Total learning time: "+min+" minutes ("+sec+" seconds)");
            System.out.println("Time spent in membership oracle: "+ (timeMembership)+" milliseconds");
            System.out.println("Time spent in equivalence oracle: "+(timeEquivalence)+" milliseconds");
            System.out.println("Number of membership queries asked: "+numMembershipQueries);
            System.out.println("Number of equivalence queries asked: "+numEquivalenceQueries);

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
            learned.createDotFile("learned", "/Users/NW/Documents/Djungarian/SVPAlib/src/sftlearning/learned/");
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

    /**
     * Gets random character that is both in interval [lowerInterval, upperInterval) and [otherLowerInterval, otherUpperInterval)
     * @param lowerInterval Lower interval bound
     * @param upperInterval Upper interval bound
     * @param otherLowerInterval Lower interval bound
     * @param otherUpperInterval Upper interval bound
     * @return Character in both intervals. \u0000 if value doesn't exist
     */
    public char getRandomCharacter(int upperInterval, int lowerInterval, int otherLowerInterval, int otherUpperInterval) {
//        System.out.println("In getRandomCharacter(int, int, int, int)");
        int lower = max(lowerInterval, otherLowerInterval);
        int upper = min(upperInterval, otherUpperInterval);
//        System.out.println("Lower: "+lower);
//        System.out.println("Upper: "+upper);

        if (upper < lower) {
            return 0;
        }

        int randomValue = ThreadLocalRandom.current().nextInt(lower, upper + 1);
//        System.out.println("Random value: "+randomValue);
//        int randomValue = (int) (lower + Math.random()*(upper-lower+1));

        try {
            return Character.toChars(randomValue)[0];
        } catch (IndexOutOfBoundsException _) {
            return 0;
        }
    }

    public char getRandomCharacter(char maxChar, char minChar, int lowerInterval, int upperInterval) {
        char[] characters = new char[] {maxChar, minChar};

        int maxCharInt = Character.codePointAt(characters, 0);
        int minCharInt = Character.codePointAt(characters, 1);
//        System.out.println("max char "+maxCharInt);
//        System.out.println("min char "+minCharInt);
//        System.out.println("max char "+maxChar);
//        System.out.println("min char "+minChar);
//        System.out.println("lowerInterval "+lowerInterval);
//        System.out.println("upperInterval "+upperInterval);

        return getRandomCharacter(maxCharInt, minCharInt, lowerInterval, upperInterval);
    }

}
