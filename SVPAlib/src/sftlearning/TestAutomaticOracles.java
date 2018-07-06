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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class TestAutomaticOracles extends SymbolicOracle<CharPred, CharFunc, Character> {

    private Scanner sc;
    int maxLength = 10;
    int numTests = 2000;
    private static final UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
    private static final SymbolicOracle o = new TestAutomaticOracles();
    private static final String command = "python /Users/NW/Documents/Djungarian/Sanitizers/src/escapeHTML.py";

    public TestAutomaticOracles() {
        sc = new Scanner(System.in);
    }


    @Override
    protected List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        return randomTransitionEO(compareTo);
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
        BinBSFTLearner ell = new BinBSFTLearner();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            learned = ell.learn(o, ba);
            System.out.println(learned);
//            learned.createDotFile("testEscapingSlashes", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
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

}
