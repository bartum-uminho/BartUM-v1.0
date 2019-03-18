/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.core;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class defines different types of logging for different simulation's components.
 * 
 * @author XT17
 * @author luisacabs   
 * @version 1.0
 */
public class NetworkLogging {
    private static final Logger fLogger=Logger.getLogger(NetworkLogging.class.getPackage().getName());
    public static int typeOfLogging;
    public static FileHandler fh;
    
    /**
     * Constructor.
     * @param type  a type of logging 
     */
    public NetworkLogging(String type){
        try {
            typeOfLogging=Integer.parseInt(type);
            fh = new FileHandler("log.txt");
            fh.setLevel(Level.ALL);
            fh.setFormatter(new SimpleFormatter());
        } catch (IOException ex) {
            Logger.getLogger(NetworkLogging.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(NetworkLogging.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    /**
     * Logging call.
     * 
     * @param typeReq   A type of logging.
     * @param message   A message to add to the <code>Logger</code>.
     */
    public static void log(String typeReq,String message){
        if(typeOfLogging==1){
            fLogger.setLevel(Level.ALL);

            if(typeReq.contains("warning")){
                fLogger.addHandler(fh);
                fLogger.warning(message);
            }
            if(typeReq.contains("config")){
                fLogger.addHandler(fh);
                fLogger.config(message);
            }   
        }
        if(typeOfLogging==2){
            if(typeReq.contains("info")){
                fLogger.addHandler(fh);
                fLogger.info(message);
            }
            if(typeReq.contains("warning")){
                fLogger.addHandler(fh);
                fLogger.warning(message);
            }
            if(typeReq.contains("config")){
                fLogger.addHandler(fh);
                fLogger.config(message);
            }
        }
    }
}
