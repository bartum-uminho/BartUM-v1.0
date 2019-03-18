package um.simulator.core.communications;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import um.simulator.actor.ActorStatus;
import um.simulator.core.SimStatus;
import um.simulator.core.NetworkLogging;

/**
 * This class spreads the status of the local actors, using multicast. <br>
 * 
 * @author XT17 
 * @author Adriano Moreira
 * @author luisacabs
 * @version 1.0
 */
public class MulticastStatusSender extends Thread {

    InetAddress multicastGroup;
    int multicastPort;
    MulticastSocket s;
    /** IP address of this host */
    String localIP;
    /** True if the multicast sender is running; set to false to stop it*/
    static boolean runningQ;
    
    /**
     * Constructor: creates a multicast sender with the given IP address and port.
     * 
     * @param multicastAddress A multicast IP address
     * @param multicastPort A port number to where the datagrams should be sent
     */
    public MulticastStatusSender (String multicastAddress, int multicastPort) {
        this.multicastPort = multicastPort;
        runningQ=true;
        try {
            multicastGroup = InetAddress.getByName(multicastAddress);
            s = new MulticastSocket();
            s.joinGroup(multicastGroup);

            localIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException ex) {
            System.out.println("Error creating MulticastSender:\n" + ex);

        }
        catch (IOException ioE) {
            System.out.println("Error creating MulticastSender:\n" + ioE);

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
    
    /** spreads the actor's status periodically */
    @Override
    public void run() {
        
        ArrayList<String> localActorsList = new ArrayList<>();
        int load;
        String packetType;
        String packet;
        Iterator itr;
        ActorStatus as;
        String actorDescription;
        try {           
            while(runningQ) {
                localActorsList.clear();
                localActorsList.addAll(SimStatus.localActorsList);
                /** creates the first packet, starting with the machine load */
                load = SimStatus.localActorsList.size();
                packetType = "01";
                packet = packetType + "@" + load + "/" + localIP;
                /** all the subsequent types are "02" */
                packetType = "02";
                itr = localActorsList.iterator();
                while(itr.hasNext()) {
                    as = SimStatus.globalActors.get((String)itr.next());
                    /** checks if the actor has been removed from globalActors */
                    if(as != null) { 
                        actorDescription = as.toString();
                        /** checks if new actor fits into the current packet */
                        if((packet.length() + actorDescription.length()) < 1472) 
                            packet = packet + "@" + actorDescription;
                        /** if the current packet is almost full, sends it */
                        else { 
                            sendPacket(packet);
                            packet = packetType + "@" + actorDescription;
                        }
                    }
                }
                /** sends the final packet */
                sendPacket(packet);
                Thread.sleep(100);
            } 
        }
        catch (InterruptedException ex) {
            System.out.println("Error in MulticastSender:\n" + ex);
            NetworkLogging.log("warning","localStatus ip "+localIP+" multicast sender failed: InterruptedException");
        }
    }

    /**
     * Sends a multicast datagram.
     * 
     * @param packet A string representing the data to be sent.
     */
    private void sendPacket(String packet) {
        byte[] bufferToSend;
        bufferToSend = packet.getBytes();
        DatagramPacket dp = new DatagramPacket(bufferToSend, bufferToSend.length, multicastGroup, multicastPort);
        try {
            s.send(dp);

        } catch (IOException ex) {
            System.out.println("Error in MulticastSender, while sending a packet:\n" + ex);
        }
    }
    /**
     * Stops the reception of datagrams for good.
     */
    public static void stopMulticastSender() {
        runningQ = false;
    }
}
