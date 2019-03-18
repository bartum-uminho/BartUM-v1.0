package um.simulator.core.communications;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.core.NetworkLogging;
import um.simulator.core.SimStatus;
/**
 * This is a TCP server that runs on <code>GlobalCoordinator</code>. 
 * Accepts and keeps track of new connections from <code>LocalCoordinator</code>'s.
 * 
 * 
 * @author XT17 
 * @author Maria João Nicolau
 * @author luisacabs
 * @version 1.0
 * 
 * 
 */
public class TCPServer extends Thread 
{
    Integer port;
    public static HashMap<String, TCPServerThread> localCoordinators = new HashMap<>();
    public boolean listening = true;
    Socket socket = null;
    ServerSocket serverSocket = null;
    /**
     * Constructor: creates a new tcp server on the given port.
     * @param port  A port to accept connections
     */
    public TCPServer (int port)
    {
        this.port=port;
    }
    
    /**
     * Prints an error messagem.
     * @param msg   An error message
     */
    public static void error(String msg)
    {
	System.err.println("ERROR: " + msg);
	System.exit(1);
    }	
    
    /**
     * Accepts new connections.
     */
    @Override
    public void run(){
        String myIP=null, localCoordinatorIP;
        
        /** creates server socket */
        try 
        {
		serverSocket = new ServerSocket(port);
                myIP=InetAddress.getLocalHost().getHostAddress();
                NetworkLogging.log("config","globalCoordinator (ip:"+myIP+") opens tcp server socket on port "+port);
                System.out.println("\tGlobal Coordinator (ip:"+myIP+") opens tcp server socket on port "+port);
	} catch (IOException e) 
        {
		error("Open socket: " + e);
	}
     	
        /** waits for tcp clients to connect */
        while  (listening) 	
	{
            try 
            {
                socket = serverSocket.accept();
                localCoordinatorIP=socket.getInetAddress().getHostAddress();
                NetworkLogging.log("config","globalCoordinator (ip:"+myIP+") establishes a new connection with a tcp client on ip "+localCoordinatorIP);
                System.out.println("\n\tNew Connection: " + localCoordinatorIP);
                /** dedicated thread to handle tcp connection */
                TCPServerThread TCPServerThread = new TCPServerThread(socket);
                TCPServerThread.start();
            } catch (IOException e) {
                if(listening)
                    error(" Accept Conection: " + e);
            }
	}
        try {
            serverSocket.close();
        } catch (IOException e) {
            error(" Close socket: " + e);
        }
    }
    
    /**
     * Sends a command to the lightest load <code>LocalCoordinator</code>. 
     * @param newActorDescription   A set of parameters for the creation of a new actor
     */
    public static void createNewActor(String newActorDescription) {
        String ipMinLoad = SimStatus.getBestMachine();
        TCPServerThread tcpThread = localCoordinators.get(ipMinLoad);
        TCPMessage msgToSend = new TCPMessage ("02");
        msgToSend.messageStr = newActorDescription;
        tcpThread.sendMsg(msgToSend);
    }
    
    public void shutdown(){
        try {
            this.listening = false;
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(TCPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}