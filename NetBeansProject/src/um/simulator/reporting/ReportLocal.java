/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.reporting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.network.NPDU;
import um.simulator.core.SimStatus;

/**
 * This class writes all message events in one file, and communication's getCounters in another.
 * All local coordinators write these two files on their system,
 * so after the simulation is over, each local much send their information
 * to the global coordinator in order to merge all of the data collected and
 * create two single files.
 * 
 * 1) MessageEvents.txt: In this file, each line correspond to a message event containing 
 * the following information:
 *  - TimeStamp: time (in milliseconds) at which the event occurred
 *  - ID: the id of the actor reporting the event
 *  - Info: type of Reporting
 *      S (Sent), R (Received), D (Dropped Duplicated Message), 
 *      T (Dropped exceeded TTL Message) and B (Dropped full Buffer Message)
 *  - Layer: layer at which the event occurred
 *    1 (Physical), 3 (Network), 7 (Application)
 *  - Data: data contained in the pdu  
 *  - Size of Buffer: used only in Network Layer reporting 
 * 2) MessageStats.txt
 * 
 * @author luisacabs
 * @version 1.0
 */
public class ReportLocal {
    private PrintWriter statistics;
    private boolean status;
    /** counters for received messages in each layer */
    private int nrofReceivedMsgPhy = 0;
    private int nrofReceivedMsgNet = 0;
    private int nrofReceivedMsgApp = 0;
    /** counters for dropped messages */
    private int nrofDroppedTTL = 0;
    private int nrofDroppedBuf = 0;
    private int nrofDroppedDup = 0;
    private int nrofDupDestination = 0;
    /** counter for sent messages in each layer */
    private int nrofSentMsgPhy = 0;
    private int nrofSentMsgNet = 0;
    private int nrofSentMsgApp = 0;
    /** list containing all message delays */
    private List<Long> msgDelay;
    private Long sumDelay = 0L;
    BufferedWriter bw;
    File f;

    /** 
     * Constructor: Creates an object to report message events and getCounters.
     * @param status    on/off report flag
     */
    public ReportLocal(boolean status) {
        this.status = status;
        if (status == true) {
            f = new File("reports/LocalMessageEvents.txt");
            msgDelay = new ArrayList<Long>();
            try {
                FileWriter fw = new FileWriter(f);
                bw = new BufferedWriter(fw);
                bw.write("TimeStamp\tID\tInfo\tLayer\tData\tSize of Buffer\n");
            } catch (IOException ex) {
                Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Reports pdu sent.
     * 
     * @param ActorID actor reporting this event
     * @param layer layer from which the reporting is called
     * @param pdu pdu sent
     * @param bufferSize buffer size (netork layer)
     */
    public synchronized void reportSentMessage(String ActorID,int layer,String pdu,int bufferSize){
        if (status) {
            boolean report = false;
            switch (layer) {
                case 1:  
                    if(SimStatus.physical){
                        nrofSentMsgPhy++;
                        report = true;
                    }
                    break;
                case 3:  
                    if(SimStatus.network){
                        nrofSentMsgNet++;
                        report = true;
                    }
                    break;
                case 7: 
                    if(SimStatus.application){
                        nrofSentMsgApp++;
                        report = true;
                    }
                    break;  
                default: 
                    break;
            }
            if(report){
                try {
                    bw.write(System.currentTimeMillis() + " " + ActorID + " S " + layer + " "
                            + pdu + " " + bufferSize + "\n");
                    
                } catch (IOException ex) {
                    Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }
    }

    /**
     * Reports the reception of a pdu.
     * 
     * @param ActorID   actor reporting this event
     * @param layer  layer from which the reporting is called
     * @param pdu   packet data unit received
     * @param bufferSize size of the buffer (network layer parameter)
     */
    public synchronized void reportReceivedMessage(String ActorID, int layer, String pdu, int bufferSize){
        if (status == true) {
            boolean report = false;
            switch (layer) {
                case 1:  
                    if(SimStatus.physical){
                        nrofReceivedMsgPhy++;
                        report = true;
                    }
                    break;
                case 3:  
                    if(SimStatus.network){
                        nrofReceivedMsgNet++;
                        report = true;
                    }
                    break;
                case 7: 
                    if(SimStatus.application){
                        nrofReceivedMsgApp++;
                        long delay = delayCalculator(pdu);
                        msgDelay.add(delay);
                        report = true;
                    }
                    break;  
                default: 
                    break;
            }
            if(report){
                try {
                    bw.write(System.currentTimeMillis() + " " + ActorID + " R " + layer + " "
                            + pdu + " " +bufferSize + "\n");
                } catch (IOException ex) {
                    Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Reports the discard of a duplicate npdu.
     * This method can only be called by the Network Layer.
     * 
     * @param ActorID actor reporting this event
     * @param npdu dropped npdu
     * @param bufferSize size of the buffer
     * @param destination flag to tell if the message was delivered to its final destination
     */
    public synchronized void reportDroppedDuplicateNetworkMessage(String ActorID, String npdu, int bufferSize, boolean destination){
       if (status && SimStatus.network) {
           try {
               bw.write(System.currentTimeMillis() + " " + ActorID + " D 3 " + npdu + " " +  bufferSize + "\n");
               nrofDroppedDup++;
               if(destination)
                   nrofDupDestination++;
           } catch (IOException ex) {
               Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
           }
        }
    }

    /**
     * Reports the discard of a npdu that reached its hop limit (TTL).
     * This method can only be called by the Network Layer.
     * 
     * @param ActorID actor reporting this event
     * @param npdu dropped npdu
     * @param bufferSize size of the buffer
     */
    public synchronized void reportDroppedTTLNetworkMessage(String ActorID,String npdu,int bufferSize){
        if (status && SimStatus.network) {
            try {
                bw.write(System.currentTimeMillis() + " " + ActorID + " T 3 " + npdu + " " +  bufferSize + "\n");
                nrofDroppedTTL++;
            } catch (IOException ex) {
                Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Reports the discard of a npdu when the current node's buffer is full.
     * This method can only be called by the Network Layer.
     * 
     * @param ActorID actor reporting this event
     * @param data dropped npdu
     * @param bufferSize size of the buffer
     */
    public synchronized void reportDroppedBufferNetworkMessage(String ActorID,String data,int bufferSize){
        if (status && SimStatus.network) {
            try {
                bw.write(System.currentTimeMillis() + " " + ActorID + " B 3 " + data + " " +  bufferSize + "\n");
                nrofDroppedBuf++;
            } catch (IOException ex) {
                Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Returns all counters concatenated.
     * @return message with all reporting counters
     * 
     */
    public String getCounters(){
        int i;
        for(i = 0;i<msgDelay.size();i++){
            sumDelay+=msgDelay.get(i);
        }
        String message;
        message = sumDelay + ":" + i + ":" + 
                nrofReceivedMsgPhy + ":" + nrofReceivedMsgNet + ":" +nrofReceivedMsgApp + ":" +
                nrofDroppedTTL + ":" + nrofDroppedBuf + ":" + nrofDroppedDup + ":" + nrofDupDestination + ":" +
                nrofSentMsgPhy + ":" + nrofSentMsgNet + ":" + nrofSentMsgApp;
        return message;
    }
    public File getReport(){
        return f;
    }
    
    /**
     * Computes the pdu delay.
     * @param pdu   received pdu
     * @return the pdu delay
     */
    public long delayCalculator(String pdu){
        long init,current, delay;
        init = Long.parseLong(pdu.split(":")[0]);
        current = System.currentTimeMillis();
        delay = current-init;
        return delay;
    }
    
    /**
     * Stops Reporting.
     * Closes 'MessageEvents.txt' and writes 'MessageStats.txt'.
     */
    public void closeReporting() {
        try {
            bw.close();
        } catch (IOException ex) {
            Logger.getLogger(ReportLocal.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
