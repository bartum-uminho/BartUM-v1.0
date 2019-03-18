/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
 */
package um.simulator.comm.network;

import java.util.HashMap;

/** This Object represents an active Neighbor from <code>PRoPHET</code> protocol.
 * It's used by <code>PRoPHET_NeighboursManager</code>.
 * 
 * @author nunojam
 * @version 1.0
*/
public class PRoPHET_Neighbour {
    
    String address;
    
    /** High-Level States.
     * HELLO+VECTOR = 1;
     * DATA = 2
     */
    Integer state;
    Long lastStateUpdate;
    
    /** Last time seen. Used by beacon */
    Long lastTimeSeen;

    /** neighbour's summary vector */
    HashMap<String, Double> vetor;

    /** Constructor: creates a PRoPHET Neighbour. 
     * 
     * @param address a destination address
     */
    PRoPHET_Neighbour(String address) {
        long currT = System.currentTimeMillis();
        this.lastStateUpdate = currT;
        this.lastTimeSeen = currT;
        this.address = address;
        this.state = 1;
        this.vetor = new HashMap();
    }

}
