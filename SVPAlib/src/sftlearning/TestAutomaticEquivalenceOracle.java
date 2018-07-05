/**
 * This file has been made by Sophie Lathouwers
 */
package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTMove;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;

/**
 * Class that is used to test different equivalence oracle implementations
 */
public class TestAutomaticEquivalenceOracle extends SymbolicOracle<CharPred, CharFunc, Character> {

    private static UnaryCharIntervalSolver ba;
    private static SymbolicOracle o;
    private Scanner sc;

    public TestAutomaticEquivalenceOracle() {
        sc = new Scanner(System.in);
    }



    /**
     * Equivalence Oracle that does random generated tests
     *
     * @param compareTo hypothesis automaton
     * @return counterexample
     * @throws TimeoutException
     */
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
                    int random = ThreadLocalRandom.current().nextInt(20, 370);
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
     * Equivalence Oracle that performs random testing by choosing random transitions in the automaton.
     * It will then generate an input that satisfies that transition.
     *
     * @param compareTo hypothesis automaton
     * @return counterexample
     * @throws TimeoutException
     */
    public List<Character> randomTransitionEO(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        int maxLength = 15;
        int numTests = 2000;
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
                        int randomChar = ThreadLocalRandom.current().nextInt(20, 370);
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
     * Implementation for the equivalence oracle
     * Within this class you can find different methods to use as an equivalence oracle, simply call one of those or implement your own
     *
     * @param compareTo hypothesis automaton
     * @return counterexample (input upon which the hypothesis automaton and system under learning act differently)
     * @throws TimeoutException
     */
    @Override
    protected List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        return randomTransitionEO(compareTo);
    }

    /**
     * Implementation of the membership oracle
     *
     * @param w input
     * @return output that is produced by the automaton upon input w
     */
    @Override
    protected List<Character> checkMembershipImpl(List<Character> w) {
        // TODO: Use exec() call to call appropriate command to execute Python/Ruby/PHP/etc.
        return escape(w);
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
     * Sanitizers that transforms < into &lt;
     * @param w input
     * @return encoded input
     */
    public static List<Character> encodeLT(List<Character> w) {
        List<Character> result = new ArrayList<>();
        for (Character c : w) {
            if (c == '<') {
                result.add('&');
                result.add('l');
                result.add('t');
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

    public static void main(String[] args) {
//        List<Character> word = new ArrayList<>();
//        printCharList(escape(word));
//        word.add('b');
//        printCharList(escape(word));
//        word.add('\\');
//        printCharList(escape(word));
//        word.add('\\');
//        printCharList(escape(word));
//        word.add('b');
//        printCharList(escape(word));

        ba = new UnaryCharIntervalSolver();
        BinBSFTLearner ell = new BinBSFTLearner();
        o = new TestAutomaticEquivalenceOracle();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            learned = ell.learn(o, ba);
            System.out.println(learned);
//            learned.createDotFile("testEscapingSlashes", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void printCharList(List<Character> word) {
        String output = "";
        for (Character c : word) {
            output +=c;
        }
        System.out.println(output);
    }

}
