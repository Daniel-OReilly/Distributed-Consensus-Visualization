package com.company;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Acceptor extends Participant {//SUBCLASS OF PARTICIPANT

    int acceptorID;
    Proposer[] proposerArray;
    int lastAcceptedValue = 0;
    int lastAccepteEpoch = 1;
    int lastPromisedEpoch = 0;
    int numProposers;
    ConcurrentLinkedQueue<Message> messageQue;
    int vectorClock;

    //Sets basic acceptor functionality
    public Acceptor(int acceptorID, int numProposers, Proposer[] proposerArray) throws IOException {
        super("A" + acceptorID);
        this.acceptorID = acceptorID;
        this.proposerArray = proposerArray;
        this.messageQue = new ConcurrentLinkedQueue<>();
        this.numProposers = numProposers;
        this.vectorClock = 1;
        super.singleLog("A", this.acceptorID, this.vectorClock, "Participant Initialized");

    }

    //CONTAINS THE BODY OF THE PAXOS ACCEPTOR ALGORITHM
    public void run() {

        while (super.isRunning) {

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

            switch (message.type) {

                case ("Prepare"):
                    vectorClock++;
                    try {
                        super.receiveLog("A", this.acceptorID, this.vectorClock, "P", message.ID, message.clock, "Prepare Received");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (lastPromisedEpoch == 0 || message.currentEpoch >= lastPromisedEpoch) {
                        int currentEpoch = message.currentEpoch;
                        lastPromisedEpoch = currentEpoch;
                        Message message1 = new Message(this.acceptorID, "Promise", currentEpoch, lastAccepteEpoch, lastAcceptedValue, this.vectorClock);

                        for (int i = 0; i < numProposers; i++) {
                            this.vectorClock++;
                            message1 = new Message(this.acceptorID, "Promise", currentEpoch, lastAccepteEpoch, lastAcceptedValue, this.vectorClock);
                            proposerArray[i].sendMessage(message1);
                            try {
                                super.singleLog("A", this.acceptorID, this.vectorClock, "Sending Promise");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }


                    }

                    break;

                case ("Propose"):
                    vectorClock++;

                    try {
                        super.receiveLog("A", this.acceptorID, this.vectorClock, "P", message.ID, message.clock, "Propose Received");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (this.lastPromisedEpoch == 0 || message.epoch >= this.lastPromisedEpoch) {
                        this.lastPromisedEpoch = message.epoch;
                        this.lastAcceptedValue = message.value;
                        this.lastAccepteEpoch = message.epoch;
                        Message message2 = new Message(this.acceptorID, "Accept", message.epoch, this.vectorClock);

                        for (int i = 0; i < numProposers; i++) {
                            this.vectorClock++;
                            message2 = new Message(this.acceptorID, "Accept", message.epoch, this.vectorClock);
                            proposerArray[i].sendMessage(message2);
                            try {
                                super.singleLog("A", this.acceptorID, this.vectorClock, "Sending Accept");

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }


                    }

                    break;
            }


        }


    }

    public synchronized void sendMessage(Message m) {
        messageQue.add(m);
        notify();
    }


}