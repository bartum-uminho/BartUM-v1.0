/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.network;

import java.io.PrintWriter;
import java.util.ArrayList;
import um.simulator.comm.application.APDU;
import um.simulator.comm.link.LinkLayer;
import um.simulator.core.SimStatus;

/**
 * This class represents a generic Network Protocol.
 * 
 * @author joaop
 * @version 1.0
 */
abstract public class NetworkProtocol extends Thread {

    /** protocol identification */
    public String protocolName;
    
    /** actor address */
    String nodeID;

    /**
     * link layer object used as transmission interface
     *
     * @see Wave80211p
     */
    LinkLayer linkLayer;

    /** memory in which the data packets are saved to future retransmissions */
    final ArrayList<NPDU> buffer;

    /**
     * times when which messages were stored in buffer
     *
     * @see #buffer
     */
    ArrayList<Long> bufferTimes;

    /** list of APDU to be sent to the application layer */
    ArrayList<APDU> dataToApp;

    /** arrayList with the id of every message which destiny is this node */
    ArrayList<String> history;

    /** TTL of the new messages */
    public int TTL = 300; 
    /**
     * size limit of field buffer
     *
     * @see #buffer
     */
    public int bufferLimit;
    
    /**
     * Running flag. Controls if a protocol is running.
     * Once it goes false, the protocol is terminated.
     */
    public boolean running;
    
  
    ArrayList<Integer> bufferOcupationValues = new ArrayList<>();

    /** Constructor: creates a Network Protocol.
     * 
     * @param linkLayer reference to the link layer
     * @param id    id of the node running this protocol
     * @param lineQueue   buffer size limit
     */
    public NetworkProtocol(LinkLayer linkLayer, String id, int lineQueue) {
        this.protocolName = "protocolName";
        this.running = true;
        this.history = new ArrayList<>();
        this.dataToApp = new ArrayList<>();
        this.bufferTimes = new ArrayList<>();
        this.buffer = new ArrayList<>();
        this.linkLayer = linkLayer;
        this.nodeID = id;
        this.bufferLimit = lineQueue;
    }

    @Override
    public void run() {
    }

    /**
     * <code>WaveAppLayer</code> uses this procedure when a packet is generated.
     * Each protocol decides what to do when a data packet is generated by appLayer
     * 
     * @param destAddress the destination of the message
     * @param apdu the application pdu
     */
    public void generatedDataPacket(String destAddress, APDU apdu){
        
    }
    
    /**
     * Sends a packet to link layer.
     * 
     * @param destAddress the destination of the message
     * @param npdu the network pdu
     */
    public void sendDataPacket(NPDU npdu, String destAddress){
    }
    
  

    /**
     * Method called by the lower layers to pass packets to the application
     * layer.
     *
     * @param frame array containing network pdus
     * @return an array containing the packets to send
     */
    public ArrayList<APDU> sendPacketsUp(ArrayList<NPDU> frame) {
        return dataToApp;
    }

    /**
     * Calculates the mean value of an <code>ArrayList</code> of integers.
     *
     * @param list
     * @return the mean value of the list 
     */
    private double getMean(ArrayList<Integer> list) {
        double sum = 0;
        int list_size = list.size();
        if (list_size == 0) {
            return 0;
        }
        for (int i = 0; i < list_size; i++) {
            sum += list.get(i);
        }
        return sum / list_size;
    }

    /**
     * Adds a message to buffer.
     *
     * @param m packet to add
     */
    public void addToBuffer(NPDU m) {
        synchronized (buffer) {
            /** checks if queue is not full or if a NPDU can be dropped from queue */
            if (buffer.size() < bufferLimit || dropOneInvalidNPDU()) {
                /** adds a packet to the buffer */
                buffer.add(m);
                bufferTimes.add(System.currentTimeMillis());
            } else { 
                /** drops an invalid NPDU from buffer */
                SimStatus.reportLocal.reportDroppedBufferNetworkMessage(nodeID,m.getData(),buffer.size());
            }
        }
    }

    /**
     * Tries to drop one NPDU that has a TTL less or equal to zero.
     *
     * @return if dropped returns true, otherwise false
     */
    public synchronized boolean dropOneInvalidNPDU() {
        /** time when NPDU was added in downQueue */
        long lastTimeUpdated; 
        int i = 0;
        NPDU npdu;
        synchronized (buffer) {
            while (i < buffer.size()) {
                npdu = buffer.get(i);
                lastTimeUpdated = bufferTimes.get(i);
                npdu.updateTTL(lastTimeUpdated);
                if (!npdu.isValid_ttl()) {
                    buffer.remove(i);
                    bufferTimes.remove(i);
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    /**
     * Verify if a NPDU is in the Buffer.
     * 
     * @param npdu network pdu
     * @return true if the npdu is in buffer, and false otherwise
     */
    public boolean isInBuffer(NPDU npdu) {
        String searchID = npdu.getId();
        String sourceAdd = npdu.getSourceAddress();
        synchronized (buffer) {
            for (NPDU n : buffer) {
                if ((n.getId() + n.getSourceAddress()).equals(searchID + sourceAdd)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Adds the number of messages in the Buffer to an arrayList. 
     * Later the buffer occupation rate can be calculated
     */
    public void getBufferOcupation() {
        bufferOcupationValues.add(buffer.size());
    }

    /**
     * Ends the protocol execution.
     */
    public void endProtocol(){
        this.running = false;
    }
}
