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
import um.simulator.comm.link.LinkLayer;
import um.simulator.core.SimStatus;
import um.simulator.reporting.ReportLocal;

/** This class represents the Direct-Delivery protocol.
 * A very simple protocol - it only sends the message when it passes by 
 * the destination node (unicast)
 *
 * @author joaop
 * @version 1.0
 */
public class DirectDelivery extends NetworkProtocol {

    /** Constructor: creates a Direct-Delivery protocol.
     * 
     * @param linkLayer reference to the link layer
     * @param id    id of the node running this protocol
     * @param lineQueue   buffer size limit
     */
    public DirectDelivery(LinkLayer linkLayer, String id, int lineQueue) {
        super(linkLayer, id, lineQueue);
        this.protocolName = "DirectDelivery";
    }

    /** Updates the TTL of the messages. 
     * Every TTL/4 seconds, sends the messages in the <code>waitLine</code>. 
     */
    @Override
    public void run() {
        int i = 0;
        while (running) {

            i++;
            /** TTL removed from cycle */
            if (i > 4)
            {
                /** when the counter reaches a quarter of the TTL sends the messages in the buffer */
                try {
                    sendDownQueue();//sends the messages in the waitLine
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
        NPDU npdu = new NPDU(nodeID, destAddress, System.currentTimeMillis() + "", "directDelivery", TTL, apdu.getData());
        SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
        /** it's directDelivery, so it's destination is unicast */
        linkLayer.sendFrame(npdu, destAddress);
        /** adds the message to the list of active messages to be sent */
        addToBuffer(npdu);
    }

    
    @Override
    public ArrayList<APDU> sendPacketsUp(ArrayList<NPDU> frame) {
        /** clears the list of frames to be sent to the application layer */
        dataToApp.clear();
        for (NPDU npdu : frame) {
            String destAdd = npdu.getDestinationAddress();
            String sourceAdd = npdu.getSourceAddress();
            if (isInBuffer(npdu) || (history.contains(npdu.getId() + sourceAdd)))//if the waitLine contains the NPDU or the NPDU was already received, the NPDU is descarted and then it is created a entry on the log
            {
                boolean isDestination = false;
                if (destAdd.equals(nodeID)){ // stats
                    isDestination = true;
                }
                SimStatus.reportLocal.reportDroppedDuplicateNetworkMessage(nodeID,npdu.getData(),buffer.size(), isDestination);
                
            } else {
                /** if the destination is this node, then it's delivered, else is descarted */
                if (destAdd.equals(nodeID)) {
                    /** the NPDU is desencapsuled and the APDU generated is added to 
                     * the list of frames to be sent to the application layer */
                    dataToApp.add(new APDU(npdu.getData()));
                    SimStatus.reportLocal.reportReceivedMessage(nodeID,3,npdu.getData(),buffer.size());
                    history.add(npdu.getId() + sourceAdd);//id of the message is added to the history
                }
            }
        }
        return dataToApp;
    }

    /** checks queue for active messages and sends them 
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
                if (npdu.isValid_ttl()) {
                    sendDataPacket(npdu, npdu.getDestinationAddress());
                    /** it's DirectDelivery, so it's destination is unicast */
                    SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
                } else {
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
