/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.actor;

import java.util.Properties;
import um.simulator.core.GlobalCoordinator;
import um.simulator.core.SimStatus;
import um.simulator.map.GlobalMap;
import um.simulator.map.MapPoint;

/**
 * This class is an traffic light generator. 
 * It creates <code>TrafficLight</code>s, not inherited from <code>Generator</code> class.
 * 
 * @author luisacabs
 * @version 1.0
 */
public class GeneratorTrafficLight{
   
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
     * Constructor: New traffic light generator.
     * Calls <code>generate</code> method.
     * 
     * @param prop properties for the generator
     * @param genName   name of the generator type
     */
    public GeneratorTrafficLight(Properties prop, String genName) {
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
        generate();
    }
    
    /**
     * Creates all traffic lights.
     */
    public void generate(){
        
        SimStatus.maps.keySet().stream().map((id_map) -> SimStatus.maps.get(id_map)).forEach((gm) -> {
            gm.trafficLights.stream().map((mp) -> "TrafficLight."+mp.getId()+":"+mp.getX()+":"+mp.getY()).forEach((desc) ->
            {
                /** String description of the new actor */
                String newActorDescription = desc + ":" + getNewActorDescription();
                /** tell the GlobalCoordinator to create a new actor */
                GlobalCoordinator.tcpServer.createNewActor(newActorDescription);
               
            });
        });
    }
    public String getNewActorDescription() {
        return  "" + physicalProtocol + ":" + PHYDataRate + ':' + PHYTxRange + ':' + FER 
                + ':' + linkProtocol + ":" + routingProtocol + ':' + routingProtocolQueue + ':' + numberOfRetransmissions
                + ":" + appName + ":" + opMode;
    }
}
