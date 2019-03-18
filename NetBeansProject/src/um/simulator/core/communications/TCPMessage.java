/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.core.communications;

import java.io.File;
import java.io.Serializable;
import um.simulator.map.GlobalMap;

/**
 * This class is a representation of a data packet used in actor's communications.
 * 
 * @author João
 * @author luisacabs
 * @version 1.0
 * 
 */
public class TCPMessage implements Serializable {
    public String messageType;
    public String messageStr;
    public GlobalMap map;
    public byte[] fileContent;
    
/**
 * Constructor: Creates a new packet with no parameters.
 */    
public TCPMessage () {
}
/**
 * Constructor: Creates a new packet with a given type.
 * <p>
 * <code>messageType</code>: <br>
 * <b>"01"</b> <code>LocalCoordinator/Visualization</code> announces itself to the <code>GlobalCoordinator</code> <br>
 * <b>"02"</b> Used between <code>LocalCoordinator</code> and <code>GlobalCoordinator</code> to share <code>Actor</code>'s data <br>
 * <b>"03"</b> <code>LocalCoordinator</code> asks for the map specified in <code>messageStr</code> <br>
 * <b>"04"</b> <code>GlobalCoordinator</code> sends map to <code>LocalCoordinator</code> 
 * <b>"05"</b> <code>GlobalCoordinator</code> sends kill message to <code>LocalCoordinator</code>
 * </p>
 * @param type      A string specifying the message type.
 */
public TCPMessage (String type) {
    this.messageType = type;
}


}
