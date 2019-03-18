/*
 * To change this template, choose Tools # Templates
 * and open the template in the editor.
 */
package um.simulator.comm.physical;

/**
 * This class represents a Physical Layer PDU. 
 * 
 * @author lmnd
 * @version 1.0
 */
public class PPDU {
    
    /** field divisor to encapsulation */
    private final String d = "#"; 
    private long time_stamp;
    private String source_id;
    private String destination_id;
    private final char protocol;
    private Double actor_x;
    private Double actor_y;
    private String data;
    private boolean read;
    private boolean sent;

    /** Constructor: creates a Physical PDU from its mandatory parameters.
     * 
     * @param time_stamp    time of PDU's creation
     * @param source_id id of the message generator
     * @param destination_id id of the message's destination
     * @param protocol  routing protcol
     * @param actor_x   actor x coordinate
     * @param actor_y   actor y coordinate
     * @param data message's payload
     */
    public PPDU(long time_stamp, String source_id, String destination_id, char protocol, Double actor_x, Double actor_y, String data) {
        this.time_stamp = time_stamp;
        this.source_id = source_id;
        this.destination_id = destination_id;
        this.protocol = protocol;
        this.actor_x = actor_x;
        this.actor_y = actor_y;
        this.read = false;
        this.sent = false;
        this.data = data;
    }
    
    /** Constructor: creates a Physical PDU from a String.
     * 
     * @param msg a String containing the mandatory parameters
     */
    public PPDU(String msg){
        
        String[] msgParts = msg.split(d);
        
        this.time_stamp = Long.valueOf(msgParts[0]);
        this.source_id = msgParts[1];
        this.destination_id = msgParts[2];
        this.protocol = msgParts[3].charAt(0);
        this.actor_x = Double.valueOf(msgParts[4]);
        this.actor_y = Double.valueOf(msgParts[5]);
        this.read = false;
        this.sent = true;
        this.data = msgParts[6];
    }

    /** Constructor: creates a copy of a Physical PDU.
     * 
     * @param m a Physical PDU
     */
    public PPDU(PPDU m) {
        this.time_stamp = m.time_stamp;
        this.source_id = m.source_id;
        this.destination_id = m.destination_id;
        this.actor_x = m.actor_x;
        this.actor_y = m.actor_y;
        this.data = m.data;
        this.read = m.read;
        this.sent = m.sent;
        this.protocol = m.getProtocol();
    }   
    
    /** Retrieves the PDU's timestamp.
     * 
     * @return a time stamp
     */
    public long getTime_stamp() {
        return time_stamp;
    }

    /** Retrieves the PDU's source node.
     * 
     * @return a node id
     */
    public String getSource_id() {
        return source_id;
    }

    /** Retrieves the PDU's destination.
     * 
     * @return a node id
     */
    public String getDestination_id() {
        return destination_id;
    }

    /** Retrieves the PDU's protocol.
     * 
     * @return a routing protocol
     */
    public char getProtocol() {
        return protocol;
    }
    
    /** Retrieves the actor's x coordinate.
     * 
     * @return a x coordinate
     */
    public Double getActor_x() {
        return actor_x;
    }

    /** Retrieves the actor's y coordinate.
     * 
     * @return a y coordinate
     */
    public Double getActor_y() {
        return actor_y;
    }
    
    /** Tells if packet was already read.
     * 
     * @return a boolean
     */
    public boolean isRead() {
        return read;
    }
    
    /** Tells if packet was already sent.
     * 
     * @return a boolean
     */
    public boolean isSent() {
        return sent;
    }
    
    /** Retrieves the PDU's payload.
     * 
     * @return a payload
     */
    public String getData() {
        return data;
    }
    
    /**
     * Changes the PDU's time stamp.
     * 
     * @param time_stamp a new timestamp
     */
    public void setTime_stamp(long time_stamp) {
        this.time_stamp = time_stamp;
    }
    
    /**
     * Changes the PDU's source id.
     * 
     * @param source_id a new source id
     */
    public void setSource_id(String source_id) {
        this.source_id = source_id;
    }

    /**
     * Changes the PDU's destination id.
     * 
     * @param destination_id a new destination
     */
    public void setDestination_id(String destination_id) {
        this.destination_id = destination_id;
    }
    
    /**
     * Changes the actor's x coordinate.
     * 
     * @param actor_x a new x coordinate
     */
    public void setActor_x(Double actor_x) {
        this.actor_x = actor_x;
    }

    /**
     * Changes the actor's y coordinate.
     * 
     * @param actor_y a new y coordinate
     */
    public void setActor_y(Double actor_y) {
        this.actor_y = actor_y;
    }
    
    /**
     * Changes message sent parameter.
     * 
     * @param sent a boolean
     */
    public void setSent(boolean sent) {
        this.sent = sent;
    }

    /**
     * Changes message payload.
     * 
     * @param data a new payload
     */
    public void setData(String data) {
        this.data = data;
    }
    
    /**
     * Changes message read parameter.
     * 
     * @param read a boolean
     */
    public void setRead(boolean read) {
        this.read = read;
    }

    /**
     * Converts PDU to a String.
     * 
     * @return String containing all the packet's parameters
     */
    public String toStringToPacket() {
        return time_stamp + d + source_id + d + destination_id + d + protocol + 
                d + actor_x + d + actor_y + d + data;
        
    }
    
    @Override
    public String toString() {
        return time_stamp + d + source_id + d + destination_id + d + protocol + 
                d + actor_x + d + actor_y + d + read + d + sent + d + data;
    }
    
    
}
