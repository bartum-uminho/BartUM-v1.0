package um.simulator.core.communications;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.actor.ActorStatus;
import um.simulator.core.SimStatus;

/**
 * This class listens to the multicast packets and updates the <code>SimStatus</code> accordingly.
 * @author XT17
 * @author  Adriano Moreira
 * @author luisacabs
 * @version 1.0
 */
public class MulticastStatusReceiver extends Thread {
    InetAddress multicastGroup;
    int multicastPort;
    /** port used in multicast sender running in this machine */
    int multicastSenderPort = -1;
    MulticastSocket s;
    /** to check if the message comes from this machine */
    String localIP;
    /** True if the multicast receiver is running; set to false to stop it*/
    static boolean runningQ; 
    /** If set to true, by calling the pauseReception method, 
     * the reception and processing of new packets is suspended 
     * until the method resumeReception is called */
    private boolean pause = false; 

    
    /**
     * Constructor: creates a multicast receiver with the given IP and port number.
     * @param multicastAddress A multicast IP address.
     * @param multicastPort A port number from where to listen to multicast datagrams.
     */
    public MulticastStatusReceiver(String multicastAddress, int multicastPort) {
        /** Multicast receiver is running */
        runningQ=true;
        this.multicastPort = multicastPort;
        try {
            multicastGroup = InetAddress.getByName(multicastAddress);
            s = new MulticastSocket(multicastPort);
            System.out.println("\tMulticast Address is: " + multicastAddress);
            System.out.println("\tMulticastGroup: " + multicastGroup.getHostAddress());
            s.joinGroup(multicastGroup);
            localIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException ex) {
            System.out.println("Error1 creating MulticastReceiver:\n" + ex);

        }
        catch (IOException ioE) {
            System.out.println("Error2 creating MulticastReceiver:\n" + ioE);

        }
    }
    
    /**
     * Sets the multicast sender port number.
     * To check if the incoming messages are its own.
     * 
     * @param port A port number from where to listen to multicast datagrams.
     */
    public void setMSPort (int port) {
        multicastSenderPort = port;
    }
    
    /**
     * Waits for the arrival of datagrams. 
     * Process the datagrams and updates <code>SimStatus</code> accordingly.
     */
    @Override
    public void run() {
        DatagramPacket dp = new DatagramPacket(new byte[1472],1472);
        String packet;
        String[] packetParts;
        ActorStatus as;
        try {
            /** waiting for the arrival of multicast packets*/
            while(runningQ) {
                /** if instructed to pause, waits until instructed otherwise (when <code>resumeReception</code> is called) */
                synchronized(this) {
                    if (pause) {
                        wait();
                    }
                }
                /** waits for the arrival of a new packet */ 
                s.receive(dp);
                
                
                /** checks if the packet is arriving from this same host, and discards it if so*/
                if( (!dp.getAddress().getHostAddress().equalsIgnoreCase(localIP)) ||
                    (dp.getAddress().getHostAddress().equalsIgnoreCase(localIP) && 
                    (dp.getPort() != multicastSenderPort))) {
                    /** packet is coming from other host*/
                    
                    packet = new String(dp.getData(),0,dp.getLength());
                    packetParts = packet.split("@");
                    
                    /** checks the packet type */
                    if(packetParts[0].equalsIgnoreCase("01")) { 
                        /** this is the first packet in a series, extracts part 1 (load/IP) and updates the <code>SimStatus</code> */
                        SimStatus.setMachineLoad(packetParts[1]); 
                        /** extracts the remaining packets (actor's status) and updates SimStatus */
                        for(int i=2; i<packetParts.length; i++) {
                            as = new ActorStatus(packetParts[i]);
                            SimStatus.setActorStatus(as);
                        }
                    }
                    
                    /** this is one of the subsequent packets (no load information)*/
                    else if(packetParts[0].equalsIgnoreCase("02")) { 
                        /** extracts the actor's status and updates SimStatus */
                        for(int i=1; i<packetParts.length; i++) {
                            as = new ActorStatus(packetParts[i]);
                            SimStatus.setActorStatus(as);
                        }
                    /** unknown packet type */    
                    }else{ 
                        System.out.println("MulticastReceiver.run(): received unknown packet type.");
                    }
                } 
            }
        }
        catch (IOException ioE){
            if(runningQ){
                System.out.println("Error in MulticastReceiver.run():\n" + ioE);
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(MulticastStatusReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Stops the reception of datagrams for good.
     */
    public void stopMulticastReceiver() {
        runningQ = false;
        s.disconnect();
    }
    
    /**
     * Pauses the reception of datagrams. Used by <code>SimScope</code> to pause <code>Visualization</code>.
     * While paused, <code>SimStatus</code> is not updated.
     */
    public void pauseReception() {
        pause = true;
    }
    
    /**
     * Resumes the reception of datagrams. 
     * Used by <code>SimScope</code> to resume <code>Visualization</code>.
     */
    public void resumeReception() {
        pause = false;
        synchronized(this) {
            notify();
        }
    }
    
    
}
