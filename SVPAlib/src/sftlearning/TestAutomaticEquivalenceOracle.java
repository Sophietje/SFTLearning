package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;

public class TestAutomaticEquivalenceOracle extends SymbolicOracle<CharPred, CharFunc, Character> {

    private static UnaryCharIntervalSolver ba;
    private static SymbolicOracle o;


    private Scanner sc;

    public TestAutomaticEquivalenceOracle() {
        sc = new Scanner(System.in);
    }


    @Override
    protected List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) throws TimeoutException {
        int maxLength = 15;
        int numTests = 2500;
        List<List<Character>> tested = new ArrayList<>();
        for (int i=0; i<numTests; i++) {
            List<Character> input = new ArrayList<>();
            boolean justStarted = true;
            boolean foundCx = false;
            while (justStarted || tested.contains(input)) {
                for (int j = 0; j < maxLength; j++) {
                    // Choose random character to add to input
                    int random = ThreadLocalRandom.current().nextInt(20, 370);
                    Character c = CharPred.MIN_CHAR;
                    for (int k = 0; k < random; k++) {
                        c++;
                    }
                    input.add(c);
                }
                justStarted = false;
                if (!tested.contains(input)) {
                    foundCx = true;
                } else {
                    input = new ArrayList<>();
                }
            }

            // Random input has been generated
            // Check whether output of hypothesis is the same as the output of the System Under Learning
            List<Character> hypothesisOutput = compareTo.outputOn(input, ba);
            List<Character> sulOutput = o.checkMembership(input);
            tested.add(input);
            if (!hypothesisOutput.equals(sulOutput)) {
                // Output was not the same thus we have found a counterexample
                return input;
            }
        }
        // No counterexample has been found!
        return null;
    }

    @Override
    protected List<Character> checkMembershipImpl(List<Character> w) {
        // TODO: Use exec() call to call appropriate command to execute Python/Ruby/PHP/etc.
//        try {
//            Process p = Runtime.getRuntime().exec("python test.py abc<>&<>&ab\\c");
//            InputStream inputStream = p.getInputStream();
//            OutputStream outputStream = p.getOutputStream();
//            String s = "";
//            for (Character c : w) {
//                s += c;
//            }
//            OutputStreamWriter ioWriter = new OutputStreamWriter(outputStream);
//            ioWriter.write(s);
//            InputStreamReader ioReader = new InputStreamReader(inputStream);
//            int i = ioReader.read();
//
//        } catch (IOException e) {
//            System.out.println("ERR: Was unable to execute the command!");
//            System.exit(0);
//        }
        // For the above mentioned command, have a look at: https://stackoverflow.com/questions/10097491/call-and-receive-output-from-python-script-in-java
        return encode(w);
    }

    public static List<Character> encode(List<Character> w) {
        List<Character> result = new ArrayList<>();
        for (Character c : w) {
            if (c == '<') {
                result.add('&');
                result.add('l');
                result.add('t');
                result.add(';');
            } else if (c == '>') {
                result.add('&');
                result.add('g');
                result.add('t');
                result.add(';');
            } else if (c == '&') {
                result.add('&');
                result.add('a');
                result.add('m');
                result.add('p');
                result.add(';');
            } else {
                result.add(c);
            }
        }
        return result;
    }

    public static List<Character> encodeLT(List<Character> w) {
        List<Character> result = new ArrayList<>();
        for (Character c : w) {
            if (c == '<') {
                result.add('&');
                result.add('a');
                result.add('m');
                result.add('p');
                result.add(';');
            } else {
                result.add(c);
            }
        }
        return result;
    }

    public static List<Character> capitalize(List<Character> w) {
        String input = "";
        for (Character c : w) {
            input += c;
        }
        String result = input.toUpperCase();
        List<Character> resultCharacters = new ArrayList<>(result.length());
        for (int i = 0; i<result.length(); i++) {
            resultCharacters.add(result.charAt(i));
        }
        return resultCharacters;
    }

    public static List<Character> escape(List<Character> w) {
        String input = "";
        for (Character c : w) {
            input += c;
        }
        String result = input.replaceAll("\\\\{1,2}",  Matcher.quoteReplacement("\\\\"));
        List<Character> resultCharacters = new ArrayList<>(result.length());
        for (int i=0; i<result.length(); i++) {
            resultCharacters.add(result.charAt(i));
        }
        return resultCharacters;
    }

    public static void main(String[] args) {
//        List<Character> word = new ArrayList<>();
//        printCharList(escape(word));
//        word.add('b');
//        printCharList(escape(word));
//        word.add('\\');
//        printCharList(escape(word));
//        word.add('\\');
//        printCharList(escape(word));
//        word.add('b');
//        printCharList(escape(word));

        ba = new UnaryCharIntervalSolver();
        BinBSFTLearner ell = new BinBSFTLearner();
        o = new TestAutomaticEquivalenceOracle();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            learned = ell.learn(o, ba);
            learned.createDotFile("testEscapingSlashes", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static void printCharList(List<Character> word) {
        String output = "";
        for (Character c : word) {
            output +=c;
        }
        System.out.println(output);
    }

}
