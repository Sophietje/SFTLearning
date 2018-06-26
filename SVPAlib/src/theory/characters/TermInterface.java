package theory.characters;

import java.util.ArrayList;
import java.util.List;

/**
 * Written by Sophie Lathouwers
 */
public interface TermInterface {

    static List<CharFunc> getIdentityFunction() {
        List<CharFunc> termFunctions = new ArrayList<>();
        termFunctions.add(CharOffset.IDENTITY);
        return termFunctions;
    }

    static <S> List<CharFunc> getConstantFunction(List<S> output) {
        if (output.size()==1) {
            List<CharFunc> terms = new ArrayList<>();
            terms.add(new CharConstant((Character) output.get(0)));
            return terms;

        } else {
            List<CharConstant> termFunctions = new ArrayList<>();
            List<CharFunc> terms = new ArrayList<>();

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
}
