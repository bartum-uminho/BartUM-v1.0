/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.actor.Actor;
import um.simulator.comm.network.NetworkLayer;
import um.simulator.comm.physical.WAVEProtocol;
import um.simulator.comm.application.ApplicationLayer;
import um.simulator.comm.link.LinkLayer;
import um.simulator.comm.physical.PhysicalLayer;
import um.simulator.core.SimStatus;

/**
 * This class represents the Communication Stack.
 * 
 * The CommStack has a dedicated <code>Thread</code> that updates the 
 * different communication layers each <code>updatePace</code> seconds.
 * 
 * @author luisacabs
 * @version 1.0
 */
public class CommStack extends Thread{

    Actor actor;
    /** to log comunication times */
    ArrayList<Long> log_comTimes = new ArrayList<>(); 
    WAVEProtocol phyProto;
    
    PhysicalLayer phyLayer;
    LinkLayer linkLayer;
    NetworkLayer networkLayer;
    ApplicationLayer appLayer;
    
    public int updatePace = 1000; 
    /** communication stack status 
     * true - up
     * false - down
     */
    public boolean status = false;
    
    /** Constructor: Creates a Communication Stack associated with an <code>Actor</code> and its parameters.
     * 
     * @param actor Actor owning the CommStack
     * @param csParams  Communication Stack parameters
     */
    public CommStack(Actor actor, String[] csParams) {
        this.actor = actor;
        /** protocol to use in the physical layer */
        String physicalProtocol = csParams[0];
        /** data rate of the physical layer */
        int PHYDataRate = Integer.parseInt(csParams[1]);
        /** range of the physical layer */
        int PHYTxRange = Integer.parseInt(csParams[2]);
        /** frame error rate of the physical layer */
        double FER = Double.parseDouble(csParams[3]);
        /** pro to use in the link layer */
        String linkProtocol = csParams[4];
        /** routing protocol for the network layer */
        String routingProtocol = csParams[5];
        /** limit of messages that the node can save in the network layer */
        int routingProtocolQueue = Integer.parseInt(csParams[6]);
        /** number of retransmissions of the network layer (if the protocol is SprayAndWait) */ 
        int numberOfRetransmissions = Integer.parseInt(csParams[7]);
        /** references to the different layers */
        phyLayer = new PhysicalLayer(actor, physicalProtocol,PHYDataRate, PHYTxRange, FER,
                linkProtocol, routingProtocol, routingProtocolQueue, numberOfRetransmissions);
        linkLayer = phyLayer.linkLayer;
        networkLayer = phyLayer.linkLayer.network;
        appLayer = phyLayer.linkLayer.network.appLayer;
    }
    
    @Override
    public void run(){
        long time_ini, deltaTime, deltaTime_ms;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(CommStack.class.getName()).log(Level.SEVERE, null, ex);
        }
        while(actor.alive || !SimStatus.globalActors.get(actor.getActorId()).actorDyingQ()) {
            time_ini = System.nanoTime();
            update();
            deltaTime = System.nanoTime() - time_ini;
            deltaTime_ms = TimeUnit.NANOSECONDS.toMillis(deltaTime);
            
            if (deltaTime_ms <= updatePace) {
                try {
                    /** sleeps for some time */
                    Thread.sleep(updatePace - deltaTime_ms); 
                } catch (InterruptedException ex) {
                    Logger.getLogger(CommStack.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                /** update cycle takes more than <code>updatePace</code> ms */
            }
        }
        this.networkLayer.turnOffLayer();
        try {
            this.networkLayer.proto.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(CommStack.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    /**
     * Reference to the physical layer. 
     * 
     * @return the physical layer object
     */
    public PhysicalLayer getPhyLayer() {
        return phyLayer;
    }

    /**
     * Reference to the application layer. 
     * 
     * @return the application layer object
     */
    public ApplicationLayer getAppLayer() {
        return appLayer;
    }
    
    /**
     * 
     * @return the communication update times
     */
    public ArrayList<Long> getComTimes(){
        return this.log_comTimes;
    }
    
    /** 
     * Update method - runs each <code>updatePace</code> ms.
     * 
     * Every time this method is executed, the physical layer is updated, i.e. the messages
     * in the buffer are sent upwards. 
     * Then, from time to time (depending on the parameters defined on the setting.properties file),
     * the application layer is also updated. If the application running in the top layer
     * is not working in trasmitter (T) or transceiver (B) mode, then it does not need
     * to be updated.
     */
    public void update() {
        phyLayer.update();
        appLayer.update();
    }

}
