/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.core.communications;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import um.simulator.comm.physical.PPDU;
import um.simulator.core.SimStatus;

/**
 * This class listens to the multicast packets exchanged between nodes
 * and updates the <code>SimStatus</code> accordingly.
 * 
 * @author lmnd
 * @version 1.0
 */
public class MulticastMessageReceiver extends Thread {
    
    
    /** 4 digits. maximum message length =(1472-4) =1468 */
    static int MAXIMUMMSGLEN = 4;
    
    InetAddress multicastGroup;
    int multicastPort;
    int multicastSenderPort = -1;
    MulticastSocket s;
    String localIP;
    static boolean offMulti;
    
    ArrayList<String> localActorsList = new ArrayList<String>();

    
    /** Constructor: Creates a multicast message receiver with a given IP address 
     * and port number.
     * 
     * @param multicastAddress  A multicast IP address
     * @param multicastPort A port number from where to listen to multicast datagrams
     */
    public MulticastMessageReceiver(String multicastAddress, int multicastPort) {

        this.offMulti=true;
        this.multicastPort = multicastPort;
        try {
            multicastGroup = InetAddress.getByName(multicastAddress);
            s = new MulticastSocket(multicastPort);
            s.joinGroup(multicastGroup);
            localIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException ex) {
            System.out.println("Error1 creating MulticastMessageReceiver:\n" + ex);
        }
        catch (IOException ioE) {
            System.out.println("Error2 creating MulticastMessageReceiver:\n" + ioE);
        }
    }
    
    /**
     * Changes the multicast port.
     * 
     * @param port A new port number to listen to.
     */
    public void setMSPort (int port) {
        multicastSenderPort = port;
    }
    
    
    @Override
    public void run() {
        
        DatagramPacket dp = new DatagramPacket(new byte[1472],1472);
        String packet;
        try {
            /** waits for the arrival of multicast packets and process them */
            while(offMulti) {

                /** waits for the arrival of a new packet */
                s.receive(dp);
                
                /** takes a picture of the current list of local actors */
                synchronized(SimStatus.localActorsList){
                    localActorsList.addAll(SimStatus.localActorsList);
                }
                
                /** checks if the packet is arriving from this same host, 
                 * and discards it if it is */
                if( (!dp.getAddress().getHostAddress().equalsIgnoreCase(localIP)) ||
                    (dp.getAddress().getHostAddress().equalsIgnoreCase(localIP) && 
                    (dp.getPort() != multicastSenderPort))) {
                    packet = new String(dp.getData(), "UTF-8");
                    int messagenum = Integer.valueOf(packet.substring(0, 2));
                    int stringposition = 2;
                    for(int i = 0; i<messagenum;i++){
                        int msglen=0;
                        msglen = Integer.valueOf(packet.substring(stringposition,stringposition+MAXIMUMMSGLEN));
                        String msg = packet.substring(stringposition+MAXIMUMMSGLEN,stringposition+MAXIMUMMSGLEN+msglen);
                        PPDU message = new PPDU(msg);
                        /** checks if it's a broadcast message and if it is
                         * from a node managed by a different <code>LocalCoordinator</code> */
                        if(message.getDestination_id().equals("Broadcast") && !localActorsList.contains(message.getSource_id())){
                            SimStatus.addBroadcastMessageReceived(new PPDU(msg));
                            
                        }
                        /** checks if destination is a local actor and 
                         * source it is a remote actor - another <code>LocalCoordinator</code> */
                        else if(localActorsList.contains(message.getDestination_id()) && !localActorsList.contains(message.getSource_id())){
                            SimStatus.addMessage(new PPDU(msg));
                        }
                        stringposition=stringposition+MAXIMUMMSGLEN+msglen;
                    }
                } 
            } 

        }
        catch (IOException ioE){
            if(offMulti){
                System.out.println("Error in MulticastMessageReceiver.run():\n" + ioE);
            }
        }
    }
    
    /** Shuts down multicast message receiver. */
    public void stopMulticast(){
        offMulti=false;
        s.close();
    }
    
    
}
