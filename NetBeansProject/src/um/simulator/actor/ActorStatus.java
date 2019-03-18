package um.simulator.actor;

import java.io.Serializable;

/**
 * This class represents the status of an actor.
 * 
 * @author XT17 
 * @author Adriano Moreira
 * @version 1.0
 */
public class ActorStatus implements Serializable {

    private String actor_id; 
    private Double actor_x; 
    private Double actor_y; 
    private Double actor_speed = 0.0; 
    private Double actor_vx = 0.0; 
    private Double actor_vy = 0.0; 
    private String label; 
    /** set to true if the actor is about to die */
    private boolean dying = false; 
    private Long actor_time; 
    private int lane; 
    /** traffic light color/state */
    private int tlState;

   
    /**
     * Constructor: creates an ActorStatus object.
     */
    public ActorStatus() {
    }
    
    /**
     * Constructor: creates an ActorStatus object and sets some of its parameters.
     * Parameters: id and position.
     * 
     * @param id The actor id.
     * @param x The actor's x coordinates.
     * @param y The actor's y coordinates.
     */
    public ActorStatus(String id, double x, double y) {
        this.actor_id = id;
        this.actor_x = x;
        this.actor_y = y;
    }
    
    /**
     * Constructor: creates an ActorStatus object and sets some of its parameters.
     * Parameters: id, position and label.
     * 
     * @param id The actor id.
     * @param x The actor's x coordinates.
     * @param y The actor's y coordinates.
     * @param label The actor's label
     */
    public ActorStatus(String id, double x, double y, String label) {
        this.actor_id = id;
        this.actor_x = x;
        this.actor_y = y;
        this.label = label;
    }

    /**
     * Constructor: creates an ActorStatus object and sets its parameters.
     * 
     * @param id the actor's id
     * @param x the actor's x axis location
     * @param y the actor's y axis location
     * @param speedx the actor's x axis speed
     * @param speedy the actor's y axis speed
     * @param label the actor's label
     */
    public ActorStatus(String id, double x, double y,double speedx, double speedy, String label) {
        this.actor_id = id;
        this.actor_x = x;
        this.actor_y = y;
        this.actor_vx = speedx;
        this.actor_vy = speedy;
        this.label = label;
    }
    
    /**
     * Constructor: creates an ActorStatus object from a string.
     * This constructor pairs with the toString() method, that converts an ActorStatus object to a string.
     * @param actorString actor information 
     */
    public ActorStatus (String actorString) {
        String[] actorParts;
        actorParts = actorString.split("#");
        this.actor_id = actorParts[0];
        this.actor_x = Double.parseDouble(actorParts[1]);
        this.actor_y = Double.parseDouble(actorParts[2]);
        this.actor_speed = Double.parseDouble(actorParts[3]);
        this.actor_vx = Double.parseDouble(actorParts[4]);
        this.actor_vy = Double.parseDouble(actorParts[5]);
        this.label = actorParts[6];
        this.dying = !actorParts[7].equalsIgnoreCase("0");
    }
    
    /** String representing the status of an actor.
     * The several fields are separated by "#"
     * 
     * @return the actor's status
     */
    @Override
    public String toString() {
        String dyingString;
        if(dying)
            dyingString = "1";
        else
            dyingString = "0";
        return actor_id + "#" + actor_x + "#" + actor_y + "#" + actor_speed + "#" + actor_vx + "#" + actor_vy + "#" + label + "#" + dyingString;
    }
    
    /** Get and Set methods */
    
    /** @return an actor id */
    public String getActorId() {
        return actor_id;
    }
    /** @param actor_id  a new actor id */
    public void setActorId(String actor_id) {
        this.actor_id=actor_id;
    }
    /** @return actor x location */
    public Double getActor_x() {
        return actor_x;
    }
    /** @param actor_x  actor's x location */
    public void setActor_x(Double actor_x) {
        this.actor_x = actor_x;
    }
    /** @return actor y location */
    public Double getActor_y() {
        return actor_y;
    }
    /** @param actor_y  actor's y location */
    public void setActor_y(Double actor_y) {
        this.actor_y = actor_y;
    }
    /** @return actor's speed */
    public Double getActor_speed() {
        return actor_speed;
    }
    /** @param actor_speed  a new speed */
    public void setActor_speed(Double actor_speed) {
        this.actor_speed = actor_speed;
    }
    /** @return actor speed in x axis */
    public Double getActor_vx() {
        return actor_vx;
    }
    /** @param actor_vx  new actor speed in x axis */
    public void setActor_vx(Double actor_vx) {
        this.actor_vx = actor_vx;
    }
    /** @return actor speed in y axis */
    public Double getActor_vy() {
        return actor_vy;
    }
    /** @param actor_vy  new actor speed in y axis */
    public void setActor_vy(Double actor_vy) {
        this.actor_vy = actor_vy;
    }
    /** @return actor alive time */
    public Long getActor_time() {
        return actor_time;
    }
    /** @param actor_time  a new actor time */
    public void setActor_time(Long actor_time) {
        this.actor_time = actor_time;
    }
    /** @return a label */
    public String getLabel() {
        return label;
    }
    /** @return a lane */
    public int getLane() {
        return lane;
    }
    /** @param label  a new actor label */
    public void setLabel(String label) {
        this.label = label;
    }
    /** @param dying dying flag */
    public void setDying(boolean dying) {
        this.dying = dying;
    }
    /** @param lane a new lane */
    public void setLane(int lane) {
        this.lane = lane;
    }
    /** @return dying flag */
    public boolean actorDyingQ() {
        return dying;
    }
    /** @return actor's id */
     public String getActor_id() {
        return actor_id;
    }
    /** @return traffic light state */
    public int getTlState() {
        return tlState;
    }
    /** @param actor_id  a new actor id */
    public void setActor_id(String actor_id) {
        this.actor_id = actor_id;
    }
    /** @param tlState  a new traffic light state */
    public void setTlState(int tlState) {
        this.tlState = tlState;
    }
}
