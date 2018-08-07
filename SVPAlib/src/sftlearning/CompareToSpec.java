/**
 * This file has been made by Sophie Lathouwers
 */
package sftlearning;

import org.sat4j.specs.TimeoutException;
import theory.characters.CharFunc;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;
import transducers.sft.SFT;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/**
 * This class is used to run a program that reads and checks a specification to a learned model
 */
public class CompareToSpec {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // Get specification file name
        System.out.println("Give the full path to the specification (DOT) file: ");
        String specPath = sc.nextLine();

        System.out.println("Give the full path where the learned model should be stored: ");
        String savePath = sc.nextLine();

        // Read specification
        try {
            SFT spec = ReadSpecification.read(specPath);

            System.out.println("Command to use for membership oracle: ");
            sc.nextLine();
            String command = sc.nextLine();

            // Get and set settings for learning a model
            TestAutomaticOracles oracles = new TestAutomaticOracles(command);
            oracles.setLearningSettings(sc);

            System.out.println("Maximum number of minutes to run?");
            int maxMinutes = sc.nextInt();
            // Read newline from previous line since nextInt doesn't read the new-line character
            // due to which nextLine will always return the empty string the first time
            sc.nextLine();

            // Learn model
            UnaryCharIntervalSolver ba = new UnaryCharIntervalSolver();
            long startTime = System.currentTimeMillis();
            BinBSFTLearner ell = new BinBSFTLearner();
            SFT<CharPred, CharFunc, Character> learned = null;
            learned = ell.learn(oracles, ba, maxMinutes);

            // Measure how long the learning took
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            long sec = totalTime/1000;
            long min = sec/60;
            System.out.println("Total learning time: "+min+" minutes ("+sec+" seconds)");
            System.out.println("Time spent in membership oracle: "+ (oracles.timeMembership)+" milliseconds");
            System.out.println("Time spent in equivalence oracle: "+(oracles.timeEquivalence)+" milliseconds");

            // Compare model and specification
            if (learned != null) {
                if (spec != null) {
                    startTime = System.currentTimeMillis();
                    boolean equal = SpecificationChecking.areEqual(learned, spec);
                    endTime = System.currentTimeMillis();
                    System.out.println("Is it equal to the specification? " + equal);
                    System.out.println("How long did it take to compare models? " + ((endTime - startTime)) + " milliseconds...");
                    List<Character> witness = learned.witness1disequality(spec, ba);
                    if (witness != null) {
                        System.out.println("Witness: " + witness);

                        System.out.println(learned.outputOn(witness, ba));
                        System.out.println(spec.outputOn(witness, ba));
                    }
                    System.out.println("Specified model:");
                    System.out.println(spec);
                }
                System.out.println("Learned model:");
                System.out.println(learned);
            } else {
                System.out.println("Timed out: Could not find a model in limited time frame");
                System.out.println("Currently established model: ");
                System.out.println(learned);
            }
            String currentDateTime = LocalDateTime.now().toString();
            learned.createDotFile("learnedModel"+ currentDateTime, savePath);
            System.out.println("The learned model can be found at: "+savePath+"learnedModel"+currentDateTime+".dot");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}
