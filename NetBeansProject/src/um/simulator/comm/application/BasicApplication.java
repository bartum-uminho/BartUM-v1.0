/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package um.simulator.comm.application;

import java.util.ArrayList;
import java.util.Random;
import um.simulator.comm.network.NetworkLayer;
import um.simulator.core.SimStatus;
import um.simulator.reporting.ReportLocal;

/**
 * This class represents a Basic Application implementing burst transmission.
 * 
 * @author luisacabs
 * @version 1.0
 */
public class BasicApplication extends Application {
    String msgPayload;
    /** number of messages per cycle*/
    public int nrofMsgCycle;
    /** beggining of message generation */
    public int msgGenStart;
    /** message generation time */
    public int msgGenTime;
    /** time break between cycles */
    public int msgBreakTime;
    /** destination of the generated messages */
    String msgDestination;
    /** number of cycles */
    public int nrofCycles;
    
    /** time passed between messages in the same cycle */
    int i = 5;
    /** time passed since CommStack is running */
    int seconds = 0;
    /** number of messages sent in the current cycle */
    int msgCycle = 0;
    /** number of cycles */
    int cycles = 0;
    /** time passed in the time break between cycles */
    int timeBreak=0;
    
    /**
     * Constructor: creates a BasicApplication.
     * 
     * @param net   a reference to the network layer
     * @param app   the name of the application running
     * @param opMode the operation mode 
     * @param id  the id of the actor running the simulation
     */
    public BasicApplication(NetworkLayer net, String app, char opMode, String id) {
        
        super(net, app, opMode, id);
        String appParams = SimStatus.appList.get(appName);
        String params[] = appParams.split(":");
        /**
         * If the actor is running the application in transmitter or transceiver mode
         * it must define the following parameters
         */
        if(opMode=='T' || opMode=='B'){
            msgPayload = params[1];
            msgDestination = params[2];
            msgGenStart = Integer.parseInt(params[3]);
            msgGenTime = Integer.parseInt(params[4]);
            msgBreakTime = Integer.parseInt(params[5]);
            nrofMsgCycle = Integer.parseInt(params[6]);
            int msgTime = msgGenTime - msgGenStart;
            int cycle = nrofMsgCycle*10 + msgBreakTime;
            nrofCycles = msgTime/cycle;
            if((nrofCycles+cycle)<=msgTime){
                nrofCycles++;
            }
        }
        if(opMode=='R' || opMode=='B'){
                
        }
    }
    /**
     * Update the Application Layer and sends a message if required.
     * 
     * This class, implements a message generation algorithm based on transmission bursts.
     * 
     */
    @Override
    public void update(){
        if(opMode=='T' || opMode=='B'){
            if(seconds>=msgGenStart && cycles<nrofCycles){
                if(msgCycle<nrofMsgCycle){
                /** in each cycle, a number of messages are generated 
                * the time between each message generation is defined as 5 seconds
                */  
                    if(i==5){
                        msgCycle++;
                        generateMessage();
                        network.getBufferOcupation();
                        i=0;
                    }
                }else if (timeBreak<msgBreakTime){
                /** after all the messages are sent, the application waits for 
                * <code>timeBreak</code> seconds before starting a new cycle
                */
                    timeBreak++;
                }else if(timeBreak==msgBreakTime && msgCycle==nrofMsgCycle){
                /** break between cycles is over - reset counters */
                    timeBreak=0;
                    msgCycle=0;
                    cycles++;
                    i=4;
                }else{
                }
                i++;
            }
            seconds++;
        }
    }
    /**
     * Generates an <code>APDU</code> and sends it to the network layer.
     * 
     * The payload of the generated message, has a specific syntax that allows the 
     * nodes to share different types of data in the same message.
     * - %rX-Y: generates a random value between X and Y;
     * - %rW-Z-Y-Z: generates a random value within the given values W, X, Y and Z.
     * - %c: integer value
     * - %s: string
     * - %i: this actor's id
     * - %p: actor's position at the time of message generation
     * - %t: timestamp at the time of message generation
     */
    @Override
    public void generateMessage(){
        
        if(msgDestination.contains("null")){
            msgDestination = generateRandomDestination();
        }
        String data = "";
        String fields[] = msgPayload.split("%");
        for(int i = 0;i<fields.length;i++){
            if(i>0)
                data+="%";
            if(fields[i].contains("-")){
                String values[];
                Random r = new Random();
                int rest;
                values = fields[i].substring(1).split("-");
                if(values.length>2){
                    rest = r.nextInt(values.length - 1);
                    data += values[rest];
                }else{
                    rest = r.nextInt(Integer.parseInt(values[1]) - Integer.parseInt(values[0])) + Integer.parseInt(values[0]);
                    data += Integer.toString(rest);
                }
            }else if(fields[i].contains("c")){
                int val = Integer.parseInt(fields[i].substring(1));
                data+= val;
            }else if(fields[i].contains("s")){
                String sval = fields[i].substring(1);
                data+=sval;
            }else if(fields[i].contains("i")){
                data += network.link.phy.actor.getActorId();
            }else if(fields[i].contains("p")){
                data +=network.link.phy.actor.getX() + "-" + network.link.phy.actor.getY();
            }else if(fields[i].contains("t")){
                data+= System.currentTimeMillis();
            }else if (!msgPayload.contains(null)){
                data = msgPayload;
            }
        }
        
        System.out.println(actorId + " GENERATED MESSAGE: " + data);
        APDU traffic = new APDU(System.currentTimeMillis() + ":" + appName + ":" + data);
        SimStatus.reportLocal.reportSentMessage(actorId,7,data,-5);
        /** the APDU and the destination address are sent to the network layer */
        network.generatedDataPacket(msgDestination, traffic);
    }
    
    @Override
    public void processReceivedMessage(APDU a){
        SimStatus.reportLocal.reportReceivedMessage(actorId,7,a.getData(),-5);
    }
    
    @Override
    public String generateRandomDestination() {
        return super.generateRandomDestination();
    }
    
    @Override
    public void receivePacketsUp(ArrayList<APDU> packets) {
        if(opMode == 'R' || opMode == 'B'){
            String[] dataSplit;
            String message;
            String app;
            for (APDU m : packets) {
                /** APDU. sourceId:app:timestamp:x:y:message */
                dataSplit = m.toString().split(":");
                app = dataSplit[1];
                message = dataSplit[2];
                if(app.equals(appName)){
                    processReceivedMessage(m);
                    System.out.println("\n" + actorId +":\tMessage received from a node running application " + app
                    + ". \n\tMessage:\n\t\t" + message);
                }
            }
        }
    }
}
