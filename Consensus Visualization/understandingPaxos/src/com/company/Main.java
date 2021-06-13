package com.company;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Scanner;

public class Main {

    public static int numAcceptors;
    public static int numProposers;

    //MAIN CLASS
    public static void main(String[] args) throws IOException, InterruptedException {
        //Basic functions on User Input
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter Numbers of Acceptors");
        numAcceptors = scan.nextInt();
        System.out.println("Enter Number of Proposers");
        numProposers = scan.nextInt();

        //Clearing of participantLogs and shivizLogs on the start of program
        File index = new File("participantLogs");
        String[] entries = index.list();
        for (String s : entries) {
            File currentFile = new File(index.getPath(), s);
            currentFile.delete();
        }
        File index2 = new File("shivizLog");
        String[] entries2 = index.list();
        for (String s : entries2) {
            File currentFile = new File(index2.getPath(), s);
            currentFile.delete();
        }

        //CREATION OF THREADS
        Proposer[] propposerArray = new Proposer[numProposers];
        Acceptor[] acceptorArray = new Acceptor[numAcceptors];

        for (int i = 1; i <= propposerArray.length; i++) {

            propposerArray[i - 1] = new Proposer(i, numAcceptors, numProposers, acceptorArray);

        }
        for (int j = 1; j <= acceptorArray.length; j++) {
            acceptorArray[j - 1] = new Acceptor(j, numProposers, propposerArray);
        }
        for (int i = 0; i < propposerArray.length; i++) {

            propposerArray[i].start();


        }
        for (int j = 0; j < acceptorArray.length; j++) {
            acceptorArray[j].start();
        }

        for (int i = 0; i < propposerArray.length; i++) {

            propposerArray[i].join();

        }
        for (int j = 0; j < acceptorArray.length; j++) {
            acceptorArray[j].shutdown();

        }
        createShivizLog(); //CREATION OF SHIVIZ LOGS

        try {

            URI uri = new URI("https://bestchai.bitbucket.io/shiviz/");

            java.awt.Desktop.getDesktop().browse(uri);
            System.out.println("Web page opened in browser");

        } catch (Exception e) {

            e.printStackTrace();
            System.exit(0);
        }
    }

    //MERGES INDIVIDUAL particpantLogs INTO ONE SHIVIZ LOG
    public static void createShivizLog() throws IOException {
        //Creating a File object for directory
        File directoryPath = new File("participantLogs/");
        //List of all files and directories
        File[] filesList = directoryPath.listFiles();
        Scanner sc = null;
        FileWriter writer = new FileWriter("shivizLog/upload.txt");
        writer.append("(?<host>\\S*) (?<clock>{.*})\\n(?<event>.*)\n");
        writer.append("\n");
        for (File file : filesList) {
            sc = new Scanner(file);
            String input;
            StringBuffer sb = new StringBuffer();
            while (sc.hasNextLine()) {
                input = sc.nextLine();
                writer.append(input + "\n");
            }
            writer.flush();
        }
        System.out.println("See the File \"shivizLog\" for log file ");
    }
}
