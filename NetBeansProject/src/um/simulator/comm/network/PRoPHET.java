/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
 */
package um.simulator.comm.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.application.APDU;
import um.simulator.comm.link.LinkLayer;
import um.simulator.core.SimStatus;

/**
 * This class represents the PRoPHET protocol.
 * 
 * Tries to identify movement paterns to reduce the flood of messages 
 * produced by the <code>Epidemic</code> protocol. It uses a probabilistic metric to
 * decide whether or not the message is relayed.
 * 
 * @author nunojam
 * @version 1.0
 * @see NetworkProtocol
 */
public class PRoPHET extends NetworkProtocol {

    /** output for debugging and tests */
    Calendar cal = Calendar.getInstance();
    PrintWriter prophet_log;
    Lock logLock;
    
    /** TTL for controlling */
    private final int controlTTL;

    /** PRoPHET constants */
    /**
     * Direct contact gain [0,1]
     *
     * @see #directContactFormula(java.lang.String)
     */
    private final double p_encounter = 0.5;

    /**
     * Predictabilities are multiplied by this constant every decayTimeUnit
     * Decay constant [0,1]
     *
     * @see #decayTimeUnit
     * @see #decayDeliveryPreds()
     */
    private final double gamma = 0.98;

    /**
     * Unit to update delivery predictabilities decay (in milisseconds).
     * This value its a reference unit because the predictability update is not
     * periodic
     *
     * @see #decayDeliveryPreds()
     */
    private final long decayTimeUnit;
    
    /** last time the predictabilities were updated by the aging formula */
    private long lastDecayTime;;

    /**
     * Transitivity constant [0,1]
     *
     * @see #transitivityFormula(java.lang.String, java.lang.String, float)
     */
    private final double beta = 0.25;

    /**
     * Delivery predictabilities for all nodes Map: String destination node id,
     * Double delivery predictability.
     * 
     * This object needs a lock to be modified by ComStack thread(when receives Hello)
     */
    ConcurrentHashMap<String, Double> predictabilities;
    Lock predLock;

    /** min value used to discard the predictability entry */
    Double minPred;

    /** timeout value of the neighbour state (in milisseconds) */
    private final int timeoutState;
    
    /** size limit of summary vector */
    private final int vectorLimit;

    /**
     * Neighbors state table (String node_id, PRoPHET_NeighborState)
     * @see PRoPHET_Neighbour
     */
    ConcurrentHashMap<String, PRoPHET_Neighbour> neighbors;

    /** Locks */
    /**
     * Lock for neighbors map (concurrency with PRoPHET_NeighboursManager)
     * 
     * @see PRoPHET_NeighboursManager
     */
    public Lock neighborsLock;

    /** condition to check if neighbors map is empty */
    public Condition neighborsNotEmpty;

    /** thread that manages the neighbors of this node */
    PRoPHET_NeighboursManager neighborsManager;

    /** variables for testing and debuging. */
    int printNeighbors = 0;
    int printBuffer = 0;
    ArrayList<String> nodesLogged;
    
    /** Constructor: creates a PRoPHET Protocol.
     * 
     * @param linkLayer Link Layer Object
     * @param id Node ID
     * @param lineQueue Buffer size limit
     */
    public PRoPHET(LinkLayer linkLayer, String id, int lineQueue) {
        super(linkLayer, id, lineQueue);
        this.lastDecayTime = System.currentTimeMillis();
        this.controlTTL = 3;
        this.protocolName = "PRoPHET";
        this.predictabilities = new ConcurrentHashMap();
        this.minPred = 0.001;
        this.predLock = new ReentrantLock();
        this.neighbors = new ConcurrentHashMap();
        this.neighborsLock = new ReentrantLock();
        this.neighborsNotEmpty = neighborsLock.newCondition();
        this.neighborsManager = new PRoPHET_NeighboursManager(this);
        this.decayTimeUnit = 10000; // milliseconds
        this.timeoutState = 10000;
        this.vectorLimit = 15;
        this.logLock = new ReentrantLock();
        
    }

    @Override
    public void run() {
    
        
        /** starts the neighbors manager thread */
        neighborsManager.start();
        try {
            
            while (running) {
                Thread.sleep(1000);
                /** update predictabilities with decay formula */
                decayDeliveryPreds();
                neighborsLock.lock();
                try {
                    /** verifies if its empty */
                    while (neighbors.isEmpty()) {
                        /** if so, unlocks the hash and waits for a signal */
                        neighborsNotEmpty.await();
                        
                    }
                    /** when a signal is received, locks neighbors again */
                    neighborsLock.lock();
                    try {
                        for (Entry<String, PRoPHET_Neighbour> e : neighbors.entrySet()) {
                            /** neighbour id */
                            String key_id = e.getKey();
                            /** neighbor values */
                            PRoPHET_Neighbour value_n = e.getValue(); 
                            Long lastStateUpdate = value_n.lastStateUpdate;
                            Long currTime = System.currentTimeMillis();
                            /** the delivery predictability is increased for the active neighbors nodes */
                            directContactFormula(key_id);
                            /** if elapsed time is smaller than timeoutState value */
                            if ((currTime - lastStateUpdate) < timeoutState) {
                                /** resends the packet acordingly to state (mandatory for hello) */
                                if(value_n.state==1){
                                    sendSummaryVector(key_id); 
                                }
                            } else {
                                value_n.state = 1;
                                value_n.lastStateUpdate = System.currentTimeMillis();
                            }
                        }
                    } finally {
                        neighborsLock.unlock();
                    }
                } finally {
                    neighborsLock.unlock();
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(PRoPHET.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            neighborsManager.endManager();
        }
    }

    /**
     * Ages all predictablities with decay formula over time.
     * Predictabilities decay <code>gamma</code> to the power of <code>elapsedTimeUnits</code>
     *
     * @see #gamma
     */
    private void decayDeliveryPreds() {
        /** lock can block the thread, so cant check time before lock */
        predLock.lock(); 
        try {   
            long currTime = System.currentTimeMillis();
            long elapsedTime = currTime - lastDecayTime;
            int elapsedTimeUnits = (int) (elapsedTime/decayTimeUnit);
            /** if the updates were to close in time (less than decayTimeUnit), do nothing */
            if (elapsedTimeUnits == 0) {
                return;
            }
            /** decay formula */
            double mult = Math.pow(gamma, elapsedTimeUnits); 
            
            for (Entry<String, Double> e : predictabilities.entrySet()) {
                Double pOld = e.getValue();
                String nID = e.getKey();
                /** if the value is too small, its discarded (not sure if worth the effort) */
                Double pNew = pOld * mult;
                e.setValue(pNew);
            }
            lastDecayTime = System.currentTimeMillis()-(elapsedTime%decayTimeUnit);
            
        } finally {
            predLock.unlock();
        }
    }

    /**
     * This formula is used when a node is in direct contact. 
     * Increments a node delivery predictability given its id.
     *
     * @param id id from node that will be updated
     */
    private void directContactFormula(String id) {
        predLock.lock();
        try {
            if (predictabilities.containsKey(id)) {
                /** if there's already a value, the formula is applied */
                Double pOld = predictabilities.get(id);
                Double pNew = pOld + (1 - pOld) * p_encounter; 
                if (pNew > 1.0) {
                    pNew = 1.0;
                }
                predictabilities.put(id, pNew);

            } else {
                predictabilities.put(id, p_encounter);
            }
        } finally {
            predLock.unlock();
        }
    }

    /**
     * Updates delivery probabilities used when a Vector is received (node A)
     * form other node (node B). It updates the delivery predictability for the
     * node C in the node A table
     *
     * @param destAddress id from final destination node (node C)
     * @param intermAddress id from the intermediary node (node B)
     * @param pBC intermediary node predictability delivery to destination
     */
    private void transitivityFormula(String destAddress, String intermAddress, double pBC) {
        predLock.lock();
        double pAC_old, pAB;
        try {
            if (predictabilities.containsKey(destAddress)) {
                pAC_old = predictabilities.get(destAddress); 
            } else {
                pAC_old = 0;
            }
            if (predictabilities.containsKey(intermAddress)) {
                pAB = predictabilities.get(intermAddress);
            } else {
                pAB = 0;
            }
            double pAC_new = pAC_old + (1 - pAC_old) * pAB * pBC * beta; 

            predictabilities.put(destAddress, pAC_new);

        } finally {
            predLock.unlock();
        }
    }

    /**
     * Sorts a map by its values.
     *
     * @param <K> key type
     * @param <V> value type
     * @param mapToSort map that will be sorted
     * @return sorted map
     */
    private static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValues(final Map<K, V> mapToSort) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(mapToSort.size());
        entries.addAll(mapToSort.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(final Map.Entry<K, V> entry1, final Map.Entry<K, V> entry2) {
                return entry2.getValue().compareTo(entry1.getValue());
            }
        });
        Map<K, V> sortedMap = new ConcurrentHashMap<>();
        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    /**
     * Receive a list of NPDU
     *
     * @param frame list of Network Packets
     * @return list of Aplication Packets
     */
    @Override
    public ArrayList<APDU> sendPacketsUp(ArrayList<NPDU> frame) {
        dataToApp.clear(); // clear the list of frames to be sent to the application layer
        for (NPDU npdu : frame) {
            String destAdd = npdu.getDestinationAddress();
            String sourceAdd = npdu.getSourceAddress();
            if (isInBuffer(npdu) || (history.contains(npdu.getId() + sourceAdd))) 
            {
                boolean isDestination = false;
                if (destAdd.equals(nodeID)){ // stats
                    isDestination = true;
                }
                SimStatus.reportLocal.reportDroppedDuplicateNetworkMessage(nodeID,npdu.getData(),buffer.size(),isDestination);
            } else { 
                parseNPDU(npdu);
            }
            
        }
        return dataToApp;
        
    }

    /**
     * This procedure parses a NPDU and decides what to do with it.
     *
     * @param npdu packet to be parsed
     */
    private void parseNPDU(NPDU npdu) {
        PRoPHET_Neighbour neighbour;
        String source, destAdd, dataFields[], v[];
        source = npdu.getSourceAddress();
        destAdd = npdu.getDestinationAddress();
        /** splits the type from the message, and for dictionaries also splits entries */
        dataFields = npdu.getData().split(",");
        /** if the message its new is sent to the application layer, 
        * if is a confirmation the number of confirmations os the message is increased */
        switch (dataFields[0]) {
            case "B":
                neighborsManager.beaconReceived(npdu);
                break;
            case "V":
                Double prob;
                neighborsLock.lock();
                try {
                    if (neighbors.containsKey(source)) {
                        neighbour = neighbors.get(source);
                        for (int i = 1; i < dataFields.length; i++) {
                            /** split key from value */
                            v = dataFields[i].split(":");
                            prob = Double.parseDouble(v[1]);
                            /** adds vector to neighbour*/
                            neighbour.vetor.put(v[0], prob); 
                            transitivityFormula(source, v[0], prob); // update probabilities
                        }
                        /** sends data if available */
                        trySendData(source);
                        /** updates neighbour to state 2 and timeout */
                        neighbour.state = 2;
                        neighbour.lastStateUpdate = System.currentTimeMillis();
                    }
                } finally {
                    neighborsLock.unlock();
                }
                break;
            case "D":
                if (isInBuffer(npdu) || history.contains(npdu.getId() + source)) {
                    
                    if(destAdd.equals(nodeID)) 
                        break;
                }else{
                    history.add(npdu.getId() + source);
                    if(destAdd.equals(nodeID)){
                        SimStatus.reportLocal.reportReceivedMessage(nodeID,3,npdu.getData(),buffer.size());
                        dataToApp.add(new APDU(dataFields[1]));//the NPDU is desencapsuled and the APDU generated is added to the list of frames to be sent to the application layer
                    }else{
                        addToBuffer(npdu);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Adds a new neighbour.
     * 
     * @param pn new active neighbour
     */
    public void newNeighbour(PRoPHET_Neighbour pn) {
        neighborsLock.lock();
        try {
            neighbors.put(pn.address, pn);
        } finally {
            neighborsLock.unlock();
        }
    }

    /**
     *
     * @param nodeID node's identification
     */
    public void neighbourGone(String nodeID) {
        //neighbors.remove(nodeID);
    }

    /**
     * Compares the own delivery predictabilities with neighbour vectors. 
     * Sends data packets in buffer that have more probability to be delivered 
     * by the neighbour node
     *
     * @param neighborId destination address
     */
    private void trySendData(String neighborId) {
        Double pBC, pAC = 0.0;
        NPDU npdu;
        Long bufferTime;
        synchronized (buffer) {
            for (int i = 0; i < buffer.size(); i++) {
                npdu = buffer.get(i);
                bufferTime = bufferTimes.get(i);
                npdu.updateTTL(bufferTime);
                bufferTimes.remove(i);
                bufferTimes.add(i, System.currentTimeMillis());
                if (npdu.isValid_ttl()) {
                    String destAddress = npdu.getDestinationAddress();
                    /** if the neighbour is the destination of the message */
                    if (destAddress.equals(neighborId)) {
                        this.sendDataPacket(npdu, destAddress);
                        SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
                    } else {
                        predLock.lock();
                        try {
                            /** own delivery predictability to destination */
                            if (predictabilities.containsKey(destAddress)) {
                                pAC = predictabilities.get(destAddress);
                            } else {
                                pAC = 0.0;
                            }
                        } finally {
                            predLock.unlock();
                        }
                        neighborsLock.lock();
                        try {
                            /** delivery predictability from the neighbour node to destination */
                            if (neighbors.containsKey(neighborId) && neighbors.get(neighborId).vetor.containsKey(destAddress)) {
                                pBC = neighbors.get(neighborId).vetor.get(destAddress);
                            } else {
                                pBC = 0.0;
                            }
                        } finally {
                            neighborsLock.unlock();
                        }
                        if (pAC < pBC) {
                            this.sendDataPacket(npdu, neighborId);
                        } else {
                            
                        }
                    }
                } else {
                    /** if is not valid is dropped and logged */
                    SimStatus.reportLocal.reportDroppedTTLNetworkMessage(nodeID,npdu.getData(),buffer.size());
                    this.buffer.remove(i);
                    this.bufferTimes.remove(i);
                }
            }

        }
    }

    /**
     * Sends a probability vector to a given destination address.
     *
     * @param destAddress the destination address
     */
    private void sendSummaryVector(String destAddress) {
        /** limit to the number of characters of the predictability value sent */
        int valueSizeLimit = 5;
        String pv = "";
        Set<Entry<String, Double>> esProb;
        predLock.lock();
        try {
            /** sort hash by values to get the most significant */
            predictabilities = (ConcurrentHashMap<String, Double>) sortMapByValues(predictabilities);
            esProb = predictabilities.entrySet();

            Iterator<Entry<String, Double>> it = esProb.iterator();
            /** iterates over the hash */
            for (int i = 0; it.hasNext() && (i < vectorLimit); i++) {
                String key = it.next().getKey();
                String value;
                try {
                    value = predictabilities.get(key).toString().substring(0, valueSizeLimit);
                    /** uses the most significant algarisms (default is 5) */
                } catch (IndexOutOfBoundsException e) {
                    /** if endIndex is larger than the length of this String
                    * all string is used */
                    value = predictabilities.get(key).toString();
                }
                pv = pv + "," + key + ":" + value;
            }

        } finally {
            predLock.unlock();
        }
        String data = "V" + pv;

        NPDU npdu = new NPDU(nodeID, destAddress, System.currentTimeMillis() + "", "PRoPHET", controlTTL, data);
        linkLayer.sendFrame(npdu, destAddress);
    }

    @Override
    public void generatedDataPacket(String destAddress, APDU apdu){
        String data = "D," + apdu.getData();
        NPDU npdu = new NPDU(nodeID, destAddress, System.currentTimeMillis() + "", "PRoPHET", TTL, data);
        /** adds the message to the list of active messages to be sent */
        addToBuffer(npdu);
        SimStatus.reportLocal.reportSentMessage(nodeID,3,npdu.getData(),buffer.size());
    }

    @Override
    public void sendDataPacket(NPDU npdu, String destAddress){
        this.linkLayer.sendFrame(npdu, destAddress);
    }
    
    /**
     * Send a Unicast "Hello" Packet to a given address (omitted packet).
     *
     * @param destAddress Destination Address
     */
    private void sendHelloPacket(String destAddress) {
        String data = "H," + nodeID;
        NPDU npdu = new NPDU(nodeID, destAddress, System.currentTimeMillis() + "", "PRoPHET", this.controlTTL, data);
        linkLayer.sendFrame(npdu, destAddress); // destination is unicast
    }

     /**
     * Sends a acknowledge packet to confirm a received NPDU (omitted packet).
     *
     * @param npdu Packet to acknowledge
     */
    private void sendAck(NPDU npdu) {
        NPDU n = new NPDU(nodeID, npdu.getSourceAddress(), System.currentTimeMillis() + "",
                "PRoPHET", TTL, "C," + npdu.getId() + npdu.getSourceAddress());
        linkLayer.sendFrame(n, n.getDestinationAddress());
    }
}
