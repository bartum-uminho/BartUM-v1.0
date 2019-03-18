/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.core.communications;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.physical.PPDU;
import um.simulator.core.NetworkLogging;
import um.simulator.core.SimStatus;

/**
 * This class sends the multicast packets to other nodes
 * and updates the <code>SimStatus</code> accordingly.
 * 
 * @author lmnd
 * @version 1.0
 */
public class MulticastMessageSender extends Thread {
    
    static int TIMESENDMESSAGESSLEEP = 1000;
    
    /** 4 digits. maximum message length =(1472-4) =1468 */
    static int MAXIMUMMSGLEN = 4;
    
    static boolean offMulti;
    
    
    ArrayList<String> localActorsList = new ArrayList<String>();
    InetAddress multicastGroup;
    int multicastPort;
    MulticastSocket s;
    String localIP;
    
    /** Constructor: creates a multicast message sender with the given IP address and port.
     * 
     * @param multicastAddress A multicast IP address
     * @param multicastPort A port number to where the datagrams should be sent
     */
    public MulticastMessageSender (String multicastAddress, int multicastPort) {
        this.multicastPort = multicastPort;
        try {
            multicastGroup = InetAddress.getByName(multicastAddress);
            s = new MulticastSocket();
            s.joinGroup(multicastGroup);
            localIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException ex) {
            System.out.println("Error creating MulticastMessageSender:\n" + ex);
        }
        catch (IOException ioE) {
            System.out.println("Error creating MulticastMessageSender:\n" + ioE);
        }
    }
    
    /**
     * Returns the local port being used.
     * 
     * @return multicast sender port 
     */
    public int getLocalPort() {
        return (s.getLocalPort());
    }
    
    @Override
    public void run() {
        String packet = "";

        int msgcount = 0;
        
        try {           
            /** periodically broadcasts all messages */
            while(offMulti) {
                
                msgcount = 0;
                
                /** takes a picture of the current list of local actors */
                localActorsList.clear();
                synchronized(SimStatus.localActorsList){
                    localActorsList.addAll(SimStatus.localActorsList);
                }
                Object keys [] = SimStatus.globalMessages.keySet().toArray();
                /** for all message entries */
                for(Object actor_id_a: keys){
                    String actor_id = (String) actor_id_a;
                    /** if message is sent in broadcast */
                    if(actor_id.equals("Broadcast")){    
                        synchronized(SimStatus.globalMessages.get(actor_id)){                       
                            for(PPDU msg: SimStatus.globalMessages.get(actor_id)){
                                /** sets message as sent */
                                msg.setSent(true);
                                
                                if((packet.length() + msg.toStringToPacket().length()) + MAXIMUMMSGLEN+2 < 1472){ 
                                    packet += String.format("%04d",msg.toStringToPacket().length());
                                    packet += msg.toStringToPacket();
                                    msgcount++;
                                }  
                                else { 
                                    /** the current packet is almost full, so lets send it */
                                    packet = String.format("%02d", msgcount) + packet;
                                    sendPacket(packet);
                                    packet = new String();
                                    packet = String.format("%04d",msg.toStringToPacket().length());
                                    packet += msg.toStringToPacket();
                                    msgcount = 1;
                                }
                            }
                            /** clears all messages after sent */
                            SimStatus.globalMessages.get(actor_id).clear();
                        }

                    }
                    /** if it is a local actor */
                    else if(localActorsList.contains(actor_id)){
                       
                        synchronized(SimStatus.globalMessages.get(actor_id)){
                            
                            PPDU msg;
                            Iterator<PPDU> i = SimStatus.globalMessages.get(actor_id).iterator();
                            
                            while(i.hasNext()){
                                msg = i.next();
                                /** if it's not a broadcast message */    
                                if(!msg.getDestination_id().equals("Broadcast")){
                                    /** if message was read already */
                                    if(msg.isRead()){
                                        /** sets message as sent */
                                        msg.setSent(true);
                                        
                                        if((packet.length() + msg.toStringToPacket().length()) + MAXIMUMMSGLEN+2 < 1472){ 
                                            packet += String.format("%04d",msg.toStringToPacket().length());
                                            packet += msg.toStringToPacket();
                                            msgcount++;
                                            
                                        }  
                                        else { 
                                            /** the current packet is almost full, so lets send it */
                                            packet = String.format("%02d", msgcount) + packet;
                                            sendPacket(packet);
                                            packet = new String();
                                            packet = String.format("%04d",msg.toStringToPacket().length());
                                            packet += msg.toStringToPacket();
                                            msgcount = 1;
                                        }

                                        /** deletes message because it was already read and sent */
                                        i.remove();


                                    }
                                    /** if message was not read already */
                                    else{

                                        /** sets message as sent */
                                        msg.setSent(true);
                                        
                                        if((packet.length() + msg.toStringToPacket().length()) + MAXIMUMMSGLEN+2 < 1472){ 
                                            packet += String.format("%04d",msg.toStringToPacket().length());
                                            packet += msg.toStringToPacket();
                                            msgcount++;
                                        }  
                                        else { 
                                            /** the current packet is almost full, so lets send it */
                                            packet = String.format("%02d", msgcount) + packet;
                                            sendPacket(packet);
                                            packet = new String();
                                            packet = String.format("%04d",msg.toStringToPacket().length());
                                            packet += msg.toStringToPacket();
                                            msgcount = 1;
                                        }
                                    }
                                } 
                            }
                        }
                        
                        
                    }
                    /** it's a remote actor and there are messages */
                    else{
                        synchronized(SimStatus.globalMessages.get(actor_id)){
                            for(PPDU msg: SimStatus.globalMessages.get(actor_id)){

                                /** sets message as sent */
                                msg.setSent(true);
                                
                                if((packet.length() + msg.toStringToPacket().length()) + MAXIMUMMSGLEN+2 < 1472){ 
                                    packet += String.format("%04d",msg.toStringToPacket().length());
                                    packet += msg.toStringToPacket();
                                    msgcount++;
                                }  
                                else { 
                                    /** the current packet is almost full, so lets send it */
                                    packet = String.format("%02d", msgcount) + packet;
                                    sendPacket(packet);
                                    packet = new String();
                                    packet = String.format("%04d",msg.toStringToPacket().length());
                                    packet += msg.toStringToPacket();
                                    msgcount = 1;
                                }

                            }
                            /** deletes all messages */
                            SimStatus.globalMessages.get(actor_id).clear();
                        }    
                        
                    }
                    
                }
                
                if(msgcount!=0){
                    packet = String.format("%02d", msgcount) + packet;
                    sendPacket(packet);
                    packet = new String();
                }
                
                Thread.sleep(TIMESENDMESSAGESSLEEP);
            } 
        } 
        catch (InterruptedException ex) {
            System.out.println("Error in MulticastMessageSender:\n" + ex);
            NetworkLogging.log("warning","localStatus ip "+localIP+" multicast message sender failed: InterruptedException");
        }
    }

    /** Sends packet to other nodes.
     * 
     * @param packet A packet to be sent
     */
    private void sendPacket(String packet) {
        byte[] bufferToSend = new byte[1472];
        try {
            bufferToSend = packet.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(MulticastMessageSender.class.getName()).log(Level.SEVERE, null, ex);
        }

        DatagramPacket dp = new DatagramPacket(bufferToSend, bufferToSend.length, multicastGroup, multicastPort);
        try {
            s.send(dp);
        } catch (IOException ex) {
            System.out.println("Error in MulticastMessageSender, while sending a packet:\n" + ex);
        }
    }
    /** Shuts down multicast message sender. */
    public static void stopMulticast(){
        offMulti=false;
    }
}
