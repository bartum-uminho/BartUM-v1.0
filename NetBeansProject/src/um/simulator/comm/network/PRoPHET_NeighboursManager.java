/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.network;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.link.LinkLayer;

/** This class manages the neighbours of a node by sending and receiving BEACONS.
 * It updates the neighbours list for the network protocol.
 *
 * @author nunojam
 * @version 1.0
 */
public class PRoPHET_NeighboursManager extends Thread {

    int beaconTTL; 
    /** interval bettween beacons (in milliseconds)interval bettween beacons (in milliseconds)*/
    int beacon_interval;
    /** timeout value of a neighbour (in milliseconds) */
    int timeout_value; 
    String nodeID; 
    /** link layer object used to broadcast beacons */
    LinkLayer linkLayer; 
    
    PRoPHET prophet;

    /** Shared variables */
    /**
     * Current Neighbors List examined by other threads
     * @see #neighborsManager
     */
    ConcurrentHashMap<String, PRoPHET_Neighbour> neighbors;
    /** Locks */
    public Lock neighborsLock;
    public Condition neighborsNotFull;
    public Condition neighborsNotEmpty;

    volatile boolean running = true;
    
    /** Consctructor: creates a PRoPHET neighbour's manager. 
     * 
     * @param prophet   the prophet protocol
     */
    public PRoPHET_NeighboursManager(PRoPHET prophet) {
        this.prophet = prophet;
        this.nodeID = prophet.nodeID;
        this.linkLayer = prophet.linkLayer;
        this.neighbors = prophet.neighbors;
        this.neighborsLock = prophet.neighborsLock;
        this.neighborsNotEmpty = prophet.neighborsNotEmpty;
        this.beacon_interval = 1000; 
        this.timeout_value = 3000; 
    }

    @Override
    public void run() {
        
        while (running) {
            /** checks neigbours timeout */
            timeoutNeighbors();
            /** broadcasts BEACON */
            broadcastBeacon();
            try {
                Thread.sleep(beacon_interval);
            } catch (InterruptedException ex) {
                Logger.getLogger(Epidemic.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Broadcasts a beacon to radio interface.
     *
     * @see NPDU
     */
    private void broadcastBeacon() {
        String data = "B," + nodeID;
        long currTime = System.currentTimeMillis();
        NPDU npdu = new NPDU(nodeID, "Broadcast", currTime+"", "PRoPHET", 2, data);
        linkLayer.sendFrame(npdu, "Broadcast");
    }

    /**
     * This procedure checks if any neighbour is out of range by checking the
     * last time beacon.
     */
    private void timeoutNeighbors() {
        neighborsLock.lock();
        try {
            Iterator<String> it = neighbors.keySet().iterator();
            while(it.hasNext()){
                String key_id = it.next();
                long curr_time = System.currentTimeMillis();
                long updt_time = neighbors.get(key_id).lastTimeSeen;
                long diff = curr_time - updt_time;
                if (diff > timeout_value) {
                    it.remove();
                }
            }
        } finally {
            neighborsLock.unlock();
        }
    }

    /** Changes the flag to exit the while loop and end the neighbours manager. */
    void endManager() {
        running = false;
    }

    /**
     * This method is called by a lower layer when a beacon is received. 
     * It puts a new Entry to neighbours hash or updates an existing one
     *
     * @param npdu Beacon Packet Received
     */
    public void beaconReceived(NPDU npdu) {
        String id = npdu.getSourceAddress();
        PRoPHET_Neighbour ns;
        neighborsLock.lock();
        try {
            if (!neighbors.containsKey(id)) { 
                ns = new PRoPHET_Neighbour(id);
                prophet.newNeighbour(ns);
                neighborsNotEmpty.signal();
            } else { 
                /** if the node is already a neighbour, updates the timeout */
                ns = neighbors.get(id);
                ns.lastTimeSeen = System.currentTimeMillis();
                neighbors.put(id, ns);
            }
        } finally {
            neighborsLock.unlock();
        }
    }
    
}
