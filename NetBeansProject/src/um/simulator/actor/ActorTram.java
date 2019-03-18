package um.simulator.actor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import um.simulator.comm.CommStack;
import um.simulator.core.LocalCoordinator;
import um.simulator.core.SimStatus;
import um.simulator.map.MapLine;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import um.simulator.map.GlobalMap;

/**
 * This class represents an Actor of type Tram. 
 * The corresponding mobility model makes the trams move along the lines at constant speed. 
 * At intersections, trams are allowed to go back with a certain probability.
 *
 * @author Rui Pinheiro 
 * @author Adriano Moreira 
 * @author Raquel Pereira
 * @version 1.0
 */
public class ActorTram extends Actor {
    /** the probability of going back at an intersection */
    final double GOBACKPROBABILITY = 0.001; 
    ArrayList<Object> points;
    ArrayList<Object> line_list;
    ArrayList<Object> map_list;
    boolean way = true;
    double initialSpeed, xis, yis, minSpeed, maxSpeed;
    double stopX, stopY;
    /** verifies if it will stop in the next node */
    double stopState = 0;
    int n_lanes;
    /** stopping time at the bus stop */
    long stopBreak; 
    /** distance to the next stop */ 
    double stopDistance;
    double neighbourDist = 100; 
    /** friction coefficient */
    double friction = 0.8; 
    double gravity = 9.8; 
    double k = Math.sqrt(2 * gravity * friction);
    double new_speed = 0;
    double a = 2.1;
    double xn = 0, yn = 0;
    /** collision probability */
    double pc = 0; 
    /** radius to look for potential colliding actors */
    double r; 
    double x_traffic, y_traffic;
    double tlDistance;
    int tlState = 0;
    /** id of the next <code>TrafficLight</code> */
    int id_tl;

    /** Constructor: Creates an <code>ActorTram</code> from a String 
     * containing its defining parameters.
     * 
     * @param actorDescription a String containing the Actor's parameters
    */
    public ActorTram(String actorDescription) {
        super(actorDescription);
        linesMapName = actorParams[11];
        speed = Double.parseDouble(actorParams[12]);
        prob_Stop = Double.parseDouble(actorParams[13]);
        appName=actorParams[14];
        opMode = actorParams[15].charAt(0);
        /** sets the initial starting point */
        xis = x; 
        yis = y; 
        xfs = x;
        yfs = y;
        xn = x;
        yn = y;
        
    }

    @Override
    public void setInitialParameters() {
        super.setInitialParameters();
        /** loads the required maps */
        linesMap = LocalCoordinator.getMap(linesMapName);
        actual_point_id = linesMap.getMapPointID(x, y);
        /** sets the initial destination point */
        setNextDestination(linesMap, GOBACKPROBABILITY);
        /** sets the initial speed vector */
        initialSpeed = speed; //save the initial speed
        setSpeedVector(speed);
    }

    @Override
    public String moveActor() {
        double collisionProb;
        collisionProb = collisionProbability();
        updateSpeed(collisionProb);
        updatePosition();
        return ("V=" + speed * 3600);
    }
    
    /** Moves. */
    private void updatePosition() {
        xn = x + (vx * time_pace);
        yn = y + (vy * time_pace);
        /** tests if the new positionexceeds the end of the current line segment */
        if ((xn - x) * (xn - x) + (yn - y) * (yn - y) > (xfs - x) * (xfs - x) + (yfs - y) * (yfs - y)) {
            x = xfs;
            y = yfs;
            xis = xfs;
            yis = yfs;
            setNextDestination(linesMap, GOBACKPROBABILITY);
            setSpeedVector(speed);
            if (speed != 0) {
                xn = x;
                yn = y;
            }
        } else { 
            x = xn;
            y = yn;
        }
        addPositionToHistory(System.currentTimeMillis(), x, y);
    }
    
    /** Calculates collision probability.
     * @return a double
     */
    


    /**
     * Gets the tag value.
     *
     * @param key the tag's name
     * @param defaultValue tag's default value
     * @return the tag value
     */
    public String getWayTag(String key, String defaultValue) {
        MapLine ml = this.linesMap.getMapLine(actual_point_id);
        int id_line = ml.getId();
        String value = linesMap.getLines().get(id_line).getTags().get(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
    
    /**
     * Updates de Actor's speed.
     * @param collisionProb a probability for collision
     */
    private void updateSpeed(double collisionProb) {

        maxSpeed = Double.parseDouble(getWayTag("maxspeed", "50"));
        n_lanes = Integer.parseInt(getWayTag("lanes", "1"));
        /** converts km/h to m/ms */
        maxSpeed = maxSpeed / 3600; 

        if (stopState != 2) {
            if (collisionProb > 0 && collisionProb < 0.95) {
                /** decreases speed */
                speed = (1 - collisionProb) * speed;
            } else if (collisionProb >= 0.95) {
                vx = 0;
                vy = 0;
                speed = 0;
            }
            if (speed < maxSpeed) {
                if (speed == 0) {
                    /** increases speed slightly */
                    speed = initialSpeed * 0.1;
                }
                speed = speed * 1.1;
            }
            if (speed > maxSpeed) {
                speed = maxSpeed;
            }

        }

        /** stops at the next node */
        if (stopState == 1) {
            stopDistance = Math.sqrt((x - stopX) * (x - stopX) + (y - stopY) * (y - stopY));
            new_speed = k * Math.sqrt(stopDistance);
            new_speed = new_speed * 0.001;
            if (speed > new_speed) {
                /** decreases speed */
                speed = new_speed;
            }
            if ((xfs != stopX && yfs != stopY) || stopDistance == 0) {
                speed = 0;
                vx = 0;
                vy = 0;
                stopState = 2;
                stopBreak = System.currentTimeMillis();
            }
        } else if (stopState == 2) {
            long tempo = System.currentTimeMillis() - stopBreak;
            if (tempo > 2 * 1000) {
                stopState = 0;
                /** increases speed */
                speed = speed + Math.pow(10, -6) * a * time_pace;
            }
        }

        /** traffic light in the way */
        if (tlState == 1) {
            int state = SimStatus.globalActors.get("TrafficLight." + id_tl).getTlState();
            /** yellow or red traffic light */
            if (state == 1 || state == 2) {
                tlDistance = Math.sqrt((x - x_traffic) * (x - x_traffic) + (y - y_traffic) * (y - y_traffic));
                new_speed = k * Math.sqrt(tlDistance - 1);
                new_speed = new_speed * 0.001;
                
                /** decreases speed */
                if (speed > new_speed) {
                    speed = new_speed;
                }
                if ((xfs != x_traffic && yfs != y_traffic) || tlDistance < 2) {
                    vx = 0;
                    vy = 0;
                    speed = 0;
                }
            }
        }

        setSpeedVector(speed);
    }

    /** Aligns the speed vector to the current lane. 
     * @param setSpeed an absolute speed value
     */
    private void setSpeedVector(double setSpeed) {
        double vk = 1 / Math.sqrt((xfs - xis) * (xfs - xis) + (yfs - yis) * (yfs - yis));
        vx = setSpeed * vk * (xfs - xis);
        vy = setSpeed * vk * (yfs - yis);
    }

    @Override
    public void setNextDestination(GlobalMap map, double goBackProbability) {
        int nextPointId;
        HashSet destIds = (HashSet) (map.pointsNeighbours.get(actual_point_id)).clone();
        if (randomGenerator.nextDouble() < goBackProbability || destIds.size()==1) { 
            /** goes back with GOBACKPROBABILITY probability */
            nextPointId = previous_point_id;
        } else { 
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
        
        /** checks tram stops */
        if (linesMap.getPoints().get(actual_point_id).getTags().containsKey("name")) {
            if (linesMap.getPoints().get(actual_point_id).getTags().get("name").equals("tram_stop")) {
                double probStop = randomGenerator.nextDouble();
                if (probStop >= prob_Stop) {
                    /** stops */
                    stopState = 1; 
                    stopX = xfs;
                    stopY = yfs;
                } else {
                    stopState = 0;
                }
            }
        }
        
        if (linesMap.getPoints().get(actual_point_id).getTags().containsKey("highway")) {
            if (linesMap.getPoints().get(actual_point_id).getTags().get("highway").equals("traffic_signals")) {
                /** saves traffic light's coordinates */
                id_tl = linesMap.getPoints().get(actual_point_id).getId();
                x_traffic = xfs;
                y_traffic = yfs;
                tlState = 1;

            } else {
                tlState = 0;
            }
        } else {
            tlState = 0;
        }

    }

    /** Calculates collision probability.
     * @return a double
     */
    private double collisionProbability() {

        HashMap<String, ActorStatus> neighboursList = new HashMap<String, ActorStatus>();
        /** neighbourhood parameters */
        double r0 = 25, r1 = 30; 
        r = r0 + r1 * speed; 
        double tempPc = 0, xi, xf, xiNg, xfNg, yi, yf, yiNg, yfNg;
        pc = 0;
        xi = x;
        yi = y; //get the absolute beginning of the speed vector
        xf = xi + vx;
        yf = yi + vy; //get the absolute end of the speed vector

        /** retrieves the current neighbours - potential colliders */
        neighboursList.clear();
        neighboursList.putAll(SimStatus.getNeighbours(id, x, y, r));

        if (!neighboursList.isEmpty()) { 
            Iterator it = neighboursList.keySet().iterator();
            Object key;
            ActorStatus item;
            /** looks into each one of the neighbours and compute the collision probability */
            while (it.hasNext()) {
                key = it.next();
                item = neighboursList.get((String) key);
                /** neighbour's vector */
                xiNg = item.getActor_x();
                yiNg = item.getActor_y();
                xfNg = xiNg + item.getActor_vx();
                yfNg = yiNg + item.getActor_vy();
                String nid = item.getActorId();

                double paralel = ((yfNg - yiNg) * (xf - xi)) - ((xfNg - xiNg) * (yf - yi));
                double coincident1 = ((xfNg - xiNg) * (yi - yiNg)) - ((yfNg - yiNg) * (xi - xiNg));
                double coincident2 = ((xf - xi) * (yi - yiNg)) - (yf - yi) * (xi - xiNg);

                double ua1 = (xfNg - xiNg) * (yi - yiNg) - (yfNg - yiNg) * (xi - xiNg);
                double ua = ua1 / paralel;

                double slopeNg = (yfNg - yiNg) / (xfNg - xiNg);
                double finalSlope = Math.atan(slopeNg);

                double neighbourSpeed = Math.abs(item.getActor_vx() / Math.cos(finalSlope));
                double d1 = Math.sqrt(Math.pow((xiNg - xi), 2) + Math.pow((yiNg - yi), 2));
                double d2 = Math.sqrt(Math.pow((xiNg - xf), 2) + Math.pow((yiNg - yf), 2));
                double d5 = Math.sqrt(Math.pow((xf - xi), 2) + Math.pow((yf - yi), 2));
                double d4 = Math.sqrt(Math.pow((xfNg - xf), 2) + Math.pow((yfNg - yf), 2));
                double d3 = Math.sqrt(Math.pow((xi - xfNg), 2) + Math.pow((yi - yfNg), 2));
                double d6 = Math.sqrt(Math.pow((xiNg - xfNg), 2) + Math.pow((yiNg - yfNg), 2));

                /** checks if the two speed vectors are parallel */
                if (paralel == 0 || paralel < 0.05 && paralel > -0.05) {
                    /** check if the two speed vectors are colinear */
                    if (coincident1 == 0 && coincident2 == 0 || coincident1 < 0.05 && coincident1 > -0.05 && coincident2 < 0.05 && coincident2 > -0.05) {
                        /** checks if this neighbour is moving ahead */
                        if (d3 > d2) {
                            /** checks if the neighbour is moving in the same direction */
                            if (d3 > d1) {
                                /** checks if the neighbour is moving slower */
                                if (speed >= neighbourSpeed) {
                                    neighbourSpeed = (double) neighbourSpeed * 3600;
                                    DecimalFormat df = new DecimalFormat("#.#");
                                    df.setRoundingMode(RoundingMode.DOWN);
                                    String aux = df.format((double) neighbourSpeed);
                                    aux = aux.replace(',', '.');
                                    Double d = new Double(aux);
                                    Double speed_viz2 = d.doubleValue();
                                    speed = speed * 3600;
                                    tempPc = 1 - (Math.exp(-(((speed - speed_viz2) * 15) / d1)));
                                    speed = speed / 3600;
                                    neighbourSpeed = neighbourSpeed / 3600;
                                } else { 
                                    //the neighbour ahead is moving faster, so they will not collide
                                }
                            } else { 
                                //the neighbour ahead is moving in the oposite direction, so they will not collide
                            }
                        } else { 
                            //the neighbour is not moving ahead, so it is moving behind
                        }
                    } else { 
                        //the speed vectors are not collinear, so they are moving in different lines
                    }
                } else { 
                    //the speed vectores are not parallel, so they are moving in different lines
                    double piX = xi + ua * (xf - xi);
                    double piY = yi + ua * (yf - yi);
                    if (d3 > d4) {
                        double di = Math.sqrt(Math.pow((piX - xi), 2) + Math.pow((piY - yi), 2));
                        double di_viz = Math.sqrt(Math.pow((piX - xiNg), 2) + Math.pow((piY - yiNg), 2));
                        double tempo = di / speed;
                        double tempo_viz = di_viz / neighbourSpeed;
                        tempPc = 1 - Math.exp(15 / Math.abs(tempo - tempo_viz));
                    } else { 
                        //the two speed vectors will not intercept ahead
                    }
                }
                /** checks if the probability of collision with the current neighbour is the highest */
                if (pc < tempPc) {
                    pc = tempPc;
                }
                tempPc = 0;
            }
        }
        return (pc);
    }

}
