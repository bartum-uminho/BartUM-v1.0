package um.simulator.actor;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static um.simulator.actor.ActorCar.BACKPROBABILITY;
import um.simulator.core.LocalCoordinator;
import um.simulator.core.SimStatus;
import um.simulator.map.GlobalMap;
import um.simulator.map.MapLine;

/** This class represents a Actor of type Car.
 *
 * @author Rui Pinheiro 
 * @author Adriano Moreira
 * @author Raquel Pereira
 * @version 1.0 
 */

public class ActorCar extends Actor {

    /** probability of not going back */
    static double BACKPROBABILITY = 0.001;
    ArrayList<Object> points;
    ArrayList<Object> line_list;
    ArrayList<Object> map_list;
    boolean way = true;
    double initialSpeed, xis, yis, maxSpeed, speedLimit = 0.08, xn = 0, yn = 0, vx1, vy1;
    int n_lanes; 
    int mode = 0, road = 0, lane, tempo_ultrapassagem, counter;
    int overtake = 0; 
    double r; 
    double pc = 0; 
    double a = 2.1;
    double maxLimit = 0;
    double new_speed = 0;
    double neighbourDist = 100;
    long stopBreak;
    double new_mode = Math.random();
    HashMap<String, ActorStatus> frontNeighbours = new HashMap();
    int actorLane = 1;

    double x_traffic, y_traffic;
    double tlDistance;
    int tlState = 0;
    int id_tl;
    double friction = 0.8; 
    double gravity = 9.8; 
    double k = Math.sqrt(2 * gravity * friction);
    

    /** Constructor: Creates an <code>ActorCar</code> from a String 
     * containing its defining parameters.
     * 
     * @param actorDescription a String containing the Actor's parameters
    */
    public ActorCar(String actorDescription) {
        super(actorDescription);
        linesMapName = actorParams[11];
        speed = Double.parseDouble(actorParams[12]); 
        appName = actorParams[13];
        opMode = actorParams[14].charAt(0);
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
        setNextDestination(linesMap, BACKPROBABILITY);
        initialSpeed = speed; 
        setSpeedVector(speed);
    }

    @Override
    public void run() {
        this.setInitialParameters();
        if (new_mode <= 0.5) {
            mode = 0;
        } else {
            mode = 1;
        }
        super.run();
    }

    @Override
    public String moveActor() {
        double value = Math.random();
        if (value < 0.1) {
            setMode();
        }
        double colisionProb;
        colisionProb = collisionProbability();
        updateSpeed(colisionProb);
        updatePosition();
        return ("V=" + speed * 3600);
    }

    /** Sets the driving mode. 
     * 0 - normal
     * 1 - agressive driving
     * 2 - parked 
     */
    public void setMode() {
        double change_mode = Math.random();
        if (change_mode >= 0 && change_mode < 0.98) {
            if (change_mode >= 0.98 && change_mode <= 1) {
                if (mode == 0) {
                    mode = 2;
                } else if (mode == 1) {
                    mode = 2;
                } else if (mode == 2) {
                    if (new_mode <= 0.5) {
                        mode = 0;
                    } else {
                        mode = 1;
                    }
                }
            }
        }

    }
    /** Moves. */
    private void updatePosition() {
        xn = xn + (time_pace * vx);
        yn = yn + (time_pace * vy);
        /** tests if the new position exceeds the end of the current line segment */
        if (((xn - x) * (xn - x)) + ((yn - y) * (yn - y)) > ((xfs - x) * (xfs - x)) + ((yfs - y) * (yfs - y))) {
            x = xfs;
            y = yfs;
            xis = xfs;
            yis = yfs;
            setNextDestination(linesMap, BACKPROBABILITY);
            setSpeedVector(speed);
            if (speed != 0) {
                xn = x;
                yn = y;
            } else {
                vx = vx1;
                vy = vy1;
                xn = x;
                yn = y;
            }
        } else 
         if (actorLane != 1) {
                if (speed != 0) {
                    actorLane = actorLane * 20;
                    double x1 = xn - actorLane * (vy) / Math.sqrt((vx * vx + vy * vy));
                    double y1 = yn + actorLane * (vx) / Math.sqrt((vx * vx + vy * vy));
                    x = x1;
                    y = y1;
                    actorLane = actorLane / 20;
                } else {
                    vx = vx1;
                    vy = vy1;
                    actorLane = actorLane * 20;
                    double x1 = xn - actorLane * (vy) / Math.sqrt((vx * vx + vy * vy));
                    double y1 = yn + actorLane * (vx) / Math.sqrt((vx * vx + vy * vy));
                    x = x1;
                    y = y1;
                    actorLane = actorLane / 20;
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
    private double collisionProbability() {

        HashMap<String, ActorStatus> neighboursList = new HashMap<String, ActorStatus>();
        /** neighbourhood parameters */
        double r0 = 25, r1 = 30; 
        /** looks at short or longer distances depending on the current speed */
        r = r0 + r1 * speed; 
        double tempPc = 0, xi, xf, xiNg, xfNg, yi, yf, yiNg, yfNg;
        pc = 0;
        xi = x;
        yi = y; 
        xf = xi + vx;
        yf = yi + vy; 

        /** retrieves the current neighbours - potential colliders */
        neighboursList.clear();
        neighboursList.putAll(SimStatus.getNeighbours(id, x, y, r));
        frontNeighbours.clear();

        if (!neighboursList.isEmpty()) { 
            Iterator it = neighboursList.keySet().iterator();
            Object key;
            ActorStatus item;

            /** looks into each one of the neighbours and computes the collision probability */
            while (it.hasNext()) {
                key = it.next();
                item = neighboursList.get((String) key);
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
                    if (coincident1 == 0 && coincident2 == 0 || coincident1 < 0.05 && coincident1 > -0.05 && coincident2 < 0.05 && coincident2 > -0.05) {
                        /** checks if this neighbour moving ahead */
                        if (d3 > d2) {
                            /** checks if the neighbour is moving in the same direction */
                            if (d3 > d1) {
                                frontNeighbours.put(nid, item);
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
                                    double var = speed - speed_viz2;
                                     if(var<1){
                                        var=1;
                                    }
                                    tempPc = 1 - (Math.exp(-(((var) * 15) / d1)));
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
                    /** checks if the two spped vectors will intercept ahead */
                    if (d3 > d4) {
                        double di = Math.sqrt(Math.pow((piX - xi), 2) + Math.pow((piY - yi), 2));
                        double diNg = Math.sqrt(Math.pow((piX - xiNg), 2) + Math.pow((piY - yiNg), 2));
                        double time = di / speed;
                        double timeNg = diNg / neighbourSpeed;
                        tempPc = 1 - Math.exp(15 / Math.abs(time - timeNg));
                    } else { 
                        //the two speed vectors will not intercept ahead
                    }
                }
                /** checks if the probability of collision with the current neighbour is the highest */
                if (pc < tempPc) {
                    pc = tempPc;
                }
            }
        }
        return (pc);
    }

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
    private void updateSpeed(double colisionProb) {

        maxSpeed = Double.parseDouble(getWayTag("maxspeed", "50"));
        maxLimit = (maxSpeed + 20) / 3600;
        n_lanes = Integer.parseInt(getWayTag("lanes", "1"));
        /** converts km/h to m/ms */
        maxSpeed = maxSpeed / 3600;
        if (speed != 0) {
            if (lane < 0) {
                lane = 0;
            }
            if (mode == 0) {
                /** traffic light in the way */
                if (tlState == 1) {
                    int estado = SimStatus.globalActors.get("TrafficLight." + id_tl).getTlState();
                    /** yellow or red traffic light */
                    if (estado == 1 || estado == 2) {
                        tlDistance = Math.sqrt((x - x_traffic) * (x - x_traffic) + (y - y_traffic) * (y - y_traffic));
                        new_speed = k * Math.sqrt(tlDistance - 1);
                        new_speed = new_speed * 0.001;
                        /** decreases speed */    
                        if (speed > new_speed) {
                            speed = new_speed;
                        }
                        if ((xfs != x_traffic && yfs != y_traffic)) {
                            vx1 = vx;
                            vy1 = vy;
                            vx = 0;
                            vy = 0;
                            speed = 0;

                        }
                    }
                }

                if (colisionProb > 0 && colisionProb < 0.95) {
                    /** decreases speed */
                    speed = (1 - colisionProb) * speed;
                } else if (colisionProb >= 0.95) 
                {
                    vx1 = vx;
                    vy1 = vy;
                    vx = 0;
                    vy = 0;
                    speed = 0;
                }
                if (speed < maxSpeed) {
                    if (speed == 0) {
                        speed = initialSpeed * 0.1;
                    }
                    speed = speed * 1.1;
                }
                if (speed > maxSpeed) {
                    speed = maxSpeed;
                }
                
                /** tries to go the right-most lane */
                if (actorLane > 1) {

                   ActorStatus as = getFrontNeighbour(actorLane - 1);
                    if (as == null || as.getActor_speed() <= speed) {
                       actorLane--;
                    }
                }
                if (colisionProb >= 0.7 && colisionProb <= 0.8) {
                    if (n_lanes > actorLane) {
                        /** starts overtaking */
                        ActorStatus as = getFrontNeighbour(actorLane);
                        if (as != null) {
                            if (as.getActor_speed() < speed - 0.003) {
                                ActorStatus as2 = getFrontNeighbour(actorLane + 1);
                                if (as2 == null || speed <= as2.getActor_speed()) {
                                    actorLane++;
                                }

                            }
                        }
                    }
                }

            }
            /** agressive mode */
            if (mode == 1) 
            {
                if (tlState == 1) {
                    int state = SimStatus.globalActors.get("TrafficLight." + id_tl).getTlState();
                    
                    /** traffic light is red or yellow */
                    if (state == 1 || state == 2) {
                        tlDistance = Math.sqrt((x - x_traffic) * (x - x_traffic) + (y - y_traffic) * (y - y_traffic));
                        new_speed = k * Math.sqrt(tlDistance - 1);
                        new_speed = new_speed * 0.001;
                        /** decreases speed */
                        if (speed > new_speed) {
                            speed = new_speed;
                        }
                        if ((xfs != x_traffic && yfs != y_traffic)) {
                            vx1 = vx;
                            vy1 = vy;
                            vx = 0;
                            vy = 0;
                            speed = 0;
                        }
                    }
                }
            }

            {
                if (colisionProb > 0 && colisionProb < 0.95) {
                    /** decreases speed */
                    speed = (1 - colisionProb) * speed;
                } else if (colisionProb >= 0.95) 
                {
                    vx1 = vx;
                    vy1 = vy;
                    vx = 0;
                    vy = 0;
                    speed = 0;
                }
                if (speed < maxLimit) {
                    if (speed == 0) {
                        speed = initialSpeed * 0.1;
                    }
                    speed = speed * 1.1;
                }
                if (speed > maxLimit) {
                    speed = maxLimit;
                }
                /** tries to go to the right-most lane */
                if (actorLane > 1) {
                    ActorStatus as = getFrontNeighbour(actorLane - 1);
                    if (as == null || as.getActor_speed() <= speed) {
                        actorLane--;
                    }
                }
                if (colisionProb >= 0.2 && colisionProb <= 0.3) {
                    /** starts overtaking */
                    if (n_lanes > actorLane) {
                        ActorStatus as = getFrontNeighbour(actorLane);
                        if (as != null) {
                            if (as.getActor_speed() < speed - 0.003) {
                                ActorStatus as2 = getFrontNeighbour(actorLane + 1);
                                if (as2 == null || speed <= as2.getActor_speed()) {
                                    actorLane++;
                                }

                            }
                        }
                    }
                }
            }

            /** parking mode */
            if (mode == 2) {
               
                if (speed < 0.01111111) {//40Km/h
                    actorLane = -3;
                    vx1 = vx;
                    vy1 = vy;
                    vx = 0.0;
                    vy = 0.0;
                    speed = 0.0;
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
    
    /** Gets the <ActorStatus> of the front Neighbour.
     * 
     * @param lane the current lane
     * @return the status of the front neighbour
     */
    private ActorStatus getFrontNeighbour(int lane) {

        ActorStatus as, as2 = null;
        double distance = 100;
        Set<String> set = frontNeighbours.keySet();
        for (String key : set) {
            as = frontNeighbours.get(key);
            int laneNg = as.getLane();
            double x = as.getActor_x();
            double y = as.getActor_y();
            double xNg = as.getActor_vx();
            double yNg = as.getActor_vy();

            if (laneNg == lane) {
                double tempDist = Math.sqrt((x - xNg) * (x - xNg) + (y - yNg) * (y - yNg));
                if (distance < tempDist) {
                    distance = tempDist;
                    as2 = as;
                }
            }
        }
        return as2;
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

        /** checks traffic lights */
        if (linesMap.getPoints().get(actual_point_id).getTags().containsKey("highway")) {
            if (linesMap.getPoints().get(actual_point_id).getTags().get("highway").equals("traffic_signals")) {
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

}
