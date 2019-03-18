package um.simulator.core;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.core.communications.MulticastStatusReceiver;
import um.simulator.core.communications.MulticastStatusSender;
import um.simulator.core.communications.TCPClient;
import um.simulator.actor.*;
import um.simulator.core.communications.TCPMessage;
import um.simulator.map.GlobalMap;
import um.simulator.core.communications.MulticastMessageReceiver; 
import um.simulator.core.communications.MulticastMessageSender; 
import um.simulator.reporting.ReportLocal;

/**
 * This class starts the Local Coordinator part of the simulator.
 * Connects to the Global Coordinator.
 * 
 * @author XT17 
 * @author Adriano Moreira 
 * @author Maria João Nicolau
 * @author luisacabs
 * @version 1.0
 * 
 */
public class LocalCoordinator {

    static TCPClient tcpLink;
    static MulticastStatusReceiver multicastReceiver;
    static MulticastStatusSender multicastSender;
    
    static MulticastMessageReceiver multicastMessageReceiver; 
    static MulticastMessageSender multicastMessageSender; 
    
    public static ReportLocal report;

    /** 
     * Constructor: Reads configuration file and initializes <code>Communications</code>.
     */
    public LocalCoordinator() {
        
        /** A - Loads and reads the configuration parameters */
        System.out.println("  1. Loading and reading the configuration parameters...");
        Properties prop = new Properties();
        String settingsFile="input/settings.properties";
        try{
            prop.load(new FileInputStream(settingsFile));
        }
        catch(IOException e){
            System.out.println("Error reading configuration file "+ settingsFile);
            System.exit(0);
        }
        String simName = prop.getProperty("Global.Name");
        System.out.println("     - Simulation name: " + simName);
        String typeLogging = prop.getProperty("Logging.Type");
        
	String coordIP = prop.getProperty("GlobalCoordinator.IP");
	int coordPort = Integer.parseInt(prop.getProperty("GlobalCoordinator.port","7575"));
        
        String multicastAddress = prop.getProperty("Multicast.IP");        
	int multicastPort = Integer.parseInt(prop.getProperty("Multicast.port","7070"));
        
        String multicastMessageAddress = prop.getProperty("MulticastMessage.IP");
	int multicastMessagePort = Integer.parseInt(prop.getProperty("MulticastMessage.port","7171"));
        
	/** B - Creates the TCP Client */
	tcpLink = new TCPClient(coordIP, coordPort, this, "localCoordinator");

	/** C - Creates the Multicast Receiver */
	multicastReceiver = new MulticastStatusReceiver(multicastAddress, multicastPort);

	/** D - Creates the Multicast Sender */
        multicastSender = new MulticastStatusSender(multicastAddress, multicastPort);
        int port = multicastSender.getLocalPort();
        multicastReceiver.setMSPort(port);
        
        /** E - Creates the Multicast Message Receiver */
	multicastMessageReceiver = new MulticastMessageReceiver(multicastMessageAddress, multicastMessagePort);

	/** F - Creates the Multicast Message Sender */
        multicastMessageSender = new MulticastMessageSender(multicastMessageAddress, multicastMessagePort);
        port = multicastMessageSender.getLocalPort();
        multicastMessageReceiver.setMSPort(port);
        
        
    }

    /** 
     * Starts and monitors the local simulation. 
     * 
     * @param args arguments
     * @throws IOException if an IO error occurs
     */
    public static void main(String[] args) throws IOException {
        System.out.println("LocalCoordinator is starting...");
        
        LocalCoordinator localCoordinator = new LocalCoordinator();

        /** Application logic: */
        
        /** 2 - Starts the TCP Client */
        System.out.println("  2. Starting the TCPClient...");
        tcpLink.start();
        
        /** 3 - Waits for the connection with the Global Coordinator to be ready */
        System.out.println("  3. Waiting for Global Coordinator...");
        
        /** 4 - Starts the MulticastReceiver */
        System.out.println("  4. Starting the MulticastReceiver...");
        multicastReceiver.start();
        
        try {
            /** 5 - Starts the ReportLocal */
            synchronized(tcpLink){
                tcpLink.wait();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(LocalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("  5. Starting the ReportLocal...");
        if(SimStatus.physical | SimStatus.network | SimStatus.application){
            SimStatus.reportLocal = new ReportLocal(true);
        }
        
        /** 6 - Waits until the Global Coordinator sends the first actor */
	int nLocalActors = SimStatus.localActorsList.size();
        
        System.out.println("  6. Waiting for the first actor to arrive:");
        while(nLocalActors == 0 && SimStatus.running) {
            nLocalActors = SimStatus.localActorsList.size();
            try {
                Thread.sleep(1000);
                System.out.print(".");
            } catch (InterruptedException ex) {
               System.out.print("Error in LocalCoordinator: " + ex);
            }
        }
        
        if(SimStatus.running){
            /** 6 - Starts the Multicast Sender */
            System.out.println("  7. Received first actor: starting the Multicast Sender...");
            multicastSender.start();

            /** 7 - Starts the Multicast Message Receiver */
            System.out.println("  8. Starting the MulticastMessageReceiver...");
            multicastMessageReceiver.start();

            /** 8 - Starts the Multicast Message Sender */
            System.out.println("  9. Starting the MulticastMessageSender...");
            multicastMessageSender.start();
        }
	
        try {
            tcpLink.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(LocalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Handles a message reception by the <code>TCPClient</code>.
     * 
     * @param message   The message to process.
     * @throws InterruptedException if an interruption occurs
     */
    public void receiveMessage(TCPMessage message) throws InterruptedException {
        
        /** this is an order to create a new actor */
        if(message.messageType.equals("02")) {
            createNewActor(message.messageStr);
        /** if message received is "05", starts dying process */    
        }else if(message.messageType.equals("05")){
            SimStatus.running = false;
            System.out.println("\t\t\t-Killing all local actors...");
            for(String actor : SimStatus.localActorsList){
                /** sets its local actors to dying mode */
                SimStatus.globalActors.get(actor).setDying(true);
            }
            synchronized(SimStatus.localActorsList){
                if(SimStatus.localActorsList.size()>0){
                    try{
                        System.out.println("\t\t\t-Waiting for local actors to die...");
                        SimStatus.localActorsList.wait();
                    } catch(InterruptedException e){
                    }
                }
            }
            System.out.println("\tAll local actors are dead!");
          
             
            SimStatus.reportLocal.closeReporting(); 
            String counters = SimStatus.reportLocal.getCounters();
            File reportFile = SimStatus.reportLocal.getReport();
            try {
                byte[] fileContent = Files.readAllBytes(reportFile.toPath());
                TCPMessage msgToSend = new TCPMessage("06");
                msgToSend.messageStr = counters;
                msgToSend.fileContent = fileContent;
            
            
         
                multicastReceiver.stopMulticastReceiver();
                multicastReceiver.join();
                multicastSender.stopMulticastSender();
                multicastSender.join();
                multicastMessageReceiver.stopMulticast();
                multicastMessageReceiver.join();
                multicastMessageSender.stopMulticast();
                multicastMessageSender.join();
                System.out.println("\tMulticast Communications are over!");
                tcpLink.sendMsg(msgToSend);
                tcpLink.disconnect();
            } catch (IOException ex) {
                Logger.getLogger(LocalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
        } else {   
            /** this messages contains a map */
            if (message.messageType.equals("04")) {
                GlobalMap gm = new GlobalMap(message.map);
                String mapName = message.messageStr;
                if (gm != null) {
                    SimStatus.addMap(mapName, gm);
                }
            }
            else     
                System.out.println("Unexpected packet type received: "+message.messageType);
        }
    }
    
    /**
     * Request a map to <code>GlobalCoordinator</code>.
     * Waits for response.
     * 
     * @param mapName   the name of the requested map.
     * @return the map object
     */
    public static GlobalMap getMap (String mapName) {
        if(!SimStatus.maps.containsKey(mapName)) { 
            /** sends a message requesting the missing map */
            TCPMessage msgToSend = new TCPMessage("03");
            msgToSend.messageStr = mapName;
            tcpLink.sendMsg(msgToSend);
            while (!SimStatus.maps.containsKey(mapName)) {
                try {
                    Thread.sleep(1000);
                    System.out.print(".");
                } catch (InterruptedException ex) {
                    System.out.print("Error in LocalCoordinator: " + ex);
                }
            }
        }
        return SimStatus.maps.get(mapName);
    }
    
    /**
     * Creates new actor after being told to do so by <code>GlobalCoordinator</code>.
     * 
     * @param newActorDescription   A set of parameters to configure new actors.
     */
    public void createNewActor(String newActorDescription) {
        String[] actorParams = newActorDescription.split(":");
        String newActorID = actorParams[0];
        if(newActorID.startsWith("Gen")) {
            Actor actor = new Actor(newActorDescription);
            SimStatus.registerLocalActor(newActorID);
            actor.start();
        }
        else if(newActorID.startsWith("Ran")) {
            ActorRandom actor = new ActorRandom(newActorDescription);
            actor.start();
        }
        else if(newActorID.startsWith("Tram")){
            ActorTram actor = new ActorTram(newActorDescription);
            actor.start();
        }
        else if(newActorID.startsWith("Ped")){
            ActorPedestrian actor = new ActorPedestrian(newActorDescription);
            actor.start();
        }
        else if(newActorID.startsWith("Car")){
            ActorCar actor = new ActorCar(newActorDescription);
            actor.start();
        }
         else if(newActorID.startsWith("Bus")){
            ActorBus actor = new ActorBus(newActorDescription);
            actor.start();
        }
         else if(newActorID.startsWith("Traf")){
            if(!SimStatus.localActorsList.contains(newActorID)){
                ActorTrafficLight tf = new ActorTrafficLight(newActorDescription);
                tf.start();
            }
            
        }
        else {
            System.out.println("WARNING: invalid type of actor: " + newActorID + ". Not created!");
        }
    }
}

