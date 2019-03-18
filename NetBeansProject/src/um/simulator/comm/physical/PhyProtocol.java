/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.physical;

import um.simulator.actor.Actor;
import um.simulator.comm.link.LinkLayer;
import um.simulator.comm.network.NPDU;

/**
 * This class represents a generic physical protocol.
 * 
 * @author luisacabs
 * @version 1.0
 */
public class PhyProtocol {
    public Actor actor;
    public LinkLayer linkLayer;
    
    /**
     * Constructor: creates a generic physical protocol.
     * 
     * @param actor the id of the actor
     * @param link  a reference to the link layer.
     */
    public PhyProtocol(Actor actor, LinkLayer link){
        this.actor = actor;
        this.linkLayer = link;
    }
    /**
     * Sends frame to the link layer.
     * 
     * @param npdu  the pdu to be sent
     * @param destAddres    the destination of the pdu
     */
    public synchronized void sendFrame(NPDU npdu, String destAddres) {
        
    }
    /**
     * Checks for messages destined to the actor.
     */
    public void flushFrames(){
    }
    /**´
     * Updates the physical layer.
     */
    public void update() {
    }
}
