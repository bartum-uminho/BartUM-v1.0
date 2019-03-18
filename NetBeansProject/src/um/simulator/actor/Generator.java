package um.simulator.actor;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import um.simulator.core.GlobalCoordinator;
import um.simulator.core.SimStatus;
import um.simulator.map.CoordinatesHelper;
import um.simulator.map.GlobalMap;

/**
 * This class is a generic actor generator.
 * An actor generator creates actors in runtime, following specific models with adjustable parameters
 * defined in the configurations file (settings.properties).<br>
 * It is used as a base for the creation of specialized generators.
 * @author XT17 
 * @author Adriano Moreira
 * @author luisacabs
 * @version 1.0
 */


public class Generator extends Thread {
    String genName; 
    String linesMapName, stopsMapName;
    /** generation probability, used at each iteration to decide if an actor is to be created or not */
    double p; 
    /** initial position of the actor */
    double xi, yi; 
    /** maximum number of actors to be generated: -1 means no limit */
    int maxActors; 
    /** the total number of actors that have been generated so far */
    int actorsCount = 0; 
    /** the time interval, in milliseconds, between iterations of the generator */
    int genPeriod = 2000; 
    int PHYDataRate;  
    int PHYTxRange;
    /** frame error rate */
    double FER; 
    String routingProtocol;
    /** routing protocol queue's size */
    int routingProtocolQueue; 
    int numberOfRetransmissions;
    String appName;
    char opMode;
    String physicalProtocol;
    String linkProtocol;
    
    /**
    * Constructor: a new <code>Generator</code>.
    * 
    * @param prop Config file properties.
    * @param genName Name of the generator 
    */
    public Generator(Properties prop, String genName) {
        
        this.genName = genName;
        /** reads the generator parameters from the configuration file */
        this.p = Double.parseDouble(prop.getProperty(genName + ".Probability","0.1"));
        Double genLon = Double.parseDouble(prop.getProperty(genName + ".lon"));
        Double genLat = Double.parseDouble(prop.getProperty(genName + ".lat"));
        Double originLon= Double.parseDouble(prop.getProperty("Map.originLon", "0"));
        Double originLat= Double.parseDouble(prop.getProperty("Map.originLat","0"));
        CoordinatesHelper.calculateConstants(originLon, originLat);
        maxActors = Integer.parseInt(prop.getProperty(genName + ".maxActors","-1"));
        PHYDataRate = Integer.parseInt(prop.getProperty(genName + ".PhyDataRate","1000"));
        PHYTxRange = Integer.parseInt(prop.getProperty(genName + ".PhyTxRange","100"));
        FER = Double.parseDouble(prop.getProperty(genName + ".PhyFrameErrorRate","0.00001"));
        routingProtocol = prop.getProperty(genName + ".RoutingProtocol","epidemic");
        routingProtocolQueue = Integer.parseInt(prop.getProperty(genName + ".RoutingProtocolQueue","50"));
        numberOfRetransmissions = Integer.parseInt(prop.getProperty(genName + ".SAWNumberOfRetransmissions", "5"));
        appName = prop.getProperty(genName + ".App","OFF");
        appName = prop.getProperty(appName,null);
        opMode = prop.getProperty(genName + ".AppOpMode","N").charAt(0);
        physicalProtocol = prop.getProperty(genName + ".PhyProtocol","WAVE");
        linkProtocol = prop.getProperty(genName + ".LinkProtocol",null);
        /** converting genLon and genLat to cartesian values */
        HashMap<String, Double> aux_xy;
        aux_xy = CoordinatesHelper.toXY(genLon, genLat);
        /** set the initial starting point */
        xi=aux_xy.get("x");
        yi=aux_xy.get("y");
        
        /** check if one or more lines maps are to be used,
         * If more than one map (lines) is to be used, ask the GlobalCoordinator 
         * to merge them into one single map
         */
        String mapsList = prop.getProperty(genName + ".Maps");
        /** no line maps are used */
        if(mapsList == null) { 
            linesMapName ="";
        /** one or more maps are to be used */
        } else { 
            String[] mapsNames = mapsList.split(",");
            if(mapsNames.length>1) {
                /** creates a copy of the new map */
                GlobalMap newMap = new GlobalMap(SimStatus.maps.get(mapsNames[0]));
                /** merges it with the remaining maps */
                GlobalMap mapToMerge;
                for (int i=1; i<mapsNames.length; i++) {
                    mapToMerge = new GlobalMap(SimStatus.maps.get(mapsNames[i]));
                    newMap.mergeWith(mapToMerge);
                }
                newMap.baseMap = false;
                newMap.mergedMap = true;
                linesMapName = SimStatus.addMap(newMap);
                SimStatus.setMapAsUsed(linesMapName);
                String mapLinesColour = prop.getProperty(genName + ".MapsColour","LIGHT_GRAY");
                SimStatus.setMapLinesColour(linesMapName, mapLinesColour);
            /** only one map is used: there is no need to do any merging */
            } else {
                linesMapName = mapsNames[0];
                SimStatus.setMapAsUsed(linesMapName);
                String mapLinesColour = prop.getProperty(genName + ".MapsColour","LIGHT_GRAY");
                SimStatus.setMapLinesColour(linesMapName, mapLinesColour);
            }
        }
        
        /** checks if one or more stops are to be used, If more than one map (stops) 
         * is to be used, ask the GlobalCoordinator to merge them into one single map 
         */
        mapsList = prop.getProperty(genName + ".Stops");
        /** no  line maps are used */
        if(mapsList == null) {
            stopsMapName ="";
        /** one or more maps are to be used */
        } else { 
            String[] mapsNames = mapsList.split(",");
            if(mapsNames.length > 1) {
                stopsMapName = "TODO";
                //mapName = GlobalCoordinator.mergeStopsMaps(mapsNames);
            /** only one map is used: there is no need to do any merging */    
            } else { 
                stopsMapName = mapsNames[0];
            }
        }
    }
    
    /**
     * Defines the values of the parameters that need to be set to configure new actors.
     * @return A <code>String</code> with the parameters' values separated by ":"
     */
    public String getNewActorDescription() {
        return xi + ":" + yi + ":"+ physicalProtocol + ":" + PHYDataRate + ':' + PHYTxRange + ':' + FER
                + ':' + linkProtocol + ":" + routingProtocol + ':' + routingProtocolQueue + ':' + numberOfRetransmissions;
         
    }

    /** <code>Thread</code>'s run method */
    @Override
    public void run() {
        while(((actorsCount < maxActors) || (maxActors == -1)) && SimStatus.running) {
            if(newActorQ()) {
                /** id for the new actor */
                String actorID = genName + "." + actorsCount; 
                /** String description of the new actor */
                String newActorDescription = actorID + ":" + getNewActorDescription();
                /** tell the GlobalCoordinator to create a new actor */
                GlobalCoordinator.tcpServer.createNewActor(newActorDescription); 
                actorsCount++;
            }
            try {
                Thread.sleep(genPeriod);
            } catch (InterruptedException ex) {
                Logger.getLogger(Generator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Decides if a new actor is to be created.
     * @return true if the random number is lower or equal to <code>p</code>.
     */
    public boolean newActorQ() {
        double value = Math.random();
        return value <= p;
    }

}
