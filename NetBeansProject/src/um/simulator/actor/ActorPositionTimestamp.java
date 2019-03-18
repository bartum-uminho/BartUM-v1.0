/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.actor;

/**
 * This class represents the position of an actor at a given timestamp.
 * @author lmnd 
 * @author ajcmoreira
 * @version 1.0
 */
public class ActorPositionTimestamp {
    
    private long timestamp;
    private Double actor_x;
    private Double actor_y;
    
    /** Constructor: creates an object that co-relates and actor's position 
     * to a given timestamp.
     * @param timestamp a given timestamp
     * @param actor_x the x coordinate of the actor's position
     * @param actor_y the y coordinate of the actor's position
     */
    public ActorPositionTimestamp(long timestamp, Double actor_x, Double actor_y) {
        this.timestamp = timestamp;
        this.actor_x = actor_x;
        this.actor_y = actor_y;
    }

    /** Gets the timestamp of this object.
     * @return timestamp
     */
    public long getTime_stamp() {
        return timestamp;
    }
    /** Gets the x coordinate of the actor's position.
     * @return x coordinate
     */
    public Double getActor_x() {
        return actor_x;
    }
    /** Gets the x coordinate of the actor's position.
     * @return x coordinate
     */
    public Double getActor_y() {
        return actor_y;
    }
    
    /**
     * Sets a new timestamp.
     * @param time_stamp new timestamp
     */
    public void setTime_stamp(long time_stamp) {
        this.timestamp = time_stamp;
    }
    /**
     * Sets a new x coordinate
     * @param actor_x new x coordinate
     */
    public void setActor_x(Double actor_x) {
        this.actor_x = actor_x;
    }
    /**
     * Sets a new y coordinate
     * @param actor_y new y coordinate
     */
    public void setActor_y(Double actor_y) {
        this.actor_y = actor_y;
    }
    
    
    
}
