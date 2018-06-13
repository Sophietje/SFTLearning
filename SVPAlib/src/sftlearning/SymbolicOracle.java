package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharPred;
import theory.characters.TermInterface;
import transducers.sft.SFT;

import java.util.List;

public abstract class SymbolicOracle<P extends CharPred, F extends TermInterface, S> {

    private int numEquivalence = 0;
    private int numMembership = 0;

    protected abstract List<S> checkEquivalenceImpl(SFT<P, F, S> compareTo) throws TimeoutException;

    protected abstract List<S> checkMembershipImpl(List<S> w) throws TimeoutException;

    /**
     *
     * @param compareTo The guessed SFA
     * @return null if equivalent, else a minimal-length list of characters that distinguishes the automata
     * @throws TimeoutException
     */
    public final List<S> checkEquivalence(SFT<P, F, S> compareTo) throws TimeoutException {
        numEquivalence++;
        return checkEquivalenceImpl(compareTo);
    }

    public final List<S> checkMembership(List<S> w) throws TimeoutException {
        numMembership++;
        return checkMembershipImpl(w);
    }

    public int getNumEquivalence() {
        return numEquivalence;
    }

    public int getNumMembership() {
        return numMembership;
    }
}
