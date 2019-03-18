/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.network;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.application.APDU;
import um.simulator.comm.link.*;
import um.simulator.core.SimStatus;

/** This class represents the Epidemic protocol.
 * Always works in broadcast. The message keeps being realyed until it's TTL reaches 0.
 * 
 * @author joaop
 * @version 1.0
 */
public class Epidemic extends NetworkProtocol {

    /** Consctructor: creates an Epidemic protocol.
     * 
     * @param linkLayer reference to the link layer
     * @param id    id of the node running this protocol
     * @param lineQueue  buffer size limit
     */
    public Epidemic(LinkLayer linkLayer, String id, int lineQueue) {
        super(linkLayer, id, lineQueue);
        this.protocolName = "Epidemic";
    }

    /** Updates the TTL of the messages. 
     * Every TTL/4 seconds, sends the messages in the <code>waitLine</code>. 
     */
    @Override
    public void run() {
        int i = 0;
        while (running) {
            i++;
            /** TTL removed from cicle */
            if (i > 20) {
                /** when the counter reaches a quarter of the TTL sends the messages in the buffer */
                try {
                    /** sends messages in the waitLine */
                    sendDownQueue();
                } catch (ParseException ex) {
                    Logger.getLogger(Epidemic.class.getName()).log(Level.SEVERE, null, ex);
                }
                i = 0;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Epidemic.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void generatedDataPacket(String destAddress, APDU apdu){
        
        NPDU npdu = new NPDU(nodeID, destAddress, System.currentTimeMillis() + "", "Epidemic", TTL, apdu.getData());
        /** it's epidemic, so it's destination is broadcast */
        linkLayer.sendFrame(npdu, "Broadcast");
        addToBuffer(npdu);
        SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
    }

    @Override
    public ArrayList<APDU> sendPacketsUp(ArrayList<NPDU> frame) {
        /** clears the list of frames to be sent to the application layer */
        dataToApp.clear();
        for (NPDU npdu : frame) {
            String destAdd = npdu.getDestinationAddress();
            String sourceAdd = npdu.getSourceAddress();
            /** if the waitLine contains the NPDU or the NPDU was already received, 
             * the NPDU is descarted and then it is created a entry on the log */
            if (isInBuffer(npdu) || (history.contains(npdu.getId() + sourceAdd))) 
            {
                boolean isDestination = false;
                if (destAdd.equals(nodeID)){ // stats
                    isDestination = true;
                }
                SimStatus.reportLocal.reportDroppedDuplicateNetworkMessage(nodeID,npdu.getData(),buffer.size(), isDestination);
            } else { 
                /** if the NPDU is new, it's analised */
                try {
                    analyze(npdu);
                } catch (ParseException ex) {
                    Logger.getLogger(Epidemic.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return dataToApp;
    }

    /**
     * Analyzes the NPDU and decides what to do with it.
     * 
     * @throws ParseException if a parsing error occurs
     * @param npdu packet to analyze
     */
    public void analyze(NPDU npdu) throws ParseException {
        
        /** if the NPDU is valid it is analysed
         * else is dropped, added to the history and logged */
        if (npdu.getDestinationAddress().equals(nodeID) || npdu.getDestinationAddress().endsWith("Broadcast")) {
            /** if the destination is this node, the message is sent to the application layer */
            dataToApp.add(new APDU(npdu.getData()));
            /** the NPDU is desencapsuled and the APDU is generated and added 
             * to the list of frames to be sent to the application layer */
            SimStatus.reportLocal.reportReceivedMessage(nodeID,3,npdu.getData(), buffer.size());
            history.add(npdu.getId() + npdu.getSourceAddress());
        } else { 
            /** if it isn't the final destination, the NPDU is added to the waitLine */
            addToBuffer(npdu);
        }
            
    }

    /** Checks queue for active messages and sends them 
     * @throws  ParseException if a parsing error occurs
     */
    public void sendDownQueue() throws ParseException {

        NPDU npdu;
        long dateStored; 

        synchronized (buffer) {
            for (int i = 0; i < buffer.size(); i++) {
                npdu = buffer.get(i);
                dateStored = bufferTimes.get(i);
                npdu.updateTTL(dateStored);

                if (npdu.isValid_ttl()) {
                    /** if npdu is valid the message is sent and logged */
                    /** it's epidemic, so it's destination is broadcast */
                    sendDataPacket(npdu, "Broadcast");
                    SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
                } else {
                    /** if is not valid is dropped and logged */
                    SimStatus.reportLocal.reportDroppedTTLNetworkMessage(nodeID,npdu.getData(),buffer.size());
                    buffer.remove(i);
                    bufferTimes.remove(i);
                    i--;
                }
            }
        }
    }
    
    @Override
    public void sendDataPacket(NPDU npdu, String destAddress){
        this.linkLayer.sendFrame(npdu, destAddress);
    }

}
