/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.link;

import um.simulator.comm.network.NetworkLayer;
import java.util.ArrayList;
import um.simulator.comm.network.NPDU;
import um.simulator.comm.physical.PhysicalLayer;
import um.simulator.comm.physical.WAVEProtocol;

/**
 *
 * @author joaop
 * @author luisacabs
 * @version 1.0
 * 
 */
public class LinkLayer {

    public PhysicalLayer phy;
    public NetworkLayer network;
    public LinkProtocol proto;
    
    /**
     * Constructor: creates a link layer. 
     * 
     * @param physicalLayer a reference to the physical layer
     * @param linkProtocol  the link protocol
     * @param routingProtocol   the network layer protocol
     * @param protocolQueue size of buffer in network layer
     * @param numberOfRetransmissions number of retransmissions used in @param routingProtocol
     */
    public LinkLayer(PhysicalLayer physicalLayer, String linkProtocol, String routingProtocol,int protocolQueue,int numberOfRetransmissions) {
        this.phy=physicalLayer;
        if(linkProtocol.equals(null)){
            proto = new NullLinkProtocol();
        }
        network = new NetworkLayer(this,routingProtocol,protocolQueue,numberOfRetransmissions);
    }
    
    /**
     * Send NPDU to the physical layer.
     * @param npdu  the pdu to be sent
     * @param destAddress   the pdu's destination
     */
    public synchronized void sendFrame(NPDU npdu, String destAddress)
    {
        phy.sendFrame(npdu,destAddress);
    }
    
    
    /**
     * Calls <code>flush()</code> method on the physical layer.
     */
    public void flush(){
        phy.flushFrames();
    }
    
    /**
     * Send frames to the network layer.
     * @param frames    the list of npdu to be sent 
     */
    public void sendFramesUp(ArrayList<NPDU> frames)
    {
        network.sendPacketUp(frames);
    }
    
}
