package um.simulator.core;

import um.simulator.comm.physical.PPDU;
import java.util.*;
import um.simulator.actor.ActorStatus;
import um.simulator.map.*;
import um.simulator.reporting.ReportLocal;

/**
 * This class represents the current status of the simulator. 
 * Includes the status of all actors in the simulation and the required 
 * maps for the actors running in this instance of the simulator.
 *
 * @author XT17 
 * @author Adriano Moreira 
 * @author Maria João Nicolau
 * @author luisacabs   
 * @version 1.0
 */
public class SimStatus {

    public static HashMap<String, GlobalMap> maps = new HashMap<>();
    public static Map<String, ActorStatus> globalActors = Collections.synchronizedMap(new HashMap<String, ActorStatus>());
    public static ArrayList<String> localActorsList = new ArrayList<>();
    public static HashMap<String, Double> machineLoadMap = new HashMap<>();
    /** stand by messages */
    public static HashMap<String,ArrayList<PPDU>> globalMessages = new HashMap<String, ArrayList<PPDU>>(); 
    public static HashMap<String,String> appList = new HashMap<String,String>();
    /** false if Global Coordinator is shutting down */
    public static boolean running = true;
    /** local coordinator reporting */
    public static ReportLocal reportLocal;
    /** layers of reporting */
    public static boolean physical = false;
    public static boolean network = false;
    public static boolean application = false;
    
    
    /**
     * Resets the SimStatus. 
     * Clears the actors and machine load: maps are not affected.
     */
    public static void clear() {
        globalActors.clear();
        machineLoadMap.clear();
    }

    
    /** 
     * Adds a new map to the simulation.
     * 
     * @param nameMap   A name to give to the new map.
     * @param map   The map to be added to <code>Simstatus</code>.
     */
    public static void addMap(String nameMap, GlobalMap map) {
        maps.put(nameMap,map);
    }

    /**
     * Adds an unnamed map to the simulation.
     * 
     * @param map   the map to be added to <code>SimStatus</code>.
     * @return new map name
     */
    public static String addMap(GlobalMap map) {
        String newMapName = "Map." + (maps.size() + 1);
        maps.put(newMapName, map);
        return newMapName;
    }

    /**
     * Sets a map as used.
     * 
     * @param mapName   Name of the used map.
     */
    public static void setMapAsUsed(String mapName) {
        maps.get(mapName).beingUsedMap = true;
    }

    /**
     * Sets the a map's lines colour.
     * 
     * @param mapName   Name of the map to change colours.
     * @param linesColour   New colour.
     */
    public static void setMapLinesColour(String mapName, String linesColour) {
        maps.get(mapName).linesColour = linesColour;
    }

    
    /**
     * Registers a new actor to the simulation. 
     * This method is called the first time a <code>Actor</code> is started.
     *
     * @param id    Id of the new <code>Actor</code>.
     * @param x     Initial x axis position.
     * @param y     Initial y axis position.
     * @param label The actor's initial label.
     */
    public synchronized static void registerNewActor(String id, double x, double y, String label) {
        ActorStatus as = new ActorStatus(id, x, y, label);
        globalActors.put(as.getActorId(), as);
        registerLocalActor(id);
        registerActorforMessages(id); 
    }

    /**
     * Updates <code>Actor</code>.
     * 
     * @param id    Id of the <code>Actor</code>.
     * @param x     Current x axis position.
     * @param y     Current y axis position.
     * @param label The actor's current label.
     */
    public synchronized static void setActorStatus(String id, double x, double y, String label) {
        ActorStatus as = new ActorStatus(id, x, y, label);
        globalActors.put(as.getActorId(), as);
    }

    /**
     * Updates <code>Actor</code>, including its speed.
     * 
     * @param id    Id of the <code>Actor</code>.
     * @param x     Current x axis position.
     * @param y     Current y axis position.
     * @param vx    Current x axis speed.
     * @param vy    Current y axis speed.
     * @param label The actor's current label.
     */
    public synchronized static void setActorStatus(String id, double x, double y, double vx, double vy, String label) {
        ActorStatus as = new ActorStatus(id, x, y, vx, vy, label);
        globalActors.put(as.getActorId(), as);
    }

    /**
     * Updates <code>Actor</code>, including its speed and starts the dying process.
     * 
     * @param id    Id of the <code>Actor</code>.
     * @param x     Current x axis position.
     * @param y     Current y axis position.
     * @param vx    Current x axis speed.
     * @param vy    Current y axis speed.
     * @param label The actor's current label.
     * @param dying dying flag
     */
    public synchronized static void setActorStatus(String id, double x, double y, double vx, double vy, String label, boolean dying) {
        ActorStatus as = new ActorStatus(id, x, y, vx, vy, label);
        /** start dying process */
        as.setDying(dying);
        globalActors.put(as.getActorId(), as);
    }

    
    /**
     * Sets, updates or removes the status of an actor from <code>ActorStatus</code>. 
     * @param as new actor status
     */
    public synchronized static void setActorStatus(ActorStatus as) {
        if (as.actorDyingQ()) {
            globalActors.remove(as.getActorId());
        } else {
            globalActors.put(as.getActorId(), as);
        }
    }

    /**
     * Deletes a <code>Actor</code> from the simulation.
     * 
     * @param id The actor's id to remove.
     */
    public synchronized static void removeActor(String id) {
        
        if (localActorsList.remove(id)) {
            if (globalActors.remove(id) == null) {
                System.out.println("WARNING: SimStatus.removeActor(): actor with id " + id + " could not be removed from the globalActors list!");
            }
        } else {
            System.out.println("WARNING: SimStatus.removeActor(): actor with id " + id + " could not be removed from the localActorsList!");
        }
        /** notifies <code>LocalCoordinator</code> that all actors were removed */
        if(localActorsList.isEmpty()){
            synchronized(localActorsList){
                localActorsList.notify();
            }
            
        }
    }

    /**
     * Sets an <code>Actor</code> as ready to receive new messages.
     * 
     * @param id    The actor's id.
     */
    public static void registerActorforMessages(String id) {
        synchronized (globalMessages) {
            if (!globalMessages.containsKey(id)) {
                globalMessages.put(id, new ArrayList<PPDU>());
            }

            if (!globalMessages.containsKey("Broadcast")) {
                globalMessages.put("Broadcast", new ArrayList<PPDU>());
            }
        }
    }

    /**
     * Adds new <code>Actor</code> to the local actors.
     * 
     * @param id    New actor's id.
     */
    public synchronized static void registerLocalActor(String id) {
        
        if (!localActorsList.contains(id)) {
            localActorsList.add(id);
        }
    }
  

    
    /**
     * Updates the status of a list of actors.
     * 
     * @param globalActorList   A list of actors to update.
     */
    public synchronized static void setListActorStatus(HashMap<String, ActorStatus> globalActorList) {
        for (ActorStatus actor : globalActorList.values()) {
            if (!localActorsList.contains(actor.getActorId())) {
                globalActors.put(actor.getActorId(), actor);
            }
        }
    }

    /**
     * Returns the status of an actor given its id.
     *
     * @param a_id The actor's id.
     * @return The status of the actor.
     */
    public static ActorStatus getActorStatus(String a_id) {
        return globalActors.get(a_id);
    }

    /**
     * This method traverses all the actors to search for the neighbours of a
     * given point within a given radius. 
     *
     * @param a_id  The actor's id.
     * @param x     Position in the x axis.
     * @param y     Position in the y axis.
     * @param radius    Distance from the given point and the neighbours to return.
     * @return  A list of neighbours.
     */
    public synchronized static HashMap<String, ActorStatus> getNeighbours(String a_id, double x, double y, double radius) {
        double distance2;
        double r2=radius;
        
        HashMap<String, ActorStatus> neighbours = new HashMap<>();

        for (ActorStatus as : globalActors.values()) {
            distance2 = Math.sqrt(CoordinatesHelper.distanceSquare(x, y, as.getActor_x(), as.getActor_y()));
            if (!as.getActorId().contains(a_id) && distance2 <= r2) {
                neighbours.put(as.getActorId(), as);
            }
        }
        return neighbours;
    }

    /**
     * Sets or updates the load of a machine.
     * @param loadAndIP     Machine load and given IP addres separated with ':'.
     */
    public static void setMachineLoad(String loadAndIP) {
        String[] parts = loadAndIP.split("/");
        machineLoadMap.put(parts[1], Double.parseDouble(parts[0]));
    }

    /**
     * Gets the lightest load machine.
     * 
     * @return  IP address of the lightest load.
     */
    public static String getBestMachine() {
        Collection c = machineLoadMap.keySet();
        Iterator itr = c.iterator();
        double minLoad = 10000;
        double load;
        String machineIP;
        String bestMachineIP = null;
        while (itr.hasNext()) {
            machineIP = (String) itr.next();
            if ((load = machineLoadMap.get(machineIP)) < minLoad) {
                minLoad = load;
                bestMachineIP = machineIP;
            }
        }
        return bestMachineIP;
    }

    /**
     * Adds a new <code>PPDU</code> packet to <code>globalMessages</code>.
     * 
     * @param m     A <code>PPDU</code> packet to add.
     */
    public static void addMessage(PPDU m) {

        if (localActorsList.contains(m.getDestination_id())) {
            synchronized (globalMessages.get(m.getDestination_id())) {
                globalMessages.get(m.getDestination_id()).add(m);
            }
        }
    }

    /**
     * Adds a new <code>PPDU</code> packet to all local actors (broadcast).
     * 
     * @param m     A <code>PPDU</code> packet to add.
     */
    public static void addBroadcastMessage(PPDU m) {
        ArrayList<String> localActorsListCopy = new ArrayList<String>();
        synchronized (localActorsList) {
            localActorsListCopy.addAll(SimStatus.localActorsList);
        }
        /** adds the broadcast message to all local actors */
        for (String actor_id : localActorsListCopy) {

            /** If destiny != source */
            if (!actor_id.equals(m.getSource_id())) {
                synchronized (globalMessages.get(actor_id)) {
                    globalMessages.get(actor_id).add(new PPDU(m));
                }
            }
        }

        /** adds to broadcast list */
        synchronized (globalMessages.get("Broadcast")) {
            globalMessages.get("Broadcast").add(new PPDU(m));
        }
    }

    /**
     * Registers the recepetion of a broadcast message.
     * 
     * @param m     The received <code>PPDU</code> packet.
     */
    public static void addBroadcastMessageReceived(PPDU m) {
        ArrayList<String> localActorsListCopy = new ArrayList<String>();
        synchronized (localActorsList) {
            localActorsListCopy.addAll(SimStatus.localActorsList);
        }

        /** adds the broadcast message to all local actors */
        for (String actor_id : localActorsListCopy) {

            /** If destiny != source */
            if (!actor_id.equals(m.getSource_id())) {
                synchronized (globalMessages.get(actor_id)) {
                    globalMessages.get(actor_id).add(new PPDU(m));
                }
            }
        }

    }
}
