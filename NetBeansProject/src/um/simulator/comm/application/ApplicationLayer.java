/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.application;

import um.simulator.comm.network.NetworkLayer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import um.simulator.comm.network.NPDU;
import um.simulator.core.SimStatus;

/**
 * This class represents the Application Layer.
 * 
 * @author joaop
 * @author luisacabs
 * @version 1.0
 */
public class ApplicationLayer {

    
    /** actor's id */
    String id; 
    float generationProb; 
    /** application name */
    String appName;
    /** application's operation mode */
    char opMode;
    NetworkLayer network;
    /** application */
    Application app;
    boolean appRunning;
    
    
    /**
     * Constructor: creates an application layer.
     * 
     * @param id    the actor running the application
     * @param appName the application's name
     * @param opMode    the operation mode
     * @param net   a reference to the network layer
     */ 
    public ApplicationLayer(String id, String appName, char opMode, NetworkLayer net) {
        this.generationProb = 1;
        this.id = id;
        this.network = net;
        this.appName = appName;
        this.opMode = opMode;
        
        if(appName.contains("BasicApplication")){
            appRunning = true;
            this.app = new BasicApplication(network,appName,opMode,this.id);
        }
    }
    
    /**
     * Calls <code>update()</code> method in the running application.
     */
    public void update() {
        if(appRunning)
            app.update();
    }
    
    /**
     * Receive packets from the lower layer
     * @param packets an array list containing application pdus
     */
    public void receivePacketsUp(ArrayList<APDU> packets) {
        if(appRunning)
            app.receivePacketsUp(packets);
    }
}
