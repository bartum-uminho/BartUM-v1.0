/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.physical;

import java.util.ArrayList;
import um.simulator.actor.Actor;
import um.simulator.comm.link.LinkLayer;
import um.simulator.comm.network.NPDU;

/**
 * This class represents the physical layer.
 * 
 * @author luisacabs
 * @version 1.0
 */
public class PhysicalLayer {
    public LinkLayer linkLayer;
    ArrayList<PPDU> buffer;
    public PhyProtocol proto;
    String phy;
    String link;
    public Actor actor;
    
    /**
     * Constructor: creates a physical layer.
     * 
     * @param actor the id of the actor
     * @param physicalProtocol  the physical protocol
     * @param PHYDataRate   data rate in the physical layer
     * @param PHYTxRange data transfer range
     * @param FER   frame error rate
     * @param linkProtocol  the link layer protocol
     * @param routingProtocol   the network layer protocol
     * @param routingProtocolQueue  size of buffer in network layer
     * @param numberOfRetransmissions   used in network protocol
     */
    public PhysicalLayer(Actor actor, String physicalProtocol, int PHYDataRate, int PHYTxRange, double FER, 
            String linkProtocol,String routingProtocol, int routingProtocolQueue, int numberOfRetransmissions) {
        this.actor = actor;
        buffer = new ArrayList<>();
        this.phy = physicalProtocol;
        this.link = linkProtocol;
        linkLayer = new LinkLayer(this, link, routingProtocol, routingProtocolQueue, numberOfRetransmissions);
        if(physicalProtocol.equalsIgnoreCase("WAVE")){
            proto = new WAVEProtocol(actor,linkLayer,PHYDataRate,PHYTxRange,FER, routingProtocol,
            routingProtocolQueue,numberOfRetransmissions);
        }else if (physicalProtocol.equalsIgnoreCase("bluetooth")){
            proto = new BluetoothProtocol(actor,linkLayer);
        }
    }
    /**
     * Call <code>sendFrame()</code> method in the physical protocol.
     * 
     * @param npdu  the pdu being sent
     * @param destAddress   the destination of the pdu
     */
    public synchronized void sendFrame(NPDU npdu, String destAddress) {
        proto.sendFrame(npdu,destAddress);
    }
    /**
     * Call <code>flushFrames()</code> method in the physical protocol.
     */
    public void flushFrames(){
        proto.flushFrames();
    }
    /**
     * Call <code>update()</code> method in the physical protocol.
     */
    public void update() {
        proto.update();
    }
}
