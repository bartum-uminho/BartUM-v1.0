/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.application;

import um.simulator.comm.network.NetworkLayer;
import java.util.ArrayList;
import java.util.Random;
import um.simulator.actor.Actor;
import um.simulator.comm.CommStack;
import um.simulator.core.SimStatus;

/**
 * This class represents a generic Application.
 *
 * @author luisacabs
 * @version 1.0
 */
public class Application {
    
    
    public String appName;
    public char opMode;
    public String actorId;
    public NetworkLayer network;
    Random randomGenerator; 
    
    /**
     * Constructor: creates an abstract application.
     * 
     * @param net   a reference to the network layer
     * @param app   the application's name
     * @param opMode    operation mode of the application
     * @param id    the actor's id running the application
     */
    public Application(NetworkLayer net, String app, char opMode, String id){
        this.network = net;
        this.appName = app;
        this.opMode = opMode;
        this.actorId=id;
        this.randomGenerator = new Random();
    }
    /**
     * Updates the Application Layer.
     * 
     * Overrided method in inherited classes.
     */
    public void update(){
    
    }
    /**
     * Generates a message and sends it to the lower Layer
     */
    public void generateMessage(){
    }
    /**
     * Method called after a message reception.
     * Process the message report it and possibly respond accordingly.
     * Example: Message received asking for the ID of the actor's running the app,
     * so this actor responds with its ID.
     * 
     * @param a and application pdu
     */
    public void processReceivedMessage(APDU a){
        
    }
    
    /**
     * Generates a random destination from the global actors
     * @return the actor id from the chosen destination
     */
    public String generateRandomDestination(){
        int rand;
        String actorDestinationID = actorId;
        /** chooses random ID from globalActors != from SourceID */
        while (actorDestinationID.equals(actorId)) {
            /** both operations have to be synchronized */
            synchronized (SimStatus.globalActors) {
                /** generates a random integer from 0 to bound(inc.) */
                rand = randomGenerator.nextInt(SimStatus.globalActors.size());
                actorDestinationID = (String) SimStatus.globalActors.keySet().toArray()[rand];
            }
        }
        return actorDestinationID;
    }
    
    /**
     * Receive packets from the lower layer
     * @param packets array list with application pdu
     */
    public void receivePacketsUp(ArrayList<APDU> packets) {
    
    }
}
