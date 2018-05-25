package learningalgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import learning.sfa.Learner;
import learning.sfa.Oracle;
import org.sat4j.specs.TimeoutException;

import automata.sfa.SFA;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class IOStringOracle2 extends Oracle<CharPred, Character> {

    private Scanner sc;

    public IOStringOracle2() {
        sc = new Scanner(System.in);
    }

    @Override
    public List<Character> checkEquivalenceImpl(SFA<CharPred, Character> compareTo) {
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
    public boolean checkMembershipImpl(List<Character> w) {
        System.out.println("Does your automaton accept " + w + " ? (y/n):");
        char in = sc.nextLine().charAt(0);
        return in == 'y';
    }

    public static void main(String[] args) {
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        BinBLearner<CharPred, Character> ell = new BinBLearner<>();
        Oracle o = new IOStringOracle2();
        SFA<CharPred, Character> learned = null;
        try {
            learned = ell.learn(o, ba);
            learned.createDotFile("testStringOracle", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

    }

}
