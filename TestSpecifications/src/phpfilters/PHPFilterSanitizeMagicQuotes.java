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
import theory.characters.CharConstant;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;

import java.util.*;

import static theory.characters.CharOffset.IDENTITY;

public class PHPFilterSanitizeMagicQuotes {
    static UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();


    private static final String PATH = "/Users/NW/Documents/Djungarian/TestSpecifications/src/phpfilters/";

    public SFT<CharPred, CharFunc, Character> getSpecification() throws TimeoutException {
        // Initialize variables needed for specification (SFT)
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<SFTMove<CharPred, CharFunc, Character>>();
        Integer initialState = 0;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<Integer, Set<List<Character>>>();

        // Make predicate needToEscape(x) = x in [', ", \, NUL]
        CharPred needToEscape = CharPred.of(ImmutableList.of('\'', '"', '\\', '\u0000'));

        // Make collection of transitions
        // Make first transition: p0 ------ ( needToEscape(x) / [\, x] ) ------> p0
        List<CharFunc> escape = new ArrayList<CharFunc>();
        escape.add(new CharConstant('\\'));
        escape.add(IDENTITY);
        SFTInputMove<CharPred, CharFunc, Character> t1 =
                new SFTInputMove<CharPred, CharFunc, Character>(0, 0, needToEscape, escape);
        transitions.add(t1);

        // Make second transition: p0 ------ ( !needToEscape(x) / [x] ) ------> p0
        List<CharFunc> identity = new ArrayList<CharFunc>();
        identity.add(IDENTITY);
        SFTInputMove<CharPred, CharFunc, Character> t2 =
                new SFTInputMove<CharPred, CharFunc, Character>(0, 0, ba.MkNot(needToEscape), identity);
        transitions.add(t2);

        // Make map of final states and tails (?)
        finalStatesAndTails.put(0, new HashSet<List<Character>>());


        SFT<CharPred, CharFunc, Character> spec = SFT.MkSFT(transitions, initialState, finalStatesAndTails, ba);
        return spec;
    }


    public static void main(String[] args) throws TimeoutException {
        // Build specification
        SFT sanitizer = new PHPFilterSanitizeMagicQuotes().getSpecification();

        // Check whether input "" is accepted
        // Check what happens to the input "829qanwsbdhauap.@di"
        // Check what happens to the input "\abc(})"
        System.out.println(sanitizer.accepts(toList(""), ba));
        System.out.println(sanitizer.accepts(toList("829qanwsbdhauap.@di"), ba));
        System.out.println(sanitizer.accepts(toList("\\{abc)}"), ba));
        System.out.println(sanitizer.accepts(toList("!#$%&'*+-=?^_`{|}~@.[]"), ba));
        System.out.println(sanitizer.accepts(toList("™⁄‹›ﬁﬂ‡°·‚—±”’Æ»Ú˘¯§"), ba));
        testSanitizer(sanitizer, "829qanwsbdhauap.@di");
        testSanitizer(sanitizer, "\\{abc)}");
        testSanitizer(sanitizer, "!#$%&'*+-=?^_`{|}~@.[]");
        testSanitizer(sanitizer, "™⁄‹›ﬁﬂ‡°·‚—±”’Æ»Ú˘¯§");
        testSanitizer(sanitizer, "\\'\"");
        testSanitizer(sanitizer, "a'b");
        sanitizer.createDotFile("PHPFilterSanitizeMagicQuotes", PATH);


        // TODO: Got a not-implemented error when executing this due to the fact that there are more than 1 transitions?
        SFA out = sanitizer.getOutputSFA(ba);
        out.createDotFile("PHPFilterSanitizeMagicQuotesOutput", PATH);

        SFT<CharPred, CharFunc, Character> composed = sanitizer.composeWith((SFT) sanitizer.clone(), ba);
        boolean idempotent = composed.decide1equality(sanitizer, ba);
        // Should not be idempotent
        System.out.println("The sanitizer is idempotent: "+idempotent);


        System.out.println("-----");
        SFA<CharPred, Character> badOutput = PHPFilterSanitizeMagicQuotes.getBadOutputSFT();
        SFA input = sanitizer.inverseImage(badOutput, ba);
        input.createDotFile("PHPFilterSanitizeMagicQuotesBadInputs", PATH);
        System.out.println("Set of bad inputs is empty: "+input.isEmpty());
    }

    private static SFA<CharPred, Character> getBadOutputSFT() {
        Collection<SFAMove<CharPred, Character>> transitions = new LinkedList<SFAMove<CharPred, Character>>();
        transitions.add(new SFAInputMove<CharPred, Character>(0, 1, new CharPred('a')));
        transitions.add(new SFAInputMove<CharPred, Character>(1, 2, new CharPred('\'')));
        transitions.add(new SFAInputMove<CharPred, Character>(2, 3, new CharPred('b')));
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
