package learningalgorithm;

import org.sat4j.specs.TimeoutException;
import sftlearning.SymbolicOracle;
import theory.characters.CharFunc;
import theory.characters.CharOffset;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IOStringOracleSFT extends SymbolicOracle<CharPred, CharFunc, Character> {

    private Scanner sc;

    public IOStringOracleSFT() {
        sc = new Scanner(System.in);
    }

    @Override
    public List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) {
        System.out.println(compareTo);
        System.out.println("Is that your automaton? (y/n):");
        char in = sc.nextLine().charAt(0);
        if (in == 'y')
            return null;
        System.out.println("Enter counterexample string a1,a2,a3... :");
        String cex = sc.nextLine();
        String[] parts = cex.split(",");
        List<Character> chars = new ArrayList<>();
        for (String s : parts) {
            for (Character c : s.toCharArray()) {
                chars.add(c);
            }
        }
        return chars;
    }

    @Override
    public List<Character> checkMembershipImpl(List<Character> w) {
        System.out.println("What output does your automaton produce on " + w + " ?:");
        String in = sc.nextLine();
        List<Character> answer = new ArrayList<>();
        for (int i=0; i<in.length(); i++) {
            answer.add(in.charAt(i));
        }
        return answer;
    }

    public List<CharFunc> getTermFunctions(CharPred p) {
        List<CharFunc> termFunctions = new ArrayList<>();
        termFunctions.add(CharOffset.IDENTITY);
        return termFunctions;
    }

    public static void main(String[] args) {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        SFTLearner ell = new SFTLearner();
        SymbolicOracle o = new IOStringOracleSFT();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            learned = ell.learn(o, ba);
            learned.createDotFile("testStringOracle", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

}
