package imaginarysanitizers;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.sat4j.specs.TimeoutException;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.intervals.BoundedIntegerSolver;
import theory.intervals.IntPred;
import theory.intervals.IntegerSolver;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;

import java.util.*;

import static theory.characters.CharOffset.IDENTITY;
import static theory.characters.StdCharPred.ALPHA_NUM;

/**
 * This class represents the sanitizer in my master thesis (Figure 4.4)
 */
public class OddEvenSanitizer {
    BoundedIntegerSolver ba = new BoundedIntegerSolver(0, 9);

//    public SFT<IntPred, Integer, Integer> getSpecification() throws TimeoutException {
//        // Initialize variables needed for specification (SFT)
//        List<SFTMove<IntPred, Integer, Integer>> transitions = new LinkedList<SFTMove<IntPred, Integer, Integer>>();
//        Integer initialState = 0;
//        Map<Integer, Set<List<Integer>>> finalStatesAndTails = new HashMap<Integer, Set<List<Integer>>>();
//
//        // Make predicate isEven => x%2==0 => x in [0, 2, 4, 6, 8]
//        IntPred zero = ba.MkAtom(0);
//        IntPred two = ba.MkAtom(2);
//        IntPred four = ba.MkAtom(4);
//        IntPred six = ba.MkAtom(6);
//        IntPred eight = ba.MkAtom(8);
//
//        ArrayList<IntPred> preds = new ArrayList<IntPred>();
//        preds.add(zero);
//        preds.add(two);
//        preds.add(four);
//        preds.add(six);
//        preds.add(eight);
//        IntPred isEven = ba.MkOr(preds);
//        // Make collection of transitions
//        // Make first transition: p0 ------ ( isEven(x) / [] ) ------> p0
//        List<CharFunc> nothing = new ArrayList<CharFunc>();
//        SFTInputMove<IntPred, Integer, Integer> t1 =
//                new SFTInputMove<IntPred, Integer, Integer>(0, 0, isEven, nothing);
//        transitions.add(t1);
//
//        // Make second transition: p0 ------ ( !isEven(x) / [x, x] ) ------> p1
//        List<Integer> identity = new ArrayList<Integer>();
//        identity.add(new Integer());
//        identity.add(x);
//        SFTInputMove<IntPred, Integer, Integer> t2 =
//                new SFTInputMove<IntPred, CharFunc, Integer>(0, 1, ba.MkNot(isEven), identity);
//        transitions.add(t2);
//
//        // Make third transition: p1 ----- (isEven(x) / [ ] ) -----> p1
//        SFTInputMove<IntPred, CharFunc, Integer> t3 = new SFTInputMove<IntPred, CharFunc, Integer>(1, 1, isEven, nothing);
//
//        // Make fourth transition: p1 ----- (!isEven(x) / [x, x]) -----> p1
//        SFTInputMove<IntPred, CharFunc, Integer> t4 = new SFTInputMove<IntPred, CharFunc, Integer>(1, 1, ba.MkNot(isEven), identity);
//
//
//        // Make map of final states and tails (?)
//        finalStatesAndTails.put(1, new HashSet<List<Integer>>());
//
//
//        SFT<CharPred, CharFunc, Integer> spec = SFT.MkSFT(transitions, initialState, finalStatesAndTails, ba);
//        return spec;
//    }

//    public static void main(String[] args) {
//        SFT even = new OddEvenSanitizer().getSpecification();
//        even.accepts(, )
//    }

}
