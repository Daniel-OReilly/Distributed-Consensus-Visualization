package com.company;

public class Message {

    //Allows for all desired message types in the Paxos Algorithm

    String type;
    int epoch, acceptedEpoch, acceptedValue, value, currentEpoch, ID, clock;

    public Message(int ID, String type, int currentEpoch, int clock) {
        this.ID = ID;
        this.type = type;
        this.currentEpoch = currentEpoch;
        this.clock = clock;
    }

    public Message(int ID, String type, int epoch, int acceptedEpoch, int acceptedValue, int clock) {
        this.ID = ID;
        this.type = type;
        this.epoch = epoch;
        this.acceptedEpoch = acceptedEpoch;
        this.acceptedValue = acceptedValue;
        this.clock = clock;
    }


    public Message(int ID, String type, int epoch, int value, int clock) {
        this.ID = ID;
        this.type = type;
        this.epoch = epoch;
        this.value = value;
        this.clock = clock;

    }


}
