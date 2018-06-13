/**
 * SVPAlib
 * theory
 * Apr 21, 2015
 * @author Loris D'Antoni
 */
package theory;

import com.google.common.collect.ImmutableList;
import jdk.nashorn.internal.ir.annotations.Immutable;
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

	public ArrayList<P> getSeparatingPredicates(ArrayList<Collection<S>> characterGroups, long timeout) throws TimeoutException {

		//If there is just one bucket return true
		ArrayList<P> out = new ArrayList<>();
		if(characterGroups.size()<=1){
			out.add(True());
			return out;
		}

		//Find largest group
		int maxGroup = 0;
		int maxSize = characterGroups.get(0).size();
		for(int i=1;i<characterGroups.size();i++){
			int ithSize = characterGroups.get(i).size();
			if(ithSize>maxSize){
				maxSize=ithSize;
				maxGroup=i;
			}
		}

		//Build negated predicate
		P largePred = False();
		for(int i=0;i<characterGroups.size();i++){
			if(i!=maxGroup)
				for(S s: characterGroups.get(i))
					largePred = MkOr(largePred, MkAtom(s));
		}
		largePred = MkNot(largePred);

		//Build list of predicates
		for(int i=0;i<characterGroups.size();i++){
			if(i!=maxGroup){
				P ithPred = False();
				for(S s: characterGroups.get(i))
					ithPred = MkOr(ithPred, MkAtom(s));
				out.add(ithPred);
			}
			else
				out.add(largePred);
		}

		return out;
	}

	// NOTE: This function will only generate IDENTITY FUNCTIONS as term generating functions!!
	public LinkedHashMap<P, List<CharFunc>> getSeparatingPredicatesAndTermFunctions(List<Collection<S>> characterGroups, long timeout) throws TimeoutException {
		//If there is just one bucket return true
		LinkedHashMap<P, List<CharFunc>> out = new LinkedHashMap<>();
		if(characterGroups.size()<=1){
			out.put(True(), F.getIdentityFunction());

//			out.put(True(), .call(True()));
			return out;
		}


		//Find largest group
		int maxGroup = 0;
		int maxSize = 0;
		for (int i=0; i<characterGroups.size(); i++) {
			int ithSize = characterGroups.get(i).size();
			if (ithSize > maxSize) {
				maxSize = ithSize;
				maxGroup = i;
			}
		}

		//Build negated predicate
		P largePred = False();
		for(int i=0;i<characterGroups.size();i++){
			if(i!=maxGroup)
				for(S s: characterGroups.get(i))
					largePred = MkOr(largePred, MkAtom(s));
		}
		largePred = MkNot(largePred);

		//Build list of predicates
		for(int i=0;i<characterGroups.size();i++){
			if(i!=maxGroup){
				P ithPred = False();
				for(S s: characterGroups.get(i))
					ithPred = MkOr(ithPred, MkAtom(s));
				out.put(ithPred, F.getIdentityFunction());
			}
			else
				out.put(largePred, F.getIdentityFunction());
		}
		return out;
	}

	public LinkedHashMap<P, List<CharFunc>> getSeparatingPredicatesAndTermFunctions(Map<Set<S>, List<S>> predOutputGroups, BinBSFTLearner.ObsTable ot, List<S> from, long timeout) throws TimeoutException {
		// predOutputGroups is a map of evidence to output
		// The evidence will be generalized into predicates here
		// Also need to generate term functions based on evidence/output-relation
		System.out.println("Generating predicates for : "+predOutputGroups.toString());
		Set<Set<S>> characterGroups = predOutputGroups.keySet();

		//If there is just one bucket return true
		LinkedHashMap<P, List<CharFunc>> out = new LinkedHashMap<>();
		if(characterGroups.size()<=1){
			System.out.println("There is at least one piece of evidence");
			// There is only one piece of evidence thus there will only be ONE transition namely TRUE()

			Iterator<Set<S>> it = predOutputGroups.keySet().iterator();
			if (it.hasNext()) {
				Set<S> pred = it.next();
				System.out.println("Reasoning about the predicate: "+pred);
				List<S> output = predOutputGroups.get(pred);
				List<CharFunc> functions = new ArrayList<>();
				int i = 0;
				if (!pred.isEmpty()) {
					for (S s : output) {
						S p = pred.iterator().next();
						// pred should consist of only one element!
						System.out.println("Reasoning about (part of) predicate: " + p);
						if (s.equals(p)) {
							System.out.println("output char == " + p);
							// If the pred = output, then we assume that we want an identity function as term generator
							functions.addAll(F.getIdentityFunction());
						} else {
							System.out.println("output char != predicate");
							// If the pred != output, then we assume that we always want to output the constant character
							List<S> chars = new ArrayList<>();
							chars.add(output.get(i));
							functions.addAll(F.getConstantFunction(chars));
						}
					}
				} else {
					if (output.isEmpty()) {
						System.out.println("Predicate and output were both the empty list, assume identity function");
						functions.addAll(F.getIdentityFunction());
					} else {
						System.out.println("Output was not empty, generate constant function for each char in output");
						functions.addAll(F.getConstantFunction(output));
					}
				}
				out.put(True(), functions);
			} else {
				// There was no known predicate, thus we assume the identity function
				out.put(True(), F.getIdentityFunction());
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
				List<CharPred> preds = splitPredicate(ithPred, ot, fromChars);

				for (CharPred p : preds) {
					// For each predicate that has been found, add appropriate term functions.
					List<CharFunc> funcs = getTermFunctions(p, ot, fromChars);
					out.put((P) p, funcs);
				}
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
				List<CharPred> preds = splitPredicate(largePred, ot, fromChars);

				for (CharPred p : preds) {
					// For each predicate that has been found, add appropriate term functions.
					List<CharFunc> funcs = getTermFunctions(p, ot, fromChars);
					out.put((P) p, funcs);
				}
			}
		}

		return out;
	}

	private List<CharFunc> getTermFunctions(CharPred p, BinBSFTLearner.ObsTable ot, List<Character> from) {
		List<CharFunc> functions = new ArrayList<>();
		// I need some word such that its extension satisfies predicate p
		List<List<Character>> words = ot.getSUR();
		for (List<Character> word : words) {
			if (isPrefix(from, word)) {
				Character extension = word.get(word.size()-1);
				if (p.isSatisfiedBy(extension)) {
					// Then deduce the term functions from the relation between predicate p and output on word w
					Map<List<Character>, List<Character>> f = ot.getF();
					List<Character> completeOutput = f.get(word);
					List<Character> outputFrom = f.get(from);
					// output contains the value produced upon recognizing the extension
					List<Character> output = new ArrayList<>();
					for (int i=outputFrom.size(); i<completeOutput.size(); i++) {
						output.add(completeOutput.get(i));
					}
					// Establish the functions based on relation between predicate p and output
					for (Character c : output) {
						if (c.equals(extension)) {
							functions.add(CharOffset.IDENTITY);
						} else {
							functions.add(new CharConstant(c));
						}
					}
				}
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
		if (ithPred == False()) {
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
			System.out.println("Checking whether "+from+" is a prefix of "+word);
			if (isPrefix(from, word)) {
				Character extension = word.get(word.size()-1);
				// And if so, and the last character satisfies the predicate
				if (ithPred.isSatisfiedBy(extension)) {
					// Then we need to check whether the predicate needs to be split into multiple predicates
					// TODO: check whether we can look up correct value in table for (word + [])
					// Find value in table:
					Map<List<Character>, List<Character>> f = ot.getF();
					List<Character> completeOutput = f.get(word);
					List<Character> outputFrom = f.get(from);
					// output contains the value produced upon recognizing the extension
					List<Character> output = new ArrayList<>();
					for (int i=outputFrom.size(); i<completeOutput.size(); i++) {
						output.add(completeOutput.get(i));
					}

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
						// First need to check whether I even need to split this predicate
						// Predicate which we are reasoning about is ithPred
						// for all characters satisfying ithPred
						// we need to check whether they provide a different set of functions
						ImmutableList<ImmutablePair<Character, Character>> intervals = ithPred.intervals;
						for (ImmutablePair<Character, Character> subPred : intervals) {
							CharPred pr = new CharPred(subPred.getLeft(), subPred.getRight());
							// If THIS sub-predicate is satisfied by the input and if the predicate has not yet been split
							if (!isSplit && pr.isSatisfiedBy(extension)) {
								Character current = subPred.getLeft();
								Character previous = null;
								boolean sameFunctions = true;
								while (current <= subPred.getRight() && !isSplit) {
									// Check whether current character has same term functions
									int i = 0;
									for (Character c : output) {
										if ((c.equals(extension) && (!functions.get(i).equals(CharOffset.IDENTITY) || !functions.get(i).equals(new CharConstant(c)))) || (!c.equals(extension) && !functions.get(i).equals(new CharConstant(c)))) {
											// Wrong term function, thus need to split this predicate!
											sameFunctions = false;
										}
									}

									if (!sameFunctions) {
										// Need to split the predicate on this character
										// Split into [left, current-1] and [current, right]
										P before = (P) new CharPred(subPred.getLeft(), previous);
										P after = (P) new CharPred(current, subPred.getRight());
										isSplit = true;
										// The interval that needed to be split has been split into 'before' and 'after'
										// before needs to be added to the predicate with all previous intervals
										left = MkOr((P) left, before);
										// after needs to be added to the predicate with all next intervals
										right = MkOr((P) right, after);

										// Make sure that all following intervals are added to the [current, right] predicate
									} else if (current == subPred.getRight()) {
										// It has not yet been split
										// However the split may happen in the rest of the interval
										// If the term functions are the same for all chars in subPred, i.e. if this is the last character
										// Then we need to add the interval/subPred to the left predicate

										// All functions were the same therefore the predicate does not need to be split
										left = MkOr((P) left, (P) pr);
									}
									i++;
									previous = current;
									current++;
								}
							} else if (!isSplit && !pr.isSatisfiedBy(extension)) {
								// This predicate is not satisfied by the input thus it is not the predicate that should be split
								// Therefore this should be part of the predicate representing everything before the split
								left = MkOr((P) left, (P) pr);
							} else {
								// The predicate has been split, therefore all following intervals are combined
								// Therefore this sub-predicate should become part of the predicate representing everything after the split
								right = MkOr((P) left, (P) new CharPred(subPred.getLeft(), subPred.getRight()));
							}
						}
					}
				}
			}

		}

		result.add(left);
		// Call splitPredicate recursively on the predicate representing everything after the split
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
