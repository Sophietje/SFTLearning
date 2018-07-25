/**
 * This file has been made by Sophie Lathouwers
 */
package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

public class TestAutomaticOutputGeneration extends SymbolicOracle<CharPred, CharFunc, Character> {

    private Scanner sc;

    private static final String command = "node /Users/NW/Documents/Djungarian/Sanitizers/encode/reverse.js";

    public TestAutomaticOutputGeneration() {
        sc = new Scanner(System.in);
    }


    @Override
    protected List<Character> checkEquivalenceImpl(SFT<CharPred, CharFunc, Character> compareTo) {
        System.out.println(compareTo);
        char in = '\u0000';
        // Ask whether the hypothesis automaton is correct
        // This question will be repeated until valid input (either 'n' or 'y') has been given
        while (in != 'n' && in != 'y') {
            System.out.println("Is that your automaton? (y/n):");
            String inLine = sc.nextLine();
            if (inLine != null && !inLine.isEmpty() && inLine.length()==1) {
                in = inLine.charAt(0);
            }
        }
        if (in == 'y') {
            return null;
        }
        System.out.println("Enter counterexample string a1,a2,a3... :");
        String cex = sc.nextLine();
        String[] parts = cex.split(",");
        List<Character> chars = new ArrayList<>();
        for (String s : parts) {
            for (Character c : s.toCharArray()) {
                chars.add(c);
            }
        }
        return chars;
    }

//    @Override
//    protected List<Character> checkMembershipImpl(List<Character> w) {
//        //
////        try {
////            Process p = Runtime.getRuntime().exec("python test.py abc<>&<>&ab\\c");
////            InputStream inputStream = p.getInputStream();
////            OutputStream outputStream = p.getOutputStream();
////            String s = "";
////            for (Character c : w) {
////                s += c;
////            }
////            OutputStreamWriter ioWriter = new OutputStreamWriter(outputStream);
////            ioWriter.write(s);
////            InputStreamReader ioReader = new InputStreamReader(inputStream);
////            int i = ioReader.read();
////
////        } catch (IOException e) {
////            System.out.println("ERR: Was unable to execute the command!");
////            System.exit(0);
////        }
//        // For the above mentioned command, have a look at: https://stackoverflow.com/questions/10097491/call-and-receive-output-from-python-script-in-java
//        return encode(w);
//    }
    @Override
    protected List<Character> checkMembershipImpl(List<Character> w) {
        String input = "";
        for (Character c : w) {
            input += c;
        }
        String output = ExecuteCommand.executeCommandPB(command.split(" "), input);
        return stringToCharList(output);
    }

    public static List<Character> removeEvenBs(List<Character> w) {
        List<Character> result = new ArrayList<>();
        int numberBs = 0;
        for (Character c : w) {
            if (c == 'b') {
                numberBs++;
            }
            if (!(c == 'b' && numberBs % 2 == 0)) {
                result.add(c);
            }
        }
        return result;
    }

    public static List<Character> encode(List<Character> w) {
        List<Character> result = new ArrayList<>();
        for (Character c : w) {
            if (c == '<') {
                result.add('&');
                result.add('g');
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

        UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
        BinBSFTLearner ell = new BinBSFTLearner();
        SymbolicOracle o = new TestAutomaticOutputGeneration();
        SFT<CharPred, CharFunc, Character> learned = null;
        try {
            learned = ell.learn(o, ba, 120);
            System.out.println(learned);
//            learned.createDotFile("testEscapingSlashes", "/Users/NW/Documents/Djungarian/SVPAlib/src/learning/sfa");
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

    public static List<Character> stringToCharList(String word) {
        List<Character> output = new ArrayList<>();
        if (word == null || word.isEmpty()) {
            return output;
        }
        for (int i=0; i<word.length(); i++) {
            output.add(word.charAt(i));
            System.out.println("Char:"+word.charAt(i));
        }
        System.out.println("Output upon "+word+" is "+output);
        return output;
    }

}
