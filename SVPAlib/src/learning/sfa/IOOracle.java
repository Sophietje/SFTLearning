/**
 * NOTICE: This file has been modified by Sophie Lathouwers!
 */

package learning.sfa;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.sat4j.specs.TimeoutException;

import automata.sfa.SFA;
import theory.BooleanAlgebra;
import theory.intervals.BoundedIntegerSolver;
import theory.intervals.IntPred;

public class IOOracle extends Oracle<IntPred, Integer> {
	
	private Scanner sc;
	
	public IOOracle() {
		sc = new Scanner(System.in);
	}

	@Override
	public List<Integer> checkEquivalenceImpl(SFA<IntPred, Integer> compareTo) throws TimeoutException {
		List<Integer> ret = new ArrayList<Integer>();
		System.out.println(compareTo);
		System.out.println("Is that your automaton? (y/n):");
		char in = sc.nextLine().charAt(0);
		if (in == 'y')
			return null;
		System.out.println("Enter counterexample string a1,a2,a3... :");
		String cex = sc.nextLine();
		for (String s : cex.split(","))
			ret.add(Integer.parseInt(s));
		return ret;
	}

	@Override
	public boolean checkMembershipImpl(List<Integer> w) {
		System.out.println("Does your automaton accept " + w + " ? (y/n):");
		char in = sc.nextLine().charAt(0);
		return in == 'y';
	}


	public static void main(String[] args) throws TimeoutException {
		BooleanAlgebra<IntPred, Integer> ba = new BoundedIntegerSolver(0,null);
		Learner<IntPred, Integer> ell = new Learner<IntPred, Integer>();
		Oracle<IntPred, Integer> o = new IOOracle();
		SFA<IntPred, Integer> learned = ell.learn(o, ba);
		learned.createDotFile("test.dot", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
	}

}
