package um.simulator.actor;

import java.util.ArrayList;
import um.simulator.comm.CommStack;
import um.simulator.core.LocalCoordinator;

/**
 * This class represents an actor that moves based on the Random Walk mobility model.
 * @author Rui Pinheiro 
 * @author Adriano Moreira
 * @version 1.0
 */
public class ActorRandom extends Actor {
    
    
    ArrayList<Object> points;
    
    /** Constructor: Creates an <code>ActorRandom</code> from a String 
     * containing its defining parameters.
     * 
     * @param actorDescription a String containing the Actor's parameters
    */
    public ActorRandom(String actorDescription) {
        super(actorDescription);
        linesMapName = actorParams[11];
        speed = Double.parseDouble(actorParams[12]);
        appName = actorParams[13];
        opMode = actorParams[14].charAt(0);
        
    }
    
    @Override
    public void setInitialParameters() {
        super.setInitialParameters();
        linesMap = LocalCoordinator.getMap(linesMapName);
        actual_point_id = linesMap.getMapPointID(x, y);
    }
    

    /**
     * This method represents the mobility model used by this actor - Random Walk.
     * It updates the spatial position of the actor.
     * In this version of the Random Walk mobility model, the actor is allowed to move outside the map area.
     * @return a String with variable information about the motion status of the user (e.g. "waiting at a bus station")
     */
    @Override
    public String moveActor() {
        x = x + (randomGenerator.nextDouble() - 0.5) * speed * time_pace;
        y = y + (randomGenerator.nextDouble() - 0.5) * speed * time_pace;
        return("random");
    }
}
