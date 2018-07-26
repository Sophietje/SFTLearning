package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharConstant;
import theory.characters.CharFunc;
import theory.characters.CharOffset;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;
import transducers.sft.SFTInputMove;
import transducers.sft.SFTMove;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class ReadSpecification {

    public static SFT<CharPred, CharFunc, Character> read(String filePath) throws TimeoutException {
        Map<Integer, Boolean> states = new HashMap<>();

        Scanner sc = null;
        try {
            sc = new Scanner(new File(filePath));
        } catch (FileNotFoundException e) {
            System.out.println("Could not find the specified file. Exiting...");
            System.exit(0);
        }
        // First line contains name which is not important
        String line = null;
        List<SFTMove<CharPred, CharFunc, Character>> transitions = new ArrayList<>();
        int initialState = -1;
        while (sc.hasNextLine() && (line = sc.nextLine()) != null) {
            if (line.matches("[0-9]+\\[.*\\]")) {
                // This line contains a state declaration
                List<Character> num = new ArrayList<>();
                int i = 0;
                while (i<line.length() && Character.isDigit(line.charAt(i))) {
                    num.add(line.charAt(i));
                    i++;
                }
                String state = num.toString().replace("[", "").replace("]", "");
                // Add to set of states

                String other = line.substring(i);
                String label = "";
                boolean isFinal = false;
                String[] attributes = other.split(",");
                for (String attr : attributes) {
                    if (attr.startsWith("label")) {
                        label = attr.split("=")[1];
                    } else if (attr.startsWith("peripheries")) {
                        int numPeripheries = Integer.valueOf(attr.split("=")[1].replace("]", ""));
                        if (numPeripheries == 2) {
                            isFinal = true;
                        }
                    }
                }
                states.put(Integer.valueOf(state), isFinal);
            } else if (line.matches("[0-9]+ -> [0-9]+ \\[.*\\]")) {
                // This line contains a transition declaration
                // Parse from state
                int i = 0;
                String from = "";
                String to = "";

                // Get chars representing from state
                while (i<line.length() && Character.isDigit(line.charAt(i))) {
                    from += line.charAt(i);
                    i++;
                }

                // Skip all characters (which should be " -> " that are not digits
                while (i<line.length() && !Character.isDigit(line.charAt(i))) {
                    i++;
                }

                // Get chars representing to state
                while (i<line.length() && Character.isDigit(line.charAt(i))) {
                    to += line.charAt(i);
                    i++;
                }

                // Read guard and term functions for transition
                String attributes = line.substring(i);
                attributes = attributes.trim();
                if (attributes.startsWith("[label=\"[")) {
                    attributes = attributes.substring("[label=\"[".length());
                }
                if (attributes.endsWith("\"]")) {
                    attributes = attributes.substring(0, attributes.length()-"\"]".length());
                }
                System.out.println("Current attributes value: "+attributes);
                // Should now be left with [...]/...
                String[] label = attributes.split("\\]/");
                for (String s : label) {
                    System.out.println("Label has part: "+s);
                }

                String guard = label[0];
                String term = null;
                System.out.println("Guard: "+guard);
                if (label.length > 1) {
                    term = label[1];
                    System.out.println("Term: "+term);
                }

                CharPred guards = parseGuard(guard);
                List<CharFunc> terms = parseTerms(term);

                transitions.add(new SFTInputMove<>(Integer.valueOf(from), Integer.valueOf(to), guards, terms));
            } else if (line.matches("XX[0-9]+ [.*]XX[0-9]+ -> [0-9]+")) {
                // This line reveals the initial state
                String[] lineParts = line.split(" ");
                String number = lineParts[lineParts.length-1];
                initialState = Integer.valueOf(number);
            }
        }
        Map<Integer, Set<List<Character>>> finalStates = new HashMap<>();
        for (Integer state : states.keySet()) {
            if (states.get(state)) {
                finalStates.put(state, new HashSet<>());
            }
        }
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        return SFT.MkSFT(transitions, initialState, finalStates, ba);
    }

    /**
     * Parses a string that represents the term functions
     * Note that it ASSUMES that all separate functions are separated by a space
     *
     * @param term
     * @return
     */
    private static List<CharFunc> parseTerms(String term) {
        // The string that is provided should look like "x + 0 a b c x x+0"
        List<CharFunc> terms = new ArrayList<>();
        List<Character> parts = new ArrayList<>();
        if (term != null && !term.isEmpty()) {
            for (char c : term.toCharArray()) {
                parts.add(c);
            }
            int i = 0;
            while (i < parts.size()) {
                System.out.println("parts.get(i): "+parts.get(i));
                System.out.println("i = "+i);
                System.out.println("parts' size = "+parts.size());
                int size = parts.size() - 5;
                System.out.println("parts.size() - 5 = "+size);
                if (i < (parts.size() - 4) && parts.get(i).equals('x') && parts.get(i+2).equals('+') && parts.get(i+4).equals('0')) {
                    terms.add(CharOffset.IDENTITY);
                    i += 5; // Skip converted chars
                } else {
                    terms.add(new CharConstant(parts.get(i)));
                    i++; // Skip converted char
                }
                i++; // Skip space after an output term
            }
        }
        System.out.println("Parsed terms into: "+terms);
        return terms;
    }

    /**
     * Parses a string that represents the guard of a transition
     * It works as follows:
     * - takes first character, if it is followed by - and a second character, then the range (first, second) is added
     * - characters may be esaped with a \ thus when encountering a \ then take the following character
     * - if multiple characters follow each other without a - inbetween, then they are all added separately
     * - the following characters can be escaped: - ( ) [ ] \t \b \n \r \f ' " \
     *
     * @param guard
     * @return
     */
    private static CharPred parseGuard(String guard) {
        return new CharPred('a');
    }

    public static void main(String[] args) {
        String filepath = "/Users/NW/Documents/Djungarian/TestSpecifications/src/phpfilters/PHPFilterSanitizeEmail.dot";
        try {
            read(filepath);
        } catch (TimeoutException e) {
            System.out.println("Timed out.");
        }
    }
}
