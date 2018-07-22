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

import static transducers.sft.SFT.MkSFT;

public class TestAutomaticOracles extends SymbolicOracle<CharPred, CharFunc, Character> {

    private Scanner sc;
    int maxLength = 10;
    int numTests = 2000;
    private static final UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
    private static final SymbolicOracle o = new TestAutomaticOracles();
    private static final String command = "node --no-deprecation ./Sanitizers/encode/cyberChefRemoveWhitespace.js";

    public TestAutomaticOracles() {
        sc = new Scanner(System.in);
    }


    @Override
    protected List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        return randomEO(compareTo);
    }

    @Override
    protected List<Character> checkMembershipImpl(List<Character> w) {
        String input = "";
        for (Character c : w) {
            input += c;
        }

        String output = ExecuteCommand.executeCommandPB(command.split(" "), input);
        return stringToCharList(output);
    }

    public static void main(String[] args) {
        SFT spec = CyberchefSpecifications.getRemoveWhitespaceSpec();
        System.out.println(spec);
        System.out.println("====================");
        long startTime = System.currentTimeMillis();
        BinBSFTLearner ell = new BinBSFTLearner();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            // Learn model
            learned = ell.learn(o, ba, 180);
            System.out.println(learned);

            // Measure how long the learning took
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            long sec = totalTime/1000;
            long min = sec/60;
            System.out.println("Total execution time: "+min+" minutes");

            // Get specfication from user
            if (learned != null) {
                boolean equal = compare(learned, spec);
                System.out.println("Is it equal to the specification? " + equal);
                if (!equal) {
                    List<Character> witness = learned.witness1disequality(spec, ba);
                    System.out.println("Witness: " + witness);

                    System.out.println(learned.outputOn(witness, ba));
                    System.out.println(spec.outputOn(witness, ba));
                    System.out.println(spec);
                }
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

                    Character c = null;
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

    public List<Character> randomEO(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        int maxLength = 15;
        int numTests = 1000;
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

            System.out.println("Input #"+i+": "+input);
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
     * Build a specification according to the user's input
     * @return
     */
    public static SFT<CharPred, CharFunc, Character> getModelFromUser() {
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
