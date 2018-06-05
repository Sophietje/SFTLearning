package theory.characters;

import theory.characters.CharFunc;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.ArrayList;
import java.util.List;

public interface TermInterface {

    public static <P> List<CharFunc> getIdentityFunction(P preds) {
        List<CharFunc> termFunctions = new ArrayList<>();
        termFunctions.add(CharOffset.IDENTITY);
        return termFunctions;
    }

    public static <P> List<CharFunc> getConstantFunction(P preds) {
        List<CharConstant> termFunctions = new ArrayList<>();
        List<CharFunc> terms = new ArrayList<>();

        termFunctions.add(new CharConstant('a'));

        for (CharConstant c : termFunctions) {
            terms.add((CharFunc) c);
        }
        return terms;
    }
}
