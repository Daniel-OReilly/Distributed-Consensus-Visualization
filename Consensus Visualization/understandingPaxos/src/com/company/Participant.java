package com.company;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Participant extends Thread {//KEY TO ALLOW PARTICIPANTS TO RUN ON THREADS

    BufferedWriter logWriter;
    Boolean isRunning;

    //Setting of required participant variables
    public Participant(String s) throws IOException {

        File log = new File("participantLogs/" + s + ".txt"); //manages log fie naming scheme and log writing
        this.logWriter = new BufferedWriter(new FileWriter(log, false));
        this.logWriter.write("");
        this.logWriter.flush();
        this.isRunning = true;

    }

    //Constructs log for Sending of Message
    public void singleLog(String participantType, int ID, int clock, String messageType) throws IOException {

        String participantName = participantType + ID;

        String message = participantName + " {\"" + participantName + "\":" + clock + "}\n" + messageType + "\n";

        this.logWriter.append(message);


    }
    //Constructs log for Receiveing of Message
    public void receiveLog(String receiverType, int receiverID, int receiverClock, String senderType, int senderID, int senderClock, String messageType) throws IOException {

        String receiverName = receiverType + receiverID;
        String senderName = senderType + senderID;

        String message = receiverName + " {\"" + senderName + "\":" + senderClock + ", \"" + receiverName + "\" :" + receiverClock + "}\n" + messageType + "\n";

        this.logWriter.append(message);

    }

    //Allows for the shutdonw and closing of participant logwriters
    public void shutdown() throws IOException {
        this.logWriter.close();
        isRunning = false;

    }
}
