package sftlearning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExecuteCommand {

    public static String executeCommandPB(String[] command, String input) {
        String[] fullCommand = new String[command.length+1];
        for (int i=0; i<command.length; i++) {
            fullCommand[i] = command[i];
        }
        fullCommand[command.length] = input;
        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println(line);
                return line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String executeCommand(String command) {
        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec(command);
            p.waitFor();

            BufferedReader br2 = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line2;
            while ((line2 = br2.readLine()) != null) {
                System.out.println(line2);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                return line;
            }
        } catch (IOException e) {
            System.out.println("Unable to execute command!");
        }
        catch (InterruptedException e) {
            System.out.println("Interrupted Exception!");
        }
        return null;
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            String cmd = "";
            for (int i=0; i<args.length; i++) {
                cmd += args[i];
                if (i != args.length-1) {
                    cmd += " ";
                }
            }
            System.out.println("Executing the command: "+cmd);
            String output = executeCommand(cmd);
            System.out.println("Output received: "+output);
        } else {
            System.out.println("No command has been defined to execute! Please specify a command...");
        }
    }
}
