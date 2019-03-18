package um.simulator.core;

import um.simulator.actor.GeneratorBus;
import um.simulator.actor.Generator;
import um.simulator.actor.GeneratorCar;
import um.simulator.actor.GeneratorTram;
import um.simulator.actor.GeneratorPed;
import um.simulator.actor.GeneratorTrafficLight;
import um.simulator.actor.GeneratorRan;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.core.communications.TCPMessage;
import um.simulator.map.GlobalMap;
import um.simulator.core.communications.MulticastStatusReceiver;
import um.simulator.core.communications.TCPServer;
import um.simulator.core.communications.TCPServerThread;
import um.simulator.reporting.ReportGlobal;
import um.simulator.reporting.ReportStatistics;

/**
 * This is the main class of the BartUM simulator. 
 * It coordinates all the aspects of the simulation.
 * This class starts the Global Coordinator part of the simulator in a particular computer.
 * 
 * @author XT17 
 * @author ajcmoreira 
 * @author Maria João Nicolau
 * @author luisacabs
 * @version 1.0
 * 
 */
public class GlobalCoordinator {
    
    /** local variables */
    private static Properties prop;
    public static TCPServer tcpServer;
    private static MulticastStatusReceiver multicastReceiver;
    private static int reportGlobalQ;
    private static int reportLocalQ;
    private static ReportGlobal reportGlobal;
    private static int simulationTime;
    static Thread timer;
    private static ReportStatistics statistics;
    private static FileOutputStream stream;
    
    
    /**
     * Constructor: Reads configuration file and initializes <code>Communications</code>.
     */
    public GlobalCoordinator() {
              
        /** A - Loads and reads the configuration parameters */
        System.out.println("  1. Loading and reading the configuration parameters...");
        prop = new Properties();
        String settingsFile="input/settings.properties";
        try{
            prop.load(new FileInputStream(settingsFile));
        }
        catch(IOException e){
            System.out.println("Error reading configuration file "+ settingsFile);
            System.exit(0);
        }
        String simName = prop.getProperty("Global.Name");
        System.out.println("\tSimulation name: " + simName);
        int coordPort = Integer.parseInt(prop.getProperty("GlobalCoordinator.port","7575"));
        String multicastAddress = prop.getProperty("Multicast.IP");
	int multicastPort = Integer.parseInt(prop.getProperty("Multicast.port","7171"));
        reportGlobalQ = Integer.parseInt(prop.getProperty("ReportGlobal.logQ"));
        simulationTime = Integer.parseInt(prop.getProperty("Global.Time"));
        
       
        /** B - Creates the TCPServer */
        tcpServer = new TCPServer(coordPort);
        
        /** C - Creates the MulticastReceiver */
        multicastReceiver = new MulticastStatusReceiver(multicastAddress, multicastPort);
        
    } 
    
    /**
     * Starts simulation.
     * Loads maps, starts <code>Communication</code>s, waits for the first <code>LocalCoordinator</code> 
     * to connect and starts the <code>Generator</code>s.
     * 
     * @throws IOException if an IO error occurs
     * @throws Exception if an exception occurs
     * @param args arguments
     */
    public static void main(String[] args) throws IOException, Exception {
        
        System.out.println("Starting the GlobalCoordinator...");
        GlobalCoordinator globalCoordinator = new GlobalCoordinator();

        /** Application logic */
        
        /** 2 - Loads the maps */
        System.out.println("  2. Loading the base maps...");
        /** reads number of maps */
        int n_map = Integer.parseInt(prop.getProperty("Map.Number"));
        
        
        /** for each map in the configuration file, gets the corresponding file name 
         * loads and parses it, and adds it to the SimStatus
         */
        int i = 1;
        ArrayList<String> mapNames = new ArrayList();
        while(i <= n_map) {
            String mapName = "Map." + i;
            String mapFile = prop.getProperty(mapName);
            if(!mapNames.contains(mapFile)){
                mapNames.add(mapFile);
                GlobalMap newMap;
                try {
                    newMap = new GlobalMap("input/"+mapFile);
                    newMap.baseMap = true;
                    SimStatus.addMap(mapName, newMap);
                } catch (IOException ex) {
                    System.out.println("GlobalCoordinator: Error loading map: " + mapName + ":\n" + ex);
                    System.exit(0);
                }
            }else{
                System.out.println("GlobalCoordinator: Error in configuration file.\nDuplicated Map: "+mapFile);
                System.exit(0);
            }
            i++;
        }
        
        /** 3 - Loads Applications*/
        System.out.println("  3. Loading Applications...");
        int n_app = Integer.parseInt(prop.getProperty("Application.Number"));
        i = 1;
        while(i<=n_app){
            String appName = prop.getProperty("Application."+i);
            String appDescription = appName;
            
            String appParams;
            if(appName.contains("BasicApplication")){
                appParams = prop.getProperty("Application" + i + ".Msg");
                appDescription+=":" + appParams;
                appParams = prop.getProperty("Application" + i + ".Destination");
                appDescription+= ":" + appParams;
                appParams = prop.getProperty("Application" + i + ".MsgGenStart");
                appDescription+=":" + appParams;
                appParams = prop.getProperty("Application" + i + ".MsgGenTime");
                appDescription+=":" + appParams;
                appParams = prop.getProperty("Application" + i + ".MsgBreakTime");
                appDescription+=":" + appParams;
                appParams = prop.getProperty("Application" + i + ".nrofMsgCycle");
                appDescription+=":" + appParams;
                appParams = prop.getProperty("Application" + i + ".matchApp");
                appDescription+=":" + appParams;
            }
            i++;
            SimStatus.appList.put(appName,appDescription);
        }
        
        /** 4 - Starts the TCPServer */
        System.out.println("  4. Starting the TCPServer...");
        tcpServer.start();
        
        /** 5 - Starts the MulticastReceiver */
        System.out.println("  5. Starting the MulticastReceiver...");
        multicastReceiver.start();
        
        /** 6 - Fetches reporting parameters for the Local Coordinators */
        reportLocalQ = Integer.parseInt(prop.getProperty("ReportLocal.logQ","0"));
        if(reportLocalQ !=0){
            System.out.println("  6. Local Coordinators Reporting ");
            int aux = Integer.parseInt(prop.getProperty("ReportLocal.logPhyQ","0"));
            setLayerReport(1,aux);
            aux = Integer.parseInt(prop.getProperty("ReportLocal.logNetQ","0"));
            setLayerReport(3,aux);
            aux = Integer.parseInt(prop.getProperty("ReportLocal.logAppQ","0"));
            setLayerReport(7,aux);
            /** 6.2 - Initializes ReportStatistics */
            statistics = new ReportStatistics(true);
        }else{
            System.out.println("  6. No Reporting in the Local Coordinators...");
            statistics = new ReportStatistics(false);
        }
        
        /** 7 - Waits for the first LocalCoordinator to connect */
        System.out.println("  7. Waiting for LocalCoordinators to connect:");     
        /** no LocalCoordinators yet */
        while(tcpServer.localCoordinators.isEmpty()) { 
            try {
                Thread.sleep(2000);
                System.out.print(".");
            } catch (InterruptedException ex) {
                System.out.print("Error in GlobalCoordinator: " + ex);
            }
        }
        
        /** 8 - Creates and starts the Generators */
        System.out.println("\n  8. Loading and starting the Generators...");
        globalCoordinator.startGenerators();
        
        /** 9 - Creates and starts the ReportingWriter */
        reportGlobalQ = Integer.parseInt(prop.getProperty("ReportGlobal.logQ","0"));
        int actsUpdatePace = Integer.parseInt(prop.getProperty("ReportGlobal.interval","200"));
        if(reportGlobalQ != 0) {
            System.out.println("  9. Start Reporting recording...");
            reportGlobal = new ReportGlobal(prop, actsUpdatePace, actsUpdatePace * 100);
            reportGlobal.start();
        }else{
            System.out.println("  9. No Reporting...");
        }
        
        
        
        /** 10 - Waits for commands */
        
        Scanner scanner = new Scanner(System.in);
        timer = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Thread.sleep(simulationTime*1000);
                    System.out.println("\n\tTIMEOUT");
                    SimStatus.running = false;
                    stopSimulation();
                    System.out.print("\tPress any key to end simulation: ");
                } catch (InterruptedException ex) {
                    Logger.getLogger(GlobalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        timer.start();
        
        while (SimStatus.running) {
            System.out.print("  9. Waiting for command: ");
            while(!scanner.hasNext());
            switch (scanner.nextLine()) {
                case "shutdown": 
                    if(SimStatus.running){
                        timer.stop();
                        stopSimulation();
                    }
                    break;
                default:
                    if(SimStatus.running)
                        System.out.println("Unknown command. Type help or ? for a list of valid commands.");
                    break;
            }
        }
    }
    
    /** 
     * Sets boolean values for Layer Reporting.
     * 
     * @param layer layer to set the report
     * @param layer 1 for reportGlobalQ ON, 0 for reportGlobalQ OFF
     */
    private static void setLayerReport(int layer, int on){
        boolean set = false;
        if(on!=0){
            set = true;
        }
        switch(layer){
            case 1:
                SimStatus.physical = set;
                break;
            case 3:
                SimStatus.network = set;
                break;
            case 7:  
                SimStatus.application = set;
                break;
        }
    }
    
    /**
     * Starts all <code>Generator</code>s of the current simulation.
     */
    private void startGenerators() {
        int nGenerators = Integer.parseInt(prop.getProperty("Generator.Number"));
        String genName = "";
        /** creates traffic lights */
        new GeneratorTrafficLight(prop,"TrafficLight");
        for(int i=1 ; i<=nGenerators ; i++) {
            genName = prop.getProperty("Generator." + i);
            Generator gen;
            if(genName.startsWith("Ran")) {
                gen = new GeneratorRan(prop, genName);
                gen.start();
            }
            else if(genName.startsWith("Tram")) {
                gen = new GeneratorTram(prop, genName);
                gen.start();
            }
            else if(genName.startsWith("Ped")) {
                gen = new GeneratorPed(prop, genName); //UPDATE as soon as there is a new type of generator
                gen.start();
            }
            else if(genName.startsWith("Car")) {
                gen = new GeneratorCar(prop, genName);
                gen.start();
            }
            else if(genName.startsWith("Cyc")) {
                gen = new Generator(prop, genName); //UPDATE as soon as there is a new type of generator
                gen.start();
            }
            else if(genName.startsWith("Bus")) {
                gen = new GeneratorBus(prop, genName);
                gen.start();                
            }
            else if(genName.startsWith("Tri")) {
                gen = new Generator(prop, genName); //UPDATE as soon as there is a new type of generator
                gen.start();
            }
            else
                System.out.println("WARNING: invalid Generator type (" + genName + "). Check the configuration file.");
        }
    }
    
    /**
     * Handles a message reception by the <code>TCPServerThread</code>.
     * 
     * @param thread    The tcp connection from where the message was received.
     * @param receivedMsg   The message to process.
     */
    public static void receiveMessage(TCPServerThread thread, TCPMessage receivedMsg) {
        /** this is a map request */
        if(receivedMsg.messageType.equals("03")) {
            TCPMessage msgToSend = new TCPMessage ("04");
            msgToSend.messageStr = receivedMsg.messageStr;
            msgToSend.map = new GlobalMap(SimStatus.maps.get(receivedMsg.messageStr));
            thread.sendMsg(msgToSend);   
        }else if(receivedMsg.messageType.equals("06")){
            /** adds statistcs from this Local Coordinator to Statistics Report */
            statistics.addReportLocal(receivedMsg.messageStr);
            try {
                stream.write(receivedMsg.fileContent);
            } catch (IOException ex) {
                Logger.getLogger(GlobalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                tcpServer.localCoordinators.remove(thread.ipLocalCoordinator.getHostAddress());
                System.out.println("\tRemoved Local Coordinator - " + 
                        tcpServer.localCoordinators.size() + " left" );
                
                if(tcpServer.localCoordinators.isEmpty()){
                    tcpServer.shutdown();
                    tcpServer.join();
                    synchronized(tcpServer){
                        tcpServer.notify();
                    }
                    /** writes the statistics file */
                    statistics.writeStatistics();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(GlobalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
            System.out.println("Unexpected packet type received: "+ receivedMsg.messageType);
    }    
    public static void stopSimulation(){
        try{
            System.out.println("\tGlobalCoordinator is shutting down...");
            SimStatus.running = false;
            if(reportGlobalQ != 0) {
                reportGlobal.stopWriting();
                reportGlobal.join();
            }
            multicastReceiver.stopMulticastReceiver();
            multicastReceiver.join();
            System.out.println("\tMulticast Receiver disconnected...");
            stream = new FileOutputStream("reports/MessageEvents.txt");
            stream.write("TimeStamp\tID\tInfo\tLayer\tData\tSize of Buffer\n".getBytes());
            for(TCPServerThread tcpst : tcpServer.localCoordinators.values()){
                TCPMessage msgToSend = new TCPMessage ("05");
                msgToSend.messageStr = "DIE";
                tcpst.sendMsg(msgToSend);
            }
            synchronized(tcpServer){
                tcpServer.wait();
            }
            System.out.println("\tTCP Server disconnected...");
            System.out.println("\tBye.");
            stream.close();
                   
        }catch(InterruptedException ex){
            Logger.getLogger(GlobalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GlobalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GlobalCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
