/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.network;

import java.util.concurrent.TimeUnit;

/**
 * This class represents a Network Layer PDU. 
 * 
 * @author joaop
 * @version 1.0
 */
public class NPDU {
    /** field divisor to encapsulation */
    String d = "#"; 
    String sourceAddress;
    String destinationAddress;
    /** packet identification */
    String packetId;
    String protocol;
    Integer timeToLive;
    String data;
    
    /** Constructor: Creates a Network Layer PDU from a Physical Layer PDU.
     * 
     * @param ppdu  A physical layer PDU.
     */
    public NPDU(String ppdu) {
        String[] fields = ppdu.split(d);
        sourceAddress = fields[0];
        destinationAddress = fields[1];
        packetId = fields[2];
        protocol = fields[3];
        timeToLive = Integer.parseInt(fields[4]);
        data = fields[5];
    }
    
    /** Constructor: Creates a Network Layer PDU from its mandatory fields.
     * 
     * @param sourceAddress the Address of the source node
     * @param destinationAddress the packet's destination
     * @param id    id of the current Actor
     * @param protocol  protocol used for routing
     * @param TTL   the packet's time to live
     * @param data  the packet's payload
     */
    public NPDU(String sourceAddress, String destinationAddress, String id, String protocol, int TTL, String data) {
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.packetId = id;
        this.protocol = protocol;
        this.timeToLive = TTL;
        this.data = data;
    }
    /**
     * Address of the packet.
     * 
     * @return the message's source node
     */
    public String getSourceAddress() {
        return sourceAddress;
    }
    
    /**
     * Changes the address of the packet.
     * 
     * @param sourceAddress a new source node
     */
    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    /**
     * Destination address of the packet.
     * 
     * @return the message's destination
     */
    public String getDestinationAddress() {
        return destinationAddress;
    }
    
    /**
     * Changes the destination address of the packet.
     * 
     * @param destinationAddress a new destination node
     */
    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }
    
    /**
     * ID of the packet.
     * 
     * @return the message's id
     */    
    public String getId() {
        return packetId;
    }
    
    /**
     * Changes the packet's id.
     * 
     * @param id a new id
     */
    public void setId(String id) {
        this.packetId = id;
    }
    
    /**
     * Protocol used.
     * 
     * @return protocol
     */
    public String getProtocol() {
        return protocol;
    }
    
    /**
     * Changes the protocol being used.
     * 
     * @param protocol a new routing protocol
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    /**
     * Time to live.
     * 
     * @return ttl
     */
    public int getTTL() {
        return timeToLive;
    }
    
    /**
     * Changes the time to live value.
     * 
     * @param TTL new ttl 
     */
    public void setTTL(int TTL) {
        this.timeToLive = TTL;
    }
    
    /**
     * Updates the TTL of the NPDU given the time when it was saved, by 
     * subtracting the current time
     * 
     * @param timeUpdated last time the ttl was updated
     * @return TTL (time to live)
     */
    public long updateTTL(long timeUpdated){
        long currentTime = System.currentTimeMillis();
        int timeStored = (int) TimeUnit.MILLISECONDS.toSeconds(currentTime - timeUpdated);
        synchronized(timeToLive){
            this.timeToLive = this.timeToLive-timeStored;
        }
        return timeToLive;
    }

    /**
     * Packet's payload.
     * 
     * @return payload
     */
    public String getData() {
        return data;
    }
    
    /**
     * Changes the packet's payload.
     * 
     * @param data new payload 
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Checks if packets is still valid. 
     * 
     * @return returns TRUE if TTL is greater than 0, otherwise returns FALSE
     */
    public boolean isValid_ttl(){
        return timeToLive > 0;
    }    
    
    /** All of the packets fields. 
     * 
     * @return the whole packet.
     */
    public String getAllFields() {
        return sourceAddress + d + destinationAddress + d + packetId + d + protocol + d + timeToLive + d + data;
    }

}
