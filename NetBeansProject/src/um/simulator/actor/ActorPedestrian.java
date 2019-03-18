package um.simulator.actor;

import java.util.ArrayList;
import um.simulator.comm.CommStack;
import um.simulator.core.LocalCoordinator;

/** This class represents a Actor of type Pedestrian.
 *
 * @author Rui Pinheiro 
 * @author Adriano Moreira
 * @version 1.0
 */
public class ActorPedestrian extends Actor {

    static double BACKPROBABILITY = 0.8;
    ArrayList<Object> pontos;
    ArrayList<Object> line_list;
    ArrayList<Object> map_list;
    boolean sentido = true;
    double speedini, xis, yis, lifetime, xn, yn, lane;
    int dying = 0, roads = 0;
    double p;
    

    /** Constructor: Creates an <code>ActorPedestrian</code> from a String 
     * containing its defining parameters.
     * 
     * @param actorDescription a String containing the Actor's parameters
    */
    public ActorPedestrian(String actorDescription) {
        super(actorDescription);
        linesMapName = actorParams[11];
        speed = Double.parseDouble(actorParams[12]);
        appName = actorParams[13];
        opMode = actorParams[14].charAt(0);
        xis = x; 
        yis = y; 
        xn = x;
        yn = y;
       
    }

    @Override
    public void setInitialParameters() {
        super.setInitialParameters();
        linesMap = LocalCoordinator.getMap(linesMapName);
        actual_point_id = linesMap.getMapPointID(x, y);
        
    }
    @Override
    public String moveActor() {
        updateSpeed();
        updatePosition();
        return (Double.toString(speed));
    }
    
    private void updateSpeed() {
        double maxSpeed = 0.004167;//15Khm/h
        if (speed != 0) {
            double p = randomGenerator.nextDouble();
            if (p >= 0.0 && p < 0.5) {
                /** speeds stays the same */
            }
            if (p >= 0.5 && p < 0.75) {
                /** increases speed */
                speed = speed + Math.pow(10, -6) * 0.1 * time_pace;

            }
            if (p >= 0.75 && p <= 1) {
                /** decreases speed */
                speed = speed - Math.pow(10, -6) * 0.1 * time_pace;              
            }
            if (maxSpeed < speed) {
                speed = maxSpeed;
            }
        }
        
        setSpeedVector(speed);
    }

    /** Moves. */
    private void updatePosition() {
        
        xn = xn + (time_pace * vx);
        yn = yn + (time_pace * vy);
        int variation = 5;
        int limit = 20;
        lane = lane + (-variation + ((variation + variation) * Math.random())); //variacao max na linha=variation e minimo=variation
        if (lane > limit) {
            lane = limit;
        } else if (lane < -limit) {
            lane = -limit;
        }
        /** tests if the new position exceeds the end of the line segment */
        if (((xn - x) * (xn - x)) + ((yn - y) * (yn - y)) > ((xfs - x) * (xfs - x)) + ((yfs - y) * (yfs - y))) {

            x = xfs;
            y = yfs;
            xis = xfs;
            yis = yfs;
            setNextDestination(linesMap, BACKPROBABILITY);
            setSpeedVector(speed);
            double x1 = x - lane * (vy) / Math.sqrt((vx * vx + vy * vy));
            double y1 = y + lane * (vx) / Math.sqrt((vx * vx + vy * vy));
            xn = x;
            yn = y;
            x = x1;
            y = y1;
        } else { 
            double x1 = x - lane * (vy) / Math.sqrt((vx * vx + vy * vy));
            double y1 = y + lane * (vx) / Math.sqrt((vx * vx + vy * vy));
            x = x1;
            y = y1;
        }

        addPositionToHistory(System.currentTimeMillis(), x, y);
    }

    /** Aligns the speed vector to the current lane. 
     * @param setSpeed an absolute speed value
     */
    private void setSpeedVector(double setSpeed) {
        double vk = 1 / Math.sqrt((xfs - xis) * (xfs - xis) + (yfs - yis) * (yfs - yis));
        vx = setSpeed * vk * (xfs - xis);
        vy = setSpeed * vk * (yfs - yis);
    }

}
