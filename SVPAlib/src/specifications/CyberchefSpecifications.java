/**
 * This file has been made by Sophie Lathouwers
 */
package specifications;

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

import static transducers.sft.SFT.MkSFT;

public class CyberchefSpecifications {

    public static SFT<CharPred, CharFunc, Character> getLowercaseSpec() throws TimeoutException {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        int numStates = 1;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<>();
        for (int i=0; i<numStates; i++) {
            finalStatesAndTails.put(i, new HashSet<>());
        }

        int initial = 0;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<>();

        Character[] uppercase = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

        List<CharPred> guards = new ArrayList<>();
        for (Character c : uppercase) {
            List<CharFunc> terms = new ArrayList<>();
            terms.add(new CharConstant(Character.toLowerCase(c)));
            CharPred g = new CharPred(c, c);
            guards.add(g);
            transitions.add(new SFTInputMove<>(0, 0, g, terms));
        }

        CharPred largeGuard = ba.False();
        List<CharFunc> largeTerms = new ArrayList<>();
        for (CharPred g : guards) {
            largeGuard = ba.MkOr(largeGuard, g);
        }
        largeGuard = ba.MkNot(largeGuard);
        largeTerms.add(CharOffset.IDENTITY);
        transitions.add(new SFTInputMove<>(0, 0, largeGuard, largeTerms));

        return MkSFT(transitions, initial, finalStatesAndTails, ba);
    }

    public static SFT<CharPred, CharFunc, Character> getRemoveNullBytesSpec() throws TimeoutException {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        int numStates = 1;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<>();
        for (int i=0; i<numStates; i++) {
            finalStatesAndTails.put(i, new HashSet<>());
        }

        int initial = 0;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<>();

        List<CharFunc> terms = new ArrayList<>();
        CharPred g = new CharPred('\0', '\0');
        transitions.add(new SFTInputMove<>(0, 9, g, terms));

        CharPred largeGuard = ba.False();
        List<CharFunc> largeTerms = new ArrayList<>();
        largeGuard = ba.MkNot(ba.MkOr(largeGuard, g));
        largeTerms.add(CharOffset.IDENTITY);
        transitions.add(new SFTInputMove<>(0, 0, largeGuard, largeTerms));

        return MkSFT(transitions, initial, finalStatesAndTails, ba);
    }

    public static SFT<CharPred, CharFunc, Character> getRemoveWhitespaceSpec() throws TimeoutException {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        int numStates = 1;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<>();
        for (int i=0; i<numStates; i++) {
            finalStatesAndTails.put(i, new HashSet<>());
        }

        int initial = 0;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<>();

        Character[] characters = {' ', '\r', '\n', '\t', '\f', '.'};

        List<CharPred> guards = new ArrayList<>();
        for (Character c : characters) {
            List<CharFunc> terms = new ArrayList<>();
            terms.add(new CharConstant(Character.toLowerCase(c)));
            CharPred g = new CharPred(c, c);
            guards.add(g);
            transitions.add(new SFTInputMove<>(0, 0, g, terms));
        }

        CharPred largeGuard = ba.False();
        List<CharFunc> largeTerms = new ArrayList<>();
        for (CharPred g : guards) {
            largeGuard = ba.MkOr(largeGuard, g);
        }
        largeGuard = ba.MkNot(largeGuard);
        largeTerms.add(CharOffset.IDENTITY);
        transitions.add(new SFTInputMove<>(0, 0, largeGuard, largeTerms));

        return MkSFT(transitions, initial, finalStatesAndTails, ba);
    }

    public static void main(String[] args) {
        try {
            System.out.println(getRemoveWhitespaceSpec());
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static SFT getAtoB() throws TimeoutException {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        int numStates = 1;
        Map<Integer, Set<List<Character>>> finalStatesAndTails = new HashMap<>();
        for (int i=0; i<numStates; i++) {
            finalStatesAndTails.put(i, new HashSet<>());
        }

        int initial = 0;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new LinkedList<>();
        List<CharPred> guards = new ArrayList<>();
        List<CharFunc> terms = new ArrayList<>();
        terms.add(new CharConstant('b'));
        CharPred g1 = new CharPred('a', 'a');
        guards.add(g1);
        transitions.add(new SFTInputMove<>(0, 0, g1, terms));
        List<CharFunc> terms2 = new ArrayList<>();
        terms2.add(new CharConstant('a'));
        CharPred g2 = new CharPred('b', 'b');
        guards.add(g2);
        transitions.add(new SFTInputMove<>(0, 0, g2, terms2));

        CharPred largeGuard = ba.False();
        List<CharFunc> largeTerms = new ArrayList<>();
        largeGuard = ba.MkOr(largeGuard, g1);
        largeGuard = ba.MkOr(largeGuard, g2);
        largeGuard = ba.MkNot(largeGuard);
        largeTerms.add(CharOffset.IDENTITY);
        transitions.add(new SFTInputMove<>(0, 0, largeGuard, largeTerms));

        return MkSFT(transitions, initial, finalStatesAndTails, ba);
    }
}
