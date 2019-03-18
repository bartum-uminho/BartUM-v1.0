package um.simulator.core.communications;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import um.simulator.core.GlobalCoordinator;
import um.simulator.core.SimStatus;
import um.simulator.map.GlobalMap;

/**
 * This class represents a dedicated thread for each tcp connection.
 * Handles requests from <code>LocalCoordinator</code> and <code>Visualization</code>.
 * 
 * @author XT17
 * @author ajcmoreira 
 * @author Maria João 
 * @author luisacabs
 * @version 1.0
 * 
 */
public class TCPServerThread extends Thread{
    
    Socket socket=null;
    ObjectInputStream ois = null;
    ObjectOutputStream oos = null;
    public InetAddress ipLocalCoordinator;
        
    /**
     * Constructor: creates a new thread for a given connection.
     * @param socket    A tcp connection
     */
    public TCPServerThread(Socket socket){
        this.socket=socket;
        try {
            oos = new ObjectOutputStream (socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream (socket.getInputStream());
        } 
        catch (IOException ioE) {
            System.out.println("Connection problems in TCPServerThread");
        }
    }
    /**
     * Handles clients requests
     */
    @Override
    public void run(){
        TCPMessage message = new TCPMessage();
        String messageType=null, messageBody=null, typeClient=null;
        Boolean connected = false;
        try {
            
            ipLocalCoordinator=socket.getInetAddress();
            while (!connected) {
                message = (TCPMessage) ois.readObject();
                messageType = message.messageType;
                if(messageType.equals("01")) {
                    messageBody = message.messageStr;
                    /** if client is a LocalCoordinator sends appList,
                     * reporting layer variables
                     * and waits for new messages */
                    if(messageBody.contains("localCoordinator"))
                    {
                       connected = true;
                       typeClient = "localCoordinator";
                       TCPServer.localCoordinators.put(ipLocalCoordinator.getHostAddress(), this);
                       SimStatus.setMachineLoad("0.0/" + ipLocalCoordinator.getHostAddress()); 
                       oos.writeObject(SimStatus.appList);
                       oos.writeObject(SimStatus.physical);
                       oos.writeObject(SimStatus.network);
                       oos.writeObject(SimStatus.application);
                       oos.flush();
                    }
                    /** if client is a Visualization sends maps and closes connection */
                    if(messageBody.contains("visualization"))
                    {
                       connected = true;
                       typeClient = "visualization";

                       Iterator mi = SimStatus.maps.keySet().iterator();
                       HashMap<String,GlobalMap> mapsToSend = new HashMap<>();
                       String mapId;
                       while (mi.hasNext()) {
                           mapId = (String) mi.next();
                           if (SimStatus.maps.get(mapId).beingUsedMap) {
                               mapsToSend.put(mapId, SimStatus.maps.get(mapId));
                           }
                       }
                       oos.writeObject(mapsToSend);
                       oos.flush();
                    }
                }
                else
                    System.out.println("Unexpected packet type: "+messageType);
            }
            if  (typeClient.equalsIgnoreCase("localCoordinator")) {
                while(connected) {
                    
                    message = (TCPMessage) ois.readObject();
                    if(message.messageType.equals("06")){
                        connected = false;
                    }
                    GlobalCoordinator.receiveMessage(this, message);   
                }
            }
            else {
                oos.close();
                ois.close();
            }
        }
        catch (IOException ioE) {
            if(connected)
                System.out.println("Connection establishment problems with " + messageBody + ioE.getLocalizedMessage());
        }
        catch (ClassNotFoundException cnfE) {
            if(connected)
                System.out.println("Connection establishment problems with " + messageBody + cnfE);
        }
    }
   
    /**
     * Sends a message to the client side of the socket.
     * @param msgToSend     a datagram packet to be sent
     */
    public synchronized void sendMsg (TCPMessage msgToSend) {
        try {
            oos.writeObject(msgToSend);
            oos.flush();
        } catch (IOException ioE) {
            System.out.println("Error sending TCP message" + ioE);
        }
    }
}