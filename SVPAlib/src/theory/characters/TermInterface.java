package theory.characters;

import theory.characters.CharFunc;
import theory.intervals.UnaryCharIntervalSolver;

import java.util.ArrayList;
import java.util.List;

public interface TermInterface {

    static List<CharFunc> getIdentityFunction() {
        List<CharFunc> termFunctions = new ArrayList<>();
        termFunctions.add(CharOffset.IDENTITY);
        return termFunctions;
    }

    static <S> List<CharFunc> getConstantFunction(List<S> output) {
        List<CharConstant> termFunctions = new ArrayList<>();
        List<CharFunc> terms = new ArrayList<>();
        System.out.println("Generating constant function for the output: "+output);

        if (output != null && !output.isEmpty()) {
            for (S c : output) {
                if (c instanceof Character) {
                    char o = (Character) c;
                    termFunctions.add(new CharConstant(o));
                    break;
                } else {
                    // Currently not used so should not occur
                    System.out.println("ERROR: Output not of type Character");
                }
            }
        } else {
            System.out.println("Output was null");
            // If output == null then it is [] ?
            // If so then we should add some constant representing the empty output...
            // TODO: What to use for constant representing the empty output?
            termFunctions.add(new CharConstant('C'));
        }

        for (CharConstant func : termFunctions) {
            terms.add((CharFunc) func);
        }

        return terms;
    }
}
