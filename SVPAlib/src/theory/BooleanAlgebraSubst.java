/**
 * NOTICE: This file has been modified by Sophie Lathouwers!
 *
 * Original file:
 * SVPAlib
 * theory
 * Apr 21, 2015
 * @author Loris D'Antoni
 */
package theory;

import javafx.util.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.sat4j.specs.TimeoutException;
import sftlearning.BinBSFTLearner;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import theory.characters.*;

import java.util.*;

/**
 * BooleanAlgebraSubst: A Boolean Alegbra with substitution
 * @param <P> The type of predicates forming the Boolean algebra
 * @param <F> The type of functions S->S in the Boolean Algebra
 * @param <S> The domain of the Boolean algebra
 */
public abstract class BooleanAlgebraSubst<P extends CharPred,F extends TermInterface,S> extends BooleanAlgebra<P, S>{

	/**
	 * Replaces every variable x in the unary function <code>f1</code>
	 * the application to the function <code>f2</code> (f2(x))
	 * @return f1(f2(x))
	 */
	public abstract F MkSubstFuncFunc(F f1, F f2);

	/**
	 * Replaces every variable x in the unary function <code>f</code>
	 * with the constant <code>c</code>
	 * @return f(c)
	 */
	public abstract S MkSubstFuncConst(F f, S c);

	/**
	 * Replaces every variable x in the predicate <code>p</code>
	 * with the application to the function <code>f</code> (f(x))
	 * @return p(f(x))
	 */
	public abstract P MkSubstFuncPred(F f, P p);

	/**
	 * Make a constant function initialized by the constant <code>s</code>
	 * @return lambda x.s
	 */
	public abstract F MkFuncConst(S s);

	/**
	 * Check whether <code>f1</code> and <code>f2</code> are equivalent relative to the predicate <code>p</code>
	 * @return lambda x.(p(x) and f1(x) != f2(x))
	 */
	public abstract boolean CheckGuardedEquality(P p, F f1, F f2);


	/**
	 * get the restricted output based on <code>p</code> and <code>f</code>
	 * @return \psi(y) = \exists x. \phi(x) \wedge f(x)=y
	 */
	public abstract P getRestrictedOutput(P p, F f);


	/**
	 * This will generate predicates and corresponding term functions
	 *
	 * @param predOutputGroups contains entries in the form of (evidence, (output on given evidence, index of To state))
	 * @param ot symbolic observation table
	 * @param from state from which transitions' predicates and terms should be generated
	 * @return map containing entries in the form (predicate, (term functions, index of To state))
	 * @throws TimeoutException
	 */
	public LinkedHashMap<P, Pair<List<CharFunc>, Integer>> getSeparatingPredicatesAndTermFunctions(Map<Set<S>, Pair<List<S>, Integer>> predOutputGroups, BinBSFTLearner.ObsTable ot, List<S> from) throws TimeoutException {
		// predOutputGroups is a map of evidence to output
		// The evidence will be generalized into predicates here
		// Also need to generate term functions based on evidence/output-relation
		System.out.println(ot.toString());
		Set<Set<S>> characterGroups = predOutputGroups.keySet();


		//If there is just one bucket return true
		LinkedHashMap<P, Pair<List<CharFunc>, Integer>> out = new LinkedHashMap<>();
		if(characterGroups.size()<=1){
			// There is only one piece of evidence thus there will only be ONE transition namely TRUE()
			Iterator<Set<S>> it = predOutputGroups.keySet().iterator();
			if (it.hasNext()) {
				Set<S> pred = it.next();
				Integer to = predOutputGroups.get(pred).getValue();
				List<S> output = predOutputGroups.get(pred).getKey();
				List<CharFunc> functions = new ArrayList<>();
				int i = 0;
				if (!pred.isEmpty()) {
					for (S s : output) {
						S p = pred.iterator().next();
						// pred should consist of only one element!
						if (s.equals(p)) {
							// If the pred = output, then we assume that we want an identity function as term generator
							functions.addAll(F.getIdentityFunction());
						} else {
							// If the pred != output, then we assume that we always want to output the constant character
							List<S> chars = new ArrayList<>();
							chars.add(output.get(i));
							functions.addAll(F.getConstantFunction(chars));
						}
					}
				} else {
					if (output.isEmpty()) {
						functions.addAll(F.getIdentityFunction());
					} else {
						functions.addAll(F.getConstantFunction(output));
					}
				}
				out.put(True(), new Pair<>(functions, to));
			} else {
				// There was no known predicate, thus we assume the identity function
				out.put(True(), new Pair<>(F.getIdentityFunction(), ot.getS().indexOf(from)));
			}
			return out;
		}


		//Find largest group
		Set<S> maxGroup = new HashSet<>();
		int maxSize = 0;
		for (Set<S> pred : predOutputGroups.keySet()) {
			int ithSize = pred.size();
			if (ithSize > maxSize) {
				maxSize = ithSize;
				maxGroup = pred;
			}
		}

		// Build negated predicate
		P largePred = False();
		for (Set<S> pred : predOutputGroups.keySet()) {
			if (!pred.equals(maxGroup)) {
				for (S s : pred) {
					largePred = MkOr(largePred, MkAtom(s));
				}
			}
		}
		largePred = MkNot(largePred);

		//Build list of predicates
		for (Set<S> pred : predOutputGroups.keySet()) {
			if (!pred.equals(maxGroup)) {
				P ithPred = False();
				for (S s : pred) {
					ithPred = MkOr(ithPred, MkAtom(s));
				}

				List<Character> fromChars = new ArrayList<>();
				for (Object o : from) {
					if (o instanceof Character) {
						fromChars.add((Character) o);
					} else {
						throw new NotImplementedException();
					}
				}
				// Need to check whether output is generated in the same way for each atom in the predicate
				// If not, then the predicate needs to be split into multiple transitions with different output functions
//				List<CharPred> preds = splitPredicate(ithPred, ot, fromChars);

//				for (CharPred p : ithPred) {
					Integer to = predOutputGroups.get(pred).getValue();
					// For each predicate that has been found, add appropriate term functions.
					List<CharFunc> funcs = getTermFunctions(ithPred, ot, fromChars);
					out.put((P) ithPred, new Pair<>(funcs, to));
//				}
			} else {
				// Large predicate either needs to output identity function or one constant character
				// How to know which one you need to output?
				// Are there things known about the predicates that are in the large predicate?
				// If so, look them up in the table and use that as a basis for choosing identity/constant function
//				out.put(largePred, F.getIdentityFunction());
				List<Character> fromChars = new ArrayList<>();
				for (Object o : from) {
					if (o instanceof Character) {
						fromChars.add((Character) o);
					} else {
						throw new NotImplementedException();
					}
				}
				// Need to check whether output is generated in the same way for each atom in the predicate
				// If not, then the predicate needs to be split into multiple transitions with different output functions
				List<CharPred> preds = new ArrayList<>();
				if (largePred.intervals.size()>=1) {
					preds = splitPredicate(largePred, ot, fromChars);
				} else {
					preds.add(largePred);
				}

				for (CharPred p : preds) {
					for (CharPred pr : splitPredicate(p, ot, fromChars)) {
						Integer to = predOutputGroups.get(pred).getValue();
						// For each predicate that has been found, add appropriate term functions.
						List<CharFunc> funcs = getTermFunctions(pr, ot, fromChars);
						out.put((P) pr, new Pair<>(funcs, to));
					}
				}
			}
		}

		return out;
	}

	private List<CharFunc> getTermFunctions(CharPred p, BinBSFTLearner.ObsTable ot, List<Character> from) throws TimeoutException {
		List<CharFunc> functions = new ArrayList<>();
		// If there are two inputs in f, for which p is true AND the outputs are DIFFERENT CONSTANTS
		// Then we need to split this predicate!


		// I need some word such that its extension satisfies predicate p
		List<List<Character>> words = ot.getSUR();
		boolean foundWord = false;
//		int x = 0;
		for (List<Character> word : words) {
			if (isPrefix(from, word)) {
				Character extension = word.get(word.size()-1);
				if (p.isSatisfiedBy(extension)) {
					// Then deduce the term functions from the relation between predicate p and output on word w
					Map<List<Character>, Pair<List<Character>, List<CharFunc>>> f = ot.getF();
					List<Character> completeOutput = f.get(word).getKey();
					List<Character> outputFrom = f.get(from).getKey();
					// output contains the value produced upon recognizing the extension
					List<Character> output = new ArrayList<>();
					for (int i=outputFrom.size(); i<completeOutput.size(); i++) {
						output.add(completeOutput.get(i));
					}
//					if (x == 0) {
						// Establish the functions based on relation between predicate p and output
						for (Character c : output) {
							if (c.equals(extension)) {
								functions.add(CharOffset.IDENTITY);
							} else {
								functions.add(new CharConstant(c));
							}
							foundWord = true;

							// TODO: Look into the following: I should split the predicate if there are two words for which the output functions are both constant but with a different constant
							// TODO: In that case the predicate should be split and the term functions should be constantA and constantB respectively
						}
//						x++;
//					} else {
//						for (int i=0; i<output.size(); i++) {
//							if ((output.get(i).equals(extension) && !functions.get(i).equals(CharOffset.IDENTITY))
//								|| (!output.get(i).equals(extension) && !functions.get(i).equals(new CharConstant(output.get(i))))) {
//								// Functions are different so the predicate needs to be split!
//								List<CharPred> preds = splitPredicate(p, ot, from);
//								for (CharPred p : preds) {
//
//								}
//							}
//						}
//
//					}
				}
			}
			if (foundWord) {
				return functions;
			}
		}
		return functions;
	}

	/**
	 * Given a predicate, it checks whether this predicate should be split into multiple predicates
	 * It returns a list of predicates which are the predicates for which we need to generate terms
	 * @param ithPred predicate which might be split
	 * @param ot observation table
	 * @param from list of characters representing the state from which the transition with predicate ithPred leaves
	 * @return list of predicates
	 */
	private List<CharPred> splitPredicate(CharPred ithPred, BinBSFTLearner.ObsTable ot, List<Character> from) throws TimeoutException {
		if (ithPred.equals(False())) {
			return new ArrayList<>();
		}

		List<List<Character>> words = ot.getSUR();
		List<CharPred> result = new ArrayList<>();
		CharPred left = False();
		CharPred right = False();
		boolean isSplit = false;
		// For all words in the table's rows
		int index = 0;
		List<CharFunc> functions = new ArrayList<>();
		for (List<Character> word : words) {
			// Check whether the word is a one-step extension of the 'from'/origin state
			if (isPrefix(from, word) && !isSplit) {
				Character extension = word.get(word.size()-1);
				// And if so, and the last character satisfies the predicate
				if (ithPred.isSatisfiedBy(extension) && !isSplit) {
					// Then we need to check whether the predicate needs to be split into multiple predicates
					// TODO: check whether we can look up correct value in table for (word + [])
					// Find value in table:
					Map<List<Character>, Pair<List<Character>, List<CharFunc>>> f = ot.getF();
					List<Character> output = f.get(word).getKey();

					if (index == 0) {
						// Cannot split before first character
						// Establish the functions that we expect the rest of the transitions to also output
						for (Character c : output) {
							if (c.equals(extension)) {
								functions.add(CharOffset.IDENTITY);
							} else {
								functions.add(new CharConstant(c));
							}
						}
						index++;
					} else {
						boolean sameFunctions = true;
						int i=0;
						for (Character c : output) {
							if ((c.equals(extension) && !functions.get(i).equals(CharOffset.IDENTITY))
									|| (!c.equals(extension) && !functions.get(i).equals(new CharConstant(c)))) {
								sameFunctions = false;
							}
							i++;
						}
						for (ImmutablePair<Character, Character> subPred : ithPred.intervals) {
							CharPred pr = new CharPred(subPred.getLeft(), subPred.getRight());
							// Need to find the specific interval in the predicate that needs to be split
							if (pr.isSatisfiedBy(extension) && !isSplit) {
								Character current = extension;
								Character previous = current;
								previous--;
								if (current > subPred.getLeft()) {
									if (!sameFunctions) {
										// Need to split the predicate on this character (extension)
										// Split into [left, current] and [current+1, right]
										P before = (P) new CharPred(subPred.getLeft(), previous);
										P after = (P) new CharPred(current, subPred.getRight());
										isSplit = true;
										// The interval that needed to be split has been split into 'before' and 'after'
										// before needs to be added to the predicate with all previous intervals
										left = MkOr((P) left, before);
										// after needs to be added to the predicate with all next intervals
										right = MkOr((P) right, after);

										// Make sure that all following intervals are added to the [current, right] predicate
									} else {
										// It has not yet been split
										// However the split may happen in the rest of the interval
										// If the term functions are the same for all chars in subPred, i.e. if this is the last character
										// Then we need to add the interval/subPred to the left predicate

										// All functions were the same therefore the predicate does not need to be split
										left = MkOr((P) left, (P) pr);
									}
								} else if (current.equals(subPred.getLeft())) {
									left = MkOr((P) left, (P) new CharPred(subPred.getLeft(), subPred.getLeft()));
									Character split = subPred.getLeft();
									split++;
									right = MkOr((P) right, (P) new CharPred(split, subPred.getRight()));
								} else {
									System.out.println("ERROR: current < subPred.left!");
								}
							} else if (!pr.isSatisfiedBy(extension) && !isSplit) {
								left = MkOr((P) left, (P) pr);
							} else if (isSplit) {
								right = MkOr((P) right, (P) pr);
							}
						}
					}
				}
			}
			if (isSplit) {
				break;
			}
		}

		if (left.equals(False()) && right.equals(False())) {
			result.add(ithPred);
			return result;
		}

		if (left.equals(ithPred)) {
			// If the left predicate is equal to the original predicate, then it did not need to be split!
			result.add(left);
			return result;
		}
		// Call splitPredicate recursively on the predicate representing everything after the split
		List<CharPred> predsLeft = splitPredicate(left, ot, from);
		for (CharPred p : predsLeft) {
			result.add(p);
		}
		List<CharPred> preds = splitPredicate(right, ot, from);
		for (CharPred p : preds) {
			result.add(p);
		}
		return result;
	}

	/**
	 * Checks whether word is a one-step extension of from
	 *
	 * ASSUMPTION: epsilon ([]) is not a one-step extension
	 * @param from original word
	 * @param word possible one-step extension
	 * @return true if word is a one-step extension of from
	 */
	private <S> boolean isPrefix(List<S> from, List<S> word) {
		// If word is one character longer than from
		if (!word.equals(from) && word.size() == from.size() + 1) {
			// Then check whether from is prefix of word
			boolean isEqual = true;
			for (int i=0; i<from.size(); i++) {
				if (word.get(i) != from.get(i)) {
					isEqual = false;
					break;
				}
			}
			return isEqual;
		}
		return false;
	}


	public static void main(String[] args) {
//		List<Character> from = new ArrayList<>();
//		from.add('a');
//		from.add('b');
//		from.add('c');
//		List<Character> word = new ArrayList<>();
//		word.add('a');
//		word.add('b');
//		// Check whether "ab" is one-step extension of "abc" (false)
//		System.out.println(isPrefix(from, word));
//		word.add('c');
//		// Check whether "abc" is one-step extension of "abc" (false)
//		System.out.println(isPrefix(from, word));
//		word.add('d');
//		// Check whether "abcd" is one-step extension of "abc" (true)
//		System.out.println(isPrefix(from, word));
//		// Check whether "[]" is one-step extension of "[]" (false)
//		System.out.println(isPrefix(new ArrayList<>(), new ArrayList<>()));
//		// Check whether "abc" is one-step extension of "[]" (false)
//		System.out.println(isPrefix(from, new ArrayList<>()));
//		from.clear();
//		// Check whether "[]" is one-step extension of "abcd" (false)
//		System.out.println(isPrefix(from, word));

		Map<Set<Character>, List<Character>> map = new HashMap<>();
		Set<Character> set = new HashSet<>();
		map.put(set, new ArrayList<>());
		List<Character> lst1 = new ArrayList<>();
//		List<Character> lst2 = new ArrayList<>();
//		System.out.println(lst1.equals(lst2));
//		System.out.println(lst1.equals(map.get(new HashSet<>())));
//		System.out.println(map.get(new HashSet<>()).equals(lst1));

		System.out.println(set.equals(lst1));
		Set<Set<Character>> keySet = map.keySet();
		for (Set<Character> key : keySet) {
			// Key is Set of characters
			// Value of map is List of characters
			System.out.println(key.equals(map.get(key)));
		}

	}

}
