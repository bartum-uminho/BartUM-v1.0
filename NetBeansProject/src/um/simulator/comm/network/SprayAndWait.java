/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package um.simulator.comm.network;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.application.APDU;
import um.simulator.comm.link.LinkLayer;
import um.simulator.core.SimStatus;

/** This class represents the Spray and Wait protocol.
 * 1st phase (Spray Phase) - message is "broadcasted" to <code>L</code> nodes.
 * 2nd phase (Wait Phase) - waits for the message to be delivered do all <code>L</code> nodes.
 * 
 * @author joaop
 * @version 1.0
 */
public class SprayAndWait extends NetworkProtocol {
    
    /** number of confirmations received for each message which origin is this node */
    HashMap<String, Integer> nConfirmations = new HashMap<>(); 
    /** number of retransmission of each message */
    int L; 
    
    /** Constructor: creates a Spray and Wait protocol.
     * 
     * @param linkLayer reference to the link layer
     * @param id    id of the node running this protocol
     * @param lineQueue   buffer size limit
     * @param numberOfRetransmissions   number of nodes to spray the message
     */
    public SprayAndWait(LinkLayer linkLayer, String id, int lineQueue, int numberOfRetransmissions) {
        super(linkLayer, id, lineQueue);
        this.protocolName = "SprayAndWait";
        L = numberOfRetransmissions;
    }
    
    @Override
    public void run() {
        int i = 0;
        while (running) {
            synchronized (buffer) {
                i++; 
                if (i > 4) {
                    /** when the counter reaches TTL/4 sends the messages in the cache */
                    try {
                        /** sends the messages in the waitLine */
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
    }

    @Override
    public void generatedDataPacket(String destAddress, APDU apdu){
        /** creates the NPDU */
        NPDU npdu = new NPDU(nodeID, destAddress, System.currentTimeMillis() + "", "SprayAndWait", TTL, apdu.getData());
        /** it's in the first phase, so it's destination is broadcast */
        linkLayer.sendFrame(npdu, "Broadcast"); 
        /** adds to the confirmation array an entry containing the id of 
         * the message and the number of confirmations received */
        nConfirmations.put(npdu.getId() + npdu.getSourceAddress(), 0); 
        /** adds the message to the list of active messages to be sent */
        addToBuffer(npdu); 
        /** generates report of message sent */
        SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
    }
    
    @Override
    public ArrayList<APDU> sendPacketsUp(ArrayList<NPDU> frame) {
        /** clears the list of frames to be sent to the application layer */
        dataToApp.clear(); 
        for (NPDU npdu : frame) {
            String destAdd = npdu.getDestinationAddress();
            String sourceAdd = npdu.getSourceAddress();
            /** if the waitLine contains the NPDU or the NPDU was already received */
            if (isInBuffer(npdu) || (history.contains(npdu.getId() + sourceAdd))) { 
                boolean isDestination = false;
                if (destAdd.equals(nodeID)){ // stats
                    isDestination = true;
                }
                SimStatus.reportLocal.reportDroppedDuplicateNetworkMessage(nodeID,npdu.getData(),buffer.size(),isDestination);
            } else { /** if the NPDU is new, it's analyzed */
                try {
                    analyze(npdu);
                } catch (ParseException ex) {
                    Logger.getLogger(Epidemic.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return dataToApp;
    }
    
    /** Analyzes the NPDU and decides what to do with it.
     * 
     * @param npdu  a network pdu
     * @throws ParseException if a parsing error occurs
     */
    public void analyze(NPDU npdu) throws ParseException {
        String destAdd = npdu.getDestinationAddress();
        String sourceAdd = npdu.getSourceAddress();
        if (destAdd.equals(nodeID)) { 
            /** if the destiny is this node, the message if its new is sent to the application layer, 
             * if is a confirmation the number of confirmations os the message is increased
             */
            if (npdu.getData().startsWith("conf"))
            {
                /** if the data is a confirmation */
                String[] fields = npdu.getData().split(":");
                int aux = nConfirmations.get(fields[1]);
                nConfirmations.put(npdu.getId() + sourceAdd, aux + 1);
            } else {
                /** the NPDU is desencapsuled and the APDU generated is added to 
                 * the list of frames to be sent to the application layer */
                dataToApp.add(new APDU(npdu.getData()));
                SimStatus.reportLocal.reportReceivedMessage(nodeID,3,npdu.getData(),buffer.size());
                /** id of the message is added to the history */
                history.add(npdu.getId() + sourceAdd);
            }
        } else {
            /** if isn't the final destination, the NPDU is added to the buffer and
            * is sent a message confirming the reception of the message */
            /** confirmation of the reception of NPDU */
            NPDU aux = new NPDU(nodeID, sourceAdd, System.currentTimeMillis() + "",
                    "SprayAndWait", TTL, "conf:" + npdu.getId() + sourceAdd);
            linkLayer.sendFrame(aux, aux.getDestinationAddress());
            addToBuffer(npdu);
        }
    }
    
    /** Checks queue for active messages and send them.
     * @throws ParseException if a parsing error occurs
     */
    public void sendDownQueue() throws ParseException {
        NPDU npdu;
        long dateStored; 
        synchronized (buffer) {
            for (int i = 0; i < buffer.size(); i++) { 
                npdu = buffer.get(i);
                dateStored = bufferTimes.get(i);
                npdu.updateTTL(dateStored);
                /** if is valid the message is sent and logged */
                if (npdu.isValid_ttl()) { 
                    /** if the source is this node then it is necessary 
                     * to check the number of confirmations */
                    if (npdu.getSourceAddress().equals(nodeID)) { 
                        if (nConfirmations.get(npdu.getId() + npdu.getSourceAddress()) >= L) { 
                            /** wait phase */
                            sendDataPacket(npdu, npdu.getDestinationAddress());
                        } else { 
                            /** spray phase */
                            sendDataPacket(npdu, "Broadcast");
                        }
                        /** if the source is another node then the destination is unicast */
                    } else { 
                        sendDataPacket(npdu, npdu.getDestinationAddress());
                    }
                    SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
                } else { 
                    /** if is not valid is dropped and logged */
                    if (nConfirmations.containsKey(npdu.getId() + npdu.getSourceAddress())) {
                        nConfirmations.remove(npdu.getId() + npdu.getSourceAddress());
                    }
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
