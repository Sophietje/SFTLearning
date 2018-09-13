/**
 * This file has been made by Sophie Lathouwers
 */

package sftlearning;

import java.io.*;

/**
 * This class is used to test the overhead of re-starting a process for each membership oracle
 */
public class TestCommandExecution {
    final static String commandMultipleProcesses = "php /Users/NW/Documents/Djungarian/Sanitizers/src/escapeHTML.php";
    final static String commandOneProcess = "php /Users/NW/Documents/Djungarian/Sanitizers/src/escapeHtmlSpecialCharsStreams.php";
    static String input = "|adba0|{PO()@&$*}}\"odau2047120najslAa";

    public static void startMultipleProcesses(int n) {
        String[] command = commandMultipleProcesses.split(" ");

        // Start n processes which each execute a command
        for (int j=0; j<n; j++) {
            String[] fullCommand = new String[command.length + 1];
            for (int i = 0; i < command.length; i++) {
                fullCommand[i] = command[i];
            }
            fullCommand[command.length] = input+j;
            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            pb.redirectErrorStream(true);
            try {
                Process p = pb.start();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = br.readLine();
//                System.out.println(line);
                p.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void startOneProcess(int n) {
        String[] command = commandOneProcess.split(" ");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            for (int i = 0; i<n; i++) {
//                System.out.println("Writing input "+input+i);
                bw.write(input+i);
                bw.newLine();
                bw.flush();
                String line = br.readLine();
//                System.out.println(line);
            }
            br.close();
            bw.close();
            p.destroy();
        } catch (IOException e) {
            System.out.println();
        }
    }

    public static void main(String[] args) {
        int n = 100;
        long start = System.currentTimeMillis();
        // Execute re-starting each process
        startMultipleProcesses(n);
        long end = System.currentTimeMillis();
        System.out.println("Re-starting each process cost "+(end-start)+" milliseconds");

        start = System.currentTimeMillis();
        // Execute starting one process and asking it multiple queries
        startOneProcess(n);
        end = System.currentTimeMillis();
        System.out.println("Using one process cost "+(end-start)+" milliseconds");
    }

}
