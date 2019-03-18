/*
 * To change this template, choose Tools | Templates
 * and open the template inLink the editor.
 */
package um.simulator.core.communications;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.core.LocalCoordinator;
import um.simulator.core.SimStatus;

/**
 * This class supports the permanent communication (signaling) with <code>GlobalCoordinator</code>.
 * 
 * @author XT17
 * @author Adriano Moreira 
 * @author Maria João Nicolau
 * @author Laurent
 * @author luisacabs
 * @version 1.0
 * 
 */
public class TCPClient extends Thread {
    /** IP address and port of the GlobalCoordinator */
    String serverIP;
    int serverPort;
    public Socket socket = null;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    LocalCoordinator lCoord;
    /** specifies the entity this TCPClient belongs to */
    String type;
    /** IP address of this host */
    String localIP; 
    public boolean mapsReceived = false;
    /** true if tcp connection is up */
    public boolean connected = false;
    
    /**
     * Constructor: a tcp connection used by <code>LocalCoordinator</code>.
     * 
     * @param ip    An ip address to connect to
     * @param port  A port to connect to
     * @param lCoord    A <code>LocalCoordinator</code> 
     * @param type  A string specifying the entity using this connection
     */    
    public TCPClient(String ip, int port, LocalCoordinator lCoord, String type) {
        serverIP = ip;
        serverPort = port;
        this.lCoord = lCoord;
        this.type = type;
        
    }
    /**
     * Constructor: a tcp connection used by <code>SimScope</code>.
     * 
     * @param ip    An ip address to connect to
     * @param port  A port to connect to 
     * @param type  A string specifying the entity using this connection
     */
    public TCPClient(String ip, int port,String type) {
        serverIP = ip;
        serverPort = port;
        this.type = type;
    }
    
    /**
     * Connects to <code>GlobalCoordinator</code> and waits for the reception of datagrams.
     */
    @Override
    public void run() {
        TCPMessage message = new TCPMessage();
        try {
            /** sets up a TCP connection with the GlobalCoordinator */
            System.out.println("     - TCPClient.run(): Trying to connect to the GlobalCoordinator...");
            socket = new Socket(serverIP, serverPort);
            System.out.println("     - TCPClient.run(): TCP connection completed.");
            localIP = InetAddress.getLocalHost().getHostAddress();
            /** creates the input and output streams */
            oos = new ObjectOutputStream (socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream (socket.getInputStream());
        } 
        catch (IOException ioE) {
            System.out.println("TCPClient.run(): Connection problems in TCPClient");
            Runtime.getRuntime().halt(0);
        }
        try {
            /** announces itself and its type */
            message.messageType = "01";
            message.messageStr = type;
            oos.writeObject(message);
            oos.flush();
            /** waits for GlobalCoordinator's appList */
            if(type.contains("localCoordinator")) {
                while (!connected) {
                    HashMap<String,String> appList = (HashMap<String,String>)ois.readObject();
                    SimStatus.appList.putAll(appList);
                    boolean aux = (boolean) ois.readObject();
                    SimStatus.physical = aux; 
                    aux = (boolean) ois.readObject();
                    SimStatus.network = aux;
                    aux = (boolean) ois.readObject();
                    SimStatus.application = aux;
                    synchronized(this){
                        this.notify();
                    }
                    connected = true;
                }
                /** waits for message reception */
                while(connected) {
                    message = (TCPMessage) ois.readObject();
                    try {
                        lCoord.receiveMessage(message);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TCPClient.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } 
                ois.close();
                oos.close();
            /** if the tcp client is a visualization entity, the connection is closed after receiving the global maps*/
            }else{
                if(type.contains("visualization")) {
                    SimStatus.maps = (HashMap<String,um.simulator.map.GlobalMap>) ois.readObject();
                    mapsReceived = true;
                    ois.close();
                    oos.close();
                }
            }
        }        
        catch (UnknownHostException uhE) {
            System.out.println("Unable to connect with the Global Coordinator:\n" + uhE);
        }
        catch (IOException ioE) {
            if(connected)
                System.out.println("Connection problems with the GlobalCoordinator:\n" + ioE);
            
        }
        catch (ClassNotFoundException cnfE) {
            System.out.println("Connection problems with the GlobalCoordinator:" + cnfE);
        }
    }
    
    /**
     * Sends a packet to <code>GlobalCoordinator</code>.
     * @param msgToSend  a message to be sent
     */
    public synchronized void sendMsg(TCPMessage msgToSend) {
        try {
            oos.writeObject(msgToSend);
            oos.flush();
        }
        catch (IOException ioE) {
            System.out.println("Error sending TCP message:\n" + ioE);
        } 
    }   
    
    /** 
     * Set tcp link do disconnected.
     */
    public void disconnect(){
        this.connected = false;
    }
}
