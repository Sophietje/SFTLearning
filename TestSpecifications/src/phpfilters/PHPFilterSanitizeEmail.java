/**
 * NOTICE: This file has been made by Sophie Lathouwers!
 * @author Sophie Lathouwers
 */

package phpfilters;

import automata.sfa.SFA;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import com.google.common.collect.ImmutableList;
import org.sat4j.specs.TimeoutException;
import sftlearning.BinBSFTLearner;
import sftlearning.ReadSpecification;
import sftlearning.SymbolicOracle;
import sftlearning.TestAutomaticOracles;
import theory.characters.CharFunc;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;
import theory.characters.CharPred;

import java.util.*;

import static theory.characters.CharOffset.IDENTITY;
import static theory.characters.StdCharPred.ALPHA_NUM;

public class PHPFilterSanitizeEmail {
    static UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
    private static final String PATH = "/Users/NW/Documents/Djungarian/TestSpecifications/src/phpfilters/";

    public SFT<CharPred, CharFunc, Character> getSpecification() throws TimeoutException {
        // Initialize variables needed for specification (SFT)
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<SFTMove<CharPred, CharFunc, Character>>();
        Integer initialState = 0;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<Integer, Set<List<Character>>>();

        // Make predicate isSafeCharater(x) = x in [a-zA-Z0-9!#$%&'*+-=?^_`{|}~@.[]]
        CharPred isSpecialChar = CharPred.of(ImmutableList.of('!', '#', '$', '%', '&', '\'', '*', '+', '-', '_', '|', '=', '?', '^', '`', '{', '}', '~', '@', '.', '[', ']'));
        CharPred isSafeChar = ba.MkOr(ALPHA_NUM, isSpecialChar);

        // Make collection of transitions
        // Make first transition: p0 ------ ( isSafeChar(x) / [x] ) ------> p0
        List<CharFunc> identity = new ArrayList<CharFunc>();
        identity.add(IDENTITY);
        SFTInputMove<CharPred, CharFunc, Character> t1 =
                new SFTInputMove<CharPred, CharFunc, Character>(0, 0, isSafeChar, identity);
        transitions.add(t1);

        // Make second transition: p0 ------ ( !isSafeChar(x) / [x] ) ------> p0
        List<CharFunc> nothing = new ArrayList<CharFunc>();
        SFTInputMove<CharPred, CharFunc, Character> t2 =
                new SFTInputMove<CharPred, CharFunc, Character>(0, 0, ba.MkNot(isSafeChar), nothing);
        transitions.add(t2);

        // Make map of final states and tails (?)
        finalStatesAndTails.put(0, new HashSet<List<Character>>());


        SFT<CharPred, CharFunc, Character> spec = SFT.MkSFT(transitions, initialState, finalStatesAndTails, ba);
        return spec;
    }


    public static void main(String[] args) throws TimeoutException {
//        // Build specification
//        SFT sanitizer = new PHPFilterSanitizeEmail().getSpecification();
//
//        // Check whether input "" is accepted
//        // Check what happens to the input "829qanwsbdhauap.@di"
//        // Check what happens to the input "\abc(})"
////        System.out.println(sanitizer.accepts(toList(""), ba));
////        System.out.println(sanitizer.accepts(toList("829qanwsbdhauap.@di"), ba));
////        System.out.println(sanitizer.accepts(toList("\\{abc)}"), ba));
////        System.out.println(sanitizer.accepts(toList("!#$%&'*+-=?^_`{|}~@.[]"), ba));
////        System.out.println(sanitizer.accepts(toList("™⁄‹›ﬁﬂ‡°·‚—±”’Æ»Ú˘¯§"), ba));
//        testSanitizer(sanitizer, "829qanwsbdhauap.@di");
//        testSanitizer(sanitizer, "\\{abc)}");
//        testSanitizer(sanitizer, "!#$%&'*+-=?^_`{|}~@.[]");
//        testSanitizer(sanitizer, "™⁄‹›ﬁﬂ‡°·‚—±”’Æ»Ú˘¯§");
//        sanitizer.createDotFile("PHPFilterSanitizeEmail", PATH);
//
//
//        SFA out = null;
//        try {
//            out = sanitizer.getOutputSFA(ba);
//            out.createDotFile("PHPFilterSanitizeEmail-Output", PATH);
//
//            SFT<CharPred, CharFunc, Character> composed = sanitizer.composeWith((SFT) sanitizer.clone(), ba);
//            boolean idempotent = composed.decide1equality(sanitizer, ba);
//            System.out.println("The sanitizer is idempotent: "+idempotent);
//
//            System.out.println("-----");
//            SFA<CharPred, Character> badOutput = PHPFilterSanitizeEmail.getBadOutputSFT();
//            SFA input = sanitizer.inverseImage(badOutput, ba);
//            input.createDotFile("PHPFilterSanitizeEmailBadInputs", PATH);
//            System.out.println("Set of bad inputs is empty: "+input.isEmpty());
//        } catch (TimeoutException e) {
//            e.printStackTrace();
//        } catch (UnsupportedOperationException e) {
//            System.out.println("ERR: Could not compute output SFA due to multiple outputfunctions for one transition");
//        }
        SFT spec = new PHPFilterSanitizeEmail().getSpecification();
        spec.createDotFile("testingSFTParser", PATH);
        SFT read = ReadSpecification.read(PATH+"testingSFTParser.dot");
        read.createDotFile("testingSFTParserRead", PATH);
        System.out.println("The read and written file are equal: "+SFT.equals(spec, read));
    }

    static SFA<CharPred, Character> getBadOutputSFT() {
        Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
        transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred('e')));
        transitions.add(new SFAInputMove<CharPred, Character>(1, 2, new CharPred('v')));
        transitions.add(new SFAInputMove<CharPred, Character>(2, 3, new CharPred('i')));
        transitions.add(new SFAInputMove<CharPred, Character>(3, 4, new CharPred('l')));
        transitions.add(new SFAInputMove<CharPred, Character>(4, 5, new CharPred('!')));
        transitions.add(new SFAInputMove<CharPred, Character>(5, 6, new CharPred('@')));
        transitions.add(new SFAInputMove<CharPred, Character>(6, 7, new CharPred('c')));
        transitions.add(new SFAInputMove<CharPred, Character>(7, 8, new CharPred('o')));
        transitions.add(new SFAInputMove<CharPred, Character>(8, 9, new CharPred('r')));
        transitions.add(new SFAInputMove<CharPred, Character>(9, 10, new CharPred('p')));
        transitions.add(new SFAInputMove<CharPred, Character>(10, 11, new CharPred(',')));
        transitions.add(new SFAInputMove<CharPred, Character>(11, 12, new CharPred('n')));
        transitions.add(new SFAInputMove<CharPred, Character>(12, 13, new CharPred('e')));
        transitions.add(new SFAInputMove<CharPred, Character>(13, 14, new CharPred('t')));
        try {
            return SFA.MkSFA(transitions, 0, Arrays.asList(14), ba);
        } catch (TimeoutException e) {
            System.out.println("ERR: Timed out!");
        }
        return null;
    }

    public static List<Character> toList(String s) {
        List<Character> list = new ArrayList<Character>();
        for (char c: s.toCharArray()) {
            list.add(c);
        }
        return list;
    }

    public static void testSanitizer(SFT sanitizer, String input) {
        System.out.println("Input:  "+input);
        try {
            List<Character> output = sanitizer.outputOn(toList(input), ba);
            System.out.print("Output: ");
            for (Character c: output) {
                System.out.print(c);
            }
        } catch (org.sat4j.specs.TimeoutException e) {
            System.out.println("ERR: Timed out!");
        }
        System.out.print('\n');
        System.out.println("====================");
    }

}
