/**
 * This file has been made by Sophie Lathouwers
 */
package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.BooleanAlgebraSubst;
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
        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();

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
            System.out.println(line);
            if (line.matches("[0-9]+\\[.*\\]")) {
                System.out.println("Matching with the state declaration");
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
                System.out.println("Matching with the transition rule");
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

                CharPred guards = parseGuard(guard, ba);
                List<CharFunc> terms = parseTerms(term);

                transitions.add(new SFTInputMove<>(Integer.valueOf(from), Integer.valueOf(to), guards, terms));
            } else if (line.matches("XX[0-9]+ \\[.*\\]XX[0-9]+ -> [0-9]+")) {
                System.out.println("Matching with the XX rule");
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
                // TODO: This first part of the if should no longer be necessary now that the identity function is outputted as x+0 instead of x + 0
                if (i < (parts.size() - 4) && parts.get(i) == 'x' && parts.get(i+2) == '+' && parts.get(i+4) == '0') {
                    terms.add(CharOffset.IDENTITY);
                    i += 5; // Skip converted chars
                } else if (i < (parts.size() - 2) && parts.get(i) == 'x' && parts.get(i+1) == '+' && parts.get(i+2) == '0') {
                    terms.add(CharOffset.IDENTITY);
                    i+=3;
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
     *      - the following characters should be escaped (each character is separated by a space): - ( ) [ ] \t \b \n \r \f ' " \
     * - the unicode character < 0x20 and > 0x7f are written down in unicode
     * - if multiple characters follow each other without a - inbetween, then they are all added separately
     *
     * @param guard
     * @return
     */
    private static CharPred parseGuard(String guard, BooleanAlgebraSubst<CharPred, CharFunc, Character> ba) throws TimeoutException {
        CharPred pred = ba.False();
        char[] chars = guard.toCharArray();
        int i = 0;
        Character previous = null;
        Character next = null;
        boolean findRange = false;
        while (i < chars.length) {
            // Recognize the character
            if (chars[i] == '\\' && chars[i+1] == 'u') {
                // Recognize a unicode char
                String identifier = "" + chars[i+2] + chars[i+3] + chars[i+4] + chars[i+5];
                // ASSUMES that it 'fits' in a single char!!
                int id = Integer.valueOf(identifier, 16);
                next = (char) id;
                i += 6;
            } else if (chars[i] == '\\') {
                // Recognize an escaped character
                if (chars[i+1] == 't') {
                    next = '\t';
                } else if (chars[i+1] == 'b') {
                    next = '\b';
                } else if (chars[i+1] == 'n') {
                    next = '\n';
                } else if (chars[i+1] == 'r') {
                    next = '\r';
                } else if (chars[i+1] == 'f') {
                    next = '\f';
                } else if (chars[i+1] == '-' || chars[i+1] =='(' || chars[i+1] == ')' || chars[i+1] == '[' || chars[i+1] == ']' || chars[i+1] == '\'' || chars[i+1] == '"' || chars[i+1] == '\\') {
                    next = chars[i+1];
                } else {
                    System.err.print("This character should not be escaped!");
                }
                i += 2;
            } else if (chars[i] == '-') {
                // Check whether it is a '-' to indicate a range, Remember the left bound of the range
                previous = next;
                findRange = true;
                i++;
            } else {
                // It was an ordinary char
                next = chars[i];
                i++;
            }

            // If the last character is not in a range, add it to the predicate
            if (!findRange && i == chars.length-1) {
                pred = ba.MkOr(pred, new CharPred(next, next));
                next = null;
            }

            // If we have found the right bound of the range, then add the range
            if (findRange && previous != null && previous != next) {
                pred = ba.MkOr(pred, new CharPred(previous, next));
                // Reset bounds
                previous = null;
                next = null;
                findRange = false;
            } else if (!findRange && previous != null) {
                // It was not a range so we need to add the previous character
                pred = ba.MkOr(pred, new CharPred(previous, previous));
                previous = null;
            }
            previous = next;
        }
        System.out.println("Parsed guard into: "+pred.toString());
        return pred;
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
