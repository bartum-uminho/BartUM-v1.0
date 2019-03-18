/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.application;

/**
 * This class represents an Application Layer PDU. 
 * 
 * @author joaop
 * @author luisacabs
 * @version 1.0
 */
public class APDU {
    /** PDU payload */
    String data;
    
    /** Constructor: creates an Application PDU from a string of data.
     * 
     * @param data  payload of the PDU
     */
    public APDU(String data) {
        this.data = data;
    }
    /** Retrieves the PDU's payload.
     * 
     * @return payload
     */
    public String getData() {
        return data;
    }
    
    /** Changes the PDU's payload.
     * 
     * @param data  new payload
     */
    public void setData(String data) {
        this.data = data;
    }

    /** Converts object APDU to a String.
     * 
     * @return PDU's payload
     */
    @Override
    public String toString() {
        return data;
    }
    
    
}
