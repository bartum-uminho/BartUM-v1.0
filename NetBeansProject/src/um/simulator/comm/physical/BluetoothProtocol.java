/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.physical;

import java.util.ArrayList;
import um.simulator.actor.Actor;
import um.simulator.actor.ActorPositionTimestamp;
import um.simulator.comm.link.LinkLayer;
import um.simulator.comm.network.NPDU;
import um.simulator.core.SimStatus;

/**
 * This class represents the Bluetooth Protocol. 
 * 
 * @author Nuno
 * @version 1.0
 */
public class BluetoothProtocol extends PhyProtocol {
    
    static int RANGE = 999999999;
    ArrayList<PPDU> buffer;
    
    /**
     * Constructor: creates a Bluetooth Protocol.
     * 
     * @param actor the id of the actor
     * @param link  a reference to the link layer
     */
    public BluetoothProtocol(Actor actor, LinkLayer link) {
        super(actor,link);
        buffer = new ArrayList<PPDU>();
    }
    /**
     * Updates Physical Layer.
     */
    public void update(){
        ArrayList <PPDU> frames =  new ArrayList <PPDU>();
        
        synchronized(SimStatus.globalMessages.get(actor.getActorId())){
            
            int i = 0;
            while(i<SimStatus.globalMessages.get(actor.getActorId()).size()){
                
                PPDU m = SimStatus.globalMessages.get(actor.getActorId()).get(i);
                double distance;
                   
                    /** remove message if it's already sent or it's broadcast */
                    if(m.isSent() || m.getDestination_id().equals("Broadcast")){
                        distance = verifyMessageReception(m);
                        if(distance==-1){
                            frames.add(m);
                            SimStatus.globalMessages.get(actor.getActorId()).remove(i);
                        }
                        else{
                            SimStatus.globalMessages.get(actor.getActorId()).remove(i);
                        }
                    }
                    else {
                        if(!m.isRead()){
                            distance = verifyMessageReception(m);
                            if(distance==-1){
                                frames.add(m);
                            }
                            else{
                                
                            }
                            
                            m.setRead(true);
                        }
                        i++;
                    }
                
                
            }
            if (!frames.isEmpty()) {
            ArrayList<NPDU> frames2 = new ArrayList<>();
            for (PPDU p : frames) {
                frames2.add(new NPDU(p.getData()));
            }
            linkLayer.sendFramesUp(frames2);
        }

        }
        
    }
    
    /**
     * Sends frame to the link layer.
     * @param m a physical pdu
     */
    public void sendFrame(PPDU m){
        buffer.add(m);
    }
    /**
     * Places frame into their specific buffer.
     */
    public void flushFrames(){
        for(PPDU m : buffer){
            if(m.getDestination_id().equals("Broadcast")){
                SimStatus.addBroadcastMessage(m);                
            }else{
                if(!SimStatus.globalMessages.containsKey(m.getDestination_id())){
                    SimStatus.globalMessages.put(m.getDestination_id(), new ArrayList<PPDU>());
                }
                synchronized(SimStatus.globalMessages.get(m.getDestination_id())){  
                    SimStatus.globalMessages.get(m.getDestination_id()).add(m);
                }
            }
        }
        buffer.clear();
    }
    
    /**
     * Checks if the actor sending the ppdu is in range.
     * 
     * @param message the ppdu being sent
     * @return the distance to the actor
     */
    public double verifyMessageReception(PPDU message){
        

        ArrayList<ActorPositionTimestamp> position_history = new ArrayList(this.actor.getPositionHistory());
        ActorPositionTimestamp tempaps = position_history.get(0);
        
        for(ActorPositionTimestamp aps : position_history){
            if(Math.abs(aps.getTime_stamp()-message.getTime_stamp()) <= Math.abs(tempaps.getTime_stamp()-message.getTime_stamp())){
                
                tempaps = aps; 
            }
            else{
               
                double distance = (Math.sqrt(Math.pow( (tempaps.getActor_x()-message.getActor_x()),2) + Math.pow((tempaps.getActor_y()-message.getActor_y()) ,2))) *0.85;
                if(distance < RANGE ){
                    return -1;
  
                }
                else{
                    return distance;
                }
            }
            
        }        
        return -1;
    }
     

    
}
