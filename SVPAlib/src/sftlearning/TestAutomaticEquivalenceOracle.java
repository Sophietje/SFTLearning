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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;

import static transducers.sft.SFT.MkSFT;
import static transducers.sft.SFT.getAccessString;

/**
 * Class that is used to test different equivalence oracle implementations
 */
public class TestAutomaticEquivalenceOracle extends SymbolicOracle<CharPred, CharFunc, Character> {

    private static UnaryCharIntervalSolver ba;
    private static SymbolicOracle o;
    private Scanner sc;
    private static int maxMinutes = 180;
    private final int minLength = 5;
    private final int maxLength = 15;
    private static int numTests = 20000;
    private static int maxTestsPerState = 100;
    private static int maxTestsPerTransition = 100;
    private static int maxTestsPerPred = 50;
    private static int EO = 0;

    public TestAutomaticEquivalenceOracle() {
        sc = new Scanner(System.in);
    }



    /**
     * Implementation for the equivalence oracle
     * Within this class you can find different methods to use as an equivalence oracle, simply call one of those or implement your own
     *
     * @param compareTo hypothesis automaton
     * @return counterexample (input upon which the hypothesis automaton and system under learning act differently)
     * @throws TimeoutException
     */
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
            default: return randomEO(compareTo);
        }
    }

    /**
     * Implementation of the membership oracle
     *
     * @param w input
     * @return output that is produced by the automaton upon input w
     */
    @Override
    protected List<Character> checkMembershipImpl(List<Character> w) {
        return escape(w);
    }

    /**
     * Equivalence Oracle that does random generated tests
     *
     * @param compareTo hypothesis automaton
     * @return counterexample
     * @throws TimeoutException
     */
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
//                System.out.println("Adding the access string: "+accString+" for state "+randomState);
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
//            System.out.println("Testing state: "+i);
            for (int j=0; j<maxTestsPerState; j++) {
//                System.out.println("Testing the state for the "+j+"-th time");
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
//                    System.out.println("Found a counterexample: "+input);
                    return input;
                }
            }
        }
        return null;
    }

    /**
     * Implementation for a sanitizers that encodes/escapes HTML by transforming <, > and & into their HTML entities.
     *
     * @param w input for sanitizers
     * @return input with encoded <, > and &
     */
    public static List<Character> encode(List<Character> w) {
        List<Character> result = new ArrayList<>();
        for (Character c : w) {
            if (c == '<') {
                result.add('&');
                result.add('l');
                result.add('t');
                result.add(';');
            } else if (c == '>') {
                result.add('&');
                result.add('g');
                result.add('t');
                result.add(';');
            } else if (c == '&') {
                result.add('&');
                result.add('a');
                result.add('m');
                result.add('p');
                result.add(';');
            } else {
                result.add(c);
            }
        }
        return result;
    }

    /**
     * Sanitizers that capitalizes a-z
     * @param w input
     * @return capitalized input
     */
    public static List<Character> capitalize(List<Character> w) {
        String input = "";
        for (Character c : w) {
            input += c;
        }
        String result = input.toUpperCase();
        List<Character> resultCharacters = new ArrayList<>(result.length());
        for (int i = 0; i<result.length(); i++) {
            resultCharacters.add(result.charAt(i));
        }
        return resultCharacters;
    }

    /**
     * Sanitizer that escapes a backslash unless it is already escaped.
     *
     * @param w input
     * @return input with escaped backslashes
     */
    public static List<Character> escape(List<Character> w) {
        String input = "";
        for (Character c : w) {
            input += c;
        }
        String result = input.replaceAll("\\\\{1,2}",  Matcher.quoteReplacement("\\\\"));
        List<Character> resultCharacters = new ArrayList<>(result.length());
        for (int i=0; i<result.length(); i++) {
            resultCharacters.add(result.charAt(i));
        }
        return resultCharacters;
    }

    public static void main(String[] args) throws TimeoutException {
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


        SFT spec = getEscapeSpec();
//        System.out.println("====================");
//        System.out.println(spec);
//        System.out.println("====================");
        long startTime = System.currentTimeMillis();
        BinBSFTLearner ell = new BinBSFTLearner();
        ba = new UnaryCharIntervalSolver();
        o = new TestAutomaticEquivalenceOracle();
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
                boolean equal = compare(learned, spec);
                System.out.println("Is it equal to the specification? " + equal);
                List<Character> witness = learned.witness1disequality(spec, ba);
                if (!equal && witness != null) {
                    System.out.println("Witness: " + witness);

                    System.out.println(learned.outputOn(witness, ba));
                    System.out.println(spec.outputOn(witness, ba));
                }
                System.out.println(spec);
                System.out.println(learned);
                List<Character> input = new ArrayList<>();
                input.add('\\');
                input.add('\\');
                System.out.println(spec.outputOn(input, ba));
                System.out.println(learned.outputOn(input, ba));
            } else {
                System.out.println("Timed out: Could not find a model in limited time frame");
            }
            //learned.createDotFile("testEscapingSlashes", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private static boolean compare(SFT<CharPred, CharFunc, Character> learned, SFT<CharPred, CharFunc, Character> spec) throws TimeoutException {
        return learned.getDomain(ba).isEquivalentTo(spec.getDomain(ba), ba) &&  learned.decide1equality(spec, ba);
    }


    public static void printCharList(List<Character> word) {
        String output = "";
        for (Character c : word) {
            output +=c;
        }
        System.out.println(output);
    }


    public static SFT<CharPred, CharFunc, Character> getEncodeSpec() throws TimeoutException {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        int numStates = 3;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<>();
        for (int i=0; i<numStates; i++) {
            finalStatesAndTails.put(i, new HashSet<>());
        }

        int initial = 0;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<>();
        List<CharPred> guards = new ArrayList<>();

        List<CharFunc> terms = new ArrayList<>();
        terms.add(CharOffset.IDENTITY);
        terms.add(new CharConstant('a'));
        terms.add(new CharConstant('m'));
        terms.add(new CharConstant('p'));
        terms.add(new CharConstant(';'));
        CharPred g = new CharPred('&', '&');
        transitions.add(new SFTInputMove<>(0, 2, g, terms));
        transitions.add(new SFTInputMove<>(1, 2, g, terms));
        transitions.add(new SFTInputMove<>(2, 2, g, terms));
        guards.add(g);

        terms = new ArrayList<>();
        terms.add(new CharConstant('&'));
        terms.add(new CharConstant('l'));
        terms.add(new CharConstant('t'));
        terms.add(new CharConstant(';'));
        g = new CharPred('<', '<');
        transitions.add(new SFTInputMove<>(0, 1, g, terms));
        transitions.add(new SFTInputMove<>(1, 1, g, terms));
        transitions.add(new SFTInputMove<>(2, 1, g, terms));
        guards.add(g);

        terms = new ArrayList<>();
        terms.add(new CharConstant('&'));
        terms.add(new CharConstant('g'));
        terms.add(new CharConstant('t'));
        terms.add(new CharConstant(';'));
        g = new CharPred('>', '>');
        transitions.add(new SFTInputMove<>(0, 1, g, terms));
        transitions.add(new SFTInputMove<>(1, 1, g, terms));
        transitions.add(new SFTInputMove<>(2, 1, g, terms));
        guards.add(g);


        CharPred largeGuard = ba.False();
        List<CharFunc> largeTerms = new ArrayList<>();
        for (CharPred gg : guards) {
            largeGuard = ba.MkOr(largeGuard, gg);
        }
        largeGuard = ba.MkNot(largeGuard);
        largeTerms.add(CharOffset.IDENTITY);
        transitions.add(new SFTInputMove<>(0, 0, largeGuard, largeTerms));
        transitions.add(new SFTInputMove<>(1, 0, largeGuard, largeTerms));
        transitions.add(new SFTInputMove<>(2, 0, largeGuard, largeTerms));

        return MkSFT(transitions, initial, finalStatesAndTails, ba);
    }

    public static SFT<CharPred, CharFunc, Character> getEscapeSpec() throws TimeoutException {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        int numStates = 2;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<>();
        for (int i=0; i<numStates; i++) {
            finalStatesAndTails.put(i, new HashSet<>());
        }

        int initial = 0;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<>();
        List<CharPred> guards = new ArrayList<>();

        List<CharFunc> terms = new ArrayList<>();
        terms.add(CharOffset.IDENTITY);
        terms.add(CharOffset.IDENTITY);
        CharPred g = new CharPred('\\', '\\');
        transitions.add(new SFTInputMove<>(0, 1, g, terms));
        guards.add(g);

        CharPred largeGuard = ba.False();
        List<CharFunc> largeTerms = new ArrayList<>();
        largeGuard = ba.MkNot(ba.MkOr(largeGuard, g));
        largeTerms.add(CharOffset.IDENTITY);
        transitions.add(new SFTInputMove<>(0, 0, largeGuard, largeTerms));
        transitions.add(new SFTInputMove<>(1, 0, largeGuard, largeTerms));

        terms = new ArrayList<>();
        transitions.add(new SFTInputMove<>(1, 0, g, terms));

        return MkSFT(transitions, initial, finalStatesAndTails, ba);
    }

}
