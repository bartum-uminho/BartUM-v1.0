package um.simulator.actor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.CommStack;
import um.simulator.comm.physical.WAVEProtocol;
import um.simulator.comm.application.ApplicationLayer;
import um.simulator.core.SimStatus;
import um.simulator.map.GlobalMap;
import um.simulator.map.MapLine;

/**
 * This class represents a generic actor. It is the base for the creation of
 * specialized actors.
 *
 * @author Rui Pinheiro 
 * @author Adriano Moreira
 * @version 1.0
 */
public class Actor extends Thread {

    String id = null; 
    /** spatial coordinates and speed of the actor */
    double x = 0, y = 0; 
    double vx = 0.0, vy = 0.0, speed = 0.0; 
    String label = ""; 
    /** lifetime of the actor, in minutes */
    public int lifetime_min; 
    /**lifetime of the actor, in milliseconds */
    long lifetime; 
    public boolean alive = true; 
    public boolean dying = false; 
    double prob_Stop;
    String linesMapName;
    /** set update time pace, in milliseconds (update rate = 1/t) */
    int time_pace = 100; 
    GlobalMap linesMap; 
    String[] actorParams; 
    /** when moving, these are the coordinates of the target point */
    double xfs, yfs; 
    /** end point of the current line segment */
    int actual_point_id = -1; 
    /** id of the point, in the map, that represents the starting point the current line segment */ 
    int previous_point_id = -1; 
    ArrayList<ActorPositionTimestamp> positionHistory = new ArrayList<>();
    /** maximum number of previous positions to hold in positionHistory */
    final int MAXHISTORY = 10; 
    int current_lane = 1;
    Random randomGenerator;
    /** Communication Stack */
    CommStack cs;
    String appName;
    char opMode;
    String[] csParams;
    

    /**
     * Constructor: creates an actor from a string that describes the values of
     * its parameters.
     *
     * @param actorDescription A String that includes all the values of its
     * parameters 
     * @see Generator
     */
    public Actor(String actorDescription) {
        actorParams = actorDescription.split(":");
        id = actorParams[0];
        x = Double.parseDouble(actorParams[1]);
        y = Double.parseDouble(actorParams[2]);
        SimStatus.registerNewActor(id, x, y, label);
        randomGenerator = new Random();
        /** sets the actor lifetime to 10 minutes */
        lifetime_min = 10; 
        lifetime = (long) TimeUnit.MINUTES.toMillis(lifetime_min); // converter minutos em milisegundos
        
        /** Initializes the Communication Stack */
        csParams = new String[]{actorParams[3], actorParams[4],actorParams[5], 
            actorParams[6], actorParams[7], actorParams[8],actorParams[9],actorParams[10]};
        
    
    }

    /**
     * This method updates the status of an actor every <code>time_pace</code> milliseconds, 
     * while it is alive. 
     * This method also controls the lifetime of the actor.
     */
    @Override
    public void run() {
        cs = new CommStack(this, this.csParams);
        /** time variables to control the <Actor>s lifetime */
        long time_ini; 
        /** lifecicle duration in ns */
        long deltaTime; 
        /** lifecicle duration in ms */
        long deltaTime_ms;
        setInitialParameters();
        /** start the comunication Thread CommStack */
        cs.start();
        
        /** while the actor is alive, keep updating it every time_pace milliseconds */
        try {
            while (alive && !SimStatus.globalActors.get(id).actorDyingQ()) {
                /** decreases lifetime as the cycles pass */
                lifetime = lifetime - time_pace; 
                if (lifetime > 0) {
                    /** saves current time in nanoseconds */
                    time_ini = System.nanoTime();
                    label = moveActor();
                    SimStatus.setActorStatus(id, x, y, vx, vy, label);
                    /** saves lifecycle duration */
                    deltaTime = System.nanoTime() - time_ini;
                    deltaTime_ms = TimeUnit.NANOSECONDS.toMillis(deltaTime);
                    /** if lifecycle duration is less than the time pace, sleeps for the time remaining */
                    if (deltaTime_ms <= time_pace) {
                        Thread.sleep(time_pace - deltaTime_ms);
                    }
                    /** if lifecycle is more than 20% bigger than time pace, launches warning */
                    if(deltaTime_ms > 0.2*time_pace) { 
                        System.err.println("WARNING - actor "+id+" lifecicle time >20% of time_pace");
                    }
                } else {
                    
                    /** Starts de dying process */
                    System.out.println("Actor " + id + " lifetime is over...");
                    dying = true;
                    /** sets actor status to 'dying' */
                    SimStatus.setActorStatus(id, x, y, vx, vy, label, dying);
                    /** sleep for some time, waiting for the news to spread */
                    Thread.sleep(500);
                }
            }
            /** completes the dying process */
            alive = false;
            cs.join();
            SimStatus.removeActor(id);
        } catch (InterruptedException ex) {
            Logger.getLogger(Actor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /** Initializes some Actor type specific parameters. */
    public void setInitialParameters() {
        /** DO NOTHING */
    }
    
    /**
     * @return an ArrayList with the Actor's position history
     */
    public ArrayList<ActorPositionTimestamp> getPositionHistory() {
        return positionHistory;
    }


    /**
     * This method represents the mobility model used by this actor. It updates
     * the spatial position of the actor (in this case it does nothing).
     *
     * @return a String with variable information about the motion status of the
     * user (e.g. "waiting at a bus station")
     */
    public String moveActor() {
        return ("notMoving");
    }

    /**
     * This method adds the current position and timestamp of the actor to a
     * buffer.
     *
     * @param ti The timestamp
     * @param x The x coordinate (longitude)
     * @param y The y coordinate (latitude)
     */
    public void addPositionToHistory(long ti, double x, double y) {
        synchronized(positionHistory){
            if (positionHistory.size() >= MAXHISTORY) {
                positionHistory.remove(0);
            }
            positionHistory.add(new ActorPositionTimestamp(ti, x, y));
        }
    }


    /** Decides which line segment to follow. 
     * 
     * @param map The GlobalMap from where to choose the line segment to follow
     * @param goBackProbability A probability value
     */
    public void setNextDestination(GlobalMap map, double goBackProbability) {
        int nextPointId;
        HashSet destIds = (HashSet) (map.pointsNeighbours.get(actual_point_id)).clone();
        if (randomGenerator.nextDouble() < goBackProbability || destIds.size()==1) { 
            /** goes back with GOBACKPROBABILITY probability */
            nextPointId = previous_point_id;
        }else{ 
            /** selects one of the other options with equal probability */
            /** removes the previous point from the set of options*/
            destIds.remove(previous_point_id);
            /** selects among the remaining options */
            ArrayList destIds2 = new ArrayList(destIds);
            nextPointId = (int) destIds2.get(randomGenerator.nextInt(destIds2.size()));
        }

        /** updates the coordinates of the destination */
        xfs = map.points.get(nextPointId).getX();
        yfs = map.points.get(nextPointId).getY();
        previous_point_id = actual_point_id;
        actual_point_id = nextPointId;

    }

    /**
     * @return the Actor's ID
     */
    public String getActorId() {
        return this.id;
    }
    
    /**
     * @return the x coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * @return the y coordinate
     */
    public double getY() {
        return y;
    }
    
    /**
     * @return the name of the Application running in this Actor
     */
    public String getAppName(){
        return this.appName;
    }
    
    /**
     * @return the name of the Application running in this Actor
     */
    public char getOpMode(){
        return this.opMode;
    }
    
}
