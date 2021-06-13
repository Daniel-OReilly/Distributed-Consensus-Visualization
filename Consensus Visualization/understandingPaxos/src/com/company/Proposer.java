package com.company;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Proposer extends Participant {

    public final int proposerID;
    public ArrayList<Integer> promisedSet;
    public ArrayList<Integer> acceptedSet;
    Acceptor[] acceptorArray;
    ArrayList<Integer> epochSet = new ArrayList<Integer>();
    int currentE, maxEpoch, numAcceptors, numProposers;
    int currentProposalValue;
    boolean trigger;
    Random rand;
    ConcurrentLinkedQueue<Message> messageQue;
    int vectorClock;

    //Set of necessary proposer variables
    public Proposer(int proposerID, int numAcceptors, int numProposers, Acceptor[] acceptorArray) throws IOException {
        super("P" + proposerID);

        this.proposerID = proposerID;

        this.promisedSet = new ArrayList<Integer>();

        this.acceptedSet = new ArrayList<Integer>();

        this.epochSet.add(proposerID);

        this.acceptorArray = acceptorArray;

        this.messageQue = new ConcurrentLinkedQueue<>();

        this.currentProposalValue = proposerID + 10;

        this.rand = new Random();

        this.vectorClock = 1;

        this.numAcceptors = numAcceptors;

        this.numProposers = numProposers;

        super.singleLog("P", this.proposerID, this.vectorClock, "Participant Initialized");
    }

    public void initialize() {

        this.epochSet.add(this.currentE + numProposers);
        this.currentE = this.epochSet.get(0);
        this.epochSet.remove(0);
        this.maxEpoch = 0;
        this.promisedSet = new ArrayList<Integer>();
        this.acceptedSet = new ArrayList<Integer>();


    }
    
    //Phase implementation of the Paxos Proposer Algorithm
    public void run() {

        do {

            try {
                trigger = partOne();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

            if (!trigger) {
                continue;
            }
            try {
                trigger = partTwo();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (!trigger) {
                continue;
            } else {
                break;
            }

        } while (true);

        try {
            super.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean partOne() throws InterruptedException, IOException {

        initialize();
        int randomSleep = rand.nextInt(1000);
        Thread.sleep(randomSleep);

        Message message1;
        this.vectorClock++;
        for (int i = 0; i < numAcceptors; i++) {

            message1 = new Message(this.proposerID, "Prepare", this.currentE, this.vectorClock);
            acceptorArray[i].sendMessage(message1);


        }
        super.singleLog("P", this.proposerID, this.vectorClock, "Sending Prepare");

        while (this.promisedSet.size() < numAcceptors / 2) {

            Message message = getNextMessage();

            switch (message.type) {

                case ("Promise"):

                    this.vectorClock++;
                    super.receiveLog("P", this.proposerID, this.vectorClock, "A", message.ID, message.clock, "Promise Received");
                    if (!(this.promisedSet.contains(message.ID))) {
                        this.promisedSet.add(message.ID);

                    }
                    if (message.acceptedEpoch != 0 && (this.maxEpoch == 0 || message.acceptedEpoch > this.maxEpoch)) {
                        this.maxEpoch = message.acceptedEpoch;
                        this.currentProposalValue = message.acceptedValue;

                    }
                    break;
                default:
                    this.vectorClock++;
                    super.receiveLog("P", this.proposerID, this.vectorClock, "A", message.ID, message.clock, "Timeout Received");
                    return false;


            }
        }

        return true;
    }

    public boolean partTwo() throws IOException {

        if (currentProposalValue == 0) {
            currentProposalValue = proposerID + 10;
        }

        Message message2;
        this.vectorClock++;
        for (int i = 0; i < numAcceptors; i++) {
            message2 = new Message(this.proposerID, "Propose", this.currentE, this.currentProposalValue, this.vectorClock);
            //this.vectorClock++;
            acceptorArray[i].sendMessage(message2);

        }
        super.singleLog("P", this.proposerID, this.vectorClock, "Sending Propose");

        while (this.acceptedSet.size() < ((numAcceptors / 2) + 1)) {

            Message message = getNextMessage();


            switch (message.type) {

                case ("Accept"):
                    this.vectorClock++;
                    super.receiveLog("P", this.proposerID, this.vectorClock, "A", message.ID, message.clock, "Accept Received");
                    if (!(this.acceptedSet.contains(message.ID))) {
                        this.acceptedSet.add(message.ID);
                    }
                    break;
                default:
                    this.vectorClock++;
                    super.receiveLog("P", this.proposerID, this.vectorClock, "A", message.ID, message.clock, "Timeout Received");
                    return false;


            }


        }
        System.out.println("Value Accepted");
        return true;
    }

    public Message getNextMessage() {
        Message message;
        synchronized (this) {

            message = messageQue.poll();

            while (message == null) {
                try {

                    wait();

                    message = messageQue.poll();


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


        }
        return message;

    }


    public synchronized void sendMessage(Message m) {
        messageQue.add(m);
        notify();
    }

}



